package com.cotask.controller;

import com.cotask.entity.Card;
import com.cotask.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "*")
public class CardController {

    @Autowired
    private CardService cardService;

    @PostMapping
    public ResponseEntity<?> createCard(@RequestBody Map<String, String> request) {
        try {
            Card card = cardService.createCard(
                    request.get("listId"),
                    request.get("title"),
                    request.get("description"),
                    request.get("dueDate")
            );
            return ResponseEntity.ok(Map.of(
                    "message", "卡片创建成功",
                    "card", card
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<?> getListCards(@PathVariable String listId) {
        try {
            List<Card> cards = cardService.getListCards(listId);
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<?> getCardDetail(@PathVariable String cardId) {
        try {
            Card card = cardService.getCardDetail(cardId);
            return ResponseEntity.ok(card);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<?> updateCard(
            @PathVariable String cardId,
            @RequestBody Map<String, String> request
    ) {
        try {
            Card card = cardService.updateCard(
                    cardId,
                    request.get("title"),
                    request.get("description"),
                    request.get("dueDate")
            );
            return ResponseEntity.ok(Map.of(
                    "message", "卡片更新成功",
                    "card", card
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<?> deleteCard(@PathVariable String cardId) {
        try {
            cardService.deleteCard(cardId);
            return ResponseEntity.ok(Map.of("message", "卡片已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{cardId}/move")
    public ResponseEntity<?> moveCard(
            @PathVariable String cardId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            String targetListId = (String) requestBody.get("targetListId");

            Double prevPosition = requestBody.get("prevPosition") != null
                    ? ((Number) requestBody.get("prevPosition")).doubleValue()
                    : null;
            Double nextPosition = requestBody.get("nextPosition") != null
                    ? ((Number) requestBody.get("nextPosition")).doubleValue()
                    : null;

            Card updatedCard = cardService.moveCard(cardId, targetListId, prevPosition, nextPosition);

            return ResponseEntity.ok(Map.of(
                    "id", updatedCard.getId(),
                    "title", updatedCard.getTitle(),
                    "position", updatedCard.getPosition(),
                    "listId", updatedCard.getTaskList().getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
