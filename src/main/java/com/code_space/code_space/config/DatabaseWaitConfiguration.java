package com.code_space.code_space.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class DatabaseWaitConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseWaitConfiguration.class);

    @Bean
    @Order(1) // Run this first, before other initialization
    public ApplicationRunner databaseConnectionWaiter(@Autowired DataSource dataSource) {
        return args -> {
            waitForDatabase(dataSource);
        };
    }

    private void waitForDatabase(DataSource dataSource) {
        int maxAttempts = 12; // 12 attempts = 6 minutes max wait
        int attemptInterval = 30000; // 30 seconds between attempts

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.info("Attempting to connect to database (attempt {}/{})", attempt, maxAttempts);

                try (Connection connection = dataSource.getConnection()) {
                    // Test the connection
                    if (connection.isValid(10)) {
                        logger.info("✅ Database connection successful!");
                        return;
                    }
                }

            } catch (SQLException e) {
                logger.warn("❌ Database connection attempt {} failed: {}", attempt, e.getMessage());

                if (attempt == maxAttempts) {
                    logger.error("🚨 Failed to connect to database after {} attempts. Application will continue but may fail.", maxAttempts);
                    throw new RuntimeException("Database connection failed after " + maxAttempts + " attempts", e);
                }

                try {
                    logger.info("⏳ Waiting {} seconds before next attempt...", attemptInterval / 1000);
                    Thread.sleep(attemptInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Database connection wait interrupted", ie);
                }
            }
        }
    }
}