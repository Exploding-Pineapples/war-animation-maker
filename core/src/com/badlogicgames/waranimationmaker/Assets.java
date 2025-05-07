/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogicgames.waranimationmaker;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Assets {

	private static final Map<String, Texture> LOADED_TEXTURES = new HashMap<>();
	private static final Map<String, Skin> LOADED_SKINS = new HashMap<>();

	public static String unitKindsPath(String file) {
		return "assets/unitkinds/" + file;
	}
	public static String flagsPath(String file) { return "assets/flags/" + file; }
	public static String mapsPath(String file) { return "assets/maps/" + file; }
	public static Array<String> countryNames = new Array<>();
	public static Array<String> images = new Array<>();
	public static Array<String> unitTypes = new Array<>();

	public static String png(String file) {
		return file + ".png";
	}

	public static Texture loadTexture (String file) {
		if (LOADED_TEXTURES.containsKey(file)) {
			return LOADED_TEXTURES.get(file);
		}

		try {
			final Texture texture = new Texture(Gdx.files.internal(file));
			LOADED_TEXTURES.put(file, texture);
			return texture;
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static Array<String> countryNames() {
		if (countryNames.isEmpty()) {
			updateCountryNames();
		}
		return countryNames;
	}

	public static Array<String> unitTypes() {
		if (unitTypes.isEmpty()) {
			updateUnitTypes();
		}
		return unitTypes;
	}

	public static Array<String> images() {
		if (images.isEmpty()) {
			updateImages();
		}
		return images;
	}

	public static Array<String> listFiles(String path) {
		Array<String> files = new Array<>();
		for (File country : Objects.requireNonNull(Gdx.files.internal(path).file().listFiles())) {
			files.add(country.getName());
		}
		return files;
	}

	public static void updateCountryNames() {
		countryNames.clear();
		countryNames = listFiles("assets/flags");
	}

	public static void updateImages() {
		images.clear();
		images = listFiles("assets/maps");
	}

	public static void updateUnitTypes() {
		unitTypes.clear();
		unitTypes = listFiles("assets/unitkinds");
	}

	public static Skin loadSkin (String file) {
		if (LOADED_SKINS.containsKey(file)) {
			return LOADED_SKINS.get(file);
		}

		final Skin skin = new Skin(Gdx.files.internal(file));
		LOADED_SKINS.put(file, skin);
		return skin;
	}

	public static BitmapFont loadFont() {
		Texture texture = new Texture(Gdx.files.internal("assets/fonts/bitstream_vera_sans/distancefield.png"), true); // true enables mipmaps
		texture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear); // linear filtering in nearest mipmap image
		return new BitmapFont(Gdx.files.internal("assets/fonts/bitstream_vera_sans/distancefield.fnt"), new TextureRegion(texture), false);
	}

	public static void playSound (Sound sound) {
		sound.play(1);
	}
}