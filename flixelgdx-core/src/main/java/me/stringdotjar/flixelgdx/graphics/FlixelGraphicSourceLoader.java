package me.stringdotjar.flixelgdx.graphics;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;

public class FlixelGraphicSourceLoader extends SynchronousAssetLoader<FlixelGraphicSource, FlixelGraphicSourceLoader.FlixelGraphicSourceParameter> {

  public static class FlixelGraphicSourceParameter extends AssetLoaderParameters<FlixelGraphicSource> {
    public boolean external = false;
  }

  public FlixelGraphicSourceLoader(FileHandleResolver resolver) {
    super(resolver);
  }

  @Override
  public FlixelGraphicSource load(AssetManager assetManager,
                                  String fileName,
                                  FileHandle file,
                                  FlixelGraphicSourceParameter parameter) {
    return new FlixelGraphicSource(fileName);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file,
      FlixelGraphicSourceParameter parameter) {
    Array<AssetDescriptor> deps = new Array<>();
    deps.add(new AssetDescriptor<>(fileName, Texture.class));
    return deps;
  }
}
