# DMDirc3

DMDirc3 is a rewrite of DMDirc using Kotlin and JavaFX.

TODO: Add more useful information here

## Development information

### Checking for updated dependencies

```console
$ ./gradlew checkUpdates
```

### Updating translation files

[![Crowdin](https://d322cqt584bo4o.cloudfront.net/dmdirc/localized.svg)](https://crowdin.com/project/dmdirc)

We'll automate this soon...

```console
$ find src/main/kotlin -name '*.kt' > FILES
$ xgettext --keyword=tr --language=java --add-comments --sort-output -j -o translations/messages.pot --files-from=FILES
```
