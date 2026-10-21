# Experiment: an unlabeled grammars-v4 ECMAScript grammar through VMF-Text

*Run 2026-10-20 against VMF-Text `0.2.4-SNAPSHOT` (unreleased `main` after the
C-trial fixes and mixed-mode formatting), VMF 0.2.11, ANTLR 4.13.2, JDK 21;
re-run 2026-10-21 against `0.2.6-SNAPSHOT` (VMF `0.2.12-SNAPSHOT`) with the
same results. Second trial language after [`../c-grammar-trial`](../c-grammar-trial/README.md).*

**Question.** The C trial fixed the grammar *shapes* of a predicate-free
grammar. What breaks on a grammar that leans on everything C did not use:
semantic predicates (23 of them), `@parser::members` and `@lexer::members`,
keywords defined as lexer rules and referenced by name (`Var`, `For`),
regular-expression literals decided in the lexer, and automatic semicolon
insertion (ASI: an `eos` rule that may match `EOF` or nothing at all)?

**Short answer.** Two defects, both in core, both fixed the same day and
covered by the regression grammar `asi` in the test-suite. Everything else
went through unaided: 55 parser rules and 103 lexer rules became 991 generated
Java files in 108 s, and after the fixes all 15 snippets round-trip
byte-exact and all 3 samples pass: two round-trip byte-exact, the third,
`3728.js`, is a negative test (it ships with `3728.js.errors`) and fails with
exactly the expected syntax error.

1. **Semantic predicates were turned into optional actions.** The
   optional-wrapping rewrite took `{...}?` for an optional element and
   produced `({...} {flag})?`. For this grammar the generated unparser grammar
   did not even compile (`not a statement` at `!here(LineTerminator)`); in a
   grammar where it does compile, the predicate silently stops guarding its
   alternative. The Java24 test grammar turned out to be such a case: its
   two predicates were written as `{ this.X(); }?` statements, which only
   compiled *because* of the wrapping — corrected along with the fix.
2. **The trailing newline was lost after an ASI statement.** `x = 1\ny = 2\n`
   came back without its last newline. Once `EOF` is matched, ANTLR makes it
   the stop token of every enclosing rule, so the hidden text before the end
   of input is recorded in the trivia slot of the nested `eos` element
   (`eos: SemiColon | EOF | {lineTerminatorAhead()}? | ...`) — and the
   generator skipped `EOF` references entirely. `EOF` now goes through a
   `Formatter.eof(...)` hook that emits the recorded hidden text for parsed
   elements and nothing for programmatic ones.

| Stage | Snippets exact | Samples passed / failed (3 tests) |
|---|---|---|
| first build | unparser grammar does not compile | — |
| predicates left alone | 13 / 15 | 3 / 0 |
| `EOF` slot emitted | 15 / 15 | 3 / 0 |

## Setup

- Grammar: grammars-v4
  [`javascript/ecmascript/ECMAScript.g4`](https://github.com/antlr/grammars-v4/blob/master/javascript/ecmascript/ECMAScript.g4)
  (Bart Kiers 2014, MIT; ECMAScript 5.1; `master` as of 2026-10-20). A
  combined grammar with 55 parser rules, 103 lexer rules, 23 semantic
  predicates, `@parser::members` (ASI helpers `here()` and
  `lineTerminatorAhead()`) and `@lexer::members` (the previous token decides
  whether `/` starts a regular-expression literal). Copied to
  [`src/main/vmf-text/jstrial/ECMAScript.g4`](src/main/vmf-text/jstrial/ECMAScript.g4)
  unchanged except for the trailing metadata comment `AutoLabel(enabled=true)`
  and `Root(rule=program)`.
- Why not grammars-v4's maintained `javascript/javascript` (or TypeScript)
  grammar: those are split into lexer + parser grammars (`tokenVocab`) with
  `superClass` base classes for both — the shape tracked in
  [#39](https://github.com/miho/VMF-Text/issues/39).
- Samples: the three `*.js` files from `grammars-v4/javascript/ecmascript/examples`
  (`helloworld.js`, `nn.js`, `3728.js`; mixed licenses, not committed —
  `fetch-samples.sh` downloads them, with the `.errors` file of the negative
  test, into `samples/`). `3728.js` is `(||`: an ANTLR issue reproducer, invalid
  ECMAScript, shipped with `3728.js.errors` (`line 1:1 mismatched input '||'
  expecting {…}`); it fails with exactly that error.
- Drivers: `jstrial.Main` parses + unparses every sample and reports
  byte-exactness and timings (every sample is a test case: it passes when it round-trips byte-exact, or — a negative test with a `<name>.errors` file next to it, the grammars-v4 convention — when parsing fails with exactly the errors listed there; the summary reads `passed x/n`); `jstrial.Snip` runs the 15 ES5 snippets in
  `snippets.txt` (statements, literals, regex vs division, ASI, comments,
  labels, `try`/`catch`/`finally`).

```bash
sh ./fetch-samples.sh
sh ./gradlew run                                              # the samples
sh ./gradlew run -PmainClass=jstrial.Snip --args=snippets.txt # isolated shapes
```

The project resolves the plugin from `mavenLocal()` first, so a
`publishToMavenLocal` of `core` and `gradle-plugin` is picked up directly.

## Numbers

| Measure | Result |
|---|---|
| Parser rules / lexer rules / predicates / named actions | 55 / 103 / 23 / 2 |
| Generated Java files | 991 |
| Generate + compile (`gradlew clean build`) | 108 s |
| Auto-labeled elements (of which alternative classes) | 373 (92) |
| Numeric-suffix names / PascalCase collision-avoidance names | 21 / 160 |
| Samples (3 tests): passed / failed | 3 / 0 (2 byte-exact round trips, 1 expected syntax error) |
| Snippets exact | 15 / 15 |
| Parse / unparse time, 689 chars (incl. JIT and ATN warm-up) | 369 ms / 54 ms |

## Findings

1. **Predicates were wrapped as optionals (fixed).** `rewriteGrammarWrapOptionals`
   treats every element ending in `?` as optional; `{...}?` ends in `?`. The
   state-injection pass likewise gave the predicate a presence flag. Both
   passes now skip actions and predicates. `@parser::members` reach the
   unparser grammar through its `import`, so the predicate compiles there
   too — the earlier C-trial assumption that "predicate/action pass-through"
   would be needed first was wrong: combined grammars with members already
   work once the rewrites leave the predicates alone.
2. **Nested `EOF` owns the trailing hidden text (fixed).** With `eos: ... |
   EOF | ...` the last statement's `eos` element records the final newline
   in its `EOF` trivia slot and the root's own `EOF` slot is empty (the
   listener's previous-token index has already advanced to `EOF`). The
   generated unparser ignored `EOF` elements, so only the root's slot — the
   one that happened to be empty here — was ever emitted (by the formatter's
   root `done()`). Every `EOF` reference is now rendered through the new
   `Formatter.eof(...)` hook; the root's `done()` fallback stays for grammars
   whose root does not end in `EOF`.
3. **Keyword lexer rules become noise properties**
   ([#47](https://github.com/miho/VMF-Text/issues/47)). Every keyword is a
   lexer rule referenced by name; auto-labeling cannot tell `Var` from
   `Identifier` and captures it: `var=Var`, `forValue=For`, `ifValue=If`,
   `returnValue=Return`, `typeof=Typeof`, `in=In` … 26 properties whose
   value is always the keyword. They round-trip, but clutter the API. A
   single-literal lexer rule should be treated like the literal itself.
4. **Repeated operands get positional suffixes** ([#47](https://github.com/miho/VMF-Text/issues/47)).
   `singleExpression '+' singleExpression` → `SingleExpression` /
   `SingleExpression2`; `if … else` → `Statement` / `Statement2`; 21 numeric
   suffixes overall. Correct, but not the `left`/`right` or `elseStatement` a
   hand-labeled grammar would use.
5. **Predicates in the generated check rules see only the fragment**
   ([#48](https://github.com/miho/VMF-Text/issues/48)). The unparser grammar
   copies alternatives *with* their predicates (18 of 23 land there) and
   validates emitted text alternative by alternative. `here(LineTerminator)`
   inspects the previous token, which a fragment does not have. No wrong
   result in this trial (ANTLR only consults predicates when alternatives are
   otherwise ambiguous, and alternative classes skip the check), but a latent
   risk for predicate-heavy grammars.
6. **Lexer members and the regex/division ambiguity work unchanged.**
   `var r = /ab+c/g;` and `var q = a / b / c;` both round-trip; the lexer
   code is used as-is by the generated parser and, via the import, by the
   unparser grammar's lexer.
7. **Fidelity is again not the problem.** Comments, blank lines and
   indentation in the parseable samples survive; the only mismatch of the
   whole trial was the `EOF` slot of finding 2.
8. **`tokenVocab` remains the gate to grammars-v4 HEAD**
   ([#39](https://github.com/miho/VMF-Text/issues/39)), exactly as the C trial
   concluded: the maintained JavaScript and TypeScript grammars are split and
   carry base classes.

## What was done with this

1. ✅ Predicates and actions are left alone by the optional-wrapping and
   state-injection passes (finding 1); regression grammar `asi`.
2. ✅ `Formatter.eof(...)`: the `EOF` trivia slot is emitted wherever the
   `EOF` reference sits (finding 2); regression grammar `asi`. The Java24 test
   grammar's `{ this.X(); }?` predicates were corrected to `{ this.X() }?`.
3. ⏳ Keyword lexer rules as literals and `left`/`right` naming (findings 3–4,
   [#47](https://github.com/miho/VMF-Text/issues/47)) — naming quality, no
   effect on fidelity.
4. ⏳ Predicate-free check rules (finding 5,
   [#48](https://github.com/miho/VMF-Text/issues/48)).
5. ⏳ `tokenVocab` / split grammars (finding 8,
   [#39](https://github.com/miho/VMF-Text/issues/39)).
