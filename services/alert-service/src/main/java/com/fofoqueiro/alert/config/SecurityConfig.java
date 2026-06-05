// @path services/alert-service/src/main/java/com/fofoqueiro/alert/config/SecurityConfig.java
// @owner alert-service
// @responsibility Phase 1 placeholder — Phase 2 adiciona JWT + WebSocket auth no handshake
// @see docs/ARCHITECTURE.md#alert-service (ADR-008)
package com.fofoqueiro.alert.config;

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
                .requestMatchers("/actuator/**", "/ws/**").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
