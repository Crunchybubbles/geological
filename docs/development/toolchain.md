# Toolchain and dependency policy

The Phase 0 build pins Java 21 bytecode, Gradle 9.7.1, Spotless 8.10.0, google-java-format 1.36.0, and JUnit 6.1.0. These versions were checked on 2026-08-30 against the Gradle release/compatibility documentation, Gradle Plugin Portal, google-java-format releases, and JUnit's current release metadata.

Primary version sources: [Gradle releases](https://gradle.org/releases/), [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html), [Spotless plugin portal](https://plugins.gradle.org/plugin/com.diffplug.spotless), [google-java-format releases](https://github.com/google/google-java-format/releases), and [JUnit releases](https://github.com/junit-team/junit-framework/releases).

The build uses the checked-in wrapper, central repository declarations, exact dependency versions, dependency lock files, reproducible JAR settings, UTF-8/UTC locale defaults, formatting checks, and `javac -Xlint:all -Werror`. CI runs the same wrapper on Linux and Windows with Temurin 21.

Minecraft and NeoForge dependencies are intentionally absent. Phase 0 is a standalone proof, and a dormant loader dependency would weaken the platform boundary. The [official NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/) currently publishes the selected 21.1 line; its exact patch and the matching supported build plugin will be verified and locked when the actual adapter scaffold begins.

To refresh dependency locks after an intentional version change:

```shell
./gradlew dependencies --write-locks
```

Run `./gradlew spotlessApply` after source edits and `./gradlew build` before submitting a change.
