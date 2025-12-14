@echo off
echo Сборка Obfuscator...

REM Компиляция всех Java файлов
javac -d out -cp ".;lib/*" ^
    src/com/example/obfuscator/*.java ^
    src/com/example/obfuscator/SimpleObfuscator.java ^
    src/com/example/obfuscator/AsmObfuscator.java ^
    src/com/example/obfuscator/ObfuscatorApp.java ^
    src/com/example/obfuscator/ObfuscatorGUI.java

if %ERRORLEVEL% neq 0 (
    echo Ошибка компиляции
    pause
    exit /b 1
)

REM Создаем директорию для JAR
mkdir dist 2>nul

REM Создаем манифест
echo Main-Class: com.example.obfuscator.ObfuscatorApp > manifest.txt
echo Class-Path: . >> manifest.txt

REM Создаем JAR
jar cvfm dist/Obfuscator.jar manifest.txt -C out .

REM Копируем библиотеки
copy lib\*.jar dist\ 2>nul

echo.
echo ====================================
echo Сборка завершена!
echo Файл: dist/Obfuscator.jar
echo ====================================
echo.
echo Использование:
echo   GUI режим:       java -jar dist/Obfuscator.jar
echo   Консольный режим: java -jar dist/Obfuscator.jar input.java outputDir
echo.
pause