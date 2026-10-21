# Experiment: the grammars-v4 HEAD JavaScript grammar through VMF-Text

*Run 2026-10-21 against VMF-Text `0.2.6-SNAPSHOT` (unreleased `main` with
trivia from character positions, #61, plus the two fixes made during this
trial), VMF `0.2.12-SNAPSHOT`, ANTLR 4.13.2, JDK 21. Fourth trial language
after [`../c-grammar-trial`](../c-grammar-trial/README.md),
[`../js-grammar-trial`](../js-grammar-trial/README.md) (ECMAScript 5.1) and
[`../py3-grammar-trial`](../py3-grammar-trial/README.md); tracked as
[#64](https://github.com/miho/VMF-Text/issues/64).*

**Question.** Split grammars ([#39](https://github.com/miho/VMF-Text/issues/39))
make the maintained grammars-v4 `javascript/javascript` grammar reachable: a
lexer grammar and a parser grammar with Java base classes, lexer modes for
template literals, a custom `ERROR` channel, lexer predicates (strict mode,
regular-expression literals), parser predicates (`n("of")`,
`notLineTerminator()`, automatic semicolon insertion through `eos`), and 64
hand-written alternative labels next to unlabeled rules. What breaks?

**Short answer.** Two auto-labeling defects, both fixed in core with regression
tests. With them, every example program the grammar itself can parse (40 of
41) and all 22 snippets round-trip byte-exact; the one failing example is
invalid input on purpose. Both grammar files are used byte for byte; the two
base classes only got a `package` line. Everything the grammar rejects, plain
ANTLR rejects too.

| Stage | Snippets exact | Examples exact / mismatch / parse failure |
|---|---|---|
| first build | generation fails (16 × ANTLR error 70) | — |
| labels avoid token names | 20 / 22 (2 grammar limits) | 33 / 7 / 1 |
| per-item lifting of repeated literals | 20 / 22 (2 grammar limits) | 40 / 0 / 1 |
| snippets the grammar accepts | 22 / 22 | 40 / 0 / 1 |

## Setup

- Grammar: grammars-v4 [`javascript/javascript`](https://github.com/antlr/grammars-v4/tree/master/javascript/javascript)
  at commit `7aa2e6b3ea` (2026-10-03; Bart Kiers, Ivan Kochurkin and
  contributors, MIT): `JavaScriptLexer.g4` (`superClass =
  JavaScriptLexerBase`, `channels { ERROR }`, mode `TEMPLATE`) and
  `JavaScriptParser.g4` (`tokenVocab = JavaScriptLexer`, `superClass =
  JavaScriptParserBase`). 85 parser rules, 132 lexer rules plus 20 fragments,
  16 parser and 13 lexer predicates, 64 hand-written alternative labels.
- Both grammar files are byte-identical to that commit
  ([`src/main/vmf-text/jsheadtrial`](src/main/vmf-text/jsheadtrial)).
  `program` is the first parser rule, so no VMF-Text metadata is needed;
  `autoLabel = true` comes from `build.gradle`.
- [`JavaScriptLexerBase.java`](src/main/java/jsheadtrial/parser/JavaScriptLexerBase.java)
  and [`JavaScriptParserBase.java`](src/main/java/jsheadtrial/parser/JavaScriptParserBase.java):
  the grammars-v4 Java files with a `package jsheadtrial.parser;` line, the
  package VMF-Text generates the lexer and parser into. One copy of the parser
  base suffices since [#62](https://github.com/miho/VMF-Text/issues/62).
  Unlike the Python lexer base, nothing else needed changing: these lexer
  methods keep ANTLR's exact token ranges.
- Examples: the grammar's 41 `examples/*.js` at the same commit (45 KB); not
  committed, `fetch-samples.sh` downloads them into `samples/`. `3728.js` is
  an intentionally invalid input (it ships with a `.errors` file).
- Drivers: `jsheadtrial.Main` (examples), `jsheadtrial.Snip`
  (`snippets.txt`, 22 ES2015+ shapes: classes with fields and accessors,
  arrow functions, nested template literals, destructuring, spread,
  async/await, generators, optional chaining, modules, regex vs division,
  ASI, comments, BigInt, labels, `new` without arguments, `"use strict"`).

```bash
sh ./fetch-samples.sh
sh ./gradlew run                                                  # the examples
sh ./gradlew run -PmainClass=jsheadtrial.Snip --args=snippets.txt  # isolated shapes
```

## Numbers

| Measure | Result |
|---|---|
| Parser rules / lexer rules + fragments / predicates (parser + lexer) | 85 / 132 + 20 / 16 + 13 |
| Generated Java files | 1623 |
| Generate + compile (`gradlew clean build -x test`, alone) | ~99 s |
| Auto-labeled elements (of which alternative classes) | 496 (155) |
| Lifted blocks: choices / per-item | 8 / 1 |
| Examples (41): exact / mismatch / parse failure | 40 / 0 / 1 (invalid input) |
| Snippets exact | 22 / 22 |
| Parse / unparse time, 45 KB (incl. warm-up) | ~2.3 s / ~0.2 s |

## Findings

1. **A label may not equal a token name (fixed).** Generation failed with
   16 × `error(70): label Identifier conflicts with token with same name`.
   Auto-labeling turns a label that equals a parser rule name into PascalCase
   (`identifier` → `Identifier`, ANTLR error 69 otherwise), and this grammar
   has the parser rule `identifier` *and* the token `Identifier` (likewise
   `templateStringAtom` / `TemplateStringAtom`). Neither spelling can be a
   label then. Auto-labeling now also collects token names (lexer rules and
   `tokens {}` entries, from the lexer grammar of a split pair too) and falls
   back to a numeric suffix (`identifier2`), the existing policy for taken
   names; a keyword-context name still wins where there is one. Core tests in
   `AutoLabelerTest` (the ANTLR tool accepts the result).
2. **A repeated literal inside a repeated block lost its iteration (fixed).**
   7 examples came back with array literals mangled: `[ 1, 2, 3 ]` as
   `[ 1,, 2 ]`, `[ n1, n2, n3, ...others ]` as `[ n1,,, n2 ]`. The rule is
   `elementList: ','* arrayElement? (','+ arrayElement)* ','*` — "everything
   is optional", including elisions such as `[1,,2]`. The `','+` inside the
   repeated block became one comma list shared by all iterations, and the
   unparser's first `','+` took every comma, starving the later elements.
   This is [#54](https://github.com/miho/VMF-Text/issues/54) (Python's default
   arguments) with a literal instead of an optional property: per-item lifting
   now also applies when a repeated block contains a `+`/`*`-repeated literal,
   so `elementList` gets `elementListItems+=elementListItem*` and each item
   its own commas. Regression grammar `commalift`. The other trials are
   unchanged by this (Python 28/28, C 27/32, ECMAScript 2/3 + invalid input).
3. **Everything else worked without changes**: the split grammar with both
   base classes; lexer modes (template literals, also nested ones);
   the custom `ERROR` channel; lexer predicates deciding `let` and regular
   expressions versus division; parser predicates — the unparser's check rules
   carry none since [#48](https://github.com/miho/VMF-Text/issues/48), the
   model parser keeps them; hand-written alternative labels next to inferred
   ones; ASI through `eos`; comments and white-space on the hidden channel.
4. **Grammar limitations, not VMF-Text's (checked with plain ANTLR).** The
   grammar has `??=` but no `||=` / `&&=` token, and it rejects two `yield`
   statements separated by `;` on one line (`yield 1; yield 2;`; with a line
   break in between they parse). A plain ANTLR parser generated from the same
   files fails on exactly these inputs and on `3728.js`; the snippet file uses
   accepted forms instead.
5. **Naming (cosmetic, open).** The numeric fallback of finding 1 lands on the
   most common node: 15 × `identifier2`, among them the token inside the
   `identifier` rule itself (`Identifier.getIdentifier2()`), plus
   `identifier3` and `templateStringAtom2`. A better fallback for a rule/token
   name clash (e.g. `identifierToken` for the token reference) would be a
   small naming change; it is not made here because it renames properties.

## What was done with this

1. ✅ Labels never equal a token name (finding 1): core `AutoLabeler`, tests in
   `AutoLabelerTest`.
2. ✅ Per-item lifting of repeated blocks with a repeated literal (finding 2):
   core `AutoLabeler`, regression grammar `commalift`.
3. ⏳ Naming of rule/token name clashes (finding 5).
