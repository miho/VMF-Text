# Lexical Preservation Assessment

## Status

The `lexical-preservation-take-2` branch now builds against the current VMF
snapshot after publishing VMF locally. The focused lexical preservation test
suite passes, including new regression coverage for leading/inter-rule hidden
text and nested unnamed optionals.

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
  while edited primitive values get conservative separator fallback.

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

The complete suite currently has one remaining failure in
`preventmultioccurrences`, caused by changed/current VMF traversal behavior for
self-referential containment. That failure is not caused by lexical preservation
logic.

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

- `optionalSymbols` is still fundamentally positional. The recent changes make
  it work for the covered nested optional cases, but a path-keyed structure
  would be more robust.
- Lexical metadata lives in `Object getPayload()` maps. This is flexible, but
  not ideal for VMF Jackson / JSON schema.
- Grammar rewriting injects Java-target ANTLR actions. This is pragmatic for
  the current Java generator but should be isolated if VMF-Text is to support
  more ANTLR target scenarios.
- Programmatically created models still need formatter policy decisions. Exact
  lexical preservation is well-defined for parsed models; generated models need
  a pretty-printing or grammar-aware separator policy.

## Recommended next design step: typed lexical metadata

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
