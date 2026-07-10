plugins {
    id("flixelgdx.java-base")
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(project(":flixelgdx-core"))

    testRuntimeOnly(libs.gdx.controllers.desktop)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.gdx.backend.headless)
    testRuntimeOnly("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-desktop")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
