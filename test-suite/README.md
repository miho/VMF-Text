# VMF-Text-Tests

## Executing the Tests

### Requirements

- Java: 1.8 <= version <= 11
- Internet connection (dependencies are downloaded automatically)
- IDE: [Gradle](http://www.gradle.org/) Plugin (not necessary for command line usage)

### IDE

Open the `VMF-Text-Tests` [Gradle](http://www.gradle.org/) project in your favourite IDE (tested with NetBeans 8.2 and IntelliJ 2018) and build it
by calling the `test` task.

### Command Line

Navigate to the [Gradle](http://www.gradle.org/) project (e.g., `path/to/VMF-Text-Test`) and enter the following command

#### Bash (Linux/macOS/Cygwin/other Unix shell)

    bash gradlew test
    
#### Windows (CMD)

    gradlew test

### Focused Lexical Preservation and Source-Bundle Checks

The suite includes focused regression tests for lexical preservation and
source-preserving persistence. During development these can be run separately:

```bash
./gradlew test --tests "eu.mihosoft.vmftext.tests.lexicalpreservation.*"
./gradlew test --tests "eu.mihosoft.vmftext.tests.sourcebundle.*"
```

Source-bundle tests exercise the generated `toSourceBundle()` and
`restoreFromSourceBundle()` parser helpers. They verify that matching source
text restores exact comments/whitespace, while mismatched or corrupted source
falls back to the stored semantic model and remains parseable.

### Viewing the Report

An HTML version of the test report is located in the build folder `build/reports/tests/test/index.html`.
