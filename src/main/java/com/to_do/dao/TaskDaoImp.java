package com.to_do.dao;

import com.to_do.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class TaskDaoImp implements TaskDao{

    private final DataSource dataSource;

    @Autowired
    public TaskDaoImp(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
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

    @Override
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
            throw new RuntimeException(throwables);
        }

        return tasks;
    }

    @Override
    public int deleteAll() {
        String sql = "DELETE FROM task";
        try(Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
        ){
            int result = statement.executeUpdate(sql);
            return  result;

        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting all tasks", e);
        }
    }

    @Override
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
                    //throw new RuntimeException("Task not found with id: " + id);
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching task with id: " + id, e);
        }
    }

    @Override
    public List<Task> findAllNotFinished() {
        List<Task> notFinishedTask = new ArrayList<>();
        String sql = "SELECT * from task WHERE finished = false";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    Task task = new Task(
                            rs.getString(2),
                            rs.getBoolean(3),
                            rs.getTimestamp(4).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS));
                    task.setId(rs.getInt(1));
                    notFinishedTask.add(task);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error fetching not finished tasks: " + e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching not finished tasks: " + e);
        }
        return notFinishedTask;
    }

    @Override
    public List<Task> findNewestTasks(Integer numberOfNewestTasks) {
        List<Task> newTasks = new ArrayList<>();
        String sql = "SELECT task_id, title, finished, created_date FROM task ORDER BY created_date DESC LIMIT ?";
        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setInt(1, numberOfNewestTasks);
            try(ResultSet rs = statement.executeQuery()){
                while (rs.next()){
                    Task task = new Task(
                            rs.getString(2),
                            rs.getBoolean(3),
                            rs.getTimestamp(4).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS));
                    task.setId(rs.getInt(1));
                    newTasks.add(task);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching new tasks: " + e);
        } return newTasks;
    }

    @Override
    public Task finishTask(Task task) {
        Task taskTofinish = new Task(task.getTitle(), true, task.getCreatedDate());
        taskTofinish.setId(task.getId());

        String sql = "UPDATE task SET title = ?, finished = ?, created_date = ? WHERE task_id = ?";
        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);){
            statement.setString(1, task.getTitle());
            statement.setBoolean(2, true);
            statement.setTimestamp(3, Timestamp.valueOf(task.getCreatedDate()));
            statement.setInt(4, task.getId());
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error to finish task with id " + task.getId() + ": " + e.getMessage(), e);
        }
        return taskTofinish;
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM task WHERE task_id = ?";

        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error to delete task with id " + id + ": " + e.getMessage(), e);
        }
    }
}
