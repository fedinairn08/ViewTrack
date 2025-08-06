package com.viewTrack.utils;

import com.viewTrack.data.entity.User;
import com.viewTrack.exeption.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtils {

    public UserDetails getUserDetailsOrThrow() {
        UserDetails principal = getUserDetails();

        if (principal != null) {
            return principal;
        }
        throw new AppException("UserDetails is null");
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
