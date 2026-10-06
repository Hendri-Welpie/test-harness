package com.pilot.testharness.user;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserDownstreamClient downstream;
    private final UserRepository repository;

    public UserService(UserDownstreamClient downstream, UserRepository repository) {
        this.downstream = downstream;
        this.repository = repository;
    }

    public UsersResponse getUsers(Integer page, Integer limit) {
        return downstream.getUsers(page, limit);
    }

    public User getUser(String userId) {
        return downstream.getUser(userId);
    }

    public User createUser(CreateUserRequest request) {
        downstream.registerUser(request);
        return repository.save(request.name(), request.email());
    }
}
