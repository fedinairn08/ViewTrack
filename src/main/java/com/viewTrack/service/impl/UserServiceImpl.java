package com.viewTrack.service.impl;

import com.viewTrack.data.entity.User;
import com.viewTrack.data.repository.UserRepository;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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
        return null;
    }

    @Override
    public Long getToWatchCount(Long userId) {
        return null;
    }

    @Override
    public Long getRatingsCount(Long userId) {
        return null;
    }

    @Override
    public User updateProfile(Long userId, String name, String surname, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(name);
        user.setSurname(surname);
        user.setEmail(email);

        return userRepository.save(user);
    }

    @Override
    public String uploadProfileImage(Long userId, MultipartFile file) {
        return null;
    }
}
