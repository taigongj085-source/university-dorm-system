package com.campus.dorm.config;

import cn.hutool.json.JSONUtil;
import com.campus.dorm.common.Result;
import com.campus.dorm.security.DormUserDetails;
import com.campus.dorm.security.Md5PasswordEncoder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Md5PasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/index", "/notices", "/notices/**", "/guide",
                                "/login", "/register", "/doRegister", "/doLogin").permitAll()
                        .requestMatchers("/admin/login", "/admin/doLogin").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/my-dorm", "/my-dorm/**",
                                "/repair", "/repair/**",
                                "/leave", "/leave/**",
                                "/visitor", "/visitor/**",
                                "/hygiene", "/hygiene/**",
                                "/profile", "/profile/**"
                        ).authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/doLogin")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler())
                        .failureHandler(loginFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/logout"))
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();
                            if (uri != null && uri.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setCharacterEncoding("UTF-8");
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                response.getWriter().write(JSONUtil.toJsonStr(Result.fail("请先登录")));
                                return;
                            }
                            if (uri != null && uri.startsWith("/admin")) {
                                response.sendRedirect("/admin/login");
                                return;
                            }
                            String redirect = uri;
                            String query = request.getQueryString();
                            if (query != null && !query.isBlank()) {
                                redirect = uri + "?" + query;
                            }
                            String encoded = URLEncoder.encode(redirect == null ? "/" : redirect, StandardCharsets.UTF_8);
                            response.sendRedirect("/login?redirect=" + encoded);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String uri = request.getRequestURI();
                            if (uri != null && uri.startsWith("/admin")) {
                                response.sendRedirect("/admin/login");
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                );
        return http.build();
    }

    private AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            Object principal = authentication.getPrincipal();
            String redirect = request.getParameter("redirect");
            if (redirect == null || redirect.isBlank() || !redirect.startsWith("/") || redirect.startsWith("//")) {
                if (principal instanceof DormUserDetails details
                        && details.getUser() != null
                        && "ADMIN".equalsIgnoreCase(details.getUser().getRole())) {
                    redirect = "/admin";
                } else {
                    redirect = "/";
                }
            }
            response.sendRedirect(redirect);
        };
    }

    private AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            String redirect = request.getParameter("redirect");
            if (redirect != null && redirect.startsWith("/admin")) {
                response.sendRedirect("/admin/login?error=1");
                return;
            }
            if (redirect == null || redirect.isBlank()) {
                redirect = "/";
            }
            String encoded = URLEncoder.encode(redirect, StandardCharsets.UTF_8);
            response.sendRedirect("/login?error=1&redirect=" + encoded);
        };
    }
}
