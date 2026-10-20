# VMF-Text Roadmap

*Last updated: 2026-10-20*

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
[#19](https://github.com/miho/VMF-Text/issues/19) (closed — most items done in
0.2.1):

- ~~Set `project.group` and `project.version` at project level in **both**
  gradle-plugin builds (VMF and VMF-Text)~~ — already done for VMF-Text at
  0.2.0; VMF's own plugin build is tracked in that sibling repo.
- ~~Align the ANTLR tool and runtime versions in the generated builds~~ — the
  "ANTLR Tool version 4.11.1 ... does not match the current runtime version
  4.13.2" warning still shows for consumers of the **0.2.0** plugin (seen in the
  showcases while they were pinned to 0.2.0); with 0.2.2 it is gone
  (verified 2026-10-20, Central-only `java24-roundtrip` run).
- ~~`gradle-plugin/gradle/project-info.gradle` hardcodes `versionId`~~ —
  now reads from `config/common.properties` like `core` does.
- ~~Publish the plugin marker to Central from the plugin build itself~~ —
  resolved for 0.2.1: fixing the `pluginMaven` GAV collision let
  `publishPlugins` sign and publish the plugin + marker directly from this
  build.
- ~~Verify the Plugin Portal listings once Gradle approves the group
  migration~~ — both listings are live (`eu.mihosoft.vmftext` 0.2.1,
  `eu.mihosoft.vmf` 0.2.11; checked 2026-10-20).
- ~~`publishPlugins` from a release tag needed
  `-Pgroup=eu.mihosoft.vmf -Pversion=<version>` in 0.2.1~~ — since 0.2.2
  `gradle/publishing.gradle` sets `group`/`version` before any plugin reads
  them; confirmed 2026-10-20 (0.2.2 published to the Portal without the flags).
- 0.2.1 shipped **without a plugin marker on Maven Central** (the Portal
  marker also pointed at a `-portal` artifact that only the Portal hosts), so
  Central-only resolution of the plugin only ever worked for 0.2.0. Fixed in
  0.2.2 (see CHANGELOG); Central-only resolution is now a CI smoke test.

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
# publishes BOTH publications java-gradle-plugin creates: the plugin jar
# (eu.mihosoft.vmf:vmf-text-gradle-plugin) and the plugin marker
# (eu.mihosoft.vmftext:eu.mihosoft.vmftext.gradle.plugin) — 0.2.1 published
# only the jar, which is why Central-only resolution broke
sh ./gradlew publishAllPublicationsToMavenCentralRepository --no-daemon
sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft --no-daemon
sh ./gradlew publishPlugins --no-daemon
# (no -Pgroup/-Pversion flags needed since 0.2.2)
```

Rehearse first, without credentials for Central/Portal but with the signing
key: `sh ./gradlew publishAllPublicationsToBuildDirRepository -Psigning.keyId=…
-Psigning.password=… -Psigning.secretKeyRingFile=…` writes exactly what would
be uploaded (jar, sources, javadoc, pom, module, marker pom — each with `.asc`)
to `build/artifacts/` for inspection.

## Ship 0.2.3 (GitHub release 2026-10-20, not on Maven Central)

The C-trial batch: `X+` lists, lifted block alternatives (auto-labeled and
hand-labeled grammars), literal naming, strict parsing, `Root(rule=…)`,
list-growth separators, VMF `0.2.11`. Released as
[v0.2.3](https://github.com/miho/VMF-Text/releases/tag/v0.2.3) with a signed
Maven-repository archive attached; deliberately **not** published to Maven
Central to keep the shared monthly release budget for other projects. The same
content is on the Central snapshot repository as `0.2.3-SNAPSHOT`; `main`
continues as `0.2.4-SNAPSHOT`. Checklist:
[`docs/releases/0.2.3.md`](docs/releases/0.2.3.md).

## Ship 0.2.2 (shipped 2026-10-20)

Model rewriting + option/robustness fixes, **plus** auto-naming polish and
RuleMap token-bearing wrappers from [#33](https://github.com/miho/VMF-Text/pull/33)
folded into this release (one Maven Central publish; no separate 0.2.3), **plus**
the Maven Central plugin-marker fix and the Gradle 10 build cleanup. Details:
[`CHANGELOG.md`](CHANGELOG.md), [`docs/releases/0.2.2.md`](docs/releases/0.2.2.md).
Released as [v0.2.2](https://github.com/miho/VMF-Text/releases/tag/v0.2.2)
(tracking issue [#29](https://github.com/miho/VMF-Text/issues/29), closed).

- [x] **Parser rule maps / model rewriting**
      ([#1](https://github.com/miho/VMF-Text/issues/1)) — `RuleMap()` flattens
      single-alt wrapper rules at their reference sites; byte-exact round trip for
      transparent wrappers (dedicated section below +
      [`docs/RULE_MAPS.md`](docs/RULE_MAPS.md)).
- [x] **`superClass` option** ([#14](https://github.com/miho/VMF-Text/issues/14))
      — a grammar-level `options { superClass = … }` flows through to both
      generated parsers (main parser via grammar pass-through; the synthesized
      unparser grammar re-emits it in `UnparserCodeGenerator`) and is no
      longer clobbered by a later rule-level `options { … }` block
      (`GrammarToModelListener.enterOptionsSpec`; regression `core`
      `GrammarOptionsTest`).
- [x] **aarch64 miniclang test guard**
      ([#23](https://github.com/miho/VMF-Text/issues/23)) — the native
      compile-and-run step (vtcc/TCC 0.9.27, which can't link modern glibc on
      aarch64) is skipped via JUnit `Assume` instead of failing the suite; the
      parse → unparse round trip still runs on every platform, amd64 (incl. CI)
      unaffected. A real aarch64 fix needs a newer TCC upstream
      (`vtcc` / `tcc-dist`), out of this repo's scope.
- [x] **Automatic naming polish** ([#31](https://github.com/miho/VMF-Text/issues/31)
      / [#33](https://github.com/miho/VMF-Text/pull/33)) — plain lower-camel
      names, keyword-context duplicates, `…y`→`…ies` plurals, inferred-name
      report under `build/reports/vmf-text/`.
- [x] **RuleMap token-bearing wrapper fidelity**
      ([#32](https://github.com/miho/VMF-Text/issues/32) /
      [#33](https://github.com/miho/VMF-Text/pull/33)) — store/restore wrapper
      shell `LexicalInfo` for byte-exact round trip of `( 1 )`-style wrappers.
- [x] Version → `0.2.2` (`config/common.properties`); `README.md`,
      `gradle-plugin/README.md`, and the aligned `examples/**` plugin ids bumped.
      VMF stays `0.2.10`, ANTLR `4.13.2`. (Showcases `java8`/`java24` stay pinned
      to a Central-published version so `./gradlew run` works out of the box.)
- [x] Release prep + #33 merged to `main`; CI green; GitHub release drafted
      ([#29](https://github.com/miho/VMF-Text/issues/29))
- [x] **Maven Central plugin marker fix + Gradle 10 build cleanup** (2026-10-20)
      — `pluginMaven` + marker published under the standard coordinates with
      Central POM metadata (0.2.1 had no Central marker, see cleanup queue
      above); Gradle 9.8.2 wrapper, deprecation-free build scripts, license
      plugin removed, Windows CI leg required, Central-only CI smoke test.
- [x] Retarget annotated tag `v0.2.2` to the release commit on `main`; refresh
      draft release notes; close #30–#32 (closed 2026-09-17)
- [x] Full-chain validation green (`sh ./build-and-test-all.sh` locally on
      2026-10-20; PR CI runs the same chain on ubuntu + windows, plus the
      ArrayLang example from `mavenLocal`).
- [x] Published 2026-10-20: `eu.mihosoft.vmf:vmf-text:0.2.2`,
      `vmf-text-gradle-plugin:0.2.2` **and** the marker
      `eu.mihosoft.vmftext:eu.mihosoft.vmftext.gradle.plugin:0.2.2` on Maven
      Central (two Portal deployments, validated with no errors), the plugin on
      the Gradle Plugin Portal (`eu.mihosoft.vmftext` 0.2.2 — `publishPlugins`
      needed no `-Pgroup`/`-Pversion` flags); Central-only resolution verified;
      GitHub release published.

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

## Phase 2 — Harden the core (mostly delivered in 0.2.1)

Shipped: the written LSP stance ([LSP_INTEGRATION.md](LSP_INTEGRATION.md)),
path-keyed optional-presence state, and — in 0.2.2 — parser rule maps / model
rewriting ([#1](https://github.com/miho/VMF-Text/issues/1)) plus the `superClass`
option fix ([#14](https://github.com/miho/VMF-Text/issues/14)). Phase 2 follow-ups
from [#19](https://github.com/miho/VMF-Text/issues/19) landed in **0.2.1**:

- [x] **Complete the typed lexical metadata migration** — typed `LexicalInfo`
      (`TriviaPiece`, `OptionalState`, …); untyped `vmf-text:` payload fallback
      retired; `CodeElement.getPayload()` deprecated for VMF-Text internals.
- [x] **Path-keyed optional-presence state** — `OptionalState` only; positional
      `optionalSymbols` removed (0.2.1).
- [x] **Formatter policy for programmatically created models** — pluggable
      `ProgrammaticSeparatorPolicy`; default matches the previous conservative
      separator fallback.
- [x] **Trivia splice + list-shape hints** — bare/parenthesized `T (',' T)*`,
      multi-list `ListShapeHint`, `separatorCount`, optional trailing `','?`,
      multi-alt hints, opener-after-trailer, bare model-typed lists,
      insert-at-0 padding/undo (0.2.1); irregular/context-sensitive separators
      still fall back (see assessment § Edit invalidation).
- [x] **Original lexeme preservation** — type-mapped lexer spellings round-trip
      when semantic values are unchanged (0.2.1).
- [x] **Optional null↔value + occurrenceIndex** — presence metadata updates;
      repeated optional paths use exact occurrence indices (0.2.1).
- [x] **Unparsing guide** — [`docs/UNPARSING.md`](docs/UNPARSING.md) (0.2.1).
- [x] **Written LSP stance** — document how to feed the generated model into an
      LSP4J-based server; deliberately do not build a language workbench.
- [x] **Isolate Java-target ANTLR action injection** — `AntlrTargetOptionalStateProvider`
      / `JavaAntlrOptionalStateProvider` (0.2.1); door left open for other
      targets without committing to them (explicit non-goal).

### Folded into 0.2.2 via [#33](https://github.com/miho/VMF-Text/pull/33)

(Originally tracked as post-0.2.2 follow-ups; kept in the same Central publish.)

- [x] **Docs: sync ROADMAP to actual status**
      ([#30](https://github.com/miho/VMF-Text/issues/30)) — this file.
- [x] **Automatic naming: richer inference + inferred-name report**
      ([#31](https://github.com/miho/VMF-Text/issues/31)) — plain lower-camel
      rule/token names (no `Node` suffix), keyword-context disambiguation for
      duplicates (e.g. `'else' statement` → `elseStatement`), write the
      auto-label report to the build directory, core + test-suite coverage.
      Background: assessment § "Recommended next design step: automatic naming".
- [x] **RuleMap: byte-exact round-trip for token-bearing wrappers**
      ([#32](https://github.com/miho/VMF-Text/issues/32)) — store/restore wrapper
      shell `LexicalInfo` on the flattened target so `( 1 )`-style wrappers
      round-trip exactly. Transparent wrappers remain byte-exact as before.

### Explicitly deferred (needs CST / per-gap model — not 0.2.x splice polish)

From `LEXICAL_PRESERVATION_ASSESSMENT.md`:

- Separators that differ by **index** (Oxford: `,` then `, and`)
- Unlabeled alt separators (`(('+'|'-') t)*` with no model property)
- Optional separator **per gap** (`(','? item)*`)
- Arbitrary cross-rule structural moves with perfect outer-container fidelity

## Parser rule maps / model rewriting — landed in 0.2.2, closed [#1](https://github.com/miho/VMF-Text/issues/1)

`RuleMap()` flattens a single-alternative wrapper rule at its reference sites:
DSL (`TypeMapping.g4`) + model (`RuleMappings`) + a post-model type-redirect pass
(`RuleMapModelRewriter`) + parse-direction conversion + unparse-direction
reconstruction. Round-trip is byte-exact for transparent wrapper rules; see
[`docs/RULE_MAPS.md`](docs/RULE_MAPS.md). Token-bearing wrapper fidelity
([#32](https://github.com/miho/VMF-Text/issues/32) /
[#33](https://github.com/miho/VMF-Text/pull/33)) is part of this same 0.2.2
ship.

## After 0.2.2 (queue)

- [x] Bump VMF to `0.2.11` (released 2026-07-26: error-message and vmf-jackson /
  JSON-schema fixes) — done on `main` after 0.2.2. VMF's own Gradle 10 fixes
  (VMF #79, merged 2026-08-03) are **not** in 0.2.11, so the one remaining
  deprecation warning in the `core` build stays until a later VMF release; the
  VMF 0.2.11 plugin *marker* is also missing from Maven Central (same defect
  VMF-Text had in 0.2.1) — fix that on the VMF side.
- **Release budget (2026-10-20):** Maven Central limits publishing per
  organization and calendar month (~7 release events, ~1,167 files, ~78 MB;
  Usage Center is the source of truth) — not a lifetime budget, but shared by
  every `eu.mihosoft` project. One VMF-Text release costs 1 event with the
  one-deployment script. **Verified the same day: snapshot uploads do not
  count** — two `0.2.3-SNAPSHOT` pushes left the organization's numbers
  unchanged. Pre-release sharing therefore goes through Central `-SNAPSHOT`s
  (90-day retention, `main` always carries the next `-SNAPSHOT`); Maven Central
  releases are batched and spent deliberately. Details:
  [#41](https://github.com/miho/VMF-Text/issues/41),
  [`docs/RELEASING.md`](docs/RELEASING.md).
- **Next phase (decided 2026-10-20), in this order — tracked in
  [#37](https://github.com/miho/VMF-Text/issues/37) (literal-only blocks in
  hand-labeled grammars), [#38](https://github.com/miho/VMF-Text/issues/38)
  (operator-literal names), [#40](https://github.com/miho/VMF-Text/issues/40)
  (mixed-mode formatting), then [#39](https://github.com/miho/VMF-Text/issues/39)
  (`tokenVocab`):**
  1. **Second big language, unlabeled (evidence first) — DONE 2026-10-20,
     C.** Write-up and reproducible project:
     [`experiments/c-grammar-trial`](experiments/c-grammar-trial/README.md).
     Result on the classic grammars-v4 C grammar with `autoLabel`: generation
     and parsing work unaided (86 rules, 58 s to generate + compile), 14 of
     32 sample programs round-trip byte-exact, 10 cannot be unparsed, 8 come
     back wrong — all traced to a short list of grammar shapes, none to the
     lexical layer. Follow-ups, in order of samples unblocked per unit of
     work — **(1)–(4) landed the same day** (CHANGELOG "Unreleased"; the
     grammar is now 27/32 exact with 0 wrong outputs, the rest are real
     syntax errors): (1) ✅ `X+`/`X*` single-element lists unparse;
     (2) ✅ nested alternative blocks are lifted to typed sub classes and every
     literal choice is captured; (3) ✅ syntax errors fail loudly by default;
     (4) ✅ `Root(rule=…)` + compile-clean alt-labeled root; (5) names for
     operator literals (`operators`, `symbols2…`) — still open, cosmetic;
     (6) `tokenVocab` / split grammars, since grammars-v4 HEAD is split and
     predicate-driven — still open. Unparse performance was a non-issue; the
     per-gap separator model is *not* needed for this grammar. Issues:
     [#37](https://github.com/miho/VMF-Text/issues/37) (lifting for
     hand-labeled grammars, found via MiniJava), [#38](https://github.com/miho/VMF-Text/issues/38),
     [#39](https://github.com/miho/VMF-Text/issues/39).
  2. **Mixed-mode formatting for programmatic elements — DONE 2026-10-20
     ([#40](https://github.com/miho/VMF-Text/issues/40)).** The
     `ProgrammaticSeparatorPolicy` gets a `FormattingContext` and is told about
     every terminal; `BlockIndentSeparatorPolicy` /
     `Formatter.newBlockIndentFormatter()` prints programmatic or edited parts
     with line breaks and block indentation while parsed bytes stay exact;
     `programmaticRanges()` reports the affected character / line ranges for
     external formatters. `JavaStyleFormatter` deleted. Test: `mixedformat`.
  Background: `LEXICAL_PRESERVATION_ASSESSMENT.md`, `docs/UNPARSING.md` §6–§7.

## Non-goals

- Competing with Xtext/Langium on bundled IDE/editor tooling.
- Multi-target ANTLR runtimes (C++/JS/Python generation).
- Projectional editing (MPS territory).

## Appendix: v0.2.0

Shipped 2026-07-12: https://github.com/miho/VMF-Text/releases/tag/v0.2.0

Consumption (works from Maven Central alone for 0.2.0 and 0.2.2+ — 0.2.1
needs `gradlePluginPortal()`, see the cleanup queue; the Portal listing is live
and optional):

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
