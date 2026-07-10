// Web backend for the video extension. No libvlc here: the browser's own decoder drives a
// hidden HTML video element and WebGL uploads the frames.

plugins {
    id("flixelgdx.java-library")
}

dependencies {
    api(project(":flixelgdx-video-core"))
    api(project(":flixelgdx-teavm"))
    implementation(libs.jetbrains.annotations)
}
