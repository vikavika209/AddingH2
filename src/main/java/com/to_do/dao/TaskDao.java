package com.to_do.dao;

import com.to_do.entity.Task;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface TaskDao {
    Task save(Task task);
    List<Task> findAll();
    int deleteAll();
    Task getById(Integer id);
    List<Task> findAllNotFinished();
    List<Task> findNewestTasks(Integer numberOfNewestTasks);
    Task finishTask(Task task);
    void deleteById(Integer id);
}
