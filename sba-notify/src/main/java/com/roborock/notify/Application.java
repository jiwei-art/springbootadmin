package com.roborock.notify;

import com.roborock.notify.config.NotifyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableAutoConfiguration
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Environment env = new SpringApplicationBuilder(NotifyConfig.class).bannerMode(Banner.Mode.OFF).run(args).getEnvironment();
        logger.info("\n" +
                        "----------------------------------------------------------" +
                        "\n\tSBA notify is running! Access URLs:" +
                        "\n\tLocal: \t\thttp://localhost:{}\n" +
                        "----------------------------------------------------------",
                env.getProperty("server.port"));
    }

}