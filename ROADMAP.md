# VMF-Text Roadmap

*Last updated: 2026-07-13*

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

### 0.2.1 / 0.2.11 build cleanup queue

Small papercuts found during the release, none user-visible — tracked in
[#19](https://github.com/miho/VMF-Text/issues/19):

- Set `project.group` and `project.version` at project level in **both**
  gradle-plugin builds (VMF and VMF-Text) so `publishPlugins` from a release
  tag no longer needs `-Pgroup=... -Pversion=...` overrides.
- Align the ANTLR tool and runtime versions in the generated builds
  (currently a harmless "tool 4.13.2 vs runtime 4.11.1" warning during
  generation).
- `gradle-plugin/gradle/project-info.gradle` hardcodes `versionId`; read it
  from `config/common.properties` like `core` does.
- Publish the plugin marker to Central from the plugin build itself (the
  0.2.0/0.2.10 markers were produced by a standalone POM-only publisher
  project).
- Verify the Plugin Portal listings once Gradle approves the group
  migration (notification arrives by email).

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

Shipped so far: the written LSP stance ([LSP_INTEGRATION.md](LSP_INTEGRATION.md))
and path-keyed optional-presence state (`899ab47`). The remaining items below
are tracked in [#19](https://github.com/miho/VMF-Text/issues/19) together with
the 0.2.1 cleanup queue.

From `LEXICAL_PRESERVATION_ASSESSMENT.md`:

- **Complete the typed lexical metadata migration** — a typed `LexicalInfo`
  mirror already ships (see README "Typed Lexical Metadata"); retire the
  untyped payload-map fallback for a clean VMF-Jackson / JSON-schema story.
- **Path-keyed optional-presence state** — remove the positional coupling of
  `optionalSymbols`.
- **Formatter policy for programmatically created models** — pluggable
  pretty-printing / grammar-aware separators where exact preservation is
  undefined by construction.
- **Trivia splice + list-shape hints** — bare/parenthesized `T (',' T)*`,
  multi-list rules via codegen `ListShapeHint`, non-comma single-terminal
  separators, opener-after-trailer, bare model-typed lists, insert-at-0
  padding after sibling trailers (0.2.1); multi-token separators still fall
  back (see assessment § Edit invalidation).
- **Original lexeme preservation** — type-mapped lexer spellings round-trip
  when semantic values are unchanged (0.2.1).
- **Optional null↔value + occurrenceIndex** — presence metadata updates;
  repeated optional paths use exact occurrence indices; `optionalSymbols`
  soft-deprecated (0.2.1).
- **Written LSP stance** — document how to feed the generated model into an
  LSP4J-based server; deliberately do not build a language workbench.
- **Isolate Java-target ANTLR action injection** — keeps a future door open
  for other ANTLR targets without committing to them.

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
