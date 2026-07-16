# Multi-List Shape Hints

When **two delimited lists** share one parser rule, trivia-size heuristics
cannot tell which slots belong to which property. The generator analyzes the
unparser model and attaches `ListShapeHint` entries to `LexicalInfo`.

Structural edits use the hint (and sibling list sizes) to splice only the
affected segment — editing `ids` must not clear or reformat `nums`.

## Why heuristics fail

For a single list like ArrayLang (`'(' item (',' item)* ')'`),
`trivia.size() == 2N+3` is enough. With two lists on one rule:

```antlr
prog: ids+=ID (',' ids+=ID)* ';' nums+=INT (',' nums+=INT)* EOF;
```

the `;` is an inter-list trailer. A size-only guess after `ids.remove(0)`
easily clears the whole rule’s trivia. Codegen hints carry:

| Field | Role |
|-------|------|
| `propertyName` | Which list (`ids`, `nums`, …) |
| `orderIndex` | Left-to-right list order on the rule |
| `prefixCount` / `suffixCount` | Terminals before/after the value×comma run |
| `kind` | Shape family (bare / parenthesized / …) |
| `modelTyped` | Primitive vs model-typed children |

## Run

Requires JDK 21 and VMF-Text **0.2.1+** from Maven Central (or `mavenLocal`):

```
cd examples/multilist-hints
./gradlew run
```

Expected output (abridged):

```
[1] hints: ids + nums
[2] ids.remove(0) keeps nums trivia →   b; 1,\n 2
[3] nums.add(9) keeps ids → …
[4] codegen ListShapeHint (0.2.1+)
```

## What to look for

- Exact unedited round-trip of odd spaces/newlines across both lists
- `ids.set` / `remove` / `add(0, …)` leave `nums` whitespace alone
- `nums` edits leave `ids` alone (including the `;` separator region)
- Insert-at-0 after `;` pads the new head (`; 0, …`) so values do not glue;
  remove restores the original source

## Layout

- `src/main/vmf-text/demo/multilist/MultiList.g4`
- `src/main/java/demo/multilist/Main.java`
- `sample/data.txt`

See also `MultiListShapeHintTest` in the test suite.
