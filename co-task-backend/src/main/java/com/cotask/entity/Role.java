package com.cotask.entity;

public enum Role {
    OWNER,   // 创建者：拥有工作区最高权限（删除工作区、解散成员、管理看板）
    ADMIN,   // 管理员：可管理看板、邀请/移除普通成员
    MEMBER   // 普通成员：仅能对分配给自己的看板和卡片进行编辑
}