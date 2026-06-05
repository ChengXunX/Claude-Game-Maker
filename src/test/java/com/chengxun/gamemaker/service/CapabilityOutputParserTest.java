package com.chengxun.gamemaker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CapabilityOutputParser 单元测试
 */
class CapabilityOutputParserTest {

    private CapabilityOutputParser parser;

    @BeforeEach
    void setUp() {
        parser = new CapabilityOutputParser();
        parser.updateKnownCapabilities(Set.of("createAgent", "sendTask", "commitCode"));
    }

    @Test
    void testParseJson() {
        String output = """
            {
              "thinking": "需要创建一个新Agent",
              "actions": [
                {"action": "createAgent", "params": {"name": "test", "role": "server-dev"}, "reason": "需要后端开发"}
              ],
              "response": "我将创建一个服务端开发Agent"
            }
            """;

        CapabilityOutputParser.ParseResult result = parser.parse(output);

        assertTrue(result.hasActions());
        assertEquals(1, result.getActions().size());
        assertEquals("createAgent", result.getActions().get(0).getCapabilityName());
        assertEquals("test", result.getActions().get(0).getParams().get("name"));
        assertEquals("我将创建一个服务端开发Agent", result.getResponse());
    }

    @Test
    void testParseJsonInCodeBlock() {
        String output = """
            我来分析一下：
            ```json
            {
              "actions": [
                {"action": "sendTask", "params": {"targetAgent": "server-dev", "taskContent": "实现登录"}}
              ],
              "response": "任务已发送"
            }
            ```
            """;

        CapabilityOutputParser.ParseResult result = parser.parse(output);

        assertTrue(result.hasActions());
        assertEquals("sendTask", result.getActions().get(0).getCapabilityName());
    }

    @Test
    void testParseRegex() {
        String output = """
            ACTION: createAgent
            PARAMS: name=test, role=server-dev
            REASON: 需要后端开发
            """;

        CapabilityOutputParser.ParseResult result = parser.parse(output);

        assertTrue(result.hasActions());
        assertEquals("createAgent", result.getActions().get(0).getCapabilityName());
        assertEquals("test", result.getActions().get(0).getParams().get("name"));
    }

    @Test
    void testParseNoActions() {
        String output = "这是一个普通的文本回复，没有能力调用。";

        CapabilityOutputParser.ParseResult result = parser.parse(output);

        assertFalse(result.hasActions());
        assertEquals(output, result.getResponse());
    }

    @Test
    void testParseNull() {
        CapabilityOutputParser.ParseResult result = parser.parse(null);

        assertFalse(result.hasActions());
        assertEquals("", result.getResponse());
    }

    @Test
    void testParseEmpty() {
        CapabilityOutputParser.ParseResult result = parser.parse("");

        assertFalse(result.hasActions());
        assertEquals("", result.getResponse());
    }

    @Test
    void testParseIntent() {
        String output = "我需要 createAgent 来创建新的服务端开发Agent";

        CapabilityOutputParser.ParseResult result = parser.parse(output);

        assertTrue(result.hasActions());
        assertEquals("createAgent", result.getActions().get(0).getCapabilityName());
    }

    @Test
    void testParseJsonWithMultipleActions() {
        String output = """
            {
              "actions": [
                {"action": "createAgent", "params": {"name": "test1", "role": "server-dev"}},
                {"action": "sendTask", "params": {"targetAgent": "server-dev", "taskContent": "任务1"}}
              ],
              "response": "多操作"
            }
            """;

        CapabilityOutputParser.ParseResult result = parser.parse(output);

        assertTrue(result.hasActions());
        assertEquals(2, result.getActions().size());
    }
}
