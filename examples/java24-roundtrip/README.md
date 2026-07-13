# Java 24 Round-Trip Showcase

Parses a real Java 24 source file into a typed VMF model, proves the unparse
is byte-identical, then applies one surgical model edit and shows that
exactly one line changes.

What it demonstrates:

- **A real-world grammar, unchanged.** The 1000-line labeled Java 24 grammar
  (records, sealed types, pattern switches, text blocks; based on ANTLR
  grammars-v4) — VMF-Text generates the complete typed model + parser +
  unparser API from this single file.
- **Exact lexical preservation.** All comments, blank lines and irregular
  formatting survive parse → unparse byte-for-byte.
- **Surgical edits.** `methodDecl.getMethodName().setText("render")` changes
  exactly one token in the output; every other byte stays untouched.

## Run it

Requires JDK 21. Everything resolves from Maven Central — no local builds:

```
./gradlew run
```

Expected output (abridged):

```
[1] sample/Shapes.java (955 chars) round-tripped byte-identically
  line 27  -     static String describe(Shape shape) {
  line 27  +     static String render(Shape shape) {
[2] one model edit -> one changed line; every other byte is untouched
```

## Layout

- `src/main/vmf-text/demo/java24/Java24.g4` — the labeled Java 24 grammar
  (copied from the VMF-Text test suite; only the `@DelegateTo` package names
  were adjusted)
- `src/main/java/demo/java24/` — small support classes referenced by the
  grammar (the `superClass` parser base and two `@DelegateTo` behaviors)
  plus the `Main` showcase
- `sample/Shapes.java` — the input file; edit it and re-run to test your own
  snippets

Note: `ANTLR Tool version ... does not match the current runtime version`
warnings during generation are harmless (tool/runtime alignment lands with
VMF-Text 0.2.1).
