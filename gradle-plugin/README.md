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
    vmfVersion   = '0.1'   // runtime version
    antlrVersion = '4.7.1' // runtime version
}
```
