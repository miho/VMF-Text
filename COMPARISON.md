# How VMF-Text Compares

*Maintained by the VMF-Text author; facts last verified 2026-07-13.
Corrections are welcome — please open an issue or PR.*

VMF-Text solves one specific problem well: **turn an existing ANTLR4 grammar
into a typed model API with exact round-trip unparsing, on the JVM.** Other
tools optimize for different things — this page says which tool fits which
job, including when the answer is not VMF-Text.

## At a glance

| | Grammar input | Model / API | Round-trip of parsed sources | Editor / LSP tooling | Platform | Scope & maturity |
|---|---|---|---|---|---|---|
| **VMF-Text** | plain ANTLR4; labels optional ([auto-labeling](README.md#automatic-labels)) | generated VMF model: immutable views, change recording, undo, clone, reflection | **exact, byte-identical** for untouched content; conservative separators for programmatically set values | none built-in | JVM (artifacts run on Java 11+) | focused library + Gradle plugin; small project, single maintainer |
| **Xtext** | own grammar language | generated EMF model | supported via serializer + node model; formatting of unchanged regions can be reused | excellent: Eclipse IDE + LSP | JVM / Eclipse | full language workbench; large, mature ecosystem |
| **Langium** | own grammar language (Xtext-inspired) | generated TypeScript AST types | no generated lexical-preserving unparser; output via templates / formatter API | excellent: LSP-first, VS Code | Node / TypeScript | language-server framework; active and growing |
| **textX** | own grammar language (PEG, Arpeggio) | Python metamodel + objects | no generated unparser; model-to-text via templates | basic (textX-LS) | Python | lightweight DSL library; established in its niche |
| **ANTLR4 alone** | ANTLR4 — the ecosystem standard ([grammars-v4](https://github.com/antlr/grammars-v4)) | parse trees + listeners/visitors | token-level edits via `TokenStreamRewriter` preserve surrounding text; no model-level unparse | none | many targets (Java, C#, Python, JS, …) | parser generator; huge ecosystem |
| **JavaParser** | none needed (Java-only) | rich Java AST + symbol resolution | `LexicalPreservingPrinter` preserves formatting of unchanged nodes | none (library) | JVM; Java sources only | single-language library; large community |

## What VMF-Text does that the others do not

- **Consumes plain ANTLR4 grammars.** Xtext, Langium and textX each define
  their own grammar language; porting an existing grammar means rewriting
  it. VMF-Text takes the grammar as-is — labels refine the API, and
  auto-labeling covers unlabeled grammars. The
  [Java 24 example](examples/java24-roundtrip) uses a grammars-v4-derived
  grammar.
- **Generates the unparser, with exact lexical preservation.** JavaParser's
  `LexicalPreservingPrinter` offers the same guarantee — for Java only.
  VMF-Text generates it for any grammar you feed it.
- **Model-level editing instead of token-level rewriting.** ANTLR's
  `TokenStreamRewriter` also preserves untouched text, but you operate on
  token indices. VMF-Text gives you typed navigation and setters
  (`model.vmf().content().stream(MethodDeclaration.class)…`), plus VMF's
  change recording, undo and immutable read-only views.

## When another tool is the better choice

- **You want IDE support for your DSL now** (content assist, validation,
  quick fixes): use **Xtext** (Eclipse/LSP) or **Langium** (VS Code/LSP).
  VMF-Text ships no editor tooling and deliberately does not plan a
  workbench (see the non-goals in [ROADMAP.md](ROADMAP.md)).
- **You analyze or transform Java specifically and need name or type
  resolution:** use **JavaParser** with its symbol solver. VMF-Text has no
  semantic analysis — it delivers the syntax as a typed model and leaves
  meaning to you.
- **Your stack is Python:** use **textX**.
- **You only need to parse, or token-level text edits are enough:** plain
  **ANTLR4** may be all you need. VMF-Text adds value once you want a typed
  model and regenerated source.
- **You need a foundation with many maintainers and decades of deployment:**
  Xtext and ANTLR have that. VMF-Text is small and focused — the full test
  suite (including the Java 24 byte-identical round-trip) runs in CI on
  every commit, but the bus factor is what it is: one maintainer.

## Scope notes

- VMF-Text is a build-time generator (Gradle plugin) plus a runtime library.
  It is not itself a parser generator — ANTLR4 parses under the hood;
  VMF-Text generates the typed model and the (un)parsing API around it.
- Exact preservation applies to parsed content. Programmatically created or
  changed values are rendered with conservative separators; a pluggable
  formatter policy is on the roadmap
  (see [LEXICAL_PRESERVATION_ASSESSMENT.md](LEXICAL_PRESERVATION_ASSESSMENT.md)).
- Further afield, and deliberately out of scope here because the paradigm
  differs: JetBrains MPS (projectional editing), Spoofax and Rascal
  (workbenches / meta-programming with deep semantic analysis).
