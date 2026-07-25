# Changelog

All notable changes to VMF-Text are documented here.

## [Unreleased]

### Added

- **Parser rule maps / model rewriting (`RuleMap`)** (#1) — a `RuleMap()` block in
  a `<!vmf-text!>` comment flattens a single-alternative wrapper parser rule at
  its reference sites, so the model exposes the wrapped target type directly:

  ```
  RuleMap() {
    (first: ValueExpression -> second: NumberLiteral) = {
        'first.getValue()',
        'ValueExpression.newBuilder().withValue(second).build()'
    }
  }
  ```

  A property that was `Expression` becomes `NumberLiteral`; parse builds the
  source then extracts the target (`first`), and unparse reconstructs the source
  from the target (`second`) so emitted text matches the original grammar.
  Round-trip is byte-exact for transparent wrapper rules. Details:
  [`docs/RULE_MAPS.md`](docs/RULE_MAPS.md).

### Fixed

- **Grammar `superClass` option preserved across rule-level `options {}`** (#14)
  — `GrammarToModelListener` re-created the grammar `Options` object on *every*
  `options { … }` block. Because rule-level option blocks are walked after the
  grammar-level one, a grammar-level `options { superClass = … }` followed by any
  rule-level `options { … }` silently lost its `superClass`, and both generated
  parsers then extended nothing. The options object is now initialized once
  (first block wins, subsequent blocks merge). Regression coverage:
  `core` `GrammarOptionsTest`.

## [0.2.1] — 2026-07-17

Library-grade unparse / lexical preservation polish on the typed `LexicalInfo`
model (no CST leap).

### Added

- **`ListShapeHint.separatorCount`** — unified splice for multi-token separators
  (`',' 'and'`) and separator-less `ID (ID)*` (`K=0`)
- **`ListShapeHint.optionalTrailingCount`** — optional trailing `','?` present/absent
  resolved from trivia size and preserved across splice
- **`ListShapeHint.alternativeIndex`** — every top-level alternative is analyzed;
  converter picks the size-matching candidate
- **`OptionalState.occurrenceIndex`** — repeated optional paths use exact indices
- **`OriginalLexeme`** — type-mapped lexer spellings round-trip when values are unchanged
- **Empty primitive list splice** — parenthesized `()` can round-trip add/remove
- Detailed unparsing guide: [`docs/UNPARSING.md`](docs/UNPARSING.md)
- Example ladder under [`examples/`](examples/) (`list-separators`, multilist,
  barelist, optional null/occurrence, original lexemes, …)

### Changed

- Optional presence is **path-keyed `OptionalState` only** (positional
  `optionalSymbols` removed)
- Insert-at-0 padding / undo for primitive and bare model-typed lists
- Multi-list trailer/opener ownership via codegen hints

### Fixed

- **Labeled parser wildcards** (`label=.` / `label+=.`) now generate correctly
  instead of a `FIXME: TYPE IS UNDEFINED` stub (#8)
- Grammar/model generation errors now **fail the build** with a clear
  `GradleException` instead of being silently downgraded to a console warning.
  Grammars that previously "succeeded" with undefined-type stubs will now
  surface the error directly instead of generating broken code.
- gradle-plugin's auto-created `pluginMaven` publication no longer collides
  with the published `mavenJava` coordinates (Gradle warned the two "will
  overwrite each other"); an eager `pluginMaven` property access in the
  signing config that only broke under a real signed publish was fixed the
  same way. Both were internal build/release-process bugs, not shipped in
  any prior released artifact.

### Still out of scope (needs CST / per-gap state)

- Index-dependent separators (Oxford comma)
- Unlabeled alt separators
- Optional separator **per gap** (`(','? item)*`)

### Publish

Published to Maven Central: `eu.mihosoft.vmf:vmf-text:0.2.1`,
`eu.mihosoft.vmf:vmf-text-gradle-plugin:0.2.1`; Gradle Plugin Portal:
plugin id `eu.mihosoft.vmftext` version `0.2.1` (including the Central
plugin marker, published from the plugin build itself for the first time).
Tag: [v0.2.1](https://github.com/miho/VMF-Text/releases/tag/v0.2.1).

## [0.2.0] — 2026-07-12

First non-preview release. See
[v0.2.0](https://github.com/miho/VMF-Text/releases/tag/v0.2.0).
