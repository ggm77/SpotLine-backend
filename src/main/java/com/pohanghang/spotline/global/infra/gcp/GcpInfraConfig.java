package com.pohanghang.spotline.global.infra.gcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
public class GcpInfraConfig {

    /**
     * 사이드채널 전용 풀. 포화 시 DiscardPolicy로 작업을 그냥 버려서
     * 호출(요청) 스레드로 예외가 튀지 않게 한다 → 원래 요청 처리에 영향 0.
     */
    @Bean(name = "gcpSideChannelExecutor")
    public Executor gcpSideChannelExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("gcp-sidechannel-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}
