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
package org.flixelgdx.backend.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import org.flixelgdx.backend.android.input.FlixelAndroidKeyMap;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.jetbrains.annotations.NotNull;

/**
 * The game surface view, extending {@link GLSurfaceView} to support soft-keyboard text input.
 *
 * <p>By overriding {@link #onCheckIsTextEditor()} and {@link #onCreateInputConnection}, this
 * view tells Android that it can accept text from an Input Method Editor (IME). When
 * {@link FlixelAndroidInputDevice#startTextInput()} is called, the launcher shows the IME and
 * focuses this view. The IME then calls back through the {@link FlixelInputConnection} inner
 * class, which forwards committed characters directly into the input device's ring buffer as
 * {@link FlixelAndroidInputDevice#TYPE_CHAR_INPUT} slots. Nothing is rendered to or stored in
 * the view itself.
 *
 * <p>Think of this view like a phone's dial pad: the screen shows a game, not the keys, but
 * the operating system still needs a "text entry" surface to attach its input machinery to.
 *
 * @see FlixelAndroidInputDevice
 * @see FlixelAndroidLauncher
 */
public class FlixelAndroidSurfaceView extends GLSurfaceView {

  @NotNull
  private final FlixelAndroidInputDevice input;

  /**
   * Creates the surface view wired to the given input device.
   *
   * @param context The host activity's context.
   * @param input The input device that receives committed text and key events.
   */
  public FlixelAndroidSurfaceView(@NotNull Context context,
      @NotNull FlixelAndroidInputDevice input) {
    super(context);
    this.input = input;
  }

  /**
   * Returns {@code true} to inform Android that this view can accept text input from an IME.
   * Without this, the system will not attach an input connection to the view.
   *
   * @return Always {@code true}.
   */
  @Override
  public boolean onCheckIsTextEditor() {
    return true;
  }

  /**
   * Creates a new {@link InputConnection} each time the IME connects. The connection type is
   * configured to suppress spell-check and auto-suggestions, since the game manages text itself.
   * The IME action is set to "Done" so the soft keyboard shows a dismissal key.
   *
   * @param outAttrs Filled in with input-type and IME-options flags.
   * @return A new {@link FlixelInputConnection} wired to the input device.
   */
  @Override
  public InputConnection onCreateInputConnection(@NotNull EditorInfo outAttrs) {
    outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    outAttrs.imeOptions =
        EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_DONE;
    return new FlixelInputConnection(this);
  }

  /**
   * The IME bridge: forwards committed text, delete events, and raw key events from the soft
   * keyboard into the input device's ring buffer. The view never stores or displays any text;
   * it is purely a conduit.
   */
  private final class FlixelInputConnection extends BaseInputConnection {

    FlixelInputConnection(@NotNull View targetView) {
      super(targetView, false);
    }

    /**
     * Called by the IME when the user confirms composed text. Each character in {@code text}
     * is forwarded as a {@link FlixelAndroidInputDevice#TYPE_CHAR_INPUT} slot.
     *
     * @param text The committed text string; never {@code null}.
     * @param newCursorPosition Ignored; the game manages the cursor itself.
     * @return Always {@code true}.
     */
    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
      for (int i = 0, n = text.length(); i < n; i++) {
        input.postChar(text.charAt(i));
      }
      return true;
    }

    /**
     * Called by the IME to delete characters around the cursor. Each deleted character before
     * the cursor is mapped to a BACKSPACE key press and release pair.
     *
     * @param beforeLength Number of characters to delete before the cursor.
     * @param afterLength Number of characters to delete after the cursor (mapped to DELETE).
     * @return Always {@code true}.
     */
    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
      for (int i = 0; i < beforeLength; i++) {
        input.postKey(FlixelAndroidInputDevice.TYPE_KEY_DOWN, FlixelKey.DEL);
        input.postKey(FlixelAndroidInputDevice.TYPE_KEY_UP, FlixelKey.DEL);
      }
      for (int i = 0; i < afterLength; i++) {
        input.postKey(FlixelAndroidInputDevice.TYPE_KEY_DOWN, FlixelKey.FORWARD_DEL);
        input.postKey(FlixelAndroidInputDevice.TYPE_KEY_UP, FlixelKey.FORWARD_DEL);
      }
      return true;
    }

    /**
     * Called by the IME when it sends a raw key event (for example, Enter or Backspace on
     * hardware keyboards attached while the IME is active). The keycode is mapped through
     * {@link FlixelAndroidKeyMap} before forwarding.
     *
     * @param event The raw key event from the IME.
     * @return {@code true} when the key was handled by the framework.
     */
    @Override
    public boolean sendKeyEvent(@NotNull KeyEvent event) {
      int flixelKey = FlixelAndroidKeyMap.toFlixelKey(event.getKeyCode());
      if (flixelKey != FlixelKey.UNKNOWN) {
        int type = event.getAction() == KeyEvent.ACTION_DOWN
            ? FlixelAndroidInputDevice.TYPE_KEY_DOWN
            : FlixelAndroidInputDevice.TYPE_KEY_UP;
        input.postKey(type, flixelKey);
        return true;
      }
      return super.sendKeyEvent(event);
    }

    /**
     * Called by the IME when composition finishes. Treats the final composition string the
     * same as committed text so characters always reach the game.
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean finishComposingText() {
      CharSequence text = getEditable();
      if (text != null && text.length() > 0) {
        commitText(text, 1);
      }
      return super.finishComposingText();
    }
  }
}
