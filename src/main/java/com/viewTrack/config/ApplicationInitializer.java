package com.viewTrack.config;

import com.viewTrack.data.entity.AuthorityRole;
import com.viewTrack.data.enums.Role;
import com.viewTrack.data.repository.AuthorityRoleRepository;
import com.viewTrack.data.repository.UserRepository;
import com.viewTrack.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationInitializer implements CommandLineRunner {

    private final AuthorityRoleRepository authorityRoleRepository;

    private final UserRepository userRepository;

    private final AuthService authService;

    @SneakyThrows
    @Override
    public void run(String... args) {
        insertAuthorityRoles();
        setAdminUser();
    }

    void insertAuthorityRoles() {
        List<AuthorityRole> roles = Arrays.stream(Role.values())
                .map(role -> new AuthorityRole().setName(role))
                .toList();

        for (var role : roles) {
            if (!authorityRoleRepository.existsByName(role.getName())) {
                authorityRoleRepository.save(role);
            }
        }
    }

    void setAdminUser() {
        String login = "admin@mail.ru";
        if (!userRepository.existsByLogin(login)) {
            authService.createAdmin(login, "admin", "admin", "admin");
        }
    }
}
