package com.badlogicgames.superjumper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogicgames.superjumper.models.*;
import com.badlogicgames.superjumper.models.Animation;
import com.badlogicgames.superjumper.models.Object;
import earcut4j.Earcut;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import java.time.LocalTime;

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
    TextureRegion battlefield;
    List<List<Node>> nodes;
    List<Unit> units;
    Object selected;
    List<double[]> keyPoints;
    List<PolynomialSplineFunction> xFunctions;
    List<PolynomialSplineFunction> yFunctions;
    LocalTime curtime;
    boolean up_pressed;
    boolean down_pressed;
    boolean left_pressed;
    boolean right_pressed;
    boolean ctrl_pressed;
    boolean paused;
    boolean animationMode;
    float mousex;
    float mousey;
    float zoomfactor;
    int selectedLine = 0;
    List<List<float[]>> insidePolys;
    public static final int DISPLAY_WIDTH = 1920;
    public static final int DISPLAY_HEIGHT = 1080;
    public static final int IMAGE_WIDTH = 40;
    public static final int IMAGE_HEIGHT = 40;
    public static final int MIN_LINE_SIZE = 2;

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BitmapFont bitmapFont = new BitmapFont();
    FrameBuffer colorlayer;

    public Screen(WarAnimationMaker game) {
        this.game = game;
        animation = FileHandler.INSTANCE.getAnimations().get(0);

        camera = new OrthographicCamera(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        camera.position.set(DISPLAY_WIDTH / 2.0f, DISPLAY_HEIGHT / 2.0f, 0);
        camera.setToOrtho(false, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        animation.camera();

        soundBounds = new Rectangle(0, 0, 64, 64);
        playBounds = new Rectangle(160 - 150, 200 + 18, 300, 36);
        highscoresBounds = new Rectangle(160 - 150, 200 - 18, 300, 36);
        helpBounds = new Rectangle(160 - 150, 200 - 18 - 36, 300, 36);
        touchPoint = new Vector3();
        bitmapFont.getData().setScale(2.5f);

        time = 0;
        paused = true;
        animationMode = true;
        keyPoints = new ArrayList<>();
        xFunctions = new ArrayList<>();
        yFunctions = new ArrayList<>();
        nodes = new ArrayList<>();

        curtime = LocalTime.now();
        colorlayer = new FrameBuffer(Pixmap.Format.RGBA8888, 1024, 720, false);

        animation.camera().goToTime(time);
        updateCam();
    }
    public List<List<float[]>> calculatePolygons(List<Line> lines) {
        List<List<float[]>> insideDrawPolyList = new ArrayList<>();

        float num = 1000.00f;
        double[][] backwardsNodePositionsList = new double[lines.size()][ (int) num * 2]; //backwards version needed to draw interior clockwise polygon
        double[][] nodePositionsList = new double[lines.size()][ (int) num * 2]; //stores the positions of all the interpolated points along the red line (not the green set points), this is used to draw polygon

        int i = 0;
        for (Line l : lines) {
            List<Node> line = l.getNodes();
            if (line.size() > MIN_LINE_SIZE) {
                double[] backwardsNodePositions = backwardsNodePositionsList[i];
                double[] nodePositions = nodePositionsList[i];

                int k = (int) num * 2; //backwards list iterator
                int m = 0; //forwards list iterator
                for (float j = 0; j < (float) line.size() - 1.00f; j += (float) line.size() / num) { //loop evaluates interpolated spline function at 1000 points to draw border of polygon
                    k -= 2;
                    nodePositions[m] = xFunctions.get(i).value(j);
                    nodePositions[m + 1] = yFunctions.get(i).value(j);
                    backwardsNodePositions[k] = xFunctions.get(i).value(j);
                    backwardsNodePositions[k + 1] = yFunctions.get(i).value(j);
                    m += 2;
                }

                backwardsNodePositionsList[i] = Arrays.copyOfRange(backwardsNodePositions, k, backwardsNodePositions.length); //for some reason, interpolation doesn't reach num and I can't figure out why. Sublist removes extra default 0.0 elements that aren't set
                nodePositionsList[i] = Arrays.copyOfRange(nodePositions, 0, m);
                i++;
            }
        }

        for (int j = 0; j < lines.size(); j++) {

            double[] insidePoly = new double[nodePositionsList[j].length];

            int l = 0;
            for (double position : nodePositionsList[j]) {
                insidePoly[l] = position;
                l++;
            }

            List<Integer> insideEarcut = Earcut.earcut(insidePoly); //turns polygon into series of triangles represented by polygon vertex indexes
            List<float[]> insideDrawPoly = new ArrayList<>();

            float v1x, v1y, v2x, v2y, v3x, v3y;
            for (i = 0; i < insideEarcut.size(); i += 3) {
                v1x = (float) insidePoly[insideEarcut.get(i) * 2];
                v1y = (float) insidePoly[insideEarcut.get(i) * 2 + 1];
                v2x = (float) insidePoly[insideEarcut.get(i + 1) * 2];
                v2y = (float) insidePoly[insideEarcut.get(i + 1) * 2 + 1];
                v3x = (float) insidePoly[insideEarcut.get(i + 2) * 2];
                v3y = (float) insidePoly[insideEarcut.get(i + 2) * 2 + 1];
                insideDrawPoly.add(new float[]{v1x, v1y, v2x, v2y, v3x, v3y}); //the polygon instance variable is then drawn by triangles in the draw function
            }

            insideDrawPolyList.add(insideDrawPoly);
        }
        return insideDrawPolyList;
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

        units = animation.getUnits();
        keyPoints.clear();
        xFunctions.clear();
        yFunctions.clear();

        //Update all nodes and interpolate line
        for (Node node : animation.getArea().getNodes()) {
            node.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
        }
        for (int lineNum = 0; lineNum < animation.getLines().size(); lineNum++) {
            List<Node> nodes = animation.getLines().get(lineNum).getNodes();

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

            for (int i = 0; i < xValues.length; i++) {
                keyPoints.add(new double[]{xValues[i], yValues[i]});
            }

            // Create a spline interpolator
            if (xValues.length > MIN_LINE_SIZE) {
                SplineInterpolator interpolator = new SplineInterpolator();
                xFunctions.add(interpolator.interpolate(evalAt, xValues));
                yFunctions.add(interpolator.interpolate(evalAt, yValues));
            }
        }

        //Update shape polygons
        insidePolys = calculatePolygons(animation.getLines());

        //Update background screen
        int viewwidth = (int) (DISPLAY_WIDTH / camera.zoom);
        int viewheight = (int) (DISPLAY_HEIGHT / camera.zoom);

        zoomfactor = 0.75f + camera.zoom / 8;

        backgroundmap = new TextureRegion(Assets.background, (int) (camera.position.x - (viewwidth - DISPLAY_WIDTH) / 2.0f), (int) (DISPLAY_HEIGHT - camera.position.y - (viewheight - DISPLAY_WIDTH) / 2.0f), viewwidth, viewheight);
        battlefield = new TextureRegion(Assets.battlefield, (int) (camera.position.x - (viewwidth - DISPLAY_WIDTH) / 2.0f), (int) (DISPLAY_HEIGHT - camera.position.y - (viewheight - DISPLAY_WIDTH) / 2.0f), viewwidth, viewheight);

        //Update all units
        for (Unit unit : units) {
            unit.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
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

        animation.camera().goToTime(time);

        //Step time, do things that only happen when not paused
        if (!paused) {
            updateCam();
            time++;
        }

    }

    public void updateCam() {
        camera.position.x = animation.camera().getPosition().getX();
        camera.position.y = animation.camera().getPosition().getY();
        camera.zoom = animation.camera().getZoom();
    }
    public void draw() {
        GL20 gl = Gdx.gl;
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //Draw background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.batcher.begin();
        game.batcher.draw(battlefield, 0.0F, 0.0F, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        game.batcher.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        //Draw the area polygons
        colorlayer.begin();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0, 0, 1, 1.0f));
        shapeRenderer.rect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);

        shapeRenderer.setColor(new Color(1, 0, 0, 1.0f));
        for (List<float[]> poly: insidePolys){
            if (poly != null) {
                for (float[] triangle : poly) {
                    shapeRenderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);
                }
            }
        }

        shapeRenderer.end();
        colorlayer.end();

        game.batcher.begin();
        Texture texture = colorlayer.getColorBufferTexture();
        TextureRegion textureRegion = new TextureRegion(texture);
        textureRegion.flip(false, true);
        game.batcher.setColor(1,1,1,0.3f); //default is white 1,1,1,1
        game.batcher.draw(textureRegion, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        game.batcher.setColor(1,1,1, 1);
        //Draw units
        for (Unit unit : animation.getUnits()) {
            if (unit.getImage().equals("israel")) {
                game.batcher.draw(Assets.flag2, unit.getScreenPosition().getX() - (IMAGE_WIDTH * zoomfactor)/2, unit.getScreenPosition().getY() - (IMAGE_HEIGHT * zoomfactor)/2, IMAGE_WIDTH * zoomfactor, IMAGE_HEIGHT * zoomfactor);
            }
        }
        game.batcher.end();

        //Draw the front line
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);

        for (int lineNum = 0; lineNum < animation.getLines().size(); lineNum++) {
            float num = 1000.00f; //the number of straight lines that will be used to approximate the spline function
            List<Node> nodes = animation.getLines().get(lineNum).getNodes();
            if (nodes.size() > MIN_LINE_SIZE) {
                for (float i = nodes.size() / num; i < (float) nodes.size() - 1.00f; i += (float) nodes.size() / num) {
                    shapeRenderer.rectLine(
                            (float) xFunctions.get(lineNum).value(i - nodes.size() / num),
                            (float) yFunctions.get(lineNum).value(i - nodes.size() / num),
                            (float) xFunctions.get(lineNum).value(i),
                            (float) yFunctions.get(lineNum).value(i),
                            5.0f
                    );
                }
            }
        }
        shapeRenderer.end();

        //Draw the background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.batcher.begin();
        game.batcher.draw(backgroundmap, 0.0F, 0.0F, DISPLAY_WIDTH, DISPLAY_HEIGHT);
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
        if ((keycode == Input.Keys.CONTROL_LEFT) || (keycode == Input.Keys.CONTROL_RIGHT)) {
            ctrl_pressed = true;
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
                    new GroupedMovement<>(
                            new HashMap<>() {{
                                put(time, new Coordinate(mousex, mousey));
                            }}
                    )
            );

            animation.getArea().getNodes().add(node);
        }
        if (keycode == Input.Keys.E) {
            time = (time / 200) * 200 + 200;
            animation.camera().goToTime(time);
            updateCam();
        }
        if (keycode == Input.Keys.V) {
            animationMode = !animationMode;
        }
        if (keycode == Input.Keys.Q) {
            time = (int) Math.ceil(time / 200.0) * 200 - 200;
            animation.camera().goToTime(time);
            updateCam();
        }
        if (keycode == Input.Keys.L) {
            var node = new Node(
                    new Coordinate(0, 0),
                    new Coordinate(mousex, mousey)
            );
            node.getMovementFrames().add(
                    new GroupedMovement<>(
                            new HashMap<>() {{
                                put(time, new Coordinate(mousex, mousey));
                            }}
                    )
            );
            animation.getLines().add(new Line());
            selectedLine = animation.getLines().size() - 1;
            animation.getLines().get(selectedLine).getNodes().add(node);
        }
        if (keycode == Input.Keys.NUM_1) {
            var node = new Node(
                    new Coordinate(0, 0),
                    new Coordinate(mousex, mousey)
            );
            node.getMovementFrames().add(
                    new GroupedMovement<>(
                            new HashMap<>() {{
                                put(time, new Coordinate(mousex, mousey));
                            }}
                    )
            );

            animation.getLines().get(selectedLine).getNodes().add(node);
        }
        if (keycode == Input.Keys.NUM_2) {
            var unit = new Unit(
                    "israel",
                    new ArrayList<>(),
                    null,
                    new Coordinate(0, 0),
                    new Coordinate(mousex, mousey)
            );
            unit.getMovementFrames().add(
                    new GroupedMovement<>(
                            new HashMap<>() {{
                                put(time, new Coordinate(mousex, mousey));
                            }}
                    )
            );

            animation.getUnits().add(unit);
        }
        if (ctrl_pressed) {
            if (keycode == Input.Keys.C) {
                animation.camera().newSetPoint(time, camera.position.x, camera.position.y, camera.zoom);
            }
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
        if ((keycode == Input.Keys.CONTROL_LEFT) || (keycode == Input.Keys.CONTROL_RIGHT)) {
            ctrl_pressed = false;
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


        Stream<Node> nodeStream = animation.getArea().getNodes().stream();
        for (Line line : animation.getLines()) {
            nodeStream = Stream.concat(nodeStream, line.getNodes().stream());
        }
        List<Node> nodeList = nodeStream.toList();
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

        for (Object node : nodeList) {
            if (node.clicked(x, y)) {
                System.out.println(node.getPosition() + " was clicked");
                selected = node;
                selectedLine = animation.getLineOfNode(node);
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
