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

        // 在 EDT 上创建并显示聊天窗口
        SwingUtilities.invokeLater(() -> {
            chatWindow = new ChatWindow(null, this::handleUserInput);

            // 将窗口定位在桌宠上方（如果 mascot 可用的话）
            try {
                Point mascotPos = mascot.getAnchor();
                chatWindow.setLocation(mascotPos.x - 50, mascotPos.y - chatWindow.getHeight() - 20);
            } catch (Exception ignore) {}

            // 确保窗口可见并置顶
            chatWindow.setVisible(true);
            chatWindow.toFront();
            chatWindow.setAlwaysOnTop(true);

            // --- 调试打印：打印当前焦点持有者（立即） ---
            System.out.println("[DEBUG] After show - focus owner: " 
                + java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());

            // 再安排一次在 EDT 稍后打印（确认焦点在事件循环处理中最终归属）
            SwingUtilities.invokeLater(() -> {
                System.out.println("[DEBUG] Later on EDT - focus owner: " 
                    + java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
            });

            // 首次请求输入框获得焦点（首选）
            chatWindow.focusInput();

            // 备用重试：若首次请求失败，做多次短延迟重试
            final java.util.concurrent.atomic.AtomicBoolean focusedRef = new java.util.concurrent.atomic.AtomicBoolean(false);
            final java.util.concurrent.atomic.AtomicReference<java.awt.Component> ownerRef = new java.util.concurrent.atomic.AtomicReference<>(
                    java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
            );

            if (ownerRef.get() instanceof javax.swing.JTextField) {
                focusedRef.set(true);
            }

            if (!focusedRef.get()) {
                // 启动后台线程进行最多 3 次重试（每次间隔 100ms），每次在 EDT 上再次请求焦点
                new Thread(() -> {
                    for (int i = 0; i < 3 && !focusedRef.get(); i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        // 在 EDT 上请求焦点
                        SwingUtilities.invokeLater(() -> {
                            chatWindow.focusInput();
                            ownerRef.set(java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
                            if (ownerRef.get() instanceof javax.swing.JTextField) {
                                focusedRef.set(true);
                            }
                        });

                        // 等一小段时间让 EDT 处理焦点事件，再检查
                        try {
                            Thread.sleep(120);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }, "ChatAction-Focus-Retry").start();
            }

            // 保持原有的关闭检测逻辑
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
