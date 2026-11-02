plugins {
  id("flixelgdx.gradle-plugin")
}

gradlePlugin {
  plugins {
    create("shaders") {
      id = "org.flixelgdx.shaders"
      implementationClass = "org.flixelgdx.gradle.shader.ShaderPlugin"
      displayName = "FlixelGDX Shader Plugin"
      description =
        "Validates a game's WGSL shaders with Dawn at build time and bundles them as resources for every FlixelGDX platform."
    }
  }
}

// The shader contract (the preamble text and the bind group numbers) has exactly one home: FlixelWgsl.java in
// flixelgdx-core. This task copies the PREAMBLE text block into a file and turns the other constants into a
// properties file, so the plugin validates against the same contract the renderer compiles. Nothing is typed
// twice, and a renamed or reshaped constant fails this task instead of silently drifting.
val wgslDir = rootProject.layout.projectDirectory.dir("flixelgdx-core/src/main")
val wgslJavaFile = wgslDir.file("java/org/flixelgdx/graphics/wgsl/FlixelWgsl.java")
val contractDir = layout.buildDirectory.dir("generated/resources/contract")

val generateContract = tasks.register("generateShaderContract") {
  group = "build"
  description = "Bundles the WGSL preamble and the shader contract constants of flixelgdx-core into the plugin."
  inputs.file(wgslJavaFile)
  outputs.dir(contractDir)
  val javaSource = wgslJavaFile.asFile
  val out = contractDir.get().dir("org/flixelgdx/gradle/shader").asFile
  doLast {
    out.mkdirs()
    val text = javaSource.readText()
    // Rebuild the string Java makes from the text block: each line minus the closing quotes' indentation, trailing
    // spaces dropped, and a final line break because the closing quotes sit on their own line. That is exact as long
    // as the block has no escapes and nothing is indented less than the closing quotes, which is checked below.
    val block = Regex("""public static final String PREAMBLE = ""\"\n(.*?)\n( *)""\";""", RegexOption.DOT_MATCHES_ALL)
      .find(text) ?: throw GradleException("FlixelWgsl.java no longer declares PREAMBLE as a text block.")
    val body = block.groupValues[1]
    if ('\\' in body) {
      throw GradleException("FlixelWgsl.PREAMBLE must not use backslash escapes; generateShaderContract copies it as-is.")
    }
    val closingIndent = block.groupValues[2].length
    val lines = body.split('\n')
    if (lines.any { it.isNotBlank() && it.length - it.trimStart().length < closingIndent }) {
      throw GradleException("FlixelWgsl.PREAMBLE's closing quotes must line up with its text.")
    }
    val preamble = lines.joinToString("\n", postfix = "\n") { if (it.isBlank()) "" else it.substring(closingIndent).trimEnd() }
    File(out, "flixel_preamble.wgsl").writeText(preamble)

    val wanted = listOf(
      "FRAGMENT_ENTRY", "VERTEX_ENTRY", "DEFAULT_VERTEX_ENTRY",
      "FRAMEWORK_GROUP", "SHADER_GROUP", "SHADER_BINDING", "GLOBALS_SIZE"
    )
    val pattern = Regex("""public static final (?:int|String) (\w+) = ("[^"]*"|\d+);""")
    val found = pattern.findAll(javaSource.readText()).associate { it.groupValues[1] to it.groupValues[2] }
    val missing = wanted.filter { it !in found }
    if (missing.isNotEmpty()) {
      throw GradleException("FlixelWgsl.java no longer declares $missing; update generateShaderContract.")
    }
    File(out, "shader_contract.properties").writeText(
      "# Generated from FlixelWgsl.java by the plugin build. Do not edit.\n" +
        wanted.joinToString("\n", postfix = "\n") { "$it=${found.getValue(it).trim('"')}" }
    )
  }
}

sourceSets.main {
  resources.srcDir(generateContract)
}

dependencies {
  implementation(libs.jetbrains.annotations)

  // The validation worker is compiled against jwebgpu but never loads it in Gradle's own JVM: it runs in a
  // forked Java process whose class path comes from the plugin's own configuration (see ShaderPlugin).
  compileOnly(libs.jwebgpu.api)
  compileOnly(libs.jwebgpu.ffm)

  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
}

// The functional tests run real Gradle builds that include the same jwebgpu checkout as this build, so they need
// to know where it is, which Gradle user home holds the dependency cache, and where the JDKs are.
tasks.named<Test>("test") {
  useJUnitPlatform()
  val jwebgpu = gradle.includedBuilds.firstOrNull { File(it.projectDir, "jwebgpu-api").isDirectory }
  if (jwebgpu != null) {
    systemProperty("flixel.test.jwebgpuDir", jwebgpu.projectDir.absolutePath)
  }
  systemProperty("flixel.test.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
  providers.gradleProperty("org.gradle.java.installations.paths").orNull?.let {
    systemProperty("flixel.test.jdkPaths", it)
  }
}
