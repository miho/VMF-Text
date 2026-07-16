# VMF-Text Examples

Runnable lexical-preservation showcases. Prefer **0.2.1+** from `mavenLocal`
(publish `core` then `gradle-plugin`) unless noted.

| Step | Example | What it shows |
|------|---------|---------------|
| 1 | [`arraylang-roundtrip`](arraylang-roundtrip) | Parenthesized primitive list: exact round-trip + `set`/`add`/`remove` |
| 2 | [`barelist-roundtrip`](barelist-roundtrip) | Bare `T (',' T)*` list splice (no brackets) |
| 3 | [`original-lexemes`](original-lexemes) | Type-mapped spellings (`1` stays `1`, not `1.0`) |
| 4 | [`optional-null-value`](optional-null-value) | Optional group appears/disappears on null↔value |
| 5 | [`optional-occurrence`](optional-occurrence) | `OptionalState.occurrenceIndex` for mixed presence |
| 6 | [`json-list-edit`](json-list-edit) | Model-typed JSON array parent-trivia splice |
| 7 | [`multilist-hints`](multilist-hints) | Two lists on one rule via codegen `ListShapeHint` |
| 8 | [`list-separators`](list-separators) | `separatorCount`: `',' 'and'` and separator-less `ID (ID)*` |
| 9 | [`java8-roundtrip`](java8-roundtrip) | Nested Java 8 model edits (Central **0.2.0**) |
| 10 | [`java24-roundtrip`](java24-roundtrip) | Java 24 method rename (Central **0.2.0**) |

Run any example with:

```
cd <example-dir>
./gradlew run
```

Requires JDK 21.
