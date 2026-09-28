package com.festpass;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/festpass?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        "spring.datasource.username=root",
        "spring.datasource.password=S5nur@nu02",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
public class MySqlConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Verify actual MySQL connection, database metadata, and tables created")
    void testRealMySqlConnection() throws Exception {
        assertNotNull(dataSource, "DataSource should not be null");
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Connection should be established");
            assertFalse(connection.isClosed(), "Connection should be open");

            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println(">>> CONNECTED TO DATABASE: " + metaData.getDatabaseProductName() + " v" + metaData.getDatabaseProductVersion());
            System.out.println(">>> JDBC DRIVER: " + metaData.getDriverName() + " v" + metaData.getDriverVersion());
            System.out.println(">>> CONNECTION URL: " + metaData.getURL());

            assertEquals("MySQL", metaData.getDatabaseProductName());

            // Verify tables exist in MySQL
            ResultSet tables = metaData.getTables("festpass", null, "%", new String[]{"TABLE"});
            boolean foundEvents = false, foundTickets = false, foundAttendees = false;
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println(">>> DETECTED TABLE IN MYSQL: " + tableName);
                if ("fest_events".equalsIgnoreCase(tableName)) foundEvents = true;
                if ("tickets".equalsIgnoreCase(tableName)) foundTickets = true;
                if ("attendees".equalsIgnoreCase(tableName)) foundAttendees = true;
            }

            assertTrue(foundEvents, "fest_events table must be created in MySQL");
            assertTrue(foundTickets, "tickets table must be created in MySQL");
            assertTrue(foundAttendees, "attendees table must be created in MySQL");
        }
    }
}
