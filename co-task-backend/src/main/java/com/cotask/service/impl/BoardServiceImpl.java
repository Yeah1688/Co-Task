package com.cotask.service.impl;

import com.cotask.entity.Board;
import com.cotask.entity.Workspace;
import com.cotask.repository.BoardRepository;
import com.cotask.repository.WorkspaceRepository;
import com.cotask.security.WorkspacePermissionChecker;
import com.cotask.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardServiceImpl implements BoardService {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspacePermissionChecker permissionChecker;

    @Override
    @Transactional
    public Board createBoard(String workspaceId, String title, String userId) {
        // 权限校验：ADMIN 及以上角色才能创建看板
        permissionChecker.checkAdminOrThrow(workspaceId, userId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("工作区不存在"));

        Board board = Board.builder()
                .title(title)
                .workspace(workspace)
                .build();

        return boardRepository.save(board);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Board> getWorkspaceBoards(String workspaceId) {
        return boardRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Board getBoardDetail(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("看板不存在"));
    }

    @Override
    @Transactional
    public Board updateBoard(String boardId, String title, String userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("看板不存在"));

        // 权限校验：ADMIN 及以上角色才能编辑看板
        permissionChecker.checkAdminOrThrow(board.getWorkspace().getId(), userId);

        if (title != null && !title.trim().isEmpty()) {
            board.setTitle(title);
        }

        return boardRepository.save(board);
    }

    @Override
    @Transactional
    public void deleteBoard(String boardId, String userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("看板不存在"));

        // 权限校验：仅 OWNER 才能删除看板
        permissionChecker.checkOwnerOrThrow(board.getWorkspace().getId(), userId);

        boardRepository.delete(board);
    }
}
