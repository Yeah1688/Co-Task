package com.cotask.service.impl;

import com.cotask.entity.*;
import com.cotask.repository.*;
import com.cotask.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TaskListRepository taskListRepository;

    @Override
    @Transactional
    public Workspace createWorkspace(String name, String description, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 1. 创建工作区本体
        Workspace workspace = Workspace.builder()
                .name(name)
                .description(description)
                .build();
        workspace.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // 2. 将创建者绑定为 OWNER
        WorkspaceMember owner = WorkspaceMember.builder()
                .workspace(savedWorkspace)
                .user(user)
                .role(Role.OWNER)
                .build();
        memberRepository.save(owner);

        // 3. 自动创建默认看板
        createDefaultBoards(savedWorkspace);

        return savedWorkspace;
    }

    /**
     * 为新工作区创建默认看板和任务列表
     */
    private void createDefaultBoards(Workspace workspace) {
        // 创建默认看板：项目任务管理
        Board defaultBoard = Board.builder()
                .title("项目任务管理")
                .workspace(workspace)
                .build();
        Board savedBoard = boardRepository.save(defaultBoard);

        // 创建默认任务列表
        createDefaultTaskLists(savedBoard);
    }

    /**
     * 为看板创建默认任务列表
     */
    private void createDefaultTaskLists(Board board) {
        String[] defaultListTitles = {"待办事项", "进行中", "审核中", "已完成"};

        for (int i = 0; i < defaultListTitles.length; i++) {
            TaskList taskList = TaskList.builder()
                    .title(defaultListTitles[i])
                    .position((double) (i + 1))
                    .board(board)
                    .build();
            taskListRepository.save(taskList);
        }
    }

    @Override
    @Transactional
    public String generateInviteCode(String workspaceId, String operatorId) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, operatorId)
                .orElseThrow(() -> new RuntimeException("你不在该工作区中"));

        if (member.getRole() == Role.MEMBER) {
            throw new RuntimeException("权限不足，只有管理员能生成邀请链接");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("工作区不存在"));

        String newCode = UUID.randomUUID().toString().substring(0, 8);
        workspace.setInviteCode(newCode);
        workspaceRepository.save(workspace);

        return newCode;
    }

    @Override
    @Transactional
    public WorkspaceMember joinWorkspaceByCode(String inviteCode, String userId) {
        Workspace workspace = workspaceRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("无效的邀请链接或邀请码已失效"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (memberRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId).isPresent()) {
            throw new RuntimeException("您已经是该工作区的成员了");
        }

        WorkspaceMember newMember = WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(Role.MEMBER)
                .build();

        return memberRepository.save(newMember);
    }

    @Override
    public boolean checkPermission(String workspaceId, String userId, String requiredRole) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId).orElse(null);
        if (member == null) return false;

        if ("OWNER".equals(requiredRole)) {
            return member.getRole() == Role.OWNER;
        } else if ("ADMIN".equals(requiredRole)) {
            return member.getRole() == Role.OWNER || member.getRole() == Role.ADMIN;
        } else {
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workspace> getUserWorkspaces(String userId) {
        List<WorkspaceMember> memberships = memberRepository.findByUserId(userId);
        return memberships.stream()
                .map(WorkspaceMember::getWorkspace)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Workspace getWorkspaceDetail(String workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("工作区不存在"));
    }

    @Override
    @Transactional
    public Workspace updateWorkspace(String workspaceId, String name, String description, String operatorId) {
        if (!checkPermission(workspaceId, operatorId, "ADMIN")) {
            throw new RuntimeException("权限不足，只有管理员能修改工作区信息");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("工作区不存在"));

        if (name != null && !name.trim().isEmpty()) {
            workspace.setName(name);
        }
        if (description != null) {
            workspace.setDescription(description);
        }

        return workspaceRepository.save(workspace);
    }

    @Override
    @Transactional
    public void deleteWorkspace(String workspaceId, String operatorId) {
        if (!checkPermission(workspaceId, operatorId, "OWNER")) {
            throw new RuntimeException("权限不足，只有所有者能删除工作区");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("工作区不存在"));

        workspaceRepository.delete(workspace);
    }

    @Override
    @Transactional
    public void removeMember(String workspaceId, String targetUserId, String operatorId) {
        if (!checkPermission(workspaceId, operatorId, "ADMIN")) {
            throw new RuntimeException("权限不足，只有管理员能移除成员");
        }

        WorkspaceMember targetMember = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new RuntimeException("该用户不是工作区成员"));

        if (targetMember.getRole() == Role.OWNER) {
            throw new RuntimeException("不能移除工作区所有者");
        }

        memberRepository.delete(targetMember);
    }

    @Override
    @Transactional
    public WorkspaceMember updateMemberRole(String workspaceId, String targetUserId, String newRole, String operatorId) {
        if (!checkPermission(workspaceId, operatorId, "OWNER")) {
            throw new RuntimeException("权限不足，只有所有者能修改成员角色");
        }

        WorkspaceMember targetMember = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new RuntimeException("该用户不是工作区成员"));

        if (targetMember.getRole() == Role.OWNER) {
            throw new RuntimeException("不能修改所有者的角色");
        }

        try {
            Role role = Role.valueOf(newRole.toUpperCase());
            targetMember.setRole(role);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的角色类型: " + newRole);
        }

        return memberRepository.save(targetMember);
    }
}
