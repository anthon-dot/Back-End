package com.code.back_end.config;

import com.code.back_end.enums.Role;
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

    private static final String ROLE_ADMIN = Role.ROLE_ADMIN.roleWithoutPrefix();
    private static final String ROLE_MARKET_SUPERVISOR = Role.ROLE_MARKET_SUPERVISOR.roleWithoutPrefix();
    private static final String ROLE_BPLO = Role.ROLE_BPLO.roleWithoutPrefix();
    private static final String ROLE_ENDORSEMENT_OFFICE = Role.ROLE_ENDORSEMENT_OFFICE.roleWithoutPrefix();

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(UserDetailsService userDetailsService) {
        return new JwtAuthFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter filter,
            AuthRateLimitFilter authRateLimitFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
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
                        ).hasAnyRole(ROLE_ADMIN)

                        .requestMatchers(
                                "/api/auth/me"
                        ).authenticated()

                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/health"
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
                                ROLE_ADMIN,
                                "TREASURER"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/stalls/**"
                        ).hasAnyRole(
                                ROLE_ADMIN,
                                "TREASURER"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/stalls/**"
                        ).hasAnyRole(
                                ROLE_ADMIN,
                                "TREASURER"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/contracts/**",
                                "/api/occupants/**"
                        ).hasAnyRole(
                                ROLE_ADMIN,
                                "TREASURER",
                                "SUPERVISOR",
                                "MARKETSUPERVISOR",
                                ROLE_MARKET_SUPERVISOR,
                                ROLE_BPLO,
                                "BPLOOFFICE",
                                "BPLO_OFFICE",
                                "ENDORSINGOFFICE",
                                "ENDORSING_OFFICE",
                                "ENDORSING_OFFICER",
                                ROLE_ENDORSEMENT_OFFICE
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/contracts/**",
                                "/api/occupants/**",
                                "/api/stakeholders/*/market-approve",
                                "/api/stakeholders/*/market-reject",
                                "/api/stakeholders/*/bplo-approve",
                                "/api/stakeholders/*/bplo-reject",
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
                                ROLE_ADMIN,
                                "TREASURER",
                                "SUPERVISOR",
                                "MARKETSUPERVISOR",
                                ROLE_MARKET_SUPERVISOR,
                                ROLE_BPLO,
                                "BPLOOFFICE",
                                "BPLO_OFFICE",
                                "ENDORSINGOFFICE",
                                "ENDORSING_OFFICE",
                                "ENDORSING_OFFICER",
                                ROLE_ENDORSEMENT_OFFICE
                        )

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/**"
                        ).hasAnyRole(
                                ROLE_ADMIN,
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
                                ROLE_ADMIN,
                                "TREASURER",
                                "SUPERVISOR",
                                "MARKETSUPERVISOR",
                                ROLE_MARKET_SUPERVISOR,
                                ROLE_BPLO,
                                "BPLOOFFICE",
                                "BPLO_OFFICE",
                                "ENDORSINGOFFICE",
                                "ENDORSING_OFFICE",
                                "ENDORSING_OFFICER",
                                ROLE_ENDORSEMENT_OFFICE
                        )

                        // EVERYTHING ELSE
                        .anyRequest()
                        .authenticated()
                )
                .addFilterBefore(
                        authRateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        filter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}


