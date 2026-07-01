package com.cotask.service.impl;

import com.cotask.entity.Card;
import com.cotask.entity.Comment;
import com.cotask.entity.User;
import com.cotask.repository.CardRepository;
import com.cotask.repository.CommentRepository;
import com.cotask.repository.UserRepository;
import com.cotask.service.ActivityLogService;
import com.cotask.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Override
    @Transactional
    public Comment createComment(String cardId, String userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("评论内容不能为空");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Comment comment = Comment.builder()
                .content(content.trim())
                .card(card)
                .user(user)
                .build();

        Comment saved = commentRepository.save(comment);

        // 记录活动日志
        try {
            String workspaceId = card.getTaskList().getBoard().getWorkspace().getId();
            activityLogService.logActivity("COMMENT", "CARD", cardId,
                    null, null, user.getName() + " 评论了卡片 \"" + card.getTitle() + "\"",
                    workspaceId, userId);
        } catch (Exception ignored) {}

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getCardComments(String cardId) {
        return commentRepository.findByCardIdOrderByCreatedAtAsc(cardId);
    }

    @Override
    @Transactional
    public Comment updateComment(String commentId, String userId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("只有评论作者可以修改评论");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("评论内容不能为空");
        }

        comment.setContent(content.trim());
        Comment saved = commentRepository.save(comment);

        // 记录活动日志
        try {
            Card card = comment.getCard();
            String workspaceId = card.getTaskList().getBoard().getWorkspace().getId();
            activityLogService.logActivity("COMMENT", "CARD", card.getId(),
                    null, null, comment.getUser().getName() + " 修改了评论",
                    workspaceId, userId);
        } catch (Exception ignored) {}

        return saved;
    }

    @Override
    @Transactional
    public void deleteComment(String commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("只有评论作者可以删除评论");
        }

        String cardId = comment.getCard().getId();
        Card card = comment.getCard();
        String workspaceId = card.getTaskList().getBoard().getWorkspace().getId();
        String userName = comment.getUser().getName();

        commentRepository.delete(comment);

        // 记录活动日志
        try {
            activityLogService.logActivity("COMMENT", "CARD", cardId,
                    null, null, userName + " 删除了评论",
                    workspaceId, userId);
        } catch (Exception ignored) {}
    }
}
