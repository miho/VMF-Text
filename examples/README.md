# VMF-Text Examples

Runnable lexical-preservation showcases, ordered by complexity.

| Step | Example | Grammar | What it shows |
|------|---------|---------|---------------|
| 1 | [`arraylang-roundtrip`](arraylang-roundtrip) | Tiny `ArrayLang` from the README | Exact round-trip + `set`/`add`/`remove` with sibling whitespace (needs 0.2.1+) |
| 2 | [`java8-roundtrip`](java8-roundtrip) | Full Java 8 | Nested model edits: rename a method + replace a string; siblings keep their bytes |
| 3 | [`java24-roundtrip`](java24-roundtrip) | Full Java 24 | Same idea on a richer grammar: one method rename in place |

Java 8 / Java 24 resolve released VMF-Text **0.2.0** from Maven Central.
ArrayLang needs **0.2.1+** (list-edit trivia splice): publish this repo to
`mavenLocal` until Central has 0.2.1 — see that example’s README.

Run any example with:

```
cd <example-dir>
./gradlew run
```

Requires JDK 21.
