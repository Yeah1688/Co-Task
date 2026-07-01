package com.cotask.repository;

import com.cotask.entity.AiTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiTaskRepository extends JpaRepository<AiTask, String> {
    List<AiTask> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<AiTask> findBySourceCardIdOrderByCreatedAtDesc(String sourceCardId);
}
