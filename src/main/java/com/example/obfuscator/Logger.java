package com.example.obfuscator;

import javax.swing.*;
import java.awt.*;  // Добавьте этот импорт для Color
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    public enum LogLevel {
        INFO("ℹ️", Color.BLUE),
        SUCCESS("✅", new Color(46, 204, 113)),  // Зеленый
        WARNING("⚠️", new Color(241, 196, 15)),  // Желтый
        ERROR("❌", new Color(231, 76, 60)),     // Красный
        DEBUG("🔍", Color.GRAY);

        private final String icon;
        private final Color color;

        LogLevel(String icon, Color color) {
            this.icon = icon;
            this.color = color;
        }

        public String getIcon() { return icon; }
        public Color getColor() { return color; }
    }

    private static Logger instance;
    private JTextArea logTextArea;
    private File logFile;
    private boolean saveToFile = false;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private Logger() {
        // Приватный конструктор
    }

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void setLogTextArea(JTextArea logTextArea) {
        this.logTextArea = logTextArea;
    }

    public void enableFileLogging(String logDir) {
        try {
            Path logPath = Paths.get(logDir);
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
            }

            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            logFile = new File(logDir + File.separator + "obfuscator_" + timestamp + ".log");
            saveToFile = true;

            logToFile("=".repeat(60));
            logToFile("НАЧАЛО СЕАНСА: " + LocalDateTime.now());
            logToFile("=".repeat(60));

        } catch (IOException e) {
            log(LogLevel.ERROR, "Не удалось создать файл логов: " + e.getMessage());
        }
    }

    public void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String logMessage = String.format("[%s] %s %s", timestamp, level.getIcon(), message);

        // Вывод в текстовую область
        if (logTextArea != null) {
            SwingUtilities.invokeLater(() -> {
                logTextArea.setForeground(level.getColor());
                logTextArea.append(logMessage + "\n");
                logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            });
        }

        // Вывод в консоль
        System.out.println(logMessage);

        // Запись в файл
        if (saveToFile && logFile != null) {
            logToFile(logMessage);
        }
    }

    private void logToFile(String message) {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(logFile, true))) {
            writer.println(message);
        } catch (IOException e) {
            System.err.println("Ошибка записи в лог-файл: " + e.getMessage());
        }
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void success(String message) {
        log(LogLevel.SUCCESS, message);
    }

    public void warning(String message) {
        log(LogLevel.WARNING, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void clear() {
        if (logTextArea != null) {
            SwingUtilities.invokeLater(() -> {
                logTextArea.setText("");
            });
        }
    }

    public void saveLogToFile() {
        if (logTextArea == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("obfuscator_log.txt"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt") ||
                        f.getName().toLowerCase().endsWith(".log");
            }

            @Override
            public String getDescription() {
                return "Лог-файлы (*.txt, *.log)";
            }
        });

        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String filePath = fileChooser.getSelectedFile().getPath();
                if (!filePath.toLowerCase().endsWith(".txt") &&
                        !filePath.toLowerCase().endsWith(".log")) {
                    filePath += ".log";
                }

                Files.write(Paths.get(filePath), logTextArea.getText().getBytes());
                success("Логи сохранены в: " + filePath);

            } catch (IOException e) {
                error("Ошибка сохранения логов: " + e.getMessage());
            }
        }
    }

    public String getLogs() {
        return logTextArea != null ? logTextArea.getText() : "";
    }
}