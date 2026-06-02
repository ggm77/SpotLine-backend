package com.pohanghang.spotline.domain.video;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class VideoStreamSink {

    // directBestEffort: 구독자 없으면 프레임 드롭 (라이브 스트림에 적합)
    private final Sinks.Many<byte[]> sink = Sinks.many().multicast().directBestEffort();

    public void emit(final byte[] chunk) {
        sink.tryEmitNext(chunk);
    }

    public Flux<byte[]> flux() {
        return sink.asFlux();
    }
}
