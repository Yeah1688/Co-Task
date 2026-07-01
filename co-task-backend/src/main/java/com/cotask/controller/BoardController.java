package com.cotask.controller;

import com.cotask.entity.Board;
import com.cotask.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards")
@CrossOrigin(origins = "*")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @PostMapping
    public ResponseEntity<?> createBoard(@RequestBody Map<String, String> request) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            Board board = boardService.createBoard(
                    request.get("workspaceId"),
                    request.get("title"),
                    currentUserId
            );
            return ResponseEntity.ok(Map.of(
                    "message", "看板创建成功",
                    "board", board
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<?> getWorkspaceBoards(@PathVariable String workspaceId) {
        try {
            List<Board> boards = boardService.getWorkspaceBoards(workspaceId);
            return ResponseEntity.ok(boards);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<?> getBoardDetail(@PathVariable String boardId) {
        try {
            Board board = boardService.getBoardDetail(boardId);
            return ResponseEntity.ok(board);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<?> updateBoard(
            @PathVariable String boardId,
            @RequestBody Map<String, String> request
    ) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            Board board = boardService.updateBoard(boardId, request.get("title"), currentUserId);
            return ResponseEntity.ok(Map.of(
                    "message", "看板更新成功",
                    "board", board
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(@PathVariable String boardId) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            boardService.deleteBoard(boardId, currentUserId);
            return ResponseEntity.ok(Map.of("message", "看板已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
