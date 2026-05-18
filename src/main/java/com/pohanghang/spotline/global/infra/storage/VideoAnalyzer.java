package com.pohanghang.spotline.global.infra.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

public class VideoAnalyzer {

    private static final Set<String> MP4_BRANDS = Set.of(
            "mp41", "mp42", "isom", "iso2", "avc1", "dash", "mmp4", "MSNV", "NDSC", "NDSH"
    );

    // 매직 넘버 검사
    public static boolean isValidMp4(final Path path) {

        if (path == null || !path.endsWith(".mp4")) {
            return false;
        }

        final File file = path.toFile();

        if (!file.exists() || !file.isFile() || file.length() < 16) {
            return false;
        }

        // 첫 16바이트 읽어서 매직넘버 검사
        byte[] header = new byte[16];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(header);
            if (bytesRead < 16) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        }

        // 1. 4~7번째 바이트가 "ftyp" 문자열인지 검증
        String ftypMarker = new String(header, 4, 4, StandardCharsets.US_ASCII);
        if (!"ftyp".equals(ftypMarker)) {
            return false;
        }

        // 2. 8~11번째 바이트(주요 브랜드)가 알려진 MP4 규격에 포함되는지 검증
        String majorBrand = new String(header, 8, 4, StandardCharsets.US_ASCII).trim();
        if (MP4_BRANDS.contains(majorBrand)) {
            return true;
        }

        // 3. 12~15번째 바이트(부가 브랜드)까지 추가로 호환성 체크
        String minorBrand = new String(header, 12, 4, StandardCharsets.US_ASCII).trim();
        return MP4_BRANDS.contains(minorBrand);
    }
}
