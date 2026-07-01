package com.cotask.repository;

import com.cotask.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByCardIdOrderByCreatedAtAsc(String cardId);
    void deleteByCardId(String cardId);
}
