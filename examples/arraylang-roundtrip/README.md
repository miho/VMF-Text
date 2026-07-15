# ArrayLang Round-Trip Showcase

The smallest lexical-preservation example: the `ArrayLang` grammar from the
VMF-Text README (`(1,2,3)` integer lists). Parses an irregularly spaced list,
proves the unparse is byte-identical, then replaces one value on the model
while keeping surrounding whitespace.

What it demonstrates:

- **Grammar → API.** One short labeled ANTLR4 file generates the typed model,
  parser and unparser.
- **Exact lexical preservation.** Odd spaces and newlines survive parse →
  unparse byte-for-byte.
- **Value edits.** `array.getValues().set(1, 99)` changes one integer; the
  rest of the source shape stays put.

## Run it

Requires JDK 21. Everything resolves from Maven Central — no local builds:

```
./gradlew run
```

Expected output (abridged):

```
[1] sample/numbers.txt (… chars) round-tripped byte-identically
[2] replaced values[1]: 2 -> 99
  before:  (1 ,  2,\n 3 ,4\n,  5 )
  after:   (1 ,  99,\n 3 ,4\n,  5 )
[3] one value edit; surrounding whitespace preserved
```

## Layout

- `src/main/vmf-text/demo/arraylang/ArrayLang.g4` — the README grammar
- `src/main/java/demo/arraylang/Main.java` — the showcase
- `sample/numbers.txt` — the input list; edit it and re-run

This is step 1 of the example ladder (ArrayLang → Java 8 → Java 24). See
[`../README.md`](../README.md).
