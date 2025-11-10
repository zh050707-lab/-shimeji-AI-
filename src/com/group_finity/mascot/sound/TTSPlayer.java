package com.group_finity.mascot.sound;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 简单的 TTS 播放器。
 * 在 Windows 平台使用 PowerShell 调用 System.Speech.Synthesis 播放文本。
 * 使用单线程执行器串行化播放请求，避免并发语音混叠。
 */
public class TTSPlayer {

    // 单线程执行器用于串行播放
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "TTS-Player-Thread");
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * 同步播放文本（阻塞当前线程直到播放结束）。
     */
    public static void speak(String text) {
        if (text == null) return;
        if (text.trim().isEmpty()) return;

        // 只有在 Windows 平台才尝试使用 PowerShell 播放
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            // 非 Windows 平台暂不实现
            return;
        }

        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("shimeji_tts_", ".txt");
            java.nio.file.Files.write(tmp, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            tmp.toFile().deleteOnExit();

            String filePath = tmp.toAbsolutePath().toString().replace("'", "''");
            String psCommand = String.format("Add-Type -AssemblyName System.Speech; (New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak([System.IO.File]::ReadAllText('%s'))", filePath);

            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", psCommand);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // 读取输出并等待结束，确保串行播放
            try (java.io.InputStream is = p.getInputStream()) {
                byte[] buf = new byte[1024];
                while (is.read(buf) != -1) {
                    // no-op
                }
            } catch (Exception ex) {
                // ignore
            }

            try {
                p.waitFor();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * 异步播放文本（提交到串行执行器）。
     */
    public static void speakAsync(final String text) {
        if (text == null || text.trim().isEmpty()) return;
        EXECUTOR.submit(() -> speak(text));
    }

    /**
     * 关闭执行器（如果需要）。
     */
    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
