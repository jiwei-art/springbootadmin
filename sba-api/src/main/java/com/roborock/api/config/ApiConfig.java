package com.roborock.api.config;

import com.roborock.repo.config.CommonRepoConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("com.roborock.api")
@Import({CommonRepoConfig.class})
public class ApiConfig {
}
