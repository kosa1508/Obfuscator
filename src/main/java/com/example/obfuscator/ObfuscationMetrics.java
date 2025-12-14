package com.example.obfuscator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ObfuscationMetrics {

    private String className;
    private Metrics originalMetrics;
    private Metrics obfuscatedMetrics;

    public static class Metrics {
        // Размер файла
        private long fileSizeBytes;

        // Метрики исходного кода
        private int linesOfCode;
        private int numberOfMethods;
        private int numberOfClasses;
        private int numberOfVariables;

        // Сложность кода
        private int cyclomaticComplexity;
        private int nestingDepth;

        // Читаемость
        private double readabilityScore;

        // Дополнительные метрики для байт-кода
        private int bytecodeInstructions;
        private int constantPoolSize;
        private int numberOfFields;

        // Геттеры и сеттеры
        public long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

        public int getLinesOfCode() { return linesOfCode; }
        public void setLinesOfCode(int linesOfCode) { this.linesOfCode = linesOfCode; }

        public int getNumberOfMethods() { return numberOfMethods; }
        public void setNumberOfMethods(int numberOfMethods) { this.numberOfMethods = numberOfMethods; }

        public int getNumberOfClasses() { return numberOfClasses; }
        public void setNumberOfClasses(int numberOfClasses) { this.numberOfClasses = numberOfClasses; }

        public int getNumberOfVariables() { return numberOfVariables; }
        public void setNumberOfVariables(int numberOfVariables) { this.numberOfVariables = numberOfVariables; }

        public int getCyclomaticComplexity() { return cyclomaticComplexity; }
        public void setCyclomaticComplexity(int cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }

        public int getNestingDepth() { return nestingDepth; }
        public void setNestingDepth(int nestingDepth) { this.nestingDepth = nestingDepth; }

        public double getReadabilityScore() { return readabilityScore; }
        public void setReadabilityScore(double readabilityScore) { this.readabilityScore = readabilityScore; }

        public int getBytecodeInstructions() { return bytecodeInstructions; }
        public void setBytecodeInstructions(int bytecodeInstructions) { this.bytecodeInstructions = bytecodeInstructions; }

        public int getConstantPoolSize() { return constantPoolSize; }
        public void setConstantPoolSize(int constantPoolSize) { this.constantPoolSize = constantPoolSize; }

        public int getNumberOfFields() { return numberOfFields; }
        public void setNumberOfFields(int numberOfFields) { this.numberOfFields = numberOfFields; }

        @Override
        public String toString() {
            return String.format(
                    "Размер файла: %d байт\n" +
                            "Строк кода: %d\n" +
                            "Методов: %d\n" +
                            "Классов: %d\n" +
                            "Переменных: %d\n" +
                            "Цикломатическая сложность: %d\n" +
                            "Макс. глубина вложенности: %d\n" +
                            "Оценка читаемости: %.2f\n" +
                            "Инструкций байт-кода: %d\n" +
                            "Размер пула констант: %d\n" +
                            "Полей: %d",
                    fileSizeBytes, linesOfCode, numberOfMethods, numberOfClasses,
                    numberOfVariables, cyclomaticComplexity, nestingDepth,
                    readabilityScore, bytecodeInstructions, constantPoolSize, numberOfFields
            );
        }
    }

    // Конструктор
    public ObfuscationMetrics(String className) {
        this.className = className;
        this.originalMetrics = new Metrics();
        this.obfuscatedMetrics = new Metrics();
    }

    // Метод для расчета метрик исходного кода
    public static Metrics calculateSourceCodeMetrics(String sourceCode) {
        Metrics metrics = new Metrics();

        // Считаем строки кода (исключая пустые строки и комментарии)
        String[] lines = sourceCode.split("\n");
        int codeLines = 0;
        boolean inBlockComment = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
                continue;
            }

            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.startsWith("//")) {
                continue;
            }

            if (trimmed.startsWith("/*")) {
                inBlockComment = true;
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
                continue;
            }

            codeLines++;
        }

        metrics.setLinesOfCode(codeLines);

        // Простые эвристики для подсчета методов, классов и переменных
        metrics.setNumberOfMethods(countOccurrences(sourceCode, "public.*\\(|private.*\\(|protected.*\\("));
        metrics.setNumberOfClasses(countOccurrences(sourceCode, "class\\s+\\w+|interface\\s+\\w+"));
        metrics.setNumberOfVariables(countOccurrences(sourceCode, "int\\s+\\w+|String\\s+\\w+|double\\s+\\w+"));

        // Оценка цикломатической сложности
        metrics.setCyclomaticComplexity(estimateCyclomaticComplexity(sourceCode));

        // Оценка глубины вложенности
        metrics.setNestingDepth(estimateNestingDepth(sourceCode));

        // Оценка читаемости (простая эвристика)
        metrics.setReadabilityScore(estimateReadability(sourceCode));

        return metrics;
    }

    // Метод для расчета метрик байт-кода
    public static Metrics calculateBytecodeMetrics(Path classFile) throws IOException {
        Metrics metrics = new Metrics();

        // Размер файла
        if (Files.exists(classFile)) {
            metrics.setFileSizeBytes(Files.size(classFile));
        }

        // Попытка получить информацию о байт-коде через javap
        try {
            ProcessBuilder pb = new ProcessBuilder("javap", "-v", classFile.toString());
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());

            // Считаем инструкции (грубая оценка)
            int instructions = countOccurrences(output, "\\d+:\\s+\\w+");
            metrics.setBytecodeInstructions(instructions);

            // Считаем размер пула констант
            int constantPool = countOccurrences(output, "#\\d+ = ");
            metrics.setConstantPoolSize(constantPool);

            // Считаем поля
            int fields = countOccurrences(output, "private|public|protected\\s+\\w+\\s+\\w+;");
            metrics.setNumberOfFields(fields);

        } catch (Exception e) {
            // Если javap не доступен, используем приблизительные значения
            System.err.println("Не удалось получить метрики байт-кода: " + e.getMessage());
        }

        return metrics;
    }

    // Вспомогательные методы
    private static int countOccurrences(String text, String pattern) {
        // Простая реализация подсчета вхождений
        return text.split(pattern).length - 1;
    }

    private static int estimateCyclomaticComplexity(String sourceCode) {
        // Упрощенная оценка цикломатической сложности
        int complexity = 1; // Базовая сложность

        complexity += countOccurrences(sourceCode, "if\\s*\\(|else\\s+if");
        complexity += countOccurrences(sourceCode, "while\\s*\\(|for\\s*\\(");
        complexity += countOccurrences(sourceCode, "case\\s+\\w+:|default:");
        complexity += countOccurrences(sourceCode, "catch\\s*\\(");
        complexity += countOccurrences(sourceCode, "\\&\\&|\\|\\|");

        return complexity;
    }

    private static int estimateNestingDepth(String sourceCode) {
        // Оценка максимальной глубины вложенности
        int maxDepth = 0;
        int currentDepth = 0;

        for (char c : sourceCode.toCharArray()) {
            if (c == '{') {
                currentDepth++;
                if (currentDepth > maxDepth) {
                    maxDepth = currentDepth;
                }
            } else if (c == '}') {
                currentDepth--;
            }
        }

        return maxDepth;
    }

    private static double estimateReadability(String sourceCode) {
        // Простая оценка читаемости (0-100)
        double score = 100.0;

        // Штраф за длинные строки
        String[] lines = sourceCode.split("\n");
        int longLines = 0;
        for (String line : lines) {
            if (line.length() > 120) {
                longLines++;
            }
        }
        score -= (longLines * 2.0);

        // Штраф за высокую вложенность
        int nestingDepth = estimateNestingDepth(sourceCode);
        if (nestingDepth > 5) {
            score -= (nestingDepth - 5) * 5.0;
        }

        // Штраф за слишком много методов в одном классе
        int methodCount = countOccurrences(sourceCode, "public.*\\(|private.*\\(|protected.*\\(");
        if (methodCount > 15) {
            score -= (methodCount - 15) * 1.0;
        }

        return Math.max(0, score);
    }

    // Геттеры и сеттеры
    public Metrics getOriginalMetrics() { return originalMetrics; }
    public void setOriginalMetrics(Metrics originalMetrics) { this.originalMetrics = originalMetrics; }

    public Metrics getObfuscatedMetrics() { return obfuscatedMetrics; }
    public void setObfuscatedMetrics(Metrics obfuscatedMetrics) { this.obfuscatedMetrics = obfuscatedMetrics; }

    public String getClassName() { return className; }

    // Метод для получения отчета
    public String getReport() {
        StringBuilder report = new StringBuilder();

        report.append("=".repeat(60)).append("\n");
        report.append("ОТЧЕТ ОБ ОБФУСКАЦИИ: ").append(className).append("\n");
        report.append("=".repeat(60)).append("\n\n");

        report.append("ИСХОДНЫЙ КОД:\n");
        report.append("------------\n");
        report.append(originalMetrics.toString()).append("\n\n");

        report.append("ОБФУСЦИРОВАННЫЙ КОД:\n");
        report.append("--------------------\n");
        report.append(obfuscatedMetrics.toString()).append("\n\n");

        report.append("ИЗМЕНЕНИЯ:\n");
        report.append("----------\n");

        // Размер файла
        long sizeChange = obfuscatedMetrics.getFileSizeBytes() - originalMetrics.getFileSizeBytes();
        double sizeChangePercent = originalMetrics.getFileSizeBytes() > 0 ?
                (double) sizeChange / originalMetrics.getFileSizeBytes() * 100 : 0;
        report.append(String.format("Размер файла: %+d байт (%+.1f%%)\n",
                sizeChange, sizeChangePercent));

        // Методы
        int methodChange = obfuscatedMetrics.getNumberOfMethods() - originalMetrics.getNumberOfMethods();
        report.append(String.format("Количество методов: %+d\n", methodChange));

        // Сложность
        int complexityChange = obfuscatedMetrics.getCyclomaticComplexity() - originalMetrics.getCyclomaticComplexity();
        report.append(String.format("Цикломатическая сложность: %+d\n", complexityChange));

        // Инструкции байт-кода
        int instructionChange = obfuscatedMetrics.getBytecodeInstructions() - originalMetrics.getBytecodeInstructions();
        report.append(String.format("Инструкций байт-кода: %+d\n", instructionChange));

        // Пул констант
        int constantPoolChange = obfuscatedMetrics.getConstantPoolSize() - originalMetrics.getConstantPoolSize();
        report.append(String.format("Размер пула констант: %+d\n", constantPoolChange));

        // Оценка эффективности
        report.append("\nОЦЕНКА ЭФФЕКТИВНОСТИ ОБФУСКАЦИИ:\n");
        report.append("--------------------------------\n");

        double effectivenessScore = calculateEffectivenessScore();
        report.append(String.format("Общий показатель обфускации: %.1f/100\n", effectivenessScore));

        if (effectivenessScore > 80) {
            report.append("✓ Отличный уровень обфускации!\n");
        } else if (effectivenessScore > 60) {
            report.append("✓ Хороший уровень обфускации\n");
        } else if (effectivenessScore > 40) {
            report.append("⚠ Умеренный уровень обфускации\n");
        } else {
            report.append("✗ Низкий уровень обфускации\n");
        }

        report.append("=".repeat(60)).append("\n");

        return report.toString();
    }

    private double calculateEffectivenessScore() {
        double score = 0;

        // Увеличение размера файла (до 25 баллов)
        double sizeIncrease = originalMetrics.getFileSizeBytes() > 0 ?
                (double) (obfuscatedMetrics.getFileSizeBytes() - originalMetrics.getFileSizeBytes()) /
                        originalMetrics.getFileSizeBytes() * 100 : 0;
        score += Math.min(sizeIncrease / 4, 25);

        // Увеличение количества методов (до 20 баллов)
        int methodIncrease = obfuscatedMetrics.getNumberOfMethods() - originalMetrics.getNumberOfMethods();
        score += Math.min(methodIncrease * 2, 20);

        // Увеличение цикломатической сложности (до 20 баллов)
        int complexityIncrease = obfuscatedMetrics.getCyclomaticComplexity() - originalMetrics.getCyclomaticComplexity();
        score += Math.min(complexityIncrease * 3, 20);

        // Увеличение инструкций байт-кода (до 15 баллов)
        int instructionIncrease = obfuscatedMetrics.getBytecodeInstructions() - originalMetrics.getBytecodeInstructions();
        score += Math.min(instructionIncrease / 10, 15);

        // Увеличение пула констант (до 10 баллов)
        int constantPoolIncrease = obfuscatedMetrics.getConstantPoolSize() - originalMetrics.getConstantPoolSize();
        score += Math.min(constantPoolIncrease / 2, 10);

        // Снижение читаемости (до 10 баллов)
        double readabilityDecrease = originalMetrics.getReadabilityScore() - obfuscatedMetrics.getReadabilityScore();
        score += Math.min(readabilityDecrease / 2, 10);

        return Math.min(score, 100);
    }
}