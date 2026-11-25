package com.admin.server.modules.ai.providers;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 兼容向量化客户端。
 * <p>请求 {@code {baseUrl}/embeddings}，请求体 {@code {"model":...,"input":[...]}}，
 * 响应读取 {@code data[i].embedding}。按模型配置的 batchSize 串行分批，单批失败直接抛出，
 * 重试由上层知识库任务负责，避免在客户端内隐式放大调用量。</p>
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    /** 向量化属于重计算请求，读超时单独放宽 */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    private static final int DEFAULT_BATCH_SIZE = 16;
    private static final int MAX_BATCH_SIZE = 128;
    private static final int DIMENSION_TOLERANCE = 0;

    /**
     * 连通性测试：发送一条极短文本并校验返回向量，返回往返耗时（毫秒）。
     *
     * @param model       向量模型配置（baseUrl / modelName / dimension / extraHeaders）
     * @param plainApiKey 明文密钥
     */
    public long testConnection(AiModelDO model, String plainApiKey) {
        long start = System.currentTimeMillis();
        List<float[]> vectors = embed(model, plainApiKey, List.of("ping"));
        if (vectors.isEmpty() || vectors.get(0).length == 0) {
            throw new BusinessException("向量接口未返回有效向量");
        }
        return System.currentTimeMillis() - start;
    }

    /**
     * 批量向量化。
     *
     * @return 与输入顺序一致的向量列表
     */
    public List<float[]> embed(AiModelDO model, String plainApiKey, List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<String> inputs = new ArrayList<>(texts.size());
        for (String text : texts) {
            inputs.add(StrUtil.nullToEmpty(text));
        }
        int batchSize = resolveBatchSize(model.getBatchSize());
        List<float[]> result = new ArrayList<>(inputs.size());
        for (int from = 0; from < inputs.size(); from += batchSize) {
            int to = Math.min(from + batchSize, inputs.size());
            result.addAll(requestBatch(model, plainApiKey, inputs.subList(from, to)));
        }
        validateDimensions(model, result);
        return result;
    }

    private List<float[]> requestBatch(AiModelDO model, String plainApiKey, List<String> batch) {
        String url = resolveEmbeddingUrl(model);
        JSONObject body = new JSONObject();
        body.set("model", StrUtil.nullToEmpty(model.getModelName()));
        JSONArray input = new JSONArray();
        batch.forEach(input::add);
        body.set("input", input);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(READ_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + StrUtil.nullToEmpty(plainApiKey))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        ProviderHttpUtils.applyExtraHeaders(builder, model.getExtraHeaders());

        HttpResponse<String> response;
        try {
            response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("向量化请求失败 model={} url={}: {}", model.getModelName(), url, e.getMessage());
            throw new BusinessException("向量化请求失败: " + e.getMessage());
        }
        if (response.statusCode() != 200) {
            throw new BusinessException("向量化接口返回异常(HTTP " + response.statusCode() + "): "
                    + StrUtil.brief(response.body(), 300));
        }
        return parseVectors(response.body());
    }

    /** 兼容 OpenAI 的 data[].embedding 与部分自建服务直接返回 embedding 数组的写法 */
    private List<float[]> parseVectors(String responseBody) {
        List<float[]> vectors = new ArrayList<>();
        if (StrUtil.isBlank(responseBody)) {
            return vectors;
        }
        JSONObject root;
        try {
            root = JSONUtil.parseObj(responseBody);
        } catch (Exception e) {
            throw new BusinessException("向量化响应不是合法 JSON: " + StrUtil.brief(responseBody, 200));
        }
        Object data = root.get("data");
        if (data instanceof JSONArray array) {
            for (Object item : array) {
                if (item instanceof JSONObject obj) {
                    Object embedding = obj.get("embedding");
                    if (embedding instanceof JSONArray vectorArray) {
                        vectors.add(toFloatArray(vectorArray));
                    }
                }
            }
        } else if (root.get("embedding") instanceof JSONArray single) {
            vectors.add(toFloatArray(single));
        }
        if (vectors.isEmpty()) {
            throw new BusinessException("向量化响应未包含向量数据: " + StrUtil.brief(responseBody, 200));
        }
        return vectors;
    }

    private float[] toFloatArray(JSONArray array) {
        float[] vector = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            vector[i] = array.getFloat(i, 0f);
        }
        return vector;
    }

    /** 配置了维度时校验实际返回维度，避免维度不一致导致静默召回为空 */
    private void validateDimensions(AiModelDO model, List<float[]> vectors) {
        int expected = model.getDimension() == null ? 0 : model.getDimension();
        if (expected <= 0) {
            return;
        }
        for (float[] vector : vectors) {
            if (Math.abs(vector.length - expected) > DIMENSION_TOLERANCE) {
                throw new BusinessException("向量维度与配置不一致：配置 " + expected
                        + " 维，接口返回 " + vector.length + " 维，请核对「向量维度」配置");
            }
        }
    }

    private int resolveBatchSize(Integer batchSize) {
        int size = batchSize == null ? DEFAULT_BATCH_SIZE : batchSize;
        return Math.max(1, Math.min(size, MAX_BATCH_SIZE));
    }

    private String resolveEmbeddingUrl(AiModelDO model) {
        return ProviderHttpUtils.resolveUrl(model.getBaseUrl(), model.getPathTemplate(),
                model.getModelName(), "/embeddings");
    }
}
