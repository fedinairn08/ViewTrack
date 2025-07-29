package com.viewTrack.config;

import com.viewTrack.data.entity.AuthorityRole;
import com.viewTrack.data.enums.Role;
import com.viewTrack.data.repository.AuthorityRoleRepository;
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

    @SneakyThrows
    @Override
    public void run(String... args) {
        insertAuthorityRoles();
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
}
