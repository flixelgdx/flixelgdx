plugins {
  id("flixelgdx.gradle-plugin")
}

gradlePlugin {
  plugins {
    create("flixelgdxBasisu") {
      id = "org.flixelgdx.basisu"
      implementationClass = "org.flixelgdx.gradle.basisu.FlixelBasisuPlugin"
      displayName = "FlixelGDX Basis Universal Compression Plugin"
      description = "Opt-in KTX2/Basis Universal texture compression system for desktop and mobile, auto-fetching the basisu encoder so developers never install anything themselves."
    }
  }
}

dependencies {
  compileOnly(libs.android.gradle.plugin)
}
