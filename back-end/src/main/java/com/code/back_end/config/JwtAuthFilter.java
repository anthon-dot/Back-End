package com.code.back_end.config;

import com.code.back_end.util.JwtUtil;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthFilter
        extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService
            userDetailsService;

    public JwtAuthFilter(
            JwtUtil jwtUtil,
            UserDetailsService
                    userDetailsService
    ) {

        this.jwtUtil = jwtUtil;
        this.userDetailsService =
                userDetailsService;
    }

    // SKIP JWT FOR PUBLIC ROUTES
    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path =
                request.getServletPath();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        return (
        "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || path.startsWith("/api/auth/update-role")
)
                || path.startsWith(
                "/swagger-ui"
        )
                || path.startsWith(
                "/v3/api-docs"
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException,
            IOException {

        String header =
                request.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (
                header == null ||
                !header.startsWith("Bearer ")
        ) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing bearer token"
            );
            return;
        }

        String token =
                header.substring(7);

        try {
            String username =
                    jwtUtil.extractUsername(token);

            if (
                    username != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null
            ) {
                var userDetails =
                        userDetailsService
                                .loadUserByUsername(
                                        username
                                );

                if (
                        jwtUtil.validateToken(
                                token,
                                userDetails
                        )
                ) {
                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authentication
                            );
                }
            }
        } catch (JwtException | IllegalArgumentException error) {
            SecurityContextHolder.clearContext();
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token"
            );
            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}
