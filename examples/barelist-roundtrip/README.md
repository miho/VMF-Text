# Bare List Round-Trip

Demonstrates lexical preservation for **bare** delimited lists — no brackets:

```antlr
bareList: items+=INT (',' items+=INT)* EOF;
```

Unlike ArrayLang's `'(' … ')'` shape (trivia footprint `2N+3`), a bare list
has footprint `2N+1` (values + commas + EOF pad). Structural `add` / `remove`
still splice comma/value slots so sibling whitespace survives.

## Why it matters

Many real grammars use bare comma-separated runs (`import a, b, c` style
lists, argument lists without always wrapping the whole run, etc.). 0.2.1
recognizes this footprint (and codegen `ListShapeHint` for multi-list rules)
so unparse-after-edit stays exact without forcing a parenthesized shape.

## Run

Requires JDK 21 and VMF-Text **0.2.1+** from `mavenLocal` (publish `core` +
`gradle-plugin` first):

```
cd examples/barelist-roundtrip
./gradlew run
```

Expected output (abridged):

```
[1] bare list round-tripped byte-identically
[2] items.set(1, 99) keeps sibling whitespace
[3] items.remove(1) splices trivia (no brackets)
[4] items.add(0, 0) → 0, …
[5] bare T (',' T)* list splice (0.2.1+)
```

## What to look for

| Edit | Expected |
|------|----------|
| Unedited round-trip | Byte-identical odd spaces/newlines |
| `items.set(i, …)` | Trivia size unchanged; neighbors kept |
| `items.remove(i)` | Two trivia slots spliced out |
| `items.add(0, …)` then `remove(0)` | Exact undo of insert-at-0 |

## Layout

- `src/main/vmf-text/demo/barelist/BareList.g4`
- `src/main/java/demo/barelist/Main.java`
- `sample/items.txt`

See also `BareListListEditLexicalPreservationTest` in the test suite.
