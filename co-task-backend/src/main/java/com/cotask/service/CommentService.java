package com.cotask.service;

import com.cotask.entity.Comment;
import java.util.List;
import java.util.Map;

public interface CommentService {
    Comment createComment(String cardId, String userId, String content);
    List<Comment> getCardComments(String cardId);
    Comment updateComment(String commentId, String userId, String content);
    void deleteComment(String commentId, String userId);
}
