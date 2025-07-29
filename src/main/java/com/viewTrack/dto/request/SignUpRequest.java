package com.viewTrack.dto.request;

import org.springframework.lang.NonNull;

public record SignUpRequest(

    @NonNull String username,

    @NonNull String password,

    @NonNull String name,

    @NonNull String surname
) {}
