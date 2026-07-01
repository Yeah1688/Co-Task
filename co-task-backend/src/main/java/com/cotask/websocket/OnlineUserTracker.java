
package com.cotask.websocket;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户追踪器
 * 用于记录哪些用户正在哪些工作区中活跃
 */
@Component
public class OnlineUserTracker {

    // 使用 ConcurrentHashMap 保证线程安全
    // 结构：workspaceId -> Set<userId>
    private final ConcurrentHashMap<String, Set<String>> workspaceUsers = new ConcurrentHashMap<>();

    /**
     * 添加用户到工作区
     * @param workspaceId 工作区ID
     * @param userId 用户ID
     */
    public void addUserToWorkspace(String workspaceId, String userId) {
        workspaceUsers.computeIfAbsent(workspaceId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    /**
     * 从工作区移除用户
     * @param workspaceId 工作区ID
     * @param userId 用户ID
     */
    public void removeUserFromWorkspace(String workspaceId, String userId) {
        Set<String> users = workspaceUsers.get(workspaceId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                workspaceUsers.remove(workspaceId);
            }
        }
    }

    /**
     * 获取工作区的所有在线用户
     * @param workspaceId 工作区ID
     * @return 用户ID集合
     */
    public Set<String> getUsersInWorkspace(String workspaceId) {
        Set<String> users = workspaceUsers.get(workspaceId);
        return users != null ? Collections.unmodifiableSet(users) : Collections.emptySet();
    }

    /**
     * 获取所有用户所在的工作区
     * @return 工作区ID集合
     */
    public Set<String> getAllActiveWorkspaces() {
        return Collections.unmodifiableSet(workspaceUsers.keySet());
    }

    /**
     * 获取用户所在的所有工作区
     * @param userId 用户ID
     * @return 工作区ID集合
     */
    public Set<String> getWorkspacesOfUser(String userId) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : workspaceUsers.entrySet()) {
            if (entry.getValue().contains(userId)) {
                result.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
