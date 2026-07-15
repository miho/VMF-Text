# VMF-Text Examples

Runnable lexical-preservation showcases, ordered by complexity. Each one
resolves released VMF-Text from Maven Central — no local library build needed.

| Step | Example | Grammar | What it shows |
|------|---------|---------|---------------|
| 1 | [`arraylang-roundtrip`](arraylang-roundtrip) | Tiny `ArrayLang` from the README | Parse `(1,2,3)`, exact round-trip, replace one integer value |
| 2 | [`java8-roundtrip`](java8-roundtrip) | Full Java 8 | Small source file; rename a method and replace a string literal |
| 3 | [`java24-roundtrip`](java24-roundtrip) | Full Java 24 | Sealed types, records, pattern switches, text blocks; one method rename |

Run any example with:

```
cd <example-dir>
./gradlew run
```

Requires JDK 21.
