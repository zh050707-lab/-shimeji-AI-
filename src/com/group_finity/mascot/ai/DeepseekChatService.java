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

    /**
     * 简单的 JSON 解析器，用于提取 "content" 字段。
     * 避免引入 Java 1.6 中没有的 JSON 库。
     */
    private String parseDeepseekResponse(String responseBody) {
        try {
            // 寻找 "content": "..."
            String contentKey = "\"content\": \"";
            int startIndex = responseBody.indexOf(contentKey);
            if (startIndex == -1) {
                return "（无法解析 AI 回复）";
            }
            
            startIndex += contentKey.length(); // 移动到回复内容的开头
            
            // 寻找内容的结尾 "
            int endIndex = responseBody.indexOf("\"", startIndex);
            if (endIndex == -1) {
                return "（无法解析 AI 回复）";
            }
            
            // 提取内容并处理转义字符
            return unescapeJson(responseBody.substring(startIndex, endIndex));
        } catch (Exception e) {
            e.printStackTrace();
            return "（解析回复时出错）";
        }
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
