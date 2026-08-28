package com.wesleyedwards.ServiceLink.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Comma-separated origins allowed to call the API cross-origin.
     *
     * Empty in the deployed setup on purpose: the Ingress serves the frontend
     * and the API from one host (/ and /api), so the browser never makes a
     * cross-origin request and nothing needs allowing. The dev default covers
     * `ng serve` on 4200 talking to Boot on 8080.
     */
    @Value("${servicelink.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // add this
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Kubernetes probes cannot authenticate. Only the health
                        // groups are open, and management.endpoint.health.show-details
                        // is `never`, so the body is just {"status":"UP"} — no
                        // component or version detail leaks.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/users/auth/**").permitAll()
                        // tickets — specific method rules first, broad (PUT/PATCH/DELETE) last
                        .requestMatchers(HttpMethod.GET, "/api/tickets/**").hasAnyRole("ADMIN", "AGENT", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/**").hasAnyRole("ADMIN", "AGENT", "USER")
                        .requestMatchers("/api/tickets/**").hasAnyRole("ADMIN", "AGENT")
                        // comments — specific method rules first, broad last
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").hasAnyRole("ADMIN", "AGENT", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").hasAnyRole("ADMIN", "AGENT", "USER")
                        // USERs may PUT (edit) — the service enforces author + edit-window; DELETE stays staff-only
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").hasAnyRole("ADMIN", "AGENT", "USER")
                        .requestMatchers("/api/comments/**").hasAnyRole("ADMIN", "AGENT")
                        // attachments — the service enforces ticket ownership and the internal-comment rule
                        .requestMatchers(HttpMethod.GET, "/api/attachments/**").hasAnyRole("ADMIN", "AGENT", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/attachments/**").hasAnyRole("ADMIN", "AGENT", "USER")
                        .requestMatchers("/api/attachments/**").hasAnyRole("ADMIN", "AGENT")
                        // users
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
