package com.roborock.notify.config;

import com.roborock.repo.config.CommonRepoConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan("com.roborock.notify")
@EnableScheduling
@Import({CommonRepoConfig.class})
public class NotifyConfig {
}
