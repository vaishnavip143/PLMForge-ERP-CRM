package com.plmforge.crm.service;

import com.plmforge.crm.dto.request.LoginRequest;
import com.plmforge.crm.dto.request.RegisterRequest;
import com.plmforge.crm.dto.response.AuthResponse;
import com.plmforge.crm.dto.response.CurrentUserResponse;
import com.plmforge.crm.dto.response.RegisterResponse;
import com.plmforge.crm.dto.response.RepresentativeResponse;
import com.plmforge.crm.entity.Customer;
import com.plmforge.crm.entity.User;
import com.plmforge.crm.enums.Role;
import com.plmforge.crm.exception.DuplicateResourceException;
import com.plmforge.crm.exception.ResourceNotFoundException;
import com.plmforge.crm.repository.CustomerRepository;
import com.plmforge.crm.repository.UserRepository;
import com.plmforge.crm.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // Public signup. Always creates a CUSTOMER - RegisterRequest has no role field,
    // so there is nothing a client could send to change that.
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("There is already an account for " + email);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);

        // Registration also creates the customer profile, otherwise the
        // self-service endpoints have nothing to read on first sign-in.
        if (!customerRepository.existsByEmailIgnoreCase(email)) {
            customerRepository.save(Customer.builder()
                    .name(savedUser.getName())
                    .email(email)
                    .build());
        }

        log.info("new customer signed up: {} (#{})", savedUser.getEmail(), savedUser.getId());

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().getDisplayName())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AuthenticationException exception) {
            log.warn("bad login for {} ({})", email, exception.getClass().getSimpleName());
            throw exception;
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user with email " + email));

        String token = jwtService.generateToken(user);
        log.info("{} signed in as {} (#{})", user.getEmail(), user.getRole(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getDisplayName())
                .build();
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user with email " + email));

        return CurrentUserResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getDisplayName())
                .authenticated(true)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RepresentativeResponse> getSalesRepresentatives() {
        return userRepository.findByRole(Role.ROLE_SALES_REPRESENTATIVE)
                .stream()
                .map(RepresentativeResponse::from)
                .toList();
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
