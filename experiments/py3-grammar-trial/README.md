# Experiment: the grammars-v4 Python 3 grammar through VMF-Text

*Run 2026-10-20 against VMF-Text `0.2.5-SNAPSHOT` (unreleased `main` after
v0.2.4 plus the fixes made during this trial), VMF 0.2.11, ANTLR 4.13.2,
JDK 21. Third trial language after [`../c-grammar-trial`](../c-grammar-trial/README.md)
and [`../js-grammar-trial`](../js-grammar-trial/README.md); requested as
"add Python 3 support".*

**Question.** Python is the first language whose structure is not in the
tokens: indentation becomes `INDENT` / `DEDENT` tokens synthesized by a
lexer base class, line ends are significant (`NEWLINE`) except inside
brackets, and the grammars-v4 grammar is split into lexer and parser
grammars with base classes. What does VMF-Text need to round-trip real
Python byte-exact, and what is left for programmatic editing?

**Short answer.** Round-tripping works once four core defects were fixed and
the grammar was combined by hand (the split form needs
[#39](https://github.com/miho/VMF-Text/issues/39)). All 28 sample files
that ship with the grammar (CPython's `asyncio` package, 389 KB) and all 23
snippets come back byte-exact; parsing never fails. Every mismatch
on the way was a VMF-Text defect, none a limitation of the approach. For
programmatic editing, a moved statement keeps its own trivia and would land
at column 0; `Python3TriviaSupport.reindent(statement, oldIndent,
newIndent)` rebases it (finding 7). Fully programmatic Python blocks still
need their `NEWLINE`/`INDENT`/`DEDENT` values set by the caller.

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
- Combined by hand into [`src/main/vmf-text/py3trial/Python3.g4`](src/main/vmf-text/py3trial/Python3.g4)
  by [`combine-python3.py`](combine-python3.py) (run it against a checkout
  of the two grammar files and the Java base class), because a combined
  grammar is what VMF-Text consumes today:
  - the parser base (two constant predicates) and the lexer base are
    inlined as `@parser::members` / `@lexer::members`;
  - the inlined lexer base gives its synthesized tokens **exact character
    ranges** (`NEWLINE` = the line-break characters, `INDENT` = the
    indentation, `DEDENT` = empty, the virtual `NEWLINE`/`DEDENT` of an
    unterminated file = empty at the end of input) — the upstream
    `commonToken()` derives bogus ranges from the text length, which
    lexical preservation cannot live with;
  - text the upstream lexer *skips* (`SKIP_`: whitespace, comments, line
    joining) goes to the hidden channel, and so does what `onNewLine()`
    used to `skip()`: blank lines, newlines inside brackets, same-level
    indentation and the remainder after `DEDENT`s. The token stream is
    lossless;
  - `INDENT` / `DEDENT` are referenced as `Python3Parser.INDENT` etc.:
    ANTLR does not copy a combined grammar's `tokens {}` block into the
    implicit lexer;
  - metadata `AutoLabel(enabled=true)`, `Root(rule=file_input)`.
- Samples: the grammar's test inputs `small/` and `large/` (28 distinct
  files, CPython's `asyncio` package, 389 KB); not committed,
  `fetch-samples.sh` downloads them into `samples/`.
- Drivers: `py3trial.Main` (samples), `py3trial.Snip` (`snippets.txt`, 13
  indentation / comment / bracket cases; `snippets2.txt`, 10 trailing-comma
  and default-argument cases), `py3trial.Edit` (moves a statement into a
  function body).

```bash
sh ./fetch-samples.sh
sh ./gradlew run                                               # all samples
sh ./gradlew run -PmainClass=py3trial.Snip --args=snippets.txt  # isolated shapes
sh ./gradlew run -PmainClass=py3trial.Edit                      # programmatic edit
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
   ranges and a lossless hidden channel, indentation, blank lines, comments
   and newlines inside brackets all survive. The only lexer-side surprise:
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
   corrected output. Still open: a Python-aware `ProgrammaticSeparatorPolicy`
   that synthesizes `NEWLINE`/`INDENT`/`DEDENT` text for blocks built
   programmatically (today the caller sets those three properties).
8. **Naming.** Rule names keep their underscores (`File_input`,
   `Simple_stmts`, `getFile_inputChoices()`), nested lifting yields
   `dictorsetmakerChoiceChoice4`, and 20 positional suffixes appear — all
   cosmetic, filed under [#47](https://github.com/miho/VMF-Text/issues/47).
9. **The split grammar is the real entry point.** Everything above was done
   on a hand-combined grammar; `tokenVocab` plus lexer / parser
   `superClass` support ([#39](https://github.com/miho/VMF-Text/issues/39))
   would let the grammars-v4 files be used as they are (the lexer base would
   still need the exact-range and hidden-channel changes, or VMF-Text has to
   derive trivia from character positions instead of hidden tokens).

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
7. ⏳ Python-aware policy for fully programmatic blocks (finding 7).
8. ⏳ Naming (#47), split grammars (#39).
