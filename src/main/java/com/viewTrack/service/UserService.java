package com.viewTrack.service;

import com.viewTrack.data.entity.User;

public interface UserService {
    User getById(long id);

    User getByLogin(String login);

    Long getWatchedCount(Long userId);

    Long getToWatchCount(Long userId);

    Long getRatingsCount(Long userId);

    User updateProfile(Long userId, String name, String surname, String email);
}
