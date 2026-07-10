// Android backend for the FlixelGDX video extension. Decodes video with the platform
// MediaPlayer into a SurfaceTexture bound to a GL_TEXTURE_EXTERNAL_OES texture, then blits
// each frame into a normal framebuffer texture so the framework can draw it like any other
// object and layering follows state add order.

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
