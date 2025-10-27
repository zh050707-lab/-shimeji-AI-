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
 * 实现 AI 对话的 Action。
 * 这是一个扩展，完全符合 OCP。
 */
public class ChatAction extends ActionBase {

    private ChatWindow chatWindow;
    private AiChatService chatService;
    private boolean isChatting = false;

    // 我们可以通过 XML 传入"聆听"和"回复"的动画
    private Animation talkingAnimation;
    private Animation listeningAnimation;

    public ChatAction(VariableMap params, AiChatService service) {
        super(params);
        this.chatService = service;
    }

    @Override
    public void init(Mascot mascot) throws VariableException {
        super.init(mascot);
        
        // 尝试从配置中加载动画
        // this.talkingAnimation = getAnimation("talking");
        // this.listeningAnimation = getAnimation("listening");
        
        // 如果没有配置动画，就使用一个内置的（例如 "Stay"）
        if (this.listeningAnimation == null) {
            this.listeningAnimation = getMascot().getAnimationSet().get("Stay");
        }
        
        getMascot().setAnimation(this.listeningAnimation);

        this.isChatting = true;
        
        // 必须在 Swing 线程上创建 UI
        SwingUtilities.invokeLater(() -> {
            chatWindow = new ChatWindow(null, this::handleUserInput);
            
            // 将窗口定位在桌宠上方
            Point mascotPos = getMascot().getAnchor();
            chatWindow.setLocation(mascotPos.x - 50, mascotPos.y - chatWindow.getHeight() - 20);
            
            chatWindow.setVisible(true);
            
            // 添加一个 WindowListener 来检测窗口关闭
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
        // 1. UI 显示 "思考中"
        if (chatWindow != null) {
            chatWindow.setThinking(true);
        }
        
        // 2. 异步请求 AI
        chatService.getResponseAsync(userInput, this::handleAiResponse);
    }

    private void handleAiResponse(String aiResponse) {
        // 3. 在 Swing 线程上更新 UI
        SwingUtilities.invokeLater(() -> {
            if (chatWindow != null) {
                chatWindow.setThinking(false);
                chatWindow.displayAiMessage(aiResponse);
            }
            // 可以在这里切换到 "talking" 动画
            // getMascot().setAnimation(this.talkingAnimation);
        });
    }

    @Override
    public boolean hasNext() throws VariableException {
        // 只要聊天窗口开着，这个 Action 就继续
        return this.isChatting;
    }

    @Override
    protected void tick() throws VariableException {
        // Action 的主循环
        // 我们可以让桌宠保持 "聆听" 动画
        if (!getMascot().getAnimation().equals(this.listeningAnimation)) {
            getMascot().setAnimation(this.listeningAnimation);
        }
        
        // 保持聊天窗口在桌宠附近
        if (chatWindow != null && chatWindow.isVisible()) {
            Point mascotPos = getMascot().getAnchor();
            chatWindow.setLocation(mascotPos.x - 50, mascotPos.y - chatWindow.getHeight() - 20);
        }
    }
}
