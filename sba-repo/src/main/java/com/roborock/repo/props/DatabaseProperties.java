package com.roborock.repo.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("common.repo.database")
@PropertySource(value = "classpath:common/repo/database.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file://${CONFIG_HOME}/common/repo/database.properties", ignoreResourceNotFound = true)
public class DatabaseProperties {

    private String driverClassName;

    private String dbUrl;

    private String username;

    private String password;

}