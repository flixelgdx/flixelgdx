package me.stringdotjar.flixelgdx.backend.ios.alert;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import me.stringdotjar.flixelgdx.backend.FlixelAlerter;
import org.robovm.apple.dispatch.DispatchQueue;
import org.robovm.apple.uikit.UIAlertAction;
import org.robovm.apple.uikit.UIAlertActionStyle;
import org.robovm.apple.uikit.UIAlertController;
import org.robovm.apple.uikit.UIAlertControllerStyle;
import org.robovm.apple.uikit.UIViewController;

/**
 * iOS implementation of {@link FlixelAlerter} using {@link UIAlertController}.
 */
public class FlixelIOSAlerter implements FlixelAlerter {

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

  private void showAlert(final String title, final String message) {
    DispatchQueue.getMainQueue().async(() -> {
      if (!(Gdx.app instanceof IOSApplication)) {
        return;
      }
      UIViewController root = ((IOSApplication) Gdx.app).getUIViewController();
      if (root == null) {
        return;
      }
      UIAlertController alert = new UIAlertController(
          title,
          message != null ? message : "",
          UIAlertControllerStyle.Alert
      );
      alert.addAction(new UIAlertAction("OK", UIAlertActionStyle.Default, null));
      root.presentViewController(alert, true, null);
    });
  }
}
