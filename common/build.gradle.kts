plugins {
    java
}

group = "com.patres.alina.common"
version = "0.0.1-SNAPSHOT"


java {
    targetCompatibility = JavaVersion.VERSION_24
    sourceCompatibility = JavaVersion.VERSION_24
}


repositories {
    mavenCentral()
}


dependencies {
    implementation("commons-io:commons-io:2.14.0")
    implementation("log4j:log4j:1.2.17")
    implementation("org.slf4j:slf4j-api:1.7.5")
    implementation("org.slf4j:slf4j-log4j12:1.7.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.squareup.retrofit2:converter-jackson:2.11.0")
}

tasks.withType<Jar>() {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}
