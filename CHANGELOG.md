# Changelog

All notable changes to VMF-Text are documented here.

## [Unreleased]

### Changed

- **VMF `0.2.12-SNAPSHOT`** (#65). `main` depends on the VMF snapshot that
  carries the generated list `equals` fix ([miho/VMF#87](https://github.com/miho/VMF/issues/87):
  lists of different length compare as unequal instead of throwing), VMF's
  Gradle 9/10 fixes, a plugin marker that reaches Maven Central, and — since
  snapshot build 2 ([miho/VMF#89](https://github.com/miho/VMF/issues/89)) — a
  VMF Gradle plugin that resolves its classpath when its tasks run, adds the
  Central snapshot repository itself and declares its runtime dependency in
  single-string notation. That map-notation dependency was the last Gradle
  deprecation warning in the `core` build; it is gone, and `core` applies the
  VMF plugin through the plain `plugins {}` DSL again. The builds resolve VMF
  from the Central snapshot repository (declared in the three builds and the
  ArrayLang example); the VMF-Text Gradle plugin adds that repository to
  consumer projects while its default `vmfVersion` is a snapshot. The
  identity-based rule removal from 0.2.5 stays; it no longer depends on the
  fix.

## [0.2.5] — 2026-10-21

Python 3 support, end to end: split lexer/parser grammars with `tokenVocab`
and base classes are one unit (#39), so the grammars-v4 Python grammar is
used as it ships ([`experiments/py3-grammar-trial`](experiments/py3-grammar-trial/README.md):
all 28 asyncio sources and 23 snippets round-trip byte-exact); the five core
defects that trial exposed are fixed (#52, #53, #54, the `tokens {}` block,
identity instead of VMF's structural equals); moved statements can be
re-indented and blocks built from scratch get their `NEWLINE`/`INDENT`/`DEDENT`
tokens and placement (#60). Naming: keyword tokens, `left`/`right`, camelCase
classes and properties for snake_case rules (#47, #63). The unparser's check
rules carry no predicates (#48), optional punctuation is left out of
programmatic elements (#60), and a parser base class lives in one package (#62).

### Publish

**GitHub release only** — tag [v0.2.5](https://github.com/miho/VMF-Text/releases/tag/v0.2.5)
with a signed Maven-repository archive (`vmf-text-0.2.5-maven-repo.zip`:
`eu.mihosoft.vmf:vmf-text:0.2.5`, `eu.mihosoft.vmf:vmf-text-gradle-plugin:0.2.5`
and the plugin marker) attached. Not published to Maven Central or the Plugin
Portal: the monthly release budget is shared across all `eu.mihosoft` projects
(see `docs/RELEASING.md`). The same content is available as `0.2.5-SNAPSHOT`
on the Central snapshot repository (90-day retention). The latest version on
Maven Central remains 0.2.2. VMF `0.2.11`, ANTLR `4.13.2`.

### Added

- **Indentation-structured languages: blocks built from scratch** (#60). In
  Python a block is `NEWLINE INDENT stmt+ DEDENT`, so a programmatic block
  carries the three tokens as properties and the caller had to set them.
  `<Grammar>TriviaSupport.fillIndentationTokens(root, indentUnit)` now sets
  every unset one by nesting depth — a `NEWLINE` becomes the line break, a
  `DEDENT` the empty string, an `INDENT` the indentation of the enclosing
  block (parsed or filled) plus one unit — and
  `Formatter.newIndentationTokenFormatter()` (`IndentationTokenSeparatorPolicy`,
  token names and spacing configurable) tracks the indentation in effect
  through every rendered `INDENT`/`DEDENT` and starts the programmatic
  statements that follow a `NEWLINE` or `DEDENT` on their own line at that
  depth, also inside a parsed block. Every token-typed model property now
  carries the VMF annotation `vmf-text:token` with the token's grammar name,
  which is what the helper goes by; a separator policy is told when a new
  document starts (`documentStart()`). Regression grammar `indent` (split,
  with a Python-style lexer base); the Python trial's `py3trial.Build` builds a
  function and nests one into a parsed class body.

- **`<Grammar>TriviaSupport.reindent(element, oldIndent, newIndent)`** rebases
  the indentation of a parsed subtree: the first line's leading whitespace,
  every inner line start that begins with the old indentation, and
  whitespace-only token values (Python's `INDENT`). A statement moved into a
  block at another depth keeps its trivia otherwise — in Python that changes
  the meaning (`experiments/py3-grammar-trial`, `py3trial.Edit`). The
  generated `<Grammar>TriviaSupport` class is public now.

- **Split grammars** (#39): a `parser grammar X` with
  `options { tokenVocab = L; }` and the `lexer grammar L` next to it are
  processed as one unit — the way grammars-v4 ships most languages. The
  model is named after the parser grammar minus its `Parser` suffix
  (`Python3Parser` + `Python3Lexer` → `Python3Model`, `Python3ModelParser`),
  the lexer is generated into the same package (its `superClass` base class
  goes there too), the unparser's check grammar shares the lexer, and the
  Gradle plugin regenerates the parser grammar when the lexer grammar
  changes. Regression grammar `splitlex`.

### Changed

- **Optional punctuation is left out of elements built from scratch** (#60).
  An element without recorded optional states printed every state-driven
  optional construct ("pretty-print"), so a programmatic Python statement came
  out as `pass;`, a C parameter list as `(int a, ...)`, and an absent
  `('(' arglist? ')')?` as `class A():`. Such a construct is now left out when
  nothing in the model asks for it: always for optional punctuation
  (`';'?`, `(',' '...')?`, `('(' ')')?` — separators and brackets nothing in
  the model could stand for), and for a block with named elements while its
  properties are all unset — `('(' bases=idList? ')')?` prints once `bases` is
  set (`RuleInfo.isUnbackedOptional()`, generated per element as a check
  against the unparsed object). An optional keyword label such as
  `('anchor:')?` keeps printing: it reads as the fuller form of the syntax.
  Elements whose optionality comes from a literal-only sibling alternative and
  parsed elements are unchanged. Regression: `optblock`.

- **Auto-labeling names** (#47, from the JavaScript and Python trials):
  a lexer rule that is a single literal (`Var: 'var';`) is a keyword, not a
  value — it gets no property any more (`var=Var`, `ifValue=If`,
  `returnValue=Return` are gone; inside a repetition it is captured as a list
  named after the literal, like a string literal) and it provides keyword
  context (`Else statement` → `elseStatement`); the two operands of an
  `expr op expr` alternative are `left` and `right` instead of `expr` /
  `expr2`; property names and synthetic rule names derived from snake_case
  rules drop the underscores (`simpleStmts`, `fileInputChoice`).
  Regression grammar `naming`.

- **snake_case parser rules are renamed to camelCase before any other
  pass** (#63), so the model class of `file_input` is `FileInput` — like its
  properties since #47 — and the ANTLR parser, the model converter and the
  unparsers all use that one spelling (`fileInput()`, `FileInputContext`).
  Only names with an underscore between word characters change
  (`subscript_` stays); a rule whose camelCase name exists already keeps its
  name; rule names inside actions and predicates are not rewritten.
  `Root(rule=file_input)` and the apply-to lists of type and rule maps accept
  either spelling; custom model interfaces declared in VMF-Text metadata
  must use the camelCase class names. Nested lifted blocks no longer stack
  suffixes: a block inside `dictorsetmakerChoice` becomes
  `dictorsetmakerChoice2`, not `dictorsetmakerChoiceChoice`.

- **One parser base class** (#62). An unqualified parser `superClass` option
  is qualified with `<package>.parser` in the grammar handed to ANTLR, so the
  model parser, the unparser's own copy of the rewritten grammar and its
  check grammar all extend the one class in `<package>.parser`; the second
  copy in `<package>.unparser.antlr4` is no longer needed (removed for the
  `java24` test grammar and the Python trial). Lexer base classes were
  already shared.

### Fixed

- **The unparser check rules no longer copy actions and semantic predicates**
  (#48). The generated `<Grammar>ModelUnparserGrammar.g4` validates the text an
  alternative produced, fragment by fragment; a predicate that looks outside
  the fragment (`here(LineTerminator)`, a symbol table) was evaluated against
  a fragment without surroundings. Actions and predicates are now stripped
  from the check rules (brace-aware, string-literal-safe); the model parser
  keeps them.
- **A `tokens {}` / `channels {}` block or a named action before the first
  parser rule no longer breaks grammar reading.** The bundled ANTLR grammar
  lexer took the identifiers inside such a block for the start of a lexer rule,
  so the first parser rule's injected `locals [...]` block was lexed as a
  character set (`mismatched input '[...]' expecting BEGIN_ARGUMENT`; the
  grammars-v4 Python 3 grammar declares `tokens { INDENT, DEDENT }`).
- **Generation no longer dies with `Index 1 out of bounds for length 1` while
  collecting the unparser rules.** The generator removed the parents of
  labeled alternatives from the rule list by structural equality; VMF's
  generated `equals` (≤ 0.2.11) indexes the second list with the first list's
  size, so the first pair of alternatives with different element counts threw.
  The generator now works by identity (the Python 3 grammar was the first to
  hit this).
- **An optional block whose only named elements are themselves optional is no
  longer emitted when it was absent** (#52). `classdef: 'class' name ('('
  arglist? ')')? ':' block` unparsed `class A:` as `class A( ):`, and the
  trailing-comma block of Python's `typedargslist` turned `def f(a, b):` into
  `def f(a, b,):`. Only a mandatory named element (no `?`/`*` suffix) can stand
  in for the presence of its alternative; otherwise the block's terminals stay
  state-driven like a literal-only block. Regression grammar `optblock`.
- **Optional literals inside a second or later labeled alternative are no
  longer dropped** (#53). The parser recorded their presence under the
  alternative's index in its rule (`/r134/a1/...`), the unparser — for which a
  labeled alternative is a rule of its own — looked under `/r134/a0/...`, so
  Python's `(value,)` came back as `(value)` and `[1, 2,]` as `[1, 2]`. Both
  sides now use index 0 for labeled alternatives. Regression grammar
  `labeledopt`.
- **Auto-labeling lifts repeated blocks with an optional property into
  per-item sub classes** (#54). `(',' tfpdef ('=' test)?)*` captured as the
  parallel lists `tfpdefs` / `tests` loses which iteration had a default:
  Python's `def f(self, sock, protocol, waiter=None)` came back as
  `def f(self, sock=None, protocol, waiter)`. Such a block becomes
  `<rule>Items+=<rule>Item*` with a synthetic `<rule>Item` rule, so each
  iteration is one typed element that keeps its own optional parts (and gets
  a usable API: `item.getTest()`). Regression grammar `itemlift`.
- **An unlabeled lexer rule that is not a plain sequence of literals is no
  longer emitted as garbage.** `NEWLINE: '
'? '
';` referenced without a
  label unparsed as `
'? '
`; such rules are now left out as documented
  (label them, `nl=NEWLINE`), while `SEMI: ';';` and `ARROW: '-' '>';` keep
  being emitted verbatim.

## [0.2.4] — 2026-10-20

Mixed-mode formatting for programmatic elements (#40) and the two core fixes
from the second unlabeled grammars-v4 trial, the ECMAScript grammar
([`experiments/js-grammar-trial`](experiments/js-grammar-trial/README.md)):
semantic predicates survive the grammar rewrites and a nested `EOF` keeps its
trailing hidden text. All 15 JavaScript snippets round-trip byte-exact; the C
trial is unchanged at 27 of 32.

### Added

- **Mixed-mode formatting for programmatic elements** (#40) — the
  lexical-preserving default formatter now gives its
  `ProgrammaticSeparatorPolicy` a `FormattingContext` (previous terminal,
  element depth, line position) via new context-aware default methods, and
  notifies the policy of every rendered terminal so it can track structure
  across parsed regions. New built-in `BlockIndentSeparatorPolicy`
  (`Formatter.newBlockIndentFormatter()`): line breaks after block openers,
  closers and statement ends, indentation by block depth, no glued tokens —
  configurable literal sets. Parsed text stays byte-exact; a subtree inserted
  into parsed text is indented at the depth of the insertion point.
- **Output ranges for programmatic / edited elements** —
  `Formatter.programmaticRanges()` reports, after each top-level unparse, the
  character and line ranges of every element rendered without lexical
  information (outermost first). Everything outside them reproduces the parsed
  input byte for byte, so an external formatter can be applied to exactly
  those lines (google-java-format `--lines`, clang-format `-lines`).
- `Formatter.begin(element, writer)` callback, the mirror of `done(...)`.

### Removed

- The deprecated, never-finished `JavaStyleFormatter` /
  `Formatter.newJavaStyleFormatter()`; use `newBlockIndentFormatter()` or a
  custom `ProgrammaticSeparatorPolicy`.

### Fixed

Both found by the second unlabeled grammars-v4 trial, the ECMAScript grammar
([`experiments/js-grammar-trial`](experiments/js-grammar-trial/README.md));
regression grammar `asi`.

- **Semantic predicates survive the grammar rewrites.** A predicate `{...}?`
  was taken for an optional element and wrapped as `({...} {flag})?`, which
  turns it into a plain action: the generated unparser grammar did not
  compile (`not a statement`), and a predicate-guarded alternative would
  silently have accepted more input. The optional-wrapping and state-injection
  passes now leave predicates and actions alone; `@parser::members` reach the
  unparser grammar through its import. The Java24 test grammar had written
  its two predicates as `{ this.X(); }?` statements, which only compiled
  because of the wrapping — corrected to `{ this.X() }?`.
- **A nested rule that matches `EOF` keeps the hidden text before the end of
  the input.** Once `EOF` is matched, ANTLR makes it the stop token of every
  enclosing rule, so with `eos: ';' | EOF | {lineTerminatorAhead()}?`
  (JavaScript's automatic semicolon insertion) the trailing newline is
  recorded in the `eos` element — which the unparser skipped: `x = 1\ny = 2\n`
  came back without its last newline. The generator now hands every `EOF`
  reference to the new `Formatter.eof(...)` hook; the default formatter emits
  the recorded hidden text for parsed elements and nothing for programmatic
  ones (custom formatters: no-op default).

### Publish

**GitHub release only** — tag [v0.2.4](https://github.com/miho/VMF-Text/releases/tag/v0.2.4)
with a signed Maven-repository archive (`vmf-text-0.2.4-maven-repo.zip`:
`eu.mihosoft.vmf:vmf-text:0.2.4`, `eu.mihosoft.vmf:vmf-text-gradle-plugin:0.2.4`
and the plugin marker) attached. Not published to Maven Central or the Plugin
Portal: the monthly release budget is shared across all `eu.mihosoft` projects
(see `docs/RELEASING.md`). The same content is available as `0.2.4-SNAPSHOT`
on the Central snapshot repository (90-day retention). The latest version on
Maven Central remains 0.2.2. VMF `0.2.11`, ANTLR `4.13.2`.

## [0.2.3] — 2026-10-20

Grammar-shape fixes driven by the unlabeled grammars-v4 C grammar trial
([`experiments/c-grammar-trial`](experiments/c-grammar-trial/README.md)): the
same grammar went from 14 to 27 of 32 sample programs byte-exact; the remaining
five use GNU extensions the grammar rejects and now fail loudly instead of
producing garbage.

### Added

- **`Root(rule=<name>)` metadata** — declares the parser rule that becomes the
  model root (and the start rule of the generated parser). Default stays "the
  first parser rule", which silently picked `primaryExpression` for a C grammar
  that lists `compilationUnit` last. Example:
  `/*<!vmf-text!> Root(rule=compilationUnit) */`. Naming a rule that does not
  exist fails generation with a clear message.
- **Auto-labeling lifts nested alternative blocks into typed sub classes** —
  a nested block with two or more alternatives (e.g. C's
  `jumpStatement: ('goto' Identifier | 'continue' | 'break' | 'return'
  expression? | …) ';'`) is rewritten into a synthetic rule `<rule>Choice`
  whose alternatives become `<Rule>ChoiceAlt1…N` sub classes, exactly like
  top-level alternatives. Before, alternatives consisting only of literals left
  nothing in the model and `return 1;` / `break;` unparsed as `;`. A
  block's EBNF suffix moves to the reference, so `(a | b)*` becomes one ordered
  list instead of two lists that lose their interleaving. Token sets such as
  `('+' | '-')` keep their literal capture; blocks with an empty alternative or
  with manual labels, actions or predicates are left untouched. The auto-label
  report lists every lifted block.
- **Auto-labeling captures every literal choice** — a non-repeated token-set
  block such as `('.' | '->') Identifier` is now labeled `operator=…` (a String
  property); only repeated ones were captured before, so `one->size` came back
  as `onesize`.

### Changed

- **Auto-labeling names captured literals after their text** (#38) — a
  literal captured inside a repeated block used to be `symbols`, `symbols2`,
  …; it is now `commas`, `semicolons`, `lBrackets` / `rBrackets`, `arrows`,
  `plusPluses`, `staticKeywords` and so on (known punctuation by name, other
  punctuation by its characters, keyword-like literals as `<keyword>Keyword`,
  `symbol` only as a last resort). Token-set blocks such as `('+' | '-')` keep
  the name `operators` (`operator` when not repeated). Generated API names
  change for auto-labeled grammars that relied on `symbols…`.
- **`parse(...)` fails on syntax errors by default** — the generated
  `<Grammar>ModelParser` throws `<Grammar>ModelParser.SyntaxErrorException`
  (with `getErrors()`, one `line:column message` per error) instead of
  returning a model built from ANTLR's error-recovered parse tree, which
  unparsed to garbage without any indication. `setFailOnSyntaxError(false)`
  restores the old behaviour; registered error listeners still see every
  error.
- **VMF `0.2.10` → `0.2.11`** — core's own model generation and its `vmf` /
  `vmf-runtime` dependencies, the Gradle plugin's default `vmfVersion`, and
  the README / example pins. Upstream changes: corrected immutable-containment
  error message, vmf-jackson (de)serialization + JSON-schema fixes.

### Fixed

- **One-or-more lists written as a single element (`items+=ID+`,
  `StringLiteral+`) can be unparsed** — the generated matcher only counted
  parser-rule elements as consumed, so any lexer-rule or terminal `X+` list
  rendered its elements and then reported the alternative as not matched
  (`Cannot unparse rule … Language model is invalid!`). Every C file with a
  string literal failed on this. The `ID (ID)*` workaround in
  `docs/UNPARSING.md` is no longer required.
- **A root rule with labeled alternatives generates a compilable unparser** —
  the top-level dispatch referenced a `<root>Unparser` field that does not
  exist when the root's alternatives are their own types; it now goes through
  the alternative-aware overload.
- **The `root` flag of a rule class is authoritative** — `rootClass()` used to
  return the first rule class regardless of the flag.
- **Appending to a comma-separated list no longer drops the new separator** —
  for list shapes the splice does not recognize (e.g. Java's
  `'(' (params (',' params)* (',' last)?)? ')'`), the formatter treated the
  `','` inside `(',' item)*` as a state-driven optional and found no recorded
  state for the new iteration, so `(int a)` + a parameter became
  `(int aint b)`. Terminals whose optionality comes only from a
  property-driven repetition are now present by construction; recorded
  presence states keep governing `?` optionals and literal-only repetitions
  (`UPRuleUtil.isOptionalForFormatter`). Regression: `test-suite`
  `listgrow`.
- **Conservative separators never glue a keyword to the previous token** —
  after an edit the fallback policy only spaced lexer-rule tokens, so an
  inserted `int insP` parameter rendered as `bint insP`; word-like terminals
  now get a space too, punctuation still does not.
- **Strict parsing exposed three silently broken existing tests** (`java8`,
  `java24` parameter insertion, `minijava` round trip): they re-parsed lossy
  unparse output through ANTLR's error recovery and compared the recovered
  model. The parameter-insertion cases are fixed by the two items above, the
  MiniJava case by the next item.
- **Literal-only nested alternative blocks in hand-labeled grammars are no
  longer dropped on unparse** (#37) — `'String' ('[' ']'|'[]') args=Identifier`
  in MiniJava unparsed `String[] arguments` as `Stringarguments`; Java 8's
  shift operators `('<' '<' | '>' '>' '>' | '>' '>')` had the same shape. Such
  blocks (two or more alternatives made only of literals / token references,
  not a plain token set, no manual labels inside) are lifted into a synthetic
  `<rule>Choice` rule with one typed sub class per alternative, exactly like
  auto-labeling does; the enclosing rule gains a `<Rule>Choice` property
  (`<rule>Choices` list when repeated). Regression: `test-suite`
  `blockliftmanual`; MiniJava's round-trip test is strict again.

### Publish

**GitHub release only** — tag [v0.2.3](https://github.com/miho/VMF-Text/releases/tag/v0.2.3)
with a signed Maven-repository archive (`vmf-text-0.2.3-maven-repo.zip`:
`eu.mihosoft.vmf:vmf-text:0.2.3`, `eu.mihosoft.vmf:vmf-text-gradle-plugin:0.2.3`
and the plugin marker) attached. Not published to Maven Central or the Plugin
Portal: Maven Central's monthly release budget is shared across all
`eu.mihosoft` projects (see `docs/RELEASING.md`), and this batch is better
spent there with a later release. The same content is available as
`0.2.3-SNAPSHOT` on the Central snapshot repository (90-day retention). The
latest version on Maven Central remains 0.2.2. VMF `0.2.11`, ANTLR `4.13.2`.

## [0.2.2] — 2026-10-20

Parser rule maps (model rewriting), fail-fast code generation and quieter
tinylog-based logging, option/robustness fixes, plus auto-naming polish and
byte-exact RuleMap token-bearing wrappers ([#33](https://github.com/miho/VMF-Text/pull/33)
folded into this release — one Maven Central publish). Also repairs the Gradle
plugin's Maven Central publication (the 0.2.1 plugin only resolved via the
Plugin Portal) and makes the builds Gradle 10-clean.

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
- **RuleMap shell LexicalInfo for token-bearing wrappers** (#32 / #33) — when a
  `RuleMap` flattens a wrapper that owns terminals (e.g.
  `expr: '(' value=numberLiteral ')'`), parse stores the discarded wrapper's
  trivia/optional-state/range as `CodeElement.getRuleMapShellLexicalInfo()` on
  the flattened target; unparse restores it onto the reconstructed source so
  unedited parse→unparse is byte-exact. Transparent wrappers and programmatic
  targets without a shell are unchanged (conservative separators). See
  [`docs/RULE_MAPS.md`](docs/RULE_MAPS.md).
- **Automatic naming polish** (#31 / #33) — auto-label drops the `Node` suffix
  (plain lower-camel rule/token names); duplicate refs after a keyword literal
  prefer contextual names (`'else' statement` → `elseStatement`); English-ish
  plurals for `…y` → `…ies`; inferred-name report is written under
  `build/reports/vmf-text/autolabel-report.txt` (Gradle `emitAutoLabelReport`).
  Labels that would collide with a parser-rule name use PascalCase in the
  rewritten grammar so ANTLR stays valid while getters remain `getStatement()`.

### Changed

- **`test-suite` miniclang `parseUnparseRunCodeTest`** now skips its native
  compile-and-run step on aarch64/arm (via JUnit `Assume`) instead of failing the
  suite: the bundled vtcc/TCC 0.9.27 cannot link modern glibc there. The
  parse → unparse round trip still runs on every platform; amd64 (including CI)
  is unaffected (#23).
- **Code generation now fails loudly instead of emitting partial output** —
  `VMFText.generate`/`generateModelCode` and `ModelGenerator` no longer swallow
  exceptions (`printStackTrace` + continue); they throw a new
  `VMFTextGenerationException`, and a non-zero ANTLR tool exit aborts generation
  rather than silently writing partial code that only fails later at `javac`
  time. The Gradle plugin surfaces this as a `GradleException`. Coverage: `core`
  `GenerationFailureTest`.
- **Logging migrated to tinylog** — ~60 direct
  `System.out`/`System.err`/`printStackTrace` sites across the core pipeline now
  go through a `Logger` (tinylog-api/impl + `tinylog.properties`), quiet by
  default at `info` level, removing the unconditional stdout writes previously
  emitted on every `generate()`.
- **ROADMAP sync** (#30 / #33) — Phase 2 items delivered in 0.2.1 are marked
  done; #31/#32 folded into this release rather than a separate 0.2.3.
- **Build: Gradle 9.8.2 wrapper (was 9.0.0) and Gradle 10-clean build scripts** —
  `prop = value` assignment, `layout.buildDirectory` instead of `buildDir`,
  single-string dependency notation, and no `Task.project` access inside
  `doLast`. `sh ./gradlew help --warning-mode all` is clean in all three builds;
  the one remaining warning during a `core` build comes from the VMF `0.2.10`
  Gradle plugin (already fixed upstream, not yet released).
- **Build: `com.github.hierynomus.license` plugin removed** from `core`,
  `gradle-plugin` and `test-suite` — unmaintained, ran with
  `ignoreFailures = true` (so it checked nothing) and breaks under Gradle 10
  (VMF #86). License headers stay in the sources; `core/config/HEADER` remains
  as the template.
- **CI: the Windows leg is required** (was `continue-on-error`; it has been
  green on every run since July), and the `examples/java24-roundtrip` job
  resolves the published artifacts from Maven Central **only** (no
  `gradlePluginPortal()` fallback), so a missing or broken Central plugin
  marker fails CI.

### Fixed

- **Grammar `superClass` option preserved across rule-level `options {}`** (#14)
  — `GrammarToModelListener` re-created the grammar `Options` object on *every*
  `options { … }` block. Because rule-level option blocks are walked after the
  grammar-level one, a grammar-level `options { superClass = … }` followed by any
  rule-level `options { … }` silently lost its `superClass`, and both generated
  parsers then extended nothing. The options object is now initialized once
  (first block wins, subsequent blocks merge). Regression coverage:
  `core` `GrammarOptionsTest`.
- **Gradle plugin loads under a Gradle daemon on JDK 17** — the plugin's Groovy
  is now compiled to Java 11 bytecode (was Java 21, which a Gradle running on
  JDK 17 could not load); Groovy also bumped `2.4.12` → `4.0.21`.
- **Gradle plugin marker is published to Maven Central again** — 0.2.1 shipped
  no `eu.mihosoft.vmftext:eu.mihosoft.vmftext.gradle.plugin` marker on Central
  (only 0.2.0 is there), and the marker that went to the Plugin Portal pointed
  at a renamed `vmf-text-gradle-plugin-portal` artifact that exists nowhere
  else, so `plugins { id "eu.mihosoft.vmftext" version "0.2.1" }` only resolved
  through `gradlePluginPortal()`. The plugin build now publishes the
  publications `java-gradle-plugin` creates (`pluginMaven` + the marker) to
  Central *and* the Portal under the standard
  `eu.mihosoft.vmf:vmf-text-gradle-plugin` coordinates, with the POM metadata
  Central requires; the hand-written `mavenJava` publication and the `-portal`
  rename are gone (`gradle-plugin/gradle/publishing.gradle`). Verified with a
  signed publish rehearsal (`publishAllPublicationsToBuildDirRepository`) and a
  consumer that resolves plugin + marker from plain Maven repositories only.
- **Jar manifests carry the real version** — `Implementation-Version` and
  `Specification-Version` read `unspecified` in the 0.2.1 jars because the
  project version was assigned after the manifest had been configured; the
  version now comes from `config/common.properties` before anything reads it.
- **The plugin no longer causes Gradle 10 deprecation warnings in consumer
  builds** — the runtime dependencies it adds (`vmf-runtime`, `antlr4-runtime`)
  use single-string notation instead of the deprecated `group:/name:/version:`
  map form.

### Publish

Published 2026-10-20 to Maven Central: `eu.mihosoft.vmf:vmf-text:0.2.2`,
`eu.mihosoft.vmf:vmf-text-gradle-plugin:0.2.2` and the plugin marker
`eu.mihosoft.vmftext:eu.mihosoft.vmftext.gradle.plugin:0.2.2` (so
`mavenCentral()` alone resolves the plugin again); Gradle Plugin Portal: plugin
id `eu.mihosoft.vmftext` version `0.2.2`. VMF stays `0.2.10`, ANTLR `4.13.2`.
Tag: [v0.2.2](https://github.com/miho/VMF-Text/releases/tag/v0.2.2).

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
