package com.pohanghang.spotline.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
        // 무한 영상 스트림(StreamingResponseBody)이 async 요청 타임아웃에 끊기지 않도록 비활성화
        configurer.setDefaultTimeout(-1);
    }
}
