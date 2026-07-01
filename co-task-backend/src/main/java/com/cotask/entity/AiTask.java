package com.cotask.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "task_type", nullable = false)
    private String taskType; // DECOMPOSE, SUMMARY, RISK_ANALYSIS

    @Column(name = "source_card_id")
    private String sourceCardId; // 可选，关联的卡片ID

    @Column(name = "workspace_id")
    private String workspaceId; // 工作区ID

    @Column(columnDefinition = "TEXT")
    private String prompt; // AI提示词

    @Column(columnDefinition = "TEXT")
    private String result; // AI返回结果

    @Column(name = "status", nullable = false)
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = "PENDING";
    }
}
