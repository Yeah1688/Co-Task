package com.cotask.service;

import com.cotask.entity.Card;
import java.util.List;

public interface CardService {
    // 创建卡片
    Card createCard(String listId, String title, String description, String dueDate);

    // 获取列表的所有卡片（已排序）
    List<Card> getListCards(String listId);

    // 获取卡片详情
    Card getCardDetail(String cardId);

    // 更新卡片
    Card updateCard(String cardId, String title, String description, String dueDate);

    // 删除卡片
    void deleteCard(String cardId);

    // 移动卡片（拖拽排序）
    Card moveCard(String cardId, String targetListId, Double prevPosition, Double nextPosition);
}
