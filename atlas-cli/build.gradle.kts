plugins {
    application
}

dependencies {
    implementation(project(":geology-core"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass = "io.github.crunchybubbles.geological.cli.AtlasCli"
}

tasks.register<JavaExec>("generateExampleAtlas") {
    group = "verification"
    description = "Generates the deterministic Phase 1 query-core review packet."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("generate", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase1/example").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("generateMaterialReview") {
    group = "verification"
    description = "Generates the deterministic Phase 2 material-state review artifact."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("materials", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase2/example").get().asFile.absolutePath)
}

tasks.register<JavaExec>("measureAtlas") {
    group = "verification"
    description = "Measures atlas and Phase 1 column queries plus approximate live memory."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("measure", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase1/measurements").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}
