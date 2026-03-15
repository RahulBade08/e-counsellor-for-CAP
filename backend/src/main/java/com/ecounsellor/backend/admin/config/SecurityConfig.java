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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * REPLACE the existing SecurityConfig.java with this file.
 *
 * Key change: admin login is now at /api/admin/auth/login (was /auth/login).
 * The old /auth/login is kept as a permitted path for backward compatibility
 * during the transition period — you can remove it once all clients are updated.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // ── PUBLIC — no token needed ───────────────────────────────────
                // Legacy admin login path (keep for backward compat)
                .requestMatchers("/auth/**").permitAll()

                // New admin login path
                .requestMatchers("/api/admin/auth/login").permitAll()

                // Student auth
                .requestMatchers("/api/student/auth/**").permitAll()
                .requestMatchers("/api/student/predict").permitAll()

                // College register & login — public
                .requestMatchers("/api/college/auth/register").permitAll()
                .requestMatchers("/api/college/auth/login").permitAll()

                // Android app event tracking — no auth needed
                .requestMatchers("/api/counselling/event/**").permitAll()
                .requestMatchers("/api/counselling/test").permitAll()

                // ── ADMIN ONLY ─────────────────────────────────────────────────
                // All /api/admin/** except the public login above
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ── STUDENT ONLY ───────────────────────────────────────────────
                .requestMatchers("/api/student/me").hasRole("STUDENT")
                .requestMatchers("/api/student/me/**").hasRole("STUDENT")

                // ── COLLEGE ONLY ───────────────────────────────────────────────
                .requestMatchers("/api/college/auth/me").hasRole("COLLEGE")
                .requestMatchers("/api/counselling/**").hasRole("COLLEGE")

                // ── EVERYTHING ELSE ────────────────────────────────────────────
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        ));

        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
