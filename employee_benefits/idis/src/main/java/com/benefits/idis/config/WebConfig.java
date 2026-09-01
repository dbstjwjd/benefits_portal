package com.benefits.idis.config;

import com.benefits.idis.admin.AdminInterceptor;
import com.benefits.idis.admin.FormImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;
    private final FormImageService formImageService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin", "/admin/**");
    }

    /**
     * 선택지 이미지는 jar 밖 디스크에 두고 여기서 읽어준다.
     * 응답자도 봐야 하므로 관리자 인터셉터가 걸리지 않는 경로에 붙인다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 파일명이 UUID 라 같은 주소의 내용이 바뀌지 않는다. 길게 캐시해도 안전하다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(formImageService.root().toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).immutable().cachePublic());
    }
}
