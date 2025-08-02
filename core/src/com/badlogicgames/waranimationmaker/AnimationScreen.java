package com.badlogicgames.waranimationmaker;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.Array;
import com.badlogicgames.waranimationmaker.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH;
import static java.lang.Math.round;

public class AnimationScreen extends ScreenAdapter implements InputProcessor {
    public static final int DEFAULT_UNIT_WIDTH = 75;
    public static final int DEFAULT_UNIT_HEIGHT = 75;
    public static final double LINE_RESOLUTION = 2.5; // Pixels per straight line
    public static final int MAX_LINES = 5000;

    WarAnimationMaker game; // Contains some variables common to all screens
    OrthographicCamera orthographicCamera; // Camera whose properties directly draw the screen

    float mouseX; // Mouse real X position
    float mouseY; // Mouse real Y position
    Animation animation; // Contains all information about animation loaded from file
    Integer time;

    ArrayList<AnyObject> selectedObjects;

    boolean shiftPressed;
    boolean ctrlPressed;

    boolean paused;
    boolean animationMode;
    boolean UIDisplayed;

    TouchMode touchMode;
    String createClass;
    SelectBoxInput<String> createSelectBoxInput;

    GL20 gl;
    ShapeRenderer shapeRenderer;
    Drawer drawer;

    //UI
    Stage stage;
    Table selectedInfoTable;
    VerticalGroup selectedGroup;
    Table leftPanel;
    VerticalGroup leftGroup;
    Label timeAndFPS;
    Label keyOptions;
    Label selectedLabel;
    List<Action> actions;
    UIVisitor uiVisitor;
    String newUnitCountry;
    SelectBoxInput<String> newUnitCountryInput;
    Integer newNodeCollectionID;
    SelectBoxInput<Integer> newNodeCollectionIDInput;
    boolean newEdgeInputsDisplayed;
    boolean newUnitInputsDisplayed;
    long commaLastUnpressed = 0;
    long periodLastUnpressed = 0;
    long firstTime = System.nanoTime();

    public AnimationScreen(WarAnimationMaker game, Animation animation)  {
        this.animation = animation;
        this.game = game;
        gl = game.gl;

        // Camera
        orthographicCamera = new OrthographicCamera(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        orthographicCamera.position.set(DISPLAY_WIDTH / 2.0f, DISPLAY_HEIGHT / 2.0f, 0);
        animation.camera();
        // Graphics init
        shapeRenderer = game.shapeRenderer;
        drawer = new Drawer(game.bitmapFont, game.fontShader, game.shapeRenderer, game.batcher, animation.getInitTime());

        // Animation init
        time = 0;//animation.getInitTime();
        paused = true;
        animationMode = true;

        //UI
        selectedObjects = new ArrayList<>();

        touchMode = TouchMode.DEFAULT;
        actions = new ArrayList<>();
        buildActions();
        UIDisplayed = false;
        stage = new Stage();

        timeAndFPS = new Label("", game.skin);
        keyOptions = new Label("", game.skin);
        leftGroup = new VerticalGroup();

        newUnitCountryInput = new SelectBoxInput<>(game.skin, (String input) -> {newUnitCountry = input; return null;}, () -> null, String.class, "New Unit Country", Assets.countryNames(), null);
        if (!Assets.countryNames().isEmpty()) {
            newUnitCountry = Assets.countryNames().first();
        } else {
            newUnitCountry = "";
        }
        newUnitInputsDisplayed = false;

        Array<String> createChoices = new Array<>();
        createChoices.addAll("Unit", "Node", "Map Label", "Arrow", "Image");

        createSelectBoxInput = new SelectBoxInput<>(
                game.skin,
                (String input) -> {
                    createClass = input;
                    return null;
                    },
                () -> createClass,
                String.class,
                "Create Mode Input",
                createChoices, null);
        createClass = "Unit";

        newNodeCollectionID = 0;
        Array<Integer> idChoices = new Array<>();
        idChoices.add(animation.getNodeCollectionID());
        for (NodeCollection nodeCollection : animation.getNodeCollections()) {
            idChoices.add(nodeCollection.getId().getValue());
        }
        newNodeCollectionIDInput = new SelectBoxInput<>(game.skin,
                (Integer input) -> { newNodeCollectionID = input; return null; },
                () -> newNodeCollectionID,
                Integer.class,
                "CollectionID of New Edge",
                idChoices,
        null);
        newEdgeInputsDisplayed = false;

        selectedLabel = new Label("", game.skin);
        selectedGroup = new VerticalGroup();
        uiVisitor = new UIVisitor(game.skin);
        selectedInfoTable = new Table();
        stage.addActor(selectedInfoTable);

        leftPanel = new Table();
        stage.addActor(leftPanel);

        game.multiplexer.clear();
        game.multiplexer.addProcessor(stage);
        game.multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(game.multiplexer);

        //game.frameExporter = new FrameExporter(DISPLAY_WIDTH, DISPLAY_HEIGHT, animation.getName());

        // Final init
        animation.camera().goToTime(time);
        updateCam();
        animation.init();
    }

    public static <T> T firstOrNull(ArrayList<T> list) {
        return list.isEmpty()? null : list.get(0);
    }

    public static boolean onScreen(Coordinate coordinate) {
        return (coordinate.getX() >= 0 && coordinate.getY() >= 0) && (coordinate.getX() < DISPLAY_WIDTH) && (coordinate.getY() < DISPLAY_HEIGHT);
    }

    public void updateCam() {
        orthographicCamera.position.x = animation.camera().getPosition().getX();
        orthographicCamera.position.y = animation.camera().getPosition().getY();
        orthographicCamera.zoom = animation.camera().getZoom();
    }

    public boolean keyDown(int keycode) {
        for (Action action : actions) {
            if (action.shouldExecute(keycode, shiftPressed, ctrlPressed, selectedObjects, touchMode)) {
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

    public void updateNewEdgeInputs() { // Makes new edges match ID with first selected node collection
        //TODO make this work for multiple node collections at once
        System.out.println("Updating new edge inputs");
        for (AnyObject selectedObject : selectedObjects) {
            if (selectedObject.getClass() == NodeCollection.class) {
                NodeCollection selectedNodeCollection = (NodeCollection) selectedObject;
                newNodeCollectionID = selectedNodeCollection.getId().getValue();
                newNodeCollectionIDInput.hide(leftGroup);
                newEdgeInputsDisplayed = false;
                break;
            }
        }
    }

    private void clearSelected() {
        for (AnyObject selectedObject : selectedObjects) {
            if (HasInputs.class.isAssignableFrom(selectedObject.getClass())) {
                ((HasInputs) selectedObject).hideInputs(selectedGroup, uiVisitor);
            }
        }
        selectedObjects.clear();
    }

    public <T extends AnyObject> void switchSelected(T newSelection) {
        clearSelected();
        addNewSelection(newSelection);
    }

    public <T extends AnyObject> void switchSelected(ArrayList<T> newSelections) {
        clearSelected();

        for (AnyObject newSelection : newSelections) {
            addNewSelection(newSelection);
        }
    }

    public <T extends AnyObject> void addNewSelection(T newSelection) {
        if (newSelection != null) {
            selectedObjects.add(newSelection);

            if (newSelection.getClass() == Node.class) { // Show new selection's parent's inputs if it has parents
                for (NodeCollection collection : animation.getParents((Node) newSelection)) {
                    if (!selectedObjects.contains(collection)) {
                        selectedObjects.add(collection);

                        if (collection != null) {
                            collection.showInputs(selectedGroup, uiVisitor);
                            selectedObjects.add(collection);
                        } else {
                            System.out.println("Warning: Null node collection");
                        }
                    }
                }
            }
            if (newSelection.getClass() == Edge.class) {
                NodeCollection collection = animation.getNodeCollection(((Edge) newSelection).getCollectionID());
                if (!selectedObjects.contains(collection)) {
                    selectedObjects.add(collection);

                    if (collection != null) {
                        collection.showInputs(selectedGroup, uiVisitor);
                        selectedObjects.add(collection);
                    } else {
                        System.out.println("Warning: Null node collection");
                    }
                }
            }

            if (HasInputs.class.isAssignableFrom(newSelection.getClass())) { // Show new selected object inputs
                ((HasInputs) newSelection).showInputs(selectedGroup, uiVisitor);
            }
            System.out.println("Selected: " + newSelection.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends AnyObject> T selectNewObject(float x, float y, ArrayList<AnyObject> selected, Class<T> type) { // Returns first selected object not already selected
        for (AnyObject selectedObject : animation.selectObjectWithType(x, y, time, type)) {
            if (!selected.contains(selectedObject)) {
                return (T) selectedObject;
            }
        }
        return null;
    }

    public <T extends AnyObject> T selectObject(float x, float y, Class<T> type) {
        return firstOrNull(animation.selectObjectWithType(x, y, time, type));
    }

    public void selectDefault(float x, float y) {
        if (ctrlPressed) {
            addNewSelection(selectNewObject(x, y, selectedObjects, AnyObject.class));
        } else {
            switchSelected(selectNewObject(x, y, selectedObjects, AnyObject.class));
        }
    }

    public boolean touchDown(int x, int y, int pointer, int button) {
        y = DISPLAY_HEIGHT - y;

        System.out.println("Clicked " + mouseX + " " + mouseY + " touch mode " + touchMode);

        if (paused) {
            if (touchMode == TouchMode.DEFAULT) { // Default behavior: select an object to show info about it
                selectDefault(x, y);
            }
            if (touchMode == TouchMode.MOVE) { // Selects an object to move. If a node is selected to be moved into another node, it will be merged
                if (selectedObjects.isEmpty()) {
                    selectDefault(x, y);
                } else {
                    clearSelected();
                }

                moveObjects(selectedObjects);
            }

            if (touchMode == TouchMode.CREATE) {
                switchSelected(animation.createObjectAtPosition(time, mouseX, mouseY, createClass, Assets.flagsPath(newUnitCountry)));
            }

            if (touchMode == TouchMode.NEW_EDGE) {
                Node newSelection = selectNewObject(x, y, selectedObjects, Node.class);
                if (newSelection != null) {
                    for (AnyObject selectedObject : selectedObjects) { // Add edge from already selected Nodes to new selected node
                        if (selectedObject.getClass() == Node.class) {
                            System.out.println("trying to select for new edge");

                            Node currentSelection = (Node) selectedObject;
                            animation.getNodeEdgeHandler().addEdge(currentSelection, newSelection, newNodeCollectionID);
                            System.out.println("Added an edge. Edges: " + currentSelection.getEdges());
                        }
                    }
                }
                if (selectedObjects.isEmpty()) { // Only change new edge collection if nothing was selected
                    switchSelected(newSelection);
                    updateNewEdgeInputs();
                } else {
                    switchSelected(newSelection);
                }
            }
        }
        return true;
    }

    private void moveObjects(ArrayList<AnyObject> objects) {
        for (AnyObject object : objects) {
            if (InterpolatedObject.class.isAssignableFrom(object.getClass())) {
                ((InterpolatedObject) object).newSetPoint(time, mouseX, mouseY);
            }
            if (object.getClass() == Node.class) {
                ((Node) object).setPosition(new Coordinate(mouseX, mouseY));
                for (NodeCollection parent : animation.getParents((Node) object)) {
                    parent.getInterpolator().updateInterpolationFunction();
                }
            }
        }
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
            orthographicCamera.zoom *= 1 - 0.05f * amountY;
        }
        return true;
    }

    private void updateUI() {
        if (animationMode) {
            if (paused) { // Update the selected object to go to mouse in move mode
                if ((touchMode == TouchMode.MOVE)) {
                    moveObjects(selectedObjects);
                }
            }
            // Set information about keyboard options and current animator state
            timeAndFPS.setText(Gdx.graphics.getFramesPerSecond() + " FPS \n" + "Time: " + time);

            StringBuilder options = new StringBuilder();
            StringJoiner optionsJoiner = new StringJoiner("\n");
            options.append("Touch mode: ").append(touchMode.name()).append("\n");
            options.append("Control pressed: ").append(ctrlPressed).append(" ").append("Shift pressed: ").append(shiftPressed).append("\n");
            for (Action action : actions) {
                if (action.couldExecute(shiftPressed, ctrlPressed, selectedObjects, touchMode)) {
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

            if (selectedObjects.isEmpty()) {
                selectedInfo.append("Nothing is selected").append("\n");
            } else {
                for (AnyObject selectedObject : selectedObjects) {
                    selectedInfo.append("Selected: ").append(selectedObject.getClass().getSimpleName()).append("\n");
                    if (selectedObject.getClass().isAssignableFrom(HasPosition.class)) {
                        selectedInfo.append("x: ").append(((HasPosition) selectedObject).getPosition().getX()).append("\n");
                        selectedInfo.append("y: ").append(((HasPosition) selectedObject).getPosition().getY()).append("\n");
                    }
                    if (selectedObject.getClass().isAssignableFrom(HasID.class)) {
                        selectedInfo.append("ID: ").append(((HasID) selectedObjects).getId().getValue()).append("\n");
                    }
                    if (selectedObject.getClass() == Node.class) {
                        ArrayList<Integer> toNodes = new ArrayList<>();
                        Node node = (Node) selectedObject;
                        for (Edge edge : node.getEdges()) {
                            toNodes.add(edge.getSegment().getSecond().getValue());
                        }
                        for (NodeCollection parent : animation.getParents((node))) {
                            // Get what parameter value the node is at within its node collection set points.
                            NodeCollectionSetPoint setPoint = parent.getInterpolator().getSetPoints().get(time);
                            if (setPoint != null) {
                                selectedInfo.append("T on Node Collection").append(parent.getId().getValue()).append(": ")
                                        .append(Math.round(setPoint.tOfNode(node) * 10000) / 10000.0).append("\n");
                            }
                        }
                        selectedInfo.append("Edges: ").append(toNodes).append("\n");
                    }
                    if (selectedObject.getClass() == NodeCollection.class) {
                        selectedInfo.append("NodeCollectionID: ").append(((NodeCollection) selectedObject).getId().getValue()).append("\n");
                    }
                }
            }

            selectedLabel.setText(selectedInfo);

            if (!UIDisplayed) {
                leftPanel.add(timeAndFPS).left().pad(10);
                leftPanel.row();
                leftPanel.add(keyOptions).pad(10);
                leftPanel.row();
                leftPanel.add(leftGroup);

                selectedInfoTable.add(selectedLabel).expandX().pad(10).left();
                selectedInfoTable.row().pad(10);
                for (AnyObject selectedObject : selectedObjects) {
                    if (HasInputs.class.isAssignableFrom(selectedObject.getClass())) {
                        ((HasInputs) selectedObject).showInputs(selectedGroup, uiVisitor);
                    }
                }
                selectedInfoTable.add(selectedGroup);

                UIDisplayed = true;
            }

            if (touchMode == TouchMode.NEW_EDGE) {
                Array<Integer> idChoices = new Array<>();
                idChoices.add(animation.getNodeCollectionID());
                for (NodeCollection nodeCollection : animation.getNodeCollections()) {
                    idChoices.add(nodeCollection.getId().getValue());
                }
                newNodeCollectionIDInput.getChoices().clear();
                newNodeCollectionIDInput.getChoices().addAll(idChoices);
                if (!newEdgeInputsDisplayed) {
                    newNodeCollectionIDInput.show(leftGroup, game.skin);
                    newEdgeInputsDisplayed = true;
                }
            } else {
                if (newEdgeInputsDisplayed) {
                    newNodeCollectionIDInput.hide(leftGroup);
                    newEdgeInputsDisplayed = false;
                }
            }

            if (touchMode == TouchMode.CREATE) {
                if (createClass.equals("Unit")) {
                    if (!newUnitInputsDisplayed) {
                        newUnitCountryInput.show(leftGroup, game.skin);
                        newUnitInputsDisplayed = true;
                    }
                } else {
                    if (newUnitInputsDisplayed) {
                        newUnitCountryInput.hide(leftGroup);
                        newUnitInputsDisplayed = false;
                    }
                }
                if (!createSelectBoxInput.getDisplayed()) {
                    createSelectBoxInput.show(leftGroup, game.skin);
                    createSelectBoxInput.setDisplayed(true);
                }
            } else {
                if (createSelectBoxInput.getDisplayed()) {
                    createSelectBoxInput.hide(leftGroup);
                    createSelectBoxInput.setDisplayed(false);
                }
                if (newUnitInputsDisplayed) {
                    newUnitCountryInput.hide(leftGroup);
                    newUnitInputsDisplayed = false;
                }
            }

            leftPanel.pack();
            leftPanel.setPosition(30, DISPLAY_HEIGHT - 30 - leftPanel.getHeight());

            selectedInfoTable.pack();
            selectedGroup.pack();
            selectedInfoTable.setPosition(DISPLAY_WIDTH - 30 - selectedInfoTable.getWidth(), DISPLAY_HEIGHT  - 30 - selectedInfoTable.getHeight());
        } else {
            if (UIDisplayed) {
                for (AnyObject selectedObject : selectedObjects) {
                    if (HasInputs.class.isAssignableFrom(selectedObject.getClass())) {
                        ((HasInputs) selectedObject).hideInputs(selectedGroup, uiVisitor);
                    }
                }
                leftPanel.clear();
                selectedInfoTable.clear();
                UIDisplayed = false;
            }
        }
    }

    private void update() {
        System.out.println("Time since last frame: " + (System.nanoTime() - firstTime));
        firstTime = System.nanoTime();
        mouseX = (float) ((double) Gdx.input.getX() - orthographicCamera.position.x * (1 - orthographicCamera.zoom) - (DISPLAY_WIDTH / 2f - orthographicCamera.position.x)) / orthographicCamera.zoom;
        mouseY = (float) ((double) (DISPLAY_HEIGHT - Gdx.input.getY()) - orthographicCamera.position.y * (1 - orthographicCamera.zoom) - (DISPLAY_HEIGHT / 2f - orthographicCamera.position.y)) / orthographicCamera.zoom;
        ctrlPressed = (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT));
        shiftPressed = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));

        //Camera
        animation.camera().goToTime(time);
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            orthographicCamera.position.y += 10 / orthographicCamera.zoom;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            orthographicCamera.position.y -= 10 / orthographicCamera.zoom;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            orthographicCamera.position.x -= 10 / orthographicCamera.zoom;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            orthographicCamera.position.x += 10 / orthographicCamera.zoom;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.COMMA)) {
            if (System.currentTimeMillis() - commaLastUnpressed >= 250) {
                updateTime(time - 1);
            }
        } else {
            commaLastUnpressed = System.currentTimeMillis();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.PERIOD)) {
            if (System.currentTimeMillis() - periodLastUnpressed >= 250) {
                updateTime(time + 1);
            }
        } else {
            periodLastUnpressed = System.currentTimeMillis();
        }

        if (!paused) { //don't update camera when paused to allow for movement when paused
            updateCam();
        }
        orthographicCamera.update();
        drawer.update(orthographicCamera, time, animationMode);
        animation.update(time, orthographicCamera, paused);

        //UI
        updateUI();

        stage.act();
        System.out.println("update done: " + (System.nanoTime() - firstTime));
    }

    @Override
    public void render(float delta) {
        update();

        animation.draw(drawer);

        if (animationMode) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // Draw contrast backgrounds for UI
            shapeRenderer.setColor(new Color(0, 0, 0, 0.5f));

            shapeRenderer.rect(leftPanel.getX(), leftPanel.getY(), leftPanel.getWidth(), leftPanel.getHeight());
            shapeRenderer.rect(selectedInfoTable.getX(), selectedInfoTable.getY(), selectedInfoTable.getWidth(), selectedInfoTable.getHeight());

            //Draw the selected objects
            for (AnyObject selectedObject : selectedObjects) {
                drawer.drawAsSelected(selectedObject);
            }

            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        stage.draw();

        //game.frameExporter.captureFrame(time); // uncomment to export (temp)

        if (!paused) { //now that both update and draw are done, advance the time
            time++;
        }
        System.out.println("draw done: " + (System.nanoTime() - firstTime));
    }

    @Override
    public void pause() {
        FileHandler.INSTANCE.save();
    }

    private void updateTime(int newTime) {
        time = newTime;
        animation.update(time, orthographicCamera, false);
        animation.camera().goToTime(time);
        updateCam();
    }

    @SuppressWarnings({"DataFlowIssue"}) // Null return required for Kotlin Unit lambda
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
            for (AnyObject selectedObject : selectedObjects) {
                if (HasAlpha.class.isAssignableFrom(selectedObject.getClass())) {
                    ((HasAlpha) selectedObject).getAlpha().newSetPoint(time, ((HasAlpha) selectedObject).getAlpha().getValue());
                    System.out.println("Set a new alpha set point, set points: " + ((HasAlpha) selectedObject).getAlpha().getSetPoints());
                }
            }
            return null;
        }, "Set alpha set point", Input.Keys.A).build());
        actions.add(Action.createBuilder(() -> {
            animationMode = !animationMode;
            return null;
        }, "Toggle animation mode", Input.Keys.V).build());
        // Selection required
        actions.add(Action.createBuilder(() -> {
            for (AnyObject selectedObject : selectedObjects) {
                if (InterpolatedObject.class.isAssignableFrom(selectedObject.getClass())) {
                    ((InterpolatedObject) selectedObject).holdPositionUntil(time);
                }
                if (selectedObject.getClass() == NodeCollection.class) {
                    ((NodeCollection) selectedObject).getInterpolator().holdValueUntil(time, animation);
                }
            }
            clearSelected();
            return null;
        }, "Hold last defined position to this time", Input.Keys.H).requiresSelected(Requirement.REQUIRES).build());
        // Does not care about selection
        actions.add(Action.createBuilder(() -> {
            updateTime((time / 200) * 200 + 200);
            return null;
        }, "Step time forward 200", Input.Keys.E).build());
        actions.add(Action.createBuilder(() -> {
            updateTime((int) Math.ceil(time / 200.0) * 200 - 200);
            return null;
        }, "Step time back 200", Input.Keys.Q).build());
        actions.add(Action.createBuilder(() -> {
            updateTime(time + 1);
            return null;
        }, "Step time forward 1", Input.Keys.PERIOD).build());
        actions.add(Action.createBuilder(() -> {
            updateTime(time - 1);
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
            if (touchMode != TouchMode.CREATE) {
                touchMode = TouchMode.CREATE;
            } else {
                touchMode = TouchMode.DEFAULT;
            }
            return null;
            }, "Create Object Mode", Input.Keys.C
        ).build());
        actions.add(Action.createBuilder(() -> {
            ArrayList<AnyObject> selectedObjectsCopy = new ArrayList<>(selectedObjects);
            for (AnyObject selectedObject : selectedObjectsCopy) {
                if (selectedObject.getClass() == Node.class) {
                    Node newNode = animation.newNode(mouseX, mouseY, time);
                    animation.getNodeEdgeHandler().insert((Node) selectedObject, newNode);
                    switchSelected(newNode);
                }
            }
            return null;
            }, "Insert", Input.Keys.I
        ).build());
        //Key presses which require control pressed
        actions.add(Action.createBuilder(() -> {
            switchSelected(animation.camera());
            return null;
        }, "Select the camera", Input.Keys.C).requiresControl(true).build());
        actions.add(Action.createBuilder(() -> {
            if (touchMode == TouchMode.NEW_EDGE) {
                touchMode = TouchMode.DEFAULT;
            } else {
                touchMode = TouchMode.NEW_EDGE;
                updateNewEdgeInputs();
            }
            return null;
        }, "Switch to New Edge Mode", Input.Keys.E).requiresControl(true).build());
        actions.add(Action.createBuilder(() -> {
            FileHandler.INSTANCE.save();
            System.out.println("saved");
            return null;
        }, "Save project", Input.Keys.S).requiresControl(true).build());
        actions.add(Action.createBuilder(() -> {
            animation.camera().getZoomInterpolator().newSetPoint(time, orthographicCamera.zoom);
            return null;
        }, "Set a camera zoom set point", Input.Keys.Z).requiresControl(true).build());
        // Key presses which require selected Object
        actions.add(Action.createBuilder(() -> {
            clearSelected();
            System.out.println("Deselected object");
            return null;
        }, "Deselect Object", Input.Keys.D).description("Deselects object").requiresSelected(Requirement.REQUIRES).build());
        actions.add(Action.createBuilder(() -> {
            for (AnyObject selectedObject : selectedObjects) {
                animation.deleteObject(selectedObject);
            }
            System.out.println("Deleted object");
            clearSelected();
            return null;
        }, "Delete selected unit", Input.Keys.FORWARD_DEL).requiresSelected(Requirement.REQUIRES).build());
        actions.add(Action.createBuilder(() -> {
            for (AnyObject selectedObject : selectedObjects) {
                if (InterpolatedObject.class.isAssignableFrom(selectedObject.getClass())) {
                    if (((InterpolatedObject) selectedObject).removeFrame(time)) {
                        System.out.println("Deleted last frame");
                    } else {
                        System.out.println("Cannot delete frame on object with less than 2 frames");
                    }
                }
            }
            clearSelected();
            touchMode = TouchMode.DEFAULT;
            return null;
        }, "Delete last frame of selected object", Input.Keys.ESCAPE, Input.Keys.DEL).build());
    }
}