package com.code_space.code_space.config;

import com.code_space.code_space.security.AuthEntryPointJwt;
import com.code_space.code_space.security.AuthTokenFilter;
import com.code_space.code_space.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Autowired
    UserService userService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeHttpRequests(authz -> authz
                        // OPTIONS requests should be allowed for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Auth endpoints (public)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Swagger documentation (public)
                        .requestMatchers("/docs/**", "/api-docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Test endpoints (public)
                        .requestMatchers("/api/test/**").permitAll()

                        // WebSocket endpoint (public)
                        .requestMatchers("/ws/**").permitAll()

                        // Specific public room endpoints (ORDER MATTERS - more specific first)
                        .requestMatchers(HttpMethod.GET, "/api/rooms/code/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rooms/invitation/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/rooms/join").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/rooms/join/invitation/*").authenticated()

                        // All other room endpoints require authentication
                        .requestMatchers("/api/rooms/**").authenticated()

                        .requestMatchers("/api/webrtc/**").authenticated()


                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins or use allowedOriginPatterns for patterns
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Allow all common HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all headers that might be sent
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (important for JWT in headers)
        configuration.setAllowCredentials(true);

        // Expose headers that the client can access
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}