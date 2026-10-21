# VMF-Text [![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=VMF-Text:%20The%20new%20framework%20for%20grammar-based%20language%20modeling!&url=https://github.com/miho/VMF-Text&via=mihosoft&hashtags=vmftext,vmf,antlr4,java,mdd,developers)

[![CI](https://github.com/miho/VMF-Text/actions/workflows/ci.yml/badge.svg)](https://github.com/miho/VMF-Text/actions/workflows/ci.yml)
[![Join the chat at https://gitter.im/VMF_/Lobby](https://badges.gitter.im/VMF_/Lobby.svg)](https://gitter.im/VMF_/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

VMF-Text is a framework for grammar-based language modeling on the Java Platform (the published artifacts run on Java 11 and later): give it a plain [ANTLR4](https://github.com/antlr/antlr4) grammar — labeled for a curated API, or [auto-labeled](#automatic-labels) as-is — and it generates a rich and clean API (based on [VMF](https://github.com/miho/VMF)) for parsing, transforming and unparsing custom textual languages, with exact round-trip fidelity for parsed sources. **The complete API is derived from just a single ANTLR4 grammar file!**

Wondering how this relates to Xtext, Langium, textX, plain ANTLR4 or JavaParser? See the honest comparison in [COMPARISON.md](COMPARISON.md). Building editor support? See the [LSP integration guide](LSP_INTEGRATION.md).

<img src="resources/img/vmf-text-01.jpg">

## Round-Trip Fidelity

VMF-Text preserves the *exact* lexical shape of parsed sources: parse a file
into a typed model, change what you need, unparse — everything you did not
touch is reproduced byte-for-byte, comments, blank lines and irregular
spacing included.

### What “exact” covers (and what it does not)

- **Unedited parsed models** unparse byte-identically (including original
  type-mapped lexemes such as `1` vs `1.0`).
- **In-place value rewrites** (non-null property set, or `list.set(i, …)`) keep
  that rule’s trivia: only the token text changes; unchanged siblings keep
  their original spellings.
- **Structural add/remove on delimited primitive lists** shaped like
  `'(' item (',' item)* ')'` or bare `item (',' item)*` surgically splice
  trivia slots so sibling whitespace survives (0.2.1+).
- **Optional null↔value** updates optional presence so groups appear/disappear
  correctly while keeping leading inter-rule whitespace; repeated optional
  paths use `OptionalState.occurrenceIndex` (0.2.1+).
- **Multi-list rules** get codegen `ListShapeHint`s so splicing one list does
  not clear another (0.2.1+).
- **Edits on nested model objects** (e.g. a `MethodDeclaration` or
  `StringLiteral` child) only affect that object’s own trivia; siblings keep
  theirs.
- **Unrecognized structural shapes** (or emptying a one-or-more list) still
  clear trivia on that rule and fall back to `ConservativeSeparatorPolicy`.
- **Source bundles** are unrelated to unparse-after-edit: they store original
  source for persistence/restore when semantics still match.

How unparsing works in detail: [`docs/UNPARSING.md`](docs/UNPARSING.md).
Edit-invalidation matrix and gaps: `LEXICAL_PRESERVATION_ASSESSMENT.md`.

Runnable showcases live under [`examples/`](examples/) and climb in
complexity (see that README for the full ladder: ArrayLang → bare lists →
original lexemes → optional null↔value / occurrenceIndex → JSON lists →
multi-list `ListShapeHint` → `separatorCount` → Java 8 / Java 24). Highlights:

1. **[`examples/arraylang-roundtrip`](examples/arraylang-roundtrip)** — the
   tiny `ArrayLang` grammar from this README. Exact round-trip, then
   `set` / `add` / `remove` while keeping sibling whitespace (needs
   VMF-Text 0.2.1+ / local publish until Central catches up).

2. **[`examples/java8-roundtrip`](examples/java8-roundtrip)** — a small Java 8
   source file with a full Java 8 grammar. Rename a method and replace a
   string literal on nested model objects; every other byte stays untouched:

```java
model.vmf().content().stream(MethodDeclaration.class)
     .filter(m -> "greet".equals(m.getMethodName()))
     .forEach(m -> m.setMethodName("sayHello"));

model.vmf().content().stream(StringLiteral.class)
     .filter(lit -> "\"hello\"".equals(lit.getStringValue()))
     .forEach(lit -> lit.setStringValue("\"hello, world\""));
```

3. **[`examples/java24-roundtrip`](examples/java24-roundtrip)** — a real Java
   24 source file (sealed types, records, pattern switches with guards, text
   blocks) with a full Java 24 grammar. One method rename on the model:

```java
model.vmf().content().stream(MethodDeclaration.class)
     .filter(m -> "describe".equals(m.getMethodName().getText()))
     .forEach(m -> m.getMethodName().setText("render"));
```

```
[1] sample/Shapes.java (955 chars) round-tripped byte-identically
  line 27  -     static String describe(Shape shape) {
  line 27  +     static String render(Shape shape) {
[2] one model edit -> one changed line; every other byte is untouched
```

Run any of them:

```
cd examples/arraylang-roundtrip   # or java8-roundtrip / java24-roundtrip
./gradlew run
```

None of this is Java-specific: the same parse → edit → unparse API is
generated for any labeled ANTLR4 grammar.

## Using VMF-Text

Checkout the tutorial projects: https://github.com/miho/VMF-Text-Tutorials

VMF-Text comes with excellent Gradle support. Make sure `mavenCentral()` is
among your plugin repositories in `settings.gradle` (this is where the plugin
and all of its dependencies are published):

```gradle
// settings.gradle
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Then add the plugin (click [here](https://plugins.gradle.org/plugin/eu.mihosoft.vmftext) to get the latest version):

```gradle
plugins {
  id "eu.mihosoft.vmftext" version "0.2.2" // use latest version
}
```
(optionally) configure VMF-Text:

```gradle
vmfText {
    // vmfVersion = '…'      // VMF runtime; defaults to the VMF version the plugin
                             // generates code with — leave it unset (honoured since
                             // 0.2.6, with a warning when it differs; ignored before)
    antlrVersion = '4.13.2'  // (runtime version)
    // autoLabel = true      // opt-in: derive labels for unlabeled grammars,
                             // see "Automatic Labels" below
}
```

The first parser rule of a grammar becomes the model root (and the start rule
of the generated parser). Grammars that list their start rule last, as the
grammars-v4 C grammar does, declare it instead:

```antlr
/*<!vmf-text!>
Root(rule=compilationUnit)
*/
```

Parsing is strict by default: `parse(...)` throws
`<Grammar>ModelParser.SyntaxErrorException` (with one `line:column message` per
error in `getErrors()`) instead of returning a model built from ANTLR's
error-recovered tree. Call `setFailOnSyntaxError(false)` on the parser to get
that model anyway.

Now just add the labeled [ANTLR4](https://github.com/antlr/antlr4) grammar file to the VMF-Text source folder, e.g.: 

```
src/main/vmf-text/my/pkg/ArrayLang.g4
```

Sample grammar for parsing strings of the form `(1,2,3)` (see the runnable
[`examples/arraylang-roundtrip`](examples/arraylang-roundtrip) showcase):

```antlr
grammar ArrayLang;

array:  '(' values+=INT (',' values+=INT)* ')' EOF;

INT: SIGN? DIGIT+
   ;

fragment SIGN :'-' ;
fragment DIGIT : [0-9];

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
    ;

/*<!vmf-text!>
TypeMap() {
  (INT    -> java.lang.Integer) = 'java.lang.Integer.parseInt(entry.getText())'
}
*/
```

Finally, call the `vmfTextGenCode` task to generate the implementation.

## Source-Preserving Persistence

VMF-Text now generates a small source bundle helper for every grammar. A source
bundle stores the semantic VMF model together with the original source text and
basic provenance metadata such as grammar name, VMF-Text version and a SHA-256
source checksum.

```java
Java8ModelParser parser = new Java8ModelParser();
Java8Model model = parser.parse(sourceText);

Java8SourceBundle bundle = parser.toSourceBundle(model, sourceText);
Java8Model restored = parser.restoreFromSourceBundle(bundle);
```

Restore follows a conservative source-preserving policy:

1. reparse the bundled source text with the generated grammar;
2. compare the reparsed model with the stored semantic model;
3. return the reparsed model, including fresh lexical-preservation payload, only
   if the semantic models match;
4. otherwise return the stored semantic model after clearing stale VMF-Text
   lexical payload so the normal formatter fallback is used.

This makes source bundles useful for editor and persistence workflows: exact
comments/whitespace are recovered when the source still corresponds to the
model, while semantic model edits or corrupted source text fall back safely to
parseable generated text.

## Automatic Labels

Curated grammars can still use explicit ANTLR labels to define the cleanest
public VMF API. For exploratory or imported grammars, VMF-Text also provides an
opt-in auto-labeling prototype. It is disabled by default and can be enabled
globally from Gradle:

```gradle
vmfText {
    autoLabel = true
}
```

or per grammar via VMF-Text metadata:

```antlr
/*<!vmf-text!>
AutoLabel(enabled=true)
*/
```

Nested alternative blocks are **lifted** into typed sub classes: a block such
as `('goto' ID | 'continue' | 'break' | 'return' expr?)` inside rule `stmt`
becomes a synthetic rule `stmtChoice` with sub classes `StmtChoiceAlt1…4`, so
the chosen alternative survives in the model and unparses correctly even when
it consists only of keywords. Token sets like `('+' | '-')` stay a captured
literal (`operators+=` when repeated, `operator=` otherwise), and a single
literal captured inside a repeated block is named after its text (`','` →
`commas`, `'['` → `lBrackets`, `'static'` → `staticKeywords`). A repeated
block with an optional property inside, such as Python's
`(',' tfpdef ('=' test)?)*`, is lifted into a per-item sub class
(`<rule>Items+=<rule>Item*`) so every iteration keeps its own optional parts.
The auto-label report lists every lifted block. Hand-labeled grammars get the
same treatment
for *literal-only* blocks such as `('[' ']' | '[]')`, which have no model
property otherwise: the enclosing rule gains a `<Rule>Choice` property whose
value is one of the generated sub classes.

A lexer rule that is a single literal (`Var: 'var';`, `Else: 'else';`) is a
keyword, not a value: it gets no property, and it provides keyword context
for the element after it (`Else statement` → `elseStatement`, like a literal
`'else'`). The two operands of an `expr op expr` alternative are `left` and
`right`. Property names and synthetic rule names derived from snake_case
rules drop the underscores (`simple_stmt*` → `simpleStmts`), and a
snake_case parser rule is itself renamed to camelCase before any other pass,
so its model class is `SimpleStmt` and the ANTLR parser says `simpleStmt()`
(a trailing underscore as in `subscript_` stays; `Root(rule=simple_stmt)`
and type-map apply-to lists accept either spelling).

Explicit labels always win and mix consistently with auto-labeling. Unlabeled
parser-rule and token references receive deterministic lower-camel names based
on the referenced rule or token (no `Node` suffix). Parser-rule refs become
plain names such as `statements` for lists; a singular ref whose lower-camel
name would collide with a parser rule (ANTLR error 69) is emitted in PascalCase
(e.g. `Statement=statement`) so getters stay `getStatement()`. Token refs stay
lower-camel, and Java-keyword token names get a `Value` suffix (e.g. `INT` →
`intValue`). Repeated elements are pluralized into list properties.

When a parser rule and a token share a name, as `identifier` and `Identifier`
do in the grammars-v4 JavaScript grammar, ANTLR accepts neither spelling as a
label, so the name says what the property holds: a reference to the rule is
`identifierNode` (`getIdentifierNode()` returns the `Identifier` node), a
reference to the token is `identifierToken` (its text), unless a keyword in
front gives a keyword-context name (`'goto' label` → `gotoLabel`). These names
are the model's API in code — getters, setters, builders
(`withIdentifierNode(...)`) and VMF reflection — and every token-typed property
also carries the annotation `vmf-text:token` with the token's grammar name, so
code can find the property that holds a token without knowing its name.

When the same base name would collide inside a type, AutoLabeler prefers a
keyword-context name if the nearest preceding sibling is a keyword-like string
literal (`[a-zA-Z_][a-zA-Z0-9_]*` after stripping quotes): e.g.
`'else' statement` becomes `elseStatement` instead of `statement2`. Otherwise
duplicate names receive stable numeric suffixes. Suffix numbering is scoped to
the generated type: in rules whose alternatives become separate typed sub
classes each alternative numbers its names independently. Generated element
names never collide with manually chosen labels (a hand-written `identifier=`
label keeps its name, and an auto-labeled sibling becomes `identifier2`). If a rule
labels only *some* of its alternatives with `#` (which ANTLR rejects on its own,
since alternative labeling is all-or-none), the remaining alternatives are
labeled automatically so the grammar stays valid while the manual labels are
preserved.

Auto-labeling also handles the structural idioms found in default (unlabeled)
ANTLR4 grammars:

- elements that can occur more than once become list properties. This includes
  elements nested inside a repeated block, so `term (('+' | '-') term)*` exposes
  the repeated `term`s as a list instead of collapsing them.
- parser rules with two or more unlabeled top-level alternatives receive
  deterministic `# <Rule>AltN` alternative labels, so each alternative becomes
  its own typed sub class (e.g. `factor : INT | IDENTIFIER | '(' expr ')'`
  yields `FactorAlt1`, `FactorAlt2`, `FactorAlt3` extending `Factor`).
- unnamed operator/separator literals inside a repeated block (such as the
  `('+' | '-')` in `(('+' | '-') term)*` or the `','` in `(',' item)*`) are
  captured as ordered list properties so the exact text is reproduced when
  unparsing instead of being dropped. Token-set groups like `('+' | '-')` are
  captured as one list; multi-token groups like `('[' ']')*` are captured
  element-wise (one ordered list per literal), since ANTLR only allows a
  single label on blocks that form a token set.

Isolated string literals outside of repeated blocks remain syntax and are not
exposed as semantic properties. During generation, VMF-Text prints an auto-label
report that maps grammar element paths to inferred property and alternative
names. When enabled via Gradle (`emitAutoLabelReport`, default `true`), the
same report is also written under `build/reports/vmf-text/` (default file
`autolabel-report.txt`, overridable with `autoLabelReportFile`). Explicit
labels always win over inferred names.

## Split Grammars (Lexer + Parser)

Most grammars-v4 languages ship as a `lexer grammar` plus a `parser grammar`
that refers to it through `options { tokenVocab = ...; }`. VMF-Text processes
such a pair as one unit: put both files into the same directory and let the
*parser* grammar be the input (the Gradle plugin skips lexer grammars and
regenerates the parser grammar when its lexer changes). The model is named
after the parser grammar minus its `Parser` suffix — `Python3Lexer.g4` +
`Python3Parser.g4` give `Python3Model`, `Python3ModelParser` and
`Python3ModelUnparser` — and the lexer is generated into the same
`<package>.parser` package, which is also where a lexer `superClass` base
class has to live. A parser `superClass` base class lives there too, once:
an unqualified `superClass` option is qualified with `<package>.parser` in
the grammar handed to ANTLR, so the unparser's check grammar (which shares
the lexer but has its own parser) extends the same class, exactly as for a
combined grammar. Lexer rules that are a single literal (`Var: 'var';`) keep
working as keywords, see above.

## Typed Lexical Metadata

Parsed `CodeElement`s expose typed lexical metadata via `getLexicalInfo()`.
The typed mirror contains `TriviaPiece` entries (text plus kind), path-keyed
`OptionalState` entries (`getOptionalStates()` with `occurrenceIndex`),
`ListShapeHint` / `OriginalLexeme` data, the original code range and a grammar
element identifier. The unparser consumes optional presence by
`(grammarElementPath, occurrenceIndex)` — see [`docs/UNPARSING.md`](docs/UNPARSING.md).

Freshly parsed models no longer write `vmf-text:` entries into
`CodeElement.getPayload()`; lexical metadata lives exclusively on `LexicalInfo`.
`getPayload()` remains on the API for user-defined extensions but is deprecated
for VMF-Text internals.

The typed metadata is ignored for semantic equality. This allows two models with
the same language semantics but different source trivia to compare as semantic
models, while source-preserving workflows can serialize or inspect the lexical
information explicitly. For semantic-only JSON/schema workflows, omit `LexicalInfo`
from the schema or use source bundles for source-preserving persistence.

Programmatically created models (no parse-time lexical info) consult a pluggable
`ProgrammaticSeparatorPolicy` inside the default formatter. The built-in
`ConservativeSeparatorPolicy` reproduces today's single-space fallback, and
`Formatter.newBlockIndentFormatter()` prints programmatic or edited parts with
line breaks and block indentation while parsed bytes stay exact
(`programmaticRanges()` tells you which lines those are). For languages whose
line ends and blocks are tokens (Python's `NEWLINE` / `INDENT` / `DEDENT`),
`<Grammar>TriviaSupport.fillIndentationTokens(root, indentUnit)` sets the
token values of blocks built from scratch by nesting depth and
`Formatter.newIndentationTokenFormatter()` places the statements inside them
at that depth, also inside a parsed block (see
[`docs/UNPARSING.md`](docs/UNPARSING.md) §7); custom
policies are consulted only where exact preservation is undefined by construction
(fresh programmatic nodes, or rules whose trivia was cleared after a
**structural** edit such as list add/remove). In-place value rewrites keep
parsed trivia — see `LEXICAL_PRESERVATION_ASSESSMENT.md` § *Edit invalidation*.

## Building VMF-Text (Core)

### Requirements

- JDK 21 (build toolchain; the published artifacts run on Java 11+)
- Internet connection (dependencies are downloaded automatically)
- IDE: [Gradle](http://www.gradle.org/) Plugin (not necessary for command line usage)

### IDE

Open the `VMF-Text/core` [Gradle](http://www.gradle.org/) project in your favourite IDE (tested with NetBeans 8.2 and IntelliJ 2018) and build it
by calling the `publishToMavenLocal` task.

### Command Line

Navigate to the [Gradle](http://www.gradle.org/) project (i.e., `path/to/VMF-Text/core`) and enter the following command

#### Bash (Linux/macOS/Cygwin/other Unix shell)

    bash gradlew publishToMavenLocal
    
#### Windows (CMD)

    gradlew publishToMavenLocal

## Building VMF-Text (Gradle Plugin)

### Requirements

- JDK 21 (build toolchain; the published artifacts run on Java 11+)
- Internet connection (dependencies are downloaded automatically)
- IDE: [Gradle](http://www.gradle.org/) Plugin (not necessary for command line usage)

### IDE

Open the `VMF-Text/gradle-plugin` [Gradle](http://www.gradle.org/) project in your favourite IDE (tested with NetBeans 8.2 and IntelliJ 2018) and build it
by calling the `publishToMavenLocal` task.

### Command Line

Navigate to the [Gradle](http://www.gradle.org/) project (i.e., `path/to/VMF-Text/gradle-plugin`) and enter the following command

#### Bash (Linux/macOS/Cygwin/other Unix shell)

    bash gradlew publishToMavenLocal
    
#### Windows (CMD)

    gradlew publishToMavenLocal 

## Testing VMF-Text (Core & Plugin)

To execute the test suite, navigate to the test project (i.e., `path/to/VMF-Text/test-suite`) and enter the following command

#### Bash (Linux/macOS/Cygwin/other Unix shell)

    bash gradlew test
    
#### Windows (CMD)

    gradlew test

This will use the latest snapshot vmf-text and gradle-plugin to execute the tests defined in the test-suite project.

### Viewing the Report

An HTML version of the test report is located in the build folder `test-suite/build/reports/tests/test/index.html`.
