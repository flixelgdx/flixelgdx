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
 *   <li><b>Keyboard:</b> {@link org.flixelgdx.input.keyboard.FlixelKeyInputManager FlixelKeyInputManager} on {@link org.flixelgdx.Flixel#keys Flixel.keys}.</li>
 *   <li><b>Mouse / pointers:</b> {@link org.flixelgdx.input.mouse.FlixelMouseManager FlixelMouseManager} on {@link org.flixelgdx.Flixel#mouse Flixel.mouse}.</li>
 *   <li><b>Gamepads:</b> {@link org.flixelgdx.input.gamepad.FlixelGamepadManager FlixelGamepadManager} on {@link org.flixelgdx.Flixel#gamepads Flixel.gamepads}.</li>
 *   <li><b>Logical actions (rebindable layers):</b> {@link org.flixelgdx.input.action action} ({@link org.flixelgdx.input.action.FlixelActionSet FlixelActionSet}, {@link org.flixelgdx.input.action.FlixelActionDigital FlixelActionDigital}, {@link org.flixelgdx.input.action.FlixelActionAnalog FlixelActionAnalog}).</li>
 * </ul>
 *
 * <p>
 * Action sets do <strong>not</strong> implement {@link com.badlogic.gdx.InputProcessor}; they poll the managers above each frame.
 * See {@link org.flixelgdx.input.action.FlixelActionSet FlixelActionSet} for lifecycle and examples.
 */
package org.flixelgdx.input;
