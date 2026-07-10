import java.util.Locale

plugins {
    id("flixelgdx.java-library")
    alias(libs.plugins.teavm)
}

teavm {
    all {
        mainClass = "org.flixelgdx.backend.teavm.FlixelTeaVMLauncher"
    }
    js {
        addedToWebApp = true
        targetFileName = "flixelgdx.js"
    }
}

val reflectionProfileRaw = (findProperty("flixelReflectionProfile") ?: "STANDARD")
    .toString().trim().uppercase(Locale.ROOT)
val reflectionProfile = if (reflectionProfileRaw in listOf("SIMPLE", "STANDARD", "ALL")) reflectionProfileRaw else "STANDARD"
val reflectionExtraPackages = (findProperty("flixelReflectionExtraPackages") ?: "")
    .toString()
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

val generateTeaVmReflectionConfig = tasks.register("generateTeaVmReflectionConfig") {
    val outputDir = layout.buildDirectory.dir("generated/teavm-config")
    outputs.dir(outputDir)
    // runtimeClasspath resolves project(':flixelgdx-core') to its consumer JAR under build/libs/.
    // That file is only created by the jar task, not by compileJava or javadoc alone, so any
    // workflow that runs processResources (e.g. javadoc pulling main output / classpath) must
    // build the core JAR first.
    dependsOn(project(":flixelgdx-core").tasks.named("jar"))

    doLast {
        val out = outputDir.get().file("teavm.json").asFile
        out.parentFile.mkdirs()

        val classNames = LinkedHashSet<String>()

        // Add all local TeaVM module classes.
        sourceSets["main"].output.classesDirs.forEach { dir ->
            if (!dir.exists()) return@forEach
            dir.walkTopDown().filter { it.isFile && it.name.endsWith(".class") }.forEach { file ->
                val rel = file.path.substring(dir.path.length + 1).replace(File.separatorChar, '.')
                classNames.add(rel.removeSuffix(".class"))
            }
        }

        // Add core classes and profile-selected dependencies.
        val interestingPrefixes = mutableListOf("org/flixelgdx/")
        if (reflectionProfile == "STANDARD" || reflectionProfile == "ALL") {
            interestingPrefixes += "com/badlogic/gdx/"
        }
        if (reflectionProfile == "ALL") {
            interestingPrefixes += listOf("com/github/tommyettinger/", "games/rednblack/miniaudio/")
        }
        reflectionExtraPackages.forEach { pkg ->
            interestingPrefixes += pkg.replace('.', '/') + "/"
        }

        configurations["runtimeClasspath"].resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val file = artifact.file
            if (!file.name.endsWith(".jar")) return@forEach
            zipTree(file).matching { include("**/*.class") }.visit {
                if (isDirectory) return@visit
                val path = relativePath.pathString
                if (interestingPrefixes.none { path.startsWith(it) }) return@visit
                classNames.add(path.replace('/', '.').removeSuffix(".class"))
            }
        }

        val classesJson = classNames.joinToString(",\n") { name ->
            """    {
      "name": "$name",
      "allDeclaredFields": true,
      "allDeclaredMethods": true,
      "allPublicFields": true,
      "allPublicMethods": true
    }"""
        }

        out.writeText("""{
  "comment": "Auto-generated reflection metadata for TeaVM ($reflectionProfile).",
  "reflection": {
    "classes": [
$classesJson
    ]
  }
}
""")
    }
}

tasks.processResources {
    from(generateTeaVmReflectionConfig) {
        into("")
    }
}

dependencies {
    api(project(":flixelgdx-core"))
    api(libs.gdx.teavm.backend)
    api(libs.gdx.teavm.controllers)
    implementation(libs.gdx.teavm.freetype)
    implementation(libs.jetbrains.annotations)
}
