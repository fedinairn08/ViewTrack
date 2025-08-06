package com.viewTrack.service;

import com.viewTrack.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse refresh(String refreshToken);

    AuthResponse signIn(String username, String password);

    AuthResponse signUp(String username, String password, String name, String surname);

    void changePassword(Long userId, String currentPassword, String newPassword);

    void createAdmin(String username, String password, String name, String surname);
}
