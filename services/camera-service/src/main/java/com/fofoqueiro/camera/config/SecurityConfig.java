// @path services/camera-service/src/main/java/com/fofoqueiro/camera/config/SecurityConfig.java
// @owner camera-service
// @responsibility Phase 1 placeholder — Phase 2 adiciona JWT + proteção do webhook /internal/media/auth
// @see docs/ARCHITECTURE.md#api-gateway (ADR-001) | docs/SDD.md#webhook-auth-mediamtx
package com.fofoqueiro.camera.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                // /internal/media/auth será protegido por X-Internal-Secret em Phase 4
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
