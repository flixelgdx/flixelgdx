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

val wgslDir = rootProject.layout.projectDirectory.dir("flixelgdx-gpu/src/main")
val wgslJavaFile = wgslDir.file("java/org/flixelgdx/backend/gpu/wgsl/FlixelWgsl.java")
val contractDir = layout.buildDirectory.dir("generated/resources/contract")

val jwebgpuVersion = libs.versions.jwebgpu.get()

val generateContract = tasks.register("generateShaderContract") {
  group = "build"
  description = "Bundles the WGSL preamble, the shader contract constants and the jwebgpu version into the plugin."
  inputs.file(wgslJavaFile)
  inputs.property("jwebgpuVersion", jwebgpuVersion)
  outputs.dir(contractDir)
  val javaSource = wgslJavaFile.asFile
  val out = contractDir.get().dir("org/flixelgdx/gradle/shader").asFile
  doLast {
    out.mkdirs()
    val text = javaSource.readText()
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
    File(out, "jwebgpu.properties").writeText(
      "# Generated from gradle/libs.versions.toml by the plugin build. Do not edit.\nversion=$jwebgpuVersion\n"
    )
  }
}

sourceSets.main {
  resources.srcDir(generateContract)
}

dependencies {
  implementation(libs.jetbrains.annotations)

  compileOnly(libs.jwebgpu.api)
  compileOnly(libs.jwebgpu.ffm)

  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
}

tasks.named<Test>("test") {
  useJUnitPlatform()
  systemProperty("flixel.test.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
  providers.gradleProperty("org.gradle.java.installations.paths").orNull?.let {
    systemProperty("flixel.test.jdkPaths", it)
  }
}
