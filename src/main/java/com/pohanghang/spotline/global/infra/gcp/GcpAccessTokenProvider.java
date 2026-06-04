package com.pohanghang.spotline.global.infra.gcp;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ADC(Application Default Credentials) 기반 액세스 토큰 제공자.
 * GeminiClient와 동일한 방식이지만 자격증명을 1회만 로드해 캐시한다.
 * 사이드채널 제품이 켜져 실제 호출이 일어날 때만 lazy 로드되므로,
 * 전부 off인 기본 상태에선 ADC가 없어도 앱이 정상 기동한다.
 */
@Component
public class GcpAccessTokenProvider {

    private static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private volatile GoogleCredentials credentials;

    public String getToken() {
        try {
            GoogleCredentials current = credentials;
            if (current == null) {
                synchronized (this) {
                    current = credentials;
                    if (current == null) {
                        current = GoogleCredentials.getApplicationDefault().createScoped(SCOPE);
                        credentials = current;
                    }
                }
            }
            current.refreshIfExpired();
            return current.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new RuntimeException("GCP 액세스 토큰 획득 실패", e);
        }
    }
}
