package com.code.back_end.config;

import com.code.back_end.util.JwtUtil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(
            JwtUtil jwtUtil
    ) {
        this.jwtUtil = jwtUtil;
    }

    // =====================
    // PASSWORD ENCODER
    // =====================
    @Bean
    public PasswordEncoder
    passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    // =====================
    // JWT FILTER
    // =====================
    @Bean
    public JwtAuthFilter jwtAuthFilter(
            UserDetailsService
                    userDetailsService
    ) {

        return new JwtAuthFilter(
                jwtUtil,
                userDetailsService
        );
    }

    // =====================
    // SECURITY
    // =====================
    @Bean
    public SecurityFilterChain
    securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter filter
    ) throws Exception {

        http

                .csrf(csrf ->
                        csrf.disable()
                )

                .cors(
                        Customizer
                                .withDefaults()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy
                                        .STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // PUBLIC APIs
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register"
                        ).permitAll()

                        .requestMatchers(
        HttpMethod.PUT,
        "/api/auth/update-role/**"
).permitAll()

                        .requestMatchers(
                                "/api/auth/me"
                        ).authenticated()

                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        .requestMatchers(
                                "/uploads/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/reports/**",
                                "/api/ai/reports/**",
                                "/api/ai/notifications/**",
                                "/api/audit-logs/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "TREASURER"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/stalls/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "TREASURER"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/stalls/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "TREASURER"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/contracts/**",
                                "/api/occupants/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "TREASURER",
                                "SUPERVISOR",
                                "MARKETSUPERVISOR",
                                "MARKET_SUPERVISOR",
                                "BPLO",
                                "BPLOOFFICE",
                                "BPLO_OFFICE",
                                "ENDORSINGOFFICE",
                                "ENDORSING_OFFICE",
                                "ENDORSING_OFFICER",
                                "ENDORISING_OFFICE"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/contracts/**",
                                "/api/occupants/**",
                                "/api/stakeholders/*/market-supervisor",
                                "/api/stakeholders/*/market-approve",
                                "/api/stakeholders/*/market-reject",
                                "/api/stakeholders/*/bplo",
                                "/api/stakeholders/*/bplo-approve",
                                "/api/stakeholders/*/bplo-reject",
                                "/api/stakeholders/*/endorsing",
                                "/api/stakeholders/*/endorse",
                                "/api/stakeholders/*/endorse-reject",
                                "/api/stakeholders/*/pay-applicant-fee",
                                "/api/stakeholders/*/approve",
                                "/api/stakeholders/*/reject",
                                "/api/stakeholders/*/treasurer-approve",
                                "/api/stakeholders/*/assign-stall",
                                "/api/stakeholders/*/bplo-approve-workflow",
                                "/api/stakeholders/*/final-endorse",
                                "/api/stakeholders/*/applicant-fee",
                                "/api/applications/*/approve",
                                "/api/applications/*/reject",
                                "/api/applications/*/endorse",
                                "/api/applications/*/endorse-reject",
                                "/api/applications/*/bplo-approve",
                                "/api/applications/*/bplo-reject"
                        ).hasAnyRole(
                                "ADMIN",
                                "TREASURER",
                                "SUPERVISOR",
                                "MARKETSUPERVISOR",
                                "MARKET_SUPERVISOR",
                                "BPLO",
                                "BPLOOFFICE",
                                "BPLO_OFFICE",
                                "ENDORSINGOFFICE",
                                "ENDORSING_OFFICE",
                                "ENDORSING_OFFICER",
                                "ENDORISING_OFFICE"
                        )

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "TREASURER"
                        )

                        .requestMatchers(
                                "/api/payments/**",
                                "/api/billings/**",
                                "/api/notifications/**",
                                "/api/applications/**",
                                "/api/stakeholders/**",
                                "/api/contracts/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/stalls/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/occupants/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "TREASURER",
                                "SUPERVISOR",
                                "MARKETSUPERVISOR",
                                "MARKET_SUPERVISOR",
                                "BPLO",
                                "BPLOOFFICE",
                                "BPLO_OFFICE",
                                "ENDORSINGOFFICE",
                                "ENDORSING_OFFICE",
                                "ENDORSING_OFFICER",
                                "ENDORISING_OFFICE"
                        )

                        // EVERYTHING ELSE
                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        filter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
