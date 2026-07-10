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

eclipse.classpath.file {
  whenMerged { classpath: org.gradle.plugins.ide.eclipse.model.Classpath ->
    classpath.entries.filterIsInstance<org.gradle.plugins.ide.eclipse.model.Library>().forEach {
      it.entryAttributes["module"] = "true"
    }
  }
}

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
