# Original Lexeme Preservation

Type-mapped lexer rules (e.g. `DOUBLE → Double`) used to re-render via
`Double.toString()`, turning parsed `1` into `1.0`. VMF-Text 0.2.1 stores
**original lexemes** on `LexicalInfo` and reuses them when the semantic value
is unchanged.

## Why it matters

Users write `1`, `1.0`, `1e0` — all the same `double`, different spellings.
Library-grade unparse must not normalize spellings on unrelated edits. Only
the value you change should get a new rendering; siblings keep their text.

## How it works

1. **Parse-time:** each lexer-rule property / list index records
   `{text, valueKey}` on `LexicalInfo.originalLexemes`
   (`valueKey = String.valueOf(semanticValue)`).
2. **Unparse:** if `String.valueOf(currentValue)` still equals `valueKey`,
   emit the recorded `text`.
3. **In-place `list.set`:** invalidates only that index’s lexeme; other
   indices keep theirs. Structural add/remove follows the usual trivia splice
   rules and assigns fresh renderings for new slots.

## Run

Requires JDK 21 and VMF-Text **0.2.1+** from `mavenLocal`:

```
cd examples/original-lexemes
./gradlew run
```

## What to look for

- Parsed `1` stays `1` (not `1.0`) after an unrelated sibling edit
- Changing a value drops that index’s lexeme and uses normal rendering
- Combined with ArrayLang-style list splice: whitespace *and* spellings

## Layout

- Grammar + `Main` under `src/main/`
- Sample input under `sample/`

See also `ArrayLangOriginalLexemePreservationTest` (and related list-edit
tests) in the test suite.
