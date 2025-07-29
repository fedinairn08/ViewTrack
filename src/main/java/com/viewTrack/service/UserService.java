package com.viewTrack.service;

import com.viewTrack.data.entity.User;

public interface UserService {
    User getById(long id);

    User getByLogin(String login);
}
