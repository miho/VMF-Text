# Experiment: the grammars-v4 Python 3 grammar through VMF-Text

*Run 2026-10-20 against VMF-Text `0.2.5-SNAPSHOT` (unreleased `main` after
v0.2.4 plus the fixes made during this trial), VMF 0.2.11, ANTLR 4.13.2,
JDK 21; re-run 2026-10-21 against `0.2.6-SNAPSHOT` (VMF `0.2.12-SNAPSHOT`)
with the grammars-v4 lexer grammar byte for byte (#61). Third trial language
after [`../c-grammar-trial`](../c-grammar-trial/README.md)
and [`../js-grammar-trial`](../js-grammar-trial/README.md); requested as
"add Python 3 support".*

**Question.** Python is the first language whose structure is not in the
tokens: indentation becomes `INDENT` / `DEDENT` tokens synthesized by a
lexer base class, line ends are significant (`NEWLINE`) except inside
brackets, and the grammars-v4 grammar is split into lexer and parser
grammars with base classes. What does VMF-Text need to round-trip real
Python byte-exact, and what is left for programmatic editing?

**Short answer.** Round-tripping works once four core defects were fixed;
since [#39](https://github.com/miho/VMF-Text/issues/39) the grammars-v4 files
are used as they are (lexer grammar + parser grammar), with an adapted lexer
base class. All 28 sample files
that ship with the grammar (CPython's `asyncio` package, 389 KB) and all 23
snippets come back byte-exact; parsing never fails. Every mismatch
on the way was a VMF-Text defect, none a limitation of the approach. For
programmatic editing, a moved statement keeps its own trivia and would land
at column 0; `Python3TriviaSupport.reindent(statement, oldIndent,
newIndent)` rebases it (finding 7). Blocks built from scratch get their
`NEWLINE`/`INDENT`/`DEDENT` values from
`Python3TriviaSupport.fillIndentationTokens(root, indentUnit)` and their
statements placed by `Formatter.newIndentationTokenFormatter()`
([#60](https://github.com/miho/VMF-Text/issues/60), `py3trial.Build`).

| Stage | Snippets exact | asyncio sources exact |
|---|---|---|
| first build | generation fails (`tokens {}` block, VMF list equals) | — |
| grammar reads, model generates | 3 / 13 (absent optional blocks emitted, `<EOF>` text) | — |
| optional blocks state-driven (#52) | 13 / 13 | 12 / 28 |
| labeled-alternative state paths (#53) | 13 / 13 | 14 / 28 |
| per-item lifting of repeated blocks (#54) | 13 / 13 | 28 / 28 |

## Setup

- Grammar: grammars-v4 [`python/python3`](https://github.com/antlr/grammars-v4/tree/master/python/python3)
  (Bart Kiers, MIT; `master` as of 2026-10-20): `Python3Lexer.g4` with
  `tokens { INDENT, DEDENT }` and `superClass = Python3LexerBase`,
  `Python3Parser.g4` with `superClass = Python3ParserBase` and
  `tokenVocab = Python3Lexer`; 119 parser rules, 100 lexer rules, 5
  semantic predicates.
- Used as the split pair they are
  ([`src/main/vmf-text/py3trial/Python3Lexer.g4`](src/main/vmf-text/py3trial/Python3Lexer.g4),
  [`Python3Parser.g4`](src/main/vmf-text/py3trial/Python3Parser.g4)): the
  parser grammar is the input, the lexer comes in through `tokenVocab`
  ([#39](https://github.com/miho/VMF-Text/issues/39)). The lexer grammar is
  the grammars-v4 file byte for byte (`SKIP_ ... -> skip` included: since
  [#61](https://github.com/miho/VMF-Text/issues/61) VMF-Text takes the text
  between two tokens from the input by position, so skipped white-space,
  comments and line joining come back). Adaptations:
  - [`Python3LexerBase.java`](src/main/java/py3trial/parser/Python3LexerBase.java)
    (the `superClass` of the lexer, in the generated lexer's package) gives
    its synthesized tokens **exact character ranges** (`NEWLINE` = the
    line-break characters, `INDENT` = the indentation, `DEDENT` = empty right
    after the line break, the virtual `NEWLINE`/`DEDENT` of an unterminated
    file = empty at the end of input — the upstream `commonToken()` derives
    ranges from the text length and the current position, which puts
    `NEWLINE` on the indentation). It skips exactly what upstream skips.
    Before #61 it also had to emit the skipped text as hidden-channel tokens
    and the `SKIP_` rule went to the hidden channel;
  - `Python3ParserBase.java` (two constant predicates) in the parser package
    (one copy since [#62](https://github.com/miho/VMF-Text/issues/62): the
    unparser's check grammar extends it there as well);
  - metadata `AutoLabel(enabled=true)`, `Root(rule=file_input)` appended to
    the parser grammar.
  The first run used a hand-combined grammar instead
  ([`combine-python3.py`](combine-python3.py), kept for reference); the
  combined route additionally had to refer to `INDENT`/`DEDENT` through the
  parser class, because ANTLR does not copy a combined grammar's `tokens {}`
  block into the implicit lexer.
- Samples: the grammar's test inputs `small/` and `large/` (28 distinct
  files, CPython's `asyncio` package, 389 KB); not committed,
  `fetch-samples.sh` downloads them into `samples/`.
- Drivers: `py3trial.Main` (samples; every sample is a test case: it passes when it round-trips byte-exact, or — a negative test with a `<name>.errors` file next to it, the grammars-v4 convention — when parsing fails with exactly the errors listed there; the summary reads `passed x/n`; the Python samples have
  no negative tests), `py3trial.Snip` (`snippets.txt`, 13
  indentation / comment / bracket cases; `snippets2.txt`, 10 trailing-comma
  and default-argument cases), `py3trial.Edit` (moves a statement into a
  function body), `py3trial.Build` (builds a function from scratch and nests
  one into a parsed class body).

```bash
sh ./fetch-samples.sh
sh ./gradlew run                                               # all samples
sh ./gradlew run -PmainClass=py3trial.Snip --args=snippets.txt  # isolated shapes
sh ./gradlew run -PmainClass=py3trial.Edit                      # programmatic edit
sh ./gradlew run -PmainClass=py3trial.Build                     # programmatic blocks
```

## Numbers

| Measure | Result |
|---|---|
| Parser rules / lexer rules / predicates | 119 / 100 / 5 |
| Generated Java files | 1700 |
| Generate + compile (`gradlew clean build`) | ~100 s |
| Auto-labeled elements (of which alternative classes) | 505 (122) |
| Lifted blocks: choices / per-item | 22 / 4 |
| Samples (28): exact / mismatch / failures | 28 / 0 / 0 |
| Snippets exact | 23 / 23 |
| Parse / unparse time, 392 KB (incl. warm-up) | ~4.3 s / ~0.3 s |

## Findings

1. **`tokens {}` before the first rule broke grammar reading (fixed).**
   VMF-Text's bundled ANTLR grammar lexer had no handling for prequel
   constructs; `INDENT` inside `tokens {}` left it in "lexer rule" state and
   the first rule's injected `locals [...]` was read as a character set.
   Ported the upstream adaptor logic; core tests `LexerAdaptorPrequelTest`,
   `TokensBlockGenerationTest`.
2. **VMF's structural `equals` assumes equal list sizes (worked around).**
   `removeAll` over the rule list compared every pair of rules; the first
   pair of alternatives with element counts 2 vs 1 threw
   `IndexOutOfBoundsException` out of `equals`. The generator now removes by
   identity; the VMF bug is [miho/VMF#87](https://github.com/miho/VMF/issues/87).
3. **An optional block whose only named element is optional was always
   emitted (fixed, [#52](https://github.com/miho/VMF-Text/issues/52)).**
   `('(' arglist? ')')?` made `class A:` into `class A( ):`, the
   trailing-comma block of `typedargslist` made `def f(a, b):` into
   `def f(a, b,):`. Only a mandatory named element may stand in for the
   presence of its block. Regression grammar `optblock`.
4. **Optional literals inside a second or later labeled alternative were
   dropped (fixed, [#53](https://github.com/miho/VMF-Text/issues/53)).**
   `(value,)` became `(value)`, `[1, 2,]` became `[1, 2]`: the parser
   recorded the comma's state under `/r134/a1/…`, the unparser asked for
   `/r134/a0/…`. Affects hand-labeled grammars as well. Regression grammar
   `labeledopt`.
5. **An optional property inside a repeated block lost its iteration
   (fixed, [#54](https://github.com/miho/VMF-Text/issues/54)).**
   `(',' tfpdef ('=' test)?)*` as parallel lists put the defaults on the
   first parameters: `def f(self, sock, protocol, waiter=None)` came back
   as `def f(self, sock=None, protocol, waiter)` — 14 of the 28 asyncio
   files. Auto-labeling now lifts such blocks into per-item sub classes
   (`typedargslistChoiceItems+=typedargslistChoiceItem*`), which is also the
   better API. Regression grammar `itemlift`.
6. **Synthesized tokens are ordinary tokens to VMF-Text.** `NEWLINE`,
   `INDENT` (text = the indentation) and `DEDENT` (empty) become string
   properties (`BlockAlt2.getNewline()/getIndent()/getDedent()`); with exact
   ranges, indentation, blank lines, comments and newlines inside brackets
   all survive (first through a lossless hidden channel, since #61 from the
   input positions, with the upstream `-> skip`). The only lexer-side surprise:
   `CommonToken` reads an empty range at the end of input as `<EOF>`, so
   empty tokens set their text explicitly.
7. **Moved statements keep their trivia (utility added).** `py3trial.Edit`
   moves `x = 2` into `def f(): return 1`: the moved statement keeps its
   trivia and prints at column 0, with the default and with the block-indent
   formatter alike — in Python that changes the meaning. The generated
   `<Grammar>TriviaSupport.reindent(element, oldIndent, newIndent)` rebases a
   parsed subtree: the first line's leading whitespace, every inner line
   start that begins with the old indentation, and whitespace-only token
   values (the `INDENT` tokens of nested blocks); the driver shows the
   corrected output.
   **Blocks built from scratch** ([#60](https://github.com/miho/VMF-Text/issues/60),
   `py3trial.Build`): a programmatic `BlockAlt2` without its `NEWLINE` is
   rejected by the unparser ("Language model is invalid" — a block without
   its line break is not a block), and the caller had to set all three token
   values. `Python3TriviaSupport.fillIndentationTokens(root, "    ")` now
   sets every unset one by nesting depth (the model knows which property
   holds which token through the `vmf-text:token` annotation): a `NEWLINE`
   becomes the line break, a `DEDENT` the empty string, an `INDENT` the
   enclosing block's indentation plus one unit — so a function nested into a
   parsed class body gets eight spaces, a top-level one four. With
   `Formatter.newIndentationTokenFormatter()` the statements inside are placed
   at that depth; the driver builds `def f(): pass; pass`, appends it to a
   parsed module, appends `pass` and `def g(): pass` to a parsed class body
   (ten token values filled), and the output re-parses byte-exact. On the way
   the driver exposed a general defect: the trailing `';'?` of
   `simple_stmts` was printed for every programmatic statement (`pass;`),
   because an element without recorded optional states printed all optional
   constructs. Optional punctuation nothing in the model asks for is now left
   out (core change, regression grammar `optblock`).
8. **Naming (improved under [#47](https://github.com/miho/VMF-Text/issues/47)).**
   Property and synthetic rule names derived from snake_case rules are
   camelCase now (`getFileInputChoices()`, `FileInputChoiceAlt2`), and since
   [#63](https://github.com/miho/VMF-Text/issues/63) the rule itself is
   renamed before any other pass, so its model class is `FileInput` /
   `SimpleStmts` too (the ANTLR parser then says `fileInput()`; the trailing
   underscore of `subscript_` stays). The 20 operand pairs of `expr op expr`
   shapes are `left` / `right`, keyword tokens such as `async` are no longer
   properties, and nested lifting no longer stacks suffixes
   (`dictorsetmakerChoice2` instead of `dictorsetmakerChoiceChoice`).
9. **The split grammar is the real entry point (supported since
   [#39](https://github.com/miho/VMF-Text/issues/39)).** Everything above was
   done on a hand-combined grammar. VMF-Text now processes a `parser grammar`
   with `tokenVocab` and the `lexer grammar` next to it as one unit, so the
   grammars-v4 files can be used as they are — the lexer grammar byte for
   byte since VMF-Text derives trivia from character positions
   ([#61](https://github.com/miho/VMF-Text/issues/61)); only the lexer base
   needs exact token ranges (see *Setup*).

## What was done with this

1. ✅ Grammar lexer handles `tokens {}` / `channels {}` / named actions
   before the first rule (finding 1).
2. ✅ Identity instead of structural equality in the generator (finding 2);
   VMF issue filed.
3. ✅ Optional blocks with only optional properties stay state-driven
   (finding 3, #52).
4. ✅ Labeled-alternative state paths agree between parser and unparser
   (finding 4, #53).
5. ✅ Per-item lifting of repeated blocks with optional properties
   (finding 5, #54).
6. ✅ `reindent(...)` for moved / inserted parsed subtrees (finding 7).
7. ✅ Blocks built from scratch: `fillIndentationTokens(...)` +
   `Formatter.newIndentationTokenFormatter()` (finding 7, #60); optional
   punctuation is no longer printed for programmatic elements (`pass;`).
8. ✅ Naming (#47): keyword tokens, `left`/`right`, camelCase for snake_case.
9. ✅ Split grammars (#39) in core, and this trial runs on the grammars-v4
   files as they are: the lexer grammar byte for byte (#61, trivia from
   character positions), the lexer base class with exact token ranges.
