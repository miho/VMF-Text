# VMF-Text-Gradle-Plugin

[ ![Download](https://api.bintray.com/packages/miho/VMF/VMF-Text-Gradle-Plugin/images/download.svg) ](https://bintray.com/miho/VMF/VMF-Text-Gradle-Plugin/_latestVersion)

Just add the plugin id to use this plugin (get this plugin from [here](https://plugins.gradle.org/plugin/eu.mihosoft.vmftext)):

```gradle
plugins {
  id "eu.mihosoft.vmftext" version "0.1.2.6" // use latest version
}
```

and (optionally) configure VMF-Text:

```gradle
vmfText {
    vmfVersion   = '0.1.1'   // runtime version
    antlrVersion = '4.7.1' // runtime version
}
```

## Building the VMF-Text Gradle Plugin

### Requirements

- Java >= 1.8
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
