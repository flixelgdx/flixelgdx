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
package org.flixelgdx.audio;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

/**
 * AssetManager loader that creates {@link FlixelSoundSource} instances from an asset key.
 *
 * <p>No file IO is performed here; the source spawns {@link FlixelSound} instances when played.
 */
public final class FlixelSoundSourceLoader
    extends SynchronousAssetLoader<FlixelSoundSource, FlixelSoundSourceLoader.FlixelSoundSourceParameter> {

  public static final class FlixelSoundSourceParameter extends AssetLoaderParameters<FlixelSoundSource> {
    public boolean external = false;
  }

  public FlixelSoundSourceLoader(FileHandleResolver resolver) {
    super(resolver);
  }

  @Override
  public FlixelSoundSource load(AssetManager assetManager,
      String fileName,
      FileHandle file,
      FlixelSoundSourceParameter parameter) {
    boolean external = parameter != null && parameter.external;
    return new FlixelSoundSource(fileName, external);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file,
      FlixelSoundSourceParameter parameter) {
    return null;
  }
}
