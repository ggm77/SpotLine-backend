package com.pohanghang.spotline.global.infra.video;

import com.pohanghang.spotline.domain.analytics.dto.RawAnalyticsDto;
import com.pohanghang.spotline.domain.analytics.entity.Analytics;
import com.pohanghang.spotline.domain.analytics.repository.AnalyticsRepository;
import com.pohanghang.spotline.domain.video.entity.Status;
import com.pohanghang.spotline.domain.video.entity.Video;
import com.pohanghang.spotline.domain.video.repository.VideoRepository;
import com.pohanghang.spotline.global.config.FfmpegConfig;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoAnalyze {

    private final FfmpegConfig ffmpegConfig;
    private final StorageManager storageManager;
    private final YoloClient yoloClient;
    private final AnalyticsRepository analyticsRepository;
    private final VideoRepository videoRepository;

    @Value("${video.analyze-chunk-size}")
    private int CHUNK_SIZE;

    @Async("asyncExecutor")
    @Transactional
    public void analyze(
            final Video video,
            final LocalDateTime startAt,
            final LocalDateTime endAt
    ) {
        final Path path = storageManager.getPath(video.getName());
        final int durationSec = getVideoLength(path.toString());
        int startTime = 0;
        int chunkIndex = 0;

        final Path outputDir = path.getParent();

        while (startTime < durationSec) {
            final String chunkFileName = "chunk_"+UUID.randomUUID().toString().substring(0,8)+"_"+chunkIndex+".mp4";
            final Path chunkPath = outputDir.resolve(chunkFileName);

            try {
                // 1. FFmpeg를 통해 10분짜리 세그먼트 하나 생성
                cutVideoSegment(path.toString(), chunkPath.toString(), startTime, CHUNK_SIZE);

                if (!chunkPath.toFile().exists()) {
                    log.error("FFmpeg 영상 분할 실패: {}번째 영상", chunkIndex+1);
                    throw new CustomException(ExceptionCode.FFMPEG_ERROR);
                }

                // 2. 외부 API로 영상 분석 요청
                final RawAnalyticsDto rawAnalyticsDto = RawAnalysisParser.parse(yoloClient.analyzeVideo(chunkPath.toString()));

                final LocalDateTime chunkStartAt = startAt.plusSeconds(startTime);
                final LocalDateTime chunkEndAt = startAt.plusSeconds(Math.min(startTime + CHUNK_SIZE, durationSec));

                // 분석 결과 파싱해서 DB 저장
                final Analytics analytics = RawAnalysisConverter.toEntity(rawAnalyticsDto, chunkStartAt, chunkEndAt);
                analyticsRepository.save(analytics);

            } catch (Exception e) {
                // 예외 발생 시 로그를 남기고 비즈니스 요구사항에 따라 멈추거나 다음  chunk로 진행
                log.error("{}번째 세그먼트 처리 중 오류 발생: {}",chunkIndex, e.getMessage());
                continue;
            } finally {
                // 3. 분석 완료 후 세그먼트 파일 즉시 삭제 (디스크 공간 확보)
                if (chunkPath.toFile().exists()) {
                    final boolean deleted = chunkPath.toFile().delete();
                    if (!deleted) {
                        log.error("파일 삭제 실패 (프로세스가 점유 중일 수 있음): {}번째 영상", chunkIndex+1);
                    }
                }
            }

            startTime += CHUNK_SIZE;
            chunkIndex++;
        }

        // 영상 처리 완료 되었다고 저장
        final Video savedVideo = videoRepository.findById(video.getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.VIDEO_NOT_FOUND));
        savedVideo.updateStatus(Status.COMPLETE);
        log.info("영상 처리 성공: {}", video.getName());
    }

    private int getVideoLength(final String filePath) {

        if (filePath == null || filePath.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        List<String> command = new ArrayList<>();
        command.add(ffmpegConfig.getFfprobe());
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("format=duration");
        command.add("-of");
        command.add("default=noprint_wrappers=1:nokey=1");
        command.add(filePath);

        final String output = execute(command, 30);

        return (int) Double.parseDouble(output.trim());
    }

    private void cutVideoSegment(
            final String inputPath,
            final String outputPath,
            final int startSeconds,
            final int durationSeconds
    ) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegConfig.getFfmpeg());
        command.add("-y");                      // 기존 파일 덮어쓰기 허용
        command.add("-ss");
        command.add(String.valueOf(startSeconds)); // 시작 시간 (초)
        command.add("-i");
        command.add(inputPath);                 // 입력 파일
        command.add("-t");
        command.add(String.valueOf(durationSeconds)); // 잘라낼 길이 (초)
        command.add("-c");
        command.add("copy");                    // 재인코딩 없이 스트림 복사 (매우 빠름)
        command.add(outputPath);                // 출력 파일

        try {
            execute(command, 30);
        } catch (Exception ex) {
            log.error("FFmpeg 영상 분할 실패: {}", inputPath);
        }
    }

    /**
     * FFmpeg 명령어를 실행하는 메서드
     * timeoutSecond로 타임아웃 시간을 설정할 수 있음.
     * @param command 실행할 명령어
     * @param timeoutSecond 타임아웃 시간 (초)
     * @return 실행 결과 문자열
     */
    private String execute(
            final List<String> command,
            final int timeoutSecond
    ) {
        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        try {
            final Process p = pb.start();

            String result;
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                result = reader.lines().collect(Collectors.joining("\n"));
            }

            if (!p.waitFor(timeoutSecond, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                log.error("[FFmpeg 타임아웃] - 명령어: {}", command);
                throw new CustomException(ExceptionCode.COMMAND_TIMEOUT);
            }

            return result;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CustomException(ExceptionCode.PROCESS_INTERRUPTED);
        } catch (final Exception ex) {
            log.error("[FFmpeg 에러 발생] {}", ex.getMessage());
            throw new CustomException(ExceptionCode.FFMPEG_ERROR, ex);
        }
    }
}
