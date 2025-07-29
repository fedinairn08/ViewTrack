package com.viewTrack.data.repository;

import com.viewTrack.data.entity.AuthorityRole;
import com.viewTrack.data.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRoleRepository extends JpaRepository<AuthorityRole, Long> {
    AuthorityRole getAuthorityRoleByName(Role name);

    boolean existsByName(Role name);
}
