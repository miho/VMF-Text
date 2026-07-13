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

The runnable [`examples/java24-roundtrip`](examples/java24-roundtrip)
showcase parses a real Java 24 source file (sealed types, records, pattern
switches with guards, text blocks) with a full Java 24 grammar, verifies the
unedited unparse is byte-identical, then renames one method *on the model*:

```java
model.vmf().content().stream(MethodDeclaration.class)
     .filter(m -> "describe".equals(m.getMethodName().getText()))
     .forEach(m -> m.getMethodName().setText("render"));
```

The diff against the original file is exactly one line:

```
[1] sample/Shapes.java (955 chars) round-tripped byte-identically
  line 27  -     static String describe(Shape shape) {
  line 27  +     static String render(Shape shape) {
[2] one model edit -> one changed line; every other byte is untouched
```

Run it yourself — it resolves the released VMF-Text straight from Maven
Central:

```
cd examples/java24-roundtrip
./gradlew run
```

None of this is Java-specific: the same parse → edit → unparse API is
generated for any labeled ANTLR4 grammar. (Exact preservation applies to
parsed content; values you set programmatically use conservative separators —
see `LEXICAL_PRESERVATION_ASSESSMENT.md`.)

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
  id "eu.mihosoft.vmftext" version "0.2.0" // use latest version
}
```
(optionally) configure VMF-Text:

```gradle
vmfText {
    vmfVersion   = '0.2.10'  // (runtime version)
    antlrVersion = '4.13.2'  // (runtime version)
    // autoLabel = true      // opt-in: derive labels for unlabeled grammars,
                             // see "Automatic Labels" below
}
```

Now just add the labeled [ANTLR4](https://github.com/antlr/antlr4) grammar file to the VMF-Text source folder, e.g.: 

```
src/main/vmf-text/my/pkg/ArrayLang.g4
```

Sample grammar for parsing strings of the form `(1,2,3)`:

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

Explicit labels always win and mix consistently with auto-labeling. Unlabeled
parser-rule and token references receive deterministic names based on grammar
order; duplicate names receive stable numeric suffixes. Suffix numbering is
scoped to the generated type: in rules whose alternatives become separate typed
sub classes each alternative numbers its names independently. Generated element
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
names.

## Typed Lexical Metadata

Parsed `CodeElement`s now expose typed lexical metadata via `getLexicalInfo()`.
The typed mirror currently contains ignored text pieces, optional-symbol states,
the original code range and a grammar element identifier. The existing raw
payload map remains available for compatibility, but the default formatter reads
typed lexical info first and falls back to the payload map if needed.

The typed metadata is ignored for semantic equality. This allows two models with
the same language semantics but different source trivia to compare as semantic
models, while source-preserving workflows can still serialize or inspect the
lexical information explicitly. For semantic-only JSON/schema workflows, treat
`CodeElement.getPayload()` as internal VMF-Text state and prefer source bundles
or typed `LexicalInfo` for source-preserving persistence.

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
