package com.badlogicgames.waranimationmaker;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogicgames.waranimationmaker.models.Object;
import com.badlogicgames.waranimationmaker.models.*;
import kotlin.Pair;

import java.util.ArrayList;
import java.util.List;

import static com.badlogicgames.waranimationmaker.Assets.loadTexture;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH;

public class AnimationScreen extends ScreenAdapter implements InputProcessor {
    public static final int DEFAULT_UNIT_WIDTH = 10;
    public static final int DEFAULT_UNIT_HEIGHT = 10;
    public static final int MIN_LINE_SIZE = 2; //minimum number of nodes needed to draw a line
    public static final int LINES_PER_NODE = 12; //number of straight lines per node on a spline

    WarAnimationMaker game; // Contains some variables common to all screens
    OrthographicCamera camera; // Camera whose properties directly draw the screen

    float mouseX; // Mouse real X position
    float mouseY; // Mouse real Y position
    float zoomFactor; // Scales everything other than the map less than actual zoom
    Animation animation; // Contains all information about animation loaded from file
    Integer time;

    ScreenObject selected;
    NodeCollection selectedNodeCollection; // if a Node is selected, this will be the Area or Line that the Node is on

    boolean shiftPressed;
    boolean ctrlPressed;

    boolean paused;
    boolean animationMode;
    boolean UIDisplayed;

    TouchMode touchMode;
    List<InputElement> inputElements;

    GL20 gl;
    Texture fullMap; // entire background map
    ShapeRenderer shapeRenderer; // Draws all geometric shapes
    FrameBuffer colorLayer; // Colored areas are drawn to this layer
    //UI
    Stage stage; // Entire UI is drawn to this
    Table selectedInfoTable;
    Table leftPanel;
    Label timeAndFPS;
    Label keyOptions;
    Label selectedLabel;
    List<Action> actions;
    Node origin;

    public AnimationScreen(WarAnimationMaker game, Animation animation) {
        this.animation = animation;
        this.game = game;
        gl = game.gl;

        // Camera
        camera = new OrthographicCamera(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        camera.position.set(DISPLAY_WIDTH / 2.0f, DISPLAY_HEIGHT / 2.0f, 0);
        animation.camera();
        // Graphics init
        shapeRenderer = game.shapeRenderer;
        fullMap = loadTexture(animation.getPath());
        colorLayer = new FrameBuffer(Pixmap.Format.RGBA8888, 1024, 720, false);
        origin = createNodeAtPosition(0, 0, 0);

        // Animation init
        time = 0;
        paused = true;
        animationMode = true;

        //UI
        touchMode = TouchMode.DEFAULT;
        actions = new ArrayList<>();
        buildActions();
        UIDisplayed = false;
        stage = new Stage();
        inputElements = new ArrayList<>();

        timeAndFPS = new Label("", game.skin);
        keyOptions = new Label("", game.skin);
        selectedLabel = new Label("", game.skin);

        selectedInfoTable = new Table().right().top();
        selectedInfoTable.setPosition(DISPLAY_WIDTH - 30, DISPLAY_HEIGHT  - 30, 1);
        buildInputs(); // Builds the InputElement objects, which will be added to the selectedInfoTable
        stage.addActor(selectedInfoTable);

        leftPanel = new Table().left().top();
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

    public void updateCam() {
        camera.position.x = animation.camera().getPosition().getX();
        camera.position.y = animation.camera().getPosition().getY();
        camera.zoom = animation.camera().getZoom();
    }

    public boolean keyDown(int keycode) {
        for (Action action : actions) {
            if (action.shouldExecute(keycode, shiftPressed, ctrlPressed, selected, touchMode)) {
                action.execute();
                break;
            }
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    public boolean touchDown(int x, int y, int pointer, int button) {
        updateMouse(x, y);
        y = DISPLAY_HEIGHT - y;

        System.out.println("Clicked " + mouseX + " " + mouseY + " touch mode " + touchMode);

        if (paused) {
            if (touchMode == TouchMode.DEFAULT) { // Default behavior: select an object to show info about it
                ArrayList<ScreenObject> selectedThings = select(x, y);
                if (selectedThings.isEmpty()) {
                    resetSelected();
                } else {
                    selected = select(x, y).get(0);
                }
            }
            if (touchMode == TouchMode.MOVE) { // Selects an object to move. If a node is selected to be moved into another node, it will be merged
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
                                resetSelected();
                                return true;
                            }
                        }
                    }
                    resetSelected();
                    return true;
                }

                ArrayList<ScreenObject> selectedThings = select(x, y);
                if (selectedThings.isEmpty()) {
                    resetSelected();
                } else {
                    selected = select(x, y).get(0);
                }
            }
            if (touchMode == TouchMode.NEW_NODE) { //will add a new Node to the selectedNodeCollection or create a new line with the node if none are selected
                if ((selected != null) && (selected.getClass() == Node.class)) {
                    int addAtIndex = selectedNodeCollection.getNodes().indexOf((Node) selected) + 1;
                    Node node;
                    ArrayList<ScreenObject> newSelections = select(x, y);
                    if (!newSelections.isEmpty()) {
                        Object newSelection = newSelections.get(0);
                        if (((newSelection != null) && (newSelection.getClass() == Node.class)) && (selectedNodeCollection.getNodes().contains(newSelection))) { // If the user clicks on another node on the same line, merge the nodes
                            addAtIndex = selectedNodeCollection.getNodes().indexOf((Node) newSelection) + 1;
                            node = createNodeAtPosition(time, newSelection.getPosition().getX(), newSelection.getPosition().getY());
                            selected = node;
                            selectedNodeCollection.getNodes().add(addAtIndex, node);
                            return true;
                        }
                    }
                    // If the user does not select another node on the same line, create a new node on the same line in front of it
                    node = createNodeAtMouse(time);
                    selected = node;
                    selectedNodeCollection.getNodes().add(addAtIndex, node);
                } else { //
                    animation.setLineID(animation.getLineID() + 1);
                    Line newLine = new Line(animation.getLineID(), new ArrayList<>());
                    animation.getLines().add(newLine);
                    Node node = createNodeAtMouse(time);
                    newLine.getNodes().add(node);
                    selectedNodeCollection = newLine;
                    selected = node;
                    return true;
                }
            }

            if (touchMode == TouchMode.SET_AREA_LINE) {
                System.out.println("Checking for clicked line");
                if (selected != null) {
                    for (Line line : animation.getLines()) {
                        for (Node node : line.getNodes()) {
                            if (node.clicked(x, y)) {
                                Area selectedArea = (Area) selectedNodeCollection;
                                selectedArea.getLineIDAndOrder().add(new Pair<>(line.getId(), selectedArea.getNodes().indexOf((Node) selected)));
                                touchMode = TouchMode.DEFAULT;
                                resetSelected();
                                System.out.println("Area set to line");
                                return true;
                            }
                        }
                    }
                }
            }
            for (InputElement inputElement : inputElements) {
                inputElement.update(selected, selectedNodeCollection, selectedInfoTable, animationMode);
            }
        }
        return false;
    }

    private Node createNodeAtMouse(int time) {
        return createNodeAtPosition(time, mouseX, mouseY);
    }

    private Node createNodeAtPosition(int time, float x, float y) {
        return new Node(new Coordinate(x, y), time);
    }

    public ArrayList<ScreenObject> select(int x, int y) { //TODO make this take in type and boolean whether to search for nonDrawNodes or not
        ArrayList<ScreenObject> output = new ArrayList<>();
        ArrayList<ScreenObject> lowPriority = new ArrayList<>();
        for (Line line : animation.getLines()) {
            for (Node node : line.getDrawNodes()) {
                if (node.clicked(x, y)) {
                    System.out.println("Line node at " + node.getPosition() + " on line " + line.getId() + " was clicked");
                    selectedNodeCollection = line;
                    output.add(node);
                }
            }
            for (Node node : line.getNonDrawNodes()) {
                if (node.clicked(x, y)) {
                    System.out.println("Line node at " + node.getPosition() + " on line " + line.getId() + " was clicked");
                    selectedNodeCollection = line;
                    lowPriority.add(node);
                }
            }
        }

        for (Area area : animation.getAreas()) {
            for (Node node : area.getDrawNodes()) {
                if (node.clicked(x, y)) {
                    System.out.println("Area node at " + node.getPosition() + " was clicked");
                    selectedNodeCollection = area;
                    output.add(node);
                }
            }
            for (Node node : area.getNonDrawNodes()) {
                if (node.clicked(x, y)) {
                    System.out.println("Area node at " + node.getPosition() + " was clicked");
                    selectedNodeCollection = area;
                    lowPriority.add(node);
                }
            }
        }

        for (Unit unit : animation.getDrawUnits(time)) {
            if (selected != unit) {
                if (unit.clicked(x, y)) {
                    System.out.println(unit.getPosition() + " was clicked");
                    output.add(unit);
                }
            }
        }
        for (Unit unit : animation.getNonDrawUnits(time)) {
            if (selected != unit) {
                if (unit.clicked(x, y)) {
                    System.out.println(unit.getPosition() + " was clicked");
                    lowPriority.add(unit);
                }
            }
        }

        output.addAll(lowPriority);

        if (!output.isEmpty()) {
            System.out.println(output.get(0));
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
        mouseX = (float) ((double) x - camera.position.x * (1 - camera.zoom) - (Gdx.graphics.getWidth() / 2.0f - camera.position.x)) / camera.zoom;
        mouseY = (float) ((double) y - camera.position.y * (1 - camera.zoom) - (Gdx.graphics.getHeight() / 2.0f - camera.position.y)) / camera.zoom;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (paused) {
            camera.zoom *= 1 - 0.05f * amountY;
        }
        return true;
    }

    public void update() {
        ctrlPressed = (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT));
        shiftPressed = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));

        //Camera
        zoomFactor = 0.75f + camera.zoom * 8;
        animation.camera().goToTime(time);
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            camera.position.y += 10 / camera.zoom;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            camera.position.y -= 10 / camera.zoom;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            camera.position.x -= 10 / camera.zoom;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            camera.position.x += 10 / camera.zoom;
        }
        if (!paused) { //don't update camera when paused to allow for movement when paused
            updateCam();
        }
        camera.update();
        origin.goToTime(0, camera.zoom, camera.position.x, camera.position.y);

        if ((selected != null) && paused) { // Update the selected object to go to mouse in move mode
            if ((touchMode == TouchMode.MOVE)) {
                selected.newSetPoint(time, mouseX, mouseY);
            }
        }

        for (Line line : animation.getLines()) { // Interpolate lines and update all nodes
            for (Node node : line.getNodes()) {
                node.update(time, camera);
            }
            line.update(LINES_PER_NODE, time);
        }
        for (Area area : animation.getAreas()) { // Calculate area polygons
            for (Node node : area.getNodes()) {
                node.update(time, camera);
            }
            area.update(time, camera.zoom, camera.position.x, camera.position.y, animation);
        }
        for (Unit unit : animation.getUnits()) {
            unit.goToTime(time, camera.zoom, camera.position.x, camera.position.y);
        }
    }

    @Override
    public void render(float delta) {
        update();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //Draw background
        game.batcher.begin();

        game.batcher.draw(fullMap, origin.getScreenPosition().getX(),
                origin.getScreenPosition().getY(),
                fullMap.getWidth() * camera.zoom,
                fullMap.getHeight() * camera.zoom);
        game.batcher.end();

        // Draw area polygons
        colorLayer.begin();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.CLEAR);
        shapeRenderer.rect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT); // Clear the color layer

        for (Area area : animation.getAreas()) { // Draw areas to the color layer
            area.draw(shapeRenderer);
        }

        shapeRenderer.end();
        colorLayer.end();

        // Draw the color layer to the screen TODO replace color layer
        game.batcher.begin();
        TextureRegion textureRegion = new TextureRegion(colorLayer.getColorBufferTexture());
        textureRegion.flip(false, true);
        game.batcher.setColor(1,1,1,0.2f); // Draw the color layers with transparency
        game.batcher.draw(textureRegion, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        game.batcher.end();

        shapeRenderer.end();

        // Draw Units and Lines
        game.batcher.begin();
        game.batcher.setColor(255,255,255, 1); // Resets to no transparency
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Unit unit : animation.getUnits()) {
            unit.draw(game.batcher, zoomFactor, game.bitmapFont);
        }
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Line line : animation.getLines()) {
            line.draw(shapeRenderer);
        }
        shapeRenderer.end();
        game.batcher.setColor(1, 1, 1, 1.0f);
        game.batcher.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        //Draw the debug circles
        if (animationMode) {shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);}
        for (Line line : animation.getLines()) {
            for (Node node : line.getNodes()) {
                node.draw(shapeRenderer, animationMode);
            }
        }
        if (animationMode) { shapeRenderer.end(); }

        if (animationMode) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            //Draw area polygon nodes
            shapeRenderer.setColor(Color.BLUE); //TODO make a different area node class
            for (Area area : animation.getAreas()) {
                for (Node node : area.getNodes()) {
                    shapeRenderer.circle(node.getScreenPosition().getX(), node.getScreenPosition().getY(), 7);
                }
            }

            //Draw the selected object
            if (selected != null) {
                selected.drawAsSelected(shapeRenderer, animationMode, camera.zoom, camera.position.x, camera.position.y);
            }

            //Draw the UI
            String text = Gdx.graphics.getFramesPerSecond() + " FPS \n" + "Time: " + time;
            timeAndFPS.setText(text);

            //Set keyboard options and current animator state
            StringBuilder options = new StringBuilder();
            options.append("Touch mode: ").append(touchMode.name()).append("\n");
            options.append("Control pressed: ").append(ctrlPressed).append(" ").append("Shift pressed: ").append(shiftPressed).append("\n");
            for (Action action : actions) {
                if (action.couldExecute(shiftPressed, ctrlPressed, selected, touchMode)) {
                    for (int key : action.getActionKeys()) {
                        options.append(Input.Keys.toString(key)).append(" ");
                    }
                    options.append(" ").append(action.getActionName()).append("\n");
                }
            }
            keyOptions.setText(options);

            //Add information about selected object
            if (selected == null) {
                selectedLabel.setText("Nothing is selected \n" + "Mouse: " + mouseX + ", " + mouseY);
            } else {
                StringBuilder selectedInfo = new StringBuilder();
                selectedInfo.append("Selected: ").append(selected.getClass().getName().substring(37)).append("\n");
                selectedInfo.append("x: ").append(selected.getPosition().getX()).append("\n");
                selectedInfo.append("y: ").append(selected.getPosition().getY()).append("\n");
                if (selectedNodeCollection != null) {
                    selectedInfo.append("Selected ").append(selectedNodeCollection.getClass().getName().substring(37)).append(": \n");
                    selectedInfo.append("Nodes: ").append(selectedNodeCollection.getNodes().size()).append("\n");
                    if (selectedNodeCollection.getClass() == Line.class) {
                        Line selectedLine = (Line) selectedNodeCollection;
                        selectedInfo.append("LineID: ").append(selectedLine.getId()).append("\n");
                    }
                    if (selectedNodeCollection.getClass() == Area.class) {
                        Area selectedArea = (Area) selectedNodeCollection;
                        selectedInfo.append(selectedArea.getLineIDAndOrder());
                    }
                }
                selectedLabel.setText(selectedInfo);
            }

            // Draw contrast backgrounds for UI
            shapeRenderer.setColor(new Color(255, 255, 255, 0.5f));
            shapeRenderer.rect(0, DISPLAY_HEIGHT, 300, 500);
            shapeRenderer.rect(DISPLAY_WIDTH - 200, DISPLAY_HEIGHT, 200, 500);

            shapeRenderer.end();

            if (!UIDisplayed) {
                leftPanel.add(timeAndFPS);
                leftPanel.row();
                leftPanel.add(keyOptions);

                selectedInfoTable.add(selectedLabel);
                selectedInfoTable.row().pad(10);
                UIDisplayed = true;
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

    @Override
    public void pause() {
        FileHandler.INSTANCE.save();
    }

    private void resetSelected() {
        selected = null;
        selectedNodeCollection = null;

        for (InputElement inputElement : inputElements) { // selected object is set to null, call all the inputElements to remove themselves
            inputElement.update(selected, selectedNodeCollection, selectedInfoTable, animationMode);
        }
    }

    public void buildInputs() {
        // Inputs which require selected Object
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            selected.setDeath(input);
            return null;
        }, () -> {
            if (selected != null) {
                if (selected.getDeath() != null) {
                    return String.valueOf(selected.getDeath());
                }
            }
            return null;
        }, Integer.class,"Set death").requiredSelectedTypes(Object.class).build());
        // Inputs which require selected unit
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            if (input != null) {
                Unit unit = (Unit) selected;
                unit.setSize(input);
            }
            return null;
        }, () -> {
            if (selected != null) {
                Unit unit = (Unit) selected;
                return unit.getSize();
            } else {
                return "";
            }
        }, String.class,"Set size").requiredSelectedTypes(Unit.class).build());
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            if (input != null) {
                Unit unit = (Unit) selected;
                unit.setType(input);
            }
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
            if (input != null) {
                Unit unit = (Unit) selected;
                unit.setName(input);
            }
            return null;
        }, () -> {
            if (selected != null) {
                Unit unit = (Unit) selected;
                return unit.getName();
            } else {
                return "";
            }
        }, String.class, "Set name").requiredSelectedTypes(Unit.class).build());
        // Inputs which require selected Area
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            if (input != null) {
                if ((selectedNodeCollection != null) && (selectedNodeCollection.getClass() == Area.class)) {
                    Area selectedArea = (Area) selectedNodeCollection;
                    for (AreaColor color : AreaColor.getEntries()) {
                        if (input.equals(color.name())) {
                            selectedArea.setColor(color);
                        }
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
        }, String.class, "Set area color").requiredSelectedTypes(Node.class).requiredSelectedNodeCollectionTypes(Area.class).build());
        // Inputs which require selected Line
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            if (input != null) {
                if ((selectedNodeCollection != null) && (selectedNodeCollection.getClass() == Line.class)) {
                    Line selectedLine = (Line) selectedNodeCollection;
                    for (AreaColor color : AreaColor.getEntries()) {
                        if (input.equals(color.name())) {
                            selectedLine.setColor(color);
                        }
                    }
                }
            }
            return null;
        }, () -> {
            if ((selectedNodeCollection != null) && (selectedNodeCollection.getClass() == Line.class)) {
                Line selectedLine = (Line) selectedNodeCollection;
                return selectedLine.getColor().name();
            }
            return "";
        }, String.class, "Set line color").requiredSelectedTypes(Node.class).requiredSelectedNodeCollectionTypes(Line.class).build());
        inputElements.add(new InputElement.Companion.Builder<>(game.skin, (input) -> {
            if (input != null) {
                if ((selectedNodeCollection != null) && (selectedNodeCollection.getClass() == Line.class)) {
                    Line selectedLine = (Line) selectedNodeCollection;
                    selectedLine.setLineThickness(input);
                }
            }
            return null;
        }, () -> {
            if ((selectedNodeCollection != null) && (selectedNodeCollection.getClass() == Line.class)) {
                Line selectedLine = (Line) selectedNodeCollection;
                return String.valueOf(selectedLine.getLineThickness());
            }
            return "";
        }, float.class, "Set line width").requiredSelectedTypes(Node.class).requiredSelectedNodeCollectionTypes(Line.class).build());
    }

    public void buildActions() {
        // Actions available when game is not inputting
        // Actions that do not care about selection
        actions.add(Action.createBuilder(() -> {
            paused = !paused;
            return null;
        }, "Pause/Unpause the game", Input.Keys.SPACE).requiresSelected(Requirement.ANY).build());
        // Shift required
        actions.add(Action.createBuilder(() -> {
            game.menu = new Menu(game);
            game.setScreen(game.menu);
            return null;
        }, "Return to the main menu", Input.Keys.ESCAPE).requiresSelected(Requirement.ANY).requiresShift(true).build());
        actions.add(Action.createBuilder(() -> {
            animationMode = !animationMode;
            return null;
        }, "Toggle animation mode", Input.Keys.V).requiresSelected(Requirement.ANY).build());
        // Selection required
        actions.add(Action.createBuilder(() -> {
            selected.holdPositionUntil(time);
            resetSelected();
            return null;
        }, "Hold last defined position to this time", Input.Keys.H).requiresSelected(Requirement.REQUIRES).build());
        // Node required
        actions.add(Action.createBuilder(() -> {
            Node node = new Node(new Coordinate(mouseX, mouseY), time);
            Area area = new Area(new ArrayList<>());
            area.getNodes().add(node);

            animation.getAreas().add(area);

            selectedNodeCollection = area;
            selected = node;
            return null;
        }, "Create new area", Input.Keys.A).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build());
        // Selection prohibited
        actions.add(Action.createBuilder(() -> {
            time = (time / 200) * 200 + 200;
            animation.camera().goToTime(time);
            updateCam();
            return null;
        }, "Step time forward 200", Input.Keys.E).build());
        actions.add(Action.createBuilder(() -> {
            time = (int) Math.ceil(time / 200.0) * 200 - 200;
            animation.camera().goToTime(time);
            updateCam();
            return null;
        }, "Step time back 200", Input.Keys.Q).build());
        actions.add(Action.createBuilder(() -> {
            time++;
            animation.camera().goToTime(time);
            updateCam();
            System.out.println(animation.getLines().get(0).getDrawNodes().size());
            return null;
        }, "Step time forward 1", Input.Keys.PERIOD).build());
        actions.add(Action.createBuilder(() -> {
            time--;
            animation.camera().goToTime(time);
            updateCam();
            System.out.println(animation.getLines().get(0).getDrawNodes().size());
            return null;
        }, "Step time back 1", Input.Keys.COMMA).build());
        actions.add(Action.createBuilder(() -> {
            if (touchMode == TouchMode.MOVE) {
                touchMode = TouchMode.DEFAULT;
                return null;
            }
            touchMode = TouchMode.MOVE;
            return null;
        }, "Switch to move mode", Input.Keys.M).requiresSelected(Requirement.REQUIRES_NOT).build());
        actions.add(Action.createBuilder(() -> {
            if (touchMode != TouchMode.NEW_NODE) {
                touchMode = TouchMode.NEW_NODE;
                System.out.println("New Node Mode");
            } else {
                touchMode = TouchMode.DEFAULT;
                resetSelected();
                System.out.println("Default Mode");
            }
            return null;
        }, "New Node Mode", Input.Keys.NUM_1).requiresSelected(Requirement.ANY).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build());
        for (int num_key = Input.Keys.NUM_2; (num_key <= Input.Keys.NUM_9) && (num_key < animation.getCountries().size() + 9); num_key++) {
            int finalNum_key = num_key;
            actions.add(Action.createBuilder(() -> {
                        Unit unit = new Unit(
                                new Coordinate(mouseX, mouseY),
                                time,
                                animation.getCountries().get(finalNum_key - 9) //number key enum number to list number
                        );

                        System.out.println(unit);

                        animation.getUnits().add(unit);
                        selected = unit;
                        return null;
                    }, "Create Unit of country " + animation.getCountries().get(finalNum_key - 9), num_key
            ).requiresSelected(Requirement.REQUIRES_NOT).requiredTouchModes(TouchMode.DEFAULT, TouchMode.MOVE).build());
        }
        //Key presses which require control pressed
        actions.add(Action.createBuilder(() -> {
            selected = animation.camera();
            return null;
        }, "Select the camera", Input.Keys.C).requiresControl(true).build());
        actions.add(Action.createBuilder(() -> {
            animation.camera().getZoomInterpolator().newSetPoint(time, camera.zoom);
            return null;
        }, "Set a camera zoom set point", Input.Keys.Z).requiresControl(true).build());
        actions.add(Action.createBuilder(() -> {
            FileHandler.INSTANCE.save();
            System.out.println("saved");
            return null;
        }, "Save project", Input.Keys.S).requiresControl(true).build());
        //Key presses which require control pressed and selected Object
        actions.add(Action.createBuilder(() -> {
            selected.setDeath(time);
            System.out.println("Death of " + selected + " set to " + selected.getDeath());
            resetSelected();
            return null;
        }, "Set death of an object", Input.Keys.D).requiresControl(true).requiresSelected(Requirement.REQUIRES).build());
        // Key presses which require selected Object
        actions.add(Action.createBuilder(() -> {
            resetSelected();
            System.out.println("Deselected object");
            return null;
        }, "Deselect Object", Input.Keys.D).description("Deselects object").requiresSelected(Requirement.REQUIRES).build());
        actions.add(Action.createBuilder(() -> {
            animation.deleteObject(selected);
            System.out.println("Deleted object");
            resetSelected();
            return null;
        }, "Delete selected unit", Input.Keys.FORWARD_DEL).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Unit.class).build());
        actions.add(Action.createBuilder(() -> {
            int selectedIndex = selectedNodeCollection.getNodes().indexOf((Node) selected);
            animation.deleteObject(selected);
            if (selectedIndex > 0) {
                selected = selectedNodeCollection.getNodes().get(selectedIndex - 1);
            } else {
                resetSelected();
            }
            System.out.println("Deleted object");
            return null;
        }, "Delete selected node and select next node", Input.Keys.FORWARD_DEL).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build());
        actions.add(Action.createBuilder(() -> {
            if (selected.removeFrame(time)) {
                System.out.println("Deleted last frame");
                System.out.println("New movements: " + selected.getXInterpolator().getSetPoints().keySet());
                System.out.println("           xs: " + selected.getXInterpolator().getSetPoints().values());
                System.out.println("           ys: " + selected.getYInterpolator().getSetPoints().values());
            } else {
                System.out.println("Cannot delete frame on object with less than 2 frames");
            }
            resetSelected();
            touchMode = TouchMode.DEFAULT;
            return null;
        }, "Delete last frame of selected object", Input.Keys.ESCAPE, Input.Keys.DEL).requiresSelected(Requirement.REQUIRES).build());
        // Key presses which require selected Node
        actions.add(Action.createBuilder(() -> {
                    touchMode = TouchMode.SET_AREA_LINE;
                    System.out.println("Set Area Line");
                    return null;
                }, "Set Area Line", Input.Keys.S
        ).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build());
    }
}