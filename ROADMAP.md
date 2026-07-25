# VMF-Text Roadmap

*Last updated: 2026-07-25*

VMF-Text occupies a niche no other framework covers: **a plain (labeled or
auto-labeled) ANTLR4 grammar in → a rich, typed VMF model plus an exact
round-trip unparser out, on the JVM.** Xtext and Langium require rewriting
grammars in their own dialects; JavaParser's lexical-preserving printer is
Java-only; raw ANTLR offers no model or unparsing layer. This roadmap turns
that differentiator into something outsiders can see and adopt.

Design background: `LEXICAL_PRESERVATION_ASSESSMENT.md`.

## Phase 0 — Ship 0.2.0 (shipped 2026-07-12)

The first non-preview release in the project's history (previous releases:
0.1–0.1.2 previews, 2018). Released as
[v0.2.0](https://github.com/miho/VMF-Text/releases/tag/v0.2.0).

- [x] VMF `0.2.10` released to Maven Central (prerequisite)
- [x] Align all version references: `vmf-text` → `0.2.0`
      (`config/common.properties`), VMF → `0.2.10` (core build, plugin
      default, README, AGENTS.md), ANTLR → `4.13.2`
- [x] CI builds against released VMF from Maven Central (no more VMF source
      build in `.github/workflows/ci.yml`)
- [x] Plugin Portal metadata (`website`/`vcsUrl`) points at `miho/VMF-Text`
      (was the retired `VMF-Text-Gradle-Plugin` repo)
- [x] Full-chain local validation green: `sh ./build-and-test-all.sh`
- [x] VMF `0.2.10` visible on `repo1.maven.org`
- [x] Commit release prep + roadmap, push, CI green
- [x] Publish `eu.mihosoft.vmf:vmf-text:0.2.0` to Maven Central
      (closed [#12](https://github.com/miho/VMF-Text/issues/12))
- [x] Publish `vmf-text-gradle-plugin:0.2.0` to Maven Central; Gradle Plugin
      Portal submission done for both plugin ids (`eu.mihosoft.vmftext`
      0.2.0, `eu.mihosoft.vmf` 0.2.10) — listings pending Gradle's approval
      of the group migration off the legacy `gradle.plugin.*` prefix
- [x] Plugin **markers** published to Maven Central
      (`eu.mihosoft.vmftext:eu.mihosoft.vmftext.gradle.plugin:0.2.0`,
      `eu.mihosoft.vmf:eu.mihosoft.vmf.gradle.plugin:0.2.10`), so
      `plugins { id "eu.mihosoft.vmftext" }` resolves with `mavenCentral()`
      in `pluginManagement` — the Portal listing is a secondary channel,
      not a requirement
- [x] Smoke test from a clean consumer project (Central-only resolution):
      generate + compile + parse/unparse round trip
- [x] Tag `v0.2.0`, GitHub release created
- [x] Issue triage: [#2](https://github.com/miho/VMF-Text/issues/2),
      [#9](https://github.com/miho/VMF-Text/issues/9) and
      [#12](https://github.com/miho/VMF-Text/issues/12) closed;
      [#8](https://github.com/miho/VMF-Text/issues/8) and
      [#14](https://github.com/miho/VMF-Text/issues/14) updated + labeled
      `enhancement`; [#1](https://github.com/miho/VMF-Text/issues/1) /
      [#7](https://github.com/miho/VMF-Text/issues/7) labeled `roadmap`
      (feed Phase 2)

## Ship 0.2.1 (shipped 2026-07-17)

Lexical-preservation polish release. Details: [`CHANGELOG.md`](CHANGELOG.md),
[`docs/UNPARSING.md`](docs/UNPARSING.md). Released as
[v0.2.1](https://github.com/miho/VMF-Text/releases/tag/v0.2.1).

- [x] Multi-alt list hints + optional trailing `','?` splice
- [x] Remove positional `optionalSymbols`; empty-list splice; `separatorCount`
- [x] Version → `0.2.1` (`config/common.properties`), README / examples aligned
- [x] Full-chain validation: `sh ./build-and-test-all.sh`
- [x] Fix `mavenJava`/`pluginMaven` publication coordinate collision (both
      defaulted to the same GAV; `pluginMaven` now publishes under a
      distinct `-portal` artifactId) and an eager `pluginMaven` property
      access in signing that only surfaced under a real signed publish
- [x] Publish `eu.mihosoft.vmf:vmf-text:0.2.1` to Maven Central
- [x] Publish `vmf-text-gradle-plugin:0.2.1` to Maven Central + Gradle Plugin
      Portal (marker published from the plugin build itself this time,
      resolving the cleanup item below)
- [x] Tag `v0.2.1`, GitHub release created

### 0.2.1 / 0.2.11 build cleanup queue

Small papercuts found during the 0.2.0 release, tracked in
[#19](https://github.com/miho/VMF-Text/issues/19) (closed):

- ~~Set `project.group` and `project.version` at project level in **both**
  gradle-plugin builds (VMF and VMF-Text)~~ — already done for VMF-Text at
  0.2.0; VMF's own plugin build is tracked in that sibling repo.
- Align the ANTLR tool and runtime versions in the generated builds (the
  "tool 4.13.2 vs runtime 4.11.1" warning did not reproduce during 0.2.1
  builds; re-check if it resurfaces).
- ~~`gradle-plugin/gradle/project-info.gradle` hardcodes `versionId`~~ —
  now reads from `config/common.properties` like `core` does.
- ~~Publish the plugin marker to Central from the plugin build itself~~ —
  resolved for 0.2.1: fixing the `pluginMaven` GAV collision let
  `publishPlugins` sign and publish the plugin + marker directly from this
  build.
- Verify the Plugin Portal listings once Gradle approves the group
  migration (notification arrives by email) — still pending on Gradle's
  side.

### Publishing prerequisites

Release credentials (Central Portal token, GPG signing key, Plugin Portal
API key) are **not** stored in this repository or on the build machine;
they are kept offline and passed per invocation as Gradle `-P` properties:
`mavenCentralUsername`/`mavenCentralPassword`,
`signing.keyId`/`signing.password`/`signing.secretKeyRingFile`,
`gradle.publish.key`/`gradle.publish.secret`.

### Publish commands (reference)

```bash
# 1) core → Maven Central
cd core
sh ./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon
sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft --no-daemon
# then press "Publish" on https://central.sonatype.com/publishing/deployments
# (or pass -PpublishingType=automatic to skip the manual click)

# 2) gradle-plugin → Maven Central + Plugin Portal
cd ../gradle-plugin
sh ./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon
sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft --no-daemon
sh ./gradlew publishPlugins --no-daemon
# publishPlugins from a release tag currently needs
#   -Pgroup=eu.mihosoft.vmf -Pversion=<version>
# (fix queued in the 0.2.1 cleanup above); the Central plugin marker is
# currently published by a separate POM-only project (see cleanup queue)
```

## Post-0.2.1 fixes (unreleased)

- **`superClass` option ([#14](https://github.com/miho/VMF-Text/issues/14)) — done.**
  A grammar-level `options { superClass = … }` flows through to both generated
  parsers (main parser inherits it via grammar pass-through; the synthesized
  unparser grammar re-emits it in `UnparserCodeGenerator`). Implemented in
  `a9a3c00`, exercised end-to-end by `examples/java24-roundtrip`. Hardened here:
  a rule-level `options { … }` block no longer clobbers the captured
  grammar-level `superClass` (`GrammarToModelListener.enterOptionsSpec`), with a
  regression test (`core` `GrammarOptionsTest`).
- **aarch64 miniclang test guard ([#23](https://github.com/miho/VMF-Text/issues/23)) — done.**
  `parseUnparseRunCodeTest` still runs the parse → unparse round trip on every
  platform, but its native compile-and-run step (vtcc/TCC 0.9.27, which can't link
  modern glibc on aarch64) is now skipped via JUnit `Assume` on aarch64/arm
  instead of failing the whole suite; amd64 (including CI) is unaffected. A real
  aarch64 fix needs a newer TCC upstream (`vtcc` / `tcc-dist`), out of this repo's
  scope.

## Phase 1 — Prove the differentiator (delivered)

- [x] **Round-trip showcase.** [`examples/java24-roundtrip`](examples/java24-roundtrip)
  parses a real Java 24 source file with the bundled `java24` grammar, proves the
  unparse is byte-identical, then applies one surgical model edit
  (`methodDecl.getMethodName().setText("render")`) and shows exactly one line
  changes — the demo no comparable tool reproduces generically. Prominent README
  "Round-Trip Fidelity" section + a runnable example ladder
  (ArrayLang → Java 8 → Java 24).
- [x] **Honest comparison page** — [`COMPARISON.md`](COMPARISON.md) (in the style
  of textX's comparison docs) covers VMF-Text vs Xtext, Langium, textX, plain
  ANTLR, JavaParser across grammar reuse, round-trip fidelity, model API depth
  (immutable views, change recording, undo, clone), editor/LSP story (VMF-Text:
  none built-in — documented bridge instead), platform and runtime weight; linked
  from the README lead.
- [x] **README refresh** — leads with the one-sentence value proposition,
  documents `autoLabel = true` for unlabeled/partially labeled grammars, and
  states the lexical-preservation guarantee precisely (exact for parsed models,
  conservative separator fallback for programmatically set values).

## Phase 2 — Harden the core (design work, scope after Phase 1)

Shipped so far: the written LSP stance ([LSP_INTEGRATION.md](LSP_INTEGRATION.md)),
path-keyed optional-presence state (`899ab47`), and — post-0.2.1, unreleased —
parser rule maps / model rewriting ([#1](https://github.com/miho/VMF-Text/issues/1),
see below) plus the `superClass` option fix
([#14](https://github.com/miho/VMF-Text/issues/14)). The still-open design items
are the **typed lexical-metadata migration**, the **formatter policy for
programmatically created models**, and **isolating Java-target ANTLR action
injection** (all marked below); these plus the 0.2.1 cleanup queue are tracked in
[#19](https://github.com/miho/VMF-Text/issues/19).

From `LEXICAL_PRESERVATION_ASSESSMENT.md`:

- **Complete the typed lexical metadata migration** *(still open)* — a typed
  `LexicalInfo` mirror already ships (see README "Typed Lexical Metadata"); retire
  the untyped payload-map fallback for a clean VMF-Jackson / JSON-schema story.
- **Path-keyed optional-presence state** — `OptionalState` only; positional
  `optionalSymbols` removed (0.2.1).
- **Formatter policy for programmatically created models** *(still open)* —
  pluggable pretty-printing / grammar-aware separators where exact preservation is
  undefined by construction.
- **Trivia splice + list-shape hints** — bare/parenthesized `T (',' T)*`,
  multi-list `ListShapeHint`, `separatorCount`, optional trailing `','?`,
  multi-alt hints, opener-after-trailer, bare model-typed lists,
  insert-at-0 padding/undo (0.2.1); irregular/context-sensitive separators
  still fall back (see assessment § Edit invalidation).
- **Original lexeme preservation** — type-mapped lexer spellings round-trip
  when semantic values are unchanged (0.2.1).
- **Optional null↔value + occurrenceIndex** — presence metadata updates;
  repeated optional paths use exact occurrence indices (0.2.1).
- **Unparsing guide** — [`docs/UNPARSING.md`](docs/UNPARSING.md) (0.2.1).
- **Written LSP stance** — document how to feed the generated model into an
  LSP4J-based server; deliberately do not build a language workbench.
- **Isolate Java-target ANTLR action injection** *(still open)* — keeps a future
  door open for other ANTLR targets without committing to them.

## Parser rule maps / model rewriting — landed (unreleased), closed [#1](https://github.com/miho/VMF-Text/issues/1)

`RuleMap()` flattens a single-alternative wrapper rule at its reference sites:
DSL (`TypeMapping.g4`) + model (`RuleMappings`) + a post-model type-redirect pass
(`RuleMapModelRewriter`) + parse-direction conversion + unparse-direction
reconstruction. Round-trip is byte-exact for transparent wrapper rules; see
[`docs/RULE_MAPS.md`](docs/RULE_MAPS.md). Feeds the broader model-flattening goal
in this phase. (Byte-exact preservation for token-bearing wrappers is a possible
follow-up.)

## Non-goals

- Competing with Xtext/Langium on bundled IDE/editor tooling.
- Multi-target ANTLR runtimes (C++/JS/Python generation).
- Projectional editing (MPS territory).

## Appendix: v0.2.0

Shipped 2026-07-12: https://github.com/miho/VMF-Text/releases/tag/v0.2.0

Consumption (works from Maven Central alone; the Portal listing is pending
approval and optional):

```gradle
// settings.gradle
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

```gradle
// build.gradle
plugins { id "eu.mihosoft.vmftext" version "0.2.0" }

vmfText {
    vmfVersion   = '0.2.10'
    antlrVersion = '4.13.2'
}
```
