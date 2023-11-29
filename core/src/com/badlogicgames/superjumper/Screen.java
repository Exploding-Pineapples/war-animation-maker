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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogicgames.superjumper.models.*;
import com.badlogicgames.superjumper.models.Animation;
import com.badlogicgames.superjumper.models.Object;
import earcut4j.Earcut;
import kotlin.collections.AbstractMutableList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    List<Unit> units;
    Object selected;
    double[][] keyPoints;
    PolynomialSplineFunction xFunction;
    PolynomialSplineFunction yFunction;
    boolean up_pressed;
    boolean down_pressed;
    boolean left_pressed;
    boolean right_pressed;
    boolean paused;
    boolean animationMode;
    float mousex;
    float mousey;
    float zoomfactor;
    List<float[]> polygon;
    public static final int DISPLAY_WIDTH = 1920;
    public static final int DISPLAY_HEIGHT = 1080;
    public static final int IMAGE_WIDTH = 40;
    public static final int IMAGE_HEIGHT = 40;

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
        paused = true;
        animationMode = true;
        polygon = new ArrayList<>();
    }

    public void update() {
        if (Gdx.input.justTouched()) {
            camera.unproject(touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0));
			/*if (soundBounds.contains(touchPoint.x, touchPoint.y)) {
				Assets.playSound(Assets.clickSound);
				Settings.soundEnabled = !Settings.soundEnabled;

			}*/
        }
        camera.update();

        nodes = animation.getLines().get(0).getNodes();
        units = animation.getUnits();

        //Update all nodes and interpolate line
        double[] xValues = new double[nodes.size()];
        double[] yValues = new double[nodes.size()];
        double[] evalAt = new double[nodes.size()];

        for (int i = 0; i < nodes.size(); i += 1) {
            evalAt[i] = i;
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

        //Update background screen
        Float[] rect = {camera.position.x, camera.position.y, camera.viewportWidth / camera.zoom, camera.viewportHeight / camera.zoom};

        int viewwidth = (int) (DISPLAY_WIDTH / camera.zoom);
        int viewheight = (int) (DISPLAY_HEIGHT / camera.zoom);

        zoomfactor = 0.75f + camera.zoom / 8;

        backgroundmap = new TextureRegion(Assets.background, (int) (camera.position.x - (viewwidth - DISPLAY_WIDTH) / 2.0f), (int) (DISPLAY_HEIGHT - camera.position.y - (viewheight - DISPLAY_WIDTH) / 2.0f), viewwidth, viewheight);

        //Update all units
        for (Unit unit : units) {
            unit.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
        }

        //Update area polygon (red shaded area)
        if (polygon.size() > 0) {
            polygon.clear();
        }

        double[] doublePoly = new double[animation.getArea().getNodes().size() * 2]; //list of polygon vertices in double, all calculation libraries use doubles

        int i = 0;
        for (Node n : animation.getArea().getNodes()) { //updates all the area nodes and adds their positions to the poly list
            n.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
            doublePoly[i] = n.getScreenPosition().getX();
            doublePoly[i + 1] = n.getScreenPosition().getY();
            i += 2;
        }

        List<Node> line = animation.getLines().get(0).getNodes();

        float num = 1000.00f;
        double[] nodePositions = new double[(int) num * 2]; //stores the positions of all the interpolated points along the red line (not the green set points), this is used to draw polygon

        int k = (int) num * 2 - 2;
        for (float j = nodes.size()/num; j < (float) nodes.size() - 1.00f; j += (float) nodes.size()/num) {
            nodePositions[k] = xFunction.value(j);
            nodePositions[k + 1] = yFunction.value(j);
            k -= 2;
        }

        Node firstNode = line.get(0);
        Node lastNode = line.get(line.size() - 1);
        List<Integer> firstAdjacentIndexes = PolygonUtils.findAdjacentVertices(doublePoly, new double[]{firstNode.getScreenPosition().getX(), firstNode.getScreenPosition().getY()}); //util finds which polygon vertices are next to both ends of the frontline
        List<Integer> lastAdjacentIndexes = PolygonUtils.findAdjacentVertices(doublePoly, new double[]{lastNode.getScreenPosition().getX(), lastNode.getScreenPosition().getY()});

        doublePoly = PolygonUtils.polygon_points(doublePoly, new int[]{firstAdjacentIndexes.get(1), lastAdjacentIndexes.get(0)});

        double[] superList = ArrayUtils.addAll(nodePositions, doublePoly); //this is the final list to be used to draw the polygon

        List<Integer> triangulated = Earcut.earcut(superList); //turns polygon into series of triangles represented by polygon vertex indexes
        float v1x, v1y, v2x, v2y, v3x, v3y;
        for (i = 0; i < triangulated.size(); i += 3) {
            v1x = (float) superList[triangulated.get(i) * 2];
            v1y = (float) superList[triangulated.get(i) * 2 + 1];
            v2x = (float) superList[triangulated.get(i + 1) * 2];
            v2y = (float) superList[triangulated.get(i + 1) * 2 + 1];
            v3x = (float) superList[triangulated.get(i + 2) * 2];
            v3y = (float) superList[triangulated.get(i + 2) * 2 + 1];
            polygon.add(new float[]{v1x, v1y, v2x, v2y, v3x, v3y}); //the polygon instance variable is then drawn by triangles in the draw function
        }

        //Handle repeated key inputs
        if (up_pressed) {
            camera.position.y += 10 / camera.zoom;
        }
        if (down_pressed) {
            camera.position.y -= 10 / camera.zoom;
        }
        if (left_pressed) {
            camera.position.x -= 10 / camera.zoom;
        }
        if (right_pressed) {
            camera.position.x += 10 / camera.zoom;
        }

        //Step time, do things that only happen when not paused
        if (!paused) {
            time++;
        }

    }

    public void draw() {
        GL20 gl = Gdx.gl;
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //Draw background
        game.batcher.begin();
        game.batcher.draw(backgroundmap, 0.0F, 0.0F, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        game.batcher.end();

        //Draw the area polygon
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(1, 0, 0, 0.3f));

        if (polygon != null) {
            for (float[] triangle : polygon) {
                shapeRenderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);
            }
        }

        shapeRenderer.end();

        game.batcher.begin();
        //Draw units
        for (Unit unit : animation.getUnits()) {
            if (unit.getImage().equals("israel")) {
                game.batcher.draw(Assets.flag2, unit.getScreenPosition().getX() - (IMAGE_WIDTH * zoomfactor)/2, unit.getScreenPosition().getY() - (IMAGE_HEIGHT * zoomfactor)/2, IMAGE_WIDTH * zoomfactor, IMAGE_HEIGHT * zoomfactor);
            }
        }
        game.batcher.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        //Draw the debug circles
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (animationMode) {
            //Draw line nodes
            shapeRenderer.setColor(Color.GREEN);
            for (double[] point : keyPoints) {
                shapeRenderer.circle((float) point[0], (float) point[1], 7);
            }
            //Draw area polygon nodes
            shapeRenderer.setColor(Color.BLUE);
            for (Node node : animation.getArea().getNodes()) {
                shapeRenderer.circle(node.getScreenPosition().getX(), node.getScreenPosition().getY(), 7);
            }
        }
        //Draw the front line
        shapeRenderer.setColor(Color.RED);

        float num = 1000.00f; //the number of straight lines that will be used to approximate the spline function
        double[][] interpolated = new double[(int) num][2];
        for (float i = nodes.size()/num; i < (float) nodes.size() - 1.00f; i += (float) nodes.size()/num) {
            shapeRenderer.rectLine(
                    (float) xFunction.value(i - nodes.size()/num),
                    (float) yFunction.value(i - nodes.size()/num),
                    (float) xFunction.value(i),
                    (float) yFunction.value(i),
                    5.0f
            );
        }
        shapeRenderer.end();
        //Draw FPS and time text
        game.batcher.begin();
        bitmapFont.draw(game.batcher, Gdx.graphics.getFramesPerSecond() + " FPS", 30, (float) DISPLAY_HEIGHT - 30);
        bitmapFont.draw(game.batcher, "Time: " + time, 30, DISPLAY_HEIGHT - 70);
        game.batcher.end();
    }

    public boolean keyDown(int keycode) {
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
        if (keycode == Input.Keys.SPACE) {
            paused = !paused;
        }
        if (keycode == Input.Keys.A) {
            var node = new Node(
                    new Coordinate(0, 0),
                    new Coordinate(mousex, mousey)
            );
            node.getMovementFrames().add(
                    new GroupedMovement(
                            new HashMap<>() {{
                                put(time, new Coordinate(mousex, mousey));
                            }}
                    )
            );

            animation.getArea().getNodes().add(node);
        }
        if (keycode == Input.Keys.E) {
            time = (time / 200) * 200 + 200;
        }
        if (keycode == Input.Keys.Q) {
            time = (int) Math.ceil(time / 200.0) * 200 - 200;
        }
        if (keycode == Input.Keys.C) {
            animationMode = !animationMode;
        }
        if (keycode == Input.Keys.NUM_1) {
            var node = new Node(
                    new Coordinate(0, 0),
                    new Coordinate(mousex, mousey)
            );
            node.getMovementFrames().add(
                    new GroupedMovement(
                            new HashMap<>() {{
                                put(time, new Coordinate(mousex, mousey));
                            }}
                    )
            );

            animation.getLines().get(0).getNodes().add(node);
        }
        if (keycode == Input.Keys.NUM_2) {
            var unit = new Unit(
                    "israel",
                    new ArrayList<GroupedMovement>(),
                    null,
                    new Coordinate(0, 0),
                    new Coordinate(mousex, mousey)
            );
            unit.getMovementFrames().add(
                    new GroupedMovement(
                            new HashMap<>() {{
                                put(time, new Coordinate(mousex, mousey));
                            }}
                    )
            );

            animation.getUnits().add(unit);
        }
        return true;
    }

    public boolean keyUp(int keycode) {
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

    public boolean keyTyped(char character) {
        return false;
    }

    public boolean touchDown(int x, int y, int pointer, int button) {
        y = DISPLAY_HEIGHT - y; //for some reason clicked is called with top left (0, 0) instead of bottom left

        mousex = (float) (floor(x) - camera.position.x * (1 - camera.zoom) - (DISPLAY_WIDTH / 2.0f - camera.position.x)) / camera.zoom;
        mousey = (float) (floor(y) - camera.position.y * (1 - camera.zoom) - (DISPLAY_HEIGHT / 2.0f - camera.position.y)) / camera.zoom;

        System.out.println(mousex + " " + mousey);

        List<Node> nodeList = Stream.concat(animation.getArea().getNodes().stream(), animation.getLines().get(0).getNodes().stream()).toList();
        List<Unit> unitList = animation.getUnits();

        if (selected != null) {
            if (button == 0) {
                selected.newSetPoint(time, mousex, mousey);
                selected = null;
                return true;
            }
            if (button == 1) {
                selected.newSetPoint(time, mousex, mousey, true);
                selected = null;
                return true;
            }
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

    public boolean touchUp(int x, int y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    public boolean touchDragged(int x, int y, int pointer) {
        return false;
    }

    public boolean mouseMoved(int x, int y) {
        y = DISPLAY_HEIGHT - y;
        mousex = (float) (floor(x) - camera.position.x * (1 - camera.zoom) - (DISPLAY_WIDTH / 2.0f - camera.position.x)) / camera.zoom;
        mousey = (float) (floor(y) - camera.position.y * (1 - camera.zoom) - (DISPLAY_HEIGHT / 2.0f - camera.position.y)) / camera.zoom;
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom *= 1 - 0.05 * amountY;
        return true;
    }

    @Override
    public void render(float delta) {
        update();
        draw();
    }

    @Override
    public void pause() {
        Settings.save();
        FileHandler.INSTANCE.save();
    }
}
