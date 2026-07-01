package com.cotask.service;

import com.cotask.entity.Board;
import java.util.List;

public interface BoardService {
    // 创建看板
    Board createBoard(String workspaceId, String title, String userId);

    // 获取工作区的所有看板
    List<Board> getWorkspaceBoards(String workspaceId);

    // 获取看板详情（包含列表和卡片）
    Board getBoardDetail(String boardId);

    // 更新看板
    Board updateBoard(String boardId, String title, String userId);

    // 删除看板
    void deleteBoard(String boardId, String userId);
}
