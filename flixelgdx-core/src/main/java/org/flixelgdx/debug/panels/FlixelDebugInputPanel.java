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
package org.flixelgdx.debug.panels;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.FlixelDebugObjectIds;
import org.flixelgdx.debug.FlixelDebugPicker;
import org.flixelgdx.debug.remote.FlixelDebugAgent;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.input.touch.FlixelTouchManager;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * The input panel of the remote debugger: it lets the debugger app (and the AI agents that drive it) press keys,
 * move and click a pointer, scroll, and type text in the running game.
 *
 * <p>Think of it as a puppeteer's hand that reaches onto the real keyboard and mouse. Nothing special is shown to
 * the game: pressed keys, mouse buttons, the pointer position, the wheel, typed characters, and touches all arrive
 * through the framework's own input managers ({@code Flixel.keys}, {@code Flixel.mouse}, {@code Flixel.touches}),
 * so game code and user interface widgets cannot tell the difference. An injected key shows up in
 * {@code pressed}, {@code rawPressed}, {@code justPressed}, {@code justReleased}, and every keyboard listener; the
 * injected pointer moves {@code Flixel.mouse.getWorldX(camera)}; and the free camera of the
 * {@linkplain FlixelDebugControlsPanel controls panel} never hides injected input from the game.
 *
 * <h2>How input is delivered</h2>
 *
 * <p>Actions from the app go into a small queue. Once per frame, before the input devices are read (see
 * {@link #applyQueued()}), the panel applies the queue. A key or mouse button press and its release are two
 * separate transitions, and the game has to see each of them in its own frame, or a quick tap would be missed. So
 * the panel applies at most one key or button transition per frame and leaves the rest for the next frames. Pointer
 * moves, scrolls, and typed text do not count as transitions, so they are all applied in the frame they reach the
 * front of the queue. A tap therefore takes two frames (press, then release), and typing a sentence takes one.
 *
 * <p>While the game is paused, the queue waits. The next time the game steps a frame (see
 * {@link FlixelDebugControlsPanel#stepFrame()}) or is resumed, the panel applies the queue as usual. This means a
 * paused game never sees input it cannot act on, and an agent can step one frame at a time and know exactly what
 * the game saw in it. The queue holds at most {@value #CAPACITY} actions; further actions are dropped.
 *
 * <p>When the debugger app disconnects, every key and button the panel is holding is released and the queue is
 * emptied, so nothing stays stuck.
 *
 * <h2>Pointer positions</h2>
 *
 * <p>Pointer positions are in the <strong>view space</strong> of the camera that the
 * {@linkplain FlixelDebugViewportPanel viewport panel} shows, the same space as its boxes. The panel converts them
 * to window pixels, so the game's pointer lands on the spot, within a pixel of rounding. The injected pointer stays
 * where it was put until the real mouse moves or the app disconnects. {@link #click(int, int)} clicks the middle of
 * an object's viewport box, which is the simplest way to press a button.
 *
 * <p>If touch input is enabled ({@code Flixel.touches.enabled}), a pointer that presses the left button also
 * starts touch pointer 0, so touch-driven games react to it.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugInputPanel input = Flixel.debug.input;
 * input.key(FlixelKey.SPACE, true);   // Press the jump key...
 * input.key(FlixelKey.SPACE, false);  // ...and let go of it a frame later.
 * input.click(Flixel.debug.ids.idOf(playButton), FlixelMouseButton.LEFT);
 * input.type("Hello");
 * }</pre>
 *
 * <p>All methods must be called from the game's main thread.
 *
 * @see FlixelDebugProtocol#KIND_INPUT
 * @see FlixelKeyInputManager#injectKey(int, boolean)
 * @see FlixelMouseInputManager#injectButton(int, boolean)
 */
public class FlixelDebugInputPanel extends FlixelDebugPanelBase {

  /** The most actions that wait in the queue. */
  public static final int CAPACITY = 256;

  /** The most typed characters that wait in the queue. */
  public static final int TEXT_CAPACITY = 8192;

  private static final int MAX_BUTTON = 4;

  private static final int EVENT_KEY = 0;
  private static final int EVENT_POINTER = 1;
  private static final int EVENT_SCROLL = 2;
  private static final int EVENT_TEXT = 3;
  private static final int EVENT_CLICK = 4;

  private final FlixelVector scratch = new FlixelVector();
  private final FlixelString readText = new FlixelString();
  private final FlixelString texts = new FlixelString();
  private final FlixelDebugPicker picker = new FlixelDebugPicker();
  private final FlixelDebugViewportPanel viewport;
  private final FlixelDebugObjectIds ids;

  private final int[] kinds = new int[CAPACITY];
  private final int[] firsts = new int[CAPACITY];
  private final int[] seconds = new int[CAPACITY];
  private final int[] stages = new int[CAPACITY];
  private final float[] xs = new float[CAPACITY];
  private final float[] ys = new float[CAPACITY];

  private int head;
  private int size;
  private int dropped;
  private int pointerX;
  private int pointerY;
  private int syncedFlags = -1;

  private boolean wasLive;

  /**
   * Creates the input panel.
   *
   * @param viewport The viewport panel whose camera gives pointer positions their meaning.
   * @param ids The id registry that turns an object id of a click into the object.
   */
  public FlixelDebugInputPanel(@NotNull FlixelDebugViewportPanel viewport, @NotNull FlixelDebugObjectIds ids) {
    super("input", "Input", FlixelDebugProtocol.KIND_INPUT);
    this.viewport = viewport;
    this.ids = ids;
    picker.hitboxes = true;
  }

  @Override
  public void update(float elapsed) {
    int flags = computeFlags();
    if (flags != syncedFlags) {
      syncedFlags = flags;
      markChanged();
    }
  }

  /**
   * Applies the queued input to the game's input managers.
   *
   * <p>The game calls this once per frame, before it reads the input devices (see
   * {@link FlixelDebugManager#applyInput()}). It applies at most one key or button transition (see the class
   * comment), and it does nothing while the game is paused and no step is pending. It also releases everything
   * that was held when it notices that the app went away. It does not allocate.
   */
  public void applyQueued() {
    boolean live = isAppLive();
    if (wasLive && !live) {
      release();
    }
    wasLive = live;
    if (size == 0) {
      return;
    }
    FlixelGame game = Flixel.game;
    if (game != null && game.isGamePaused() && !game.isStepPending()) {
      return;
    }
    while (size > 0 && !applyFront()) {
      // Moves, scrolls, and text are applied in the same frame as what follows them.
    }
  }

  /**
   * Queues a key press or release.
   *
   * @param key A {@link FlixelKey} code. Invalid codes are ignored.
   * @param down {@code true} to press, {@code false} to release.
   * @return {@code true} when the action was queued.
   */
  public boolean key(int key, boolean down) {
    FlixelKeyInputManager keys = Flixel.keys;
    if (keys == null || !keys.isValidKeycode(key)) {
      return false;
    }
    return push(EVENT_KEY, key, down ? 1 : 0, 0f, 0f);
  }

  /**
   * Queues a pointer move, press, or release at a point of the viewport's camera.
   *
   * @param phase One of {@link FlixelDebugProtocol#INPUT_POINTER_MOVE}, {@link FlixelDebugProtocol#INPUT_POINTER_DOWN},
   *     or {@link FlixelDebugProtocol#INPUT_POINTER_UP}. A press and a release also move the pointer first.
   * @param button A {@link FlixelMouseButton} code, used by a press and a release.
   * @param viewX The horizontal position in the camera's view space.
   * @param viewY The vertical position in the camera's view space.
   * @return {@code true} when the action was queued. It is {@code false} for an unknown phase, an invalid button, a
   *     position that is not a finite number, or a full queue.
   */
  public boolean pointer(int phase, int button, float viewX, float viewY) {
    if (phase < FlixelDebugProtocol.INPUT_POINTER_MOVE || phase > FlixelDebugProtocol.INPUT_POINTER_UP
        || button < 0 || button > MAX_BUTTON || !Float.isFinite(viewX) || !Float.isFinite(viewY)) {
      return false;
    }
    return push(EVENT_POINTER, phase, button, viewX, viewY);
  }

  /**
   * Queues a mouse wheel scroll.
   *
   * @param amount The scroll amount. A positive amount scrolls down, like a wheel turned towards the user.
   * @return {@code true} when the action was queued. It is {@code false} for a number that is not finite.
   */
  public boolean scroll(float amount) {
    return Float.isFinite(amount) && push(EVENT_SCROLL, 0, 0, amount, 0f);
  }

  /**
   * Queues text to be typed, delivered like characters from a keyboard so text boxes receive it.
   *
   * <p>Text longer than {@value FlixelDebugProtocol#INPUT_MAX_TEXT_CHARS} characters is cut. This does not press
   * any key, so use {@link #key(int, boolean)} for Enter, Backspace, and arrows.
   *
   * @param text The text to type.
   * @return {@code true} when the action was queued. It is {@code false} for empty text or a full queue.
   */
  public boolean type(@NotNull CharSequence text) {
    int length = Math.min(text.length(), FlixelDebugProtocol.INPUT_MAX_TEXT_CHARS);
    if (length == 0 || size >= CAPACITY || texts.length() + length > TEXT_CAPACITY) {
      if (length > 0) {
        dropped++;
      }
      return false;
    }
    int start = texts.length();
    for (int i = 0; i < length; i++) {
      texts.concat(text.charAt(i));
    }
    return push(EVENT_TEXT, start, length, 0f, 0f);
  }

  /**
   * Queues a click on the middle of an object's box: the pointer moves there, the button goes down, and in the
   * next frame it goes up, so {@code justPressed} and {@code justReleased} both fire.
   *
   * <p>The box is the one the {@linkplain FlixelDebugViewportPanel viewport panel} shows for the object on its
   * camera, measured when the click is applied. An unknown id, or an object that is not on that camera at that
   * time, makes the click do nothing.
   *
   * @param id The object's id from {@link FlixelDebugObjectIds}.
   * @param button A {@link FlixelMouseButton} code.
   * @return {@code true} when the action was queued.
   */
  public boolean click(int id, int button) {
    if (button < 0 || button > MAX_BUTTON) {
      return false;
    }
    return push(EVENT_CLICK, id, button, 0f, 0f);
  }

  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    writer.writeU8(computeFlags());
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    switch (action) {
      case FlixelDebugProtocol.INPUT_KEY -> {
        boolean down = reader.readBool();
        key(reader.readI32(), down);
      }
      case FlixelDebugProtocol.INPUT_POINTER -> {
        int phase = reader.readU8();
        int button = reader.readU8();
        pointer(phase, button, reader.readF32(), reader.readF32());
      }
      case FlixelDebugProtocol.INPUT_SCROLL -> scroll(reader.readF32());
      case FlixelDebugProtocol.INPUT_TEXT -> type(reader.readString(readText));
      case FlixelDebugProtocol.INPUT_CLICK_OBJECT -> {
        int id = reader.readI32();
        click(id, reader.readU8());
      }
      default -> {
      }
    }
  }

  /**
   * Releases every key and button the panel holds, hands the pointer back to the real mouse, and empties the
   * queue.
   */
  @Override
  public void reset() {
    release();
    syncedFlags = -1;
  }

  private void release() {
    head = 0;
    size = 0;
    texts.clear();
    FlixelKeyInputManager keys = Flixel.keys;
    if (keys != null) {
      keys.releaseInjected();
    }
    FlixelMouseInputManager mouse = Flixel.mouse;
    if (mouse != null) {
      mouse.releaseInjected();
    }
    FlixelTouchManager touches = Flixel.touches;
    if (touches != null && touches.isInjected(0)) {
      touches.injectTouchReleased(0, pointerX, pointerY);
    }
  }

  /** Applies the action at the front of the queue. Returns whether it was a key or button transition. */
  private boolean applyFront() {
    int i = head;
    int first = firsts[i];
    int second = seconds[i];
    switch (kinds[i]) {
      case EVENT_KEY -> {
        FlixelKeyInputManager keys = Flixel.keys;
        if (keys != null) {
          keys.injectKey(first, second != 0);
        }
        pop();
        return true;
      }
      case EVENT_POINTER -> {
        boolean applied = applyPointer(first, second, xs[i], ys[i]);
        pop();
        return applied && first != FlixelDebugProtocol.INPUT_POINTER_MOVE;
      }
      case EVENT_SCROLL -> {
        FlixelMouseInputManager mouse = Flixel.mouse;
        if (mouse != null) {
          mouse.injectScroll(0f, xs[i]);
        }
        pop();
        return false;
      }
      case EVENT_TEXT -> {
        FlixelKeyInputManager keys = Flixel.keys;
        for (int k = 0; keys != null && k < second; k++) {
          keys.injectTyped(texts.charAt(first + k));
        }
        pop();
        return false;
      }
      default -> {
        return applyClick(i);
      }
    }
  }

  private boolean applyPointer(int phase, int button, float viewX, float viewY) {
    FlixelCamera cam = viewport.getShownCamera();
    if (cam == null) {
      return false;
    }
    scratch.set(viewX, viewY);
    cam.getViewport().project(scratch);
    movePointer(Math.round(scratch.x), Math.round(scratch.y));
    if (phase == FlixelDebugProtocol.INPUT_POINTER_DOWN) {
      press(button, true);
    } else if (phase == FlixelDebugProtocol.INPUT_POINTER_UP) {
      press(button, false);
    }
    return true;
  }

  /** Moves the pointer to the middle of the object on the first call and releases the button on the second. */
  private boolean applyClick(int i) {
    int button = seconds[i];
    if (stages[i] == 1) {
      press(button, false);
      pop();
      return true;
    }
    Object target = ids.get(firsts[i]);
    FlixelCamera cam = viewport.getShownCamera();
    if (target == null || cam == null || !picker.measure(target, cam)) {
      pop();
      return false;
    }
    scratch.set(picker.getBoundsLeft() + picker.getBoundsWidth() * 0.5f,
        picker.getBoundsTop() + picker.getBoundsHeight() * 0.5f);
    cam.getViewport().project(scratch);
    movePointer(Math.round(scratch.x), Math.round(scratch.y));
    press(button, true);
    stages[i] = 1;
    return true;
  }

  private void movePointer(int x, int y) {
    pointerX = x;
    pointerY = y;
    FlixelMouseInputManager mouse = Flixel.mouse;
    if (mouse != null) {
      mouse.injectPosition(x, y);
    }
    FlixelTouchManager touches = Flixel.touches;
    if (touches != null && touches.isInjected(0)) {
      touches.injectTouchDragged(0, x, y);
    }
  }

  private void press(int button, boolean down) {
    FlixelMouseInputManager mouse = Flixel.mouse;
    if (mouse != null) {
      mouse.injectButton(button, down);
    }
    FlixelTouchManager touches = Flixel.touches;
    if (touches == null || button != FlixelMouseButton.LEFT) {
      return;
    }
    if (down && touches.enabled) {
      touches.injectTouched(0, pointerX, pointerY);
    } else if (!down && touches.isInjected(0)) {
      touches.injectTouchReleased(0, pointerX, pointerY);
    }
  }

  private boolean push(int kind, int first, int second, float x, float y) {
    if (size >= CAPACITY) {
      dropped++;
      return false;
    }
    int i = (head + size) % CAPACITY;
    kinds[i] = kind;
    firsts[i] = first;
    seconds[i] = second;
    stages[i] = 0;
    xs[i] = x;
    ys[i] = y;
    size++;
    return true;
  }

  private void pop() {
    head = (head + 1) % CAPACITY;
    size--;
    if (size == 0) {
      texts.clear();
    }
  }

  private static int computeFlags() {
    int flags = FlixelDebugProtocol.INPUT_FLAG_KEYS | FlixelDebugProtocol.INPUT_FLAG_POINTER
        | FlixelDebugProtocol.INPUT_FLAG_SCROLL | FlixelDebugProtocol.INPUT_FLAG_TEXT;
    FlixelTouchManager touches = Flixel.touches;
    if (touches != null && touches.enabled) {
      flags |= FlixelDebugProtocol.INPUT_FLAG_TOUCH;
    }
    return flags;
  }

  private static boolean isAppLive() {
    FlixelDebugManager debug = Flixel.debug;
    FlixelDebugAgent agent = debug != null ? debug.getAgent() : null;
    return agent != null && agent.isLive();
  }

  /**
   * Returns how many actions wait in the queue.
   *
   * @return A count from 0 to {@value #CAPACITY}.
   */
  public int getQueued() {
    return size;
  }

  /**
   * Returns how many actions were dropped because the queue was full.
   *
   * @return The drop count since the panel was created.
   */
  public int getDropped() {
    return dropped;
  }
}
