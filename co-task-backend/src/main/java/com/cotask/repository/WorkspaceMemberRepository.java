package com.cotask.repository;

import com.cotask.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, String> {
    // 查询某个用户加入的所有工作区（用于前端左侧边栏切换）
    List<WorkspaceMember> findByUserId(String userId);

    // 查询某个特定工作区下的所有成员列表（用于成员管理面板）
    List<WorkspaceMember> findByWorkspaceId(String workspaceId);

    // 💡 核心权限校验查询：获取某个用户在某个工作区内的具体角色
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(String workspaceId, String userId);
}