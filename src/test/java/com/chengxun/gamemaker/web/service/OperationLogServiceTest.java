package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.OperationLog;
import com.chengxun.gamemaker.web.repository.OperationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperationLogService Tests")
class OperationLogServiceTest {

    @Mock
    private OperationLogRepository logRepository;

    @InjectMocks
    private OperationLogService operationLogService;

    @Captor
    private ArgumentCaptor<OperationLog> logCaptor;

    // ---- log() ----

    @Nested
    @DisplayName("log()")
    class LogTests {

        @Test
        @DisplayName("should create and save an OperationLog with all fields")
        void shouldCreateAndSaveLogWithAllFields() {
            operationLogService.log(1L, "LOGIN", "user_session", "User logged in", "192.168.1.1");

            verify(logRepository).save(logCaptor.capture());
            OperationLog captured = logCaptor.getValue();

            assertEquals(1L, captured.getUserId());
            assertEquals("LOGIN", captured.getAction());
            assertEquals("user_session", captured.getTargetName());
            assertEquals("User logged in", captured.getDetail());
            assertEquals("192.168.1.1", captured.getIpAddress());
        }

        @Test
        @DisplayName("should save log with null optional fields")
        void shouldSaveLogWithNullOptionalFields() {
            operationLogService.log(null, "ANONYMOUS", null, null, null);

            verify(logRepository).save(logCaptor.capture());
            OperationLog captured = logCaptor.getValue();

            assertNull(captured.getUserId());
            assertEquals("ANONYMOUS", captured.getAction());
            assertNull(captured.getTargetName());
            assertNull(captured.getDetail());
            assertNull(captured.getIpAddress());
        }

        @Test
        @DisplayName("should call repository save exactly once")
        void shouldCallSaveExactlyOnce() {
            operationLogService.log(1L, "ACTION", "target", "detail", "127.0.0.1");

            verify(logRepository, times(1)).save(any(OperationLog.class));
        }

        @Test
        @DisplayName("should handle various action types")
        void shouldHandleVariousActionTypes() {
            String[] actions = {"LOGIN", "LOGOUT", "CREATE", "UPDATE", "DELETE", "EXPORT"};

            for (String action : actions) {
                operationLogService.log(1L, action, "target", "detail", "127.0.0.1");
            }

            verify(logRepository, times(actions.length)).save(any(OperationLog.class));
        }
    }

    // ---- getRecentLogs() ----

    @Nested
    @DisplayName("getRecentLogs()")
    class GetRecentLogsTests {

        @Test
        @DisplayName("should return recent logs from repository")
        void shouldReturnRecentLogs() {
            OperationLog log1 = new OperationLog();
            log1.setAction("LOGIN");
            OperationLog log2 = new OperationLog();
            log2.setAction("LOGOUT");
            List<OperationLog> expected = Arrays.asList(log1, log2);

            when(logRepository.findTop20ByOrderByCreatedAtDesc()).thenReturn(expected);

            List<OperationLog> result = operationLogService.getRecentLogs();

            assertEquals(2, result.size());
            assertEquals("LOGIN", result.get(0).getAction());
            assertEquals("LOGOUT", result.get(1).getAction());
            verify(logRepository).findTop20ByOrderByCreatedAtDesc();
        }

        @Test
        @DisplayName("should return empty list when no logs exist")
        void shouldReturnEmptyListWhenNoLogs() {
            when(logRepository.findTop20ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

            List<OperationLog> result = operationLogService.getRecentLogs();

            assertTrue(result.isEmpty());
            verify(logRepository).findTop20ByOrderByCreatedAtDesc();
        }

        @Test
        @DisplayName("should delegate to correct repository method")
        void shouldDelegateToCorrectRepositoryMethod() {
            when(logRepository.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of());

            operationLogService.getRecentLogs();

            verify(logRepository).findTop20ByOrderByCreatedAtDesc();
            verifyNoMoreInteractions(logRepository);
        }
    }

    // ---- getUserLogs() ----

    @Nested
    @DisplayName("getUserLogs()")
    class GetUserLogsTests {

        @Test
        @DisplayName("should return logs for a specific user")
        void shouldReturnLogsForSpecificUser() {
            OperationLog log1 = new OperationLog();
            log1.setUserId(42L);
            log1.setAction("LOGIN");
            OperationLog log2 = new OperationLog();
            log2.setUserId(42L);
            log2.setAction("UPDATE");
            List<OperationLog> expected = Arrays.asList(log1, log2);

            when(logRepository.findByUserIdOrderByCreatedAtDesc(42L)).thenReturn(expected);

            List<OperationLog> result = operationLogService.getUserLogs(42L);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(l -> l.getUserId().equals(42L)));
            verify(logRepository).findByUserIdOrderByCreatedAtDesc(42L);
        }

        @Test
        @DisplayName("should return empty list when user has no logs")
        void shouldReturnEmptyListWhenUserHasNoLogs() {
            when(logRepository.findByUserIdOrderByCreatedAtDesc(999L)).thenReturn(Collections.emptyList());

            List<OperationLog> result = operationLogService.getUserLogs(999L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should pass userId correctly to repository")
        void shouldPassUserIdCorrectlyToRepository() {
            when(logRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            operationLogService.getUserLogs(1L);

            verify(logRepository).findByUserIdOrderByCreatedAtDesc(1L);
        }

        @Test
        @DisplayName("should handle different user IDs independently")
        void shouldHandleDifferentUserIds() {
            OperationLog user1Log = new OperationLog();
            user1Log.setUserId(1L);
            OperationLog user2Log = new OperationLog();
            user2Log.setUserId(2L);

            when(logRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Arrays.asList(user1Log));
            when(logRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(Arrays.asList(user2Log));

            List<OperationLog> result1 = operationLogService.getUserLogs(1L);
            List<OperationLog> result2 = operationLogService.getUserLogs(2L);

            assertEquals(1, result1.size());
            assertEquals(1L, result1.get(0).getUserId());
            assertEquals(1, result2.size());
            assertEquals(2L, result2.get(0).getUserId());
        }
    }
}
