package com.pohanghang.spotline.domain.video;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class VideoStreamSink {

    // onBackpressureBuffer: 구독자 demand 타이밍과 무관하게 버퍼링 후 전달 (emit 드롭 방지)
    private final Sinks.Many<byte[]> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void emit(final byte[] chunk) {
        sink.tryEmitNext(chunk);
    }

    public Flux<byte[]> flux() {
        return sink.asFlux();
    }
}
