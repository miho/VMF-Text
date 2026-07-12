# CLAUDE.md

Environment setup, build chain, and dependency details are shared by all
coding agents and live in AGENTS.md:

@AGENTS.md

## Quick reference

- Full build + test (mirrors CI): `sh ./build-and-test-all.sh`
- Tests only (core/plugin already published): `cd test-suite && sh ./gradlew test --no-daemon`
- Focused lexical preservation tests:
  `cd test-suite && sh ./gradlew test --tests "eu.mihosoft.vmftext.tests.lexicalpreservation.*" --no-daemon`

## Known limitations

- Java string templates are intentionally unsupported because the preview
 feature was withdrawn after Java 22 and is not part of Java 24.
- Lexical preservation is exact for parsed models; programmatically edited
  values fall back to conservative separators (see
  `LEXICAL_PRESERVATION_ASSESSMENT.md`).
