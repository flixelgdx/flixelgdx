/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.audio;

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
public final class FlixelSoundSourceLoader extends SynchronousAssetLoader<FlixelSoundSource, FlixelSoundSourceLoader.FlixelSoundSourceParameter> {

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
  public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, FlixelSoundSourceParameter parameter) {
    return null;
  }
}

