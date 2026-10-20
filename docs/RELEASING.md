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

## Maven Central publishing limits (enforced since 2026-10-01)

Maven Central limits publishing **per organization and per calendar month**
(the `eu.mihosoft` organization is shared by VMF-Text, VMF and every other
`eu.mihosoft.*` namespace). The published thresholds are roughly **7 release
events, ~1,167 files and ~78 MB per month**; the
[Usage Center](https://central.sonatype.com/publishing/usage) is the source of
truth and shows the current month, warnings at 75 % / 90 %, and trends. A
release event is one publish operation (one Portal deployment); every file
counts, including POMs, `.asc` signatures, checksums, sources and javadoc jars.
Exceeding a threshold starts a 30-day grace period; afterwards only releases
that would keep the organization over the limit are rejected. Published
artifacts are never removed. Details:
<https://central.sonatype.org/publish/maven-central-publishing-limits/>.

Consequences for this project:

- One VMF-Text release as published in 0.2.2 costs **2 release events** (core
  and gradle-plugin were handed off as separate deployments) and about 76
  files, well under 2 MB. Uploading both modules into the staging repository
  *before* the single `releaseToCentralPortal` hand-off makes it **one** event
  (see §3).
- Release only what is worth sharing; do not release for documentation or
  metadata changes. Pre-release builds go through the snapshot channel below.
- Downloads are not counted.

### Pre-release channel: Central SNAPSHOTs

The Central Portal hosts `-SNAPSHOT` versions at
<https://central.sonatype.com/repository/maven-snapshots/> (enable "SNAPSHOTs"
on the namespace at <https://central.sonatype.com/publishing/namespaces>
first). Snapshots are not validated or signed, are available immediately, are
meant to be overwritten, and are **deleted after 90 days**. Whether they count
toward the monthly limits is not documented; check the Usage Center after the
first push. The build already targets that URL for `-SNAPSHOT` versions
(`gradle/publishing.gradle`, repository `MavenCentral`):

```bash
# config/common.properties → publication.version = 0.2.3-SNAPSHOT
cd core          && sh ./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon <creds>
cd ../gradle-plugin && sh ./gradlew publishAllPublicationsToMavenCentralRepository --no-daemon <creds>
```

Consumers add the snapshot repository (Gradle: a `maven { url = "…/maven-snapshots/" }`
entry in `pluginManagement.repositories` and `repositories`, ideally with a
`content { includeGroup "eu.mihosoft.vmf"; includeGroup "eu.mihosoft.vmftext" }`
filter) and use the `-SNAPSHOT` version.

Fallback if snapshots turn out to count: a static Maven repository on GitHub
Pages (publish with `publishAllPublicationsToBuildDirRepository`, commit
`build/artifacts/` to a `maven-repo` branch) — free, unlimited, anonymous
reads, works for the plugin marker — at the cost of discoverability.

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
   (currently VMF `0.2.11`, ANTLR `4.13.2`).
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

To spend **one** release event instead of two, upload *both* modules into the
open staging repository first and hand off once: run the two `publish…ToMavenCentralRepository`
tasks (core, then gradle-plugin), look up the staging key, then a single
`releaseToCentralPortal`. The deployment then carries all three purls
(`vmf-text`, `vmf-text-gradle-plugin`, the plugin marker). The commands below
show the per-module form used for 0.2.2.

```bash
# 1) core → Maven Central
cd core
sh ./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon
# look up the open staging repository key first (see below) and pass it:
sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft \
  -PrepositoryKey=<key> -PpublishingType=automatic --no-daemon
# (omit -PpublishingType to review + press "Publish" on
#  https://central.sonatype.com/publishing/deployments instead)

# 2) gradle-plugin → Maven Central + Plugin Portal
cd ../gradle-plugin
# publishes the plugin jar AND the plugin marker (both created by
# java-gradle-plugin) — never publish only 'mavenJava'/the jar
sh ./gradlew publishAllPublicationsToMavenCentralRepository --no-daemon
sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft --no-daemon
sh ./gradlew publishPlugins --no-daemon
# (no -Pgroup/-Pversion flags needed since 0.2.2 — group/version come from
#  config/common.properties before any plugin reads them)
```

**Staging repository key.** The `releaseToCentralPortal` fallback that hands off
the namespace's *default* repository failed with `HTTP 400` for release
uploads in 0.2.1, so always pass the key explicitly. Find it with the same
token (Bearer = base64 of `mavenCentralUsername:mavenCentralPassword`):

```
GET https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?state=open
→ {"repositories":[{"key":"<key>","state":"open","portal_deployment_id":…}]}
```

The default slot is reused for the next module, so repeat the lookup before
**each** hand-off. After the hand-off the entry moves to `state=released` and
carries the `portal_deployment_id`; the authoritative state and the exact
purls of a deployment come from

```
POST https://central.sonatype.com/api/v1/publisher/status?id=<portal_deployment_id>
→ {"deploymentState":"PUBLISHING|PUBLISHED|FAILED","purls":[…],"errors":{}}
```

`PUBLISHING` can last a while; `repo1.maven.org` serves the files some minutes
after `PUBLISHED`.

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
