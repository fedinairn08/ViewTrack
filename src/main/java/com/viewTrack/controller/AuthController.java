package com.viewTrack.controller;

import com.viewTrack.dto.BasicApiResponse;
import com.viewTrack.dto.request.JwtRefreshRequest;
import com.viewTrack.dto.request.SignInRequest;
import com.viewTrack.dto.request.SignUpRequest;
import com.viewTrack.dto.response.AuthResponse;
import com.viewTrack.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signIn")
    public ResponseEntity<BasicApiResponse<AuthResponse>> signIn(@RequestBody SignInRequest authRequest) {
        return ResponseEntity.ok(new BasicApiResponse<>(authService.signIn(authRequest.username(), authRequest.password())));
    }


    @PostMapping("/signUp")
    public ResponseEntity<BasicApiResponse<AuthResponse>> signUp(@RequestBody SignUpRequest authRequest) {
        return ResponseEntity.ok(new BasicApiResponse<>(authService.signUp(authRequest.username(),
                authRequest.password(), authRequest.name(), authRequest.surname())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<BasicApiResponse<AuthResponse>> refresh(@RequestBody JwtRefreshRequest refreshRequest) {
        return ResponseEntity.ok(new BasicApiResponse<>(authService.refresh(refreshRequest.refreshToken())));
    }
}
