plugins {
    java
}

group = "com.ultraop"
version = "0.1.0-SNAPSHOT"

description = "Paper 1.21.11 implementation of the Motion Capture (MoCap) mod behavior."

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

 tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
