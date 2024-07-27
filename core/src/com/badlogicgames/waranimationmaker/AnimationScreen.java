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
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogicgames.waranimationmaker.models.Object;
import com.badlogicgames.waranimationmaker.models.*;
import kotlin.Pair;

import java.util.*;

import static com.badlogicgames.waranimationmaker.Assets.loadTexture;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH;
import static java.lang.Math.round;

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
    List<NodeCollection> selectedNodeCollections; // if a Node is selected, this will be the Area or Line that the Node is on

    boolean shiftPressed;
    boolean ctrlPressed;

    boolean paused;
    boolean animationMode;
    boolean UIDisplayed;

    TouchMode touchMode;

    GL20 gl;
    Texture fullMap; // entire background map
    ShapeRenderer shapeRenderer; // Draws all geometric shapes
    FrameBuffer colorLayer; // Colored areas are drawn to this layer
    //UI
    Stage stage; // Entire UI is drawn to this
    Table selectedInfoTable;
    VerticalGroup inputGroup;
    Table leftPanel;
    Label timeAndFPS;
    Label keyOptions;
    Label selectedLabel;
    List<Action> actions;
    Node origin;
    UIVisitor uiVisitor;

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
        origin = animation.createNodeAtPosition(0, 0, 0);

        // Animation init
        time = 0;
        paused = true;
        animationMode = true;

        //UI
        selectedNodeCollections = new ArrayList<>(2);
        touchMode = TouchMode.DEFAULT;
        actions = new ArrayList<>();
        buildActions();
        UIDisplayed = false;
        stage = new Stage();

        timeAndFPS = new Label("", game.skin);
        keyOptions = new Label("", game.skin);
        selectedLabel = new Label("", game.skin);

        inputGroup = new VerticalGroup();
        uiVisitor = new UIVisitor(inputGroup, game.skin);
        selectedInfoTable = new Table();
        stage.addActor(selectedInfoTable);

        leftPanel = new Table();
        stage.addActor(leftPanel);

        game.multiplexer.clear();
        game.multiplexer.addProcessor(stage);
        game.multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(game.multiplexer);

        // Final init
        animation.camera().goToTime(time);
        updateCam();
        animation.load(game.skin);
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

    public ArrayList<ScreenObject> select(float x, float y) {
        return select(x, y, NodeCollection.class);
    }

    public ArrayList<ScreenObject> select(float x, float y, Class<? extends NodeCollection> clazz) {
        if (selected != null) {
            selected.hideInputs(uiVisitor);
            for (NodeCollection collection : selectedNodeCollections) {
                collection.hideInputs(uiVisitor);
            }
        }

        ArrayList<ScreenObject> selectedThings = animation.selectObject(x, y);
        if (selectedThings.isEmpty()) {
            resetSelected();
            return new ArrayList<>(0);
        } else {
            selected = selectedThings.get(0);
            selected.showInputs(uiVisitor);
            selectedNodeCollections = animation.getParentsOfType(selected.getId(), clazz);
            for (NodeCollection collection : selectedNodeCollections) {
                collection.showInputs(uiVisitor);
            }
            return selectedThings;
        }
    }

    public boolean touchDown(int x, int y, int pointer, int button) {
        y = DISPLAY_HEIGHT - y;

        System.out.println("Clicked " + mouseX + " " + mouseY + " touch mode " + touchMode);

        if (paused) {
            if (touchMode == TouchMode.DEFAULT) { // Default behavior: select an object to show info about it
                select(x, y);
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
                select(x, y);
            }
            if (touchMode == TouchMode.NEW_NODE) { //will add a new Node to the selectedNodeCollection or create a new line with the node if none are selected
                if ((selected != null) && (selected.getClass() == Node.class)) {
                    NodeCollection selectedNodeCollection = selectedNodeCollections.get(0);
                    int addAtIndex = selectedNodeCollection.getNodeIDs().indexOf((NodeID) selected.getId()) + 1;
                    Node node;
                    ArrayList<ScreenObject> newSelections = animation.selectObjectWithType(x, y, Node.class);
                    if (!newSelections.isEmpty()) {
                        Node newSelection = (Node) newSelections.get(0);
                        if ((newSelection != null) && (selectedNodeCollection.getNodeIDs().contains(newSelection.getId()))) { // If the user clicks on another node on the same line, merge the nodes
                            addAtIndex = selectedNodeCollection.getNodeIDs().indexOf(newSelection.getId()) + 1;
                            node = animation.createNodeAtPosition(time, newSelection.getPosition().getX(), newSelection.getPosition().getY());
                            selected = node;
                            selected.showInputs(uiVisitor);
                            selectedNodeCollection.getNodeIDs().add(addAtIndex, node.getId());
                            return true;
                        }
                    }
                    // If the user does not select another node on the same line, create a new node on the same line in front of it
                    node = animation.createNodeAtPosition(time, mouseX, mouseY);
                    selected = node;
                    selected.showInputs(uiVisitor);
                    selectedNodeCollections.get(0).getNodeIDs().add(addAtIndex, node.getId());
                } else {
                    selected = animation.createNodeAtPosition(time, mouseX, mouseY);
                    selected.showInputs(uiVisitor);
                    selectedNodeCollections.add(animation.addNewLine(Collections.singletonList((NodeID) selected.getId())));

                    return true;
                }
            }

            if (touchMode == TouchMode.SET_AREA_LINE) {
                System.out.println("Checking for clicked line");
                if (selected != null) {
                    selectedNodeCollections = animation.getParentsOfType(selected.getId(), Area.class);
                    for (Line line : animation.getLines()) {
                        for (NodeID nodeID : line.getNodeIDs()) {
                            Node node = animation.getNodeByID(nodeID);
                            assert node != null;
                            if (node.clicked(x, y)) {
                                Area selectedArea = (Area) selectedNodeCollections.get(0);
                                selectedArea.getOrderOfLineSegments().getOrDefault(selectedArea.getNodeIDs().indexOf(node.getId()),
                                        Collections.singletonList(new LineSegment(line.getId(),
                                        new Pair<>(line.getNodeIDs().get(0), line.getNodeIDs().get(line.getNodeIDs().size() - 1)))));
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

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (paused) {
            camera.zoom *= 1 - 0.05f * amountY;
        }
        return true;
    }

    public void update() {
        mouseX = (float) ((double) Gdx.input.getX() - camera.position.x * (1 - camera.zoom) - (Gdx.graphics.getWidth() / 2.0f - camera.position.x)) / camera.zoom;
        mouseY = (float) ((double) (DISPLAY_HEIGHT - Gdx.input.getY()) - camera.position.y * (1 - camera.zoom) - (Gdx.graphics.getHeight() / 2.0f - camera.position.y)) / camera.zoom;
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

        animation.update(time, camera, inputGroup, animationMode);

        //UI
        if (animationMode) {
            // Set information about keyboard options and current animator state
            timeAndFPS.setText(Gdx.graphics.getFramesPerSecond() + " FPS \n" + "Time: " + time);

            StringBuilder options = new StringBuilder();
            StringJoiner optionsJoiner = new StringJoiner("\n");
            options.append("Touch mode: ").append(touchMode.name()).append("\n");
            options.append("Control pressed: ").append(ctrlPressed).append(" ").append("Shift pressed: ").append(shiftPressed).append("\n");
            for (Action action : actions) {
                if (action.couldExecute(shiftPressed, ctrlPressed, selected, touchMode)) {
                    StringJoiner keysJoiner = new StringJoiner(", ");
                    for (int key : action.getActionKeys()) {
                        keysJoiner.add(Input.Keys.toString(key) + " ");
                    }
                    optionsJoiner.add(keysJoiner + action.getActionName());
                }
            }
            options.append(optionsJoiner);
            keyOptions.setText(options);

            //Add information about mouse position selected object
            StringBuilder selectedInfo = new StringBuilder("Mouse: " + round(mouseX) + ", " + round(mouseY) + "\n");

            if (selected == null) {
                selectedInfo.append("Nothing is selected");
            } else {
                selectedInfo.append("Selected: ").append(selected.getClass().getName().substring(43)).append("\n");
                selectedInfo.append("x: ").append(selected.getPosition().getX()).append("\n");
                selectedInfo.append("y: ").append(selected.getPosition().getY()).append("\n");
                selectedInfo.append("ID: ").append(selected.getId().getValue()).append("\n");
                if (selectedNodeCollections != null) {
                    for (NodeCollection nodeCollection : selectedNodeCollections) {
                        selectedInfo.append("Selected ").append(nodeCollection.getClass().getName().substring(43)).append(": \n");
                        selectedInfo.append("Nodes: ").append(nodeCollection.getNodeIDs().size()).append("\n");
                        if (nodeCollection.getClass() == Line.class) {
                            Line selectedLine = (Line) nodeCollection;
                            selectedInfo.append("LineID: ").append(selectedLine.getId().getValue()).append("\n");
                        }
                        if (nodeCollection.getClass() == Area.class) {
                            selectedInfo.append(nodeCollection).append("\n");
                        }
                    }
                }
            }
            selectedLabel.setText(selectedInfo);

            if (!UIDisplayed) {
                leftPanel.add(timeAndFPS).left().pad(10);
                leftPanel.row();
                leftPanel.add(keyOptions).pad(10);
                leftPanel.row();

                selectedInfoTable.add(selectedLabel).expandX().pad(10).left();
                selectedInfoTable.row().pad(10);
                if (selected != null) {
                    selected.showInputs(uiVisitor);
                }
                selectedInfoTable.add(inputGroup);

                UIDisplayed = true;
            }

            leftPanel.pack();
            leftPanel.setPosition(30, DISPLAY_HEIGHT - 30 - leftPanel.getHeight());

            selectedInfoTable.pack();
            inputGroup.pack();
            selectedInfoTable.setPosition(DISPLAY_WIDTH - 30 - selectedInfoTable.getWidth(), DISPLAY_HEIGHT  - 30 - selectedInfoTable.getHeight());
        } else {
            if (UIDisplayed) {
                if (selected != null) {
                    selected.hideInputs(uiVisitor);
                }
                leftPanel.clear();
                selectedInfoTable.clear();
                UIDisplayed = false;
            }
        }

        stage.act();
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
        animation.getNodeHandler().draw(game.batcher, shapeRenderer, colorLayer, animationMode);
        animation.getUnitHandler().draw(game, shapeRenderer, zoomFactor);

        game.batcher.setColor(1, 1, 1, 1.0f);

        if (animationMode) {
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // Draw contrast backgrounds for UI
            shapeRenderer.setColor(new Color(0, 0, 0, 0.5f));

            shapeRenderer.rect(leftPanel.getX(), leftPanel.getY(), leftPanel.getWidth(), leftPanel.getHeight());
            shapeRenderer.rect(selectedInfoTable.getX(), selectedInfoTable.getY(), selectedInfoTable.getWidth(), selectedInfoTable.getHeight());

            //Draw the selected object
            if (selected != null) {
                selected.drawAsSelected(shapeRenderer, animationMode, camera.zoom, camera.position.x, camera.position.y);
            }

            shapeRenderer.end();
            Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
        }

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
        selected.hideInputs(uiVisitor);
        for (NodeCollection nodeCollection : selectedNodeCollections) {
            nodeCollection.hideInputs(uiVisitor);
        }
        selected = null;
        selectedNodeCollections = null;
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
            return null;
        }, "Step time forward 1", Input.Keys.PERIOD).build());
        actions.add(Action.createBuilder(() -> {
            time--;
            animation.camera().goToTime(time);
            updateCam();
            return null;
        }, "Step time back 1", Input.Keys.COMMA).build());
        actions.add(Action.createBuilder(() -> {
            if (touchMode == TouchMode.MOVE) {
                touchMode = TouchMode.DEFAULT;
                return null;
            }
            touchMode = TouchMode.MOVE;
            return null;
        }, "Toggle move mode", Input.Keys.M).requiresSelected(Requirement.ANY).build());
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
        }, "New Node Mode", Input.Keys.NUM_1).requiresSelected(Requirement.ANY).build());
        actions.add(Action.createBuilder(() -> {
            Node node = animation.createNodeAtPosition(time, mouseX, mouseY);
            Area area = animation.addNewArea(Collections.singletonList(node.getId()));

            selectedNodeCollections = Collections.singletonList(area);
            selected = node;
            touchMode = TouchMode.NEW_NODE;
            return null;
        }, "Create new area", Input.Keys.A).requiresSelected(Requirement.REQUIRES_NOT).build());
        actions.add(Action.createBuilder(() -> {
            Node node = animation.createNodeAtPosition(time, mouseX, mouseY);
            Area area = animation.addNewArea(Collections.singletonList(node.getId()));

            Line selectedLine = null;

            System.out.println(selectedNodeCollections);

            for (NodeCollection nodeCollection : selectedNodeCollections) {
                if (nodeCollection instanceof Line) {
                    selectedLine = (Line) nodeCollection;
                }
            }

            assert selectedLine != null;
            area.getOrderOfLineSegments().put(1, Collections.singletonList(new LineSegment(selectedLine.getId(),
                    new Pair<>(selectedLine.getNodeIDs().get(0),
                            selectedLine.getNodeIDs().get(selectedLine.getNodeIDs().size() - 1))))); //TODO make it safe to accidentally select an area node


            selectedNodeCollections = Collections.singletonList(area);
            selected = node;
            touchMode = TouchMode.NEW_NODE;
            return null;
        }, "Create new area on selected line", Input.Keys.A).requiresSelected(Requirement.REQUIRES).clearRequiredSelectedTypes().requiredSelectedTypes(Node.class).build());
        for (int num_key = Input.Keys.NUM_2; (num_key <= Input.Keys.NUM_9) && (num_key < animation.getCountries().size() + 9); num_key++) {
            int finalNum_key = num_key;
            actions.add(Action.createBuilder(() -> {
                        selected = animation.getUnitHandler().newUnit(
                                new Coordinate(mouseX, mouseY),
                                time,
                                animation.getCountries().get(finalNum_key - 9) // Number key enum number to list number
                        );

                        System.out.println(selected);
                        selected.showInputs(uiVisitor);

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
            int selectedIndex = -1;

            for (NodeCollection nodeCollection : selectedNodeCollections) {
                System.out.println(selected);
                selectedIndex = nodeCollection.getNodeIDs().indexOf((NodeID) selected.getId());
            }

            animation.deleteObject(selected);

            if (selectedNodeCollections.size() == 1) {
                if (selectedIndex > 1) {
                    selected = animation.getNodeByID(selectedNodeCollections.get(0).getNodeIDs().get(selectedIndex - 1));
                }
            }

            resetSelected();

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