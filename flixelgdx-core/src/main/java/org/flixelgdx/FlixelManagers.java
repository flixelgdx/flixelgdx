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

import org.flixelgdx.backend.FlixelBootManager;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.functional.FlixelCreatable;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The global registry for any component that needs to be created, updated, and destroyed
 * in the global lifecycle of the game loop.
 *
 * <p>Think of it like a subway train and its engine: it always goes to the same locations and
 * drives the power of the train itself, but people can board it and tag along with it to go
 * through the same paths to get to their desired location.
 *
 * <p>This class and its interface {@link Manager} work just like that. {@link FlixelManagers}
 * hooks into the global lifecycle ({@link FlixelCreatable create()}, {@link FlixelUpdatable update()},
 * and {@link FlixelDestroyable destroy()}), and any class that implements {@link Manager} can
 * tag right along with it, without subclassing {@link FlixelGame} or weird hacks.
 *
 * <p>If you're planning to make a library or extension (or want to make something that hooks
 * into the lifecycle of the engine), this is the place to do it. It's advised that you configure
 * and register your managers at boot time before the game starts; a {@link FlixelBootManager boot callback}
 * is a good place to do so.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyManager implements FlixelManagers.Manager {
 *
 *   @Override
 *   public void create() {
 *     // Configure values here.
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     // Update logic here.
 *   }
 *
 *   @Override
 *   public void destroy() {
 *     // Cleanup logic here.
 *   }
 * }
 *
 * // Then during the boot sequence, add your manager. It will now be driven by the same exact
 * // lifecycle flags the framework and its engine handle.
 * Flixel.boot.afterStart(() -> Flixel.managers.add(new MyManager()));
 * }</pre>
 */
public class FlixelManagers implements FlixelCreatable, FlixelUpdatable, FlixelDestroyable {

  private final FlixelArray<Manager> managers;

  public FlixelManagers() {
    managers = new FlixelArray<>(32);
  }

  public void add(@NotNull Manager manager) {
    if (managers.contains(manager, true)) {
      return;
    }
    managers.add(Objects.requireNonNull(manager));
  }

  public void remove(@NotNull Manager manager) {
    managers.removeValue(Objects.requireNonNull(manager), true);
  }

  @Override
  public void create() {
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
  }

  public interface Manager extends FlixelCreatable, FlixelUpdatable, FlixelDestroyable {}
}
