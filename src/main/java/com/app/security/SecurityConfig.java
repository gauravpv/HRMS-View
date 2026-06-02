package com.app.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.app.repository.RoleRepository;
import com.app.repository.UserRepository;
import com.app.security.handler.CustomAuthenticationSuccessHandler;
import com.app.security.handler.FormLoginAuthenticationSuccessHandler;
import com.app.service.impl.UserSessionServiceImpl;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LogManager.getLogger(SecurityConfig.class);

    @Value("${app.auth.azure.enabled:false}")
    private boolean azureEnabled;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    private FormLoginAuthenticationSuccessHandler formLoginAuthenticationSuccessHandler;

    @Autowired
    private HrmsUserDetailsService hrmsUserDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserSessionServiceImpl userSessionService;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(hrmsUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(request -> request
                .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/vendor/**").permitAll()
                .requestMatchers(
                        "/error", "/access-denied",
                        "/not-found", "/notfound",
                        "/logged-in", "/loggedin",
                        "/session-expired")
                    .permitAll()
                .requestMatchers("/").hasAnyAuthority("ADMIN", "EDITOR", "USER")
                .requestMatchers("/index").hasAnyAuthority("ADMIN", "EDITOR", "USER")
                .requestMatchers(
                        "/data-management", "/dataManagement",
                        "/search-data", "/searchData",
                        "/history-data", "/historyData",
                        "/table-status", "/tableStatus")
                    .hasAnyAuthority("ADMIN", "EDITOR", "USER")
                .requestMatchers("/data-upload", "/dataUpload").hasAnyAuthority("ADMIN", "EDITOR")
                .requestMatchers(
                        "/data-movement", "/dataMovement",
                        "/move-to-master", "/moveToMaster",
                        "/move-to-main", "/moveToMain")
                    .hasAnyAuthority("ADMIN", "EDITOR")
                .requestMatchers(
                        "/user-management", "/userManagement",
                        "/users", "/active", "/inactive", "/inActive")
                    .hasAuthority("ADMIN")
                .requestMatchers("/deactivate", "/deActive", "/activate").hasAuthority("ADMIN")
                .requestMatchers("/api/user/**").hasAnyAuthority("ADMIN", "EDITOR", "USER")
                .anyRequest().authenticated());

        if (azureEnabled) {
            logger.info("Security: Azure AD OAuth2 login enabled");
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(new AzureOidcUserService(userRepository, roleRepository)))
                .successHandler(customAuthenticationSuccessHandler));
        } else {
            logger.info("Security: form login enabled");
            http
                .authenticationProvider(daoAuthenticationProvider())
                .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .successHandler(formLoginAuthenticationSuccessHandler)
                    .failureHandler((request, response, exception) -> {
                        logger.warn("Form login failed from {}: {}", request.getRemoteAddr(), exception.getMessage());
                        response.sendRedirect(request.getContextPath() + "/login?error=true");
                    })
                    .permitAll());
        }

        http
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    String principal = authentication != null ? authentication.getName() : "anonymous";
                    logger.debug("Logout user={} remote={}", principal, request.getRemoteAddr());
                    if (authentication != null) {
                        userSessionService.setSessionInactiveOnLogout(authentication.getName());
                    }
                    if (request.getSession() != null) {
                        request.getSession().invalidate();
                    }
                    response.sendRedirect("/login?logout=true");
                })
                .deleteCookies("JSESSIONID")
                .permitAll())
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
                .sessionRegistry(sessionRegistry)
                .expiredUrl("/session-expired"));

        return http.build();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
