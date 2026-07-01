
package com.cotask.security;

import com.cotask.entity.Role;
import com.cotask.entity.WorkspaceMember;
import com.cotask.repository.WorkspaceMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 工作区权限校验工具类
 * 提供统一的 RBAC 权限检查逻辑，供 Board / Card / TaskList 等 Service 层调用
 *
 * 权限等级：OWNER > ADMIN > MEMBER
 * - OWNER：可删除工作区、修改成员角色、管理看板和卡片
 * - ADMIN：可创建/编辑/删除看板、邀请/移除普通成员、管理卡片
 * - MEMBER：仅可查看和编辑分配给自己的卡片
 */
@Component
public class WorkspacePermissionChecker {

    private final WorkspaceMemberRepository memberRepository;

    @Autowired
    public WorkspacePermissionChecker(WorkspaceMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * 获取用户在工作区中的角色，若不是成员则抛出异常
     */
    public WorkspaceMember getMemberOrThrow(String workspaceId, String userId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new RuntimeException("您不是该工作区的成员，无权操作"));
    }

    /**
     * 检查用户是否至少拥有指定角色等级的权限
     * @param workspaceId 工作区ID
     * @param userId 用户ID
     * @param requiredRole 最低要求的角色（OWNER / ADMIN / MEMBER）
     * @throws RuntimeException 权限不足时抛出
     */
    public void checkRoleOrThrow(String workspaceId, String userId, Role requiredRole) {
        WorkspaceMember member = getMemberOrThrow(workspaceId, userId);
        Role actualRole = member.getRole();

        if (!hasSufficientRole(actualRole, requiredRole)) {
            throw new RuntimeException("权限不足，需要 " + requiredRole.name() + " 及以上角色才能执行此操作");
        }
    }

    /**
     * 检查用户是否为工作区成员（最低 MEMBER 权限）
     */
    public void checkMemberOrThrow(String workspaceId, String userId) {
        getMemberOrThrow(workspaceId, userId);
    }

    /**
     * 检查用户是否至少为 ADMIN（可管理看板和卡片）
     */
    public void checkAdminOrThrow(String workspaceId, String userId) {
        checkRoleOrThrow(workspaceId, userId, Role.ADMIN);
    }

    /**
     * 检查用户是否为 OWNER（可删除工作区和修改角色）
     */
    public void checkOwnerOrThrow(String workspaceId, String userId) {
        checkRoleOrThrow(workspaceId, userId, Role.OWNER);
    }

    /**
     * 判断实际角色是否满足要求角色
     * OWNER > ADMIN > MEMBER
     */
    private boolean hasSufficientRole(Role actual, Role required) {
        return actual.ordinal() <= required.ordinal();
    }
}
