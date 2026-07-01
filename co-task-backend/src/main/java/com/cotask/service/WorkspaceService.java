package com.cotask.service;

import com.cotask.entity.Workspace;
import com.cotask.entity.WorkspaceMember;
import java.util.List;

public interface WorkspaceService {
    // 创建新工作区（自动将创建者设为 OWNER）
    Workspace createWorkspace(String name, String description, String userId);

    // 为工作区生成唯一的邀请码（存储或临时计算，这里采用为工作区表扩展 invite_code 字段的方案）
    String generateInviteCode(String workspaceId, String operatorId);

    // 用户通过邀请码加入工作区（赋予 MEMBER 角色）
    WorkspaceMember joinWorkspaceByCode(String inviteCode, String userId);

    // 校验当前用户在工作区内的权限等级
    boolean checkPermission(String workspaceId, String userId, String requiredRole);

    // 获取用户参与的所有工作区
    List<Workspace> getUserWorkspaces(String userId);

    // 获取工作区详情（包含成员列表）
    Workspace getWorkspaceDetail(String workspaceId);

    // 更新工作区信息
    Workspace updateWorkspace(String workspaceId, String name, String description, String operatorId);

    // 删除工作区
    void deleteWorkspace(String workspaceId, String operatorId);

    // 移除工作区成员
    void removeMember(String workspaceId, String targetUserId, String operatorId);

    // 更新成员角色
    WorkspaceMember updateMemberRole(String workspaceId, String targetUserId, String newRole, String operatorId);
}
