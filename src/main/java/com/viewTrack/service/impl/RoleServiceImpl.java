package com.viewTrack.service.impl;

import com.viewTrack.data.entity.User;
import com.viewTrack.service.RoleService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final AuthUtils authUtils;

    @Override
    public boolean isAdmin() {
        try {
            User user = authUtils.getUserEntity();
            return user.getRoles().stream()
                    .anyMatch(role -> "ADMIN".equals(role.getName()));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isUser() {
        try {
            User user = authUtils.getUserEntity();
            return user.getRoles().stream()
                    .anyMatch(role -> "USER".equals(role.getName()));
        } catch (Exception e) {
            return false;
        }
    }
}
