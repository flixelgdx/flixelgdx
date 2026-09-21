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
package org.flixelgdx.gradle.packagr;

import org.gradle.api.Named;
import org.gradle.api.provider.Property;

/**
 * One platform a game is packaged for, configured inside the {@code targets} block.
 *
 * <p>A target is just a name paired with an {@link OperatingSystem} and an {@link Architecture}.
 * The convenience methods on {@link PackagrTargets} (such as {@code linuxX64()}) create the common
 * targets for you; declaring one directly with {@code register(...)} is how you describe a platform
 * the convenience methods do not cover, for example a Linux ARM handheld:
 *
 * <pre>{@code
 * targets {
 *   linuxX64()
 *   register("steamDeck") {
 *     os = OperatingSystem.LINUX
 *     arch = Architecture.AARCH64
 *   }
 * }
 * }</pre>
 *
 * <p>The name is used for the task that builds this target ({@code package<Name>}) and for the
 * output directory ({@code build/packagr/<name>}).
 */
public interface PackagrTarget extends Named {

  /**
   * The operating system this target runs on.
   *
   * <p>The convenience methods on {@link PackagrTargets} set this for you. A target declared
   * directly must set it, or packaging that target fails with a clear error.
   *
   * @return The operating-system property.
   */
  Property<OperatingSystem> getOs();

  /**
   * The CPU architecture this target runs on.
   *
   * <p>The convenience methods on {@link PackagrTargets} set this for you. A target declared
   * directly must set it, or packaging that target fails with a clear error.
   *
   * @return The architecture property.
   */
  Property<Architecture> getArch();
}
