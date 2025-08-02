package com.viewTrack.service.impl;

import com.viewTrack.data.entity.User;
import com.viewTrack.data.enums.Type;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.data.repository.UserMovieRepository;
import com.viewTrack.data.repository.UserRepository;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMovieRepository userMovieRepository;

    private final ReviewRepository reviewRepository;

    @Override
    public User getById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String.format("User with id: %s -- is not found", id))
                );
    }

    @Override
    public User getByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String.format("User with login: %s -- is not found", login))
                );
    }

    @Override
    public Long getWatchedCount(Long userId) {
        return userMovieRepository.countByUserIdAndType(userId, Type.WATCHED);
    }

    @Override
    public Long getToWatchCount(Long userId) {
        return userMovieRepository.countByUserIdAndType(userId, Type.TO_WATCH);
    }

    @Override
    public Long getRatingsCount(Long userId) {
        return reviewRepository.countByUser_Id(userId);
    }

    @Override
    public User updateProfile(Long userId, String name, String surname, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(name);
        user.setSurname(surname);
        user.setLogin(email);

        return userRepository.save(user);
    }
}
