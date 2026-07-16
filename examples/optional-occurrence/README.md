# Optional Occurrence Index

Repeated optional elements that share a grammar path (e.g. several
`('(' name ')')?` siblings) now record `OptionalState.occurrenceIndex`.
The formatter consumes **exact** `(path, occurrence)` pairs so mixed
presence cannot be reassigned to earlier encounters.

## Why it exists

Path-keyed optional state alone is not enough when the *same* path appears
more than once in one unparse walk (sibling items, each with an optional
name group). A simple encounter counter can mis-associate presence after
edits: setting a name on the *second* absent item could interact with the
*first* present group.

`occurrenceIndex` is assigned at parse time (per path, per occurrence) and
is preferred by the formatter when `>= 0`. Older models without indices
still fall back to encounter order.

## Grammar

```antlr
root: items+=item (',' items+=item)* EOF;
item: 'x' ('(' name=ID ')')? ;
```

## Run

Requires JDK 21 and VMF-Text **0.2.1+** from `mavenLocal` (publish `core`
then `gradle-plugin` until Central has 0.2.1):

```
cd examples/optional-occurrence
./gradlew run
```

Expected output (abridged):

```
[1] optional states carry occurrenceIndex >= 0
[2] setName on 2nd item keeps first group: x (keep), x (new)
[3] OptionalState.occurrenceIndex (0.2.1+)
```

## What to look for

| Check | Meaning |
|-------|---------|
| Round-trip of `x, x (a), x (b), x` | Mixed presence is exact |
| `setName` on an absent sibling | Earlier `(keep)` group stays put |
| `setName(null)` on one sibling | Other groups untouched |
| Odd spaces / newlines | Leading inter-item trivia survives |

## Layout

- `src/main/vmf-text/demo/optocc/OptOccurrence.g4`
- `src/main/java/demo/optocc/Main.java`
- `sample/items.txt`

See also `OptionalOccurrenceIndexTest` in the test suite.
