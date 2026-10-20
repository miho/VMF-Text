# Releasing VMF-Text

The canonical, repeatable process for cutting a VMF-Text release. Higher-level
phase tracking lives in [`ROADMAP.md`](../ROADMAP.md); the per-release checklist
for the version in flight lives under [`docs/releases/`](releases/). This guide is
the *how*; those are the *what / when*.

VMF-Text ships three artifacts from one version
([`config/common.properties`](../config/common.properties) →
`publication.version`):

- `eu.mihosoft.vmf:vmf-text` — core
- `eu.mihosoft.vmf:vmf-text-gradle-plugin` — Gradle plugin
- Gradle Plugin Portal listing `eu.mihosoft.vmftext` (+ its Central plugin marker)

## Prerequisites

Release credentials are **not** stored in this repository or on the build
machine — they are kept offline and passed per invocation as Gradle `-P`
properties (see
[ROADMAP → Publishing prerequisites](../ROADMAP.md#publishing-prerequisites)):

- Maven Central Portal: `mavenCentralUsername` / `mavenCentralPassword`
- GPG signing: `signing.keyId` / `signing.password` / `signing.secretKeyRingFile`
- Gradle Plugin Portal: `gradle.publish.key` / `gradle.publish.secret`

Also required: the JDK 21 toolchain (see [`AGENTS.md`](../AGENTS.md)), a clean
working tree on `main`, and CI green on the release commit.

## 1. Prep (on a `release/<version>` branch)

1. Bump `publication.version` in `config/common.properties`.
2. Align version references: `README.md`, `gradle-plugin/README.md`, and the
   `examples/**` plugin ids. Confirm the pinned VMF and ANTLR versions
   (currently VMF `0.2.10`, ANTLR `4.13.2`).
3. Finalize the `CHANGELOG.md` entry — date it and list Added / Changed / Fixed.
4. Add the ROADMAP "Ship &lt;version&gt;" section and the per-release checklist at
   `docs/releases/<version>.md`.
5. Open a PR, get CI green, merge to `main`.

## 2. Validate

```bash
sh ./build-and-test-all.sh   # mirrors CI: core → gradle-plugin → test-suite
```

CI runs the same chain on ubuntu + windows, plus the ArrayLang example resolved
from `mavenLocal`. Do not publish unless this is green on the release commit.

Then rehearse the publish itself with the signing key but without Central /
Portal credentials — in `core` and in `gradle-plugin`:

```bash
sh ./gradlew publishAllPublicationsToBuildDirRepository \
  -Psigning.keyId=… -Psigning.password=… -Psigning.secretKeyRingFile=… --no-daemon
```

`build/artifacts/` then holds exactly what would be uploaded. For the plugin
that must be `eu/mihosoft/vmf/vmf-text-gradle-plugin/<version>/` (jar, sources,
javadoc, pom, module) **and**
`eu/mihosoft/vmftext/eu.mihosoft.vmftext.gradle.plugin/<version>/` (the marker
pom, depending on `vmf-text-gradle-plugin`), every file with an `.asc`. No
`-portal` artifact may appear (that was the 0.2.1 defect).

## 3. Publish artifacts (needs the offline credentials)

Mirrors
[ROADMAP → Publish commands (reference)](../ROADMAP.md#publish-commands-reference).

```bash
# 1) core → Maven Central
cd core
sh ./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon
sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft --no-daemon
# then press "Publish" on https://central.sonatype.com/publishing/deployments
# (or pass -PpublishingType=automatic to skip the manual click)

# 2) gradle-plugin → Maven Central + Plugin Portal
cd ../gradle-plugin
# publishes the plugin jar AND the plugin marker (both created by
# java-gradle-plugin) — never publish only 'mavenJava'/the jar
sh ./gradlew publishAllPublicationsToMavenCentralRepository --no-daemon
sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft --no-daemon
sh ./gradlew publishPlugins --no-daemon
# -Pgroup=eu.mihosoft.vmf -Pversion=<version> was required in 0.2.1; since 0.2.2
# group/version are set before any plugin reads them, so pass the flags only if
# publishPlugins reports 'unspecified'
```

## 4. Tag + GitHub release

```bash
git tag -a v<version> -F <annotated-message>   # summary + Added/Changed/Fixed
git push origin v<version>
gh release create v<version> --target main \
  --title "v<version> — <headline>" --notes-file <notes>
# add --draft to stage the notes first, then publish once artifacts are live
```

The tag and draft can be cut *before* the artifacts are published; publish the
GitHub release only once the Central + Portal artifacts resolve.

## 5. Verify

- Central: `eu.mihosoft.vmf:vmf-text:<version>` and
  `…:vmf-text-gradle-plugin:<version>` visible on `repo1.maven.org`.
- Plugin marker resolves — `plugins { id "eu.mihosoft.vmftext" version "<version>" }`
  from a clean consumer whose `pluginManagement` lists `mavenCentral()` **only**
  (no `gradlePluginPortal()` fallback — that fallback hid the missing 0.2.1
  marker). `repo1.maven.org` must show
  `eu/mihosoft/vmftext/eu.mihosoft.vmftext.gradle.plugin/<version>/`.
- Plugin Portal listing approved (secondary channel; can lag Gradle's review).
- Smoke-test a clean consumer project against Central-only resolution.

## 6. Post-publish

- Flip the `CHANGELOG.md` entry's "Publish (pending)" note to "Published"
  (coordinates + tag link), mirroring the previous release entry.
- Check off the ROADMAP "Ship &lt;version&gt;" boxes.
- Bump the plugin pin in `examples/java8-roundtrip` and
  `examples/java24-roundtrip` (CI's Central-only smoke test of the published
  artifacts) and the `examples/README.md` table to the new version; CI must stay
  green.
- Close the release tracking issue.
- Delete the merged `release/<version>` branch (local + `origin`); the tag
  preserves the commit.
