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
package me.stringdotjar.flixelgdx.input.gamepad;

/**
 * High-level controller family used for UI prompts (for example "press A" vs "press X") and
 * telemetry. Input indices still come from each {@link com.badlogic.gdx.controllers.Controller#getMapping()}.
 */
public enum FlixelGamepadModel {

  /** Sony DualShock 4 and compatible controllers. */
  PS4,

  /** Sony DualSense and compatible controllers. */
  PS5,

  /** Microsoft Xbox 360 style layout. */
  XBOX_360,

  /** Microsoft Xbox One and Series style layout. */
  XBOX_ONE,

  /** Nintendo Switch Pro Controller and similar layouts. */
  SWITCH_PRO,

  /** Nintendo Wii and Wii U style controllers when reported as such by the backend. */
  WII,

  /** OUYA controller. */
  OUYA,

  /**
   * Family not recognized from the runtime name. Polling still uses that controller's
   * {@link com.badlogic.gdx.controllers.Controller#getMapping()} like any other model.
   */
  UNKNOWN
}
