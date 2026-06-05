package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.AgentLog;
import com.chengxun.gamemaker.web.repository.AgentLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentLogService Tests")
class AgentLogServiceTest {

    @Mock
    private AgentLogRepository logRepository;

    @InjectMocks
    private AgentLogService agentLogService;

    @Captor
    private ArgumentCaptor<AgentLog> agentLogCaptor;

    // ---- logAsync() ----

    @Nested
    @DisplayName("logAsync()")
    class LogAsyncTests {

        @Test
        @DisplayName("should save an AgentLog with all fields populated")
        void shouldSaveAgentLogWithAllFields() {
            agentLogService.logAsync("agent-1", "TestAgent", "TASK_STARTED", "INFO",
                "Task started", "Detailed info", "proj-1", "task-1", "proceed", 500L);

            verify(logRepository).save(agentLogCaptor.capture());
            AgentLog captured = agentLogCaptor.getValue();

            assertEquals("agent-1", captured.getAgentId());
            assertEquals("TestAgent", captured.getAgentName());
            assertEquals("TASK_STARTED", captured.getAction());
            assertEquals("INFO", captured.getLevel());
            assertEquals("Task started", captured.getSummary());
            assertEquals("Detailed info", captured.getDetail());
            assertEquals("proj-1", captured.getProjectId());
            assertEquals("task-1", captured.getTaskId());
            assertEquals("proceed", captured.getDecision());
            assertEquals(500L, captured.getDurationMs());
        }

        @Test
        @DisplayName("should save an AgentLog with null optional fields")
        void shouldSaveAgentLogWithNullOptionalFields() {
            agentLogService.logAsync("agent-2", "Agent2", "AI_CALL", "INFO",
                "AI called", null, null, null, null, null);

            verify(logRepository).save(agentLogCaptor.capture());
            AgentLog captured = agentLogCaptor.getValue();

            assertEquals("agent-2", captured.getAgentId());
            assertEquals("Agent2", captured.getAgentName());
            assertEquals("AI_CALL", captured.getAction());
            assertEquals("INFO", captured.getLevel());
            assertEquals("AI called", captured.getSummary());
            assertNull(captured.getDetail());
            assertNull(captured.getProjectId());
            assertNull(captured.getTaskId());
            assertNull(captured.getDecision());
            assertNull(captured.getDurationMs());
        }

        @Test
        @DisplayName("should not throw when repository save fails")
        void shouldNotThrowWhenRepositorySaveFails() {
            when(logRepository.save(any(AgentLog.class))).thenThrow(new RuntimeException("DB error"));

            assertDoesNotThrow(() ->
                agentLogService.logAsync("agent-1", "Agent", "TASK_STARTED", "INFO",
                    "Summary", null, null, null, null, null));
        }
    }

    // ---- info() convenience methods ----

    @Nested
    @DisplayName("info()")
    class InfoTests {

        @Test
        @DisplayName("should log with INFO level using 4-param overload")
        void shouldLogInfoWithFourParams() {
            agentLogService.info("a1", "Agent", "ACTION", "summary text");

            verify(logRepository).save(agentLogCaptor.capture());
            AgentLog log = agentLogCaptor.getValue();

            assertEquals("a1", log.getAgentId());
            assertEquals("Agent", log.getAgentName());
            assertEquals("ACTION", log.getAction());
            assertEquals("INFO", log.getLevel());
            assertEquals("summary text", log.getSummary());
            assertNull(log.getDetail());
        }

        @Test
        @DisplayName("should log with INFO level using 5-param overload")
        void shouldLogInfoWithFiveParams() {
            agentLogService.info("a1", "Agent", "ACTION", "summary text", "detail text");

            verify(logRepository).save(agentLogCaptor.capture());
            AgentLog log = agentLogCaptor.getValue();

            assertEquals("INFO", log.getLevel());
            assertEquals("summary text", log.getSummary());
            assertEquals("detail text", log.getDetail());
        }
    }

    // ---- warn() ----

    @Nested
    @DisplayName("warn()")
    class WarnTests {

        @Test
        @DisplayName("should log with WARN level")
        void shouldLogWithWarnLevel() {
            agentLogService.warn("a1", "Agent", "WARNING_ACTION", "warn summary", "warn detail");

            verify(logRepository).save(agentLogCaptor.capture());
            AgentLog log = agentLogCaptor.getValue();

            assertEquals("WARN", log.getLevel());
            assertEquals("warn summary", log.getSummary());
            assertEquals("warn detail", log.getDetail());
        }
    }

    // ---- error() ----

    @Nested
    @DisplayName("error()")
    class ErrorTests {

        @Test
        @DisplayName("should log with ERROR level")
        void shouldLogWithErrLevel() {
            agentLogService.error("a1", "Agent", "ERROR_ACTION", "error summary", "error detail");

            verify(logRepository).save(agentLogCaptor.capture());
            AgentLog log = agentLogCaptor.getValue();

            assertEquals("ERROR", log.getLevel());
            assertEquals("error summary", log.getSummary());
            assertEquals("error detail", log.getDetail());
        }
    }

    // ---- decision() ----

    @Nested
    @DisplayName("decision()")
    class DecisionTests {

        @Test
        @DisplayName("should log a decision with DECISION action and INFO level")
        void shouldLogDecision() {
            agentLogService.decision("a1", "Agent", "decided to proceed", "go ahead");

            verify(logRepository).save(agentLogCaptor.capture());
            AgentLog log = agentLogCaptor.getValue();

            assertEquals("DECISION", log.getAction());
            assertEquals("INFO", log.getLevel());
            assertEquals("decided to proceed", log.getSummary());
            assertEquals("go ahead", log.getDecision());
            assertNull(log.getDetail());
        }
    }

    // ---- taskLog() ----

    @Nested
    @DisplayName("taskLog()")
    class TaskLogTests {

        @Test
        @DisplayName("should log a task event with projectId and taskId")
        void shouldLogTaskEvent() {
            agentLogService.taskLog("a1", "Agent", "TASK_RECEIVED", "New task", "t1", "p1");

            verify(logRepository).save(agentLogCaptor.capture());
            AgentLog log = agentLogCaptor.getValue();

            assertEquals("TASK_RECEIVED", log.getAction());
            assertEquals("INFO", log.getLevel());
            assertEquals("New task", log.getSummary());
            assertEquals("t1", log.getTaskId());
            assertEquals("p1", log.getProjectId());
        }
    }

    // ---- aiCall() ----

    @Nested
    @DisplayName("aiCall()")
    class AiCallTests {

        @Test
        @DisplayName("should log an AI call with duration")
        void shouldLogAiCallWithDuration() {
            agentLogService.aiCall("a1", "Agent", "AI call completed", 1234L);

            verify(logRepository).save(agentLogCaptor.capture());
            AgentLog log = agentLogCaptor.getValue();

            assertEquals("AI_CALL", log.getAction());
            assertEquals("INFO", log.getLevel());
            assertEquals("AI call completed", log.getSummary());
            assertEquals(1234L, log.getDurationMs());
        }
    }

    // ---- searchLogs() ----

    @Nested
    @DisplayName("searchLogs()")
    class SearchLogsTests {

        @Test
        @DisplayName("should delegate to repository searchLogs with correct parameters")
        void shouldDelegateSearchToRepository() {
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<AgentLog> expectedPage = new PageImpl<>(Arrays.asList(new AgentLog()));
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

            when(logRepository.searchLogs("agent-1", "TASK_STARTED", "INFO", "keyword",
                start, end, pageRequest)).thenReturn(expectedPage);

            Page<AgentLog> result = agentLogService.searchLogs("agent-1", "TASK_STARTED",
                "INFO", "keyword", start, end, 0, 10);

            assertEquals(expectedPage, result);
            verify(logRepository).searchLogs("agent-1", "TASK_STARTED", "INFO", "keyword",
                start, end, pageRequest);
        }

        @Test
        @DisplayName("should pass null filters to repository")
        void shouldPassNullFilters() {
            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<AgentLog> expectedPage = new PageImpl<>(List.of());

            when(logRepository.searchLogs(null, null, null, null, null, null, pageRequest))
                .thenReturn(expectedPage);

            Page<AgentLog> result = agentLogService.searchLogs(null, null, null, null, null, null, 0, 20);

            assertEquals(expectedPage, result);
        }
    }

    // ---- getRecentLogs() ----

    @Nested
    @DisplayName("getRecentLogs()")
    class GetRecentLogsTests {

        @Test
        @DisplayName("should delegate to repository with correct page request")
        void shouldDelegateToRepository() {
            PageRequest pageRequest = PageRequest.of(1, 25);
            Page<AgentLog> expectedPage = new PageImpl<>(List.of());

            when(logRepository.findAllByOrderByCreatedAtDesc(pageRequest)).thenReturn(expectedPage);

            Page<AgentLog> result = agentLogService.getRecentLogs(1, 25);

            assertEquals(expectedPage, result);
            verify(logRepository).findAllByOrderByCreatedAtDesc(pageRequest);
        }
    }

    // ---- getRecent50() ----

    @Nested
    @DisplayName("getRecent50()")
    class GetRecent50Tests {

        @Test
        @DisplayName("should return top 50 logs from repository")
        void shouldReturnTop50Logs() {
            AgentLog log1 = new AgentLog();
            AgentLog log2 = new AgentLog();
            List<AgentLog> expected = Arrays.asList(log1, log2);

            when(logRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(expected);

            List<AgentLog> result = agentLogService.getRecent50();

            assertEquals(2, result.size());
            assertEquals(expected, result);
            verify(logRepository).findTop50ByOrderByCreatedAtDesc();
        }
    }

    // ---- getStatsByAction() ----

    @Nested
    @DisplayName("getStatsByAction()")
    class GetStatsByActionTests {

        @Test
        @DisplayName("should return action statistics from repository")
        void shouldReturnActionStats() {
            Object[] row1 = {"TASK_STARTED", 10L};
            Object[] row2 = {"AI_CALL", 5L};
            List<Object[]> expected = Arrays.asList(row1, row2);

            when(logRepository.countByAction()).thenReturn(expected);

            List<Object[]> result = agentLogService.getStatsByAction();

            assertEquals(2, result.size());
            assertEquals("TASK_STARTED", result.get(0)[0]);
            assertEquals(10L, result.get(0)[1]);
            verify(logRepository).countByAction();
        }
    }

    // ---- getStatsByAgent() ----

    @Nested
    @DisplayName("getStatsByAgent()")
    class GetStatsByAgentTests {

        @Test
        @DisplayName("should return agent statistics from repository")
        void shouldReturnAgentStats() {
            Object[] row1 = {"agent-1", 15L};
            Object[] row2 = {"agent-2", 8L};
            List<Object[]> expected = Arrays.asList(row1, row2);

            when(logRepository.countByAgent()).thenReturn(expected);

            List<Object[]> result = agentLogService.getStatsByAgent();

            assertEquals(2, result.size());
            assertEquals("agent-1", result.get(0)[0]);
            assertEquals(15L, result.get(0)[1]);
            verify(logRepository).countByAgent();
        }
    }

    // ---- getTotalCount() ----

    @Nested
    @DisplayName("getTotalCount()")
    class GetTotalCountTests {

        @Test
        @DisplayName("should return total count from repository")
        void shouldReturnTotalCount() {
            when(logRepository.count()).thenReturn(42L);

            long count = agentLogService.getTotalCount();

            assertEquals(42L, count);
            verify(logRepository).count();
        }

        @Test
        @DisplayName("should return zero when no logs exist")
        void shouldReturnZeroWhenNoLogs() {
            when(logRepository.count()).thenReturn(0L);

            long count = agentLogService.getTotalCount();

            assertEquals(0L, count);
        }
    }
}
