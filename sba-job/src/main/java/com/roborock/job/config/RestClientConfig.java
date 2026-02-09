package com.roborock.job.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean(name = "restClient")
    @Primary
    public RestClient restClient() {
        var factory = ClientHttpRequestFactoryBuilder
                .detect()
                .build(HttpClientSettings.defaults()
                        .withConnectTimeout(Duration.ofSeconds(10))
                        .withReadTimeout(Duration.ofSeconds(10)));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean
    @Primary
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .configureForJackson2()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
