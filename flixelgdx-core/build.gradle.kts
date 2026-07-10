/*
 * Copyright (c) 2025 FlixelGDX contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
  id("flixelgdx.java-library")
}

// Embed version from build so Flixel.getVersion() can read it at runtime (Gradle does not set
// JAR manifest like Maven does).
val generateFlixelVersion = tasks.register<WriteProperties>("generateFlixelVersion") {
  destinationFile = layout.buildDirectory.file("generated/version/version.properties")
  property("version", project.version)
}
tasks.processResources {
  from(generateFlixelVersion) {
    into("org/flixelgdx")
  }
}

dependencies {
  api(libs.gdx)
  api(libs.gdx.freetype)
  api(libs.gdx.controllers.core)
  api(libs.basisu.gdx)
  api(libs.anim8)
  api(libs.utils)

  implementation(libs.jetbrains.annotations)
}
