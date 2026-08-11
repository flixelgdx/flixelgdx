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
package org.flixelgdx.backend.desktop.input;

import org.flixelgdx.input.mouse.FlixelMouseCursor;
import org.flixelgdx.input.mouse.FlixelMouseIconManager;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.sdl.SDLMouse;

/**
 * SDL3-backed cursor manager for the desktop backend.
 *
 * <p>System cursors are created lazily on first use and cached for the session lifetime. Call
 * {@link #dispose()} before SDL shuts down to free all cursor handles. The runner is responsible
 * for calling it in the correct order before {@code SDL_Quit}.
 *
 * <p>The {@link FlixelMouseCursor#GRAB} and {@link FlixelMouseCursor#GRABBING} presets have no
 * matching SDL3 system cursor, so both fall back to the default arrow. All other presets map
 * directly to an SDL3 system cursor.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.mouse.icons.setCursor(FlixelMouseCursor.IBEAM);
 * Flixel.mouse.icons.resetCursor();
 * }</pre>
 */
public class FlixelSdlMouseIconManager implements FlixelMouseIconManager {

  /**
   * SDL3 system cursor ID for each {@link FlixelMouseCursor} ordinal. A value of {@code -1}
   * means the cursor entry is handled specially (currently only {@link FlixelMouseCursor#NONE},
   * which hides the cursor rather than loading a system cursor).
   */
  private static final int[] SDL_CURSOR_IDS = {
      SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT,     // ARROW
      SDLMouse.SDL_SYSTEM_CURSOR_TEXT,        // IBEAM
      SDLMouse.SDL_SYSTEM_CURSOR_WAIT,        // WAIT
      SDLMouse.SDL_SYSTEM_CURSOR_CROSSHAIR,   // CROSSHAIR
      SDLMouse.SDL_SYSTEM_CURSOR_POINTER,     // HAND
      SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT,     // GRAB (no SDL3 equivalent, falls back to arrow)
      SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT,     // GRABBING (no SDL3 equivalent, falls back to arrow)
      SDLMouse.SDL_SYSTEM_CURSOR_EW_RESIZE,   // HORIZONTAL_RESIZE
      SDLMouse.SDL_SYSTEM_CURSOR_NS_RESIZE,   // VERTICAL_RESIZE
      SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE, // NORTH_WEST_SOUTH_EAST_RESIZE
      SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE, // NORTH_EAST_SOUTH_WEST_RESIZE
      SDLMouse.SDL_SYSTEM_CURSOR_MOVE,        // ALL_RESIZE
      SDLMouse.SDL_SYSTEM_CURSOR_NOT_ALLOWED, // NOT_ALLOWED
      -1,                                     // NONE (hide cursor via SDL_HideCursor)
  };

  /** Cached SDL cursor handle per {@link FlixelMouseCursor} ordinal; {@code 0} = not yet created. */
  private final long[] handles = new long[FlixelMouseCursor.values().length];

  private FlixelMouseCursor current = FlixelMouseCursor.ARROW;
  private boolean hidden;

  @Override
  public void setCursor(@NotNull FlixelMouseCursor cursor) {
    current = cursor;
    if (cursor == FlixelMouseCursor.NONE) {
      if (!hidden) {
        SDLMouse.SDL_HideCursor();
        hidden = true;
      }
      return;
    }
    if (hidden) {
      SDLMouse.SDL_ShowCursor();
      hidden = false;
    }
    SDLMouse.SDL_SetCursor(handleFor(cursor));
  }

  @Override
  public void resetCursor() {
    setCursor(FlixelMouseCursor.ARROW);
  }

  @Override
  @NotNull
  public FlixelMouseCursor getCursor() {
    return current;
  }

  @Override
  public boolean supportsCursors() {
    return true;
  }

  /**
   * Destroys all cached SDL cursor handles.
   *
   * <p>Must be called before SDL shuts down. The runner invokes this automatically at the end of
   * the game loop, before {@code bgfx_shutdown} and {@code SDL_Quit}.
   */
  public void dispose() {
    for (int i = 0; i < handles.length; i++) {
      if (handles[i] != 0L) {
        SDLMouse.SDL_DestroyCursor(handles[i]);
        handles[i] = 0L;
      }
    }
  }

  private long handleFor(@NotNull FlixelMouseCursor cursor) {
    int ordinal = cursor.ordinal();
    if (handles[ordinal] == 0L) {
      handles[ordinal] = SDLMouse.SDL_CreateSystemCursor(SDL_CURSOR_IDS[ordinal]);
    }
    return handles[ordinal];
  }
}
