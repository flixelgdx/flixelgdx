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
    // AWT cannot be used in a GraalVM native image binary: Toolkit.<clinit> loads
    // native AWT libraries via JNI_FatalError, which is non-recoverable. Fall back
    // to stderr so the user still sees the alert text.
    if (System.getProperty("org.graalvm.nativeimage.imagecode") != null) {
      System.err.println("[FlixelGDX Alert] " + title + ": " + msg);
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
