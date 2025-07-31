package com.viewTrack.utils;

import com.viewTrack.data.entity.User;
import com.viewTrack.exeption.AppException;
import com.viewTrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthUtils {

    private final UserService userService;

    public UserDetails getUserDetailsOrThrow() {
        UserDetails principal = getUserDetails();

        if (principal != null) {
            return principal;
        }
        throw new AppException("UserDetails is null");
    }

    public Optional<UserDetails> getUserDetailsOrNull() {
        UserDetails principal =  getUserDetails();

        if (principal != null) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }


    private static UserDetails getUserDetails() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null){
            return null;
        }

        return (UserDetails) authentication.getPrincipal();
    }

    public User getUserEntity() {
        return (User) getUserDetailsOrThrow();
    }
}
