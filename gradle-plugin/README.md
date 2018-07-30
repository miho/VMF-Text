# VMF-Text-Gradle-Plugin

Get this plugin from here: https://plugins.gradle.org/plugin/eu.mihosoft.vmftext

Just add the plugin id to use this plugin:

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
