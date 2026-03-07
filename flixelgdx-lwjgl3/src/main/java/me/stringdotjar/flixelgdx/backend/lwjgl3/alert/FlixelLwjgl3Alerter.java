package me.stringdotjar.flixelgdx.backend.lwjgl3.alert;

import me.stringdotjar.flixelgdx.backend.FlixelAlerter;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;

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
