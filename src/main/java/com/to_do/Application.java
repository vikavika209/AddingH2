package com.to_do;

import com.to_do.entity.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.h2.jdbcx.JdbcDataSource;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;

@SpringBootApplication
public class Application {

    @Bean
    public DataSource dataSource() {
        JdbcDataSource jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:./db");
        jdbcDataSource.setUser("user");
        jdbcDataSource.setPassword("password");
        return jdbcDataSource;
    }

    @Bean
    public CommandLineRunner cmd (DataSource dataSource){
        Logger logger = LoggerFactory.getLogger(Application.class);
        return args -> {
            try(InputStream inputStream = this.getClass().getResourceAsStream("/initial.sql")) {
                String sql = null;
                if (inputStream == null) {
                    logger.error("Error reading file");
                    throw new IOException("Error reading file");
                } else {
                    sql = new String(inputStream.readAllBytes());
                }
                executeSQL(sql, dataSource);
                insertTask(dataSource, "home work", false, LocalDateTime.now());
                printTasksFromDB(dataSource, logger);
            } catch (IOException | SQLException e) {
                logger.error("Error occurred: ", e);
            }
        };
    }
    private void executeSQL(String sql, DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void insertTask(DataSource dataSource, String title, boolean finished, LocalDateTime createdDate) throws SQLException {
        String insertSql = "INSERT INTO task (title, finished, created_date) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
            preparedStatement.setString(1, title);
            preparedStatement.setBoolean(2, finished);
            preparedStatement.setTimestamp(3, Timestamp.valueOf(createdDate));
            preparedStatement.executeUpdate();
        }
    }

    private void printTasksFromDB(DataSource dataSource, Logger logger) throws SQLException {
        String selectSql = "SELECT task_id, title, finished, created_date FROM task";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(selectSql)) {
            logger.info("Tasks from DB: ");
            while (rs.next()) {
                Task task = new Task(
                        rs.getString("title"),
                        rs.getBoolean("finished"),
                        rs.getTimestamp("created_date").toLocalDateTime());
                task.setId(rs.getInt("task_id"));
                logger.info(String.valueOf(task));
            }
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

    }
}