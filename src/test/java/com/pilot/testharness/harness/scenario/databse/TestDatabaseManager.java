package com.pilot.testharness.harness.scenario.databse;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public class TestDatabaseManager {

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseManager(
            DataSource dataSource
    ) {
        this.jdbcTemplate =
                new JdbcTemplate(dataSource);
    }

    public void reset() {

        List<String> tables =
                jdbcTemplate.queryForList(
                        """
                                SELECT schemaname || '.' || tablename
                                FROM pg_tables
                                WHERE schemaname NOT IN (
                                    'pg_catalog',
                                    'information_schema'
                                )
                                AND tablename NOT IN (
                                    'flyway_schema_history',
                                    'databasechangelog',
                                    'databasechangeloglock'
                                )
                                ORDER BY schemaname, tablename
                                """,
                        String.class
                );

        if (tables.isEmpty()) {
            return;
        }

        jdbcTemplate.execute(
                "TRUNCATE TABLE "
                        + String.join(", ", tables)
                        + " RESTART IDENTITY CASCADE"
        );
    }

    public void execute(String sql) {
        jdbcTemplate.execute(sql);
    }

    public List<Map<String, Object>> query(
            String sql
    ) {
        return jdbcTemplate.queryForList(sql);
    }
}