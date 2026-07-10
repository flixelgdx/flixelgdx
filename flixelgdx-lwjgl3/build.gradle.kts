plugins {
    id("flixelgdx.java-library")
}

dependencies {
    api(project(":flixelgdx-common"))
    api(project(":flixelgdx-core"))
    api(project(":flixelgdx-jvm"))
    api("games.rednblack.miniaudio:gdx-miniaudio-platform:${libs.versions.miniaudio.get()}:natives-desktop")
    api(libs.gdx.backend.lwjgl3)
    api("com.badlogicgames.gdx:gdx-freetype-platform:${libs.versions.gdx.get()}:natives-desktop")
    api(libs.gdx.controllers.desktop)
    api("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-desktop")
    api("com.crashinvaders.basisu:basisu-wrapper:${libs.versions.basisuGdx.get()}:natives-desktop")
    api(libs.imgui.binding)
    api(libs.imgui.lwjgl3)
    runtimeOnly(libs.imgui.natives.linux)
    runtimeOnly(libs.imgui.natives.macos)
    runtimeOnly(libs.imgui.natives.windows)

    implementation(libs.jetbrains.annotations)
    implementation(libs.jansi)

    // Provides the Feature interface and RuntimeJNIAccess used by FlixelGraalFeature.
    // Compile-only: the class runs inside native-image at build time, not in user runtimes.
    compileOnly(libs.graalvm.nativeimage)
}
