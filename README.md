# DMDirc3

[![Build Status](https://cloud.drone.io/api/badges/DMDirc/dmdirc3/status.svg)](https://cloud.drone.io/DMDirc/dmdirc3)
[![codecov](https://codecov.io/gh/DMDirc/dmdirc3/branch/master/graph/badge.svg)](https://codecov.io/gh/DMDirc/dmdirc3)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/dmdirc/localized.svg)](https://crowdin.com/project/dmdirc)

DMDirc3 is a rewrite of DMDirc using Kotlin and JavaFX.

TODO: Add more useful information here

## Development information

### Checking for updated dependencies

```console
$ ./gradlew checkUpdates
```

### Updating translation files

We'll automate this soon...

```console
$ find src/main/kotlin -name '*.kt' > FILES
$ xgettext --keyword=tr --language=java --add-comments --sort-output --omit-header -s -o translations/messages.pot --files-from=FILES
```
