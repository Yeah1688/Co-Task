package com.cotask.service.impl;

import com.cotask.entity.ActivityLog;
import com.cotask.entity.User;
import com.cotask.entity.Workspace;
import com.cotask.repository.ActivityLogRepository;
import com.cotask.repository.UserRepository;
import com.cotask.repository.WorkspaceRepository;
import com.cotask.service.ActivityLogService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Override
    @Transactional
    public ActivityLog logActivity(String actionType, String entityType, String entityId,
                                   String oldValues, String newValues, String description,
                                   String workspaceId, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("工作区不存在"));

        ActivityLog log = ActivityLog.builder()
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .description(description)
                .workspace(workspace)
                .user(user)
                .build();

        return activityLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getWorkspaceActivities(String workspaceId) {
        return activityLogRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getEntityActivities(String entityId) {
        return activityLogRepository.findByEntityIdOrderByCreatedAtDesc(entityId);
    }
}
