package com.cotask.controller;

import com.cotask.entity.ActivityLog;
import com.cotask.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "*")
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<?> getWorkspaceActivities(@PathVariable String workspaceId) {
        try {
            List<ActivityLog> activities = activityLogService.getWorkspaceActivities(workspaceId);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<?> getEntityActivities(@PathVariable String entityId) {
        try {
            List<ActivityLog> activities = activityLogService.getEntityActivities(entityId);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
