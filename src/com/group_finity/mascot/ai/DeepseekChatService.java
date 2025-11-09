package com.group_finity.mascot.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.swing.SwingWorker;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.memory.MemoryManager;

/**
 * AI 服务的 Deepseek API 实现。
 * 实现了 AiChatService 接口。
 */
public class DeepseekChatService implements AiChatService {

    // 使用配置中的 API Key（由设置窗口保存到 conf/settings.properties） 
    private final String apiKey;
    private final String apiEndpoint;
    private final String personality;
    private final String systemPrompt;
    private final boolean enabled;
    private final boolean logConversations;
    private final MemoryManager memoryManager;

    public DeepseekChatService(MemoryManager memoryManager) {
        // 从主配置读取 AI 设置（settings.properties）
        Properties props = null;
        try {
            if (Main.getInstance() != null) {
                props = Main.getInstance().getProperties();
            }
        } catch (Throwable t) {
            props = null;
        }
        if (props == null) {
            props = new Properties();
        }
        
        // 读取所有 AI 相关设置
        this.apiKey = props.getProperty("ai.api_key", "").trim();
        this.apiEndpoint = props.getProperty("ai.endpoint", "https://api.deepseek.com/chat/completions").trim();
        this.personality = props.getProperty("ai.personality", "Friendly").trim();
        this.systemPrompt = props.getProperty("ai.system_prompt", "").trim();
        this.logConversations = Boolean.parseBoolean(props.getProperty("ai.log_conversations", "false"));
        boolean enabledProp = Boolean.parseBoolean(props.getProperty("ai.enabled", "false"));
        
        this.enabled = enabledProp && !this.apiKey.isEmpty();
        if (!this.enabled) {
            System.err.println("AI 服务未启用或未配置 API Key，已禁用 DeepseekChatService。");
        }
        this.memoryManager = memoryManager; // 使用传入的对话记忆管理器
    }

    @Override
    public void getResponseAsync(String userInput, java.util.function.Consumer<String> callback) {
        // 如果未启用或未配置 API Key，立即返回友好提示
        if (!this.enabled) {
            callback.accept("AI 未启用或未配置 API Key。请在设置中填写 API Key 并启用 AI。");
            return;
        }

        // 使用 SwingWorker 确保网络请求在后台线程
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // 1. 构建 JSON 请求体，包含历史对话
                StringBuilder messagesJson = new StringBuilder();
                messagesJson.append("[");

                // 先添加系统初始设定（如果有）
                if (!systemPrompt.isEmpty()) {
                    String fullSystemPrompt = "你的性格是 " + personality + "。" + systemPrompt;
                    messagesJson.append("{\"role\": \"system\", \"content\": \"")
                              .append(escapeJson(fullSystemPrompt))
                              .append("\"}");
                }

                // 添加最近的历史对话（最多5条）
                try {
                    List<MemoryManager.Message> history = memoryManager.getRecentMessages(5);
                    for (MemoryManager.Message msg : history) {
                        if (messagesJson.length() > 1) {
                            messagesJson.append(",");
                        }
                        messagesJson.append("{\"role\": \"")
                                  .append(msg.getRole())
                                  .append("\", \"content\": \"")
                                  .append(escapeJson(msg.getText()))
                                  .append("\"}");
                    }
                    
                    // 记录对话历史
                    if (logConversations) {
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter("CHAT_HISTORY_" + 
                                new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) + ".md", true);
                            java.io.BufferedWriter bw = new java.io.BufferedWriter(fw);
                            for (MemoryManager.Message msg : history) {
                                bw.write(msg.getRole() + ": " + msg.getText());
                                bw.newLine();
                            }
                            bw.close();
                        } catch (Exception e) {
                            System.err.println("无法记录对话历史: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("无法加载历史对话: " + e.getMessage());
                }

                // 添加当前用户输入
                if (messagesJson.length() > 1) {
                    messagesJson.append(",");
                }
                messagesJson.append("{\"role\": \"user\", \"content\": \"")
                           .append(escapeJson(userInput))
                           .append("\"}");
                messagesJson.append("]");

                String jsonInputString = "{\"model\": \"deepseek-chat\", \"messages\": " + messagesJson.toString() + "}";
                // 2. 创建 URL 和 HttpURLConnection
                URL url = new URL(apiEndpoint);
                // 诊断日志：打印将要发送到的 endpoint 和请求体大小（注意：不要打印 API Key）
                try {
                    int len = jsonInputString == null ? 0 : jsonInputString.length();
                    String preview = jsonInputString == null ? "" : (jsonInputString.length() > 200 ? jsonInputString.substring(0, 200) + "..." : jsonInputString);
                    System.err.println("[AI DEBUG] POST " + url.toString() + " payload_len=" + len + " preview=" + preview);
                } catch (Throwable t) {
                    // ignore logging errors
                }
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + DeepseekChatService.this.apiKey);
                // 设置连接和读取超时，避免无限等待
                conn.setConnectTimeout(10000); // 10秒连接超时
                conn.setReadTimeout(30000); // 30秒读取超时
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
                    // 如果出错，优先读取错误流；如果错误流为空（某些服务器/连接会返回 null），回退到 response message
                    java.io.InputStream es = conn.getErrorStream();
                    if (es != null) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(es, "UTF-8"))) {
                            StringBuilder errorResponse = new StringBuilder();
                            String errorLine;
                            while ((errorLine = br.readLine()) != null) {
                                errorResponse.append(errorLine.trim());
                            }
                            throw new RuntimeException("HTTP 错误码: " + responseCode + ", 错误信息: " + errorResponse.toString() + ", url=" + url.toString());
                        }
                    } else {
                        String respMsg = "";
                        try { respMsg = conn.getResponseMessage(); } catch (Throwable t) { /* ignore */ }
                        throw new RuntimeException("HTTP 错误码: " + responseCode + ", 无错误流, responseMessage=" + respMsg + ", url=" + url.toString());
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get(); // 从 doInBackground 获取结果
                    callback.accept(result); // 将结果传递给回调
                } catch (Exception e) {
                    // 打印完整异常到 stderr 供调试
                    e.printStackTrace();
                    String msg = e.getMessage();
                    if (msg == null) msg = e.toString();
                    // 将更详细的错误信息反馈到 UI（简短版）
                    callback.accept("抱歉，我连接 Deepseek 失败了: " + msg);
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
