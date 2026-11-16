/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
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
package org.flixelgdx.gradle.assets;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Gradle plugin that generates a type-safe {@code Assets} class listing every file in a game's assets folder.
 *
 * <p>Asset paths are usually typed as plain strings, so a typo or a renamed file only shows up when the game runs
 * and fails to load something. This plugin turns the assets folder into Java code: every folder becomes a nested
 * class in PascalCase, and every file becomes an UPPER_SNAKE_CASE constant holding its path. A missing or renamed
 * file then breaks the build instead of the game.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * plugins {
 *   id 'java'
 *   id 'org.flixelgdx.assets' version '<flixel-version>'
 * }
 * }</pre>
 *
 * @see AssetsExtension
 */
public class AssetsPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    AssetsExtension ext = project.getExtensions().create(AssetsExtension.NAME, AssetsExtension.class);
    ext.getAssetsDir().convention(
        project.getRootProject().getLayout().getProjectDirectory().dir(AssetsExtension.DEFAULT_ASSETS_DIR));
    ext.getPackageName().convention(project.provider(() -> String.valueOf(project.getGroup())));
    ext.getClassName().convention(AssetsExtension.DEFAULT_CLASS_NAME);
  }
}
