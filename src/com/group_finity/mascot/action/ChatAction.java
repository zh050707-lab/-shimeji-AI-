package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.ai.AiChatService;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import com.group_finity.mascot.ui.ChatWindow;

import java.awt.Point;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * ChatAction：一个与 AI 后端通信并管理聊天窗口的 Action 实现。
 * 适配了当前的 ActionBase 构造器签名。
 */
public class ChatAction extends ActionBase {

    private ChatWindow chatWindow;
    private AiChatService chatService;
    private boolean isChatting = false;

    // 动画可以通过构造器传入，但为简化实现我们不强制使用
    private Animation talkingAnimation;
    private Animation listeningAnimation;

    // 与 ActionBase 完整签名兼容的构造器
    public ChatAction(java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap params, final AiChatService service) {
        super(schema, animations, params);
        this.chatService = service;
    }

    @Override
    public void init(Mascot mascot) throws VariableException {
        super.init(mascot);

        this.isChatting = true;

        // 在 Swing 线程上创建聊天窗口
        SwingUtilities.invokeLater(() -> {
            chatWindow = new ChatWindow(null, this::handleUserInput);

            // 将窗口定位在桌宠上方（如果 mascot 可用的话）
            try {
                Point mascotPos = mascot.getAnchor();
                chatWindow.setLocation(mascotPos.x - 50, mascotPos.y - chatWindow.getHeight() - 20);
            } catch (Exception ignore) {}

            chatWindow.setVisible(true);

            // 关闭时停止聊天 action
            chatWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    isChatting = false;
                }
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    isChatting = false;
                }
            });
        });
    }

    private void handleUserInput(String userInput) {
        if (chatWindow != null) {
            chatWindow.setThinking(true);
        }
        // 异步调用 AI 服务
        chatService.getResponseAsync(userInput, this::handleAiResponse);
    }

    private void handleAiResponse(String aiResponse) {
        SwingUtilities.invokeLater(() -> {
            if (chatWindow != null) {
                chatWindow.setThinking(false);
                chatWindow.displayAiMessage(aiResponse);
            }
        });
    }

    @Override
    public boolean hasNext() {
        // 只要聊天窗口存在且未关闭就继续
        return this.isChatting;
    }

    @Override
    protected void tick() throws VariableException {
        // 保持窗口相对位置（若需要）
        if (chatWindow != null && chatWindow.isVisible()) {
            try {
                Point mascotPos = getMascot().getAnchor();
                chatWindow.setLocation(mascotPos.x - 50, mascotPos.y - chatWindow.getHeight() - 20);
            } catch (Exception ignore) {}
        }
    }
}
