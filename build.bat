@echo off
REM Requiere Java 17 y Gradle 8.x en el PATH
cd forge-1.20.1 && call gradle build && cd ..
cd fabric-1.20.1 && call gradle build && cd ..
echo Listo. Jars en forge-1.20.1\build\libs y fabric-1.20.1\build\libs
