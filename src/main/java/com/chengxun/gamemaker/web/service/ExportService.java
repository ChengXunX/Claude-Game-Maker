package com.chengxun.gamemaker.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 数据导出服务
 * 支持 CSV、JSON 格式导出
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * 导出为 CSV 格式
     *
     * @param headers 表头列表
     * @param rows    数据行（每行是一个 Map，key 对应 header）
     * @return CSV 字节数组
     */
    public byte[] exportCsv(List<String> headers, List<Map<String, Object>> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

            // 写入 BOM（Excel 兼容）
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            // 写入表头
            writer.println(String.join(",", headers.stream()
                .map(this::escapeCsvField)
                .toArray(String[]::new)));

            // 写入数据行
            for (Map<String, Object> row : rows) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < headers.size(); i++) {
                    if (i > 0) line.append(",");
                    Object value = row.get(headers.get(i));
                    line.append(escapeCsvField(value != null ? value.toString() : ""));
                }
                writer.println(line.toString());
            }

            writer.flush();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("CSV export failed", e);
            return new byte[0];
        }
    }

    /**
     * 导出为 JSON 格式
     */
    public byte[] exportJson(List<Map<String, Object>> data) {
        try {
            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < data.size(); i++) {
                if (i > 0) json.append(",\n");
                json.append("  {");
                int j = 0;
                for (Map.Entry<String, Object> entry : data.get(i).entrySet()) {
                    if (j > 0) json.append(",");
                    json.append("\n    \"").append(escapeJson(entry.getKey())).append("\": ");
                    Object value = entry.getValue();
                    if (value == null) {
                        json.append("null");
                    } else if (value instanceof Number || value instanceof Boolean) {
                        json.append(value);
                    } else {
                        json.append("\"").append(escapeJson(value.toString())).append("\"");
                    }
                    j++;
                }
                json.append("\n  }");
            }
            json.append("\n]");
            return json.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("JSON export failed", e);
            return new byte[0];
        }
    }

    /**
     * 生成导出文件名
     */
    public String generateFilename(String prefix, String extension) {
        return String.format("%s_%s.%s", prefix, LocalDateTime.now().format(DATE_FORMAT), extension);
    }

    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
