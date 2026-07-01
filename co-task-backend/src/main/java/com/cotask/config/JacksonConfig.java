package com.cotask.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化配置
 * 注册 Hibernate6Module 以正确处理 Hibernate 懒加载代理的 JSON 序列化
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // 启用强制懒加载：Jackson 序列化时自动触发 Hibernate 懒加载查询
        // 配合 spring.jpa.open-in-view=true（默认），Session 在视图渲染期间保持打开
        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        return module;
    }
}
