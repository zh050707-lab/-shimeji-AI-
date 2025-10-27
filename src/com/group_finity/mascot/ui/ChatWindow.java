package com.group_finity.mascot.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * 聊天 UI 窗口
 */
public class ChatWindow extends JWindow {
    
    private final JTextArea chatDisplay;
    private final JTextField inputField;
    private final Consumer<String> onUserInput; // 回调函数，当用户输入时调用

    public ChatWindow(Frame owner, Consumer<String> onUserInput) {
        super(owner);
        this.onUserInput = onUserInput;

        setAlwaysOnTop(true); // 保持在最前
        setLayout(new BorderLayout());
        
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
                    onUserInput.accept(text); // 调用回调
                    inputField.setText("");
                }
            }
        });

        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        pack();
        
        // 可选：添加窗口关闭逻辑
        // ...
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
            // 移除 "思考中..."
            String text = chatDisplay.getText();
            if(text.endsWith("（思考中...）")) {
                chatDisplay.setText(text.substring(0, text.lastIndexOf("\n")));
            }
        }
    }
}
