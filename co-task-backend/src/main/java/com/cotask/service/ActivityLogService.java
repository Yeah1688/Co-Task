package com.cotask.service;

import com.cotask.entity.ActivityLog;
import java.util.List;
import java.util.Map;

public interface ActivityLogService {
    ActivityLog logActivity(String actionType, String entityType, String entityId,
                            String oldValues, String newValues, String description,
                            String workspaceId, String userId);

    List<ActivityLog> getWorkspaceActivities(String workspaceId);
    List<ActivityLog> getEntityActivities(String entityId);
}
