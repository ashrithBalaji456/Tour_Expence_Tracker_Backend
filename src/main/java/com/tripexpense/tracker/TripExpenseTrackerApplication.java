package com.tripexpense.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class TripExpenseTrackerApplication {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(TripExpenseTrackerApplication.class, args);
    }

    @PostConstruct
    public void dropUniqueConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE pre_trip_members DROP CONSTRAINT IF EXISTS uk_hhawsncs4us42a2r9h53m5r87");
            System.out.println("Successfully dropped unique constraint uk_hhawsncs4us42a2r9h53m5r87 on pre_trip_members name column!");
        } catch (Exception e) {
            System.err.println("Could not drop constraint: " + e.getMessage());
        }
    }
}
