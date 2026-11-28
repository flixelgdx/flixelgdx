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
package org.flixelgdx;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.functional.FlixelCreatable;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FlixelManagers implements FlixelCreatable, FlixelUpdatable, FlixelDestroyable {

  private FlixelArray<Manager> managers;
  private boolean initialized = false;

  public FlixelManagers() {
    managers = new FlixelArray<>(32);
  }

  public void add(@NotNull Manager manager) {
    if (initialized) {
      throw new RuntimeException("create() was already executed, managers can no longer be added.");
    }
    if (managers.contains(manager, true)) {
      return;
    }
    managers.add(Objects.requireNonNull(manager));
  }

  @Override
  public void create() {
    initialized = true;

    for (Manager manager : managers) {
      manager.create();
    }
  }

  @Override
  public void update(float elapsed) {
    for (Manager manager : managers) {
      manager.update(elapsed);
    }
  }

  @Override
  public void destroy() {
    for (Manager manager : managers) {
      manager.destroy();
    }
    initialized = false;
  }

  public interface Manager extends FlixelCreatable, FlixelUpdatable, FlixelDestroyable {

    String getId();
  }
}
