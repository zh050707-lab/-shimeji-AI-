package com.group_finity.mascot.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import com.group_finity.mascot.memory.MemoryManager;

/**
 * 聊天 UI 窗口
 * 使用 JDialog 以获得更稳定的焦点行为（无装饰、非模态）
 * 支持从顶部面板拖动移动窗口。
 */
public class ChatWindow extends JDialog {
    
    private final JTextArea chatDisplay;
    private final JTextField inputField;
    private final java.util.function.Consumer<String> onUserInput; // 回调函数，当用户输入时调用
    private com.group_finity.mascot.Mascot mascot; // 关联的桌宠实例
    private Runnable onClose; // 窗口关闭时的回调函数
    private final MemoryManager memoryManager; // 对话记忆管理器

    // 拖动偏移（按下时记录）
    private Point dragOffset;
    
    public void setMascot(com.group_finity.mascot.Mascot mascot) {
        this.mascot = mascot;
    }
    
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public ChatWindow(Frame owner, java.util.function.Consumer<String> onUserInput) {
        // 使用 JDialog 的 Frame 构造器；owner 可以为 null（JDialog 处理 null 更可靠）
        super(owner, false); // false => 非模态
        this.onUserInput = onUserInput;
        this.memoryManager = new MemoryManager();

        // 无装饰，外观类似 JWindow
        setUndecorated(true);

        // 确保窗口可以获取键盘焦点
        setFocusableWindowState(true);

        setAlwaysOnTop(true); // 保持在最前
        setLayout(new BorderLayout());
        
        // --- 顶部按钮面板（右对齐），包含一个“删除”按钮 ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        topPanel.setOpaque(false);
        JButton deleteButton = new JButton("删除");
        deleteButton.setFocusable(false); // 避免按钮一出现就抢占输入焦点
        deleteButton.setToolTipText("删除并关闭聊天窗口");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 保留历史记录但关闭窗口
                try {
                    inputField.setText("");
                } catch (Exception ex) {
                    // ignore
                }
                // 调用关闭回调
                if (onClose != null) {
                    onClose.run();
                }
                dispose();
            }
        });
        topPanel.add(deleteButton);
        // TTS 切换按钮（在聊天窗口直接控制语音开关）
        javax.swing.JToggleButton ttsToggle = new javax.swing.JToggleButton("语音");
        ttsToggle.setFocusable(false);
        ttsToggle.setToolTipText("启用/禁用 AI 回复语音（持久化到 conf/settings.properties）");
        try {
            boolean ttsEnabled = Boolean.parseBoolean(com.group_finity.mascot.Main.getInstance().getProperties().getProperty("TTS", "true"));
            ttsToggle.setSelected(ttsEnabled);
        } catch (Exception ex) {
            // ignore
        }
        ttsToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean on = ttsToggle.isSelected();
                try {
                    com.group_finity.mascot.Main.getInstance().getProperties().setProperty("TTS", String.valueOf(on));
                    java.nio.file.Path configPath = java.nio.file.Paths.get(".", "conf", "settings.properties");
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(configPath.toFile())) {
                        com.group_finity.mascot.Main.getInstance().getProperties().store(out, "Shimeji-ee Configuration Options");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        topPanel.add(ttsToggle);
        add(topPanel, BorderLayout.NORTH);
        // --- 顶部按钮面板结束 ---

        chatDisplay = new JTextArea(5, 30);
        chatDisplay.setEditable(false);
        chatDisplay.setLineWrap(true);
        chatDisplay.setWrapStyleWord(true);
        
        // 加载最近的对话历史（最多显示10条）
        StringBuilder initialText = new StringBuilder("你好！想聊点什么？\n\n");
        initialText.append("=== 最近的对话记录 ===\n");
        try {
            List<MemoryManager.Message> history = memoryManager.getRecentMessages(10);
            if (!history.isEmpty()) {
                for (MemoryManager.Message msg : history) {
                    String role = "assistant".equals(msg.getRole()) ? "桌宠" : "你";
                    initialText.append(String.format("%s: %s\n", role, msg.getText()));
                }
                initialText.append("=== 历史记录结束 ===\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        chatDisplay.setText(initialText.toString());
        JScrollPane scrollPane = new JScrollPane(chatDisplay);
        
        inputField = new JTextField(30);
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = inputField.getText().trim();
                if (!text.isEmpty()) {
                    displayUserMessage(text);
                    if (onUserInput != null) {
                        onUserInput.accept(text); // 调用回调
                    }
                    inputField.setText("");
                }
            }
        });

        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        // Pack 之后再显示位置才会生效
        pack();

        // 允许用 ESC 或窗口关闭触发处置（若需要）
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // ---- 给 topPanel 添加拖动支持 ----
        MouseAdapter dragger = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 记录按下时相对于窗口左上角的偏移
                Point pOnScreen = e.getLocationOnScreen();
                dragOffset = new Point(pOnScreen.x - getX(), pOnScreen.y - getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset != null) {
                    Point pOnScreen = e.getLocationOnScreen();
                    // 计算窗口的新左上坐标
                    int nx = pOnScreen.x - dragOffset.x;
                    int ny = pOnScreen.y - dragOffset.y;
                    setLocation(nx, ny);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragOffset = null;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                topPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                topPanel.setCursor(Cursor.getDefaultCursor());
            }
        };

        topPanel.addMouseListener(dragger);
        topPanel.addMouseMotionListener(dragger);
        // ---- 拖动支持添加完毕 ----
    }

    // 在窗口可见后请求输入框获得焦点（在 EDT 上执行）
    public void focusInput() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (inputField != null && inputField.isEnabled()) {
                    inputField.requestFocusInWindow();
                    inputField.requestFocus();
                }
            }
        });
    }

    public void displayUserMessage(String message) {
        String text = chatDisplay.getText();
        // 如果文本以历史记录分隔符结束，先添加一个换行
        if (text.trim().endsWith("=== 历史记录结束 ===")) {
            chatDisplay.append("\n");
        }
        chatDisplay.append("\n你: " + message);
        try {
            memoryManager.addMessage("user", message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void displayAiMessage(String message) {
        String text = chatDisplay.getText();
        // 如果文本以历史记录分隔符结束，先添加一个换行
        if (text.trim().endsWith("=== 历史记录结束 ===")) {
            chatDisplay.append("\n");
        }
        chatDisplay.append("\n桌宠: " + message);
        try {
            memoryManager.addMessage("assistant", message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TTS 播放（仅在配置中开启且未全局静音时）
        try {
            boolean ttsEnabled = Boolean.parseBoolean(com.group_finity.mascot.Main.getInstance().getProperties().getProperty("TTS", "true"));
            if (ttsEnabled && !com.group_finity.mascot.sound.Sounds.isMuted()) {
                com.group_finity.mascot.sound.TTSPlayer.speakAsync(message);
            }
        } catch (Throwable t) {
            // 不影响主流程
            t.printStackTrace();
        }
    }
    
    public void setThinking(boolean thinking) {
        if (thinking) {
            inputField.setEnabled(false);
            chatDisplay.append("\n桌宠: （思考中...）");
        } else {
            inputField.setEnabled(true);
            // 尝试移除最后一行的 "（思考中...）" 提示（若存在）
            String text = chatDisplay.getText();
            if (text.endsWith("（思考中...）")) {
                int lastNewline = text.lastIndexOf("\n");
                if (lastNewline >= 0) {
                    chatDisplay.setText(text.substring(0, lastNewline));
                } else {
                    chatDisplay.setText("");
                }
            }
        }
    }

    /**
     * 获取最近的对话记录（可选择条数）。
     * 如果出现异常会返回空列表。
     */
    public java.util.List<MemoryManager.Message> getRecentMessages(int limit) {
        try {
            return memoryManager.getRecentMessages(limit);
        } catch (IOException e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
}
