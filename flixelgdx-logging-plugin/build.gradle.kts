plugins {
    id("flixelgdx.gradle-plugin")
}

gradlePlugin {
    plugins {
        create("flixelLogging") {
            id = "org.flixelgdx.logging"
            implementationClass = "org.flixelgdx.gradle.logging.FlixelLoggingPlugin"
            displayName = "FlixelGDX Logging Plugin"
            description = "Weaves explicit source file and line into FlixelLogger calls after compileJava for accurate traces (desktop, TeaVM, etc.)."
        }
    }
}

dependencies {
    implementation(libs.asm)
    implementation(libs.asm.tree)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":flixelgdx-core"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
