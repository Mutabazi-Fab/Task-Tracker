package com.throughline.taskmanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** incidents.business_unit used to be a fixed enum, and Hibernate created a CHECK constraint listing
 *  exactly those 15 values. */
@Component
@RequiredArgsConstructor
public class IncidentSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE IF EXISTS incidents DROP CONSTRAINT IF EXISTS incidents_business_unit_check");
    }
}
