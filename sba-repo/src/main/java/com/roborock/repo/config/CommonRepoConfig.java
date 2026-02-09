package com.roborock.repo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan("com.roborock.repo")
@EnableConfigurationProperties
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CommonRepoConfig {
}
