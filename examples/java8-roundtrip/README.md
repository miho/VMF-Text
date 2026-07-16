# Java 8 Round-Trip Showcase

A small, real Java grammar example: parse a short Java 8 source file into a
typed VMF model, prove the unparse is byte-identical, then replace a method
name and a string literal on the model — every other byte stays untouched.

What it demonstrates:

- **A full language grammar.** The labeled Java 8 grammar (based on ANTLR
  grammars-v4) generates the complete typed model + parser + unparser API.
- **Exact lexical preservation.** Comments, blank lines and irregular
  formatting survive parse → unparse byte-for-byte.
- **Value replacement.** `setMethodName("sayHello")` and
  `setStringValue("\"hello, world\"")` change exactly those tokens.

## Run it

Requires JDK 21. Everything resolves from Maven Central — no local builds:

```
./gradlew run
```

Expected output (abridged):

```
[1] sample/Greeter.java (… chars) round-tripped byte-identically
  line 13  -   public static void greet( String[] args ){
  line 13  +   public static void sayHello( String[] args ){
  line 14  -     System.out.println( "hello" );   // trailing comment
  line 14  +     System.out.println( "hello, world" );   // trailing comment
[2] two model value edits -> two changed lines; every other byte is untouched
```

## Layout

- `src/main/vmf-text/demo/java8/Java8.g4` — the labeled Java 8 grammar
  (copied from the VMF-Text test suite; only the `@DelegateTo` package names
  were adjusted)
- `src/main/java/demo/java8/` — `@DelegateTo` support classes plus `Main`
- `sample/Greeter.java` — the input file; edit it and re-run

This is step 2 of the example ladder (ArrayLang → Java 8 → Java 24). See
[`../README.md`](../README.md).

Note: `ANTLR Tool version ... does not match the current runtime version`
warnings during generation are harmless (tool/runtime alignment lands with
VMF-Text 0.2.1).
