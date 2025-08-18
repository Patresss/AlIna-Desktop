import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    application
    idea
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.patres.alina.server"
version = "0.0.2-SNAPSHOT"


java {
    targetCompatibility = JavaVersion.VERSION_24
    sourceCompatibility = JavaVersion.VERSION_24
}


repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

configurations {
    all {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}


dependencies {
    implementation(project(":common"))
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb:3.2.2")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail:3.2.2")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("se.michaelthelin.spotify:spotify-web-api-java:8.4.1")


    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

}

tasks.withType<Test> {
    useJUnitPlatform()
}
