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

  implementation(libs.kotlin.stdlib.jdk7)
  implementation(libs.kotlin.stdlib.jdk8)

  api(project(":flixelgdx-core"))
  api(project(":flixelgdx-gpu"))
  api(libs.jwebgpu.jni)
  api(project(":flixelgdx-miniaudio"))
  api(project(":flixelgdx-jvm"))
  api(libs.multidex)
  implementation(libs.androidx.activity)
  implementation(libs.jetbrains.annotations)
}
