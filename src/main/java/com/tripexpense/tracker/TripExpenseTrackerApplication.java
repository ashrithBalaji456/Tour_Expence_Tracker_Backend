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
    public void dropConstraints() {
        try {
            jdbcTemplate.execute("ALTER TABLE pre_trip_members DROP CONSTRAINT IF EXISTS uk_hhawsncs4us42a2r9h53m5r87");
            System.out.println("Successfully dropped unique constraint uk_hhawsncs4us42a2r9h53m5r87 on pre_trip_members name column!");
        } catch (Exception e) {
            System.err.println("Could not drop pre_trip_members constraint: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE expenses DROP CONSTRAINT IF EXISTS expenses_category_check");
            System.out.println("Successfully dropped check constraint expenses_category_check on expenses table!");
        } catch (Exception e) {
            System.err.println("Could not drop expenses check constraint: " + e.getMessage());
        }

        // Diagnostics
        try {
            System.out.println("=== DIAGNOSTICS: USERS ===");
            jdbcTemplate.queryForList("SELECT id, username, email FROM users")
                .forEach(row -> System.out.println("User: " + row));

            System.out.println("=== DIAGNOSTICS: TRIP GROUPS ===");
            jdbcTemplate.queryForList("SELECT id, name, creator_id FROM trip_groups")
                .forEach(row -> System.out.println("TripGroup: " + row));

            System.out.println("=== DIAGNOSTICS: TRIP GROUP MEMBERS ===");
            jdbcTemplate.queryForList("SELECT group_id, username FROM trip_group_members")
                .forEach(row -> System.out.println("GroupMember: " + row));

            System.out.println("=== DIAGNOSTICS: SETTLEMENT EXPENSES ===");
            jdbcTemplate.queryForList("SELECT id, title, amount, paid_by, category, notes FROM expenses WHERE category = 'SETTLEMENT'")
                .forEach(row -> System.out.println("SettlementExpense: " + row));
        } catch (Exception e) {
            System.err.println("Diagnostics failed: " + e.getMessage());
        }
    }
}
