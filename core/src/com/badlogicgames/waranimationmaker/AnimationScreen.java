package com.badlogicgames.waranimationmaker;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.Array;
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedBoolean;
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedID;
import com.badlogicgames.waranimationmaker.models.*;
import kotlin.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static com.badlogicgames.waranimationmaker.Assets.loadTexture;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH;
import static com.badlogicgames.waranimationmaker.models.ModelsKt.projectToScreen;
import static java.lang.Math.round;

public class AnimationScreen extends ScreenAdapter implements InputProcessor {
    public static final int DEFAULT_UNIT_WIDTH = 10;
    public static final int DEFAULT_UNIT_HEIGHT = 10;
    public static final int MIN_LINE_SIZE = 1; // Minimum number of edges needed to draw a line
    public static final int LINES_PER_NODE = 12; // Number of straight lines per node on a spline

    WarAnimationMaker game; // Contains some variables common to all screens
    OrthographicCamera orthographicCamera; // Camera whose properties directly draw the screen

    float mouseX; // Mouse real X position
    float mouseY; // Mouse real Y position
    float zoomFactor; // Scales everything other than the map less than actual zoom
    Animation animation; // Contains all information about animation loaded from file
    Integer time;

    ScreenObject selected;
    List<EdgeCollection> selectedEdgeCollections; // if a Node is selected, this will be the Area or Line that the Node is on
    List<Edge> selectedEdges;

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
    Integer newEdgeCollectionID;
    SelectBoxInput<Integer> newEdgeCollectionIDInput;
    boolean newEdgeInputsDisplayed;
    boolean newUnitInputsDisplayed;

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
        fullMap = loadTexture(animation.getMapPath());
        colorLayer = new FrameBuffer(Pixmap.Format.RGBA8888, 1024, 720, false);

        // Animation init
        time = 0;
        paused = true;
        animationMode = true;

        //UI
        selectedEdgeCollections = new ArrayList<>();
        selectedEdges = new ArrayList<>();
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

        newEdgeCollectionID = 0;
        Array<Integer> idChoices = new Array<>();
        idChoices.add(animation.getEdgeCollectionId(true));
        for (EdgeCollection edgeCollection : animation.getEdgeCollections()) {
            idChoices.add(edgeCollection.getId().getValue());
        }
        newEdgeCollectionIDInput = new SelectBoxInput<>(game.skin,
                (Integer input) -> { newEdgeCollectionID = input; return null; },
                () -> newEdgeCollectionID,
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

        // Final init
        animation.camera().goToTime(time);
        updateCam();
        animation.load();
    }

    public static <T> T firstOrNull(ArrayList<T> list) {
        return list.isEmpty()? null : list.get(0);
    }

    public void updateCam() {
        orthographicCamera.position.x = animation.camera().getPosition().getX();
        orthographicCamera.position.y = animation.camera().getPosition().getY();
        orthographicCamera.zoom = animation.camera().getZoom();
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

    public void updateNewEdgeInputs() {
        if (!selectedEdgeCollections.isEmpty()) { // Update the inputs to match the new edge type
            System.out.println("Updating new edge inputs");
            newEdgeCollectionID = selectedEdgeCollections.get(0).getId().getValue();
            newEdgeCollectionIDInput.hide(leftGroup);
            newEdgeInputsDisplayed = false;
        }
    }

    private void clearSelected() {
        if (selected != null) {
            selected.hideInputs(selectedGroup, uiVisitor);
        }
        for (EdgeCollection collection : selectedEdgeCollections) {
            collection.hideInputs(selectedGroup, uiVisitor);
        }
        selected = null;
        selectedEdges.clear();
        selectedEdgeCollections.clear();
    }

    public <T extends ScreenObject> void switchSelected(T newSelection) {
        clearSelected(); // Hide previous selected object

        selected = newSelection;

        // Show new selected object
        if (selected != null) {
            System.out.println("Selected: " + selected.getClass().getSimpleName() + " " + selected.getId().getValue());
            selectedEdgeCollections = animation.getParents(selected.getId());
            selected.showInputs(selectedGroup, uiVisitor);
            for (EdgeCollection collection : selectedEdgeCollections) {
                collection.showInputs(selectedGroup, uiVisitor);
            }
        }
    }

    public void switchSelected(ArrayList<Edge> newSelections) {
        clearSelected();

        selectedEdges = newSelections;

        if (!newSelections.isEmpty()) {
            for (Edge newSelection : newSelections) {
                if (newSelection.shouldDraw(time)) {
                    System.out.println("new edge selection id: " + newSelection.getCollectionID().getValue());
                    EdgeCollection edgeCollection = animation.getEdgeCollectionByID(new EdgeCollectionID(newSelection.getCollectionID().getValue()));
                    if (edgeCollection != null) {
                        System.out.println("selected edge collection " + edgeCollection.getId().getValue());
                        selectedEdgeCollections.add(edgeCollection);
                        edgeCollection.showInputs(selectedGroup, uiVisitor);
                    }
                }
            }
        }
    }

    public <T extends ObjectClickable> ArrayList<T> selectNewScreenObject(float x, float y, T selected, Class<T> type) {
        ArrayList<T> selectedThings = new ArrayList<>();
        selectedThings.add(selected);
        return selectNewScreenObject(x, y, selectedThings, type);
    }

    public <T extends ObjectClickable> ArrayList<T> selectNewScreenObject(float x, float y, ArrayList<T> selected, Class<T> type) {
        ArrayList<T> selectedThings = animation.selectObjectWithType(x, y, type);
        selectedThings.removeIf(selected::contains);
        return selectedThings;
    }

    public <T extends ObjectClickable> T selectScreenObject(float x, float y, Class<T> type) {
        return firstOrNull(animation.selectObjectWithType(x, y, type));
    }

    public boolean touchDown(int x, int y, int pointer, int button) {
        y = DISPLAY_HEIGHT - y;

        System.out.println("Clicked " + mouseX + " " + mouseY + " touch mode " + touchMode);

        if (paused) {
            if (touchMode == TouchMode.DEFAULT) { // Default behavior: select an object to show info about it
                ArrayList<ScreenObject> selectedThings = animation.selectObjectWithType(x, y, ScreenObject.class);
                if (!selectedThings.isEmpty()) {
                    switchSelected(selectedThings.get(0));
                } else {
                    switchSelected(animation.selectObjectWithType(x, y, Edge.class));
                }
            }
            if (touchMode == TouchMode.MOVE) { // Selects an object to move. If a node is selected to be moved into another node, it will be merged
                if (selected != null) {
                    if (selected.getClass() == Node.class) {
                        ArrayList<Node> selectedNodes = selectNewScreenObject(x, y, (Node) selected, Node.class);
                        if (!selectedNodes.isEmpty()) {
                            //If the current selection is a Node and the user clicks another Node, merge the 2 nodes by setting the selected to the same point and setting its death
                            Node newSelection = selectedNodes.get(0);
                            selected.newSetPoint(time, newSelection.getPosition().getX(), newSelection.getPosition().getY());
                            clearSelected();
                            return true;
                        }
                    }
                    selected.newSetPoint(time, mouseX, mouseY);
                    clearSelected();
                    return true;
                }
                switchSelected(selectScreenObject(x, y, ScreenObject.class));
            }

            if (touchMode == TouchMode.NEW_NODE) { // Will create a new Node. If a node is selected and has exactly 1 edge pointing away from it, the new node will be inserted between the selected node and the next node
                Node newNode = animation.createNodeAtPosition(time, mouseX, mouseY);
                if ((selected != null) && (selected.getClass() == Node.class)) {
                    if (((Node) selected).getEdges().size() == 1) {
                        Edge existingEdge = ((Node) selected).getEdges().get(0);
                        newNode.getEdges().add(new Edge(existingEdge.getCollectionID(), new Pair<>(newNode.getId(), existingEdge.getSegment().getSecond()), new ArrayList<>(), new InterpolatedBoolean(false, time))); // Create an edge pointing from the new node to the next node
                        existingEdge.setSegment(new Pair<>((NodeID) selected.getId(), newNode.getId())); // Change the edge of the selected node to point to the new node
                    }
                }
                switchSelected(newNode);
                return true;
            }

            if (touchMode == TouchMode.NEW_EDGE) {
                if (selected == null || selected.getClass() != Node.class) {
                    switchSelected((Node) firstOrNull(animation.selectObjectWithType(x, y, Node.class)));
                    updateNewEdgeInputs();
                } else {
                    Node newSelection = firstOrNull(selectNewScreenObject(x, y, (Node) selected, Node.class));
                    if (newSelection != null) {
                        ((Node) selected).getEdges().add(new Edge(new InterpolatedID(time, newEdgeCollectionID), new Pair<>(((Node) selected).getId(), newSelection.getId()), new ArrayList<>(), new InterpolatedBoolean(false, time)));
                        System.out.println("Added an edge. Edges: " + ((Node) selected).getEdges());
                    }
                    switchSelected(newSelection);
                }
            }

            if (touchMode == TouchMode.NEW_UNIT) {
                switchSelected(animation.getUnitHandler().newUnit(
                        new Coordinate(mouseX, mouseY),
                        time,
                        Assets.flagsPath(newUnitCountry)
                ));

                System.out.println(selected);
                selected.showInputs(selectedGroup, uiVisitor);
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
            orthographicCamera.zoom *= 1 - 0.05f * amountY;
        }
        return true;
    }

    private void update() {
        mouseX = (float) ((double) Gdx.input.getX() - orthographicCamera.position.x * (1 - orthographicCamera.zoom) - (Gdx.graphics.getWidth() / 2.0f - orthographicCamera.position.x)) / orthographicCamera.zoom;
        mouseY = (float) ((double) (DISPLAY_HEIGHT - Gdx.input.getY()) - orthographicCamera.position.y * (1 - orthographicCamera.zoom) - (Gdx.graphics.getHeight() / 2.0f - orthographicCamera.position.y)) / orthographicCamera.zoom;
        ctrlPressed = (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT));
        shiftPressed = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));

        //Camera
        zoomFactor = 0.75f + orthographicCamera.zoom * 8;
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
        if (!paused) { //don't update camera when paused to allow for movement when paused
            updateCam();
        }
        orthographicCamera.update();

        if ((selected != null) && paused) { // Update the selected object to go to mouse in move mode
            if ((touchMode == TouchMode.MOVE)) {
                selected.newSetPoint(time, mouseX, mouseY);
            }
        }

        animation.update(time, orthographicCamera);

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

            if (!selectedEdges.isEmpty()) {
                selectedInfo.append("Selected Edges: ").append(selectedEdges).append("\n");
            }
            if (selected == null) {
                selectedInfo.append("No screen object is selected");
            } else {
                selectedInfo.append("Selected: ").append(selected.getClass().getSimpleName()).append("\n");
                selectedInfo.append("x: ").append(selected.getPosition().getX()).append("\n");
                selectedInfo.append("y: ").append(selected.getPosition().getY()).append("\n");
                selectedInfo.append("ID: ").append(selected.getId().getValue()).append("\n");

                if (selected.getClass() == Node.class) {
                    ArrayList<Integer> nodes = new ArrayList<>();
                    for (Edge edge : ((Node) selected).getEdges()) {
                        nodes.add(edge.getSegment().getSecond().getValue());
                    }
                    selectedInfo.append("Edges: ").append(nodes).append("\n");
                }
                for (EdgeCollection edgeCollection : selectedEdgeCollections) {
                    selectedInfo.append("Selected ").append(edgeCollection.getClass().getSimpleName()).append(": \n");
                    selectedInfo.append("# Edges: ").append(edgeCollection.getEdges().size()).append("\n");
                    selectedInfo.append("EdgeCollectionID: ").append(edgeCollection.getId().getValue()).append("\n");
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
                if (selected != null) {
                    selected.showInputs(selectedGroup, uiVisitor);
                }
                selectedInfoTable.add(selectedGroup);

                UIDisplayed = true;
            }

            if (touchMode == TouchMode.NEW_EDGE) {
                Array<Integer> idChoices = new Array<>();
                idChoices.add(animation.getEdgeCollectionId(true) + 1);
                for (EdgeCollection edgeCollection : animation.getEdgeCollections()) {
                    idChoices.add(edgeCollection.getId().getValue());
                }
                newEdgeCollectionIDInput.getChoices().clear();
                newEdgeCollectionIDInput.getChoices().addAll(idChoices);
                if (!newEdgeInputsDisplayed) {
                    newEdgeCollectionIDInput.show(leftGroup, game.skin);
                    newEdgeInputsDisplayed = true;
                }
            } else {
                if (newEdgeInputsDisplayed) {
                    newEdgeCollectionIDInput.hide(leftGroup);
                    newEdgeInputsDisplayed = false;
                }
            }

            if (touchMode == TouchMode.NEW_UNIT) {
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

            leftPanel.pack();
            leftPanel.setPosition(30, DISPLAY_HEIGHT - 30 - leftPanel.getHeight());

            selectedInfoTable.pack();
            selectedGroup.pack();
            selectedInfoTable.setPosition(DISPLAY_WIDTH - 30 - selectedInfoTable.getWidth(), DISPLAY_HEIGHT  - 30 - selectedInfoTable.getHeight());
        } else {
            if (UIDisplayed) {
                if (selected != null) {
                    selected.hideInputs(selectedGroup, uiVisitor);
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
        Coordinate origin = projectToScreen(new Coordinate(0, 0), orthographicCamera.zoom, orthographicCamera.position.x, orthographicCamera.position.y);
        game.batcher.draw(fullMap, origin.getX(), origin.getY(), fullMap.getWidth() * orthographicCamera.zoom, fullMap.getHeight() * orthographicCamera.zoom);
        game.batcher.end();

        // Draw Units and Lines
        animation.draw(game, colorLayer, animationMode, zoomFactor, time, orthographicCamera);

        game.batcher.setColor(1, 1, 1, 1.0f);

        if (animationMode) {
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // Draw contrast backgrounds for UI
            shapeRenderer.setColor(new Color(0, 0, 0, 0.5f));

            shapeRenderer.rect(leftPanel.getX(), leftPanel.getY(), leftPanel.getWidth(), leftPanel.getHeight());
            shapeRenderer.rect(selectedInfoTable.getX(), selectedInfoTable.getY(), selectedInfoTable.getWidth(), selectedInfoTable.getHeight());

            //Draw the selected object and edges
            if (selected != null) {
                selected.drawAsSelected(shapeRenderer, animationMode, animation.camera());
            }
            for (Edge edge : selectedEdges) {
                edge.drawAsSelected(shapeRenderer, animationMode);
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
            switchSelected(animation.newArrow(mouseX, mouseY, time));
            return null;
        }, "Create a new arrow", Input.Keys.A).requiresSelected(Requirement.ANY).build());
        actions.add(Action.createBuilder(() -> {
            animationMode = !animationMode;
            return null;
        }, "Toggle animation mode", Input.Keys.V).requiresSelected(Requirement.ANY).build());
        // Selection required
        actions.add(Action.createBuilder(() -> {
            selected.holdPositionUntil(time);
            clearSelected();
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
                clearSelected();
                System.out.println("Default Mode");
            }
            return null;
        }, "New Node Mode", Input.Keys.NUM_1).requiresSelected(Requirement.ANY).build());
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
            if (touchMode != TouchMode.NEW_UNIT) {
                touchMode = TouchMode.NEW_UNIT;
            } else {
                touchMode = TouchMode.DEFAULT;
            }
            return null;
            }, "Create Unit", Input.Keys.U
        ).build());
        //Key presses which require control pressed
        actions.add(Action.createBuilder(() -> {
            selected = animation.camera();
            return null;
        }, "Select the camera", Input.Keys.C).requiresControl(true).build());
        actions.add(Action.createBuilder(() -> {
            animation.camera().getZoomInterpolator().newSetPoint(time, orthographicCamera.zoom);
            return null;
        }, "Set a camera zoom set point", Input.Keys.Z).requiresControl(true).build());
        actions.add(Action.createBuilder(() -> {
            FileHandler.INSTANCE.save();
            System.out.println("saved");
            return null;
        }, "Save project", Input.Keys.S).requiresControl(true).build());
        //Key presses which require control pressed and selected Object
        actions.add(Action.createBuilder(() -> {
            if (selected != null) {
                selected.getDeath().newSetPoint(time, !selected.getDeath().getValue());
                System.out.println("Set death of " + selected);
            }
            for (Edge edge : selectedEdges) {
                edge.getDeath().newSetPoint(time, !edge.getDeath().getValue());
            }
            clearSelected();
            return null;
        }, "Set death of an object", Input.Keys.D).requiresControl(true).build());
        // Key presses which require selected Object
        actions.add(Action.createBuilder(() -> {
            clearSelected();
            System.out.println("Deselected object");
            return null;
        }, "Deselect Object", Input.Keys.D).description("Deselects object").requiresSelected(Requirement.REQUIRES).build());
        actions.add(Action.createBuilder(() -> {
            if (selected != null) {
                animation.deleteObject(selected);
            }
            for (Edge edge : selectedEdges) {
                animation.getNodeHandler().removeEdge(edge);
            }
            System.out.println("Deleted object");
            clearSelected();
            return null;
        }, "Delete selected unit", Input.Keys.FORWARD_DEL).clearRequiredSelectedTypes().requiredSelectedTypes(Unit.class).build());
        actions.add(Action.createBuilder(() -> {
            animation.deleteObject(selected);

            clearSelected();

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
            clearSelected();
            touchMode = TouchMode.DEFAULT;
            return null;
        }, "Delete last frame of selected object", Input.Keys.ESCAPE, Input.Keys.DEL).requiresSelected(Requirement.REQUIRES).build());
    }
}