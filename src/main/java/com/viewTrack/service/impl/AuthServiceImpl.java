package com.viewTrack.service.impl;

import com.viewTrack.data.entity.AuthorityRole;
import com.viewTrack.data.entity.User;
import com.viewTrack.data.enums.Role;
import com.viewTrack.data.repository.AuthorityRoleRepository;
import com.viewTrack.data.repository.UserRepository;
import com.viewTrack.dto.response.AuthResponse;
import com.viewTrack.exeption.AccessDeniedException;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.security.jwt.JwtTokenProvider;
import com.viewTrack.service.AuthService;
import com.viewTrack.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder;

    private final AuthorityRoleRepository authorityRoleRepository;

    private final UserRepository userRepository;

    private final UserService userService;

    @Override
    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw new AccessDeniedException();
        }
        Long userId = jwtTokenProvider.parseId(refreshToken);
        return new AuthResponse(jwtTokenProvider.createAccessToken(userId), jwtTokenProvider.createRefreshToken(userId));
    }

    @Override
    public AuthResponse signIn(String login, String password) {
        User user = userService.getByLogin(login);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResourceNotFoundException("User with this password is not found");
        }
        setAuthentication(user);
        return new AuthResponse(
                jwtTokenProvider.createAccessToken(user.getId()),
                jwtTokenProvider.createRefreshToken(user.getId())
        );
    }

    @Override
    public AuthResponse signUp(String login, String password, String name, String surname) {
        User user = new User()
                .setLogin(login)
                .setPassword(passwordEncoder.encode(password))
                .setName(name)
                .setSurname(surname)
                .setRoles(getDefaultRole());

        user = userRepository.save(user);
        setAuthentication(user);

        return new AuthResponse(
                jwtTokenProvider.createAccessToken(user.getId()),
                jwtTokenProvider.createRefreshToken(user.getId())
        );
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Текущий пароль неверен");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    @Transactional
    @Override
    public void createAdmin(String login, String password, String name, String surname) {
        User user = new User()
                .setLogin(login)
                .setPassword(passwordEncoder.encode(password))
                .setName(name)
                .setSurname(surname)
                .setRoles(getAdminRole());

        userRepository.save(user);
    }

    private Set<AuthorityRole> getDefaultRole() {
        return Set.of(
                authorityRoleRepository.getAuthorityRoleByName(Role.USER)
        );
    }

    private Set<AuthorityRole> getAdminRole() {
        return Set.of(
                authorityRoleRepository.getAuthorityRoleByName(Role.USER)
        );
    }

    private void setAuthentication(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
