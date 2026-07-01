package com.cotask.controller;

import com.cotask.entity.Comment;
import com.cotask.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody Map<String, String> request) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            Comment comment = commentService.createComment(
                    request.get("cardId"),
                    currentUserId,
                    request.get("content")
            );
            return ResponseEntity.ok(Map.of(
                    "message", "评论创建成功",
                    "comment", comment
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/card/{cardId}")
    public ResponseEntity<?> getCardComments(@PathVariable String cardId) {
        try {
            List<Comment> comments = commentService.getCardComments(cardId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable String commentId,
            @RequestBody Map<String, String> request
    ) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            Comment comment = commentService.updateComment(
                    commentId,
                    currentUserId,
                    request.get("content")
            );
            return ResponseEntity.ok(Map.of(
                    "message", "评论更新成功",
                    "comment", comment
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable String commentId) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            commentService.deleteComment(commentId, currentUserId);
            return ResponseEntity.ok(Map.of("message", "评论已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
