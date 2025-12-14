package com.example.obfuscator;

import javax.swing.*;

public class ObfuscatorApp {
    public static void main(String[] args) {
        // Если нет аргументов или есть аргумент --gui, запускаем GUI
        if (args.length == 0 || (args.length == 1 && args[0].equals("--gui"))) {
            launchGUI();
        } else {
            // Иначе запускаем консольную версию
            try {
                runConsoleMode(args);
            } catch (Exception e) {
                System.err.println("Ошибка: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void launchGUI() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Устанавливаем системный Look and Feel для лучшего вида
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Если не получилось, используем стандартный
            }

            ObfuscatorGUI gui = new ObfuscatorGUI();
            gui.setVisible(true);

            // Показываем сообщение о режиме
            System.out.println("Запущен GUI режим обфускатора");
            System.out.println("Для консольного режима используйте: java -jar Obfuscator.jar input.java outputDir");
        });
    }

    private static void runConsoleMode(String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String input = args[0];
        String outputDir = args[1];

        // Проверяем дополнительные опции
        boolean enableLoopObfuscation = false;
        boolean enableAsmObfuscation = true;

        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("--loop-obfuscation")) {
                enableLoopObfuscation = true;
            } else if (args[i].equals("--no-asm")) {
                enableAsmObfuscation = false;
            } else if (args[i].equals("--help") || args[i].equals("-h")) {
                printUsage();
                return;
            }
        }

        // Проверяем существование входного файла
        java.nio.file.Path inputPath = java.nio.file.Paths.get(input);
        if (!java.nio.file.Files.exists(inputPath)) {
            System.err.println("Ошибка: входной файл не найден: " + input);
            return;
        }

        // Создаем директорию для вывода
        java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir);
        if (!java.nio.file.Files.exists(outputPath)) {
            java.nio.file.Files.createDirectories(outputPath);
        }

        System.out.println("=== Java Obfuscator - Консольный режим ===");
        System.out.println("Входной файл: " + input);
        System.out.println("Выходная директория: " + outputDir);
        System.out.println("Расширенная обфускация циклов: " + (enableLoopObfuscation ? "ВКЛЮЧЕНА" : "ВЫКЛЮЧЕНА"));
        System.out.println("ASM обфускация: " + (enableAsmObfuscation ? "ВКЛЮЧЕНА" : "ВЫКЛЮЧЕНА"));

        // 1. AST обфускация (JavaParser)
        System.out.println("\n[1/3] Применяем AST-обфускацию...");
        SimpleObfuscator simpleObf = new SimpleObfuscator();
        SimpleObfuscator.Result result = simpleObf.obfuscate(input);

        // Сохраняем обфусцированный Java файл
        java.nio.file.Path javaFilePath = outputPath.resolve(result.className + ".java");
        java.nio.file.Files.write(javaFilePath, result.source.getBytes());
        System.out.println("✓ AST-обфускация завершена");
        System.out.println("  Создан файл: " + javaFilePath);

        // 2. Компиляция
        System.out.println("\n[2/3] Компилируем обфусцированный код...");
        try {
            ProcessBuilder javacBuilder = new ProcessBuilder(
                    "javac",
                    "-encoding", "UTF-8",
                    "-d", outputDir,
                    javaFilePath.toString()
            );

            Process javac = javacBuilder
                    .redirectErrorStream(true)
                    .start();

            String output = new String(javac.getInputStream().readAllBytes());
            int code = javac.waitFor();

            if (code != 0) {
                System.err.println("✗ Ошибка компиляции:");
                System.err.println(output.substring(0, Math.min(output.length(), 500)));
                return;
            }

            System.out.println("✓ Компиляция успешна");

        } catch (Exception e) {
            System.err.println("✗ Ошибка при компиляции: " + e.getMessage());
            return;
        }

        // 3. ASM обфускация (если включена)
        if (enableAsmObfuscation) {
            System.out.println("\n[3/3] Применяем ASM-обфускацию...");
            java.nio.file.Path classFile = outputPath.resolve(result.className + ".class");

            if (java.nio.file.Files.exists(classFile)) {
                AsmObfuscator asmObf = new AsmObfuscator();

                try {
                    // 3.1. Базовая обфускация
                    java.nio.file.Path asmClassFile = outputPath.resolve(result.className + "_obf.class");
                    asmObf.obfuscateClass(classFile, asmClassFile);
                    System.out.println("✓ Базовая ASM-обфускация завершена");
                    System.out.println("  Создан файл: " + asmClassFile);

                    // 3.2. Расширенная обфускация циклов (если включена)
                    if (enableLoopObfuscation) {
                        java.nio.file.Path enhancedClassFile = outputPath.resolve(result.className + "_enhanced.class");
                        asmObf.obfuscateWithEnhancedLoops(classFile, enhancedClassFile);
                        System.out.println("✓ Расширенная обфускация циклов завершена");
                        System.out.println("  Создан файл: " + enhancedClassFile);
                    }

                    // 3.3. Дополнительно: обфускация с переименованием
                    java.nio.file.Path renamedClassFile = outputPath.resolve(result.className + "_renamed.class");
                    asmObf.obfuscateWithRenaming(classFile, renamedClassFile);
                    System.out.println("✓ Обфускация с переименованием завершена");
                    System.out.println("  Создан файл: " + renamedClassFile);

                } catch (Exception e) {
                    System.err.println("✗ Ошибка ASM-обфускации: " + e.getMessage());
                    System.err.println("  Пробуем создать хотя бы .class файл...");

                    // Копируем оригинальный .class файл как резервный вариант
                    java.nio.file.Path backupClassFile = outputPath.resolve(result.className + "_backup.class");
                    java.nio.file.Files.copy(classFile, backupClassFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("  Создан резервный файл: " + backupClassFile);
                }
            } else {
                System.err.println("✗ Class файл не найден: " + classFile);
            }
        } else {
            System.out.println("\n[3/3] ASM-обфускация пропущена по запросу пользователя");
        }

        // 4. Итоги
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ОБФУСКАЦИЯ ЗАВЕРШЕНА!");
        System.out.println("=".repeat(60));
        System.out.println("Созданы следующие файлы:");

        try {
            java.nio.file.Files.list(outputPath)
                    .filter(p -> p.getFileName().toString().contains(result.className))
                    .sorted()
                    .forEach(p -> {
                        try {
                            long size = java.nio.file.Files.size(p);
                            System.out.printf("  • %-40s (%d bytes)%n",
                                    p.getFileName(), size);
                        } catch (java.io.IOException e) {
                            System.out.println("  • " + p.getFileName());
                        }
                    });
        } catch (java.io.IOException e) {
            System.out.println("Не удалось получить список файлов");
        }

        System.out.println("=".repeat(60));

        // Рекомендуем основной файл в зависимости от настроек
        if (enableLoopObfuscation && enableAsmObfuscation) {
            System.out.println("Основной результат: " + result.className + "_enhanced.class");
            System.out.println("(с расширенной обфускацией циклов)");
        } else if (enableAsmObfuscation) {
            System.out.println("Основной результат: " + result.className + "_obf.class");
        } else {
            System.out.println("Основной результат: " + result.className + ".class");
            System.out.println("(ASM-обфускация отключена)");
        }

        System.out.println("=".repeat(60));

        // Выводим рекомендации по использованию
        System.out.println("\nРекомендации по использованию:");
        if (enableLoopObfuscation) {
            System.out.println("✓ Используйте файл с суффиксом '_enhanced.class' для максимальной защиты");
        } else {
            System.out.println("✓ Запустите с опцией '--loop-obfuscation' для лучшей защиты циклов");
        }

        // Информация о размерах файлов
        try {
            java.nio.file.Path originalClass = outputPath.resolve(result.className + ".class");
            java.nio.file.Path obfuscatedClass = outputPath.resolve(result.className + "_obf.class");

            if (java.nio.file.Files.exists(originalClass) && java.nio.file.Files.exists(obfuscatedClass)) {
                long originalSize = java.nio.file.Files.size(originalClass);
                long obfuscatedSize = java.nio.file.Files.size(obfuscatedClass);
                double increase = ((double)obfuscatedSize / originalSize - 1) * 100;

                System.out.printf("✓ Размер файла увеличен на: %.1f%%\n", increase);
            }
        } catch (java.io.IOException e) {
            // Игнорируем ошибки при получении размеров
        }
    }

    private static void printUsage() {
        System.out.println("=== Java Obfuscator ===");
        System.out.println("Два режима работы:");
        System.out.println();
        System.out.println("1. GUI режим (по умолчанию):");
        System.out.println("   java -jar Obfuscator.jar");
        System.out.println("   или");
        System.out.println("   java -jar Obfuscator.jar --gui");
        System.out.println();
        System.out.println("2. Консольный режим:");
        System.out.println("   java -jar Obfuscator.jar input.java outputDir [опции]");
        System.out.println();
        System.out.println("Опции:");
        System.out.println("  --loop-obfuscation     Включить расширенную обфускацию циклов");
        System.out.println("  --no-asm               Отключить ASM обфускацию");
        System.out.println("  --help, -h             Показать эту справку");
        System.out.println();
        System.out.println("Примеры:");
        System.out.println("  java -jar Obfuscator.jar Test.java ./output");
        System.out.println("  java -jar Obfuscator.jar MyClass.java ./dist --loop-obfuscation");
        System.out.println("  java -jar Obfuscator.jar App.java ./build --no-asm");
    }
}