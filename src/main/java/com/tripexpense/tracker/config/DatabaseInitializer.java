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
            jdbcTemplate.execute("ALTER TABLE pre_trip_members ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE pre_trip_members ADD COLUMN IF NOT EXISTS is_departed BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE pre_trip_members ADD COLUMN IF NOT EXISTS settled_on_departure BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE expenses ADD COLUMN IF NOT EXISTS expense_time TIME");
            jdbcTemplate.execute("UPDATE pre_trip_members SET split_mode = 'FROM_START' WHERE split_mode IS NULL");
            jdbcTemplate.execute("UPDATE pre_trip_members SET is_departed = FALSE WHERE is_departed IS NULL");
            jdbcTemplate.execute("UPDATE pre_trip_members SET settled_on_departure = FALSE WHERE settled_on_departure IS NULL");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS member_expense_allocations (
                    id BIGSERIAL PRIMARY KEY,
                    member_id BIGINT NOT NULL REFERENCES pre_trip_members(id) ON DELETE CASCADE,
                    expense_id BIGINT,
                    source_type VARCHAR(32),
                    title VARCHAR(255) NOT NULL,
                    original_amount NUMERIC(10, 2),
                    allocated_amount NUMERIC(10, 2) NOT NULL,
                    days_count INT,
                    total_days INT,
                    notes TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            log.info("Database schema columns and allocation tables verified successfully!");
        } catch (Exception e) {
            log.warn("Auto-migration schema notice: {}", e.getMessage());
        }
    }
}
