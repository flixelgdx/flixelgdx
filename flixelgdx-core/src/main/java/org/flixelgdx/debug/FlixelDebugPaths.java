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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelState;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.panels.FlixelDebugInspectorPanel;
import org.flixelgdx.group.FlixelGroupable;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a readable path for a game object and turns such a path back into the object.
 *
 * <p>Think of a street address. "State, 3rd door, 1st door inside" is enough for a visitor to find the exact
 * room, and it still means something if the visitor reads it later. The debugger uses the same idea: the path
 * {@code state.3.0} means "the member at index 0 of the member at index 3 of the current state".
 *
 * <h2>Format</h2>
 *
 * <p>A path is a list of steps separated by dots. The first step names where to start:
 *
 * <ul>
 *   <li>{@code state} is {@link Flixel#state}.</li>
 *   <li>{@code substate} is the sub state that is open on top of the state. {@code substate.substate} is the one
 *       open on top of that, and so on.</li>
 *   <li>Any label given to {@link FlixelDebugInspectorPanel#track(Object, String)} starts at that tracked
 *       object. A label may contain dots.</li>
 * </ul>
 *
 * <p>Every later step is either a member index (a decimal number, the position in the group's member list,
 * counting empty slots) or the word {@code substate}, which steps from a state into its open sub state. The framework
 * has no per-object debug names (a {@code FlixelBasic} only has a numeric id), so member indexes are the only
 * naming. Indexes change when members are added or removed in front of an object, so a path is a snapshot, not a
 * permanent address.
 *
 * <pre>{@code
 * state            // The current state itself.
 * state.3          // The fourth member of the state.
 * state.3.0        // The first member of that member, which must be a group.
 * substate.2       // The third member of the open sub state.
 * managers.0       // The first member of the object tracked with the label "managers".
 * }</pre>
 *
 * <p>Building and resolving a path never creates objects except for the text you pass in. Searching walks the
 * object tree once, so avoid calling {@link #build(FlixelDebugInspectorPanel, Object, FlixelString)} every frame
 * for large scenes.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelString path = new FlixelString();
 * if (FlixelDebugPaths.build(Flixel.debug.inspector, enemy, path)) {
 *   Flixel.info("Enemy lives at {}", path);
 *   Object same = FlixelDebugPaths.resolve(Flixel.debug.inspector, path); // == enemy
 * }
 * }</pre>
 *
 * @see FlixelDebugInspectorPanel
 */
public final class FlixelDebugPaths {

  /** The first step of a path that starts at the current state. */
  public static final String STATE = "state";

  /** The first step of a path that starts at the open sub state, and the step that descends into one. */
  public static final String SUBSTATE = "substate";

  /** How deep a search goes, which also protects against a group that contains itself. */
  public static final int MAX_DEPTH = 64;

  private FlixelDebugPaths() {}

  /**
   * Writes the path of an object into a string.
   *
   * <p>The state is searched first, then each sub state, then each tracked object in the order they were tracked.
   * The first match wins when an object appears more than once.
   *
   * @param roots The inspector that knows the tracked objects.
   * @param target The object to find.
   * @param out Receives the path. It is cleared first, and left empty when the object is not found.
   * @return {@code true} when the object was found.
   */
  public static boolean build(@NotNull FlixelDebugInspectorPanel roots, @Nullable Object target,
      @NotNull FlixelString out) {
    out.clear();
    if (target == null) {
      return false;
    }
    FlixelState level = Flixel.state;
    for (int depth = 0; level != null && depth < MAX_DEPTH; depth++) {
      if (depth == 0) {
        out.concat(STATE);
      } else {
        if (depth > 1) {
          out.concat('.');
        }
        out.concat(SUBSTATE);
      }
      if (search(level, target, out, 0)) {
        return true;
      }
      level = level.getSubState();
      // The next level is written as "substate" repeated, so keep the earlier repeats and drop the old state.
      if (depth == 0) {
        out.clear();
      }
    }
    for (int i = 0, n = roots.getTrackedCount(); i < n; i++) {
      out.clear();
      out.concat(roots.getTrackedLabel(i));
      if (search(roots.getTracked(i), target, out, 0)) {
        return true;
      }
    }
    out.clear();
    return false;
  }

  /**
   * Finds the object a path points to.
   *
   * <p>This does not allocate. An unknown path, an index out of range, a step into something that is not a group,
   * or an empty slot gives {@code null}.
   *
   * @param roots The inspector that knows the tracked objects.
   * @param path The path, in the format described in the class comment.
   * @return The object, or {@code null} when the path does not lead to one.
   */
  @Nullable
  public static Object resolve(@NotNull FlixelDebugInspectorPanel roots, @Nullable CharSequence path) {
    if (path == null || path.length() == 0) {
      return null;
    }
    int length = path.length();
    int pos = 0;
    Object node = null;
    if (startsWithStep(path, 0, STATE)) {
      node = Flixel.state;
      pos = STATE.length();
    } else if (startsWithStep(path, 0, SUBSTATE)) {
      node = Flixel.state != null ? Flixel.state.getSubState() : null;
      pos = SUBSTATE.length();
    } else {
      for (int i = 0, n = roots.getTrackedCount(); i < n; i++) {
        String label = roots.getTrackedLabel(i);
        if (startsWithStep(path, 0, label)) {
          node = roots.getTracked(i);
          pos = label.length();
          break;
        }
      }
    }
    while (node != null && pos < length) {
      // Skip the dot in front of the next step.
      pos++;
      if (startsWithStep(path, pos, SUBSTATE)) {
        if (!(node instanceof FlixelState state)) {
          return null;
        }
        node = state.getSubState();
        pos += SUBSTATE.length();
        continue;
      }
      int index = 0;
      int digits = 0;
      while (pos < length && path.charAt(pos) >= '0' && path.charAt(pos) <= '9' && digits < 9) {
        index = index * 10 + (path.charAt(pos) - '0');
        pos++;
        digits++;
      }
      if (digits == 0 || (pos < length && path.charAt(pos) != '.')) {
        return null;
      }
      if (!(node instanceof FlixelGroupable<?> group)) {
        return null;
      }
      FlixelArray<?> members = group.getMembers();
      if (members == null || index >= members.getSize()) {
        return null;
      }
      node = members.get(index);
    }
    return node;
  }

  private static boolean search(Object node, Object target, FlixelString out, int depth) {
    if (node == target) {
      return true;
    }
    if (depth >= MAX_DEPTH || !(node instanceof FlixelGroupable<?> group)) {
      return false;
    }
    FlixelArray<?> members = group.getMembers();
    if (members == null) {
      return false;
    }
    int mark = out.length();
    for (int i = 0, n = members.getSize(); i < n; i++) {
      Object member = members.get(i);
      if (member == null) {
        continue;
      }
      out.concat('.').concat(i);
      if (search(member, target, out, depth + 1)) {
        return true;
      }
      out.setLength(mark);
    }
    return false;
  }

  /** Returns whether the step at {@code from} is exactly {@code step}, ending at a dot or at the end of the path. */
  private static boolean startsWithStep(CharSequence path, int from, String step) {
    int end = from + step.length();
    if (step.isEmpty() || end > path.length()) {
      return false;
    }
    for (int i = 0; i < step.length(); i++) {
      if (path.charAt(from + i) != step.charAt(i)) {
        return false;
      }
    }
    return end == path.length() || path.charAt(end) == '.';
  }
}
