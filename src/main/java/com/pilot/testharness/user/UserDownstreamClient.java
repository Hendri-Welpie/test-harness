package com.pilot.testharness.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class UserDownstreamClient {

    private static final String USERS_PATH = "/api/v1/users";

    private final RestClient restClient;

    public UserDownstreamClient(
            RestClient.Builder builder,
            @Value("${downstream.base-url}") String baseUrl
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public UsersResponse getUsers(Integer page, Integer limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(USERS_PATH)
                        .queryParamIfPresent("page", Optional.ofNullable(page))
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .build())
                .retrieve()
                .body(UsersResponse.class);
    }

    public User getUser(String userId) {
        try {
            return restClient.get()
                    .uri(USERS_PATH + "/{userId}", userId)
                    .retrieve()
                    .body(User.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException(userId);
        }
    }

    public void registerUser(CreateUserRequest request) {
        restClient.post()
                .uri(USERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
