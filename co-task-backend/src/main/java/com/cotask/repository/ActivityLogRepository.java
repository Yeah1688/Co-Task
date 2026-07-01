package com.cotask.repository;

import com.cotask.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, String> {
    List<ActivityLog> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<ActivityLog> findByEntityIdOrderByCreatedAtDesc(String entityId);
}
