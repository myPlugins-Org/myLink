plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "9.0.0-beta4"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.myplugins"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("org.apache.logging.log4j:log4j-core:2.24.1")
    implementation(kotlin("stdlib"))
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("net.dv8tion:JDA:5.2.1") {
        exclude(module = "opus-java")
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }
    shadowJar {
        archiveClassifier.set("")
        relocate("org.mongodb", "org.myplugins.mylink.libs.mongodb")
        relocate("kotlinx.coroutines", "org.myplugins.mylink.libs.coroutines")
        relocate("kotlin", "org.myplugins.mylink.libs.kotlin")
        relocate("net.dv8tion", "org.myplugins.mylink.libs.jda")
        minimize()
    }
    build {
        dependsOn(shadowJar)
    }
    runServer {
        minecraftVersion("1.21.11")
    }
}