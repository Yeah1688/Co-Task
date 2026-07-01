package com.cotask.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "action_type", nullable = false)
    private String actionType; // CREATE, UPDATE, DELETE, MOVE, COMMENT等

    @Column(name = "entity_type", nullable = false)
    private String entityType; // CARD, LIST, BOARD等

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues; // JSON格式存储旧值

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues; // JSON格式存储新值

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 人类可读的描述，如"张三将卡片移到了已完成"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
