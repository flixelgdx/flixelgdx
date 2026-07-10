plugins {
  id("flixelgdx.android-library")
}

android {
  namespace = "org.flixelgdx.backend.android.video"
  compileSdk = 36

  defaultConfig {
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
  api(project(":flixelgdx-video-core"))
  api(project(":flixelgdx-android"))
  compileOnly(libs.jetbrains.annotations)
}
