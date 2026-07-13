# LSP Integration Guide

VMF-Text deliberately ships **no editor tooling** — no Eclipse plugin, no
VS Code extension, no language-server framework. That is a scope decision,
not an accident (see the non-goals in [ROADMAP.md](ROADMAP.md)): if you want
a framework that *generates* the language server for you, use
[Langium or Xtext](COMPARISON.md).

What VMF-Text gives you instead is a generated, typed API that maps cleanly
onto a language server you write yourself, e.g. with
[LSP4J](https://github.com/eclipse-lsp4j/lsp4j) (`org.eclipse.lsp4j` on
Maven Central). This guide documents that mapping. Code snippets use the
Java 24 grammar from
[`examples/java24-roundtrip`](examples/java24-roundtrip); everything works
the same for any generated grammar API.

## Capability map

| LSP capability | Generated building block |
|---|---|
| `publishDiagnostics` | ANTLR syntax errors via `parser.getErrorListeners().add(…)` |
| `documentSymbol` | `model.vmf().content().stream(…)` + `CodeElement.getCodeRange()` |
| `rename`, code actions, refactorings | typed model edit + `unparse` — untouched text stays byte-identical, so edits are minimal and safe |
| `formatting` | a custom formatter passed to the unparser (`unparser.setFormatter(…)`) |
| hover, definition, completion | yours to build — VMF-Text has no name resolution or scoping; the model plus `pathToRoot()` gives you the syntactic context |

Every generated model type extends `CodeElement`, so every node carries its
original source range:

```java
CodeRange r = element.getCodeRange();
r.getStart().getLine();          // 1-based (ANTLR convention)
r.getStart().getCharPosInLine(); // 0-based
```

LSP positions are 0-based in both line and character — subtract 1 from the
line:

```java
static Range toLspRange(CodeRange r) {
    return new Range(
        new Position(r.getStart().getLine() - 1, r.getStart().getCharPosInLine()),
        new Position(r.getStop().getLine() - 1, r.getStop().getCharPosInLine()));
}
```

## Diagnostics

Reparse on `didOpen`/`didChange` and publish the collected syntax errors.
Parsing is whole-file — there is no incremental reparse; debounce change
events if your grammar or files are large:

```java
void reparse(String uri, String text) {
    Java24ModelParser parser = new Java24ModelParser();
    List<Diagnostic> diagnostics = new ArrayList<>();
    parser.getErrorListeners().add(new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg,
                                RecognitionException e) {
            Position p = new Position(line - 1, charPositionInLine);
            diagnostics.add(new Diagnostic(new Range(p, p), msg,
                    DiagnosticSeverity.Error, "vmf-text"));
        }
    });
    models.put(uri, parser.parse(text));
    client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
}
```

## Document symbols

Stream the typed model, map ranges:

```java
List<DocumentSymbol> symbols = model.vmf().content()
        .stream(MethodDeclaration.class)
        .map(m -> new DocumentSymbol(
                m.getMethodName().getText(),
                SymbolKind.Method,
                toLspRange(m.getCodeRange()),           // full declaration
                toLspRange(m.getMethodName().getCodeRange()))) // selection
        .collect(Collectors.toList());
```

The same pattern serves classes, fields, records — any rule type the
grammar defines.

## Rename (and why round-trip fidelity matters here)

A rename provider is the
[round-trip showcase](examples/java24-roundtrip) wearing an LSP hat: edit
the model, unparse, and return the new text. Because everything you did not
touch is reproduced byte-identically, replacing the whole document produces
a minimal effective change — no reformatting noise in the user's diff:

```java
model.vmf().content().stream(MethodDeclaration.class)
     .filter(m -> contains(m.getMethodName().getCodeRange(), params.getPosition()))
     .forEach(m -> m.getMethodName().setText(params.getNewName()));

String newText = new Java24ModelUnparser().unparse(model);
// return newText as a full-document TextEdit
```

Two honest caveats:

- **Declaration only.** VMF-Text does not resolve references; renaming all
  *usages* requires your own resolution logic on top of the model.
- **Ranges reflect the original parse.** After model edits, reparse the
  unparsed text to refresh `getCodeRange()` values before serving further
  requests.

## Persistence for open editors

The generated source bundles (see "Source-Preserving Persistence" in the
[README](README.md)) store the semantic model together with the exact source
text and a checksum — useful when a server wants to persist state between
sessions without losing lexical fidelity.

## What this guide will not grow into

Content assist, cross-file linking, semantic validation and workspace
indexing are language-workbench territory. VMF-Text stays a
grammar-to-model library; the comparison page tells you
[when Xtext or Langium is the better choice](COMPARISON.md).
