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
