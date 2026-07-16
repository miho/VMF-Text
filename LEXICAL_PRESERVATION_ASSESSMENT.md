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
- Model edits keep lexical trivia for model-type changes and for **in-place**
  primitive/string rewrites (`list.set`, non-null property set). Structural
  add/remove (or null↔value) still clears trivia on that element and falls
  back to conservative separators — see
  [Edit invalidation](#edit-invalidation-primitive-vs-model-typed).

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
trivia slots — one slot before each terminal of that rule. Token **text** is
not stored in trivia; it is rendered from the property value. The generated
change listener in `model-converter.vm`
(`registerIgnoredPiecesOfTextChangeListener`) then:

| Edit kind | What happens to trivia |
|-----------|------------------------|
| Change / insert a **model-typed** child (`CodeElement`) | Keep parent trivia; pad the new child’s leading trivia with a space if needed so tokens do not glue together |
| **In-place** non-model rewrite (`list.set(i, …)`, or property set with both old and new non-null) | **Keep** all trivia (terminal footprint unchanged; only token text changes) |
| **Structural** add/remove on delimited primitive lists `'(' item (',' item)* ')'` when `trivia.size()` is `2N+2` or `2N+3`, or bare `item (',' item)*` when size is `2N`/`2N+1` | **Splice** two trivia slots (comma+value) so siblings keep their whitespace; supports bulk index ops; insert-at-0 padding is undoable |
| **Structural** add/remove on model-typed delimited lists `'['/'{' item (',' item)* ']'/'}'` when parent trivia size is `N+1` (or `2` if empty) | **Splice** parent comma/bracket slots (JSON arrays/objects); child leading trivia stays on each item |
| **Optional** null↔value on a non-model property | **Update** optional presence (flip recorded states / legacy all-true symbols) and keep leading trivia; insert a trivia slot for a newly present value terminal |
| Other structural non-model change (unknown shape, emptying a one-or-more primitive list) | **Clear** all trivia slots on that element → conservative separators |

After a clear, `DefaultFormatter` sees an empty trivia list and uses
`separatorBeforeEdited` / `ConservativeSeparatorPolicy` (typically a single
space before lexer-rule tokens, none before string terminals like `,`).

**Original lexemes:** type-mapped lexer tokens store their parse-time spelling
on `LexicalInfo.originalLexemes`. Unparse reuses that text when
`String.valueOf(value)` still matches the recorded key, so `(1, 2)` stays
`(1, 2)` and sibling edits keep unchanged spellings.

### ArrayLang vs Java / JSON

```antlr
array: '(' values+=INT (',' values+=INT)* ')' EOF;   // flat primitives on one node
```

`array.getValues().set(1, 99)` is an in-place list set: trivia is kept, so
`(1 ,  2,\n 3 )` becomes `(1 ,  99,\n 3 )`. `add` / `remove` on the same shape
splice two trivia slots (recognized when `trivia.size() == 2N+3`). (On
`lexical-preservation-take-2` and VMF-Text ≤0.2.0 any non-model edit cleared
all slots and reformatted to `( 1, 99, 3)`.)

Java method/string edits also keep sibling whitespace because they either
rewrite a property in place on a nested `CodeElement`, or only clear that
child’s trivia. JSON number edits behave similarly when each number is its own
`NumberValue` model type.

**Source bundles do not participate** in unparse-after-edit. They persist
original source for restore-when-semantics-match.

### Remaining gaps / what we can still improve

1. **Other list shapes:** only the delimited `'(' item (',' item)* ')'`
   footprint (`size == 2N+3`) is spliced today; bare `T (',' T)*` / multi-list
   rules still `clear()`. Extend via codegen hints from the unparser model.
2. **Grammar shape workaround:** wrap each list item as a model type
   (`value: n=INT`) so add/remove only invalidates that leaf.
3. **True token rewriting:** per-occurrence original lexemes / stream indices —
   larger redesign, not present today.

### What improved over `lexical-preservation-take-2`

Already on main before this work: leading/inter-rule hidden text via the previous
default-channel token; no space/tab collapsing; path-keyed `OptionalState`;
typed `LexicalInfo` / `TriviaPiece`; pluggable `ProgrammaticSeparatorPolicy`.

**This fix (0.2.1):** keep trivia on in-place primitive/string rewrites; surgically
splice trivia for ArrayLang/CombinedLexer-style delimited primitive lists
(including bulk ops and no-EOF footprints); splice **parent** comma/bracket
trivia for JSON-style model-typed delimited lists. Re-entrancy guard prevents
nested `triviaPieces` mutations from clearing state mid-splice.
take-2 (and ≤0.2.0) always emptied the hidden-text list for any non-model
property change.

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
