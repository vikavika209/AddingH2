package com.to_do.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import java.sql.Timestamp;
import com.to_do.entity.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskDaoImp {
    private final DataSource dataSource;

    public TaskDaoImp(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Logger logger = LoggerFactory.getLogger(TaskDaoImp.class);

    public Task save(Task task) {
        String sql = "INSERT INTO task (title, finished, created_date) VALUES (?, ?, ?)";
        try(
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {

            statement.setString(1, task.getTitle());
            statement.setBoolean(2, task.getFinished());
            statement.setTimestamp(3, java.sql.Timestamp.valueOf(task.getCreatedDate()));
            statement.executeUpdate();

            try(ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    task.setId(resultSet.getInt(1));
                }
            }

        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }

        return task;
    }

    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT task_id, title, finished, created_date FROM task ORDER BY task_id";
        try(Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)
        ) {

            while(resultSet.next()) {
                final Task task = new Task(
                        resultSet.getString(2),
                        resultSet.getBoolean(3),
                        resultSet.getTimestamp(4).toLocalDateTime()
                );
                task.setId(resultSet.getInt(1));
                tasks.add(task);
            }

        } catch (SQLException throwables) {
            logger.error("Error while finding all", throwables);
            throw new RuntimeException(throwables);
        }

        return tasks;
    }

    public int deleteAll() {
        String sql = "DELETE FROM task";
        try(Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
        ){
            int result = statement.executeUpdate(sql);
            return  result;

        } catch (SQLException e) {
            logger.error("Error while deleting all: {}", String.valueOf(e));
            throw new RuntimeException("Error while deleting all tasks", e);
        }

    }

    public Task getById(Integer id) {

        String sql = "SELECT * FROM task WHERE task_id = ?";

        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try(ResultSet resultSet = statement.executeQuery()){
                if (resultSet.next()){
                    Task task = new Task(
                            resultSet.getString(2),
                            resultSet.getBoolean(3),
                            resultSet.getTimestamp(4) != null ?
                                    resultSet.getTimestamp(4).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS) :
                                    null
                    );
                    task.setId(resultSet.getInt(1));
                    return task;
                }else {
                    return null;
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching task with id: {}", id, e);
            throw new RuntimeException("Error fetching task with id: " + id, e);
        }
    }

    public List<Task> findAllNotFinished() {
        List<Task> notFinishedTask = new ArrayList<>();
        String sql = "SELECT * from task WHERE finished = false";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    Task task = taskFromResultSet(rs);
                    notFinishedTask.add(task);
                }
            } catch (SQLException e) {
                logger.error("SQLException while fetching not finished tasks: {}", String.valueOf(e));
                throw new RuntimeException("Error fetching not finished tasks: " + e);
            }
        } catch (SQLException e) {
            logger.error("SQLException fetching not finished tasks: {}", String.valueOf(e));
            throw new RuntimeException("Error fetching not finished tasks: " + e);
        }
        return notFinishedTask;
    }

    public List<Task> findNewestTasks(Integer numberOfNewestTasks) {
        List<Task> newTasks = new ArrayList<>();
        String sql = "SELECT task_id, title, finished, created_date FROM task ORDER BY created_date DESC LIMIT ?";
        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setInt(1, numberOfNewestTasks);
            try(ResultSet rs = statement.executeQuery()){
                while (rs.next()){
                    Task task = taskFromResultSet(rs);
                    newTasks.add(task);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching new tasks: {}", String.valueOf(e));
            throw new RuntimeException("Error fetching new tasks: " + e);
        }


        return newTasks;
    }

    public void finishTask(Task task) {

        if (task == null){
            logger.debug("Task is null");
        }
        else {
            Task taskTofinish = new Task(task.getTitle(), true, task.getCreatedDate());
            taskTofinish.setId(task.getId());

            String sql = "UPDATE task SET title = ?, finished = ?, created_date = ? WHERE task_id = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);) {
                statement.setString(1, task.getTitle());
                statement.setBoolean(2, true);
                statement.setTimestamp(3, Timestamp.valueOf(task.getCreatedDate()));
                statement.setInt(4, task.getId());
                statement.executeUpdate();

            } catch (SQLException e) {
                logger.error("Error to finish task with id = {}: {} ", task.getId(), String.valueOf(e));
                throw new RuntimeException("Error to finish task with id " + task.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    public void deleteById(Integer id) {
        Task task = getById(id);
        if(task == null){
            logger.debug("Task with id = {} doesn't exist", id);
        }
        else {
            String sql = "DELETE FROM task WHERE task_id = ?";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();

            } catch (SQLException e) {
                logger.error("Error to delete task with id = {}: {} ", id, String.valueOf(e));
                throw new RuntimeException("Error to delete task with id " + id + ": " + e.getMessage(), e);
            }
        }
    }
    private Task taskFromResultSet(ResultSet rs){
        Task task = null;
        try {
            task = new Task(
                    rs.getString("title"),
                    rs.getBoolean("finished"),
                    rs.getTimestamp("created_date").toLocalDateTime().truncatedTo(ChronoUnit.SECONDS));
        } catch (SQLException e) {
            logger.error("SQLException while getting task from Result Set");
            throw new RuntimeException(e);
        }
        try {
            task.setId(rs.getInt("task_id"));
        } catch (SQLException e) {
            logger.error("SQLException to set Id to task getting from Result Set");
            throw new RuntimeException(e);
        }
        return task;
    }
}