package com.code_space.code_space.service;

import com.code_space.code_space.dto.*;
import com.code_space.code_space.entity.User;
import com.code_space.code_space.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private MfaService mfaService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    public AuthResponse register(RegisterRequest signUpRequest) {
        if (userService.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already taken!");
        }

        // Create new user account
        User user = new User(signUpRequest.getFirstName(),
                signUpRequest.getLastName(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        userService.save(user);

        // Generate JWT token
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(signUpRequest.getEmail(), signUpRequest.getPassword()));

        String jwt = jwtUtils.generateJwtToken(authentication);

        return new AuthResponse(jwt, user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getMfaEnabled());
    }

    public AuthResponse login(AuthRequest loginRequest) {
        User user = userService.findByEmail(loginRequest.getEmail());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        try {
            // First verify password
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()));

            // If MFA is enabled, verify MFA code
            if (user.getMfaEnabled()) {
                if (loginRequest.getMfaCode() == null || loginRequest.getMfaCode().isEmpty()) {
                    // Return response indicating MFA is required
                    return new AuthResponse(true);
                }

                if (!mfaService.verifyCode(user.getMfaSecret(), loginRequest.getMfaCode())) {
                    throw new RuntimeException("Invalid MFA code");
                }
            }

            // Generate JWT token
            String jwt = jwtUtils.generateJwtToken(authentication);

            return new AuthResponse(jwt, user.getId(), user.getEmail(),
                    user.getFirstName(), user.getLastName(), user.getMfaEnabled());

        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid credentials");
        }
    }

    public MfaSetupResponse setupMfa(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        String secret = mfaService.generateSecret();
        String qrCodeUrl = mfaService.generateQrCodeImageUri(secret, email);

        // Temporarily store the secret (don't save to database until verification)
        user.setMfaSecret(secret);
        userService.save(user);

        return new MfaSetupResponse(qrCodeUrl, secret);
    }

    public boolean enableMfa(String email, String code) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (user.getMfaSecret() == null) {
            throw new RuntimeException("MFA setup not initiated");
        }

        if (mfaService.verifyCode(user.getMfaSecret(), code)) {
            user.setMfaEnabled(true);
            userService.save(user);
            return true;
        }

        return false;
    }

    public boolean disableMfa(String email, String code) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!user.getMfaEnabled()) {
            throw new RuntimeException("MFA is not enabled");
        }

        if (mfaService.verifyCode(user.getMfaSecret(), code)) {
            user.setMfaEnabled(false);
            user.setMfaSecret(null);
            userService.save(user);
            return true;
        }

        return false;
    }
}