plugins {
    java
    `maven-publish`
}

group = "net.techcable"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

java {
    toolchain {
        // TODO: Remove module-info.java to make compatible with java 8
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "tinyjson"

            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "github-tinyjson"
            url = uri("https://maven.pkg.github.com/Techcable/tinyjson")
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
