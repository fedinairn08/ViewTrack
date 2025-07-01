package com.viewTrack.data.entity;

import com.viewTrack.data.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;

import static com.viewTrack.data.entity.AbstractEntity.DEFAULT_GENERATOR;

@Accessors(chain = true)
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "roles")
@SequenceGenerator(name = DEFAULT_GENERATOR, sequenceName = "roles_seq")
public class AuthorityRole extends AbstractEntity implements GrantedAuthority {

    @Enumerated(EnumType.STRING)
    @Column(name = "name", length = 20)
    private Role name;

    @Override
    public String getAuthority() {
        return "ROLE_" + name;
    }
}
