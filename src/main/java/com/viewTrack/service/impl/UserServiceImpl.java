package com.viewTrack.service.impl;

import com.viewTrack.data.entity.User;
import com.viewTrack.data.repository.UserRepository;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
