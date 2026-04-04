/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.functional.supplier;

/**
 * Represents a supplier of {@code char}-valued results.
 */
@FunctionalInterface
public interface CharSupplier {

  /**
   * Gets a char result.
   *
   * @return a result.
   */
  char getAsChar();
}
