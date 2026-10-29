# Sondas de QA

Programas sueltos (paquete por defecto) que miden el rendimiento, el comportamiento con varios hilos y las entradas
hostiles del motor. No son tests: miden y comparan, y sirven para repetir las mediciones antes y después de un cambio.
Los límites de [`docs/referencia/comun/limites.md`](../../docs/referencia/comun/limites.md) se eligieron con `BombsProbe`.

Desde la raíz del repositorio (`resources/` y `test/` se leen relativos al directorio de trabajo):

```bash
mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
CP="target/classes:$(cat target/cp.txt)"
mkdir -p target/qa
javac -cp "$CP" -d target/qa tools/qa/*.java
java -Djava.awt.headless=true -cp "target/qa:$CP" ConcurrencyProbe     # secuencial frente a 8 hilos
java -Djava.awt.headless=true -cp "target/qa:$CP" ScaleProbe           # tiempos por tamaño de documento
java -Djava.awt.headless=true -cp "target/qa:$CP" GsubCostProbe        # coste de GSUB en PDFBox
java -Djava.awt.headless=true -cp "target/qa:$CP" GsubGlyphsProbe      # qué cadenas cambian con GSUB
java -Djava.awt.headless=true -cp "target/qa:$CP" HostileInputsProbe   # entradas patológicas
java -Djava.awt.headless=true -cp "target/qa:$CP" ColdStartProbe       # arranque en frío por partes y fuentes por documento
# La fuente que da AWT por su familia; en turco, Dialog (lo que dibujaba JSVG antes de SvgFonts)
java -Djava.awt.headless=true -Duser.language=tr -Duser.country=TR -cp target/qa AwtFontProbe
# Imágenes y SVG pequeños que dibujan muchísimo; cada caso en una JVM aparte con la memoria limitada
java -Xmx3g -cp "target/qa:$CP" BombsProbe gen /tmp/bombs
for f in argb-12000.png binary-30000.png rects-300k.svg use-9x10.svg; do java -Xmx512m -cp "target/qa:$CP" BombsProbe run /tmp/bombs $f; done
```

Un test de la suite con la misma idea que `ConcurrencyProbe` es `ConcurrentConversionTest`.
