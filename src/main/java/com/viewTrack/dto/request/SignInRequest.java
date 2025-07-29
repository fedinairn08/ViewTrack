package com.viewTrack.dto.request;

import org.springframework.lang.NonNull;

public record SignInRequest(

    @NonNull String username,

    @NonNull String password

) {}
