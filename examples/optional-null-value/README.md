# Optional Null↔Value Edits

When an optional group wraps a property (`('(' name=ID ')')?`), toggling the
property between `null` and a value must update **optional presence** and keep
leading inter-rule whitespace.

## Why it matters

Without presence updates, setting a formerly-null property either omitted the
group forever or cleared trivia and reformatted neighbors. Value→null used to
be especially noisy (full trivia clear on that element).

## Behavior (0.2.1+)

| Edit | Effect |
|------|--------|
| `null → value` (group was absent) | Mark optional terminals present; ensure trivia for `( value )` |
| `null → value` (empty `()` already present) | Insert a trivia slot for the value; keep parens |
| `value → null` | Do **not** clear leading trivia; unparser skips the property-required group |
| In-place rename (`"a"` → `"b"`) | Keep group + sibling whitespace |

Presence is recorded as path-keyed `OptionalState` entries. When the same
optional path repeats across siblings, see
[`optional-occurrence`](../optional-occurrence) for `occurrenceIndex`.

## Run

Requires JDK 21 and VMF-Text **0.2.1+** from Maven Central (or `mavenLocal`):

```
cd examples/optional-null-value
./gradlew run
```

## What to look for

- Leading whitespace before the rule survives null↔value flips
- Absent → present inserts a well-formed optional group
- Present → absent removes the group without reformatting neighbors

## Layout

- Grammar + `Main` under `src/main/`
- Sample input under `sample/`

See also `OptionalNullValueLexicalPreservationTest` and
`OptionalOccurrenceIndexTest` in the test suite.
