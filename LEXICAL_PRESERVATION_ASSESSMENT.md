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
- Lexical metadata lives in `Object getPayload()` maps. This is flexible, but
  not ideal for VMF Jackson / JSON schema.
- Grammar rewriting injects Java-target ANTLR actions. This is pragmatic for
  the current Java generator but should be isolated if VMF-Text is to support
  more ANTLR target scenarios.
- Programmatically created models still need formatter policy decisions. Exact
  lexical preservation is well-defined for parsed models; generated models need
  a pretty-printing or grammar-aware separator policy.

## Recommended next design step: typed lexical metadata

**Status (0.2.1):** partially implemented — `LexicalInfo` now carries typed
`OptionalState { grammarElementPath, present }` entries in addition to the
flat arrays, and the default formatter reads typed data first. Remaining
work: typed `TriviaPiece` structures for the hidden-text pieces and retiring
the payload-map fallback.

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
