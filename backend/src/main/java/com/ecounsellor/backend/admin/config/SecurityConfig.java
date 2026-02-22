package com.ecounsellor.backend.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * UPDATED SecurityConfig — adds student auth endpoints to permitted list.
 * REPLACE your existing SecurityConfig.java with this file.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize on controllers
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // ── Public endpoints (no token needed) ────────────────────
                .requestMatchers("/auth/**").permitAll()                     // admin login
                .requestMatchers("/api/student/auth/**").permitAll()         // student register + login
                .requestMatchers("/api/student/predict").permitAll()         // college search (public)
                .requestMatchers("/api/college/**").permitAll()              // college info (public)
                .requestMatchers("/api/counselling/event/**").permitAll()    // view/shortlist events
                .requestMatchers("/api/counselling/test").permitAll()

                // ── Admin-only endpoints ───────────────────────────────────
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ── Student-only endpoints ─────────────────────────────────
                .requestMatchers("/api/student/me").hasRole("STUDENT")
                .requestMatchers("/api/student/me/**").hasRole("STUDENT")

                // ── College counselling dashboard (any authenticated) ──────
                .requestMatchers("/api/counselling/**").authenticated()

                // ── Everything else: permit ────────────────────────────────
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
