package com.throughline.taskmanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * incidents.business_unit used to be a fixed enum, and Hibernate created a CHECK constraint
 * listing exactly those 15 values. It is now free text holding a department name, but
 * spring.jpa.hibernate.ddl-auto=update never drops or rewrites an existing constraint, so
 * on a database created before the change that old CHECK would reject every new department
 * name. This drops it once at startup (a no-op when it is already gone, or on a fresh
 * database that never had it). No row is read or changed.
 */
@Component
@RequiredArgsConstructor
public class IncidentSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE IF EXISTS incidents DROP CONSTRAINT IF EXISTS incidents_business_unit_check");
    }
}
