plugins {
    id("flixelgdx.java-library")
}

dependencies {
    api(project(":flixelgdx-core"))
    api(project(":flixelgdx-jvm"))
    api(libs.gdx.backend.robovm)
    api("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-ios")
    api(libs.robovm.rt)
    api(libs.robovm.cocoatouch)
    api(libs.gdx.controllers.ios)
}
