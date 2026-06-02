package com.app.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.app.model.Users;
import com.app.repository.RoleRepository;
import com.app.repository.UserRepository;
import com.app.security.handler.CustomAuthenticationSuccessHandler;
import com.app.serviceImpl.UserSessionServiceImpl;


@Configuration
@EnableWebSecurity
public class SecurityConfig{
	
	
    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    
    @Autowired
    private RoleRepository roleRepo; 
    
    @Autowired
    private UserRepository userRepo; 
    
    @Autowired
    private UserSessionServiceImpl userSessionService;
    
    @Value("${spring.cloud.azure.active-directory.profile.tenant-id}")
    private String tenantId;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
	    http
    		.csrf(customizer -> customizer.disable())
    		.authorizeHttpRequests((request) -> request
				.requestMatchers("/").hasAnyAuthority("ADMIN","EDITOR","USER")
				.requestMatchers("/index").hasAnyAuthority("ADMIN","EDITOR","USER")
				.requestMatchers("/notfound").hasAnyAuthority("ADMIN","EDITOR","USER")
				.requestMatchers("/session-expired").hasAnyAuthority("ADMIN","EDITOR","USER")
				.requestMatchers("/dataUpload").hasAnyAuthority("ADMIN","EDITOR")
				.requestMatchers("/searchData").hasAnyAuthority("ADMIN","EDITOR","USER")
				.requestMatchers("/historyData").hasAnyAuthority("ADMIN","EDITOR","USER")
				.requestMatchers("/moveToMaster").hasAnyAuthority("ADMIN","EDITOR")
				.requestMatchers("/moveToMain").hasAnyAuthority("ADMIN","EDITOR")
				.requestMatchers("/users").hasAnyAuthority("ADMIN")
				.requestMatchers("/active").hasAnyAuthority("ADMIN")
				.requestMatchers("/inActive").hasAnyAuthority("ADMIN")
				.requestMatchers("/deActive").hasAnyAuthority("ADMIN")
				.requestMatchers("/activate").hasAnyAuthority("ADMIN")
				.requestMatchers("/api/user/**").hasAnyAuthority("ADMIN","EDITOR","USER")
				.requestMatchers("/js/**", "/css/**").permitAll()
				.anyRequest().authenticated())
    		.oauth2Login(oauth2Login -> oauth2Login
                .userInfoEndpoint(userInfoEndpoint -> 
                    userInfoEndpoint
                        .oidcUserService(oidcUserService1())
                )
                .successHandler(customAuthenticationSuccessHandler))
    		.logout(logout -> logout
	            .logoutUrl("/logout")
	            .logoutSuccessHandler((request, response, authentication) -> {
	            	DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
	                String preferredUsername = oidcUser.getAttribute("preferred_username");
	                userSessionService.setSessionInactiveOnLogout(preferredUsername);
	            	//System.out.println(preferredUsername);
	                if (request.getSession() != null) {
	                    request.getSession().invalidate();
	                }
	            })
	            .deleteCookies("JSESSIONID")
	            .permitAll()
	        )
            .sessionManagement(session -> session
                .maximumSessions(1)   // Allow only one active session per user
                .maxSessionsPreventsLogin(true)  // Prevent new login if the max session is reached
                .expiredUrl("/session-expired")  // Redirect to this page if the session is expired
            );
//            .headers(headers -> headers
//            	    .contentSecurityPolicy(csp -> csp
//            	        .policyDirectives("default-src 'self'; script-src 'self' http://localhost:8090 'unsafe-inline'; script-src-elem 'unsafe-inline' http://localhost:8090 https://cdnjs.cloudflare.com https://code.jquery.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:;")
//            	    )
//            );

	    
	    return http.build();
	}

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            var oidcUser = (org.springframework.security.oauth2.core.oidc.user.OidcUser) authentication.getPrincipal();
            String userName = oidcUser.getFullName(); // User's full name
            String email = oidcUser.getPreferredUsername(); // User's email        
            System.out.println("User Name: " + userName);
            System.out.println("User Email: " + email);
            response.sendRedirect("/");
        };
	}

    @Bean
    OidcUserService oidcUserService() {
        OidcUserService oidcUserService = new OidcUserService();
        return oidcUserService;
    }
    
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService1() {
		final OidcUserService delegate = new OidcUserService();

		return (userRequest) -> {
			OidcUser oidcUser = delegate.loadUser(userRequest);
			String email = oidcUser.getPreferredUsername();
			Optional<Users> existingUser = userRepo.findByEmail(email);
			 if (!(existingUser.isEmpty())) {
				    Users userSession = existingUser.get();
					List<String> roles = roleRepo.findRolebyUserId(userSession.getUserId());
			        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
			        
			        for (String role : roles) {
			            authorities.add(new SimpleGrantedAuthority(role));
			        }
			        oidcUser = new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
			 }
			
			return oidcUser;
		};
	}

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}