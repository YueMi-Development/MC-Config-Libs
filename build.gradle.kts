plugins {
    `java-library`
    `maven-publish`
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "mc-config-libs"
            pom {
                name.set("MC Config Libs")
                description.set("Reusable configuration migration library for Paper plugins")
                url.set("https://github.com/YueMi-Development/MC-Config-Libs")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("yuemi")
                        name.set("YueMi-Development")
                    }
                }
                scm {
                    url.set("https://github.com/YueMi-Development/MC-Config-Libs")
                    connection.set("scm:git:https://github.com/YueMi-Development/MC-Config-Libs.git")
                    developerConnection.set("scm:git:https://github.com/YueMi-Development/MC-Config-Libs.git")
                }
            }
        }
    }
    repositories {
        maven {
            val isSnapshot = version.toString().endsWith("SNAPSHOT")
            url = uri(if (isSnapshot) "https://repo.yuemi.my.id/repository/maven-snapshots/" else "https://repo.yuemi.my.id/repository/maven-releases/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
