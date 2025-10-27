package com.group_finity.mascot.ai;

/**
 * AI 对话服务接口
 * OCP 的核心：我们依赖于这个抽象，而不是具体的实现。
 */
public interface AiChatService {
    /**
     * 获取 AI 的回复
     * @param userInput 用户的输入
     * @return AI 的回复字符串
     */
    String getResponse(String userInput);

    /**
     * 异步获取 AI 的回复
     * @param userInput 用户的输入
     * @param callback 获取回复后的回调函数
     */
    void getResponseAsync(String userInput, java.util.function.Consumer<String> callback);
}
