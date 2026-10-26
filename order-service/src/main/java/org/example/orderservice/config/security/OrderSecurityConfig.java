package org.example.orderservice.config.security;

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
 * Defines API security for the Order API.
 */
@Configuration
@EnableMethodSecurity
public class OrderSecurityConfig {

    /**
     * Requires appropriate OAuth scopes for Order operations
     */
    @Bean
    SecurityFilterChain orderSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders")
                        .hasAuthority("SCOPE_orders.write")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/**")
                        .hasAuthority("SCOPE_orders.read")
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}
