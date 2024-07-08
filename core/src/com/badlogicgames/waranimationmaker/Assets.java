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
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.util.HashMap;
import java.util.Map;

public class Assets {

	private static final Map<String, Texture> LOADED_TEXTURES = new HashMap<>();
	private static final Map<String, Skin> LOADED_SKINS = new HashMap<>();

	public static Texture loadTexture (String file) {
		if (LOADED_TEXTURES.containsKey(file)) {
			return LOADED_TEXTURES.get(file);
		}

		final Texture texture = new Texture(Gdx.files.internal(file));
		LOADED_TEXTURES.put(file, texture);
		return texture;
	}

	public static Skin loadSkin (String file) {
		if (LOADED_SKINS.containsKey(file)) {
			return LOADED_SKINS.get(file);
		}

		final Skin skin = new Skin(Gdx.files.internal(file));
		LOADED_SKINS.put(file, skin);
		return skin;
	}

	public static void playSound (Sound sound) {
		sound.play(1);
	}
}