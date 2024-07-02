package com.badlogicgames.superjumper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogicgames.superjumper.models.*;
import com.badlogicgames.superjumper.models.Animation;
import com.badlogicgames.superjumper.models.Object;
import kotlin.Pair;

import java.util.*;

import static com.badlogicgames.superjumper.Assets.loadTexture;
import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_WIDTH;

public class AnimationScreen extends ScreenAdapter implements InputProcessor {
    public static final int DEFAULT_UNIT_WIDTH = 10;
    public static final int DEFAULT_UNIT_HEIGHT = 10;
    public static final int MIN_LINE_SIZE = 2; //minimum number of nodes needed to draw a line
    public static final int LINES_PER_LINE = 1000; //number of straight lines used to represent the splines

    WarAnimationMaker game;
    OrthographicCamera camera;

    float mouseX; //current mouse x position
    float mouseY; //current mouse y position
    float zoomFactor; //separate from camera.zoom, changes certain things at a different scale
    Animation animation; //contains all information about animation loaded from file
    Integer time;
    TextureRegion backgroundMap;

    Object selected;
    NodeCollection selectedNodeCollection; // if a Node is selected, this will be the Area or Line that the Node is on

    boolean up_pressed;
    boolean down_pressed;
    boolean left_pressed;
    boolean right_pressed;
    boolean shift_pressed;
    boolean ctrl_pressed;
    boolean paused;
    boolean animationMode;
    boolean UIDisplayed; //current state of UI
    boolean justTyped;
    int lastPressType;
    TouchMode touchMode;
    Texture backgroundImage;
    String input;
    List<Action> actions;
    List<InputElement> inputElements;

    private final BitmapFont bitmapFont;
    ShapeRenderer shapeRenderer;
    GL20 gl;
    FrameBuffer colorLayer;
    Stage stage;
    Table selectedInfoTable;
    Table leftPanel;
    Label timeAndFPS;
    Label keyOptions;
    Label selectedLabel;

    public AnimationScreen(WarAnimationMaker game, Animation animation) {
        this.animation = animation;

        this.game = game;
        gl = game.gl;

        //camera
        camera = new OrthographicCamera(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        camera.position.set(DISPLAY_WIDTH / 2.0f, DISPLAY_HEIGHT / 2.0f, 0);
        animation.camera();
        //war animation init
        shapeRenderer = game.shapeRenderer;
        bitmapFont = game.bitmapFont;
        bitmapFont.getData().setScale(2.5f);
        backgroundImage = loadTexture(animation.getPath());
        time = 0;
        paused = true;
        animationMode = true;
        colorLayer = new FrameBuffer(Pixmap.Format.RGBA8888, 1024, 720, false);
        for (Line line : animation.getLines()) {
            line.interpolate(LINES_PER_LINE, time);
        }
        //UI
        touchMode = TouchMode.DEFAULT;
        KeyPressActionKt.getActions().clear();
        buildActions();
        actions = KeyPressActionKt.getActions();
        input = "";
        UIDisplayed = false;
        justTyped = false;
        lastPressType = 0;
        stage = new Stage();
        inputElements = new ArrayList<>();

        timeAndFPS = new Label("", game.skin);
        keyOptions = new Label("", game.skin);
        selectedLabel = new Label("", game.skin);

        selectedInfoTable = new Table();
        selectedInfoTable.right().top();
        selectedInfoTable.setPosition(DISPLAY_WIDTH - 30, DISPLAY_HEIGHT  - 30, 1);
        buildInputs();
        stage.addActor(selectedInfoTable);

        leftPanel = new Table();
        leftPanel.left().top();
        leftPanel.setPosition(30, DISPLAY_HEIGHT - 30);
        stage.addActor(leftPanel);

        game.multiplexer.clear();
        game.multiplexer.addProcessor(stage);
        game.multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(game.multiplexer);

        // Final init
        animation.camera().goToTime(time);
        updateCam();
    }

    public void update() {
        ctrl_pressed = (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT));
        shift_pressed = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));
        //Assets.playSound(Assets.clickSound);
        //Camera
        camera.update();
        zoomFactor = 0.75f + camera.zoom * 8;
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
            if ((touchMode == TouchMode.MOVE)) {
                selected.newSetPoint(time, mouseX, mouseY);
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
        int viewWidth = (int) (DISPLAY_WIDTH / camera.zoom);
        int viewHeight = (int) (DISPLAY_HEIGHT / camera.zoom);
        backgroundMap = new TextureRegion(backgroundImage, (int) (camera.position.x - (viewWidth - DISPLAY_WIDTH) / 2.0f), (int) (DISPLAY_HEIGHT - camera.position.y - (viewHeight - DISPLAY_WIDTH) / 2.0f), viewWidth, viewHeight);

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
        game.batcher.draw(backgroundMap, 0F, 0F, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        game.batcher.end();


        //Draw the colored areas to buffer
        colorLayer.begin();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.CLEAR);
        shapeRenderer.rect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);

        for (Area area: animation.getAreas()){
            area.draw(shapeRenderer);
        }

        shapeRenderer.end();
        colorLayer.end();

        //Draw the colored areas to the screen
        game.batcher.begin();
        Texture texture = colorLayer.getColorBufferTexture();
        TextureRegion textureRegion = new TextureRegion(texture);
        textureRegion.flip(false, true);
        game.batcher.setColor(1,1,1,0.2f); //default is white 1,1,1,1
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
            unit.draw(game.batcher, zoomFactor, bitmapFont);
        }
        game.batcher.setColor(1, 1, 1, 1.0f); //resets to no transparency, if this isn't here the background breaks and I don't know why
        game.batcher.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        //Draw the debug circles
        if (animationMode) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            //Draw line nodes
            for (Line line : animation.getLines()) {
                shapeRenderer.setColor(Color.YELLOW);
                for (Node node : line.getNonDrawNodes(time)) { //Uses getNodes() to get non draw nodes (dead or before start time)
                    shapeRenderer.circle(node.getScreenPosition().getX(), node.getScreenPosition().getY(), 7);
                }
                shapeRenderer.setColor(Color.GREEN);
                for (Node node : line.getDrawNodes(time)) { // getDrawNodes() to show the defined ones as green
                    shapeRenderer.circle(node.getScreenPosition().getX(), node.getScreenPosition().getY(), 7);
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
            shapeRenderer.end();
            //Draw the UI
            timeAndFPS.setText(Gdx.graphics.getFramesPerSecond() + " FPS \n" + "Time: " + time);

            //Set keyboard options and current animator state
            StringBuilder options = new StringBuilder();
            options.append("Touch mode: ").append(touchMode.name()).append("\n");
            options.append("Control pressed: ").append(ctrl_pressed).append(" ").append("Shift pressed: ").append(shift_pressed).append("\n");
            for (Action action : actions) {
                if (action.couldExecute(shift_pressed, ctrl_pressed, selected, touchMode)) {
                    for (int key : action.getActionKeys()) {
                        options.append(Input.Keys.toString(key)).append(" ");
                    }
                    options.append(" ").append(action.getActionName()).append("\n");
                }
            }
            keyOptions.setText(options);

            //Add information about selected object
            if (selected == null) {
                selectedLabel.setText("Nothing is selected");
            } else {
                StringBuilder selectedInfo = new StringBuilder();
                selectedInfo.append("Selected: ").append(selected.getClass().getName().substring(37)).append("\n");
                selectedInfo.append("x: ").append(selected.getPosition().getX()).append("\n");
                selectedInfo.append("y: ").append(selected.getPosition().getY()).append("\n");
                selectedLabel.setText(selectedInfo);
            }
            if (!UIDisplayed) {
                leftPanel.add(timeAndFPS);
                leftPanel.row().pad(10);
                leftPanel.add(keyOptions);

                selectedInfoTable.add(selectedLabel);
                selectedInfoTable.row().pad(10);
                UIDisplayed = true;
            }

            for (InputElement inputElement : inputElements) {
                inputElement.update(selected, selectedInfoTable, animationMode);
            }
        } else {
            if (UIDisplayed) {
                leftPanel.clear();
                selectedInfoTable.clear();
                UIDisplayed = false;
            }
        }

        stage.act();
        stage.draw();

        if (!paused) { //now that both update and draw are done, advance the time
            time++;
        }
    }

    public boolean keyTyped(char character) {
        return false;
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

        for (Action action : KeyPressActionKt.getActions()) {
            if (action.shouldExecute(keycode, shift_pressed, ctrl_pressed, selected, touchMode)) {
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
        return true;
    }

    public boolean touchDown(int x, int y, int pointer, int button) {
        lastPressType = button;
        updateMouse(x, y);
        y = DISPLAY_HEIGHT - y;

        System.out.println("Clicked " + mouseX + " " + mouseY + " touch mode " + touchMode);

        if (paused) {
            if (touchMode == TouchMode.DEFAULT) { // Default behavior: select an object to show info about it
                ArrayList<Object> selectedThings = select(x, y);
                if (selectedThings.isEmpty()) {
                    resetSelected();
                } else {
                    selected = select(x, y).get(0);
                }
            }
            if (touchMode == TouchMode.MOVE) { // selects an object to move
                if (selected != null) {
                    selected.newSetPoint(time, mouseX, mouseY);
                    if (selected.getClass() == Node.class) {
                        Object newSelection = null;
                        for (Object selection : select(x, y)) {
                            if (selection != selected) {
                                newSelection = selection;
                            }
                        }
                        if (newSelection != null) {
                            if (newSelection.getClass() == Node.class) { //If the current selection is a Node and the user clicks another Node, merge the 2 nodes by setting the selected to the same point and setting its death
                                selected.newSetPoint(time, newSelection.getPosition().getX(), newSelection.getPosition().getY());
                                selected.setDeath(time);
                                resetSelected();
                                return true;
                            }
                        }
                    }
                    resetSelected();
                    return true;
                }

                ArrayList<Object> selectedThings = select(x, y);
                if (selectedThings.isEmpty()) {
                    resetSelected();
                } else {
                    selected = select(x, y).get(0);
                }
            }
            if (touchMode == TouchMode.NEW_NODE) { //will add a new Node to the List<Node> selectedNodeCollection.getNodes(), this includes front lines and area borders
                if ((selected != null) && (selected.getClass() == Node.class)) {
                    int addAtIndex = selectedNodeCollection.getNodes().indexOf((Node) selected) + 1;
                    Node node;
                    Object newSelection = select(x, y).get(0);
                    if ((newSelection != null) && (newSelection.getClass() == Node.class)) {
                        addAtIndex = selectedNodeCollection.getNodes().indexOf((Node) newSelection) + 1;
                        if (addAtIndex != 0) {
                            node = createNodeAtPosition(time, newSelection.getPosition().getX(), newSelection.getPosition().getY());
                        } else {
                            node = createNodeAtMouse(time);
                        }
                    } else {
                        node = createNodeAtMouse(time);
                    }

                    selected = node;
                    selectedNodeCollection.getNodes().add(addAtIndex, node);
                } else {
                    ArrayList<Object> selectedThings = select(x, y);
                    if (selectedThings.isEmpty()) {
                        resetSelected();
                    } else {
                        selected = select(x, y).get(0);
                    }
                }
            }

            if (touchMode == TouchMode.SET_AREA_LINE) {
                System.out.println("Checking for clicked line");
                if (selected != null) {
                    for (Line l : animation.getLines()) {
                        for (Object node : l.getNodes()) {
                            if (node.clicked(x, y)) {
                                Area selectedArea = animation.getAreaOfNode(selected);
                                selectedArea.getLineIDs().add(new Pair<>(l.getId(), selectedArea.getNodes().indexOf((Node) selected)));
                                touchMode = TouchMode.DEFAULT;
                                resetSelected();
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

    private Node createNodeAtMouse(int time) {
        Node node = new Node(
                new Coordinate(0, 0),
                new Coordinate(mouseX, mouseY)
        );

        HashMap<Integer, Coordinate> hashMap = new HashMap<>();
        hashMap.put(time, new Coordinate(mouseX, mouseY));

        node.getMovementFrames().add(
                new GroupedMovement<>(hashMap)
        );
        return node;
    }

    private Node createNodeAtPosition(int time, float x, float y) {
        Node node = new Node(
                new Coordinate(0, 0),
                new Coordinate(x, y)
        );

        HashMap<Integer, Coordinate> hashMap = new HashMap<>();
        hashMap.put(time, new Coordinate(x, y));

        node.getMovementFrames().add(
                new GroupedMovement<>(hashMap)
        );
        return node;
    }

    public ArrayList<Object> select(int x, int y) {
        ArrayList<Object> output = new ArrayList<>();
        for (Line line : animation.getLines()) {
            for (Object node : line.getDrawNodes(time)) {
                if (node.clicked(x, y)) {
                    System.out.println("Line node at " + node.getPosition() + " on line " + line.getId() + " was clicked");
                    selectedNodeCollection = line;
                    output.add(node);
                }

            }
        }

        for (Area area : animation.getAreas()) {
            for (Object node : area.getDrawNodes(time)) {
                if (node.clicked(x, y)) {
                    System.out.println("Area node at " + node.getPosition() + " was clicked");
                    selectedNodeCollection = area;
                    output.add(node);
                }
            }
        }

        for (Unit unit : animation.getUnits()) { // TODO: Make a getDrawUnits() function
            if (selected != unit) {
                if (unit.clicked(x, y)) {
                    System.out.println(unit.getPosition() + " was clicked");
                    output.add(unit);
                }
            }
        }
        return output;
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
        mouseX = (float) ((double) x - camera.position.x * (1 - camera.zoom) - (DISPLAY_WIDTH / 2.0f - camera.position.x)) / camera.zoom;
        mouseY = (float) ((double) y - camera.position.y * (1 - camera.zoom) - (DISPLAY_HEIGHT / 2.0f - camera.position.y)) / camera.zoom;
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
        FileHandler.INSTANCE.save();
    }

    private void resetSelected() {
        selected = null;
        selectedNodeCollection = null;
    }

    public void buildInputs() {
        // Inputs which require selected unit
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            Unit unit = (Unit) selected;
            unit.setSize(input);
            return null;
        }, () -> {
            if (selected != null) {
                Unit unit = (Unit) selected;
                return unit.getSize();
            } else {
                return "";
            }
        }, String.class,"Set size").requiredSelectedTypes(Unit.class).build() );
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            Unit unit = (Unit) selected;
            unit.setType(input);
            return null;
        }, () -> {
            if (selected != null) {
                Unit unit = (Unit) selected;
                return unit.getType();
            } else {
                return "";
            }
        }, String.class, "Set type").requiredSelectedTypes(Unit.class).build());
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            Unit unit = (Unit) selected;
            unit.setName(input);
            return null;
        }, () -> {
            if (selected != null) {
                Unit unit = (Unit) selected;
                return unit.getName();
            } else {
                return "";
            }
        }, String.class, "Set name").requiredSelectedTypes(Unit.class).build());
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            if ((selectedNodeCollection != null) && (selectedNodeCollection.getClass() == Area.class)) {
                Area selectedArea = (Area) selectedNodeCollection;
                for (AreaColor color : AreaColor.getEntries()) {
                    if (input.equals(color.name())) {
                        selectedArea.setColor(color);
                    }
                }
            }
            return null;
        }, () -> {
            if ((selectedNodeCollection != null) && (selectedNodeCollection.getClass() == Area.class)) {
                Area selectedArea = (Area) selectedNodeCollection;
                return selectedArea.getColor().name();
            }
            return "";
        }, String.class, "Set area color").requiredSelectedTypes(Node.class).build());
    }

    public void buildActions() {
        // Actions available when game is not inputting
        // Actions that do not care about selection
        Action.createBuilder(() -> {
            paused = !paused;
            return null;
        }, "Pause/Unpause the game", Input.Keys.SPACE).requiresSelected(Requirement.ANY).build();
        // Shift required
        Action.createBuilder(() -> {
            game.menu = new Menu(game);
            game.setScreen(game.menu);
            return null;
        }, "Return to the main menu", Input.Keys.ESCAPE).requiresSelected(Requirement.ANY).requiresShift(true).build();
        Action.createBuilder(() -> {
            animationMode = !animationMode;
            return null;
        }, "Toggle animation mode", Input.Keys.V).requiresSelected(Requirement.ANY).build();
        // Selection required
        Action.createBuilder(() -> {
            selected.newSetPoint(time);
            resetSelected();
            return null;
        }, "Keep object at current position", Input.Keys.N).requiresSelected(Requirement.REQUIRES).build();
        // Node required
        Action.createBuilder(() -> {
            HashMap<Integer, Coordinate> hashMap = new HashMap<>();
            hashMap.put(time, new Coordinate(mouseX, mouseY));

            Node node = new Node(
                    new Coordinate(0, 0),
                    new Coordinate(mouseX, mouseY)
            );
            node.getMovementFrames().add(
                    new GroupedMovement<>(hashMap)
            );
            Area a = new Area(new ArrayList<>(), AreaColor.RED, new ArrayList<>(), new ArrayList<>());
            //a.getLineIDs().add(new Pair<>(0, 0));

            animation.getAreas().add(a);

            selectedNodeCollection = a;
            selected = node;
            selectedNodeCollection.getNodes().add((Node) selected);
            return null;
        }, "Create new area", Input.Keys.A).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build();
        // Selection prohibited
        Action.createBuilder(() -> {
            time = (time / 200) * 200 + 200;
            animation.camera().goToTime(time);
            updateCam();
            return null;
        }, "Step time forward 200", Input.Keys.E).build();
        Action.createBuilder(() -> {
            time = (int) Math.ceil(time / 200.0) * 200 - 200;
            animation.camera().goToTime(time);
            updateCam();
            return null;
        }, "Step time back 200", Input.Keys.Q).build();
        Action.createBuilder(() -> {
            time++;
            animation.camera().goToTime(time);
            updateCam();
            System.out.println(animation.getLines().get(0).getDrawNodes(time).size());
            return null;
        }, "Step time forward 1", Input.Keys.PERIOD).build();
        Action.createBuilder(() -> {
            time--;
            animation.camera().goToTime(time);
            updateCam();
            System.out.println(animation.getLines().get(0).getDrawNodes(time).size());
            return null;
        }, "Step time back 1", Input.Keys.COMMA).build();
        Action.createBuilder(() -> {
            if (touchMode == TouchMode.MOVE) {
                touchMode = TouchMode.DEFAULT;
                return null;
            }
            touchMode = TouchMode.MOVE;
            return null;
        }, "Switch to move mode", Input.Keys.M).requiresSelected(Requirement.REQUIRES_NOT).build();
        Action.createBuilder(() -> {
            createNodeAtMouse(time);
            animation.setLineID(animation.getLineID() + 1);
            animation.getLines().add(new Line(animation.getLineID(), new ArrayList<>(), new Float[0], new Float[0]));
            selectedNodeCollection = Objects.requireNonNull(animation.getLineByID(animation.getLineID()));
            Node node = createNodeAtMouse(time);
            selectedNodeCollection.getNodes().add(node);
            selected = node;
            return null;
        }, "Create new line", Input.Keys.L).requiresSelected(Requirement.REQUIRES_NOT).build();
        Action.createBuilder(() -> {
            if (touchMode != TouchMode.NEW_NODE) {
                touchMode = TouchMode.NEW_NODE;
                System.out.println("New Node Mode");
            } else {
                touchMode = TouchMode.DEFAULT;
                resetSelected();
                System.out.println("Default Mode");
            }
            return null;
        }, "New Node Mode", Input.Keys.NUM_1).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build();
        for (int num_key = Input.Keys.NUM_2; (num_key <= Input.Keys.NUM_9) && (num_key < animation.getCountries().size() + 9); num_key++) {
            int finalNum_key = num_key;
            Action.createBuilder(() -> {
                    Unit unit = new Unit(
                            animation.getCountries().get(finalNum_key - 9), //number key enum number to list number
                            null,
                            "infantry",
                            "XX",
                            new ArrayList<>(),
                            null,
                            new Coordinate(0, 0),
                            new Coordinate(mouseX, mouseY),
                            0.0f
                    );

                    HashMap<Integer, Coordinate> hashMap = new HashMap<>();
                    hashMap.put(time, new Coordinate(mouseX, mouseY));

                    unit.getMovementFrames().add(
                            new GroupedMovement<>(hashMap)
                    );

                    System.out.println(unit);

                    animation.getUnits().add(unit);
                    selected = unit;
                    return null;
                }, "Create Unit of country " + animation.getCountries().get(finalNum_key - 9), num_key
            ).requiresSelected(Requirement.REQUIRES_NOT).requiredTouchModes(TouchMode.DEFAULT, TouchMode.MOVE).build();
        }
        //Key presses which require control pressed
        Action.createBuilder(() -> {
            animation.camera().newSetPoint(time, camera.position.x, camera.position.y, camera.zoom);
            return null;
        }, "Set camera set point", Input.Keys.C).requiresControl(true).build();
        Action.createBuilder(() -> {
            FileHandler.INSTANCE.save();
            System.out.println("saved");
            return null;
        }, "Save project", Input.Keys.S).requiresControl(true).build();
        //Key presses which require control pressed and selected Object
        Action.createBuilder(() -> {
            selected.setDeath(time);
            System.out.println("Death of " + selected + " set to " + selected.getDeath());
            resetSelected();
            return null;
        }, "Set death of an object", Input.Keys.D).requiresControl(true).requiresSelected(Requirement.REQUIRES).build();
        // Key presses which require selected Object
        Action.createBuilder(() -> {
            resetSelected();
            System.out.println("Deselected object");
            return null;
        }, "Deselect Object", Input.Keys.D).description("Deselects object").requiresSelected(Requirement.REQUIRES).build();
        Action.createBuilder(() -> {
            animation.deleteObject(selected);
            System.out.println("Deleted object");
            resetSelected();
            return null;
        }, "Delete selected unit", Input.Keys.FORWARD_DEL).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Unit.class).build();
        Action.createBuilder(() -> {
            int selectedIndex = selectedNodeCollection.getNodes().indexOf((Node) selected);
            animation.deleteObject(selected);
            if (selectedIndex > 0) {
                selected = selectedNodeCollection.getNodes().get(selectedIndex - 1);
            } else {
                resetSelected();
            }
            System.out.println("Deleted object");
            return null;
        }, "Delete selected node and select next node", Input.Keys.FORWARD_DEL).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build();
        Action.createBuilder(() -> {
            if (selected.removeFrame(time)) {
                System.out.println("Deleted last frame");
                System.out.println("New movements: " + selected.getMovementFrames());
            } else {
                System.out.println("Cannot delete last frame on object with less than 2 frames");
            }
            resetSelected();
            touchMode = TouchMode.DEFAULT;
            return null;
        }, "Delete last frame of selected object", Input.Keys.ESCAPE, Input.Keys.DEL).requiresSelected(Requirement.REQUIRES).build();
        // Key presses which require selected Node
        Action.createBuilder(() -> {
                    touchMode = TouchMode.SET_AREA_LINE;
                    System.out.println("Set Area Line");
                    return null;
                }, "Set Area Line", Input.Keys.S
        ).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build();
    }
}