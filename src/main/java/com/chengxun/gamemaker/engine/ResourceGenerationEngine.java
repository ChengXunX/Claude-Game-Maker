package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 资源生成引擎
 * 统一调用各类资源生成 API（音频、图片、视频等），屏蔽不同服务商的差异
 *
 * 支持的资源类型：
 * - AUDIO_MUSIC: 背景音乐（Suno / Udio / 火山引擎）
 * - AUDIO_SFX: 音效（ElevenLabs / 自建模型）
 * - IMAGE_SPRITE: 2D 精灵/贴图（DALL-E / Stable Diffusion / 通义万相）
 * - IMAGE_UI: UI 素材（同上）
 * - SHADER_CODE: Shader 代码（复用文本引擎，由调用方处理）
 *
 * 设计原则：
 * - 统一接口，不同后端通过 ProviderAdapter 适配
 * - 异步生成，返回文件路径
 * - 支持自定义 Provider 扩展
 *
 * @author chengxun
 * @since 4.0.0
 */
@Component
public class ResourceGenerationEngine {

    private static final Logger log = LoggerFactory.getLogger(ResourceGenerationEngine.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 已注册的 Provider 适配器 */
    private final Map<String, ProviderAdapter> providers = new LinkedHashMap<>();

    public ResourceGenerationEngine(AppConfig appConfig) {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build();

        // 注册内置 Provider
        providers.put("suno", new SunoAdapter());
        providers.put("elevenlabs", new ElevenLabsAdapter());
        providers.put("dalle", new DallEAdapter());
        providers.put("stability", new StabilityAdapter());
        providers.put("zhipu_image", new ZhipuImageAdapter());
        providers.put("openai_compatible", new OpenAiResourceAdapter());
    }

    /**
     * 生成资源
     *
     * @param request 资源生成请求
     * @return 生成结果
     */
    public ResourceResult generateResource(ResourceRequest request) {
        String providerName = resolveProvider(request);
        ProviderAdapter adapter = providers.get(providerName);
        if (adapter == null) {
            return ResourceResult.failure("不支持的资源提供商: " + providerName);
        }

        log.info("开始生成资源: type={}, provider={}, prompt={}",
            request.resourceType, providerName,
            request.prompt.length() > 100 ? request.prompt.substring(0, 100) + "..." : request.prompt);

        try {
            ResourceResult result = adapter.generate(request, httpClient, objectMapper);
            if (result.success) {
                log.info("资源生成成功: {}", result.filePath);
            } else {
                log.warn("资源生成失败: {}", result.error);
            }
            return result;
        } catch (Exception e) {
            log.error("资源生成异常: provider={}", providerName, e);
            return ResourceResult.failure("生成异常: " + e.getMessage());
        }
    }

    /**
     * 根据请求参数解析应该使用哪个 Provider
     */
    private String resolveProvider(ResourceRequest request) {
        // 1. 显式指定
        if (request.provider != null && !request.provider.isEmpty()) {
            return request.provider;
        }
        // 2. 根据资源类型和 API URL 推断
        if (request.apiUrl != null) {
            String lower = request.apiUrl.toLowerCase();
            if (lower.contains("suno")) return "suno";
            if (lower.contains("elevenlabs")) return "elevenlabs";
            if (lower.contains("dall-e") || lower.contains("openai")) return "dalle";
            if (lower.contains("stability") || lower.contains("stable-diffusion")) return "stability";
            if (lower.contains("bigmodel") || lower.contains("zhipu")) return "zhipu_image";
        }
        // 3. 根据资源类型默认
        return switch (request.resourceType) {
            case AUDIO_MUSIC -> "suno";
            case AUDIO_SFX -> "elevenlabs";
            case IMAGE_SPRITE, IMAGE_UI -> "dalle";
            default -> "openai_compatible";
        };
    }

    /**
     * 获取所有可用的 Provider 名称
     */
    public Set<String> getAvailableProviders() {
        return providers.keySet();
    }

    // ===== 数据模型 =====

    public enum ResourceType {
        AUDIO_MUSIC,    // 背景音乐
        AUDIO_SFX,      // 音效
        IMAGE_SPRITE,   // 2D 精灵/贴图
        IMAGE_UI,       // UI 素材
        SHADER_CODE,    // Shader 代码
        OTHER           // 其他
    }

    /**
     * 资源生成请求
     */
    public static class ResourceRequest {
        public ResourceType resourceType;
        public String prompt;           // 生成提示词
        public String apiUrl;           // API 地址
        public String apiKey;           // API Key
        public String model;            // 模型名称
        public String provider;         // 指定 provider（可选）
        public String outputDir;        // 输出目录
        public String outputFileName;   // 输出文件名（可选）
        public Map<String, Object> params = new HashMap<>(); // 额外参数

        public static ResourceRequest of(ResourceType type, String prompt, String apiUrl, String apiKey) {
            ResourceRequest req = new ResourceRequest();
            req.resourceType = type;
            req.prompt = prompt;
            req.apiUrl = apiUrl;
            req.apiKey = apiKey;
            return req;
        }
    }

    /**
     * 资源生成结果
     */
    public static class ResourceResult {
        public boolean success;
        public String filePath;         // 本地文件路径
        public String fileUrl;          // 远程 URL（如有）
        public String mimeType;
        public long fileSize;
        public Map<String, Object> metadata = new HashMap<>();
        public String error;

        public static ResourceResult success(String filePath) {
            ResourceResult r = new ResourceResult();
            r.success = true;
            r.filePath = filePath;
            return r;
        }

        public static ResourceResult failure(String error) {
            ResourceResult r = new ResourceResult();
            r.success = false;
            r.error = error;
            return r;
        }
    }

    // ===== Provider 适配器接口 =====

    public interface ProviderAdapter {
        ResourceResult generate(ResourceRequest request, OkHttpClient httpClient, ObjectMapper mapper) throws Exception;
    }

    // ===== 内置 Provider 实现 =====

    /**
     * Suno 音乐生成适配器
     */
    private static class SunoAdapter implements ProviderAdapter {
        @Override
        public ResourceResult generate(ResourceRequest request, OkHttpClient httpClient, ObjectMapper mapper) throws Exception {
            String baseUrl = request.apiUrl != null ? request.apiUrl : "https://api.suno.ai";

            // 构建请求体
            Map<String, Object> body = new HashMap<>();
            body.put("prompt", request.prompt);
            body.put("make_instrumental", request.params.getOrDefault("instrumental", false));
            if (request.params.containsKey("style")) body.put("style", request.params.get("style"));
            if (request.params.containsKey("title")) body.put("title", request.params.get("title"));

            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/v1/generate")
                .addHeader("Authorization", "Bearer " + request.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    return ResourceResult.failure("Suno API 错误: " + response.code() + " " + response.body().string());
                }
                JsonNode root = mapper.readTree(response.body().string());

                // Suno 返回音频 URL，需要下载
                String audioUrl = root.has("audio_url") ? root.get("audio_url").asText() :
                    root.has("data") && root.get("data").isArray() && root.get("data").size() > 0 ?
                        root.get("data").get(0).get("audio_url").asText() : null;

                if (audioUrl == null) {
                    return ResourceResult.failure("Suno 未返回音频 URL");
                }

                // 下载音频文件
                String fileName = request.outputFileName != null ? request.outputFileName :
                    "music_" + System.currentTimeMillis() + ".mp3";
                String outputDir = request.outputDir != null ? request.outputDir : "/tmp/resources";
                Files.createDirectories(Path.of(outputDir));
                String filePath = outputDir + "/" + fileName;
                downloadFile(httpClient, audioUrl, filePath);

                ResourceResult result = ResourceResult.success(filePath);
                result.fileUrl = audioUrl;
                result.mimeType = "audio/mpeg";
                result.metadata.put("provider", "suno");
                return result;
            }
        }
    }

    /**
     * ElevenLabs 音效生成适配器
     */
    private static class ElevenLabsAdapter implements ProviderAdapter {
        @Override
        public ResourceResult generate(ResourceRequest request, OkHttpClient httpClient, ObjectMapper mapper) throws Exception {
            String baseUrl = request.apiUrl != null ? request.apiUrl : "https://api.elevenlabs.io";

            Map<String, Object> body = new HashMap<>();
            body.put("text", request.prompt);
            body.put("model_id", request.model != null ? request.model : "eleven_multilingual_v2");

            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/v1/text-to-speech/" + request.params.getOrDefault("voice_id", "21m00Tcm4TlvDq8ikWAM"))
                .addHeader("xi-api-key", request.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    return ResourceResult.failure("ElevenLabs API 错误: " + response.code());
                }
                String fileName = request.outputFileName != null ? request.outputFileName :
                    "sfx_" + System.currentTimeMillis() + ".mp3";
                String outputDir = request.outputDir != null ? request.outputDir : "/tmp/resources";
                Files.createDirectories(Path.of(outputDir));
                String filePath = outputDir + "/" + fileName;
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(response.body().bytes());
                }
                ResourceResult result = ResourceResult.success(filePath);
                result.mimeType = "audio/mpeg";
                result.metadata.put("provider", "elevenlabs");
                return result;
            }
        }
    }

    /**
     * DALL-E 图片生成适配器
     */
    private static class DallEAdapter implements ProviderAdapter {
        @Override
        public ResourceResult generate(ResourceRequest request, OkHttpClient httpClient, ObjectMapper mapper) throws Exception {
            String baseUrl = request.apiUrl != null ? request.apiUrl : "https://api.openai.com";

            Map<String, Object> body = new HashMap<>();
            body.put("model", request.model != null ? request.model : "dall-e-3");
            body.put("prompt", request.prompt);
            body.put("size", request.params.getOrDefault("size", "1024x1024"));
            body.put("quality", request.params.getOrDefault("quality", "standard"));
            body.put("n", 1);

            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/v1/images/generations")
                .addHeader("Authorization", "Bearer " + request.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    return ResourceResult.failure("DALL-E API 错误: " + response.code() + " " + response.body().string());
                }
                JsonNode root = mapper.readTree(response.body().string());
                String imageUrl = root.get("data").get(0).get("url").asText();

                String fileName = request.outputFileName != null ? request.outputFileName :
                    "image_" + System.currentTimeMillis() + ".png";
                String outputDir = request.outputDir != null ? request.outputDir : "/tmp/resources";
                Files.createDirectories(Path.of(outputDir));
                String filePath = outputDir + "/" + fileName;
                downloadFile(httpClient, imageUrl, filePath);

                ResourceResult result = ResourceResult.success(filePath);
                result.fileUrl = imageUrl;
                result.mimeType = "image/png";
                result.metadata.put("provider", "dalle");
                return result;
            }
        }
    }

    /**
     * Stability AI (Stable Diffusion) 适配器
     */
    private static class StabilityAdapter implements ProviderAdapter {
        @Override
        public ResourceResult generate(ResourceRequest request, OkHttpClient httpClient, ObjectMapper mapper) throws Exception {
            String baseUrl = request.apiUrl != null ? request.apiUrl : "https://api.stability.ai";

            Map<String, Object> body = new HashMap<>();
            body.put("text_prompts", List.of(Map.of("text", request.prompt, "weight", 1)));
            body.put("cfg_scale", request.params.getOrDefault("cfg_scale", 7));
            body.put("steps", request.params.getOrDefault("steps", 30));
            body.put("width", request.params.getOrDefault("width", 1024));
            body.put("height", request.params.getOrDefault("height", 1024));

            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image")
                .addHeader("Authorization", "Bearer " + request.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    return ResourceResult.failure("Stability API 错误: " + response.code());
                }
                JsonNode root = mapper.readTree(response.body().string());
                String base64 = root.get("artifacts").get(0).get("base64").asText();

                String fileName = request.outputFileName != null ? request.outputFileName :
                    "sprite_" + System.currentTimeMillis() + ".png";
                String outputDir = request.outputDir != null ? request.outputDir : "/tmp/resources";
                Files.createDirectories(Path.of(outputDir));
                String filePath = outputDir + "/" + fileName;
                Files.write(Path.of(filePath), Base64.getDecoder().decode(base64));

                ResourceResult result = ResourceResult.success(filePath);
                result.mimeType = "image/png";
                result.metadata.put("provider", "stability");
                return result;
            }
        }
    }

    /**
     * 智谱图片生成适配器
     */
    private static class ZhipuImageAdapter implements ProviderAdapter {
        @Override
        public ResourceResult generate(ResourceRequest request, OkHttpClient httpClient, ObjectMapper mapper) throws Exception {
            String baseUrl = request.apiUrl != null ? request.apiUrl : "https://open.bigmodel.cn";

            Map<String, Object> body = new HashMap<>();
            body.put("model", request.model != null ? request.model : "cogview-3");
            body.put("prompt", request.prompt);
            body.put("size", request.params.getOrDefault("size", "1024x1024"));

            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/paas/v4/images/generations")
                .addHeader("Authorization", "Bearer " + request.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    return ResourceResult.failure("智谱 API 错误: " + response.code() + " " + response.body().string());
                }
                JsonNode root = mapper.readTree(response.body().string());
                String imageUrl = root.get("data").get(0).get("url").asText();

                String fileName = request.outputFileName != null ? request.outputFileName :
                    "image_" + System.currentTimeMillis() + ".png";
                String outputDir = request.outputDir != null ? request.outputDir : "/tmp/resources";
                Files.createDirectories(Path.of(outputDir));
                String filePath = outputDir + "/" + fileName;
                downloadFile(httpClient, imageUrl, filePath);

                ResourceResult result = ResourceResult.success(filePath);
                result.fileUrl = imageUrl;
                result.mimeType = "image/png";
                result.metadata.put("provider", "zhipu");
                return result;
            }
        }
    }

    /**
     * OpenAI 兼容资源生成适配器（通用兜底）
     */
    private static class OpenAiResourceAdapter implements ProviderAdapter {
        @Override
        public ResourceResult generate(ResourceRequest request, OkHttpClient httpClient, ObjectMapper mapper) throws Exception {
            // 通用 OpenAI 兼容格式，根据 resourceType 选择 endpoint
            String baseUrl = request.apiUrl != null ? request.apiUrl : "https://api.openai.com";
            String endpoint = switch (request.resourceType) {
                case IMAGE_SPRITE, IMAGE_UI -> "/v1/images/generations";
                default -> "/v1/chat/completions";
            };

            Map<String, Object> body = new HashMap<>();
            if (endpoint.contains("images")) {
                body.put("model", request.model != null ? request.model : "dall-e-3");
                body.put("prompt", request.prompt);
                body.put("size", request.params.getOrDefault("size", "1024x1024"));
                body.put("n", 1);
            } else {
                body.put("model", request.model != null ? request.model : "gpt-4");
                body.put("messages", List.of(Map.of("role", "user", "content", request.prompt)));
            }

            Request httpRequest = new Request.Builder()
                .url(baseUrl + endpoint)
                .addHeader("Authorization", "Bearer " + request.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    return ResourceResult.failure("API 错误: " + response.code() + " " + response.body().string());
                }
                JsonNode root = mapper.readTree(response.body().string());

                if (endpoint.contains("images")) {
                    String imageUrl = root.get("data").get(0).get("url").asText();
                    String fileName = request.outputFileName != null ? request.outputFileName :
                        "resource_" + System.currentTimeMillis() + ".png";
                    String outputDir = request.outputDir != null ? request.outputDir : "/tmp/resources";
                    Files.createDirectories(Path.of(outputDir));
                    String filePath = outputDir + "/" + fileName;
                    downloadFile(httpClient, imageUrl, filePath);
                    ResourceResult result = ResourceResult.success(filePath);
                    result.fileUrl = imageUrl;
                    result.mimeType = "image/png";
                    return result;
                } else {
                    // 文本响应，保存为文件
                    String content = root.get("choices").get(0).get("message").get("content").asText();
                    String fileName = request.outputFileName != null ? request.outputFileName :
                        "resource_" + System.currentTimeMillis() + ".txt";
                    String outputDir = request.outputDir != null ? request.outputDir : "/tmp/resources";
                    Files.createDirectories(Path.of(outputDir));
                    String filePath = outputDir + "/" + fileName;
                    Files.writeString(Path.of(filePath), content);
                    return ResourceResult.success(filePath);
                }
            }
        }
    }

    // ===== 工具方法 =====

    private static void downloadFile(OkHttpClient client, String url, String filePath) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("下载失败: " + response.code());
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(response.body().bytes());
            }
        }
    }
}
