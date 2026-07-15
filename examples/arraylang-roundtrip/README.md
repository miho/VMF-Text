# ArrayLang Round-Trip Showcase

The smallest lexical-preservation example: the `ArrayLang` grammar from the
VMF-Text README (`(1,2,3)` integer lists).

What it demonstrates:

- **Grammar → API.** One short labeled ANTLR4 file generates the typed model,
  parser and unparser.
- **Exact lexical preservation for unedited parses.** Odd spaces and newlines
  survive parse → unparse byte-for-byte.
- **Primitive list edits fall back to conservative separators.**  
  `array.getValues().set(1, 99)` clears trivia on the whole `Array` node
  (flat `List<Integer>`, not nested model objects), so the list reformats to
  something like `( 1, 99, 3, 4, 5)`. Source bundles do not change this —
  they are for persistence/restore, not unparse-after-edit.

For edits that keep surrounding bytes, see the Java 8 / Java 24 examples
(nested `CodeElement`s). Details:
[`LEXICAL_PRESERVATION_ASSESSMENT.md`](../../LEXICAL_PRESERVATION_ASSESSMENT.md)
§ *Edit invalidation*.

## Run it

Requires JDK 21. Everything resolves from Maven Central — no local builds:

```
./gradlew run
```

Expected output (abridged):

```
[1] sample/numbers.txt (… chars) round-tripped byte-identically
  source:  (1 ,  2,\n 3 ,4\n,  5 )\n
[2] replaced values[1]: 2 -> 99
  after:  ( 1, 99, 3, 4, 5)
[3] flat primitive list edit -> conservative separators for that rule;
    unedited round-trips stay byte-identical (step [1])
```

## Layout

- `src/main/vmf-text/demo/arraylang/ArrayLang.g4` — the README grammar
- `src/main/java/demo/arraylang/Main.java` — the showcase
- `sample/numbers.txt` — the input list; edit it and re-run

This is step 1 of the example ladder (ArrayLang → Java 8 → Java 24). See
[`../README.md`](../README.md).
