package com.greenmiles.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.greenmiles.backend.security.AuthRateLimitFilter;
import com.greenmiles.backend.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthRateLimitFilter authRateLimitFilter,
            @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://a41.03e.mytemp.website,https://a41.03e.mytemp.website}") List<String> allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authRateLimitFilter = authRateLimitFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/driver/login",
                                "/api/v1/auth/admin/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/health",
                                "/actuator/**")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/logout")
                        .authenticated()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/security/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/drivers/**")
                        .hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers("/api/v1/bookings/**", "/api/v1/notifications/**")
                        .hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/v1/courier/**")
                        .hasAnyRole("USER", "ADMIN")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * GoDaddy / temp hosts often serve the UI on http while Render uses https; browsers treat those as
     * different origins. For each configured origin, also allow the same host over the other scheme
     * (except localhost, which is usually http-only in dev).
     */
    static List<String> expandOriginsWithSchemeMirror(List<String> raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String o : raw) {
            if (o == null) {
                continue;
            }
            String t = o.trim();
            if (t.isEmpty()) {
                continue;
            }
            out.add(t);
            if (t.startsWith("https://")) {
                out.add("http://" + t.substring("https://".length()));
            } else if (t.startsWith("http://")) {
                String rest = t.substring("http://".length());
                if (!rest.startsWith("localhost") && !rest.startsWith("127.0.0.1")) {
                    out.add("https://" + rest);
                }
            }
        }
        return new ArrayList<>(out);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(expandOriginsWithSchemeMirror(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
