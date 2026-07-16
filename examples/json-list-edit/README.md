# JSON List Edit (Model-Typed)

JSON-style arrays store **bracket/comma trivia on the parent** and leading
whitespace on each child value. Structural add/remove splices parent slots
without clearing child trivia.

## Why it differs from ArrayLang

| | ArrayLang | MiniJson (this example) |
|--|-----------|-------------------------|
| List items | primitives on one node | model-typed children (e.g. `num`) |
| Parent trivia | every terminal slot | `'['` / `,` / `']'` only (`N+1` or `2` if empty) |
| Empty list | not representable in the simple grammar | `[]` alt |
| Child leading WS | on the parent between commas | on each child element |

Editing a number’s value only touches that child’s lexeme/trivia. Adding or
removing an element splices the parent’s comma/bracket slots so remaining
children keep their leading whitespace.

## Run

Requires JDK 21 and VMF-Text **0.2.1+** from `mavenLocal`:

```
cd examples/json-list-edit
./gradlew run
```

## What to look for

- Exact unedited round-trip of a formatted array
- `add` / `remove` keep sibling child leading trivia
- Empty `[]` is a valid alt (not cleared to a broken shape)
- In-place value edits leave brackets/commas alone

## Layout

- Grammar + `Main` under `src/main/`
- Sample input under `sample/`

See also `JsonArrayListEditLexicalPreservationTest` (and
`JSONLexicalStressTest`) in the test suite.

**Note:** Prefer a rule name like `num` over `number` so generated types do not
clash with `java.lang.Number`.
