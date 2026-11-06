package com.tripexpense.tracker.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Checking and verifying database schema columns...");
            jdbcTemplate.execute("ALTER TABLE pre_trip_members ADD COLUMN IF NOT EXISTS split_mode VARCHAR(32) DEFAULT 'FROM_START'");
            jdbcTemplate.execute("ALTER TABLE pre_trip_members ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE expenses ADD COLUMN IF NOT EXISTS expense_time TIME");
            jdbcTemplate.execute("UPDATE pre_trip_members SET split_mode = 'FROM_START' WHERE split_mode IS NULL");
            log.info("Database schema columns verified successfully!");
        } catch (Exception e) {
            log.warn("Auto-migration schema notice: {}", e.getMessage());
        }
    }
}
