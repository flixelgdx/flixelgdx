package org.flixelgdx.backend.html5;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;

/**
 * Helper class for interacting with the browser's native Window Management API.
 */
public final class FlixelHtml5MonitorHelper {

  private FlixelHtml5MonitorHelper() {}

  @JSBody(params = { "callback" }, script = """
      if (!window.getScreenDetails) {
        callback(null);
        return;
      }
      window.getScreenDetails().then(details => {
        callback(details.screens);
        details.addEventListener('screenschange', () => callback(details.screens));
      }).catch(err => {
        callback(null);
      });"""
  )
  public static native void requestScreenDetails(MonitorUpdateCallback callback);

  public interface MonitorUpdateCallback {
    void onUpdate(JSArray<JSScreenDetailed> screens);
  }

  public interface JSWindowManagement extends JSObject {

    @JSProperty
    JSArray<JSScreenDetailed> getScreens(); // Maps to screenDetails.screens
  }

  public interface JSScreenDetailed extends JSObject {

    @JSProperty
    String getLabel();

    @JSProperty
    int getWidth();

    @JSProperty
    int getHeight();

    @JSProperty
    int getLeft();

    @JSProperty
    int getTop();

    @JSProperty(value = "isPrimary")
    boolean isPrimary();
  }

  @JSBody(script = "return typeof window !== 'undefined' && 'getScreenDetails' in window;")
  public static native boolean isWindowManagementSupported();
}
