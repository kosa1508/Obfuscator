package com.example.obfuscator;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObfuscatorGUI extends JFrame {

    // Цветовая палитра
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);     // Основной синий
    private static final Color SECONDARY_COLOR = new Color(52, 152, 219);   // Вторичный синий
    private static final Color ACCENT_COLOR = new Color(231, 76, 60);       // Акцентный красный
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113);     // Зеленый успеха
    private static final Color WARNING_COLOR = new Color(241, 196, 15);     // Желтый предупреждения
    private static final Color INFO_COLOR = new Color(155, 89, 182);        // Фиолетовый информации
    private static final Color DARK_BG = new Color(44, 62, 80);             // Темный фон
    private static final Color LIGHT_BG = new Color(236, 240, 241);         // Светлый фон
    private static final Color TEXT_COLOR = new Color(52, 73, 94);          // Цвет текста
    private static final Color CODE_BG = new Color(248, 249, 250);          // Фон для кода

    // Шрифты
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font CODE_FONT = new Font("Consolas", Font.PLAIN, 13);
    private static final Font LOG_FONT = new Font("Consolas", Font.PLAIN, 11);
    private static final Font ICON_FONT = new Font("Segoe UI Emoji", Font.PLAIN, 14);

    // Компоненты интерфейса
    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    private JTextArea logTextArea;
    private JCheckBox enableLoopObfuscationCheckBox;
    private JCheckBox enableAsmObfuscationCheckBox;
    private JCheckBox enableCommentsCheckBox;
    private JCheckBox enableFakeCodeCheckBox;
    private JComboBox<String> astMethodComboBox; // ИЗМЕНЕНО: выбор метода AST обфускации
    private JProgressBar progressBar;
    private JButton obfuscateButton;
    private JButton clearButton;
    private JButton loadFileButton;
    private JButton metricsButton;
    private JButton clearLogsButton;
    private JButton saveLogsButton;
    private JButton viewLogsButton;
    private JComboBox<String> logLevelComboBox;
    private JLabel statusLabel;
    private JLabel titleLabel;
    private JTabbedPane tabbedPane;

    // Метрики обфускации
    private ObfuscationMetrics currentMetrics;

    // Логгер
    private CustomLogger logger;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ObfuscatorGUI() {
        initLogger(); // Инициализируем логгер
        initComponents();
        setupLayout();
        setupListeners();
        setupFrame();
    }

    private void initLogger() {
        logger = new CustomLogger();
    }

    private void initComponents() {
        // Заголовок
        titleLabel = new JLabel("🛡️ JAVA OBFUSCATOR PRO", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBackground(PRIMARY_COLOR);
        titleLabel.setOpaque(true);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Настройка области ввода (без ограничений по размеру)
        inputTextArea = createUnlimitedTextArea("📝 Исходный код Java");
        inputTextArea.setText("// Вставьте сюда Java код для обфускации\n" +
                "public class Example {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\");\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            System.out.println(i);\n" +
                "        }\n" +
                "    }\n" +
                "}");

        // Настройка области вывода (без ограничений по размеру)
        outputTextArea = createUnlimitedTextArea("🔒 Обфусцированный код");
        outputTextArea.setEditable(false);

        // Настройка логов
        logTextArea = new JTextArea();
        logTextArea.setFont(LOG_FONT);
        logTextArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(SECONDARY_COLOR, 2),
                        "📋 Лог обфускации",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        HEADER_FONT,
                        PRIMARY_COLOR
                ),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        logTextArea.setEditable(false);
        logTextArea.setForeground(TEXT_COLOR);
        logTextArea.setBackground(LIGHT_BG);

        // Панель настроек
        enableLoopObfuscationCheckBox = createStyledCheckBox("🔄 Обфускация циклов", true);
        enableAsmObfuscationCheckBox = createStyledCheckBox("⚙️ ASM обфускация (байт-код)", true);
        enableCommentsCheckBox = createStyledCheckBox("💬 Добавлять комментарии", true);
        enableFakeCodeCheckBox = createStyledCheckBox("🎭 Добавлять фиктивный код", true);

        // Выбор метода AST-обфускации (ИЗМЕНЕНО)
        String[] astMethods = {
                "1️⃣ Базовый AST метод (простая обфускация)",
                "2️⃣ Улучшенные циклы (сложная обфускация потоков)",
                "3️⃣ С переименованием (агрессивная обфускация)"
        };
        astMethodComboBox = new JComboBox<>(astMethods);
        astMethodComboBox.setFont(BUTTON_FONT);
        astMethodComboBox.setBackground(Color.WHITE);
        astMethodComboBox.setForeground(TEXT_COLOR);
        astMethodComboBox.setFocusable(false);
        astMethodComboBox.setMaximumRowCount(3);
        astMethodComboBox.setEnabled(true); // Всегда включено, так как AST обфускация всегда выполняется

        // Прогресс бар
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setForeground(SUCCESS_COLOR);
        progressBar.setBackground(LIGHT_BG);
        progressBar.setFont(BUTTON_FONT);
        progressBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        progressBar.setString("Готов");

        // Кнопки управления
        obfuscateButton = createStyledButton("🚀 Запустить обфускацию", PRIMARY_COLOR);
        clearButton = createStyledButton("🗑️ Очистить всё", ACCENT_COLOR);
        loadFileButton = createStyledButton("📂 Загрузить из файла", SUCCESS_COLOR);
        metricsButton = createStyledButton("📊 Показать метрики", WARNING_COLOR);
        clearLogsButton = createStyledButton("🗑️ Очистить логи", new Color(189, 195, 199));
        saveLogsButton = createStyledButton("💾 Сохранить логи", new Color(149, 165, 166));
        viewLogsButton = createStyledButton("👁️ Показать логи", new Color(52, 152, 219));

        metricsButton.setEnabled(false);

        // Выбор уровня логов
        String[] logLevels = {"📊 Все логи", "❌ Только ошибки", "✅ Только успехи", "⚠️ Только предупреждения"};
        logLevelComboBox = new JComboBox<>(logLevels);
        logLevelComboBox.setFont(BUTTON_FONT);
        logLevelComboBox.setBackground(Color.WHITE);
        logLevelComboBox.setForeground(TEXT_COLOR);
        logLevelComboBox.setFocusable(false);
        logLevelComboBox.setMaximumRowCount(4);

        // Статус бар
        statusLabel = new JLabel("✅ Готов к работе");
        statusLabel.setFont(BUTTON_FONT);
        statusLabel.setForeground(SUCCESS_COLOR);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, PRIMARY_COLOR),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        statusLabel.setBackground(new Color(240, 240, 240));
        statusLabel.setOpaque(true);

        // Инициализация вкладок
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(TITLE_FONT);
    }

    private JTextArea createUnlimitedTextArea(String title) {
        JTextArea textArea = new JTextArea() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return false;
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                if (getParent() instanceof JViewport) {
                    dim.width = Math.max(dim.width, getParent().getWidth());
                }
                return dim;
            }
        };

        textArea.setFont(CODE_FONT);
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(SECONDARY_COLOR, 2),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        HEADER_FONT,
                        PRIMARY_COLOR
                ),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        textArea.setBackground(CODE_BG);
        textArea.setForeground(TEXT_COLOR);

        // ОТКЛЮЧАЕМ перенос строк для горизонтальной прокрутки
        textArea.setLineWrap(false);

        // Включаем табуляцию
        textArea.setTabSize(4);

        // Настраиваем скроллинг для больших текстов
        textArea.setCaret(new DefaultCaret() {
            @Override
            public void setSelectionVisible(boolean visible) {
                super.setSelectionVisible(true);
            }
        });

        // Включаем авто-прокрутку при добавлении текста
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        return textArea;
    }

    private JCheckBox createStyledCheckBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setFont(HEADER_FONT);
        checkBox.setForeground(TEXT_COLOR);
        checkBox.setBackground(Color.WHITE);
        checkBox.setFocusPainted(false);
        checkBox.setIconTextGap(10);
        return checkBox;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Эффект при наведении
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(bgColor.brighter().darker(), 2),
                        BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(bgColor.darker(), 1),
                        BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
        });

        return button;
    }

    private void setupLayout() {
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(Color.WHITE);

        // Верхняя панель с заголовком
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        buttonPanel.add(obfuscateButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(loadFileButton);
        buttonPanel.add(metricsButton);

        // Панель настроек
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBackground(Color.WHITE);
        settingsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                        "⚙️ Настройки обфускации",
                        TitledBorder.CENTER,
                        TitledBorder.TOP,
                        TITLE_FONT,
                        PRIMARY_COLOR
                ),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        settingsPanel.add(enableLoopObfuscationCheckBox, gbc);

        gbc.gridx = 1;
        settingsPanel.add(enableAsmObfuscationCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        settingsPanel.add(enableCommentsCheckBox, gbc);

        gbc.gridx = 1;
        settingsPanel.add(enableFakeCodeCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);

        // Панель для выбора метода AST (ИЗМЕНЕНО)
        JPanel astMethodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        astMethodPanel.setBackground(Color.WHITE);
        astMethodPanel.add(new JLabel("🌳 Метод AST обфускации:"));
        astMethodPanel.add(astMethodComboBox);
        settingsPanel.add(astMethodPanel, gbc);

        // Объединяем верхние панели
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(Color.WHITE);
        northPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(Color.WHITE);
        controlPanel.add(buttonPanel, BorderLayout.NORTH);
        controlPanel.add(settingsPanel, BorderLayout.CENTER);
        controlPanel.add(progressBar, BorderLayout.SOUTH);
        northPanel.add(controlPanel, BorderLayout.CENTER);

        // Основная панель с кодом (с улучшенным скроллингом)
        JScrollPane inputScrollPane = new JScrollPane(inputTextArea);
        inputScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        inputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        inputScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        inputScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
        outputScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        outputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        outputScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        outputScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        JSplitPane codeSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputScrollPane, outputScrollPane);
        codeSplitPane.setResizeWeight(0.5);
        codeSplitPane.setDividerLocation(0.5);
        codeSplitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        codeSplitPane.setOneTouchExpandable(true);

        // Панель управления логами
        JPanel logControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        logControlPanel.setBackground(Color.WHITE);
        logControlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        logControlPanel.add(new JLabel("📊 Уровень логов:"));
        logControlPanel.add(logLevelComboBox);
        logControlPanel.add(Box.createHorizontalStrut(20));
        logControlPanel.add(clearLogsButton);
        logControlPanel.add(saveLogsButton);
        logControlPanel.add(viewLogsButton);

        // Нижняя панель с логами
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setPreferredSize(new Dimension(800, 200));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        logPanel.add(logControlPanel, BorderLayout.SOUTH);

        // Создаем вкладки
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(codeSplitPane, BorderLayout.CENTER);

        tabbedPane.addTab("📝 Обфускация", mainPanel);
        tabbedPane.addTab("📊 Метрики", createMetricsPanel());
        tabbedPane.addTab("📋 О логах", createLogsInfoPanel());

        // Устанавливаем фон только для существующих вкладок
        tabbedPane.setBackgroundAt(0, Color.WHITE);
        tabbedPane.setBackgroundAt(1, Color.WHITE);
        tabbedPane.setBackgroundAt(2, Color.WHITE);

        // Главный сплиттер
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, logPanel);
        mainSplitPane.setResizeWeight(0.75);
        mainSplitPane.setDividerLocation(0.75);
        mainSplitPane.setOneTouchExpandable(true);

        // Собираем всё вместе
        add(northPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JTextPane metricsInfo = new JTextPane();
        metricsInfo.setContentType("text/html");
        metricsInfo.setText("<html><body style='font-family: Segoe UI; font-size: 13pt; padding: 20px;'>"
                + "<h2 style='color: #2980b9;'>📊 МЕТРИКИ ОБФУСКАЦИИ</h2>"
                + "<hr style='border: 1px solid #3498db;'>"
                + "<p>Система метрик оценивает эффективность обфускации по нескольким параметрам:</p>"
                + "<h3 style='color: #27ae60;'>📈 Измеряемые параметры:</h3>"
                + "<ul>"
                + "<li><b>Размер файла:</b> увеличение размера после обфускации</li>"
                + "<li><b>Сложность кода:</b> цикломатическая сложность</li>"
                + "<li><b>Читаемость:</b> оценка понятности кода (0-100)</li>"
                + "<li><b>Количество методов:</b> добавление фиктивных методов</li>"
                + "<li><b>Инструкции байт-кода:</b> количество JVM инструкций</li>"
                + "<li><b>Пул констант:</b> размер пула констант класса</li>"
                + "</ul>"
                + "<h3 style='color: #e74c3c;'>🎯 Оценка эффективности:</h3>"
                + "<ul>"
                + "<li>0-40 баллов: низкая эффективность</li>"
                + "<li>41-60 баллов: средняя эффективность</li>"
                + "<li>61-80 баллов: хорошая эффективность</li>"
                + "<li>81-100 баллов: отличная эффективность</li>"
                + "</ul>"
                + "<p style='color: #9b59b6; font-style: italic;'>"
                + "Метрики рассчитываются автоматически после каждой обфускации."
                + "</p>"
                + "</body></html>");
        metricsInfo.setEditable(false);
        metricsInfo.setBackground(new Color(255, 248, 225));
        metricsInfo.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Кнопка для просмотра последних метрик
        JButton viewLastMetricsButton = createStyledButton("📈 Показать последние метрики", WARNING_COLOR);
        viewLastMetricsButton.addActionListener(e -> {
            if (currentMetrics != null) {
                showStyledMetricsReport();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Сначала выполните обфускацию!",
                        "Нет данных",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(viewLastMetricsButton);

        panel.add(new JScrollPane(metricsInfo), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createLogsInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JTextPane logsInfo = new JTextPane();
        logsInfo.setContentType("text/html");
        logsInfo.setText("<html><body style='font-family: Segoe UI; font-size: 13pt; padding: 20px;'>"
                + "<h2 style='color: #2980b9;'>📋 СИСТЕМА ЛОГИРОВАНИЯ</h2>"
                + "<hr style='border: 1px solid #3498db;'>"
                + "<h3 style='color: #27ae60;'>🎯 Функции логгера:</h3>"
                + "<ul>"
                + "<li><b>Реальное время:</b> Логи выводятся сразу в интерфейс</li>"
                + "<li><b>Сохранение в файл:</b> Автоматическое сохранение в папку logs</li>"
                + "<li><b>Фильтрация:</b> Разные уровни логирования (инфо, ошибки, предупреждения)</li>"
                + "<li><b>Очистка:</b> Можно очистить логи в памяти и в файлах</li>"
                + "</ul>"
                + "<h3 style='color: #e74c3c;'>🔧 Использование:</h3>"
                + "<ul>"
                + "<li><b>🗑️ Очистить логи:</b> Удаляет все логи из памяти и интерфейса</li>"
                + "<li><b>💾 Сохранить логи:</b> Сохраняет текущие логи в файл</li>"
                + "<li><b>👁️ Показать логи:</b> Открывает папку с сохраненными логами</li>"
                + "<li><b>📊 Уровень логов:</b> Фильтрует отображаемые сообщения</li>"
                + "</ul>"
                + "<h3 style='color: #9b59b6;'>📁 Расположение логов:</h3>"
                + "<p>Логи сохраняются в папке <b>logs</b> рядом с программой.</p>"
                + "<p>Файлы именуются по шаблону: <i>obfuscation_ГГГГ-ММ-ДД_ЧЧ-мм-сс.log</i></p>"
                + "</body></html>");
        logsInfo.setEditable(false);
        logsInfo.setBackground(new Color(240, 248, 255));
        logsInfo.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Кнопка для тестирования логгера
        JButton testLoggerButton = createStyledButton("🧪 Тест логгера", INFO_COLOR);
        testLoggerButton.addActionListener(e -> {
            logger.info("Тестовое информационное сообщение");
            logger.success("Тестовое сообщение об успехе");
            logger.warning("Тестовое предупреждение");
            logger.error("Тестовая ошибка");
            logger.debug("Тестовое отладочное сообщение");

            JOptionPane.showMessageDialog(this,
                    "Тестовые сообщения добавлены в лог!\n" +
                            "Проверьте область логов внизу окна.",
                    "Тест логгера",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(testLoggerButton);

        panel.add(new JScrollPane(logsInfo), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void setupListeners() {
        // Кнопка обфускации
        obfuscateButton.addActionListener(e -> startObfuscation());

        // Кнопка очистки
        clearButton.addActionListener(e -> {
            inputTextArea.setText("");
            outputTextArea.setText("");
            logger.clear();
            metricsButton.setEnabled(false);
            currentMetrics = null;
            statusLabel.setText("✅ Очищено");
            statusLabel.setForeground(SUCCESS_COLOR);
            progressBar.setValue(0);
            progressBar.setString("Готов");
            logger.info("Очищены все поля");
        });

        // Кнопка загрузки файла
        loadFileButton.addActionListener(e -> loadFromFile());

        // Кнопка показа метрик
        metricsButton.addActionListener(e -> showMetricsReport());

        // Чекбокс ASM обфускации (не зависит от выбора AST метода)
        enableAsmObfuscationCheckBox.addActionListener(e -> {
            logger.info("ASM обфускация (байт-код): " +
                    (enableAsmObfuscationCheckBox.isSelected() ? "включена" : "отключена"));
        });

        // Управление логами
        clearLogsButton.addActionListener(e -> {
            logger.clear();
            logger.info("Логи очищены");
        });

        saveLogsButton.addActionListener(e -> {
            if (logger.saveToFile()) {
                logger.success("Логи успешно сохранены в файл");
            }
        });

        viewLogsButton.addActionListener(e -> {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                boolean created = logsDir.mkdirs();
                if (created) {
                    logger.info("Создана папка logs: " + logsDir.getAbsolutePath());
                }
            }

            if (logsDir.exists() && logsDir.isDirectory()) {
                try {
                    Desktop.getDesktop().open(logsDir);
                    logger.info("Открыта папка с логами: " + logsDir.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Не удалось открыть папку logs: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ObfuscatorGUI.this,
                            "Не удалось открыть папку logs:\n" + ex.getMessage() +
                                    "\n\nПапка находится по пути: " + logsDir.getAbsolutePath(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logger.error("Папка logs не существует или недоступна");
                JOptionPane.showMessageDialog(ObfuscatorGUI.this,
                        "Папка logs не существует или недоступна!",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Выбор уровня логов
        logLevelComboBox.addActionListener(e -> {
            String selected = (String) logLevelComboBox.getSelectedItem();
            logger.setLogLevel(selected);
            logger.info("Уровень логов изменен на: " + selected);
        });

        // Горячие клавиши
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        InputMap inputMap = inputTextArea.getInputMap();
        ActionMap actionMap = inputTextArea.getActionMap();

        // Ctrl+Enter - обфускация
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "obfuscate");
        actionMap.put("obfuscate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                obfuscateButton.doClick();
            }
        });

        // Ctrl+O - загрузка файла
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "load");
        actionMap.put("load", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadFileButton.doClick();
            }
        });

        // Ctrl+M - показать метрики
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK), "metrics");
        actionMap.put("metrics", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                metricsButton.doClick();
            }
        });

        // Ctrl+L - очистить логи
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "clearLogs");
        actionMap.put("clearLogs", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearLogsButton.doClick();
            }
        });
    }

    private void setupFrame() {
        setTitle("🛡️ Java Obfuscator Pro - Профессиональный инструмент для защиты кода");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        // Устанавливаем иконку
        try {
            setIconImage(createAppIcon());
        } catch (Exception e) {
            // Игнорируем ошибку с иконкой
        }

        // Стилизация
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Кастомные настройки
            UIManager.put("TabbedPane.background", Color.WHITE);
            UIManager.put("TabbedPane.foreground", PRIMARY_COLOR);
            UIManager.put("TabbedPane.selected", SECONDARY_COLOR);
            UIManager.put("SplitPane.background", Color.WHITE);
            UIManager.put("SplitPane.dividerSize", 10);

        } catch (Exception e) {
            // Используем стандартный LookAndFeel
        }

        // Делаем окно красивым
        getRootPane().setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 3));

        // Связываем логгер с текстовой областью
        logger.setLogArea(logTextArea);

        // Логируем запуск приложения
        logger.info("🚀 Java Obfuscator Pro запущен");
    }

    private Image createAppIcon() {
        // Создаем простую иконку с логотипом
        int size = 64;
        java.awt.image.BufferedImage icon = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = icon.createGraphics();

        // Включаем сглаживание
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Рисуем фон
        GradientPaint gradient = new GradientPaint(0, 0, PRIMARY_COLOR, size, size, SECONDARY_COLOR);
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, size, size, 20, 20);

        // Рисуем рамку
        g2d.setColor(PRIMARY_COLOR.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(1, 1, size-3, size-3, 20, 20);

        // Рисуем шестеренку (символ обфускации)
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));

        // Внешний круг
        g2d.drawOval(size/4, size/4, size/2, size/2);

        // Зубья шестеренки
        int centerX = size/2;
        int centerY = size/2;
        int radius = size/4;

        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            double x1 = centerX + radius * Math.cos(angle);
            double y1 = centerY + radius * Math.sin(angle);
            double x2 = centerX + (radius + 8) * Math.cos(angle);
            double y2 = centerY + (radius + 8) * Math.sin(angle);

            g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
        }

        g2d.dispose();
        return icon;
    }

    private void startObfuscation() {
        String sourceCode = inputTextArea.getText().trim();

        if (sourceCode.isEmpty()) {
            logger.error("Не введен код для обфускации!");
            JOptionPane.showMessageDialog(this,
                    "Введите Java код для обфускации!",
                    "Ошибка",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Отключаем кнопки на время обработки
        setControlsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setString("Начинаем...");
        logger.clear();
        logger.info("🎯 === НАЧАЛО СЕАНСА ОБФУСКАЦИИ ===");
        logger.info("⚙️ Настройки обфускации:");
        logger.info("   🔄 Обфускация циклов: " + enableLoopObfuscationCheckBox.isSelected());
        logger.info("   ⚙️ ASM обфускация (байт-код): " + enableAsmObfuscationCheckBox.isSelected());
        // Логируем выбранный метод AST обфускации
        String selectedMethod = (String) astMethodComboBox.getSelectedItem();
        logger.info("   🌳 Метод AST обфускации: " + selectedMethod);

        statusLabel.setText("⏳ Начинаем обфускацию...");
        statusLabel.setForeground(PRIMARY_COLOR);

        // Запускаем в отдельном потоке
        executor.submit(() -> {
            try {
                // Шаг 0: Рассчитываем метрики исходного кода
                progressBar.setValue(5);
                progressBar.setString("Анализ кода...");
                statusLabel.setText("🔍 Анализируем исходный код...");
                logger.info("📊 === АНАЛИЗ ИСХОДНОГО КОДА ===");

                ObfuscationMetrics.Metrics originalSourceMetrics =
                        ObfuscationMetrics.calculateSourceCodeMetrics(sourceCode);
                logger.success("✅ Рассчитаны метрики исходного кода");
                logger.info("   📏 Строк кода: " + originalSourceMetrics.getLinesOfCode());
                logger.info("   🛠️ Методов: " + originalSourceMetrics.getNumberOfMethods());
                logger.info("   🧮 Цикломатическая сложность: " + originalSourceMetrics.getCyclomaticComplexity());

                // Шаг 1: Сохраняем код во временный файл
                progressBar.setValue(10);
                progressBar.setString("Подготовка...");
                statusLabel.setText("📁 Создаем временный файл...");
                logger.info("📦 === ПОДГОТОВКА К ОБФУСКАЦИИ ===");
                logger.info("📝 Создаем временный файл...");

                Path tempDir = Files.createTempDirectory("obfuscator_");
                Path inputFile = tempDir.resolve("InputClass.java");
                Files.write(inputFile, sourceCode.getBytes());
                logger.success("✅ Создан временный файл: " + inputFile);

                // Шаг 2: AST обфускация (всегда выполняется)
                progressBar.setValue(30);
                progressBar.setString("AST обфускация...");
                statusLabel.setText("🌳 Выполняем AST обфускацию...");
                logger.info("🔄 [1/2] Применяем AST-обфускацию...");

                SimpleObfuscator simpleObf = new SimpleObfuscator();
                SimpleObfuscator.Result astResult = simpleObf.obfuscate(inputFile.toString());

                logger.success("✅ AST-обфускация завершена");
                logger.info("   🏷️ Имя класса: " + astResult.className);

                Path javaFilePath = tempDir.resolve(astResult.className + ".java");
                Files.write(javaFilePath, astResult.source.getBytes());

                // Создаем объект метрик с обновленным кодом
                currentMetrics = new ObfuscationMetrics(astResult.className);
                currentMetrics.setOriginalMetrics(originalSourceMetrics);

                // Рассчитываем метрики после AST обфускации
                ObfuscationMetrics.Metrics astMetrics = ObfuscationMetrics.calculateSourceCodeMetrics(astResult.source);
                currentMetrics.setObfuscatedMetrics(astMetrics);

                // Обновляем output text area
                final String obfuscatedCode = astResult.source;
                SwingUtilities.invokeLater(() -> {
                    outputTextArea.setText(obfuscatedCode);
                    outputTextArea.setCaretPosition(0);
                });

                // Шаг 3: Компиляция и ASM обфускация (опционально)
                if (enableAsmObfuscationCheckBox.isSelected()) {
                    progressBar.setValue(60);
                    progressBar.setString("Компиляция...");
                    statusLabel.setText("⚙️ Компилируем код...");
                    logger.info("🔧 [2/2] Компилируем и применяем ASM-обфускацию...");

                    try {
                        ProcessBuilder javacBuilder = new ProcessBuilder(
                                "javac",
                                "-encoding", "UTF-8",
                                "-d", tempDir.toString(),
                                javaFilePath.toString()
                        );

                        Process javac = javacBuilder.start();
                        String output = new String(javac.getInputStream().readAllBytes());
                        int code = javac.waitFor();

                        if (code != 0) {
                            logger.error("❌ Ошибка компиляции:");
                            logger.error(output.substring(0, Math.min(output.length(), 500)));
                            throw new RuntimeException("Ошибка компиляции");
                        }

                        logger.success("✅ Компиляция успешна");

                        // Шаг 4: ASM обфускация байт-кода
                        progressBar.setValue(80);
                        progressBar.setString("ASM обфускация...");
                        statusLabel.setText("🔧 Выполняем ASM обфускацию байт-кода...");
                        logger.info("🔬 Применяем ASM-обфускацию байт-кода...");

                        Path classFile = tempDir.resolve(astResult.className + ".class");
                        if (Files.exists(classFile)) {
                            // Рассчитываем метрики оригинального байт-кода
                            ObfuscationMetrics.Metrics originalBytecodeMetrics =
                                    ObfuscationMetrics.calculateBytecodeMetrics(classFile);

                            // Применяем ASM обфускацию (всегда базовый метод, так как нет выбора)
                            AsmObfuscator asmObf = new AsmObfuscator();
                            Path asmClassFile = tempDir.resolve(astResult.className + "_obf.class");

                            // Всегда используем базовый метод ASM обфускации
                            asmObf.obfuscateClass(classFile, asmClassFile);

                            logger.success("✅ ASM-обфускация байт-кода завершена");

                            // Рассчитываем метрики обфусцированного байт-кода
                            ObfuscationMetrics.Metrics obfuscatedBytecodeMetrics =
                                    ObfuscationMetrics.calculateBytecodeMetrics(asmClassFile);

                            // Обновляем метрики
                            currentMetrics.getOriginalMetrics().setFileSizeBytes(
                                    originalBytecodeMetrics.getFileSizeBytes());
                            currentMetrics.getOriginalMetrics().setBytecodeInstructions(
                                    originalBytecodeMetrics.getBytecodeInstructions());
                            currentMetrics.getOriginalMetrics().setConstantPoolSize(
                                    originalBytecodeMetrics.getConstantPoolSize());

                            currentMetrics.getObfuscatedMetrics().setFileSizeBytes(
                                    obfuscatedBytecodeMetrics.getFileSizeBytes());
                            currentMetrics.getObfuscatedMetrics().setBytecodeInstructions(
                                    obfuscatedBytecodeMetrics.getBytecodeInstructions());
                            currentMetrics.getObfuscatedMetrics().setConstantPoolSize(
                                    obfuscatedBytecodeMetrics.getConstantPoolSize());

                            logger.info("   📏 Размер файла: " + originalBytecodeMetrics.getFileSizeBytes() +
                                    " → " + obfuscatedBytecodeMetrics.getFileSizeBytes() + " байт");
                            logger.info("   🧩 Инструкций байт-кода: " + originalBytecodeMetrics.getBytecodeInstructions() +
                                    " → " + obfuscatedBytecodeMetrics.getBytecodeInstructions());
                            logger.info("   💾 Создан файл: " + asmClassFile);
                        }
                    } catch (Exception e) {
                        logger.warning("⚠️ ASM обфускация пропущена: " + e.getMessage());
                        logger.debug("Детали ошибки: " + e.getMessage());
                    }
                }

                // Финальный шаг: показываем отчет
                progressBar.setValue(95);
                progressBar.setString("Генерация отчета...");
                statusLabel.setText("📊 Генерируем отчет...");
                logger.info("📈 === ФИНАЛЬНЫЙ ОТЧЕТ ===");

                if (currentMetrics != null) {
                    String metricsReport = currentMetrics.getReport();
                    logger.info(metricsReport);
                }

                progressBar.setValue(100);
                progressBar.setString("Готово!");
                statusLabel.setText("✅ Обфускация завершена успешно!");
                statusLabel.setForeground(SUCCESS_COLOR);
                logger.success("🎉 === ОБФУСКАЦИЯ УСПЕШНО ЗАВЕРШЕНА ===");

                // Показываем диалог с отчетом
                SwingUtilities.invokeLater(() -> {
                    showStyledMetricsReport();

                    metricsButton.setEnabled(true);
                    setControlsEnabled(true);
                    progressBar.setVisible(false);

                    // Переключаемся на вкладку метрик
                    tabbedPane.setSelectedIndex(1);

                    // Анимация успеха
                    Timer timer = new Timer(300, event -> {
                        statusLabel.setForeground(SUCCESS_COLOR.darker());
                    });
                    timer.setRepeats(false);
                    timer.start();

                    Timer timer2 = new Timer(600, event -> {
                        statusLabel.setForeground(SUCCESS_COLOR);
                    });
                    timer2.setRepeats(false);
                    timer2.start();
                });

                // Очищаем временные файлы
                try {
                    Files.walk(tempDir)
                            .sorted((a, b) -> -a.compareTo(b))
                            .forEach(path -> {
                                try { Files.delete(path); }
                                catch (IOException e) { /* Игнорируем */ }
                            });
                    logger.debug("🧹 Временные файлы очищены");
                } catch (IOException e) {
                    logger.warning("⚠️ Ошибка при удалении временных файлов: " + e.getMessage());
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("❌ Ошибка: " + e.getMessage());
                    statusLabel.setForeground(ACCENT_COLOR);
                    logger.error("💥 ОШИБКА: " + e.getMessage());
                    logger.debug("Детали ошибки: " + e.getMessage());
                    setControlsEnabled(true);
                    progressBar.setVisible(false);
                    progressBar.setString("Ошибка!");
                });
            }
        });
    }

    private void showStyledMetricsReport() {
        if (currentMetrics == null) {
            JOptionPane.showMessageDialog(this,
                    "Сначала выполните обфускацию!",
                    "Нет данных",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String report = currentMetrics.getReport();

        // Создаем красивое диалоговое окно с отчетом
        JDialog dialog = new JDialog(this, "📊 Отчет об обфускации", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(1000, 750);
        dialog.setLocationRelativeTo(this);

        // Заголовок
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("📊 Детальный отчет об обфускации");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton closeButton = createStyledButton("❌ Закрыть", ACCENT_COLOR);
        closeButton.addActionListener(e -> dialog.dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);

        // Область с отчетом
        JTextArea reportArea = new JTextArea(report);
        reportArea.setFont(CODE_FONT);
        reportArea.setEditable(false);
        reportArea.setBackground(new Color(245, 245, 245));
        reportArea.setForeground(TEXT_COLOR);
        reportArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane scrollPane = new JScrollPane(reportArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 2));

        // Панель с действиями
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JButton copyButton = createStyledButton("📋 Копировать отчет", SECONDARY_COLOR);
        copyButton.addActionListener(e -> {
            reportArea.selectAll();
            reportArea.copy();
            reportArea.select(0, 0);
            JOptionPane.showMessageDialog(dialog,
                    "📋 Отчет скопирован в буфер обмена!",
                    "✅ Успех",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        JButton saveButton = createStyledButton("💾 Сохранить отчет", SUCCESS_COLOR);
        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("obfuscation_report.txt"));
            int result = fileChooser.showSaveDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    String filePath = fileChooser.getSelectedFile().getPath();
                    if (!filePath.toLowerCase().endsWith(".txt")) {
                        filePath += ".txt";
                    }
                    Files.write(Paths.get(filePath), report.getBytes());
                    JOptionPane.showMessageDialog(dialog,
                            "✅ Отчет сохранен в:\n" + filePath,
                            "💾 Успех",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "❌ Ошибка сохранения: " + ex.getMessage(),
                            "💥 Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        actionPanel.add(copyButton);
        actionPanel.add(saveButton);
        actionPanel.add(closeButton);

        // Собираем диалог
        dialog.add(headerPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(actionPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showMetricsReport() {
        showStyledMetricsReport();
    }

    private void loadFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("📂 Выберите Java файл для загрузки");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".java");
            }

            @Override
            public String getDescription() {
                return "Java файлы (*.java)";
            }
        });

        // Стилизация FileChooser
        fileChooser.setBackground(Color.WHITE);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String content = new String(Files.readAllBytes(fileChooser.getSelectedFile().toPath()));
                inputTextArea.setText(content);
                inputTextArea.setCaretPosition(0);
                statusLabel.setText("📂 Загружен файл: " + fileChooser.getSelectedFile().getName());
                statusLabel.setForeground(SUCCESS_COLOR);
                logger.success("✅ Загружен файл: " + fileChooser.getSelectedFile().getName());

                // Подсветка успешной загрузки
                inputTextArea.setBackground(new Color(230, 255, 230));
                Timer timer = new Timer(1000, event -> {
                    inputTextArea.setBackground(CODE_BG);
                });
                timer.setRepeats(false);
                timer.start();

            } catch (IOException e) {
                logger.error("❌ Ошибка загрузки файла: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "❌ Ошибка загрузки файла:\n" + e.getMessage(),
                        "💥 Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void setControlsEnabled(boolean enabled) {
        obfuscateButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        loadFileButton.setEnabled(enabled);
        metricsButton.setEnabled(!enabled ? metricsButton.isEnabled() : false);
        enableLoopObfuscationCheckBox.setEnabled(enabled);
        enableAsmObfuscationCheckBox.setEnabled(enabled);
        enableCommentsCheckBox.setEnabled(enabled);
        enableFakeCodeCheckBox.setEnabled(enabled);
        astMethodComboBox.setEnabled(enabled); // AST метод всегда доступен
        inputTextArea.setEnabled(enabled);
        clearLogsButton.setEnabled(enabled);
        saveLogsButton.setEnabled(enabled);
        viewLogsButton.setEnabled(enabled);
        logLevelComboBox.setEnabled(enabled);

        // Визуальная обратная связь
        if (!enabled) {
            obfuscateButton.setBackground(PRIMARY_COLOR.darker());
            obfuscateButton.setText("⏳ Обработка...");
            statusLabel.setForeground(WARNING_COLOR);
        } else {
            obfuscateButton.setBackground(PRIMARY_COLOR);
            obfuscateButton.setText("🚀 Запустить обфускацию");
            if (statusLabel.getText().contains("✅")) {
                statusLabel.setForeground(SUCCESS_COLOR);
            }
        }
    }

    public static void main(String[] args) {
        // Устанавливаем тему для всего приложения
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Кастомные настройки для лучшего вида
            UIManager.put("Panel.background", Color.WHITE);
            UIManager.put("OptionPane.background", Color.WHITE);
            UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 12));

        } catch (Exception e) {
            // Используем стандартный LookAndFeel
        }

        // Отключаем предупреждения от IntelliJ IDEA
        System.setProperty("java.awt.datatransfer.disableNonStandardDataFlavors", "true");

        SwingUtilities.invokeLater(() -> {
            ObfuscatorGUI gui = new ObfuscatorGUI();
            gui.setVisible(true);
        });
    }

    // Вложенный класс для кастомного логгера
    class CustomLogger {
        private JTextArea logArea;
        private StringBuilder logBuffer = new StringBuilder();
        private String logLevel = "📊 Все логи";

        public void setLogArea(JTextArea logArea) {
            this.logArea = logArea;
        }

        public void setLogLevel(String level) {
            this.logLevel = level;
        }

        private boolean shouldLog(String level) {
            if (logLevel.equals("📊 Все логи")) return true;
            if (logLevel.equals("❌ Только ошибки")) return level.equals("ERROR");
            if (logLevel.equals("✅ Только успехи")) return level.equals("SUCCESS");
            if (logLevel.equals("⚠️ Только предупреждения")) return level.equals("WARNING");
            return true;
        }

        private void addLog(String message, String level, String emoji) {
            if (!shouldLog(level)) return;

            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String logEntry = String.format("[%s] %s %s\n", timestamp, emoji, message);

            logBuffer.append(logEntry);

            if (logArea != null) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(logEntry);
                    // Автоскроллинг к концу
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        }

        public void info(String message) {
            addLog(message, "INFO", "🔍");
        }

        public void success(String message) {
            addLog(message, "SUCCESS", "✅");
        }

        public void warning(String message) {
            addLog(message, "WARNING", "⚠️");
        }

        public void error(String message) {
            addLog(message, "ERROR", "❌");
        }

        public void debug(String message) {
            addLog(message, "DEBUG", "🐛");
        }

        public void clear() {
            logBuffer.setLength(0);
            if (logArea != null) {
                SwingUtilities.invokeLater(() -> {
                    logArea.setText("");
                });
            }
        }

        public boolean saveToFile() {
            try {
                File logsDir = new File("logs");
                if (!logsDir.exists()) {
                    logsDir.mkdirs();
                }

                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                File logFile = new File(logsDir, "obfuscation_" + timestamp + ".log");

                try (PrintWriter writer = new PrintWriter(new FileWriter(logFile))) {
                    writer.write("=== Логи обфускатора Java ===\n");
                    writer.write("Дата: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                    writer.write("================================\n\n");
                    writer.write(logBuffer.toString());
                }

                info("Логи сохранены в: " + logFile.getAbsolutePath());
                return true;
            } catch (Exception e) {
                error("Ошибка сохранения логов: " + e.getMessage());
                return false;
            }
        }
    }
}