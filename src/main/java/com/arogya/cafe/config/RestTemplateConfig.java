package com.arogya.cafe.config;

import java.net.http.HttpClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Force HTTP/1.1. By default the JDK HttpClient advertises an HTTP/2 cleartext (h2c)
        // upgrade — "Connection: Upgrade, HTTP2-Settings" + "Upgrade: h2c" — which the bill
        // scanner's server (uvicorn/httptools) cannot handle; it corrupts the multipart upload
        // and the scan fails with 422. Pinning HTTP/1.1 keeps the forwarded request well-formed.
        HttpClient httpClient =
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        return builder.requestFactory(() -> new JdkClientHttpRequestFactory(httpClient))
                .build();
    }
}
