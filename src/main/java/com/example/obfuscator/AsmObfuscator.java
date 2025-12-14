package com.example.obfuscator;

import org.objectweb.asm.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.objectweb.asm.Opcodes.*;

public class AsmObfuscator {

    private final Random random = new Random();

    /**
     * Безопасная обфускация байт-кода с улучшенной обфускацией циклов
     */
    public void obfuscateClass(Path inputClass, Path outputClass) throws IOException {
        byte[] original = Files.readAllBytes(inputClass);

        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, 0);

        ClassVisitor cv = new ClassVisitor(ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name,
                                             String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                // Пропускаем конструкторы и специальные методы
                if (name.equals("<init>") || name.equals("<clinit>") || name.equals("main")) {
                    return mv;
                }

                return new MethodVisitor(ASM9, mv) {
                    private boolean inLoop = false;
                    private int loopDepth = 0;
                    private final Random localRandom = new Random();

                    @Override
                    public void visitCode() {
                        super.visitCode();

                        // Добавляем простой ложный код в начале каждого метода
                        mv.visitInsn(NOP);
                        mv.visitInsn(NOP);
                        mv.visitLdcInsn(123456);
                        mv.visitInsn(POP);
                        mv.visitLdcInsn(100);
                        mv.visitLdcInsn(200);
                        mv.visitInsn(IADD);
                        mv.visitInsn(POP);
                    }

                    @Override
                    public void visitJumpInsn(int opcode, Label label) {
                        // Обнаружение начала циклов
                        if (opcode == IFEQ || opcode == IFNE || opcode == IFLT ||
                                opcode == IFGE || opcode == IFGT || opcode == IFLE ||
                                opcode == IF_ICMPEQ || opcode == IF_ICMPNE) {

                            // Случайно добавляем обфускацию циклов (30% шанс)
                            if (localRandom.nextDouble() > 0.7) {
                                addLoopObfuscationBeforeJump(label);
                            }
                        }

                        super.visitJumpInsn(opcode, label);
                    }

                    @Override
                    public void visitIincInsn(int var, int increment) {
                        // Обфускация инкрементов (часто используются в циклах)
                        if (localRandom.nextDouble() > 0.8) {
                            // Добавляем фиктивный инкремент
                            mv.visitIincInsn(var + 1, 0); // фиктивный инкремент несуществующей переменной
                            mv.visitVarInsn(ILOAD, var + 1);
                            mv.visitInsn(POP);
                        }
                        super.visitIincInsn(var, increment);
                    }

                    @Override
                    public void visitVarInsn(int opcode, int var) {
                        // Случайно добавляем ложные операции с переменными в циклах
                        if (inLoop && localRandom.nextDouble() > 0.7) {
                            if (opcode == ILOAD) {
                                // Дублируем загрузку переменной
                                super.visitVarInsn(opcode, var);
                                mv.visitInsn(DUP);
                                mv.visitInsn(POP);
                            }
                        }
                        super.visitVarInsn(opcode, var);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        // Добавляем NOP перед некоторыми инструкциями
                        if (opcode == RETURN || opcode == IRETURN || opcode == ARETURN) {
                            // Добавляем обфусцированный код перед возвратом
                            addLoopObfuscationBeforeReturn();
                        }

                        // Случайно добавляем NOP инструкции
                        if (localRandom.nextDouble() > 0.8) {
                            mv.visitInsn(NOP);
                        }

                        super.visitInsn(opcode);
                    }

                    @Override
                    public void visitLabel(Label label) {
                        // Случайно добавляем фиктивные метки для усложнения CFG
                        if (localRandom.nextDouble() > 0.9) {
                            Label fakeLabel = new Label();
                            mv.visitJumpInsn(GOTO, fakeLabel);
                            mv.visitLabel(fakeLabel);
                            mv.visitInsn(NOP);
                        }
                        super.visitLabel(label);
                    }

                    @Override
                    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                        // Обфускация switch (часто используется для оптимизации циклов)
                        super.visitLookupSwitchInsn(dflt, keys, labels);

                        // Добавляем фиктивный switch после реального
                        if (localRandom.nextDouble() > 0.85) {
                            addFakeSwitch();
                        }
                    }

                    @Override
                    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
                        super.visitTableSwitchInsn(min, max, dflt, labels);

                        if (localRandom.nextDouble() > 0.85) {
                            addFakeSwitch();
                        }
                    }

                    /**
                     * Добавляет обфускацию перед условными переходами (циклами)
                     */
                    private void addLoopObfuscationBeforeJump(Label target) {
                        // Добавляем фиктивные вычисления, которые не влияют на результат
                        mv.visitLdcInsn(localRandom.nextInt(1000));
                        mv.visitLdcInsn(localRandom.nextInt(1000));
                        mv.visitInsn(IADD);
                        mv.visitInsn(POP);

                        // Фиктивное условие, которое всегда true
                        Label fakeLabel = new Label();
                        mv.visitInsn(ICONST_1);
                        mv.visitJumpInsn(IFNE, fakeLabel);
                        mv.visitInsn(NOP); // Этот код никогда не выполняется
                        mv.visitLabel(fakeLabel);
                    }

                    /**
                     * Добавляет обфускацию перед возвратом из метода
                     */
                    private void addLoopObfuscationBeforeReturn() {
                        // Создаем фиктивный мини-цикл перед возвратом
                        Label loopStart = new Label();
                        Label loopEnd = new Label();

                        mv.visitInsn(ICONST_0);
                        mv.visitVarInsn(ISTORE, 100); // Используем высокий индекс переменной

                        mv.visitLabel(loopStart);
                        mv.visitVarInsn(ILOAD, 100);
                        mv.visitLdcInsn(1);
                        mv.visitJumpInsn(IF_ICMPGE, loopEnd);

                        // Тело фиктивного цикла
                        mv.visitLdcInsn("fake_loop");
                        mv.visitInsn(POP);

                        mv.visitIincInsn(100, 1);
                        mv.visitJumpInsn(GOTO, loopStart);

                        mv.visitLabel(loopEnd);
                    }

                    /**
                     * Добавляет фиктивный switch statement
                     */
                    private void addFakeSwitch() {
                        Label defaultLabel = new Label();
                        Label[] labels = new Label[3];
                        for (int i = 0; i < labels.length; i++) {
                            labels[i] = new Label();
                        }

                        mv.visitLdcInsn(localRandom.nextInt(3));
                        mv.visitLookupSwitchInsn(defaultLabel,
                                new int[]{0, 1, 2}, labels);

                        // Кейс 0
                        mv.visitLabel(labels[0]);
                        mv.visitInsn(NOP);
                        mv.visitJumpInsn(GOTO, defaultLabel);

                        // Кейс 1
                        mv.visitLabel(labels[1]);
                        mv.visitInsn(NOP);
                        mv.visitJumpInsn(GOTO, defaultLabel);

                        // Кейс 2
                        mv.visitLabel(labels[2]);
                        mv.visitInsn(NOP);
                        mv.visitJumpInsn(GOTO, defaultLabel);

                        // Дефолтный кейс
                        mv.visitLabel(defaultLabel);
                        mv.visitInsn(NOP);
                    }
                };
            }

            @Override
            public FieldVisitor visitField(int access, String name,
                                           String descriptor,
                                           String signature, Object value) {
                // Добавляем фиктивные поля, которые могут использоваться в циклах
                if ((access & ACC_PRIVATE) != 0 && random.nextDouble() > 0.5) {
                    super.visitField(ACC_PRIVATE | ACC_STATIC,
                            "loopCounter" + random.nextInt(1000),
                            "I", null, null);
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public void visitEnd() {
                // Добавляем методы для работы с циклами
                addLoopHelperMethods();
                super.visitEnd();
            }

            /**
             * Добавляет вспомогательные методы для обфускации циклов
             */
            private void addLoopHelperMethods() {
                // Метод для создания фиктивного итератора
                addFakeIteratorMethod();

                // Метод для создания фиктивных границ цикла
                addLoopBoundsMethod();
            }

            private void addFakeIteratorMethod() {
                MethodVisitor mv = cv.visitMethod(ACC_PRIVATE | ACC_STATIC,
                        "getFakeIterator", "()Ljava/util/Iterator;",
                        "()Ljava/util/Iterator<Ljava/lang/Integer;>;", null);
                if (mv != null) {
                    mv.visitCode();

                    // Создаем список
                    mv.visitIntInsn(BIPUSH, 10);
                    mv.visitTypeInsn(ANEWARRAY, "java/lang/Integer");
                    mv.visitVarInsn(ASTORE, 0);

                    // Заполняем фиктивными значениями
                    for (int i = 0; i < 10; i++) {
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitIntInsn(BIPUSH, i);
                        mv.visitIntInsn(BIPUSH, i * 100);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
                                "(I)Ljava/lang/Integer;", false);
                        mv.visitInsn(AASTORE);
                    }

                    // Преобразуем в список и возвращаем итератор
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList",
                            "([Ljava/lang/Object;)Ljava/util/List;", false);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator",
                            "()Ljava/util/Iterator;", true);

                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(3, 1);
                    mv.visitEnd();
                }
            }

            private void addLoopBoundsMethod() {
                MethodVisitor mv = cv.visitMethod(ACC_PRIVATE | ACC_STATIC,
                        "calculateLoopBounds", "(II)[I", null, null);
                if (mv != null) {
                    mv.visitCode();

                    // Создаем массив из 3 элементов
                    mv.visitIntInsn(BIPUSH, 3);
                    mv.visitIntInsn(NEWARRAY, T_INT);
                    mv.visitVarInsn(ASTORE, 2);

                    // Заполняем массив фиктивными значениями
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitVarInsn(ILOAD, 0);
                    mv.visitInsn(IASTORE);

                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_1);
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitInsn(IASTORE);

                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_2);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(IASTORE);

                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(4, 3);
                    mv.visitEnd();
                }
            }
        };

        cr.accept(cv, 0);
        byte[] modified = cw.toByteArray();
        Files.write(outputClass, modified);
    }

    /**
     * Расширенная версия с усиленной обфускацией циклов
     */
    public void obfuscateWithEnhancedLoops(Path inputClass, Path outputClass) throws IOException {
        byte[] original = Files.readAllBytes(inputClass);

        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, 0);

        ClassVisitor cv = new ClassVisitor(ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name,
                                             String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                // Пропускаем конструкторы и специальные методы
                if (name.equals("<init>") || name.equals("<clinit>") || name.equals("main")) {
                    return mv;
                }

                return new MethodVisitor(ASM9, mv) {
                    private int fakeLoopCounter = 0;

                    @Override
                    public void visitCode() {
                        super.visitCode();

                        // Добавляем фиктивный цикл в начале метода
                        addDummyLoopAtStart();
                    }

                    @Override
                    public void visitJumpInsn(int opcode, Label label) {
                        // Преобразуем простые циклы в сложные
                        if (isLoopCondition(opcode)) {
                            transformLoopStructure(opcode, label);
                        } else {
                            super.visitJumpInsn(opcode, label);
                        }
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        // Перед возвратом добавляем фиктивный цикл
                        if (opcode == RETURN || opcode == IRETURN || opcode == ARETURN) {
                            addDummyLoopBeforeReturn();
                        }
                        super.visitInsn(opcode);
                    }

                    /**
                     * Проверяет, является ли инструкция условием цикла
                     */
                    private boolean isLoopCondition(int opcode) {
                        return opcode == IFEQ || opcode == IFNE || opcode == IFLT ||
                                opcode == IFGE || opcode == IFGT || opcode == IFLE ||
                                opcode == IF_ICMPEQ || opcode == IF_ICMPNE ||
                                opcode == IF_ICMPLT || opcode == IF_ICMPGE ||
                                opcode == IF_ICMPGT || opcode == IF_ICMPLE;
                    }

                    /**
                     * Преобразует структуру цикла (ИСПРАВЛЕННАЯ ВЕРСИЯ)
                     */
                    private void transformLoopStructure(int originalOpcode, Label originalLabel) {
                        // Вместо простого перехода создаем сложную структуру
                        Label fakeExit = new Label();
                        Label fakeContinue = new Label();

                        // Создаем сложное условие
                        int rand1 = random.nextInt(1000);
                        int rand2 = random.nextInt(1000);

                        mv.visitLdcInsn(rand1);
                        mv.visitLdcInsn(rand2);

                        if (random.nextBoolean()) {
                            // Используем сравнение через вычитание
                            mv.visitInsn(ISUB);
                            mv.visitInsn(DUP);
                            mv.visitInsn(POP); // Удаляем лишнюю копию
                            Label fakeElse = new Label();
                            mv.visitJumpInsn(IFEQ, fakeElse); // Если равны
                            mv.visitInsn(ICONST_1);
                            mv.visitJumpInsn(IFNE, fakeContinue);
                            mv.visitLabel(fakeElse);
                            mv.visitInsn(ICONST_1);
                            mv.visitJumpInsn(IFNE, fakeContinue);
                        } else {
                            // Используем битовую операцию
                            mv.visitInsn(IAND);
                            Label fakeElse = new Label();
                            mv.visitJumpInsn(IFEQ, fakeElse);
                            mv.visitInsn(ICONST_1);
                            mv.visitJumpInsn(IFNE, fakeContinue);
                            mv.visitLabel(fakeElse);
                            mv.visitInsn(ICONST_1);
                            mv.visitJumpInsn(IFNE, fakeContinue);
                        }

                        mv.visitJumpInsn(GOTO, fakeExit);

                        mv.visitLabel(fakeContinue);
                        // Оригинальный переход
                        super.visitJumpInsn(originalOpcode, originalLabel);

                        mv.visitLabel(fakeExit);
                    }

                    /**
                     * Добавляет фиктивный цикл в начале метода
                     */
                    private void addDummyLoopAtStart() {
                        if (random.nextDouble() > 0.7) {
                            fakeLoopCounter++;
                            Label loopStart = new Label();
                            Label loopEnd = new Label();

                            // Инициализация фиктивного счетчика
                            mv.visitInsn(ICONST_0);
                            mv.visitVarInsn(ISTORE, 100 + fakeLoopCounter);

                            mv.visitLabel(loopStart);
                            mv.visitVarInsn(ILOAD, 100 + fakeLoopCounter);
                            mv.visitLdcInsn(3); // 3 итерации
                            mv.visitJumpInsn(IF_ICMPGE, loopEnd);

                            // Тело фиктивного цикла
                            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
                            mv.visitLdcInsn("Dummy loop iteration: " + fakeLoopCounter);
                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                                    "(Ljava/lang/String;)V", false);

                            // Инкремент
                            mv.visitIincInsn(100 + fakeLoopCounter, 1);
                            mv.visitJumpInsn(GOTO, loopStart);

                            mv.visitLabel(loopEnd);
                        }
                    }

                    /**
                     * Добавляет фиктивный цикл перед возвратом
                     */
                    private void addDummyLoopBeforeReturn() {
                        if (random.nextDouble() > 0.5) {
                            Label cleanupLoop = new Label();
                            Label cleanupEnd = new Label();

                            // Фиктивный cleanup цикл
                            mv.visitInsn(ICONST_0);
                            mv.visitVarInsn(ISTORE, 200);

                            mv.visitLabel(cleanupLoop);
                            mv.visitVarInsn(ILOAD, 200);
                            mv.visitLdcInsn(2);
                            mv.visitJumpInsn(IF_ICMPGE, cleanupEnd);

                            // Фиктивные операции
                            mv.visitLdcInsn(System.currentTimeMillis());
                            mv.visitInsn(POP2);

                            mv.visitIincInsn(200, 1);
                            mv.visitJumpInsn(GOTO, cleanupLoop);

                            mv.visitLabel(cleanupEnd);
                        }
                    }
                };
            }
        };

        cr.accept(cv, 0);
        byte[] modified = cw.toByteArray();
        Files.write(outputClass, modified);
    }

    /**
     * Альтернативная версия с переименованием
     */
    public void obfuscateWithRenaming(Path inputClass, Path outputClass) throws IOException {
        byte[] original = Files.readAllBytes(inputClass);

        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, 0);

        ClassVisitor cv = new ClassVisitor(ASM9, cw) {
            private int methodCounter = 0;
            private int fieldCounter = 0;

            @Override
            public MethodVisitor visitMethod(int access, String name,
                                             String descriptor, String signature,
                                             String[] exceptions) {
                // Переименовываем непубличные методы
                String newName = name;
                if (!name.equals("<init>") && !name.equals("<clinit>") &&
                        !name.equals("main") && (access & ACC_PUBLIC) == 0) {
                    methodCounter++;
                    newName = "m" + methodCounter;
                }

                return super.visitMethod(access, newName, descriptor, signature, exceptions);
            }

            @Override
            public FieldVisitor visitField(int access, String name,
                                           String descriptor,
                                           String signature, Object value) {
                // Переименовываем непубличные поля
                String newName = name;
                if ((access & ACC_PUBLIC) == 0) {
                    fieldCounter++;
                    newName = "f" + fieldCounter;
                }

                return super.visitField(access, newName, descriptor, signature, value);
            }
        };

        cr.accept(cv, 0);
        byte[] modified = cw.toByteArray();
        Files.write(outputClass, modified);
    }
}