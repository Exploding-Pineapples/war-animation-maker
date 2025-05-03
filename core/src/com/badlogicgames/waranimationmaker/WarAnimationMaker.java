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

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class WarAnimationMaker extends Game {
	// Used by all screens
	GL20 gl;
	public SpriteBatch batcher;
	public ShapeRenderer shapeRenderer;
	public BitmapFont bitmapFont;
	public ShaderProgram fontShader;
	public Skin skin;
	public InputMultiplexer multiplexer;
	public GlyphLayout layout;
	public static final int DISPLAY_WIDTH = 1920;
	public static final int DISPLAY_HEIGHT = 1080;

	public Menu menu;

	@Override
	public void create() {
		// Initialize rendering objects
		batcher = new SpriteBatch();
		layout = new GlyphLayout();
		shapeRenderer = new ShapeRenderer();
		bitmapFont = Assets.loadFont();
		fontShader = new ShaderProgram(Gdx.files.internal("assets/fonts/bitstream_vera_sans/font.vert"), Gdx.files.internal("assets/fonts/bitstream_vera_sans/font.frag"));
		if (!fontShader.isCompiled()) {
			Gdx.app.error("fontShader", "compilation failed:\n" + fontShader.getLog());
		}
		skin = Assets.loadSkin("skins/glassy/skin/glassy-ui.json");
		multiplexer = new InputMultiplexer();

		gl = Gdx.gl;
		Gdx.graphics.setTitle("War Animation Maker");
		FileHandler.INSTANCE.load();
		FileHandler.INSTANCE.save();
		menu = new Menu(this);
		setScreen(menu);
	}

	@Override
	public void render() {
		super.render();
	}
}
