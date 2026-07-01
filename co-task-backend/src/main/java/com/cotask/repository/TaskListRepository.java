package com.cotask.repository;
import com.cotask.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskListRepository extends JpaRepository<TaskList, String> {}