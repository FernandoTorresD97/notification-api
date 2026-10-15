package com.portfolio.notificationapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import java.util.ArrayList;

@Configuration
public class ApiSecurityConfig {
    @Bean SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable()).cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/channels", "/api/v1/channels/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/notifications").hasAnyRole("ADMIN", "INTEGRATION")
                .requestMatchers("/api/**").hasRole("ADMIN")
                .anyRequest().denyAll())
            .httpBasic(Customizer.withDefaults()).build();
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService apiUsers(PasswordEncoder encoder,
        @Value("${notification.security.admin-username:}") String admin,
        @Value("${notification.security.admin-password:}") String adminPassword,
        @Value("${notification.security.integration-username:}") String integration,
        @Value("${notification.security.integration-password:}") String integrationPassword) {
        var users = new ArrayList<UserDetails>();
        addUser(users, encoder, admin, adminPassword, "ADMIN");
        addUser(users, encoder, integration, integrationPassword, "INTEGRATION");
        if (!admin.isBlank() && admin.equals(integration)) throw new IllegalArgumentException("Admin and integration usernames must differ");
        return new InMemoryUserDetailsManager(users);
    }
    private void addUser(ArrayList<UserDetails> users, PasswordEncoder encoder, String name, String password, String role) {
        if (name.isBlank() && password.isBlank()) return; // No credentials means no account, never a public fallback.
        if (name.isBlank() || password.length() < 16) throw new IllegalArgumentException("Configure a username and a password with at least 16 characters for " + role);
        users.add(User.withUsername(name).password(encoder.encode(password)).roles(role).build());
    }
}
