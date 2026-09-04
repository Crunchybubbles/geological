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

tasks.register<JavaExec>("secondaryPlacers") {
    group = "verification"
    description = "Writes the deterministic Phase 6 source-budgeted secondary-placer review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("secondary-placers", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase6/secondary-placers").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("paleosurface") {
    group = "verification"
    description = "Writes the deterministic Phase 6 structural paleosurface review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("paleosurface", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase6/paleosurface").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("glacial") {
    group = "verification"
    description = "Writes the deterministic Phase 6 opt-in glacial transport review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("glacial", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase6/glacial").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("greisen") {
    group = "verification"
    description = "Writes the deterministic Phase 7 residual-fluid greisen review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("greisen", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase7/greisen").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("skarn") {
    group = "verification"
    description = "Writes the deterministic Phase 7 carbonate-contact skarn review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("skarn", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase7/skarn").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("epithermal") {
    group = "verification"
    description = "Writes the deterministic Phase 7 shallow-hydrothermal epithermal review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("epithermal", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase7/epithermal").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("orogenicGold") {
    group = "verification"
    description = "Writes the deterministic Phase 7 metamorphic-fluid orogenic-gold review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("orogenic-gold", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase7/orogenic-gold").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("basinHydrothermal") {
    group = "verification"
    description = "Writes the deterministic Phase 7 MVT/SEDEX/sediment-hosted copper review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("basin-hydrothermal", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase7/basin-hydrothermal").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("uranium") {
    group = "verification"
    description = "Writes the deterministic Phase 7 unconformity and sandstone uranium review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("uranium", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase7/uranium").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("layeredIntrusion") {
    group = "verification"
    description = "Writes the deterministic Phase 7 layered-intrusion chromite and Ni-Cu-PGE review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("layered-intrusion", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase7/layered-intrusion").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

tasks.register<JavaExec>("carbonatiteKimberlite") {
    group = "verification"
    description = "Writes the deterministic Phase 7 carbonatite/peralkaline REE and kimberlite/diamond review."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("carbonatite-kimberlite", "--seed", "8675309", "--output", layout.buildDirectory.dir("phase7/carbonatite-kimberlite").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}
