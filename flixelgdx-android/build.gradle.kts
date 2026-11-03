import java.util.Locale

plugins {
  id("flixelgdx.android-library")
}

android {
  namespace = "org.flixelgdx"
  compileSdk = 36

  defaultConfig {
    multiDexEnabled = true
    minSdk = 26
    ndk {
      // jwebgpu (Dawn) ships 64-bit libraries only, so a 32-bit ABI could never start the renderer.
      abiFilters += listOf("arm64-v8a", "x86_64")
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
  }
}

dependencies {
  "coreLibraryDesugaring"(libs.desugar.jdk.libs)

  // androidx.activity pulls kotlinx-coroutines 1.6.4, which depends on the legacy kotlin-stdlib-jdk7
  // and kotlin-stdlib-jdk8 artifacts at 1.6.21. Those old versions still contain classes that also live
  // in kotlin-stdlib, which breaks dexing with duplicate kotlin.* classes. Depending on the current
  // (empty) jdk7/jdk8 artifacts directly makes dependency resolution pick them over the old ones in
  // every consuming game.
  implementation(libs.kotlin.stdlib.jdk7)
  implementation(libs.kotlin.stdlib.jdk8)

  api(project(":flixelgdx-core"))
  api(project(":flixelgdx-gpu"))
  api(libs.jwebgpu.jni)
  api(project(":flixelgdx-miniaudio"))
  api(libs.multidex)
  implementation(libs.androidx.activity)
  implementation(libs.jetbrains.annotations)
}
