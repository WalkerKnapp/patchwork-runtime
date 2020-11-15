rootProject.name = "patchwork-runtime"

pluginManagement {
    repositories {
        mavenCentral()
        jcenter()
        maven {
            name = "Fabric"
            setUrl("https://maven.fabricmc.net/")
        }
        maven {
            setUrl("https://jitpack.io/")
        }
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "org.anarres") {
                useModule("com.github.shevek.jarjar:jarjar-gradle:9a7eca72f958038d46bd4f83bbe2275519f2693f")
            }

            if (requested.id.namespace == "net.vrallev") {
                useModule("net.vrallev.gradle:jarjar-gradle:1.1.0")
            }
        }
    }
}
