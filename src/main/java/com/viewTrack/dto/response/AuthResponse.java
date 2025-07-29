package com.viewTrack.dto.response;

public record AuthResponse(
        String accessToken,

        String refreshToken
) {
}
