package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.model.AgentMessage;
import java.util.Map;

/**
 * 消息总线接口
 * 定义Agent间通信的标准接口
 *
 * 主要功能：
 * - Agent注册和注销
 * - 消息发送和广播
 * - 消息统计
 *
 * @author chengxun
 * @since 1.0.0
 */
public interface MessageBusInterface {

    /**
     * 注册Agent到消息总线（无项目关联，用于全局 Agent）
     *
     * @param agent 要注册的Agent
     */
    void registerAgent(Agent agent);

    /**
     * 注册Agent到消息总线（项目级隔离）
     * 消息只在同一项目内的 Agent 之间传递
     *
     * @param agent 要注册的Agent
     * @param projectId 所属项目 ID
     */
    void registerAgent(Agent agent, String projectId);

    /**
     * 从消息总线注销Agent
     *
     * @param agentId Agent ID（运行时 ID: projectId:agentRole）
     */
    void unregisterAgent(String agentId);

    /**
     * 发送消息
     * 如果toAgentId为空，则广播给同项目的所有Agent
     * 消息只在同一项目内传递，不会跨项目
     *
     * @param message 消息
     */
    void send(AgentMessage message);

    /**
     * 获取已注册的Agent列表（跨项目，管理员用）
     *
     * @return Agent映射表
     */
    Map<String, Agent> getAgents();

    /**
     * 获取指定项目的已注册Agent列表
     *
     * @param projectId 项目 ID
     * @return 该项目下的 Agent 映射表
     */
    Map<String, Agent> getProjectAgents(String projectId);
}
