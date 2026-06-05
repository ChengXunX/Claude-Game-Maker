package com.chengxun.gamemaker.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExportService 单元测试
 */
class ExportServiceTest {

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService();
    }

    @Test
    void testExportCsv() {
        List<String> headers = List.of("Name", "Age", "City");
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(Map.of("Name", "Alice", "Age", 30, "City", "Beijing"));
        rows.add(Map.of("Name", "Bob", "Age", 25, "City", "Shanghai"));

        byte[] result = exportService.exportCsv(headers, rows);

        assertNotNull(result);
        String csv = new String(result);
        assertTrue(csv.contains("Name,Age,City"));
        assertTrue(csv.contains("Alice"));
        assertTrue(csv.contains("Bob"));
    }

    @Test
    void testExportCsvWithSpecialChars() {
        List<String> headers = List.of("Content");
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(Map.of("Content", "hello, world"));

        byte[] result = exportService.exportCsv(headers, rows);
        String csv = new String(result);

        assertTrue(csv.contains("\"hello, world\""));
    }

    @Test
    void testExportCsvEmpty() {
        byte[] result = exportService.exportCsv(List.of("A"), List.of());

        assertNotNull(result);
        assertTrue(new String(result).contains("A"));
    }

    @Test
    void testExportJson() {
        List<Map<String, Object>> data = List.of(
            Map.of("name", "Alice", "age", 30),
            Map.of("name", "Bob", "age", 25)
        );

        byte[] result = exportService.exportJson(data);

        assertNotNull(result);
        String json = new String(result);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
        assertTrue(json.contains("["));
        assertTrue(json.contains("]"));
    }

    @Test
    void testExportJsonEmpty() {
        byte[] result = exportService.exportJson(List.of());

        assertNotNull(result);
        assertTrue(new String(result).contains("["));
        assertTrue(new String(result).contains("]"));
    }

    @Test
    void testGenerateFilename() {
        String filename = exportService.generateFilename("test", "csv");

        assertNotNull(filename);
        assertTrue(filename.startsWith("test_"));
        assertTrue(filename.endsWith(".csv"));
    }

    @Test
    void testExportCsvWithNull() {
        List<String> headers = List.of("Name", "Value");
        List<Map<String, Object>> rows = List.of(Map.of("Name", "test"));

        byte[] result = exportService.exportCsv(headers, rows);

        assertNotNull(result);
    }
}
