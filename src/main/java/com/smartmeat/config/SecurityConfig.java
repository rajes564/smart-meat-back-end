package com.smartmeat.config;

import com.smartmeat.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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

import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final JwtAuthFilter jwtAuthFilter;
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        return http
//            .csrf(c -> c.disable())
//            .cors(c -> c.configurationSource(corsConfigurationSource()))
//            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//            .authorizeHttpRequests(auth -> auth
//                // Public endpoints
//                .requestMatchers("/api/auth/**").permitAll()
//                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
//                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
//                .requestMatchers(HttpMethod.GET, "/api/shop/**").permitAll()
//                .requestMatchers(HttpMethod.GET, "/api/reviews/public").permitAll()
//                .requestMatchers("/uploads/**").permitAll()
//                .requestMatchers("/api/sse/**").permitAll()
//
//                // Customer endpoints
//                .requestMatchers("/api/orders/my/**").hasAnyRole("CUSTOMER","ADMIN")
//             //   .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("CUSTOMER","ADMIN","SELLER")
//            //    .requestMatchers("/api/reviews/submit").hasAnyRole("CUSTOMER","ADMIN")
//                
//                
//                .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
//                .requestMatchers(HttpMethod.GET,  "/api/orders/*").permitAll()   // order tracking page too
//                .requestMatchers(HttpMethod.POST, "/api/reviews/submit").permitAll()
//
//                // Seller endpoints — ONLY new sale
//                .requestMatchers(HttpMethod.POST, "/api/sales").hasAnyRole("SELLER","ADMIN")
//                .requestMatchers(HttpMethod.GET, "/api/sales/today-summary").hasAnyRole("SELLER","ADMIN")
//
//                // Admin-only endpoints
//                .requestMatchers("/api/admin/**").hasRole("ADMIN")
//                .requestMatchers("/api/khata/**").hasRole("ADMIN")
//                .requestMatchers("/api/inventory/**").hasRole("ADMIN")
//                .requestMatchers("/api/expenses/**").hasRole("ADMIN")
//                .requestMatchers("/api/reports/**").hasRole("ADMIN")
//                .requestMatchers("/api/users/**").hasRole("ADMIN")
//                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
//                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
//                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
//
//                .anyRequest().authenticated()
//            )
//            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
//            .build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOriginPatterns(List.of("*"));
//        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
//        config.setAllowedHeaders(List.of("*"));
//        config.setAllowCredentials(true);
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder(12);
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//        return config.getAuthenticationManager();
//    }
//}





@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(c -> c.disable())
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ── Fully public — no token required ──────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/shop/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/public").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/sse/**").permitAll()
                .requestMatchers("/api/payment/**").permitAll()

                // Guest customers can place orders and submit reviews without login
                .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/orders/*").permitAll()  // order status tracking
                .requestMatchers(HttpMethod.POST, "/api/reviews/submit").permitAll()

                // ── Authenticated customer endpoints ───────────────────────────
                .requestMatchers("/api/orders/my/**").hasAnyRole("CUSTOMER","ADMIN")

                // ── Seller endpoints — new sale only ───────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/sales").hasAnyRole("SELLER","ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/sales/today-summary").hasAnyRole("SELLER","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/orders/pos").hasAnyRole("SELLER","ADMIN")

                // ── Admin-only endpoints ───────────────────────────────────────
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/khata/**").hasRole("ADMIN")
                .requestMatchers("/api/inventory/**").hasRole("ADMIN")
                .requestMatchers("/api/suppliers/**").hasRole("ADMIN")
                .requestMatchers("/api/expenses/**").hasRole("ADMIN")
                .requestMatchers("/api/reports/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/orders").hasAnyRole("ADMIN","SELLER")
                .requestMatchers(HttpMethod.PATCH,  "/api/orders/**").hasAnyRole("ADMIN","SELLER")
                .requestMatchers(HttpMethod.POST,   "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
