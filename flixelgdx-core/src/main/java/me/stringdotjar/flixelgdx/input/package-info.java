/**
 * Input event handling and device abstraction for FlixelGDX.
 *
 * <p>
 * This package contains the core classes and interfaces responsible for capturing, processing,
 * and dispatching input events from all supported platforms and devices in FlixelGDX. It serves
 * as the entry point for polling or receiving notifications about user interactions such as
 * keyboard input, mouse movement and clicks, touch gestures, controller/gamepad actions,
 * and virtual or custom device signals. Its design enables consistent, cross-platform handling
 * of user controls for games and interactive applications.
 *
 * <h2>Where things live</h2>
 *
 * <ul>
 *   <li><b>Keyboard:</b> {@link me.stringdotjar.flixelgdx.input.keyboard.FlixelKeyInputManager} on {@link me.stringdotjar.flixelgdx.Flixel#keys}.</li>
 *   <li><b>Mouse / pointers:</b> {@link me.stringdotjar.flixelgdx.input.mouse.FlixelMouseManager} on {@link me.stringdotjar.flixelgdx.Flixel#mouse}.</li>
 *   <li><b>Gamepads:</b> {@link me.stringdotjar.flixelgdx.input.gamepad.FlixelGamepadManager} on {@link me.stringdotjar.flixelgdx.Flixel#gamepads}.</li>
 *   <li><b>Logical actions (rebindable layers):</b> {@link me.stringdotjar.flixelgdx.input.action} ({@link me.stringdotjar.flixelgdx.input.action.FlixelActionSet}, {@link me.stringdotjar.flixelgdx.input.action.FlixelActionDigital}, {@link me.stringdotjar.flixelgdx.input.action.FlixelActionAnalog}).</li>
 * </ul>
 *
 * <p>
 * Action sets do <strong>not</strong> implement {@link com.badlogic.gdx.InputProcessor}; they poll the managers above each frame.
 * See {@link me.stringdotjar.flixelgdx.input.action.FlixelActionSet} for lifecycle and examples.
 */
package me.stringdotjar.flixelgdx.input;
