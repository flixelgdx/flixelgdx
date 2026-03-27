/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import org.jetbrains.annotations.NotNull;

/**
 * AssetManager loader that loads a text file into a {@link String}.
 *
 * <p>This is intended for caching data files (JSON, YAML, config, etc.) as raw text. Parsing can be
 * layered on top without forcing a dependency choice into the core.
 */
public final class FlixelStringAssetLoader extends SynchronousAssetLoader<String, FlixelStringAssetLoader.StringParameter> {

  public static final class StringParameter extends AssetLoaderParameters<String> {
    @NotNull
    public String charset = "UTF-8";
  }

  public FlixelStringAssetLoader(FileHandleResolver resolver) {
    super(resolver);
  }

  @Override
  public String load(AssetManager assetManager,
                     String fileName,
                     FileHandle file,
                     StringParameter parameter) {
    String charset = (parameter != null && parameter.charset != null) ? parameter.charset : "UTF-8";
    return file.readString(charset);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, StringParameter parameter) {
    return null;
  }
}

