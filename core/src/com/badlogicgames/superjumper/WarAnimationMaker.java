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

package com.badlogicgames.superjumper;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogicgames.superjumper.originalgame.Settings;

public class WarAnimationMaker extends Game {
	// used by all screens
	GL20 gl;
	public SpriteBatch batcher;
	public ShapeRenderer shapeRenderer;
	public BitmapFont bitmapFont;
	public LoadingScreen loadingScreen;

	public static final int DISPLAY_WIDTH = 1920;
	public static final int DISPLAY_HEIGHT = 1080;
	
	@Override
	public void create () {
		// Initialize rendering objects
		batcher = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		bitmapFont = new BitmapFont();
		loadingScreen = new LoadingScreen(this);

		gl = Gdx.gl;
		Gdx.graphics.setTitle("War Animation Maker");
		Settings.load();
		FileHandler.INSTANCE.load();
		FileHandler.INSTANCE.save();
		Assets.load();
		Menu menu = new Menu(this);
		Gdx.input.setInputProcessor(menu);
		setScreen(menu);
	}
	
	@Override
	public void render() {
		super.render();
	}
}
