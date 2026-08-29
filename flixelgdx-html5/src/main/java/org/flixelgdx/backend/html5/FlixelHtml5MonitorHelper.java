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
package org.flixelgdx.backend.html5;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;

/**
 * Helper class for interacting with the browser's native Window Management API.
 */
public final class FlixelHtml5MonitorHelper {

  private FlixelHtml5MonitorHelper() {}

  /**
   * Callback that gets executed whenever JavaScript triggers a {@code 'screenschange'} event.
   *
   * @see #requestScreenDetails
   */
  @JSFunctor
  public interface MonitorUpdateCallback extends JSObject {
    void onUpdate(JSArray<JSScreen> screens);
  }

  /**
   * JavaScript data container which holds information of a computer monitor.
   *
   * <p>This is primarily used in {@link MonitorUpdateCallback} inside of a TeaVM {@link JSArray}.
   *
   * @see MonitorUpdateCallback
   * @see #requestScreenDetails
   */
  public interface JSScreen extends JSObject {

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

  /**
   * Native function that returns if the Window Management API is allowed to be used on the
   * JavaScript side.
   *
   * @return If the browser's native Window Management API is available.
   */
  @JSBody(script = "return typeof window !== 'undefined' && 'getScreenDetails' in window;")
  public static native boolean isWindowManagementSupported();

  /**
   * Sends a browser request to the user for permission to have access to window management.
   *
   * <p>This is primarily used by {@link FlixelHtml5HostIntegration#requestMonitorPermission()}
   * and inside {@link FlixelHtml5Runner} during the boot sequence (aka during startup). It is
   * incredibly important to note that this method should be called only <b>once</b>, as it registers
   * a {@code 'screenschange'} callback on the JavaScript side, which automatically updates the
   * monitors list that {@link FlixelHtml5HostIntegration} uses.
   *
   * @param callback The {@link MonitorUpdateCallback} that will be used when JavaScript
   *     triggers a {@code 'screenschange'} callback.
   * @author stringdotjar
   */
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
      });""")
  public static native void requestScreenDetails(MonitorUpdateCallback callback);
}
