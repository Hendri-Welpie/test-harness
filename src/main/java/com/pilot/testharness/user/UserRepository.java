package com.pilot.testharness.user;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public User save(String name, String email) {
        return jdbcClient
                .sql("INSERT INTO users (name, email) VALUES (:name, :email) RETURNING id, created_at")
                .param("name", name)
                .param("email", email)
                .query((rs, rowNum) -> new User(
                        rs.getLong("id"),
                        name,
                        email,
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .single();
    }
}