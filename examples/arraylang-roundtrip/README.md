# ArrayLang Round-Trip Showcase

The smallest lexical-preservation example: the `ArrayLang` grammar from the
VMF-Text README (`(1,2,3)` integer lists).

What it demonstrates:

- **Grammar → API.** One short labeled ANTLR4 file generates the typed model,
  parser and unparser.
- **Exact lexical preservation.** Odd spaces and newlines survive parse →
  unparse byte-for-byte.
- **In-place value edits.** `array.getValues().set(1, 99)` changes only that
  integer; surrounding whitespace stays (VMF-Text 0.2.1+).

## Run it

Requires JDK 21 and VMF-Text **0.2.1+** (the in-place list-edit fix). Until
0.2.1 is on Maven Central, publish this repo locally first:

```
# from the VMF-Text checkout
cd core && sh ./gradlew publishToMavenLocal --no-daemon
cd ../gradle-plugin && sh ./gradlew publishToMavenLocal --no-daemon

cd ../examples/arraylang-roundtrip
./gradlew run
```

Expected output (abridged):

```
[1] sample/numbers.txt (… chars) round-tripped byte-identically
  source:  (1 ,  2,\n 3 ,4\n,  5 )\n
[2] replaced values[1]: 2 -> 99
  after:   (1 ,  99,\n 3 ,4\n,  5 )\n
[3] one value edit; surrounding whitespace preserved
```

## Layout

- `src/main/vmf-text/demo/arraylang/ArrayLang.g4` — the README grammar
- `src/main/java/demo/arraylang/Main.java` — the showcase
- `sample/numbers.txt` — the input list; edit it and re-run

This is step 1 of the example ladder (ArrayLang → Java 8 → Java 24). See
[`../README.md`](../README.md).
