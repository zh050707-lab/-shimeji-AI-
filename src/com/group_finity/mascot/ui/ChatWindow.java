package com.group_finity.mascot.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
                // 清空对话并关闭窗口
                try {
                    chatDisplay.setText("");
                } catch (Exception ex) {
                    // ignore
                }
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
        add(topPanel, BorderLayout.NORTH);
        // --- 顶部按钮面板结束 ---

        chatDisplay = new JTextArea(5, 30);
        chatDisplay.setEditable(false);
        chatDisplay.setLineWrap(true);
        chatDisplay.setWrapStyleWord(true);
        chatDisplay.setText("你好！想聊点什么？");
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
        chatDisplay.append("\n你: " + message);
    }

    public void displayAiMessage(String message) {
        chatDisplay.append("\n桌宠: " + message);
    }
    
    public void setThinking(boolean thinking) {
        if (thinking) {
            inputField.setEnabled(false);
            displayAiMessage("（思考中...）");
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
}
