package com.group_finity.mascot.memory;

import java.io.File;
import java.util.List;

/**
 * Simple demo showing how to use MemoryManager.
 * Run as a standalone Java application from project root.
 */
public class ChatMemoryDemo {
    public static void main(String[] args) throws Exception {
        MemoryManager mm = new MemoryManager(new File("conf/memory.json"));

        System.out.println("Clearing memory and adding demo messages...");
        mm.clearMemory();

        mm.addMessage("user", "你好，Shimeji！");
        mm.addMessage("assistant", "你好！我能帮你什么？");
        mm.addMessage("user", "告诉我一个笑话。\n谢谢。");

        System.out.println("Recent messages:");
        List<MemoryManager.Message> recent = mm.getRecentMessages(10);
        for (MemoryManager.Message m : recent) {
            System.out.println(m.toString());
        }

        System.out.println("Done. memory stored in conf/memory.json");
    }
}
