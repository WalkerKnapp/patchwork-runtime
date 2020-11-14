plugins {
    java
    `maven-publish`
    id("fabric-loom") version "0.5-SNAPSHOT"
}

base.archivesBaseName = "patchwork-runtime"
group = "net.patchworkmc"
version = "0.0.1"

repositories {
    mavenCentral()
    maven {
        setUrl("https://dl.bintray.com/user11681/maven")
    }
    maven {
        setUrl("https://jitpack.io")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.14.4")
    mappings("net.fabricmc:yarn:1.14.4+build.16:v2")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.15.1+build.260-1.14")
    modImplementation("net.fabricmc:fabric-loader:0.10.0+build.208")

    include("net.devtech:grossfabrichacks:7.4")
    modApi("net.devtech:grossfabrichacks:7.4")

    implementation("com.github.PatchworkMC:patchwork-patcher:baf0ad124e")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact("remapJar") {
                builtBy(tasks.getByName("remapJar"))
            }
            artifact("sourcesJar") {
                builtBy(tasks.getByName("remapSourcesJar"))
            }
        }
    }
}

tasks.withType(Jar::class).configureEach {
    from("LICENSE") {
        rename { "${this}_${base.archivesBaseName}" }
    }
}

tasks.withType(ProcessResources::class).configureEach {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

tasks.withType(JavaCompile::class).configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"

    // The Minecraft launcher currently installs Java 8 for users, so your mod probably wants to target Java 8 too
    // JDK 9 introduced a new way of specifying this that will make sure no newer classes or methods are used.
    // We'll use that if it's available, but otherwise we'll use the older option.
    val targetVersion = 8
    if (JavaVersion.current().isJava9Compatible) {
        options.release.set(targetVersion)
    }
}
