package com.roborock.sbadminserver.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminServerProperties adminServer;

    public SecurityConfig(AdminServerProperties adminServer) {
        this.adminServer = adminServer;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = 
            new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminServer.path("/"));

        http
            .authorizeHttpRequests(authorize -> authorize
                // 允许访问静态资源
                .requestMatchers(
                    this.adminServer.path("/assets/**"),
                    this.adminServer.path("/actuator/info"),
                    this.adminServer.path("/actuator/health"),
                    this.adminServer.path("/login")
                ).permitAll()
                // 允许 instances 端点（用于客户端注册）
                .requestMatchers(HttpMethod.POST, this.adminServer.path("/instances")).permitAll()
                .requestMatchers(HttpMethod.DELETE, this.adminServer.path("/instances/*")).permitAll()
                // 允许 actuator 端点
                .requestMatchers("/actuator/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage(this.adminServer.path("/login"))
                .successHandler(successHandler)
            )
            .logout(logout -> logout
                .logoutUrl(this.adminServer.path("/logout"))
            )
            .httpBasic(httpBasic -> {})
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    this.adminServer.path("/instances"),
                    this.adminServer.path("/instances/*"),
                    this.adminServer.path("/actuator/**")
                )
            );

        return http.build();
    }
}
