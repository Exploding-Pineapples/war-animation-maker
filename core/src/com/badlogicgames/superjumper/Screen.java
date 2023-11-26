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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogicgames.superjumper.models.Animation;
import com.badlogicgames.superjumper.models.Line;
import com.badlogicgames.superjumper.models.Node;
import com.badlogicgames.superjumper.models.Object;
import com.badlogicgames.superjumper.models.Unit;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.Math.floor;

public class Screen extends ScreenAdapter implements InputProcessor {
	WarAnimationMaker game;
	OrthographicCamera camera;
	Rectangle soundBounds;
	Rectangle playBounds;
	Rectangle highscoresBounds;
	Rectangle helpBounds;
	Vector3 touchPoint;
	Animation animation;
	Integer time;
	TextureRegion backgroundmap;
	List<Node> nodes;
	Object selected;
	double[][] keyPoints;
	PolynomialSplineFunction xFunction;
	PolynomialSplineFunction yFunction;
	boolean up_pressed;
	boolean down_pressed;
	boolean left_pressed;
	boolean right_pressed;
	public static final int DISPLAY_WIDTH = 1920;
	public static final int DISPLAY_HEIGHT = 1080;

	private final ShapeRenderer shapeRenderer = new ShapeRenderer();
	private final BitmapFont bitmapFont = new BitmapFont();

	public Screen(WarAnimationMaker game) {
		this.game = game;

		camera = new OrthographicCamera(DISPLAY_WIDTH, DISPLAY_HEIGHT);
		camera.position.set(DISPLAY_WIDTH / 2.0f, DISPLAY_HEIGHT / 2.0f, 0);
		camera.setToOrtho(false, DISPLAY_WIDTH, DISPLAY_HEIGHT);

		soundBounds = new Rectangle(0, 0, 64, 64);
		playBounds = new Rectangle(160 - 150, 200 + 18, 300, 36);
		highscoresBounds = new Rectangle(160 - 150, 200 - 18, 300, 36);
		helpBounds = new Rectangle(160 - 150, 200 - 18 - 36, 300, 36);
		touchPoint = new Vector3();
		bitmapFont.getData().setScale(2.5f);

		animation = FileHandler.INSTANCE.getAnimations().get(0);
		time = 0;
		}

	public void update () {
		if (Gdx.input.justTouched()) {
			camera.unproject(touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0));

			/*if (soundBounds.contains(touchPoint.x, touchPoint.y)) {
				Assets.playSound(Assets.clickSound);
				Settings.soundEnabled = !Settings.soundEnabled;

			}*/
		}

		Float[] rect = {camera.position.x, camera.position.y, camera.viewportWidth/camera.zoom, camera.viewportHeight/camera.zoom};

		int mapw = animation.getImageDimensions().getFirst();
		int maph = animation.getImageDimensions().getSecond();

		System.out.println(camera.zoom);

		//rect[0]*resizeratio, rect[1]*resizeratio, rect[2]*resizeratio, rect[3]*resizeratio
		int viewwidth = (int) (DISPLAY_WIDTH/camera.zoom);
		int viewheight = (int) (DISPLAY_HEIGHT/camera.zoom);

		backgroundmap = new TextureRegion(Assets.background, (int) (camera.position.x - (viewwidth-DISPLAY_WIDTH)/2.0f), (int) (DISPLAY_HEIGHT - camera.position.y - (viewheight-DISPLAY_WIDTH)/2.0f), viewwidth, viewheight);

		nodes = animation.getLines().get(0).getNodes();

		double[] xValues = new double[nodes.size()];
		double[] yValues = new double[nodes.size()];
		double[] evalAt = new double[nodes.size()];


		for (double i = 0; i < 1.0f; i += 1.0f / nodes.size()) {
			evalAt[(int) (i * nodes.size())] = i;
		}

		Node node;

		for (int i = 0; i < nodes.size(); i++) {
			node = nodes.get(i);
			node.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
			xValues[i] = node.getScreenPosition().getX();
			yValues[i] = node.getScreenPosition().getY();
		}

		keyPoints = new double[xValues.length][2];

		for (int i = 0; i < xValues.length; i++) {
			keyPoints[i] = new double[]{xValues[i], yValues[i]};
		}

		// Create a spline interpolator
		SplineInterpolator interpolator = new SplineInterpolator();
		xFunction = interpolator.interpolate(evalAt, xValues);
		yFunction = interpolator.interpolate(evalAt, yValues);

		if (up_pressed) {
			camera.position.y += 10/camera.zoom;
		}
		if (down_pressed) {
			camera.position.y -= 10/camera.zoom;
		}
		if (left_pressed) {
			camera.position.x -= 10/camera.zoom;
		}
		if (right_pressed) {
			camera.position.x += 10/camera.zoom;
		}

		time++;
	}
	public void draw() {
		GL20 gl = Gdx.gl;
		gl.glClearColor(0, 0, 0, 1);
		gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		camera.update();

		//Draw background and FPS text
		game.batcher.begin();
		game.batcher.draw(backgroundmap, 0.0F, 0.0F, DISPLAY_WIDTH, DISPLAY_HEIGHT);
		bitmapFont.draw(game.batcher, Gdx.graphics.getFramesPerSecond() + " FPS", 30, (float) DISPLAY_HEIGHT - 30);
		game.batcher.end();



		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(Color.GREEN);
		for (double[] point : keyPoints) {
			shapeRenderer.circle((float) point[0], (float) point[1], 7);
		}

		shapeRenderer.setColor(Color.RED);
		int num = 100; //the number of straight lines that will be used to approximate the spline function
		double[][] interpolated = new double[num][2];
		for (double i = 1.0f / num; i < 1 - 1.0f / nodes.size(); i += 1.0f / num) {
			shapeRenderer.rectLine((float) xFunction.value(i - 1.0f / num), (float) yFunction.value(i - 1.0f / num), (float) xFunction.value(i), (float) yFunction.value(i), 5.0f);
		}

		shapeRenderer.end();
	}

	public boolean keyDown (int keycode) {
		if (keycode == Input.Keys.UP) {
			up_pressed = true;
		}
		if (keycode == Input.Keys.DOWN) {
			down_pressed = true;
		}
		if (keycode == Input.Keys.LEFT) {
			left_pressed = true;
		}
		if (keycode == Input.Keys.RIGHT) {
			right_pressed = true;
		}
		return true;
	}

	public boolean keyUp (int keycode) {
		if (keycode == Input.Keys.UP) {
			up_pressed = false;
		}
		if (keycode == Input.Keys.DOWN) {
			down_pressed = false;
		}
		if (keycode == Input.Keys.LEFT) {
			left_pressed = false;
		}
		if (keycode == Input.Keys.RIGHT) {
			right_pressed = false;
		}
		return true;
	}

	public boolean keyTyped (char character) {
		return false;
	}

	public boolean touchDown (int x, int y, int pointer, int button) {
		float mousex = (float) (floor(x)-camera.position.x*(1-camera.zoom)-(DISPLAY_WIDTH/2.0f-camera.position.x))/camera.zoom;
		float mousey = (float) (floor(y)-camera.position.y*(1-camera.zoom)-(DISPLAY_HEIGHT/2.0f-camera.position.y))/camera.zoom;

		List<Node> nodeList = Stream.concat(animation.getArea().getNodes().stream(), animation.getLines().get(0).getNodes().stream()).toList();
		List<Unit> unitList = animation.getUnits();

		y = DISPLAY_HEIGHT - y; //for some reason clicked is called with top left (0, 0) instead of bottom left

		if (selected != null) {
			selected.newSetPoint(time, x, y);
			selected = null;
			return true;
		}

		for (Object object : nodeList) {
			if (object.clicked(x, y)) {
				System.out.println(object.getPosition() + " was clicked");
				selected = object;
				return true;
			}
		}
		for (Object object : unitList) {
			if (object.clicked(x, y)) {
				System.out.println(object.getPosition() + " was clicked");
				selected = object;
				return true;
			}
		}
		return false;
	}

	public boolean touchUp (int x, int y, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	public boolean touchDragged (int x, int y, int pointer) {
		return false;
	}
	public boolean mouseMoved (int x, int y) {
		return false;
	}
	@Override
	public boolean scrolled (float amountX, float amountY) {
		camera.zoom *= 1 - 0.05 * amountY;
		return  true;
	}

	@Override
	public void render (float delta) {
		update();
		draw();
	}

	@Override
	public void pause () {
		Settings.save();
		FileHandler.INSTANCE.save();
	}
}
