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

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration exposed as the {@code assetsClass} block in a game module's build script.
 *
 * <p>Every setting has a working default, so a game that keeps its files in the usual {@code assets/} folder at the
 * root of the project only needs to set the package the class goes in (and not even that when the project already
 * has a {@code group}).
 *
 * <pre>{@code
 * assetsClass {
 *   // The folder that is scanned (default: the assets folder at the root of the project).
 *   assetsDir = rootProject.layout.projectDirectory.dir("assets")
 *
 *   // The package the class is generated in (default: the project's group).
 *   packageName = "com.mygame"
 *
 *   // The name of the generated class (default: Assets).
 *   className = "Assets"
 *
 *   // Ant-style patterns, relative to assetsDir, for files that should not get a constant.
 *   excludes = ["source-art/**", "notes.txt"]
 * }
 * }</pre>
 */
public interface AssetsExtension {

  /** The name the extension is registered under. */
  String NAME = "assetsClass";

  /** The folder, relative to the root project, that is scanned by default. */
  String DEFAULT_ASSETS_DIR = "assets";

  /** The name of the generated class by default. */
  String DEFAULT_CLASS_NAME = "Assets";

  /**
   * The folder that is scanned for files, including its subfolders.
   *
   * <p>Each subfolder becomes a nested class and each file becomes a constant holding its path relative to this
   * folder, which is the same path {@code Flixel.assets} and {@code Flixel.files.internal(...)} expect.
   *
   * @return The assets folder property, defaulting to {@value #DEFAULT_ASSETS_DIR} at the root of the project.
   */
  DirectoryProperty getAssetsDir();

  /**
   * The Java package the class is generated in, such as {@code com.mygame}.
   *
   * <p>An empty value puts the class in the default package, which code inside a package cannot import, so set this
   * whenever the project has no {@code group}.
   *
   * @return The package name property, defaulting to the project's {@code group}.
   */
  Property<String> getPackageName();

  /**
   * The simple name of the generated class.
   *
   * @return The class name property, defaulting to {@value #DEFAULT_CLASS_NAME}.
   */
  Property<String> getClassName();

  /**
   * Ant-style patterns, relative to the assets folder, for files that should not get a constant.
   *
   * <p>Hidden files and folders (names starting with a dot) are always skipped.
   *
   * @return The exclude patterns, empty by default.
   */
  ListProperty<String> getExcludes();
}
