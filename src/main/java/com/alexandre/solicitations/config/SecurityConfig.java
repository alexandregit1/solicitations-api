package com.alexandre.solicitations.config;

import com.alexandre.solicitations.model.enums.Role;
import com.alexandre.solicitations.security.AuthenticatedUser;
import com.alexandre.solicitations.security.SecurityErrorHandler;
import com.alexandre.solicitations.security.TokenService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Role checks by route. Owner and UF coverage depend on the resource, so they live in the services. */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login")
                        .permitAll()
                        .requestMatchers("/admin/**", "/actuator/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/analyst/**")
                        .hasAnyRole("ANALYST", "ADMIN")
                        .requestMatchers("/solicitations/**")
                        .hasRole("CLIENT")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(
                        oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(SecurityConfig::toAuthentication))
                                .authenticationEntryPoint(errorHandler))
                .exceptionHandling(e -> e.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
                .build();
    }

    // The principal becomes an AuthenticatedUser and the "role" claim the authority ROLE_<role>
    private static UsernamePasswordAuthenticationToken toAuthentication(Jwt jwt) {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString(TokenService.ROLE_CLAIM)));
        return new UsernamePasswordAuthenticationToken(
                user, jwt, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
    }

    @Bean
    SecretKey jwtKey(@Value("${jwt.secret}") String secret) {
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must have at least 32 characters");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return NimbusJwtEncoder.withSecretKey(jwtKey).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
