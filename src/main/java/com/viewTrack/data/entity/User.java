package com.viewTrack.data.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

import static com.viewTrack.data.entity.AbstractEntity.DEFAULT_GENERATOR;

@Accessors(chain = true)
@Entity
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
@SequenceGenerator(name = DEFAULT_GENERATOR, sequenceName = "users_seq")
@NoArgsConstructor
public class User extends AbstractEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "surname", nullable = false)
    private String surname;

    @Column(name = "email")
    private String email;

    @Column(name = "login", nullable = false)
    @NonNull
    private String login;

    @Column(name = "password", nullable = false)
    @NonNull
    private String password;

    @ManyToOne
    @JoinColumn(name = "profile_image_id")
    private Image profileImage;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private AuthorityRole role;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_movies",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    private List<Movie> movies = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "user_streaming_services",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "streaming_service_id")
    )
    private final List<StreamingService> platforms = new ArrayList<>();
}
