# VMF-Text Examples

Runnable lexical-preservation showcases, ordered by complexity. Each one
resolves released VMF-Text from Maven Central — no local library build needed.

| Step | Example | Grammar | What it shows |
|------|---------|---------|---------------|
| 1 | [`arraylang-roundtrip`](arraylang-roundtrip) | Tiny `ArrayLang` from the README | Exact unedited round-trip; then a flat primitive list edit (whole-rule conservative separators) |
| 2 | [`java8-roundtrip`](java8-roundtrip) | Full Java 8 | Nested model edits: rename a method + replace a string; siblings keep their bytes |
| 3 | [`java24-roundtrip`](java24-roundtrip) | Full Java 24 | Same idea on a richer grammar: one method rename in place |

Why step 1 reformats after edit and steps 2–3 do not: ArrayLang stores
`values` as `List<Integer>` on one node, so one `set(i, …)` clears that node’s
trivia. Java edits change properties on nested `CodeElement`s, so only that
child’s trivia is dropped. See the root README section *What “exact” covers*
and `LEXICAL_PRESERVATION_ASSESSMENT.md` § *Edit invalidation*.

Run any example with:

```
cd <example-dir>
./gradlew run
```

Requires JDK 21.
