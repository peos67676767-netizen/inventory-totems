#!/bin/bash
# Requiere Java 17 y Gradle 8.x en el PATH
set -e
(cd forge-1.20.1 && gradle build)
(cd fabric-1.20.1 && gradle build)
echo "Listo. Jars en forge-1.20.1/build/libs y fabric-1.20.1/build/libs"
