// This module must stay platform neutral and TeaVM safe: no JVM-only helpers,
// no LWJGL, no JNA. Platform backends live in the sibling video modules.

plugins {
    id("flixelgdx.java-library")
}

dependencies {
    api(project(":flixelgdx-core"))
    implementation(libs.jetbrains.annotations)
}
