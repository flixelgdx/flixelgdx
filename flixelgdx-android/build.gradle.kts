import java.util.Locale

plugins {
  id("flixelgdx.android-library")
}

android {
  namespace = "org.flixelgdx"
  compileSdk = 36

  defaultConfig {
    multiDexEnabled = true
    minSdk = 24
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  "coreLibraryDesugaring"(libs.desugar.jdk.libs)

  api(project(":flixelgdx-core"))
  api(project(":flixelgdx-miniaudio"))
  api(libs.multidex)
  implementation(libs.jetbrains.annotations)
}
