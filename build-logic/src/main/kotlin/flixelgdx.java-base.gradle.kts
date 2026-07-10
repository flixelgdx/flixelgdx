/**
 * Baseline convention applied to every FlixelGDX Java subproject.
 *
 * <p>Covers: project coordinates, IDE metadata (Eclipse + IntelliJ), centralized repository
 * declarations, Java compile encoding, and Spotless formatting rules. Modules that need
 * publication or the {@code java-library} surface area apply {@code flixelgdx.java-library}
 * instead, which in turn applies this plugin.
 */

plugins {
    eclipse
    idea
    id("com.diffplug.spotless")
}

val groupId: String by project
val projectVersion: String by project

group = groupId
version = projectVersion

eclipse.project.name = project.name

idea {
    module {
        outputDir = file("build/classes/java/main")
        testOutputDir = file("build/classes/java/test")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

spotless {
    java {
        eclipse("4.33").configFile("${rootDir}/gradle/spotless/eclipse-formatter.xml")
        importOrder("com", "org", "io", "java", "javax", "jdk", "", "\\#")
        removeUnusedImports()
        endWithNewline()
        trimTrailingWhitespace()
    }
}
