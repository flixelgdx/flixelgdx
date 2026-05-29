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

