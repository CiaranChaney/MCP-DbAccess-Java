package com.ciaranchaney.mcpdb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/mcp/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/mcp/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/mcp/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/mcp").permitAll() // JSON-RPC initialize
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}

