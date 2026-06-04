package com.pohanghang.spotline.domain.video;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

@Component
public class VideoStreamSink {

    // onBackpressureBuffer: 구독자 demand 타이밍과 무관하게 버퍼링 후 전달 (emit 드롭 방지)
    // autoCancel=false: 구독자가 0이 돼도(프론트 연결 종료) sink를 종료하지 않아 재연결 시 계속 전달
    private final Sinks.Many<byte[]> sink =
            Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

    public void emit(final byte[] chunk) {
        sink.tryEmitNext(chunk);
    }

    public Flux<byte[]> flux() {
        return sink.asFlux();
    }
}
