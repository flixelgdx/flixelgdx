/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Starts a minimal libGDX headless application so {@link Gdx#app} and related statics are valid for tests.
 */
public final class GdxHeadlessExtension implements BeforeAllCallback, AfterAllCallback {

  private static HeadlessApplication application;
  private static int refCount;

  @Override
  public synchronized void beforeAll(ExtensionContext context) {
    if (refCount == 0) {
      HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
      application = new HeadlessApplication(new ApplicationAdapter() {}, configuration);
    }
    refCount++;
  }

  @Override
  public synchronized void afterAll(ExtensionContext context) {
    refCount--;
    if (refCount == 0 && application != null) {
      application.exit();
      application = null;
    }
  }
}
