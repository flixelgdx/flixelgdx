/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

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
