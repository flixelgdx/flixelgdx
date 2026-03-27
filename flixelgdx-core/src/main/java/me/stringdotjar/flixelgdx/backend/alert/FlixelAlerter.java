/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.alert;

/**
 * Interface for displaying alert notifications to the user.
 */
public interface FlixelAlerter {
  void showInfoAlert(String title, String message);
  void showWarningAlert(String title, String message);
  void showErrorAlert(String title, String message);
}
