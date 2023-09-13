package com.wildevent.WildEventMenager.security.auth;

import com.wildevent.WildEventMenager.location.model.Location;
import com.wildevent.WildEventMenager.location.service.LocationService;
import com.wildevent.WildEventMenager.role.model.Role;
import com.wildevent.WildEventMenager.role.service.RoleService;
import com.wildevent.WildEventMenager.security.config.JwtService;
import com.wildevent.WildEventMenager.user.model.WildUser;
import com.wildevent.WildEventMenager.user.repository.WildUserRepository;
import com.wildevent.WildEventMenager.user.service.email.EmailSendingService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final WildUserRepository wildUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailSendingService emailSendingService;
    private final UserDetailsService userDetailsService;
    private final LocationService locationService;
    private final RoleService roleService;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        String randomPassword = generateRandomPassword(8);
        List<Location> locations = locationService.mapLocationsFromIds(request.getLocationIds());
        Set<Role> roles = roleService.mapRolesFromIds(request.getRoleIds());

        WildUser user = WildUser.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(randomPassword))
                .phone(request.getPhone())
                .active(true)
                .location(locations)
                .role(roles)
                .build();

        wildUserRepository.save(user);
        String resetToken = jwtService.generatePasswordResetToken(user);

        emailSendingService.sendPasswordResetEmail(user.getEmail(), resetToken);

        return AuthenticationResponse.builder()
                .token("User created and email sent.")
                .build();
    }

    public AuthenticationResponse authenticate(RegisterRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            WildUser user = wildUserRepository.findByEmail(request.getEmail())
                    .orElseThrow();

            var jwtToken = jwtService.generateToken(user);

            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .build();
        } catch (Exception e) {
            logger.error("Error during authentication: ", e);
            throw e;
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        String username = jwtService.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (jwtService.isTokenValid(token, userDetails)) {
            String tokenType = jwtService.extractTokenType(token);

            if(!"reset_password".equals((tokenType))) {
                throw new IllegalArgumentException("Invalid token type");
            }

            WildUser user = wildUserRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            user.setPassword(passwordEncoder.encode(newPassword));
            wildUserRepository.save(user);
        } else {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    public void generateResetLink(ResetPasswordByUserRequest request) {
        try {
            WildUser user = wildUserRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            String resetToken = jwtService.generatePasswordResetToken(user);
            emailSendingService.sendPasswordResetEmail(user.getEmail(), resetToken);
        } catch (Exception e) {
            logger.error("Error during generating reset link: ", e);
            throw e;

        }

    }

    private String generateRandomPassword(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-+<>?";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            password.append(characters.charAt(index));
        }

        return password.toString();
    }

}
