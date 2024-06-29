package com.badlogicgames.superjumper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogicgames.superjumper.models.*;
import com.badlogicgames.superjumper.models.Animation;
import com.badlogicgames.superjumper.models.Object;
import com.badlogicgames.superjumper.originalgame.Settings;
import kotlin.Pair;

import java.util.*;
import java.util.stream.Stream;

import static com.badlogicgames.superjumper.Assets.loadTexture;
import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_WIDTH;
import static java.lang.Character.isLetterOrDigit;

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
    boolean shift_pressed;
    boolean paused;
    boolean animationMode;
    List<Node> selectedList;
    String[] countries;
    TouchMode touchMode;
    InputMode inputMode;
    Texture backgroundImage;
    String input;
    List<Action> actions;

    public static final int DEFAULT_UNIT_WIDTH = 10;
    public static final int DEFAULT_UNIT_HEIGHT = 10;
    public static final int MIN_LINE_SIZE = 2; //minimum number of nodes needed to draw a line
    public static final int LINES_PER_LINE = 1000; //number of straight lines used to represent the splines

    private final BitmapFont bitmapFont;
    ShapeRenderer shapeRenderer;
    GL20 gl;
    FrameBuffer colorlayer;

    public Screen(WarAnimationMaker game) {
        this.game = game;
        gl = game.gl;
        animation = FileHandler.INSTANCE.getAnimations().get(0);
        //camera
        camera = new OrthographicCamera(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        camera.position.set(DISPLAY_WIDTH / 2.0f, DISPLAY_HEIGHT / 2.0f, 0);
        animation.camera();
        //not gonna touch
        touchPoint = new Vector3();
        //war animation init
        shapeRenderer = game.shapeRenderer;
        bitmapFont = game.bitmapFont;
        bitmapFont.getData().setScale(2.5f);
        backgroundImage = loadTexture(animation.getPath());
        time = 0;
        paused = true;
        animationMode = true;
        countries = new String[]{"assets/flags/naziflag.png", "assets/flags/frflag.png", "assets/flags/bgflag.png", "assets/flags/luxflag.png"};
        colorlayer = new FrameBuffer(Pixmap.Format.RGBA8888, 1024, 720, false);
        for (Line line : animation.getLines()) {
            line.interpolate(LINES_PER_LINE, time);
        }
        //UI
        touchMode = TouchMode.DEFAULT;
        inputMode = InputMode.NONE;
        buildActions();
        actions = ActionKt.getActions();
        input = "";

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
            if (inputMode == InputMode.NONE) {
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

            area.calculatePolygon(convertedLineIDs, time);
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
            unit.draw(game.batcher, zoomfactor, bitmapFont);
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
        if (animationMode) {
            game.batcher.begin();
            bitmapFont.getData().setScale(2.5f);
            bitmapFont.setColor(Color.WHITE);
            bitmapFont.draw(game.batcher, Gdx.graphics.getFramesPerSecond() + " FPS", 30, (float) DISPLAY_HEIGHT - 30);
            bitmapFont.draw(game.batcher, "Time: " + time, 30, DISPLAY_HEIGHT - 70);

            //Draw current input
            bitmapFont.getData().setScale(10.0f);
            bitmapFont.draw(game.batcher, input, (float) DISPLAY_WIDTH / 2, (float) DISPLAY_HEIGHT / 2);

            //Draw keyboard options
            bitmapFont.getData().setScale(1.0f);
            StringBuilder options = new StringBuilder();
            for (Action action : actions) {
                if (action.couldExecute(false, ctrl_pressed, selected, inputMode)) {
                    for (int key : action.getActionKeys()) {
                        options.append(Input.Keys.toString(key)).append(" ");
                    }
                    options.append(" ").append(action.getActionName()).append("\n");
                }
            }
            bitmapFont.draw(game.batcher, options, 30, DISPLAY_HEIGHT - 110);

            game.batcher.end();
        }

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
        if (keycode == Input.Keys.SHIFT_LEFT) {
            shift_pressed = true;
        }

        for (Action action : ActionKt.getActions()) {
            if (action.shouldExecute(keycode, false, ctrl_pressed, selected, inputMode)) {
                action.execute();
                break;
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
        if (keycode == Input.Keys.SHIFT_LEFT) {
            shift_pressed = false;
        }
        return true;
    }

    public boolean keyTyped(char character) {
        if (inputMode != InputMode.NONE) {
            if (isLetterOrDigit(character)) {
                input += character;
            }
        }
        return true;
    }

    public boolean touchDown(int x, int y, int pointer, int button) {
        updateMouse(x, y);
        y = DISPLAY_HEIGHT - y;

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
                    if (animation.getLines().isEmpty()) { //if there is no frontline, make one
                        animation.getLines().add(new Line(animation.getLineID(), new ArrayList<>(), new Float[0], new Float[0]));
                        animation.setLineID(animation.getLineID() + 1);
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
        updateMouse(x, y);
        return true;
    }

    public void updateMouse(int x, int y) {
        y = DISPLAY_HEIGHT - y;
        mousex = (float) ((double) x - camera.position.x * (1 - camera.zoom) - (DISPLAY_WIDTH / 2.0f - camera.position.x)) / camera.zoom;
        mousey = (float) ((double) y - camera.position.y * (1 - camera.zoom) - (DISPLAY_HEIGHT / 2.0f - camera.position.y)) / camera.zoom;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom *= (float) (1 - 0.05 * amountY);
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

    public void buildActions() {
        //Actions available when game is not inputting and nothing is selected
        Action.createBuilder(() -> {
            paused = !paused;
            return null;
        }, List.of(Input.Keys.SPACE), "Pause/Unpause the game").build();
        Action.createBuilder(() -> {
            selected = null;
            System.out.println("Deselected object");
            return null;
        }, List.of(Input.Keys.U), "Deselect Object").description("Deselects object").requiresSelected(true).build();
        Action.createBuilder(() -> {
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
            return null;
        }, List.of(Input.Keys.A), "Create new area").requiresSelected(true).build();
        Action.createBuilder(() -> {
            touchMode = TouchMode.SET_AREA_LINE;
            System.out.println("Set Area Line");
            return null;
        }, List.of(Input.Keys.S), "Set Area Line"
        ).requiresSelected(true).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build();
        Action.createBuilder(() -> {
            time = (time / 200) * 200 + 200;
            animation.camera().goToTime(time);
            updateCam();
            return null;
        }, List.of(Input.Keys.E), "Step time forward 200").build();
        Action.createBuilder(() -> {
            time++;
            animation.camera().goToTime(time);
            updateCam();
            System.out.println(animation.getLines().get(0).getDrawNodes(time).size());
            return null;
        }, List.of(Input.Keys.PERIOD), "Step time forward 1").build();
        Action.createBuilder(() -> {
            time--;
            animation.camera().goToTime(time);
            updateCam();
            System.out.println(animation.getLines().get(0).getDrawNodes(time).size());
            return null;
        }, List.of(Input.Keys.COMMA), "Step time back 1").build();
        Action.createBuilder(() -> {
            animationMode = !animationMode;
            return null;
        }, List.of(Input.Keys.V), "Toggle animation mode").build();
        Action.createBuilder(() -> {
            time = (int) Math.ceil(time / 200.0) * 200 - 200;
            animation.camera().goToTime(time);
            updateCam();
            return null;
        }, List.of(Input.Keys.Q), "Step time back 200").build();
        Action.createBuilder(() -> {
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
            animation.setLineID(animation.getLineID() + 1);
            animation.getLines().add(new Line(animation.getLineID(), new ArrayList<>(), new Float[0], new Float[0]));
            selectedList = Objects.requireNonNull(animation.getLineByID(animation.getLineID())).getNodes();
            selectedList.add(node);
            selected = node;
            return null;
        }, List.of(Input.Keys.L), "Create new line").build();
        Action.createBuilder(() -> {
            if (touchMode != TouchMode.NEW_NODE) {
                touchMode = TouchMode.NEW_NODE;
                System.out.println("New Node Mode");
            } else {
                touchMode = TouchMode.DEFAULT;
                selected = null;
                System.out.println("Default Mode");
            }
            return null;
        }, List.of(Input.Keys.NUM_1), "New Node Mode").build();
        for (int num_key = Input.Keys.NUM_2; num_key < Input.Keys.NUM_9; num_key++) {
            int finalNum_key = num_key;
            Action.createBuilder(() -> {
                var unit = new Unit(
                        countries[finalNum_key - 9], //number key enum number to list number
                        null,
                        "infantry",
                        "XX",
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
                selected = unit;
                return null;
                }, List.of(num_key), "Create Unit of country " + (finalNum_key - 9)
            ).build();
        }
        //Key presses which require control pressed
        Action.createBuilder(() -> {
            animation.camera().newSetPoint(time, camera.position.x, camera.position.y, camera.zoom);
            return null;
        }, List.of(Input.Keys.C), "Set camera set point").requiresControl(true).build();
        Action.createBuilder(() -> {
            Settings.save();
            FileHandler.INSTANCE.save();
            System.out.println("saved");
            return null;
        }, List.of(Input.Keys.S), "Save project").requiresControl(true).build();
        //Key presses which require control pressed and selected object
        Action.createBuilder(() -> {
            selected.setDeath(time);
            System.out.println("Death of " + selected + " set to " + selected.getDeath());
            selected = null;
            return null;
        }, List.of(Input.Keys.D), "Set death of an object").requiresControl(true).requiresSelected(true).build();
        // Key presses which require selected Object
        Action.createBuilder(() -> {
            animation.deleteObject(selected);
            System.out.println("Deleted object");
            selected = null;
            return null;
        }, List.of(Input.Keys.FORWARD_DEL), "Delete selected object").requiresSelected(true).build();
        Action.createBuilder(() -> {
            if (selected.removeFrame(time)) {
                System.out.println("Deleted last frame");
            } else {
                System.out.println("Cannot delete last frame on object with less than 2 frames");
            }
            selected = null;
            touchMode = TouchMode.DEFAULT;
            return null;
        }, List.of(Input.Keys.ESCAPE, Input.Keys.DEL), "Delete last frame of selected object").requiresSelected(true).build();
        // Key presses which require selected Unit
        Action.createBuilder(() -> {
            inputMode = InputMode.NAME_INPUT;
            return null;
        }, List.of(Input.Keys.N), "Input name for selected unit").requiresSelected(true).clearRequiredSelectedTypes().requiredSelectedTypes(Unit.class).build();
        Action.createBuilder(() -> {
            inputMode = InputMode.TYPE_INPUT;
            return null;
        }, List.of(Input.Keys.T), "Input type for selected unit").requiresSelected(true).clearRequiredSelectedTypes().requiredSelectedTypes(Unit.class).build();
        Action.createBuilder(() -> {
            inputMode = InputMode.SIZE_INPUT;
            return null;
        }, List.of(Input.Keys.S), "Input size for selected unit").requiresSelected(true).clearRequiredSelectedTypes().requiredSelectedTypes(Unit.class).build();
        // Key presses which require game to be in input mode
        Action.createBuilder(() -> {
            Unit unit = (Unit) selected;
            if (inputMode == InputMode.NAME_INPUT) {
                unit.setName(input);
                System.out.println("Name set to: " + unit.getName());
            }
            if (inputMode == InputMode.TYPE_INPUT) {
                unit.setType(input);
            }
            if (inputMode == InputMode.SIZE_INPUT) {
                unit.setSize(input);
            }
            inputMode = InputMode.NONE;
            input = "";
            selected = null;
            return null;
        }, List.of(Input.Keys.ENTER), "Submit Input").requiredInputModes().requiresSelected(true).excludedInputModes(InputMode.NONE).build();
        Action.createBuilder(() -> {
            if (!input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
            }
            return null;
        }, List.of(Input.Keys.BACKSPACE), "Delete last character of input").requiredInputModes().requiresSelected(true).excludedInputModes(InputMode.NONE).build();
    }
}