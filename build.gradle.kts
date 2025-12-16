plugins {
    java
    application
    idea
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.2"

    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.patres.alina.ui-desktop"
version = "1.0-SNAPSHOT"
application {
    mainClass.set("com.patres.alina.AppLauncher")
}

java {
    targetCompatibility = JavaVersion.VERSION_24
    sourceCompatibility = JavaVersion.VERSION_24
}

javafx {
    version = "26-ea+2"
    modules("javafx.controls", "javafx.fxml", "javafx.swing", "javafx.web")
}

repositories {
    mavenCentral()
    maven { url = uri("https://sandec.jfrog.io/artifactory/repo") }
    maven { url = uri("https://repo.spring.io/milestone") }
}


dependencies {
    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.21.0")
    implementation("org.commonmark:commonmark-ext-autolink:0.21.0")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:0.21.0")
    implementation("org.commonmark:commonmark-ext-task-list-items:0.21.0")

    implementation("io.github.mkpaz:atlantafx-base:2.1.0")

    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-feather-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-material2-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-devicons-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-bootstrapicons-pack:12.3.1")

    implementation("org.jetbrains:annotations:23.0.0")
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("commons-io:commons-io:2.14.0")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    // common module
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")

    // server module
    implementation("org.springframework.ai:spring-ai-starter-model-openai:1.0.3")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client:1.0.3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

val runArgsValue = listOf(
    "-Djavafx.enablePreview=true",
    "-Djava.awt.headless=false"
)

val title = "AlIna"
// macOS bundle identifier used for PKG/App bundle metadata
val macPackageIdentifier = "com.patres.alina"

tasks.withType<JavaExec> {
    jvmArgs = runArgsValue
}

// Runtime image and installer packaging
// Inspired by build-working-examlpe.gradle, adapted for Spring + JavaFX
val os = org.gradle.internal.os.OperatingSystem.current()

jlink {
    addExtraDependencies("javafx")
    // Ensure jackson-dataformat-yaml is included on module path
    addExtraDependencies("jackson-dataformat-yaml")

    // Reduce image size a bit while keeping debug symbols for easier troubleshooting
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages"))

    // Merge popular non-modular libs into the main module to avoid JPMS issues
    // Avoid merging jackson to prevent cycles with delegating modules
    forceMerge("spring")
    forceMerge("reactor")
    forceMerge("micrometer")
    forceMerge("netty")
    forceMerge("annotations")

    // Use additive module-info to avoid deriving unwanted requires/provides
    mergedModule {
        additive = true
    }

    launcher {
        name = title
        // Pass through JVM args used during run
        jvmArgs.addAll(runArgsValue)
    }

    // Configure jpackage for platform-specific installer
    jpackage {
        // Allow override via -PinstallerType=exe|msi|pkg|dmg|deb|rpm|app-image
        val requestedInstallerType = (findProperty("installerType") as String?)?.lowercase()
        // Choose sensible default per platform
        installerType = when {
            !requestedInstallerType.isNullOrBlank() -> requestedInstallerType
            os.isWindows -> "exe"
            os.isMacOsX -> "pkg" // use dmg/pkg on macOS; pkg is scriptable
            os.isLinux -> "deb"
            else -> "app-image"
        }

        // Sanitize version for platforms that require numeric versions (macOS/Windows/Linux packages)
        fun sanitizeVersion(ver: String): String {
            val digits = ver.replace(Regex("[^0-9.]"), "")
            val parts = digits.split('.').filter { it.isNotBlank() }
            val normalized = (parts + listOf("0", "0", "0")).take(3).joinToString(".")
            return if (normalized.isBlank()) "1.0.0" else normalized
        }
        val needsNumericVersion = os.isMacOsX || os.isWindows || os.isLinux
        appVersion = if (needsNumericVersion) sanitizeVersion(project.version.toString()) else project.version.toString()
        // Put deliverables under release/<version>/
        outputDir = "release/${project.version}"

        // Common metadata
        installerOptions.addAll(listOf(
            "--description", (project.description ?: "AlIna Desktop")
        ))

        // Icons per platform, if present
        val iconIco = file("src/main/resources/icon/desktop/main_icon.ico")
        val iconIcns = file("src/main/resources/icon/desktop/main_icon.icns")
        if (os.isWindows && iconIco.exists()) {
            imageOptions.addAll(listOf("--icon", iconIco.path))
        }
        if (os.isMacOsX) {
            if (iconIcns.exists()) {
                imageOptions.addAll(listOf("--icon", iconIcns.path))
            }
            installerOptions.addAll(listOf(
                "--mac-package-identifier", macPackageIdentifier,
                "--mac-package-name", title
            ))
        }
        if (os.isLinux && iconIco.exists()) {
            // jpackage accepts .png/.ico for linux; use ico if provided (fallback)
            imageOptions.addAll(listOf("--icon", iconIco.path))
        }

        // Extra platform options
        if (os.isWindows) {
            installerOptions.addAll(listOf(
                "--win-dir-chooser",
                "--win-menu",
                "--win-shortcut"
            ))
        }
        if (os.isLinux) {
            installerOptions.addAll(listOf(
                "--linux-shortcut"
            ))
        }
    }
}

// Convenience task: create installer and remove intermediate app image folder named as launcher
tasks.register("createInstaller") {
    group = "distribution"
    description = "Build platform installer via jpackage and clean temp image dir."
    dependsOn(tasks.named("jpackage"))
    doLast {
        val imageDir = file("${rootDir}/release/${project.version}/$title")
        if (imageDir.exists()) {
            val deleted = project.delete(imageDir)
            println("Removed intermediate image directory '$imageDir': $deleted")
        }
    }
}

// Convenience task using fat JAR path (avoids jlink/JPMS issues)
tasks.register("createInstallerFat") {
    group = "distribution"
    description = "Build platform installer via jpackageFat (uses Spring Boot fat JAR)."
    dependsOn(tasks.named("jpackageFat"))
}

// Zip the app image (not the installer) as an alternative delivery
tasks.register<Zip>("packageExecutableZip") {
    group = "distribution"
    description = "Zip the generated app image under release/<version>/."
    archiveFileName.set("${title}-${project.version}.zip")
    destinationDirectory.set(file("${rootDir}/release/${project.version}"))
    from("$buildDir/image")
}

// Alternative packaging path: jpackage using Spring Boot fat JAR (no jlink)
// Useful when JPMS module graph is complex (Spring + Reactor + JavaFX)
tasks.register<Exec>("jpackageFat") {
    group = "distribution"
    description = "Create installer using Boot fat JAR (non-modular)."
    dependsOn(tasks.named("bootJar"))

    doFirst {
        file("release/${project.version}").mkdirs()
    }

    val os = org.gradle.internal.os.OperatingSystem.current()
    val requestedInstallerType = (findProperty("installerType") as String?)?.lowercase()
    val installerType = when {
        !requestedInstallerType.isNullOrBlank() -> requestedInstallerType
        os.isWindows -> "exe"
        os.isMacOsX -> "pkg"
        os.isLinux -> "deb"
        else -> "app-image"
    }

    val iconIco = file("src/main/resources/com/patres/alina/uidesktop/assets/app-icon.ico")
    val iconIcns = file("src/main/resources/icon/desktop/main_icon.icns")
    val iconPng = file("src/main/resources/com/patres/alina/uidesktop/assets/icon-square-512.png")
    val bootJar = layout.buildDirectory.file("libs/${project.name}-${project.version}.jar").get().asFile

    fun sanitizeVersion(ver: String): String {
        val digits = ver.replace(Regex("[^0-9.]"), "")
        val parts = digits.split('.').filter { it.isNotBlank() }
        val normalized = (parts + listOf("0", "0", "0")).take(3).joinToString(".")
        return if (normalized.isBlank()) "1.0.0" else normalized
    }

    val versionForInstaller = if (os.isMacOsX || os.isWindows || os.isLinux) sanitizeVersion(project.version.toString()) else project.version.toString()

    val baseCmd = mutableListOf(
        "jpackage",
        "--type", installerType,
        "--name", title,
        "--app-version", versionForInstaller,
        "--dest", file("release/${project.version}").absolutePath,
        "--input", file("build/libs").absolutePath,
        "--main-jar", bootJar.name,
        "--java-options", runArgsValue.joinToString(" ")
    )
    if (os.isWindows && iconIco.exists()) {
        baseCmd += listOf("--icon", iconIco.absolutePath, "--win-dir-chooser", "--win-menu", "--win-shortcut")
    }
    if (os.isMacOsX) {
        if (iconIcns.exists()) {
            baseCmd += listOf("--icon", iconIcns.absolutePath)
        }
        baseCmd += listOf("--mac-package-identifier", macPackageIdentifier, "--mac-package-name", title)
    }
    if (os.isLinux) {
        if (iconPng.exists()) {
            baseCmd += listOf("--icon", iconPng.absolutePath, "--linux-shortcut")
        } else if (iconIco.exists()) {
            baseCmd += listOf("--icon", iconIco.absolutePath, "--linux-shortcut")
        }
    }
    commandLine(baseCmd)
}

// Alternative packaging (recommended with jnativehook): non-boot jar + explicit classpath
tasks.register<Exec>("jpackageApp") {
    group = "distribution"
    description = "Create installer using plain JAR and explicit classpath (avoids nested-jar classloader)."
    dependsOn(tasks.named("jar"))

    doFirst {
        // prepare input dir with main jar and dependencies
        val inputDir = file("$buildDir/jpackage/input").apply { mkdirs() }
        val libDir = file("$inputDir/lib").apply { mkdirs() }

        // copy main jar (Spring Boot reconfigures 'jar' to produce '-plain.jar')
        val mainJar = layout.buildDirectory.file("libs/${project.name}-${project.version}-plain.jar").get().asFile
        mainJar.copyTo(file("$inputDir/${mainJar.name}"), overwrite = true)

        // copy dependencies
        configurations.runtimeClasspath.get().files.forEach { dep ->
            dep.copyTo(file("$libDir/${dep.name}"), overwrite = true)
        }
        val os = org.gradle.internal.os.OperatingSystem.current()
        val installerType = when {
            os.isWindows -> "exe"
            os.isMacOsX -> "pkg"
            os.isLinux -> "deb"
            else -> "app-image"
        }

        fun sanitizeVersion(ver: String): String {
            val digits = ver.replace(Regex("[^0-9.]"), "")
            val parts = digits.split('.').filter { it.isNotBlank() }
            val normalized = (parts + listOf("0", "0", "0")).take(3).joinToString(".")
            return if (normalized.isBlank()) "1.0.0" else normalized
        }

        val versionForInstaller = if (os.isMacOsX || os.isWindows || os.isLinux) sanitizeVersion(project.version.toString()) else project.version.toString()

        // build classpath from copied dependencies (relative to --input)
        val classPath = file("$buildDir/jpackage/input/lib").listFiles()?.filter { it.isFile && it.name.endsWith(".jar") }
            ?.sortedBy { it.name }
            ?.joinToString(separator = File.pathSeparator) { "lib/${it.name}" }
            ?: ""

        val iconIco = file("src/main/resources/com/patres/alina/uidesktop/assets/app-icon.ico")
        val iconIcns = file("src/main/resources/icon/desktop/main_icon.icns")
        val iconPng = file("src/main/resources/com/patres/alina/uidesktop/assets/icon-square-512.png")
        // main jar already defined above as 'mainJar'

        val baseCmd = mutableListOf(
            "jpackage",
            "--type", installerType,
            "--name", title,
            "--app-version", versionForInstaller,
            "--dest", file("release/${project.version}").absolutePath,
            "--input", file("$buildDir/jpackage/input").absolutePath,
            "--main-jar", mainJar.name,
            "--main-class", application.mainClass.get(),
            "--java-options", runArgsValue.joinToString(" ")
        )
        if (classPath.isNotBlank()) {
            baseCmd += listOf("--class-path", classPath)
        }

        if (os.isWindows && iconIco.exists()) {
            baseCmd += listOf("--icon", iconIco.absolutePath, "--win-dir-chooser", "--win-menu", "--win-shortcut")
        }
        if (os.isMacOsX) {
            if (iconIcns.exists()) baseCmd += listOf("--icon", iconIcns.absolutePath)
            baseCmd += listOf("--mac-package-identifier", macPackageIdentifier, "--mac-package-name", title)
        }
        if (os.isLinux) {
            if (iconPng.exists()) {
                baseCmd += listOf("--icon", iconPng.absolutePath, "--linux-shortcut")
            } else if (iconIco.exists()) {
                baseCmd += listOf("--icon", iconIco.absolutePath, "--linux-shortcut")
            }
        }

        println("Executing: ${baseCmd.joinToString(" ")}")
        commandLine(baseCmd)
    }
}
