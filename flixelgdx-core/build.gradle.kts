/*
 * Copyright (c) 2025 FlixelGDX contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.gradle.external.javadoc.CoreJavadocOptions

plugins {
    id("flixelgdx.java-library")
}

// Embed version from build so Flixel.getVersion() can read it at runtime (Gradle does not set
// JAR manifest like Maven does).
val generateFlixelVersion = tasks.register<WriteProperties>("generateFlixelVersion") {
    destinationFile = layout.buildDirectory.file("generated/version/version.properties")
    property("version", project.version)
}
tasks.processResources {
    from(generateFlixelVersion) {
        into("org/flixelgdx")
    }
}

/**
 * libGDX {@code gdx} depends on {@code gdx-jnigen-loader}; both ship classes under
 * {@code com.badlogic.gdx.utils}. On the JPMS module path they become automatic modules
 * {@code gdx} and {@code gdx.jnigen.loader}, which is an illegal split of the same package.
 * Merge the loader into {@code gdx} via {@code --patch-module} and omit the loader JAR from
 * {@code --module-path} (same approach as for javadoc).
 */
fun gdxJnigenModulePathAndPatch(classpathFiles: Collection<File>): List<String> {
    val jnigen = classpathFiles.filter { it.name.startsWith("gdx-jnigen-loader-") }
    val rest = classpathFiles.filter { !it.name.startsWith("gdx-jnigen-loader-") }
    val modulePath = rest.joinToString(File.pathSeparator) { it.absolutePath }
    if (jnigen.isEmpty()) {
        return listOf(modulePath)
    }
    val patch = "gdx=" + jnigen.joinToString(File.pathSeparator) { it.absolutePath }
    return listOf(modulePath, patch)
}

// Put dependencies on module path so module-info.java can resolve requires (automatic modules).
tasks.named<JavaCompile>("compileJava") {
    doFirst {
        val task = this as JavaCompile
        val mp = gdxJnigenModulePathAndPatch(task.classpath.files)
        val args = mutableListOf("--module-path", mp[0])
        if (mp.size > 1) {
            args += listOf("--patch-module", mp[1])
        }
        task.options.compilerArgs = args
        task.classpath = files()
    }
}

// Require Eclipse (and IDEs that use Eclipse project model) to put dependencies on the module
// path, making "requires gdx" etc. resolve. Without this, only Gradle's compileJava sees them
// as modules.
eclipse.classpath.file {
    whenMerged { classpath: org.gradle.plugins.ide.eclipse.model.Classpath ->
        classpath.entries.filterIsInstance<org.gradle.plugins.ide.eclipse.model.Library>().forEach {
            it.entryAttributes["module"] = "true"
        }
    }
}

// Document as a real JPMS module: all deps on --module-path (same layout as compileJava).
// Javadoc runs in classpath mode otherwise; mixing JPMS + automatic modules on the javadoc
// tool's module path is brittle with Gradle's defaults.
tasks.named<Javadoc>("javadoc") {
    modularity.inferModulePath = false
    classpath = files()
    doFirst {
        val cpFiles = (sourceSets.main.get().compileClasspath + sourceSets.main.get().output.classesDirs).files
        val modulePathFiles = cpFiles.filter { !it.name.startsWith("gdx-jnigen-loader-") }
        (options as CoreJavadocOptions).modulePath = modulePathFiles.toList()
        val jnigen = cpFiles.filter { it.name.startsWith("gdx-jnigen-loader-") }
        if (jnigen.isNotEmpty()) {
            val patchFile = layout.buildDirectory.file("tmp/javadoc-patch.options").get().asFile
            patchFile.parentFile.mkdirs()
            patchFile.writeText("--patch-module gdx=" + jnigen.joinToString(File.pathSeparator) { it.absolutePath } + "\n")
            val opts = options as CoreJavadocOptions
            opts.optionFiles = (opts.optionFiles ?: emptyList()) + patchFile
        }
    }
}

dependencies {
    api(libs.gdx)
    api(libs.gdx.freetype)
    api(libs.gdx.controllers.core)
    api(libs.basisu.gdx)
    api(libs.anim8)
    api(libs.utils)

    implementation(libs.jetbrains.annotations)
}
