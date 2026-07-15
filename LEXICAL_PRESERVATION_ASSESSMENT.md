# Lexical Preservation Assessment

## Status

The `lexical-preservation-take-2` branch now builds against the current VMF
snapshot after publishing VMF locally. The focused lexical preservation test
suite passes, including new regression coverage for leading/inter-rule hidden
text and nested unnamed optionals. The complete VMF-Text test suite also passes
on the current JDK 21-based VM.

## What changed

- Hidden text collection now initializes each parse-rule context from the
  previous **default-channel** token. This preserves hidden-channel trivia that
  appears before a rule's first token, including leading root whitespace and
  whitespace between repeated child parser rules.
- The default lexical-preserving formatter no longer collapses repeated spaces
  or tabs. Parsed source is treated as exact text to reproduce.
- Normal unparsing no longer emits optional-symbol debug output.
- Optional subrule state tracking no longer records a separate state for the
  subrule wrapper itself. The contained optional terminals/lexer elements carry
  the relevant presence state, avoiding positional desynchronization for cases
  like `('(' names+=IDENTIFIER* ')')?`.
- Model edits keep lexical trivia for model-type/list changes where possible,
  while edited primitive values get conservative separator fallback (same
  invalidate-all-trivia policy as on `lexical-preservation-take-2`; see
  [Edit invalidation](#edit-invalidation-primitive-vs-model-typed) below).

## Current test evidence

Passing targeted checks:

```bash
cd test-suite
./gradlew clean test --tests "eu.mihosoft.vmftext.tests.lexicalpreservation.*" --no-daemon
./gradlew test --tests "eu.mihosoft.vmftext.tests.arraylang.TestArrayLang" \
  --tests "eu.mihosoft.vmftext.tests.ruleinfo.RuleInfoTest" --no-daemon
./gradlew clean test --tests "eu.mihosoft.vmftext.tests.json.Test" \
  --tests "eu.mihosoft.vmftext.tests.arraylang.TestArrayLang" \
  --tests "eu.mihosoft.vmftext.tests.lexicalpreservation.*" --no-daemon
```

The complete suite now passes:

```bash
cd test-suite
./gradlew test --no-daemon
```

## Assessment of the current approach

The current lexical preservation design is a good incremental architecture:

1. parse with the normal ANTLR grammar;
2. convert the parse tree to VMF objects;
3. attach parse-context, hidden-text and optional-presence metadata;
4. let the generated unparser and formatter reconstruct the concrete syntax.

The main strength is that the semantic model remains mostly grammar-derived,
while comments/whitespace do not need to become semantic properties. The main
weakness is that the metadata is currently held in untyped payload maps and
optional-presence is order-sensitive.

## Risks that remain

- ~~`optionalSymbols` is still fundamentally positional.~~ **Resolved (0.2.1):**
  optional presence is now recorded as `"<grammar-element-path>=<present>"`
  entries and mirrored into typed `OptionalState` objects on `LexicalInfo`;
  the formatter consumes them keyed by `RuleInfo.getRulePath()` with
  per-path occurrence counters. A missing entry means *absent* instead of
  shifting all subsequent states, which eliminates the cross-path
  desynchronization class entirely. Because the unparser model is built from
  the rewritten grammar, `rewriteGrammar` runs in two passes (structural
  wrappers first, then a reparse and path computation on the final
  structure), so recorded and consumed paths are identical by construction.
  The legacy positional `Boolean` list remains as a derived view and as the
  consumption fallback for pre-0.2.1 data.
  *Residual limitation:* elements that are effectively optional by
  alternative structure (no EBNF suffix) record presence only when their
  alternative is taken; repeated occurrences of the *same* path with mixed
  presence can therefore still be assigned to the earliest occurrences.
  EBNF optionals (`x?`, `x*`) record explicit presence/absence per encounter
  and are exact.
- ~~Lexical metadata lives in `Object getPayload()` maps.~~ **Resolved (0.2.1):**
  lexical metadata is typed on `LexicalInfo` (`TriviaPiece`, `OptionalState`);
  freshly parsed models no longer write `vmf-text:` payload entries.
- ~~Grammar rewriting injects Java-target ANTLR actions.~~ **Isolated (0.2.1):**
  Java-target injection and read-back live behind `AntlrTargetOptionalStateProvider`
  (`JavaAntlrOptionalStateProvider`); grammar rewrite behaviour is unchanged.
- ~~Programmatically created models still need formatter policy decisions.~~
  **Resolved (0.2.1):** `ProgrammaticSeparatorPolicy` is pluggable; default
  policy matches the previous conservative separator fallback.

## Edit invalidation (primitive vs model-typed)

On each `CodeElement`, parse-time hidden text is stored as an ordered list of
trivia slots — one slot before each terminal of that rule. The generated change
listener in `model-converter.vm`
(`registerIgnoredPiecesOfTextChangeListener`) then:

| Edit kind | What happens to trivia |
|-----------|------------------------|
| Change / insert a **model-typed** child (`CodeElement`) | Keep parent trivia; pad the new child’s leading trivia with a space if needed so tokens do not glue together |
| Change a **non-model** property (primitive, `String`, flat `List<Integer>`, …) | **Clear all** trivia slots on that element |

After a clear, `DefaultFormatter` sees an empty trivia list and uses
`separatorBeforeEdited` / `ConservativeSeparatorPolicy` (typically a single
space before lexer-rule tokens, none before string terminals like `,`).

### ArrayLang vs Java / JSON

```antlr
array: '(' values+=INT (',' values+=INT)* ')' EOF;   // flat primitives on one node
```

`array.getValues().set(1, 99)` is a non-model list change on the single `Array`
element, so **every** slot is dropped and the whole list reformats, e.g.
`(1 ,  2,\n 3 )` → `( 1, 99, 3)`.

Java method/string edits look “surgical” because the changed property lives on
a **nested** `CodeElement` (`MethodDeclaration`, `StringLiteral`, …). Only that
node’s trivia is cleared; siblings keep theirs. JSON number edits behave
similarly when each number is its own `NumberValue` model type.

**Source bundles do not help here.** They persist original source for
restore-when-semantics-match; they are not consulted by the unparser after a
live model edit.

**`lexical-preservation-take-2` did not preserve ArrayLang value edits
either.** It used the same policy (payload key `vmf-text:ignored-pieces-of-text`
replaced with an empty list). Current main only moved that state onto typed
`LexicalInfo.triviaPieces`.

There is no “anchor” API in the lexical layer today. What acts like an anchor
is a **model-typed child** that owns its own trivia list.

### What we can do about it

1. **Grammar shape (works today, no core change):** wrap each list item in a
   parser rule so each value is a `CodeElement`:
   `value: n=INT` / `array: '(' values+=value (',' values+=value)* ')'`.
   Then `set` on one value only invalidates that leaf.
2. **Surgical trivia update (core fix):** on indexed primitive list changes,
   do not `clear()` the whole list; map the list index to the corresponding
   terminal/trivia slots (the TODO in `model-converter.vm` from 2018) and leave
   siblings alone.
3. **True token rewriting (larger redesign):** per-occurrence original token
   text / stream indices and splice edits — closer to `TokenStreamRewriter`,
   not present today.

## Recommended next design step: typed lexical metadata

**Status (0.2.1):** implemented — `LexicalInfo` carries typed `TriviaPiece`
entries, `OptionalState { grammarElementPath, present }` objects, and the
default formatter reads typed data exclusively. The untyped payload-map fallback
has been retired; `CodeElement.getPayload()` is deprecated for VMF-Text internals.
Programmatically created models consult a pluggable `ProgrammaticSeparatorPolicy`.

For robust editor/JSON-schema support, lexical state should become explicit and
typed, or be explicitly excluded from the semantic JSON schema:

```text
LexicalInfo
  - TriviaPiece[] leadingTrivia
  - TriviaPiece[] trailingTrivia
  - OptionalState[] optionalStates
  - String grammarElementPath
  - CodeRange originalRange

OptionalState
  - String grammarElementPath
  - int occurrenceIndex
  - boolean present

TriviaPiece
  - String text
  - TriviaKind kind
```

This can still be attached as non-semantic metadata, but it gives VMF/Jackson a
clear schema story instead of arbitrary `Object` payloads.

## Recommended next design step: automatic naming

Manual ANTLR labels are still the best way for grammar authors to define a
clean API, and they should remain authoritative. However, for the larger goal of
feeding VMF-Text a Java/C++/JavaScript/DSL grammar and receiving a rich VMF
model, explicit labels everywhere are too expensive.

Recommended naming policy:

- explicit labels always win;
- parser-rule references default to lower-camel rule names;
- token references default to lower-camel token names;
- repeated elements become list properties;
- duplicate references get deterministic suffixes or path-derived names;
- literals are usually syntax, not semantic properties, except operator-like
  literals that should be represented as discriminators;
- unlabeled alternatives get deterministic generated class names unless ANTLR
  alt labels are present;
- the generator should emit a report of inferred names so users can decide where
  to add explicit labels.

This gives VMF-Text a migration path from "labeled ANTLR grammar required" to
"ANTLR-compatible grammar accepted, explicit labels improve API quality".
