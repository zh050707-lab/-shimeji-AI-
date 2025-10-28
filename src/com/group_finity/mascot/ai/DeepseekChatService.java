package com.group_finity.mascot.ai;

import javax.swing.SwingWorker;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AI 服务的 Deepseek API 实现。
 * 实现了 AiChatService 接口。
 */
public class DeepseekChatService implements AiChatService {

    private static final String API_KEY = "sk-3750675d0be6489b851f4db22a4d2cd8";
    
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    public DeepseekChatService() {
        // 构造函数，你可以在这里检查 API Key 是否已设置
        if (API_KEY.equals("sk-xxxxxxxxxxxxxxxxxxxxxxxx")) {
            System.err.println("警告：尚未在 DeepseekChatService.java 中设置 API Key！");
        }
    }

    @Override
    public void getResponseAsync(String userInput, java.util.function.Consumer<String> callback) {
        
        // 使用 SwingWorker 确保网络请求在后台线程
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // 1. 构建 JSON 请求体
                // 注意：为了兼容 Java 1.6，我们手动拼接 JSON 字符串
                String jsonInputString = "{\"model\": \"deepseek-chat\", \"messages\": [" +
                                         "{\"role\": \"user\", \"content\": \"" + escapeJson(userInput) + "\"}" +
                                         "]}";

                // 2. 创建 URL 和 HttpURLConnection
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setDoOutput(true); // 允许发送请求体

                // 3. 发送请求
                try (OutputStream os = conn.getOutputStream()) {
                    // 使用 "UTF-8" 确保兼容 Java 1.6
                    byte[] input = jsonInputString.getBytes("UTF-8");
                    os.write(input, 0, input.length);
                }

                // 4. 读取响应
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        // 5. 解析响应并返回
                        return parseDeepseekResponse(response.toString());
                    }
                } else {
                    // 如果出错，读取错误流
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String errorLine;
                        while ((errorLine = br.readLine()) != null) {
                            errorResponse.append(errorLine.trim());
                        }
                        throw new RuntimeException("HTTP 错误码: " + responseCode + ", 错误信息: " + errorResponse.toString());
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get(); // 从 doInBackground 获取结果
                    callback.accept(result); // 将结果传递给回调
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.accept("抱歉，我连接 Deepseek 失败了...");
                }
            }
        };
        worker.execute();
    }

    @Override
    public String getResponse(String userInput) {
        final java.util.concurrent.atomic.AtomicReference<String> ref = new java.util.concurrent.atomic.AtomicReference<>("(no response)");
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        getResponseAsync(userInput, (result) -> {
            ref.set(result);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ref.get();
    }

    /**
     * 简单的 JSON 解析器，用于提取 "content" 字段。
     * 避免引入 Java 1.6 中没有的 JSON 库。
     *
     * 【已修改】增强了查找逻辑，以正确处理 Deepseek 的嵌套响应。
     */
    private String parseDeepseekResponse(String responseBody) {
        try {
            // 调试辅助：如果解析失败，我们会在调用处打印整个 responseBody。
            // 这里直接实现更鲁棒的查找：优先在 assistant 出现之后查找 content，
            // 若失败则尝试最近的 content（从后往前），最后尝试第一次出现的 content。

            // 1) 尝试在包含 "assistant" 的位置之后查找 content
            int assistantIndex = responseBody.indexOf("assistant");
            int contentPos = -1;
            if (assistantIndex != -1) {
                contentPos = indexOfContentKeyAfter(responseBody, assistantIndex);
            }

            // 2) 如果上面失败，尝试从后往前找到最后一个 content
            if (contentPos == -1) {
                contentPos = lastIndexOfContentKey(responseBody);
            }

            // 3) 如果仍然失败，尝试第一次出现的 content
            if (contentPos == -1) {
                contentPos = indexOfContentKeyAfter(responseBody, 0);
            }

            if (contentPos == -1) {
                // 返回的结构不包含 content，记录用于调试
                return "（无法解析 AI 回复：未找到 content 键）";
            }

            // 找到 content key 后，定位冒号后的起始引号
            int colon = responseBody.indexOf(':', contentPos);
            if (colon == -1) {
                return "（无法解析 AI 回复：content 后缺少冒号）";
            }

            // 找到第一个未被空格阻挡的双引号（value 的起始引号）
            int i = colon + 1;
            while (i < responseBody.length() && Character.isWhitespace(responseBody.charAt(i))) {
                i++;
            }
            if (i >= responseBody.length() || responseBody.charAt(i) != '\"') {
                // 如果不是双引号，尝试处理 content: { ... } 的情况（返回整个对象的文本）
                // 为简单起见，返回无法解析
                return "（无法解析 AI 回复：content 值不是字符串）";
            }

            int startIndex = i + 1;
            int endIndex = findClosingQuote(responseBody, startIndex);
            if (endIndex == -1) {
                return "（无法解析 AI 回复：未找到结束引号）";
            }

            String raw = responseBody.substring(startIndex, endIndex);
            return unescapeJson(raw);
        } catch (Exception e) {
            e.printStackTrace();
            return "（解析回复时出错）";
        }
    }
    
    /** 在 fromIndex 之后寻找 "content" 键的起始位置（返回键名的引号开头索引），找不到返回 -1 */
    private int indexOfContentKeyAfter(String s, int fromIndex) {
        int idx = s.indexOf("\"content\"", Math.max(0, fromIndex));
        if (idx != -1) return idx;
        // 兼容无双引号或不同空格的情形，查找 content:
        idx = s.indexOf("content", Math.max(0, fromIndex));
        return idx == -1 ? -1 : idx;
    }

    /** 从后往前查找 "content" 键，返回找到位置或 -1 */
    private int lastIndexOfContentKey(String s) {
        int idx = s.lastIndexOf("\"content\"");
        if (idx != -1) return idx;
        idx = s.lastIndexOf("content");
        return idx;
    }

    /** 找到未被转义的关闭引号位置，从 start (第一个字符后) 开始搜索，找不到返回 -1 */
    private int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                // 计算前导反斜杠数量，若为偶数则该引号未被转义
                int backslashes = 0;
                int j = i - 1;
                while (j >= 0 && s.charAt(j) == '\\') {
                    backslashes++;
                    j--;
                }
                if ((backslashes % 2) == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 辅助方法：转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 辅助方法：反转义 JSON 字符串中的特殊字符
     */
    private String unescapeJson(String s) {
        return s.replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
