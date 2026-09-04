import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.tasks.Jar

plugins {
    base
    id("com.diffplug.spotless") version "8.10.0" apply false
}

group = "io.github.crunchybubbles.geological"
version = "0.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        withSourcesJar()
        withJavadocJar()
    }

    extensions.configure<SpotlessExtension> {
        java {
            googleJavaFormat("1.36.0")
            formatAnnotations()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("-Duser.language=en", "-Duser.country=US", "-Duser.timezone=UTC")
        testLogging {
            events("failed", "skipped")
        }
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
    }

    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    tasks.matching { it.name == "spotlessJava" }.configureEach {
        outputs.doNotCacheIf("formatter worker classpaths must be initialized per project") { true }
    }
}

tasks.register("generateExampleAtlas") {
    group = "verification"
    description = "Generates Phase 1 atlas and Phase 2 material-state review artifacts."
    dependsOn(":atlas-cli:generateExampleAtlas", ":atlas-cli:generateMaterialReview")
}

tasks.register("measureAtlas") {
    group = "verification"
    description = "Runs the atlas/column runtime and memory measurement harness."
    dependsOn(":atlas-cli:measureAtlas")
}

tasks.register("benchmarkWorldgen") {
    group = "verification"
    description = "Runs the Phase 4 Overworld generation-order, seam, and server observation harness."
    dependsOn(":atlas-cli:benchmarkWorldgen")
}

tasks.register("explorationTelemetry") {
    group = "verification"
    description = "Runs the bounded Phase 5 exploration clue-sufficiency and travel-burden harness."
    dependsOn(":atlas-cli:explorationTelemetry")
}

// Serialize the formatter's isolated workers across modules; concurrent Windows clean builds can
// otherwise race while loading google-java-format's provisioned classes.
project(":atlas-cli").tasks.matching { it.name.startsWith("spotless") }.configureEach {
    mustRunAfter(project(":geology-core").tasks.matching { it.name.startsWith("spotless") })
}
