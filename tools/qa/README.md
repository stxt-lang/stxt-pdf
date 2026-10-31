# QA probes

Standalone programs (default package) that measure the performance, the behavior with several threads and the hostile
inputs of the engine. They are not tests: they measure and compare, and they serve to repeat the measurements before and
after a change. The limits of [`docs/en/reference/common/limits.md`](../../docs/en/reference/common/limits.md) were chosen with `BombsProbe`.

From the root of the repository (`resources/` and `test/` are read relative to the working directory):

```bash
mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
CP="target/classes:$(cat target/cp.txt)"
mkdir -p target/qa
javac -cp "$CP" -d target/qa tools/qa/*.java
java -Djava.awt.headless=true -cp "target/qa:$CP" ConcurrencyProbe     # sequential against 8 threads
java -Djava.awt.headless=true -cp "target/qa:$CP" ScaleProbe           # times by document size
java -Djava.awt.headless=true -cp "target/qa:$CP" GsubCostProbe        # cost of GSUB in PDFBox
java -Djava.awt.headless=true -cp "target/qa:$CP" GsubGlyphsProbe      # which strings change with GSUB
java -Djava.awt.headless=true -cp "target/qa:$CP" HostileInputsProbe   # pathological inputs
java -Djava.awt.headless=true -cp "target/qa:$CP" ColdStartProbe       # cold start by parts and fonts per document
# The font AWT gives for its family; in Turkish, Dialog (what JSVG drew before SvgFonts)
java -Djava.awt.headless=true -Duser.language=tr -Duser.country=TR -cp target/qa AwtFontProbe
# Small images and SVGs that draw a huge amount; each case in a separate JVM with limited memory
java -Xmx3g -cp "target/qa:$CP" BombsProbe gen /tmp/bombs
for f in argb-12000.png binary-30000.png rects-300k.svg use-9x10.svg; do java -Xmx512m -cp "target/qa:$CP" BombsProbe run /tmp/bombs $f; done
```

A test of the suite with the same idea as `ConcurrencyProbe` is `ConcurrentConversionTest`.
