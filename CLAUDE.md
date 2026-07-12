# CLAUDE.md

Environment setup, build chain, and dependency details are shared by all
coding agents and live in AGENTS.md:

@AGENTS.md

## Quick reference

- Full build + test (mirrors CI): `sh ./build-and-test-all.sh`
- Tests only (core/plugin already published): `cd test-suite && sh ./gradlew test --no-daemon`
- Focused lexical preservation tests:
  `cd test-suite && sh ./gradlew test --tests "eu.mihosoft.vmftext.tests.lexicalpreservation.*" --no-daemon`

## Known WIP

- The java24 grammar port is unfinished: `test-suite/src/test/java/eu/mihosoft/vmftext/tests/java24/Test.java`
  targets model APIs the grammar does not generate yet (literal hierarchy,
  alt-labeled declarators) and is excluded from test compilation in
  `test-suite/build.gradle`. Do not "fix" the exclusion without finishing the
  grammar work; `IdentifierStringTest` in the same package covers what already
  works.
- Lexical preservation is exact for parsed models; programmatically edited
  values fall back to conservative separators (see
  `LEXICAL_PRESERVATION_ASSESSMENT.md`).
