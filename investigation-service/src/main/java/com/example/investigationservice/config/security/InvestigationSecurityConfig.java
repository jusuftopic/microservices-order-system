package com.example.investigationservice.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Defines API security for the Investigation API.
 */
@Configuration
@EnableMethodSecurity
public class InvestigationSecurityConfig {

    /**
     * Requires appropriate OAuth scopes for Investigation operations
     */
    @Bean
    SecurityFilterChain investigationSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/investigations/orders/**")
                        .hasAuthority("SCOPE_investigations.read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/investigations/orders/**")
                        .hasAuthority("SCOPE_investigations.ask")
                        .requestMatchers(HttpMethod.POST, "/api/v1/investigations/questions/**")
                        .hasAuthority("SCOPE_investigations.feedback")
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}
