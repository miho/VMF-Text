# Experiment: an unlabeled grammars-v4 C grammar through VMF-Text

*Run 2026-10-20 against VMF-Text 0.2.2 (plugin from Maven Central / mavenLocal),
VMF 0.2.11, ANTLR 4.13.2, JDK 21. Roadmap item "second big language, unlabeled".*

**Question.** What breaks when a real, unlabeled ANTLR grammar is fed to
VMF-Text with `autoLabel = true`, and how good is the round trip? The Java 24
showcase never answered this: its grammar was labeled by hand.

**Short answer.** Generation, compilation and parsing all work out of the box
(86 parser rules → 1147 generated Java files in under a minute, no manual
labels). Unparsing does **not**: on the 32 sample programs that ship with the
grammar, 14 are byte-exact, 10 fail to unparse at all and 8 come back wrong.
Every failure reduces to a short list of grammar shapes that an imported
grammar uses freely and VMF-Text does not handle yet. None of them is about
whitespace fidelity: where the model is complete, the bytes are exact.

## Setup

- Grammar: the last *single-file* `C.g4` from grammars-v4
  (commit `e25e2973`, BSD, Sam Harwell 2013; 1111 lines, 86 parser rules,
  no labels, no predicates, hidden-channel comments and directives). Copied
  to [`src/main/vmf-text/ctrial/C.g4`](src/main/vmf-text/ctrial/C.g4) with two
  edits: `compilationUnit` moved to the top (see finding 4) and a trailing
  `AutoLabel(enabled=true)` metadata comment.
- Why not grammars-v4 HEAD: the C grammar there is now split into
  `CLexer.g4` + `CParser.g4` (`tokenVocab`), both with `superClass` base
  classes; the lexer base runs the *C preprocessor* (gcc/clang) over the
  input before tokenizing, and the parser carries a symbol table with ~25
  semantic functions behind 17 predicates and 3 actions for the typedef-name
  ambiguity. That is not a grammar-only artifact any more; see finding 8.
- Samples: the 32 `*.c` files from `grammars-v4/c/examples` (mixed licenses,
  not committed — `fetch-samples.sh` downloads them into `samples/`).
- Drivers: `ctrial.Main` parses + unparses every sample and reports
  byte-exactness and timings; `ctrial.Snip` runs the one-line snippets in
  `snippets.txt`, which isolate each failure.

```bash
sh ./fetch-samples.sh
sh ./gradlew run                                             # the 32 samples
sh ./gradlew run -PmainClass=ctrial.Snip --args=snippets.txt # isolated shapes
```

## Numbers

| Measure | Result |
|---|---|
| Parser rules / generated Java files | 86 / 1147 |
| Auto-labeled elements (of which alternative classes) | 430 (143) |
| Generate + compile (`gradlew build`, cold) | 58 s |
| Samples: exact / mismatch / unparse failure / parse failure | 14 / 8 / 10 / 0 |
| Total parse time, 36 KB of C (incl. JIT and ATN warm-up) | 1.0 s |
| Total unparse time, same input | 78 ms |
| ANTLR syntax errors swallowed while producing those results | 15 in one file alone |

Unparse speed is a non-issue: nothing in this grammar triggers the
unparser's slow full-check mode. Whitespace and comments survive wherever the
model is complete, including leading file comments.

## Findings (each isolated by a snippet)

1. **`X+` single-element lists cannot be unparsed.** `primaryExpression:
   … | StringLiteral+ | …` is auto-labeled `stringLiterals`, parses fine, and
   every unparse throws `Cannot unparse rule 'PrimaryExpressionAlt3'. Language
   model is invalid!` — even for `char *s = "a";`. This is the shape
   `docs/UNPARSING.md` §5 tells grammar authors to avoid (`items+=ID+`); an
   imported grammar cannot be rewritten around it. **10 of the 32 samples
   fail only because of this.**
2. **Keyword-only alternatives inside a block lose their choice.**
   `jumpStatement: ('goto' Identifier | 'continue' | 'break' | 'return'
   expression? | …) ';'` — the alternatives that consist only of a literal
   leave nothing in the model, so `return 1;`, `return;` and `break;` all
   unparse as `;` (and `continue;` is exact by accident). Top-level
   alternatives get alternative classes (`PrimaryExpressionAlt1…8`), nested
   blocks do not. A discriminator (captured keyword, enum, or alternative
   classes for blocks) is needed. Hits every function body with a `return`.
3. **Syntax errors are silent.** `typedef struct {} MyStruct;` is a GNU
   extension the classic grammar does not accept; ANTLR prints
   `no viable alternative` to stderr and recovers, `CModelParser.parse` returns
   a model built from the recovered tree, and unparse emits ` MyStruct;`. One
   sample produced 15 syntax errors and still came back byte-exact. The
   generated parser has error-listener plumbing but no bail/strict default;
   for a round-trip tool a parse with errors should fail or be clearly flagged.
   The three "leading comment lost" mismatches are all downstream of this.
4. **The model root is the first parser rule, by convention.** The C grammar
   lists expressions first and `compilationUnit` last, so the root type was
   `PrimaryExpression`. Worse: because that rule got alternative classes, the
   generated `CModelUnparser` did not compile (it dispatches to a
   `primaryExpressionUnparser` field that does not exist for an alt-labeled
   rule). Needed: explicit root selection (`Root(rule=…)` metadata or
   "the rule that ends in `EOF`"), and a compile-clean unparser for an
   alt-labeled root.
5. **Operator literals get placeholder names.** `('*' | '/' | '%')` becomes
   `operators`; other literal captures become `symbols`, `symbols2` …
   `symbols6` (18 + 8 occurrences). They round-trip, but the API is poor —
   the "operator-like literals as typed discriminators" item from the
   assessment. Also: 172 of 430 inferred property names are PascalCase
   because the lower-camel form collides with a parser-rule name; getters are
   unaffected, only the rewritten grammar looks odd.
6. **Naming otherwise holds up.** Keyword-context names work in practice
   (`'return' expression?` → `returnExpression`, `'goto' unaryExpression` →
   `gotoUnaryExpression`), plurals are right for this grammar, and only 11
   names needed numeric suffixes.
7. **Fidelity is not the problem.** Every sample whose model was complete came
   back byte-exact, with no edits made; the lexical layer did not produce a
   single mismatch of its own.
8. **grammars-v4 HEAD is out of reach for structural reasons.** Split
   lexer/parser grammars (`tokenVocab`), a lexer `superClass` (not
   expressible in a combined grammar at all), and predicate/action
   pass-through would all be needed just to generate; the preprocessing lexer
   is incompatible with byte-exact round trips by design. `tokenVocab`
   support is the one piece worth having, since most grammars-v4 languages
   are now split.

## What to do with this

In order of samples unblocked per unit of work:

1. Make `X+` (and `X*`) single-element lists unparse (finding 1) — the
   generated matcher/unparser for an EBNF-suffixed lexer or rule reference
   with no explicit second occurrence.
2. Give block alternatives a discriminator (finding 2): capture the chosen
   keyword literal as a property, or lift nested alternative blocks to
   alternative classes the way top-level alternatives already are.
3. Fail loudly on syntax errors by default, with an opt-out (finding 3).
4. Explicit root rule + compile-clean alt-labeled root (finding 4).
5. Better names for operator literals (finding 5) — cosmetic, API-facing.
6. `tokenVocab` / split grammars (finding 8) — a separate feature.

Items 1–4 together should take this grammar from 14/32 to roughly 29/32
exact; the remaining three samples use GNU extensions the grammar rejects.
