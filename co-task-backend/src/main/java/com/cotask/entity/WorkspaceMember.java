package com.cotask.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"workspace_id", "user_id"}) // 确保一个用户在一个工作区里只有一条角色记录
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onJoin() {
        this.joinedAt = LocalDateTime.now();
    }
}