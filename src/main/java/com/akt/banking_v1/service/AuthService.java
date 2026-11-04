package com.akt.banking_v1.service;

import com.akt.banking_v1.dto.AuthResponse;
import com.akt.banking_v1.dto.LoginRequest;
import com.akt.banking_v1.repository.UserRepository;
import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest loginRequest){

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        UserDetails userDetails = (UserDetails) Objects.requireNonNull(authentication.getPrincipal());

        String accessToken = jwtService.generateToken(userDetails);

        return new AuthResponse(accessToken);
    }
}
