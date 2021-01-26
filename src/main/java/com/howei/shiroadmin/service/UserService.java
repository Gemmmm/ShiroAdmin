package com.howei.shiroadmin.service;

import com.howei.shiroadmin.model.User;

import java.util.List;

public interface UserService {
    User getByUsernameAndPassword(String username, String password);

    int insert(User user);

    int delete(Integer id);

    List<User> getAll();

    User getByUsername(String username);

    int update(User user);
}
