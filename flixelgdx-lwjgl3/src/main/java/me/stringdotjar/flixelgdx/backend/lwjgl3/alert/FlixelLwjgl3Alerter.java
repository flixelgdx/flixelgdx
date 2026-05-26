/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3.alert;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;

import me.stringdotjar.flixelgdx.backend.alert.FlixelAlerter;

public class FlixelLwjgl3Alerter implements FlixelAlerter {

  @Override
  public void showInfoAlert(String title, String message) {
    showAlert(title, message, JOptionPane.INFORMATION_MESSAGE);
  }

  @Override
  public void showWarningAlert(String title, String message) {
    showAlert(title, message, JOptionPane.WARNING_MESSAGE);
  }

  @Override
  public void showErrorAlert(String title, String message) {
    showAlert(title, message, JOptionPane.ERROR_MESSAGE);
  }

  private void showAlert(String title, Object message, int type) {
    String msg = message != null ? message.toString() : "null";
    // AWT is unavailable in GraalVM native image. The property is set to "runtime"
    // when the binary runs as a compiled native executable. Attempting to access
    // java.awt.EventQueue in that context calls Toolkit.<clinit>, which tries to
    // load native AWT libraries and crashes with a JNI FatalError.
    if (System.getProperty("org.graalvm.nativeimage.imagecode") != null) {
      return;
    }
    if (EventQueue.isDispatchThread()) {
      JOptionPane.showMessageDialog(null, msg, title, type);
    } else {
      try {
        EventQueue.invokeAndWait(() -> {
          JOptionPane.showMessageDialog(null, msg, title, type);
        });
      } catch (InterruptedException | InvocationTargetException e) {
        // Ignore.
      }
    }
  }
}
