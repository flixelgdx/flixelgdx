/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input;

import com.badlogic.gdx.InputProcessor;

/**
 * Input manager that participates in libGDX {@link com.badlogic.gdx.InputMultiplexer} wiring.
 *
 * <p>Gamepads use {@link com.badlogic.gdx.controllers.ControllerListener} instead and therefore
 * implement only {@link FlixelInputManager}.
 */
public interface FlixelInputProcessorManager extends FlixelInputManager {

  /**
   * Stable {@link InputProcessor} instance safe to register on the multiplexer for the whole game
   * session.
   *
   * @return Non-null processor instance.
   */
  InputProcessor getInputProcessor();
}
