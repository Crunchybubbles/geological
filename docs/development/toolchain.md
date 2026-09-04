# Toolchain and dependency policy

The build pins Java 21 bytecode, Gradle 9.7.1, Spotless 8.10.0, google-java-format 1.36.0, JUnit 6.1.0, and Jackson Databind 3.2.1. These versions were checked on 2026-08-30 against the Gradle release/compatibility documentation, Gradle Plugin Portal, google-java-format releases, JUnit's current release metadata, and FasterXML's Jackson release records.

Primary version sources: [Gradle releases](https://gradle.org/releases/), [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html), [Spotless plugin portal](https://plugins.gradle.org/plugin/com.diffplug.spotless), [google-java-format releases](https://github.com/google/google-java-format/releases), [JUnit releases](https://github.com/junit-team/junit-framework/releases), [Jackson 3.2 releases](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.2), and [Jackson Databind 3.2.1 on Maven Central](https://central.sonatype.com/artifact/tools.jackson.core/jackson-databind/3.2.1).

The build uses the checked-in wrapper, central repository declarations, exact dependency versions, dependency lock files, reproducible JAR settings, UTF-8/UTC locale defaults, formatting checks, and `javac -Xlint:all -Werror`. CI runs the same wrapper on Linux and Windows with Temurin 21.

Only the internal `spotlessJava` transform is excluded from Gradle's build cache and serialized across the two modules. This avoids reusing a provisioned formatter-worker classpath that proved unreliable during cached parallel Windows clean builds; compilation, tests, and packaging remain cacheable.

Jackson is an implementation dependency only of `geology-core` and is confined to strict JSON authoring ingestion. Public geology/query APIs expose project-owned types, and runtime atlas queries do not parse JSON. Jackson 3 is the current line recommended by FasterXML for new projects; all transitive artifacts are locked and SHA-256 verified by Gradle.

The platform-neutral `geology-core` and `atlas-cli` modules remain free of Minecraft and loader
dependencies. The first loader adapter is now a separate `neoforge-adapter` module. It pins
Minecraft `1.21.1`, NeoForge `21.1.249`, ModDevGradle `2.0.146`, Parchment `2024.11.17`, and
Java 21 in `neoforge-adapter/gradle.properties`; its resolved configurations are checked in via
`neoforge-adapter/gradle.lockfile` and `gradle/verification-metadata.xml`. The settings-level
NeoForged repositories plugin keeps the loader repository available without weakening Gradle's
central repository policy. The adapter currently exposes only the entry point and a dimension/
chunk-to-core identity bridge; terrain, block registration, and world-preset ownership remain
future slices. See the [official ModDevGradle documentation](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)
and [NeoForge 21.1.249 artifacts](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.249/)
for the upstream toolchain references.

To refresh dependency locks after an intentional version change:

```shell
./gradlew dependencies --write-locks
```

Run `./gradlew spotlessApply` after source edits and `./gradlew build` before submitting a change.
