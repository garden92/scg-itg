package com.kt.kol.gateway.itg.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
        return http
                .httpBasic(Customizer.withDefaults())
                .csrf((csrf) -> csrf.requireCsrfProtectionMatcher(
                    new NegatedServerWebExchangeMatcher(
                        ServerWebExchangeMatchers.pathMatchers("/**")
                    )
                ))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/**").authenticated()
                        .anyExchange().permitAll())
                .build();
    }
}
