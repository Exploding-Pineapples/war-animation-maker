package com.badlogicgames.superjumper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogicgames.superjumper.models.*;
import com.badlogicgames.superjumper.models.Animation;
import com.badlogicgames.superjumper.models.Object;
import com.badlogicgames.superjumper.originalgame.Settings;
import kotlin.Pair;

import java.util.*;
import java.util.stream.Stream;

import static com.badlogicgames.superjumper.Assets.loadTexture;
import static java.lang.Math.floor;

public class Screen extends ScreenAdapter implements InputProcessor {
    WarAnimationMaker game;
    OrthographicCamera camera;
    Vector3 touchPoint; //point of last mouse click
    float mousex; //current mouse x position
    float mousey; //current mouse y position
    float zoomfactor; //separate from camera.zoom, changes certain things at a different scale
    Animation animation; //contains all information about animation loaded from file
    Integer time;
    TextureRegion backgroundmap;
    Object selected;
    boolean up_pressed;
    boolean down_pressed;
    boolean left_pressed;
    boolean right_pressed;
    boolean ctrl_pressed;
    boolean paused;
    boolean animationMode;
    List<Node> selectedList;
    String[] countries;
    TouchMode touchMode;
    Texture backgroundImage;
    int lineID;

    public static final int DISPLAY_WIDTH = 1920;
    public static final int DISPLAY_HEIGHT = 1080;
    public static final int IMAGE_WIDTH = 40;
    public static final int IMAGE_HEIGHT = 40;
    public static final int MIN_LINE_SIZE = 2; //minimum number of nodes needed to draw a line
    public static final int LINES_PER_LINE = 1000; //number of straight lines used to represent the splines

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BitmapFont bitmapFont = new BitmapFont();
    FrameBuffer colorlayer;

    public Screen(WarAnimationMaker game) {
        this.game = game;
        animation = FileHandler.INSTANCE.getAnimations().get(0);
        //camera
        camera = new OrthographicCamera(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        camera.position.set(DISPLAY_WIDTH / 2.0f, DISPLAY_HEIGHT / 2.0f, 0);
        //camera.setToOrtho(false, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        animation.camera();
        //not gonna touch
        touchPoint = new Vector3();
        bitmapFont.getData().setScale(2.5f);
        //war animation init
        backgroundImage = loadTexture(animation.getPath());
        time = 0;
        paused = true;
        animationMode = true;
        countries = new String[]{"hamas", "israel"};
        colorlayer = new FrameBuffer(Pixmap.Format.RGBA8888, 1024, 720, false);
        lineID = 0;
        //UI
        touchMode = TouchMode.DEFAULT;

        animation.camera().goToTime(time);
        updateCam();
    }

    public void update() {
        if (Gdx.input.justTouched()) {
            camera.unproject(touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0));
			/*if (soundBounds.contains(touchPoint.x, touchPoint.y)) {
				Assets.playSound(Assets.clickSound);
				Settings.soundEnabled = !Settings.soundEnabled;
			}*/
        }
        //Camera
        camera.update();
        zoomfactor = 0.75f + camera.zoom * 8;
        animation.camera().goToTime(time);
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
        if (!paused) { //don't update camera when paused to allow for movement when paused
            updateCam();
        }

        if (selected != null && paused) { //automatically updates the selected object to go to the mouse for interactive adding
            if ((touchMode == TouchMode.DEFAULT) || (touchMode == TouchMode.NEW_NODE)) {
                selected.newSetPoint(time, mousex, mousey);
                selected.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
            }
        }

        //Update area polygons
        for (Area area : animation.getAreas()) {
            for (Node node : area.getNodes()) {
                node.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
            }

            //Since the area function requires lines and not line IDs, all the line IDs are turned into lines using the instance method of animation.getLineByID and then passed into the area.calculatePolygon() function
            List<Pair<Integer, Integer>> lineIDs = area.getLineIDs();
            List<Pair<Line, Integer>> convertedLineIDs = new ArrayList<>();

            for (Pair<Integer, Integer> line : lineIDs) {
                convertedLineIDs.add(new Pair<>(animation.getLineByID(line.getFirst()), line.getSecond()));
            }

            area.calculatePolygon(convertedLineIDs);
        }

        //Update background screen
        int viewwidth = (int) (DISPLAY_WIDTH / camera.zoom);
        int viewheight = (int) (DISPLAY_HEIGHT / camera.zoom);
        backgroundmap = new TextureRegion(backgroundImage, (int) (camera.position.x - (viewwidth - DISPLAY_WIDTH) / 2.0f), (int) (DISPLAY_HEIGHT - camera.position.y - (viewheight - DISPLAY_WIDTH) / 2.0f), viewwidth, viewheight);

        //update all nodes
        for (Line line : animation.getLines()) {
            for (Node node : line.getNodes()) {
                node.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
            }
        }

        //Update all units
        for (Unit unit : animation.getUnits()) {
            unit.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
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
        game.batcher.begin();
        game.batcher.draw(backgroundmap, 0F, 0F, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        game.batcher.end();


        //Draw the colored areas to buffer
        colorlayer.begin();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.CLEAR);
        shapeRenderer.rect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);

        for (Area area: animation.getAreas()){
            area.draw(shapeRenderer);
        }

        shapeRenderer.end();
        colorlayer.end();

        //Draw the colored areas to the screen
        game.batcher.begin();
        Texture texture = colorlayer.getColorBufferTexture();
        TextureRegion textureRegion = new TextureRegion(texture);
        textureRegion.flip(false, true);
        game.batcher.setColor(1,1,1,0.3f); //default is white 1,1,1,1
        game.batcher.draw(textureRegion, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        game.batcher.setColor(1,1,1, 1);
        game.batcher.end();

        //Draw the front line
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);

        for (Line line : animation.getLines()) {
            if (line.interpolate(LINES_PER_LINE, time)) { //lines.interpolate interpolates the points to draw into its instance variables and returns true only if there were more than 2 points
                for (int i = 0; i < LINES_PER_LINE; i++) {
                    shapeRenderer.rectLine(
                            line.getInterpolatedX()[i],
                            line.getInterpolatedY()[i],
                            line.getInterpolatedX()[i + 1],
                            line.getInterpolatedY()[i + 1],
                            5.0f
                    );
                }
            }
        }
        shapeRenderer.end();

        game.batcher.begin();
        //Draw units
        for (Unit unit : animation.getUnits()) {
            for (int i = 0; i < countries.length; i++) {
                String image = countries[i];
                if (unit.getImage().equals(image)) { //draw only for the correct country
                    game.batcher.setColor(1, 1, 1, 1.0f); //sets no transparency by default
                    game.batcher.draw(Assets.flags[i], unit.getScreenPosition().getX() - (IMAGE_WIDTH * zoomfactor) / 2, unit.getScreenPosition().getY() - (IMAGE_HEIGHT * zoomfactor) / 2, IMAGE_WIDTH * zoomfactor, IMAGE_HEIGHT * zoomfactor);
                }
            }

        }
        game.batcher.setColor(1, 1, 1, 1.0f); //resets to no transparency, if this isn't here the background breaks and I don't know why
        game.batcher.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        //Draw the debug circles
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (animationMode) {
            //Draw line nodes
            shapeRenderer.setColor(Color.GREEN);
            for (Line l : animation.getLines()) {
                for (Node n : l.getNodes()) { //Uses getNodes() instead of getDrawNodes() to show all nodes, even ones before their defined time
                    shapeRenderer.circle(n.getScreenPosition().getX(), n.getScreenPosition().getY(), 7);
                }
            }
            //Draw area polygon nodes
            shapeRenderer.setColor(Color.BLUE);
            for (Area area : animation.getAreas()) {
                for (Node node : area.getNodes()) {
                    shapeRenderer.circle(node.getScreenPosition().getX(), node.getScreenPosition().getY(), 7);
                }
            }
            //Draw the selected node
            if (selected != null) {
                shapeRenderer.setColor(Color.ORANGE);
                shapeRenderer.rect(selected.getScreenPosition().getX() - 6.0f, selected.getScreenPosition().getY() - 6.0f, 12.0f, 12.0f);
                //System.out.println("selected");
            }
        }
        shapeRenderer.end();
        //Draw FPS and time text
        game.batcher.begin();
        bitmapFont.draw(game.batcher, Gdx.graphics.getFramesPerSecond() + " FPS", 30, (float) DISPLAY_HEIGHT - 30);
        bitmapFont.draw(game.batcher, "Time: " + time, 30, DISPLAY_HEIGHT - 70);
        game.batcher.end();

        if (!paused) { //now that both update and draw are done, advance the time
            time++;
        }
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

        if ((keycode >= 7) && (keycode <= 16)) { //keycodes 7-16 inclusive correspond to number keys 0 - 9
            int key_num = keycode - 7;
            if (key_num == 1) { //1 creates frontline nodes
                if (touchMode != TouchMode.NEW_NODE) {
                    touchMode = TouchMode.NEW_NODE;
                    System.out.println("New Node Mode");
                } else {
                    touchMode = TouchMode.DEFAULT;
                    selected = null;
                    System.out.println("Default Mode");
                }
            }
            if ((key_num > 1) && (key_num - 2 < countries.length)) { //2+ create units of different countries
                var unit = new Unit(
                        countries[key_num - 2],
                        new ArrayList<>(),
                        null,
                        new Coordinate(0, 0),
                        new Coordinate(mousex, mousey),
                        0.0f
                );
                unit.getMovementFrames().add(
                        new GroupedMovement<>(
                                new HashMap<>() {{
                                    put(time, new Coordinate(mousex, mousey));
                                }}
                        )
                );
                System.out.println(unit);

                animation.getUnits().add(unit);
            }
        }

        boolean actioned = false;
        if (selected != null) {
            if (keycode == Input.Keys.C) {
                selected = null;
                System.out.println("Deselected object");
            }
            if (keycode == Input.Keys.FORWARD_DEL) {
                animation.deleteObject(selected);
                System.out.println("Deleted object");
                actioned = true;
            }
            if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.DEL) {
                if (selected.removeFrame(time)) {
                    System.out.println("Deleted last frame");
                } else {
                    System.out.println("Cannot delete last frame on object with less than 2 frames");
                }
                selected = null;
                touchMode = TouchMode.DEFAULT;
                actioned = true;
            }
        }

        if (ctrl_pressed) {
            if (keycode == Input.Keys.C) {
                animation.camera().newSetPoint(time, camera.position.x, camera.position.y, camera.zoom);
            }
            if (keycode == Input.Keys.S) {
                Settings.save();
                FileHandler.INSTANCE.save();
                System.out.println("saved");
            }
            if (selected != null) {
                if (keycode == Input.Keys.D) {
                    selected.setDeath(time);
                    System.out.println("Death of " + selected + " set to " + selected.getDeath());
                    actioned = true;
                }
            }
        } else {
            if (keycode == Input.Keys.A) { //create a new area
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
                Area a = new Area(new ArrayList<>(), AreaColor.RED, new ArrayList<>(), new ArrayList<>());
                //a.getLineIDs().add(new Pair<>(0, 0));

                animation.getAreas().add(a);

                selectedList = a.getNodes();
                selected = node;
                selectedList.add((Node) selected);
            }
            if (keycode == Input.Keys.S) {
                touchMode = TouchMode.SET_AREA_LINE;
                System.out.println("Set Area Line");
            }
            if (keycode == Input.Keys.E) {
                time = (time / 200) * 200 + 200;
                animation.camera().goToTime(time);
                updateCam();
            }
            if (keycode == Input.Keys.PERIOD) {
                time++;
                animation.camera().goToTime(time);
                updateCam();
                System.out.println(animation.getLines().get(0).getDrawNodes(time).size());
            }
            if (keycode == Input.Keys.COMMA) {
                time--;
                animation.camera().goToTime(time);
                updateCam();
                System.out.println(animation.getLines().get(0).getDrawNodes(time).size());
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
                lineID++;
                animation.getLines().add(new Line(lineID, new ArrayList<>(), new Float[0], new Float[0]));
                selectedList = Objects.requireNonNull(animation.getLineByID(lineID)).getNodes();
                selectedList.add(node);
                selected = node;
            }
        }

        if (actioned) {
            selected = null;
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

        System.out.println("Clicked " + mousex + " " + mousey + " touch mode " + touchMode);

        if (paused) {
            if (touchMode == TouchMode.DEFAULT) { //default behavior: selects an object to move when paused
                Stream<Node> nodeStream = Stream.of();
                for (Area area : animation.getAreas()) {
                    nodeStream = Stream.concat(nodeStream, area.getNodes().stream());
                }
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
                        System.out.println("Node at " + node.getPosition() + " on line " + animation.getListOfObject(node) + " was clicked");
                        selected = node;
                        selectedList = animation.getListOfNode(node);
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
            }

            if (touchMode == TouchMode.NEW_NODE) { //will add a new Node to the List<Node> selectedList, this includes frontlines and area borders
                int addAtIndex;
                //Conditional is really for selecting addAtIndex, which allows inserting at any point along a line based on what was previously selected
                if (selected == null || selected.getClass() == Unit.class) { //if there was no selected node or the selected object was a Unit, there is no adding on. By default, it goes to the first frontline
                    if (animation.getLines().size() == 0) { //if there is no frontline, make one
                        animation.getLines().add(new Line(lineID, new ArrayList<>(), new Float[0], new Float[0]));
                        lineID++;
                        selectedList = animation.getLines().get(0).getNodes();
                        addAtIndex = 0;
                    } else { //if there is a frontline (and nothign was selected), add to the last node
                        selectedList = animation.getLines().get(0).getNodes();
                        selected = selectedList.get(selectedList.size() - 1);
                        addAtIndex = selectedList.size();
                    }
                } else { //if something was selected, add the new node to its list
                    addAtIndex = selectedList.indexOf(selected) + 1;
                }

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
                selected = node;
                selectedList.add(addAtIndex, node);
            }

            if (touchMode == TouchMode.SET_AREA_LINE) {
                System.out.println("Checking for clicked line");
                if (selected != null) {
                    for (Line l : animation.getLines()) {
                        for (Object node : l.getNodes()) {
                            if (node.clicked(x, y)) {
                                Area selectedArea = animation.getAreaOfNode(selected);
                                selectedArea.getLineIDs().add(new Pair<>(l.getId(), selectedArea.getNodes().indexOf(selected)));
                                touchMode = TouchMode.DEFAULT;
                                selected = null;
                                System.out.println("Area set to line");
                                return true;
                            }
                        }
                    }
                }
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