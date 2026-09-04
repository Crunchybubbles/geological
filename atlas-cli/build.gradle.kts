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

tasks.register<JavaExec>("benchmarkWorldgen") {
    group = "verification"
    description = "Measures deterministic Overworld generation order, seams, and server runtime observations."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("worldgen-benchmark", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase4/worldgen").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("explorationTelemetry") {
    group = "verification"
    description = "Measures bounded Phase 5 exploration clue sufficiency and travel burden."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("exploration-telemetry", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase5/exploration").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("secondaryWeathering") {
    group = "verification"
    description = "Writes the deterministic Phase 6 source-budgeted secondary-weathering review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("secondary-weathering", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase6/secondary").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("laterite") {
    group = "verification"
    description = "Writes the deterministic Phase 6 source-budgeted bauxite/Ni-Co laterite review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("laterite", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase6/laterite").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}
