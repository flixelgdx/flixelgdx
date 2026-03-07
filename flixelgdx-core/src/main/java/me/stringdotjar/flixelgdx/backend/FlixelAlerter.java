package me.stringdotjar.flixelgdx.backend;

/**
 * Interface for displaying alert notifications to the user.
 */
public interface FlixelAlerter {
  void showInfoAlert(String title, String message);
  void showWarningAlert(String title, String message);
  void showErrorAlert(String title, String message);
}
