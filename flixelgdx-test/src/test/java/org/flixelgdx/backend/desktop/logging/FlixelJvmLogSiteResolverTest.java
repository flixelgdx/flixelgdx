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
package org.flixelgdx.backend.desktop.logging;

import org.flixelgdx.Flixel;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogSite;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelJvmLogSiteResolverTest {

  private static final String FILE = "FlixelJvmLogSiteResolverTest.java";
  private static final String CLASS = FlixelJvmLogSiteResolverTest.class.getName();

  private FlixelDefaultLogger logger;
  private FlixelLogger originalLog;
  private String file;
  private int line;
  private String cls;
  private String method;
  private int calls;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    logger = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    logger.setConsoleSink(null);
    logger.setSiteResolver(new FlixelJvmLogSiteResolver());
    logger.addExtraSink(entry -> {
      FlixelLogSite site = entry.site();
      file = site.getFileName();
      line = site.getLine();
      cls = site.getClassName();
      method = site.getMethodName();
      calls++;
    });
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
  }

  @Test
  void directLoggerCallReportsTheCaller() {
    int expected = new Throwable().getStackTrace()[0].getLineNumber() + 1;
    logger.info("direct");
    assertSite(expected, "directLoggerCallReportsTheCaller");
  }

  @Test
  void everyLevelAndOverloadReportsTheCaller() {
    int expected = new Throwable().getStackTrace()[0].getLineNumber() + 1;
    logger.warn("value {} and {}", 1, 2);
    assertSite(expected, "everyLevelAndOverloadReportsTheCaller");

    expected = new Throwable().getStackTrace()[0].getLineNumber() + 1;
    logger.error("failed", new IllegalStateException("boom"));
    assertSite(expected, "everyLevelAndOverloadReportsTheCaller");

    expected = new Throwable().getStackTrace()[0].getLineNumber() + 1;
    logger.debug("many {} {} {} {}", 1, 2, 3, 4);
    assertSite(expected, "everyLevelAndOverloadReportsTheCaller");
  }

  @Test
  void taggedChildReportsTheCaller() {
    FlixelLogger child = logger.tagged("Child");
    int expected = new Throwable().getStackTrace()[0].getLineNumber() + 1;
    child.info("from a child");
    assertSite(expected, "taggedChildReportsTheCaller");
  }

  @Test
  void flixelFacadeIsSkipped() {
    Flixel.log = logger;
    int expected = new Throwable().getStackTrace()[0].getLineNumber() + 1;
    Flixel.info("through the facade");
    assertSite(expected, "flixelFacadeIsSkipped");

    expected = new Throwable().getStackTrace()[0].getLineNumber() + 1;
    Flixel.warn("with {}", "args");
    assertSite(expected, "flixelFacadeIsSkipped");

    expected = new Throwable().getStackTrace()[0].getLineNumber() + 1;
    Flixel.log.error("through Flixel.log");
    assertSite(expected, "flixelFacadeIsSkipped");
  }

  @Test
  void messageLoggedFromALambdaReportsTheLambdaBody() {
    int[] expected = new int[1];
    Runnable r = () -> {
      expected[0] = new Throwable().getStackTrace()[0].getLineNumber() + 1;
      logger.info("from a lambda");
    };
    r.run();
    assertEquals(1, calls);
    assertEquals(FILE, file);
    assertEquals(expected[0], line);
    assertEquals(CLASS, cls);
    assertTrue(method.startsWith("lambda$"), method);
  }

  @Test
  void unresolvableStackReportsUnknown() {
    FlixelLogSite out = new FlixelLogSite();
    assertFalse(new FlixelJvmLogSiteResolver().resolve(out));
  }

  private void assertSite(int expectedLine, String expectedMethod) {
    assertEquals(FILE, file);
    assertEquals(expectedLine, line);
    assertEquals(CLASS, cls);
    assertEquals(expectedMethod, method);
    assertTrue(calls > 0);
  }
}
