package com.example.obfuscator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.Modifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class SimpleObfuscator {

    private static final List<String> DUMMY_COMMENTS = Arrays.asList(
            "TODO: refactor later",
            "optimization needed",
            "legacy code, do not touch",
            "fixme: check edge cases",
            "HACK: temporary workaround",
            "review required",
            "performance critical",
            "deprecated approach",
            "WARNING: undocumented behavior",
            "FIX: potential null pointer",
            "CHECK: boundary conditions",
            "IMPORTANT: do not modify",
            "BUG: possible memory leak",
            "OPTIMIZE: for better performance",
            "SECURITY: verify input validation",
            "COMPATIBILITY: backward compatibility issue",
            "TEST: add more test cases",
            "REFACTOR: extract method",
            "CLEANUP: remove unused code",
            "DOCUMENT: add javadoc"
    );

    private final Random random = new Random();

    public static class Result {
        public final String source;
        public final String className;

        public Result(String source, String className) {
            this.source = source;
            this.className = className;
        }
    }

    public Result obfuscate(String inputPath) throws FileNotFoundException {
        NameGenerator.reset();

        CompilationUnit cu = StaticJavaParser.parse(new File(inputPath));

        // 1. Находим публичный класс и переименовываем его
        ClassOrInterfaceDeclaration topClass = cu
                .findFirst(ClassOrInterfaceDeclaration.class, ClassOrInterfaceDeclaration::isPublic)
                .orElseThrow(() -> new IllegalStateException("Public class not found"));

        String originalName = topClass.getNameAsString();
        String obfName = originalName + "_obf";

        topClass.setName(obfName);
        topClass.getConstructors().forEach(ctor -> ctor.setName(obfName));

        // 2. Обфускация методов
        cu.findAll(MethodDeclaration.class).forEach(md -> {
            Map<String, String> localMap = new HashMap<>();

            // 2.1. Параметры
            for (Parameter p : md.getParameters()) {
                String oldName = p.getNameAsString();
                String newName = NameGenerator.nextShortName();
                localMap.put(oldName, newName);
                p.setName(newName);
            }

            // 2.2. Локальные переменные
            md.findAll(VariableDeclarator.class).forEach(vd -> {
                String oldName = vd.getNameAsString();
                if (!localMap.containsKey(oldName)) {
                    String newName = NameGenerator.nextShortName();
                    localMap.put(oldName, newName);
                    vd.setName(newName);
                }
            });

            // 2.3. Использования имён
            md.findAll(NameExpr.class).forEach(ne -> {
                String oldName = ne.getNameAsString();
                String mapped = localMap.get(oldName);
                if (mapped != null) {
                    ne.setName(mapped);
                }
            });

            // 3. Обфускация циклов (40% методов)
            if (random.nextDouble() > 0.6) {
                obfuscateLoops(md);
            }

            // 4. Добавляем ложный код ВНУТРИ метода
            addFakeCodeInsideMethod(md);

            // 5. Добавляем фиктивные переменные с комментариями
            addDummyVariablesWithComments(md);

            // 6. Добавляем комментарии
            addComments(md);
        });

        // 7. Добавляем фиктивные методы в класс
        addFakeMethods(topClass);

        // 8. Добавляем фиктивные импорты
        addFakeImports(cu);

        // 9. Получаем исходный код
        String sourceCode = cu.toString();

        return new Result(sourceCode, obfName);
    }

    /**
     * Добавляем ложный код внутри методов
     */
    private void addFakeCodeInsideMethod(MethodDeclaration md) {
        if (!md.getBody().isPresent()) {
            return;
        }

        BlockStmt body = md.getBody().get();
        List<Statement> originalStatements = new ArrayList<>(body.getStatements());

        // Получаем список уже существующих имен переменных в методе
        Set<String> existingVars = new HashSet<>();
        md.findAll(VariableDeclarator.class).forEach(vd -> {
            existingVars.add(vd.getNameAsString());
        });
        md.findAll(Parameter.class).forEach(p -> {
            existingVars.add(p.getNameAsString());
        });

        for (int i = 0; i < originalStatements.size(); i++) {
            Statement stmt = originalStatements.get(i);

            // Добавляем ложный код перед каждым 3-м statement
            if (random.nextDouble() > 0.6) {
                addFakeStatement(body, i, existingVars);
                i++; // Увеличиваем индекс, так как добавили statement
            }

            // Добавляем ложный код внутри if, for, while
            if (stmt instanceof IfStmt) {
                addFakeCodeToIf((IfStmt) stmt, existingVars);
            } else if (stmt instanceof ForStmt) {
                addFakeCodeToFor((ForStmt) stmt, existingVars);
            } else if (stmt instanceof WhileStmt) {
                addFakeCodeToWhile((WhileStmt) stmt, existingVars);
            }
        }

        // Добавляем ложный код в конце метода (перед return, если есть)
        if (random.nextDouble() > 0.4) {
            addFakeCodeAtEnd(body, existingVars);
        }
    }

    private void addFakeStatement(BlockStmt body, int position, Set<String> existingVars) {
        int type = random.nextInt(5);
        String fakeVar = generateUniqueVarName(existingVars);
        existingVars.add(fakeVar);

        try {
            Statement fakeStmt = null;

            switch (type) {
                case 0:
                    fakeStmt = StaticJavaParser.parseStatement(
                            "int " + fakeVar + " = " + random.nextInt(100) + ";");
                    break;
                case 1:
                    String timeVar = generateUniqueVarName(existingVars);
                    existingVars.add(timeVar);
                    fakeStmt = StaticJavaParser.parseStatement(
                            "long " + timeVar + " = System.currentTimeMillis();");
                    break;
                case 2:
                    fakeStmt = StaticJavaParser.parseStatement(
                            "if (false) { System.out.println(\"Never happens\"); }");
                    break;
                case 3:
                    String exVar = generateUniqueVarName(existingVars);
                    existingVars.add(exVar);
                    fakeStmt = StaticJavaParser.parseStatement(
                            "try { int x = 1 / 1; } catch (Exception " + exVar + ") { /* ignore */ }");
                    break;
                case 4:
                    String loopVar = generateUniqueVarName(existingVars);
                    existingVars.add(loopVar);
                    fakeStmt = StaticJavaParser.parseStatement(
                            "for (int " + loopVar + " = 0; " + loopVar + " < " + random.nextInt(5) + "; " + loopVar + "++) { /* empty */ }");
                    break;
            }

            if (fakeStmt != null && position < body.getStatements().size()) {
                body.getStatements().add(position, fakeStmt);
            }
        } catch (Exception e) {
            // Простая версия
            ExpressionStmt simpleStmt = new ExpressionStmt(
                    new VariableDeclarationExpr(
                            new VariableDeclarator(
                                    PrimitiveType.intType(),
                                    fakeVar,
                                    new IntegerLiteralExpr(random.nextInt(100))
                            )
                    )
            );
            if (position < body.getStatements().size()) {
                body.getStatements().add(position, simpleStmt);
            }
        }
    }

    private void addFakeCodeToIf(IfStmt ifStmt, Set<String> existingVars) {
        if (ifStmt.getThenStmt() instanceof BlockStmt) {
            BlockStmt thenBlock = (BlockStmt) ifStmt.getThenStmt();
            if (random.nextDouble() > 0.7) {
                addFakeStatement(thenBlock, 0, existingVars);
            }
        }

        if (ifStmt.getElseStmt().isPresent() && ifStmt.getElseStmt().get() instanceof BlockStmt) {
            BlockStmt elseBlock = (BlockStmt) ifStmt.getElseStmt().get();
            if (random.nextDouble() > 0.7) {
                addFakeStatement(elseBlock, 0, existingVars);
            }
        }
    }

    private void addFakeCodeToFor(ForStmt forStmt, Set<String> existingVars) {
        if (forStmt.getBody() instanceof BlockStmt) {
            BlockStmt body = (BlockStmt) forStmt.getBody();
            if (random.nextDouble() > 0.7) {
                addFakeStatement(body, 0, existingVars);
            }
        }
    }

    private void addFakeCodeToWhile(WhileStmt whileStmt, Set<String> existingVars) {
        if (whileStmt.getBody() instanceof BlockStmt) {
            BlockStmt body = (BlockStmt) whileStmt.getBody();
            if (random.nextDouble() > 0.7) {
                addFakeStatement(body, 0, existingVars);
            }
        }
    }

    private void addFakeCodeAtEnd(BlockStmt body, Set<String> existingVars) {
        int lastReturnIndex = -1;
        for (int i = body.getStatements().size() - 1; i >= 0; i--) {
            if (body.getStatement(i).toString().contains("return")) {
                lastReturnIndex = i;
                break;
            }
        }

        if (lastReturnIndex != -1) {
            String fakeVar = generateUniqueVarName(existingVars);
            existingVars.add(fakeVar);
            try {
                Statement fakeStmt = StaticJavaParser.parseStatement(
                        "int " + fakeVar + " = " + body.getStatements().size() + "; // fake counter");
                body.getStatements().add(lastReturnIndex, fakeStmt);
            } catch (Exception e) {
                ExpressionStmt simpleStmt = new ExpressionStmt(
                        new VariableDeclarationExpr(
                                new VariableDeclarator(
                                        PrimitiveType.intType(),
                                        fakeVar,
                                        new IntegerLiteralExpr(random.nextInt(100))
                                )
                        )
                );
                body.getStatements().add(lastReturnIndex, simpleStmt);
            }
        } else {
            addFakeStatement(body, body.getStatements().size(), existingVars);
        }
    }

    private void addDummyVariablesWithComments(MethodDeclaration md) {
        if (!md.getBody().isPresent()) {
            return;
        }

        BlockStmt body = md.getBody().get();

        Set<String> existingVars = new HashSet<>();
        md.findAll(VariableDeclarator.class).forEach(vd -> {
            existingVars.add(vd.getNameAsString());
        });
        md.findAll(Parameter.class).forEach(p -> {
            existingVars.add(p.getNameAsString());
        });

        if (random.nextDouble() > 0.3) {
            String varName = generateUniqueVarName(existingVars);
            existingVars.add(varName);
            String comment = DUMMY_COMMENTS.get(random.nextInt(DUMMY_COMMENTS.size()));

            try {
                String code = "int " + varName + " = " + random.nextInt(1000) + "; // " + comment;
                Statement stmt = StaticJavaParser.parseStatement(code);
                body.addStatement(0, stmt);
            } catch (Exception e) {
                ExpressionStmt simpleStmt = new ExpressionStmt(
                        new VariableDeclarationExpr(
                                new VariableDeclarator(
                                        PrimitiveType.intType(),
                                        varName,
                                        new IntegerLiteralExpr(random.nextInt(100))
                                )
                        )
                );
                body.addStatement(0, simpleStmt);
            }
        }
    }

    private void addComments(MethodDeclaration md) {
        if (md.getBody().isPresent()) {
            BlockStmt body = md.getBody().get();

            for (Statement stmt : body.getStatements()) {
                if (random.nextDouble() > 0.8) {
                    String comment = DUMMY_COMMENTS.get(random.nextInt(DUMMY_COMMENTS.size()));
                    String stmtStr = stmt.toString();
                    if (!stmtStr.contains("//")) {
                        try {
                            Statement newStmt = StaticJavaParser.parseStatement(
                                    "// " + comment + "\n" + stmtStr);
                            int index = body.getStatements().indexOf(stmt);
                            body.getStatements().set(index, newStmt);
                        } catch (Exception e) {
                            // Игнорируем ошибки
                        }
                    }
                }
            }
        }
    }

    private void addFakeMethods(ClassOrInterfaceDeclaration clazz) {
        int count = 1 + random.nextInt(2);

        for (int i = 0; i < count; i++) {
            try {
                String methodName = "fakeMethod" + random.nextInt(1000);
                String methodCode =
                        "private int " + methodName + "() {\n" +
                                "    int result = " + random.nextInt(100) + ";\n" +
                                "    for (int i = 0; i < " + (3 + random.nextInt(5)) + "; i++) {\n" +
                                "        result += i * i;\n" +
                                "    }\n" +
                                "    return result;\n" +
                                "}";

                CompilationUnit tempCu = StaticJavaParser.parse("class Temp { " + methodCode + " }");
                Optional<MethodDeclaration> fakeMethodOpt = tempCu.findFirst(MethodDeclaration.class);

                if (fakeMethodOpt.isPresent()) {
                    MethodDeclaration fakeMethod = fakeMethodOpt.get().clone();
                    clazz.addMember(fakeMethod);
                }
            } catch (Exception e) {
                // Игнорируем ошибки
            }
        }
    }

    private void addFakeImports(CompilationUnit cu) {
        List<String> safeFakeImports = Arrays.asList(
                "java.util.concurrent.atomic.*",
                "java.security.*",
                "javax.crypto.*",
                "java.net.*",
                "java.nio.*",
                "java.time.*",
                "java.util.stream.*",
                "java.util.function.*"
        );

        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            String fakeImport = safeFakeImports.get(random.nextInt(safeFakeImports.size()));
            cu.addImport(fakeImport, false, false);
        }
    }

    private String generateUniqueVarName(Set<String> existingVars) {
        String[] prefixes = {"var", "tmp", "v", "x", "y", "z", "a", "b", "c", "d"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        int counter = 0;
        String varName;

        do {
            varName = prefix + counter++;
            if (counter > 10000) {
                varName = prefix + "_" + System.currentTimeMillis() % 10000;
                break;
            }
        } while (existingVars.contains(varName));

        return varName;
    }

    /**
     * Обфускация циклов - безопасная реализация
     */
    private void obfuscateLoops(MethodDeclaration md) {
        if (!md.getBody().isPresent()) {
            return;
        }

        // 1. Обфускация for-циклов
        List<ForStmt> forLoops = md.findAll(ForStmt.class);

        for (ForStmt forLoop : forLoops) {
            if (random.nextDouble() > 0.7) { // 30% шанс
                try {
                    wrapLoopWithDummy(forLoop);
                } catch (Exception e) {
                    // Игнорируем ошибки
                }
            }
        }

        // 2. Обфускация while-циклов
        List<WhileStmt> whileLoops = md.findAll(WhileStmt.class);

        for (WhileStmt whileLoop : whileLoops) {
            if (random.nextDouble() > 0.7) { // 30% шанс
                try {
                    addPreLoopCode(whileLoop);
                } catch (Exception e) {
                    // Игнорируем ошибки
                }
            }
        }

        // 3. Обфускация do-while циклов
        List<DoStmt> doLoops = md.findAll(DoStmt.class);

        for (DoStmt doLoop : doLoops) {
            if (random.nextDouble() > 0.7) { // 30% шанс
                try {
                    addPostLoopCode(doLoop);
                } catch (Exception e) {
                    // Игнорируем ошибки
                }
            }
        }
    }

    /**
     * Оборачивает цикл фиктивным внешним циклом
     */
    private void wrapLoopWithDummy(ForStmt forLoop) throws Exception {
        String wrapperVar = "wrap" + random.nextInt(1000);
        String dummyVar = "dummy" + random.nextInt(1000);

        String originalLoop = forLoop.toString().trim();

        // Убираем комментарии из начала строки, если есть
        if (originalLoop.startsWith("//")) {
            int newlineIndex = originalLoop.indexOf('\n');
            if (newlineIndex > 0) {
                originalLoop = originalLoop.substring(newlineIndex + 1).trim();
            }
        }

        String obfuscated =
                "// Обфусцированный цикл\n" +
                        "for (int " + wrapperVar + " = 0; " + wrapperVar + " < 1; " + wrapperVar + "++) {\n" +
                        "    int " + dummyVar + " = " + random.nextInt(100) + ";\n" +
                        "    " + originalLoop + "\n" +
                        "}";

        Statement newLoop = StaticJavaParser.parseStatement(obfuscated);
        forLoop.replace(newLoop);
    }

    /**
     * Добавляет фиктивный код перед while-циклом
     */
    private void addPreLoopCode(WhileStmt whileLoop) throws Exception {
        String dummyVar = "pre" + random.nextInt(1000);

        String condition = whileLoop.getCondition().toString();
        String body = whileLoop.getBody().toString().trim();

        // Убираем фигурные скобки, если тело - блок
        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1).trim();
        }

        String obfuscated =
                "// Фиктивный код перед циклом\n" +
                        "int " + dummyVar + " = " + random.nextInt(100) + ";\n" +
                        "while (" + condition + ") {\n" +
                        "    " + body + "\n" +
                        "}";

        Statement newLoop = StaticJavaParser.parseStatement(obfuscated);
        whileLoop.replace(newLoop);
    }

    /**
     * Добавляет фиктивный код после do-while цикла
     */
    private void addPostLoopCode(DoStmt doLoop) throws Exception {
        String dummyVar = "post" + random.nextInt(1000);

        String condition = doLoop.getCondition().toString();
        String body = doLoop.getBody().toString().trim();

        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1).trim();
        }

        String obfuscated =
                "do {\n" +
                        "    " + body + "\n" +
                        "} while (" + condition + ");\n" +
                        "int " + dummyVar + " = " + random.nextInt(100) + "; // Фиктивная переменная";

        Statement newLoop = StaticJavaParser.parseStatement(obfuscated);
        doLoop.replace(newLoop);
    }

    /**
     * Простая обфускация - преобразование for в while
     */
    private void convertForToWhile(ForStmt forLoop) throws Exception {
        String init = forLoop.getInitialization().toString().replace(";", "");
        String condition = forLoop.getCompare().isPresent() ?
                forLoop.getCompare().get().toString() : "true";
        String update = forLoop.getUpdate().toString().replace(";", "");
        String body = forLoop.getBody().toString().trim();

        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1).trim();
        }

        String whileVersion =
                "// For преобразован в while\n" +
                        init + ";\n" +
                        "while (" + condition + ") {\n" +
                        "    " + body + "\n" +
                        "    " + update + ";\n" +
                        "}";

        Statement newLoop = StaticJavaParser.parseStatement(whileVersion);
        forLoop.replace(newLoop);
    }
}