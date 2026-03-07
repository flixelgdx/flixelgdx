package me.stringdotjar.flixelgdx.backend.teavm.alert;

import me.stringdotjar.flixelgdx.backend.FlixelAlerter;
import org.teavm.jso.JSBody;

/**
 * Web (TeaVM) implementation of {@link FlixelAlerter} using the browser's {@code alert()}.
 */
public class FlixelTeaVMAlerter implements FlixelAlerter {

  @Override
  public void showInfoAlert(String title, String message) {
    showAlert(title, message);
  }

  @Override
  public void showWarningAlert(String title, String message) {
    showAlert(title, message);
  }

  @Override
  public void showErrorAlert(String title, String message) {
    showAlert(title, message);
  }

  private void showAlert(String title, String message) {
    String text = (title != null ? title : "") + (message != null ? "\n" + message : "");
    alert(text);
  }

  @JSBody(params = "text", script = "alert(text);")
  private static native void alert(String text);
}
