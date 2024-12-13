package com.to_do;

import com.to_do.entity.Task;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.h2.jdbcx.JdbcDataSource;
import javax.sql.DataSource;
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
        return args -> {
            try(InputStream inputStream = this.getClass().getResourceAsStream("/initial.sql")){
                String sql = null;
                if (inputStream == null){
                    throw new RuntimeException("Error reading file");
                } else {
                    sql = new String(inputStream.readAllBytes());
                }
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                statement.executeUpdate(sql);

                String insertSql = "INSERT INTO task (title, finished, created_date) VALUES (?, ?, ?)";
                try(Connection connection1 = dataSource.getConnection();
                    PreparedStatement preparedStatement = connection1.prepareStatement(insertSql)){
                    preparedStatement.setString(1, "home work");
                    preparedStatement.setBoolean(2, false);
                    preparedStatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    preparedStatement.executeUpdate();
                }
                System.out.println("Tasks from DB: ");
                ResultSet rs = statement.executeQuery("SELECT task_id, title, finished, created_date FROM task");
                while (rs.next()){
                    Task task = new Task(
                            rs.getString(2),
                            rs.getBoolean(3),
                            rs.getTimestamp(4).toLocalDateTime());
                    task.setId(rs.getInt(1));
                    System.out.println(task);
                }
            }

        };
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

    }
}