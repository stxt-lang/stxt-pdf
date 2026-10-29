# Sondas de QA

Programas sueltos (paquete por defecto) que acompañan a [`docs/ROADMAP-QA-ARQ.md`](../../docs/ROADMAP-QA-ARQ.md).
No son tests: miden y comparan, y sus números están en el apartado "Línea base" de la hoja de ruta.
Sirven para repetir las mediciones antes y después de cada cambio.

Desde la raíz del repositorio (`resources/` y `test/` se leen relativos al directorio de trabajo):

```bash
mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
CP="target/classes:$(cat target/cp.txt)"
mkdir -p target/qa
javac -cp "$CP" -d target/qa tools/qa/*.java
java -Djava.awt.headless=true -cp "target/qa:$CP" ConcurrencyProbe     # TS-02: secuencial frente a 8 hilos
java -Djava.awt.headless=true -cp "target/qa:$CP" ScaleProbe           # PF-*: tiempos por tamaño de documento
java -Djava.awt.headless=true -cp "target/qa:$CP" GsubCostProbe        # QA-03/PF-01: coste de GSUB en PDFBox
java -Djava.awt.headless=true -cp "target/qa:$CP" GsubGlyphsProbe      # QA-03: qué cadenas cambian con GSUB
java -Djava.awt.headless=true -cp "target/qa:$CP" HostileInputsProbe   # QA-06: entradas patológicas
# QA-02: la fuente que da AWT por su familia; en turco, Dialog (lo que dibujaba JSVG antes de la sesión 4)
java -Djava.awt.headless=true -Duser.language=tr -Duser.country=TR -cp target/qa AwtFontProbe
# QA-06: imágenes y SVG pequeños que dibujan muchísimo; cada caso en una JVM aparte con la memoria limitada
java -Xmx3g -cp "target/qa:$CP" BombsProbe gen /tmp/bombs
for f in argb-12000.png binary-30000.png rects-300k.svg use-9x10.svg; do java -Xmx512m -cp "target/qa:$CP" BombsProbe run /tmp/bombs $f; done
```

Un test de la suite con la misma idea que `ConcurrencyProbe` es la tarea TS-02.
