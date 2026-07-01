package com.cotask.controller;

import com.cotask.entity.Workspace;
import com.cotask.entity.WorkspaceMember;
import com.cotask.repository.WorkspaceMemberRepository;
import com.cotask.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workspaces")
@CrossOrigin(origins = "*")
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @PostMapping
    public ResponseEntity<?> createWorkspace(@RequestBody Map<String, String> request) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            Workspace ws = workspaceService.createWorkspace(
                    request.get("name"),
                    request.get("description"),
                    currentUserId
            );
            return ResponseEntity.ok(Map.of(
                    "message", "工作区创建成功",
                    "workspaceId", ws.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserWorkspaces() {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            List<Workspace> workspaces = workspaceService.getUserWorkspaces(currentUserId);
            return ResponseEntity.ok(workspaces);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    @GetMapping("/my")
    public ResponseEntity<?> getMyWorkspaces() {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            // 获取用户所有成员关系（包含工作区和角色信息）
            List<WorkspaceMember> memberships = memberRepository.findByUserId(currentUserId);

            // 转换为前端期望的 DTO 格式：[{id, name, role}, ...]
            List<Map<String, Object>> result = memberships.stream()
                    .map(m -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", m.getWorkspace().getId());
                        dto.put("name", m.getWorkspace().getName());
                        dto.put("role", m.getRole().name());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    @GetMapping("/{workspaceId}")
    public ResponseEntity<?> getWorkspaceDetail(@PathVariable String workspaceId) {
        try {
            Workspace workspace = workspaceService.getWorkspaceDetail(workspaceId);
            return ResponseEntity.ok(workspace);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{workspaceId}")
    public ResponseEntity<?> updateWorkspace(
            @PathVariable String workspaceId,
            @RequestBody Map<String, String> request
    ) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            Workspace workspace = workspaceService.updateWorkspace(
                    workspaceId,
                    request.get("name"),
                    request.get("description"),
                    currentUserId
            );
            return ResponseEntity.ok(Map.of(
                    "message", "工作区更新成功",
                    "workspace", workspace
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<?> deleteWorkspace(@PathVariable String workspaceId) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            workspaceService.deleteWorkspace(workspaceId, currentUserId);
            return ResponseEntity.ok(Map.of("message", "工作区已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{workspaceId}/invite-link")
    public ResponseEntity<?> getInviteLink(@PathVariable String workspaceId) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            String code = workspaceService.generateInviteCode(workspaceId, currentUserId);
            return ResponseEntity.ok(Map.of("inviteCode", code, "inviteUrl", "http://localhost:5173/join?code=" + code));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinWorkspace(@RequestBody Map<String, String> request) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            WorkspaceMember member = workspaceService.joinWorkspaceByCode(request.get("inviteCode"), currentUserId);
            return ResponseEntity.ok(Map.of(
                    "message", "成功加入工作区",
                    "workspaceId", member.getWorkspace().getId(),
                    "role", member.getRole().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<?> getWorkspaceMembers(@PathVariable String workspaceId) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            if (!workspaceService.checkPermission(workspaceId, currentUserId, "MEMBER")) {
                return ResponseEntity.badRequest().body(Map.of("message", "您不是该工作区的成员"));
            }

            Workspace workspace = workspaceService.getWorkspaceDetail(workspaceId);
            List<WorkspaceMember> members = workspace.getMembers();
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{workspaceId}/members/{targetUserId}")
    public ResponseEntity<?> removeMember(
            @PathVariable String workspaceId,
            @PathVariable String targetUserId
    ) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            workspaceService.removeMember(workspaceId, targetUserId, currentUserId);
            return ResponseEntity.ok(Map.of("message", "成员已移除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{workspaceId}/members/{targetUserId}/role")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable String workspaceId,
            @PathVariable String targetUserId,
            @RequestBody Map<String, String> request
    ) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            WorkspaceMember member = workspaceService.updateMemberRole(
                    workspaceId,
                    targetUserId,
                    request.get("role"),
                    currentUserId
            );
            return ResponseEntity.ok(Map.of(
                    "message", "成员角色更新成功",
                    "member", member
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
