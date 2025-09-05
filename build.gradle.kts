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
    implementation("org.springframework.ai:spring-ai-starter-model-openai:1.0.1")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client:1.0.1")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

}



tasks.withType<Test> {
    useJUnitPlatform()
}

val runArgsValue = listOf(
    "-Djavafx.enablePreview=true"
)

val title = "AlIna"

tasks.withType<JavaExec> {
    jvmArgs = runArgsValue
}

