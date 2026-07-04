package com.docportal.docportal.services;

import com.docportal.docportal.entities.User;

import java.util.List;

public interface UserService {

    User save(User user);

    User update(Long id, User user);

    void delete(Long id);

    List<User> findAll();

    User findById(Long id);

    User findByUsername(String username);

}
