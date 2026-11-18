package com.ganzithon.pulse.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** 그림 이미지의 절대 URL을 만든다. 설정이 없으면 요청을 받은 서버 주소를 쓴다 */
@Component
public class PublicUrlResolver {
    private final String publicBaseUrl;

    public PublicUrlResolver(@Value("${pulse.public-base-url:}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String pictureUrl(String image) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/")
                    ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            return base + "/pictures/" + image;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/pictures/{image}").buildAndExpand(image).toUriString();
    }
}
