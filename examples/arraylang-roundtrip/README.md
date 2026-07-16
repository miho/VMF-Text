# ArrayLang Round-Trip Showcase

The smallest lexical-preservation example: the `ArrayLang` grammar from the
VMF-Text README (`(1,2,3)` integer lists).

What it demonstrates:

- **Grammar → API.** One short labeled ANTLR4 file generates the typed model,
  parser and unparser.
- **Exact lexical preservation.** Odd spaces and newlines survive parse →
  unparse byte-for-byte.
- **In-place and structural edits (0.2.1+).** `values.set`, `add`, and
  `remove` keep sibling whitespace for the `'(' item (',' item)* ')'` shape.

## Run it

Requires JDK 21 and VMF-Text **0.2.1+** from Maven Central (or publish this
repo locally first):

```
cd core && sh ./gradlew publishToMavenLocal --no-daemon
cd ../gradle-plugin && sh ./gradlew publishToMavenLocal --no-daemon

cd ../examples/arraylang-roundtrip
./gradlew run
```

Expected output (abridged):

```
[1] sample/numbers.txt (…) round-tripped byte-identically
[2] values.set(1, 99)   → surrounding whitespace kept
[3] values.remove(1)    → surgical trivia splice
[4] values.add(42)      → comma + value inserted
[5] set/add/remove keep sibling whitespace (0.2.1+)
```

## Layout

- `src/main/vmf-text/demo/arraylang/ArrayLang.g4` — the README grammar
- `src/main/java/demo/arraylang/Main.java` — the showcase
- `sample/numbers.txt` — the input list

See also `ArrayLangListEditLexicalPreservationTest` in the test suite.
