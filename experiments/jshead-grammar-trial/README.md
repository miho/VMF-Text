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
tests. With them, all 41 example programs pass and all 22 snippets
round-trip byte-exact: 40 examples round-trip byte-exact, and the 41st,
`3728.js`, is a negative test — it ships with `3728.js.errors` and fails with
exactly the error listed there. Both grammar files are used byte for byte; the
two base classes only got a `package` line. Everything the grammar rejects,
plain ANTLR rejects too.

| Stage | Snippets exact | Examples passed / failed (41 tests) |
|---|---|---|
| first build | generation fails (16 × ANTLR error 70) | — |
| labels avoid token names | 20 / 22 (2 grammar limits) | 34 / 7 |
| per-item lifting of repeated literals | 20 / 22 (2 grammar limits) | 41 / 0 |
| snippets the grammar accepts | 22 / 22 | 41 / 0 |
| names by kind for rule/token clashes, counted per label (#73) | 22 / 22 | 41 / 0 |

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
  committed, `fetch-samples.sh` downloads them, with the `.errors` file of the
  negative test, into `samples/`. `3728.js` (`(||`) is a negative test: its
  `3728.js.errors` expects `line 1:1 no viable alternative at input '(||'`.
- Drivers: `jsheadtrial.Main` (examples; every sample is a test case: it passes when it round-trips byte-exact, or — a negative test with a `<name>.errors` file next to it, the grammars-v4 convention — when parsing fails with exactly the errors listed there; the summary reads `passed x/n`), `jsheadtrial.Snip`
  (`snippets.txt`, 22 ES2015+ shapes: classes with fields and accessors,
  arrow functions, nested template literals, destructuring, spread,
  async/await, generators, optional chaining, modules, regex vs division,
  ASI, comments, BigInt, labels, `new` without arguments, `"use strict"`),
  `jsheadtrial.Build` (the model in code: edits a parsed `continue outer`,
  builds `stop: break stop;` with the builders, lists properties through VMF
  reflection).

```bash
sh ./fetch-samples.sh
sh ./gradlew run                                                  # the examples
sh ./gradlew run -PmainClass=jsheadtrial.Snip --args=snippets.txt  # isolated shapes
sh ./gradlew run -PmainClass=jsheadtrial.Build                     # the model in code
```

## Numbers

| Measure | Result |
|---|---|
| Parser rules / lexer rules + fragments / predicates (parser + lexer) | 85 / 132 + 20 / 16 + 13 |
| Generated Java files | 1623 |
| Generate + compile (`gradlew clean build -x test`, alone) | ~99 s |
| Auto-labeled elements (of which alternative classes) | 496 (155) |
| Lifted blocks: choices / per-item | 8 / 1 |
| Numbered element names (all several references in one type) | 8 |
| Examples (41 tests): passed / failed | 41 / 0 (40 byte-exact round trips, 1 expected syntax error) |
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
   `tokens {}` entries, from the lexer grammar of a split pair too); a
   keyword-context name still wins where there is one, else the name says what
   the property holds (finding 5). Core tests in `AutoLabelerTest` (the ANTLR
   tool accepts the result).
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
5. **The names are the model's API in code (fixed, [#73](https://github.com/miho/VMF-Text/issues/73)).**
   The first fallback for finding 1 was a numeric suffix, and
   `jsheadtrial.Build` showed what that means for code that works with the
   model: `ContinueStatement.getIdentifier2()` returned the `Identifier` node,
   `IdentifierAlt1.getIdentifier2()` its text, and building `stop: break stop;`
   read `withIdentifier2(IdentifierAlt1.newBuilder().withIdentifier2("stop")…)`.
   The names now say what the property holds: a reference to the rule is
   `identifierNode`, a reference to the token `identifierToken` (14 ×
   `identifierNode`, 2 × `identifierToken`, 1 × `templateStringAtomToken`);
   VMF reflection lists `identifierToken : String (token Identifier)` through
   the `vmf-text:token` annotation, so code can also find a token's property
   without knowing its name. The same check exposed a second cause of
   unreadable names: `singleExpression` repeats the label `# NewExpression`
   three times, which made auto-labeling count names across all 48
   alternatives — `AdditiveExpression.getSingleExpression15()` /
   `getSingleExpression16()`, no `left` / `right`, an `identifierNode2` in a
   class with one identifier. Names are counted per label now: binary
   expressions read `getLeft()` / `getRight()`, and the numbered names fell
   from 38 to 8, each one several references in one type (the ternary's three
   operands, `for (init; condition; update)`, a computed property's key and
   value, the two backticks of a template literal) or the grammar's own token
   `OctalIntegerLiteral2`. The built program unparses as ` stop : break stop ;`
   (conservative separators) and re-parses byte-exact. Regression grammar
   `nameclash` builds such a model with the builders.

## What was done with this

1. ✅ Labels never equal a token name (finding 1): core `AutoLabeler`, tests in
   `AutoLabelerTest`.
2. ✅ Per-item lifting of repeated blocks with a repeated literal (finding 2):
   core `AutoLabeler`, regression grammar `commalift`.
3. ✅ Names for rule/token clashes by what they hold (`identifierNode`,
   `identifierToken`) and names counted per alternative label (finding 5,
   [#73](https://github.com/miho/VMF-Text/issues/73)): core `AutoLabeler`,
   tests in `AutoLabelerTest`, regression grammar `nameclash`.
