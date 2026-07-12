# VMF-Text Roadmap

*Last updated: 2026-07-12*

VMF-Text occupies a niche no other framework covers: **a plain (labeled or
auto-labeled) ANTLR4 grammar in → a rich, typed VMF model plus an exact
round-trip unparser out, on the JVM.** Xtext and Langium require rewriting
grammars in their own dialects; JavaParser's lexical-preserving printer is
Java-only; raw ANTLR offers no model or unparsing layer. This roadmap turns
that differentiator into something outsiders can see and adopt.

Design background: `LEXICAL_PRESERVATION_ASSESSMENT.md`.

## Phase 0 — Ship 0.2.0 (in progress)

The first non-preview release in the project's history (previous releases:
0.1–0.1.2 previews, 2018). Everything below is version-aligned in the working
tree; remaining steps are validation, publishing, and housekeeping.

- [x] VMF `0.2.10` released to Maven Central (prerequisite, done 2026-07-12)
- [x] Align all version references: `vmf-text` → `0.2.0`
      (`config/common.properties`), VMF → `0.2.10` (core build, plugin
      default, README, AGENTS.md), ANTLR → `4.13.2`
- [x] CI builds against released VMF from Maven Central (no more VMF source
      build in `.github/workflows/ci.yml`)
- [x] Plugin Portal metadata (`website`/`vcsUrl`) points at `miho/VMF-Text`
      (was the retired `VMF-Text-Gradle-Plugin` repo)
- [ ] Full-chain local validation green: `sh ./build-and-test-all.sh`
- [ ] VMF `0.2.10` visible on `repo1.maven.org` (Central sync) — required
      before pushing, CI resolves VMF from Central
- [ ] Commit release prep + roadmap, push, CI green
- [ ] Publish `eu.mihosoft.vmf:vmf-text:0.2.0` to Maven Central
      (closes [#12](https://github.com/miho/VMF-Text/issues/12))
- [ ] Publish `vmf-text-gradle-plugin:0.2.0` to the Gradle Plugin Portal
      (plugin id `eu.mihosoft.vmftext`; last Portal release was 0.1.2.7)
      and Maven Central
- [ ] Smoke test from a clean consumer project (no `mavenLocal`):
      `plugins { id "eu.mihosoft.vmftext" version "0.2.0" }`, small grammar,
      generate + compile + parse/unparse round trip
- [ ] Tag `v0.2.0`, create GitHub release (draft notes in the appendix)
- [ ] Issue triage:
  - close [#2](https://github.com/miho/VMF-Text/issues/2) (lexical
    preserving unparser — shipped in 0.2.0)
  - close [#12](https://github.com/miho/VMF-Text/issues/12) (Maven Central)
  - retest [#8](https://github.com/miho/VMF-Text/issues/8) (labeling `.`)
    and [#9](https://github.com/miho/VMF-Text/issues/9) (grammar without
    lexer rules) against 0.2.0; close with repro or update status
  - answer [#14](https://github.com/miho/VMF-Text/issues/14) (superClass
    option) with a decision
  - label [#1](https://github.com/miho/VMF-Text/issues/1) /
    [#7](https://github.com/miho/VMF-Text/issues/7) (rule/model rewriting)
    as roadmap/design — feeds Phase 2
- [ ] Post-release cleanup: `gradle-plugin/gradle/project-info.gradle`
      hardcodes `versionId`; read it from `config/common.properties` like
      `core` does

### Publishing prerequisites (one-time machine setup)

`~/.gradle/gradle.properties` must define (a commented template is already in
place there):

- `mavenCentralUsername` / `mavenCentralPassword` — Central Portal user token
  (namespace `eu.mihosoft`)
- `signing.keyId` / `signing.password` / `signing.secretKeyRingFile` — GPG
  release key
- `gradle.publish.key` / `gradle.publish.secret` — Gradle Plugin Portal API
  key

### Publish commands

```bash
# 1) core → Maven Central
cd core
sh ./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon
sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft --no-daemon
# then press "Publish" on https://central.sonatype.com/publishing/deployments
# (or pass -PpublishingType=automatic to skip the manual click)

# 2) gradle-plugin → Plugin Portal + Maven Central
cd ../gradle-plugin
sh ./gradlew publishPlugins --no-daemon
sh ./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon
sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft --no-daemon
```

## Phase 1 — Prove the differentiator (~2 weeks after 0.2.0)

- **Round-trip showcase.** Parse a real Java 24 source file with the bundled
  `java24` grammar, apply a programmatic model edit (rename a method, add an
  annotation), unparse — everything untouched stays byte-identical. Runnable
  example + prominent README section. This is the demo no comparable tool
  can reproduce generically.
- **Honest comparison page** (in the style of textX's comparison docs):
  VMF-Text vs Xtext, Langium, textX, plain ANTLR, JavaParser — grammar reuse,
  round-trip fidelity, model API depth (immutable views, change recording,
  undo, clone), editor/LSP story (VMF-Text: none built-in — documented
  bridge instead), platform and runtime weight.
- **README refresh.** Lead with the one-sentence value proposition, document
  `autoLabel = true` for unlabeled/partially labeled grammars, state the
  lexical-preservation guarantee precisely (exact for parsed models,
  conservative separator fallback for programmatically set values).

## Phase 2 — Harden the core (design work, scope after Phase 1)

From `LEXICAL_PRESERVATION_ASSESSMENT.md`:

- **Typed lexical metadata** (`LexicalInfo` / `TriviaPiece` /
  `OptionalState`) instead of untyped payload maps → clean VMF-Jackson /
  JSON-schema story.
- **Path-keyed optional-presence state** — remove the positional coupling of
  `optionalSymbols`.
- **Formatter policy for programmatically created models** — pluggable
  pretty-printing / grammar-aware separators where exact preservation is
  undefined by construction.
- **Written LSP stance** — document how to feed the generated model into an
  LSP4J-based server; deliberately do not build a language workbench.
- **Isolate Java-target ANTLR action injection** — keeps a future door open
  for other ANTLR targets without committing to them.

## Non-goals

- Competing with Xtext/Langium on bundled IDE/editor tooling.
- Multi-target ANTLR runtimes (C++/JS/Python generation).
- Projectional editing (MPS territory).

## Appendix: draft release notes v0.2.0

> **VMF-Text 0.2.0** — first stable release.
>
> VMF-Text turns a plain ANTLR4 grammar into a typed model API with parsing,
> unparsing, and transformation support, built on the VMF modeling framework.
>
> **Highlights**
> - **Exact lexical preservation:** parse → edit the model → unparse;
>   all untouched text (whitespace, comments, formatting) is reproduced
>   byte-identically. Programmatically set values fall back to conservative
>   separators (see `LEXICAL_PRESERVATION_ASSESSMENT.md`).
> - **Auto-labeling (`autoLabel = true`):** consume unlabeled or partially
>   labeled ANTLR4 grammars; explicit labels always win, inferred names are
>   deterministic — tested against complex real-world grammars.
> - **Full Java 24 grammar** in the test suite as an end-to-end proof
>   (string templates intentionally unsupported — the preview feature was
>   withdrawn after Java 22).
> - **Modernized stack:** JDK 21 toolchain, Gradle 9, ANTLR 4.13.2,
>   VMF 0.2.10 — all dependencies resolve from Maven Central.
> - **Availability:** `eu.mihosoft.vmf:vmf-text:0.2.0` and
>   `eu.mihosoft.vmf:vmf-text-gradle-plugin:0.2.0` on Maven Central; Gradle
>   plugin `eu.mihosoft.vmftext` on the Plugin Portal.
>
> ```gradle
> plugins { id "eu.mihosoft.vmftext" version "0.2.0" }
> vmfText {
>     vmfVersion   = '0.2.10'
>     antlrVersion = '4.13.2'
> }
> ```
