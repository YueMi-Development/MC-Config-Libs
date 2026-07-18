plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT")
}
