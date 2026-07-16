# List Separators (`separatorCount`)

`ListShapeHint` now carries **`separatorCount`** — how many terminals sit
between consecutive list items — so trivia splice is not limited to a single
`,`.

| Shape | Grammar | `separatorCount` |
|-------|---------|------------------|
| Multi-token | `ID (',' 'and' ID)*` | `2` |
| No separator | `ID (ID)*` | `0` |
| Classic | `ID (',' ID)*` | `1` |

Slot math (primitive lists):

```
size = prefix + n + (n-1)*separatorCount + suffix
```

## Run

Requires JDK 21 and VMF-Text **0.2.1+** from `mavenLocal`:

```
cd examples/list-separators
./gradlew run
```

## What to look for

- Exact unedited round-trip across both lists (joined by `;`)
- `multi` in-place `set` keeps odd spaces around `,` / `and`
- `nosep` `remove` keeps sibling leading whitespace
- Hints report `separatorCount` 2 and 0 respectively

## Layout

- `src/main/vmf-text/demo/listsep/ListSep.g4`
- `src/main/java/demo/listsep/Main.java`
- `sample/data.txt`

See also `MultiSepLexicalPreservationTest` and `NoSepLexicalPreservationTest`.
