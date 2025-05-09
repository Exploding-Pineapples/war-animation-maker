package com.badlogicgames.waranimationmaker;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogicgames.waranimationmaker.models.Animation;
import kotlin.Pair;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.badlogic.gdx.Gdx.gl;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH;

public class NewAnimationScreen extends ScreenAdapter implements InputProcessor {
    WarAnimationMaker game;
    public Stage stage;
    public Label imageLabel;
    public Label countriesLabel;
    public MultipleFileButton addCountriesButton;
    public Label warningLabel;
    Table table;
    ArrayList<Actor> elements;

    public NewAnimationScreen(WarAnimationMaker game, Animation animation) {
        this.game = game;
        stage = new Stage();
        table = new Table();
        elements = new ArrayList<>();

        Table titleTable = new Table();
        titleTable.setPosition(DISPLAY_WIDTH / 2f, DISPLAY_HEIGHT - 100);
        Label titleLabel;
        if (FileHandler.INSTANCE.getAnimations().contains(animation)) {
            titleLabel = new Label("Editing " + animation.getName(), game.skin);
        } else {
            titleLabel = new Label("Creating new animation", game.skin);
        }
        titleTable.add(titleLabel);
        stage.addActor(titleTable);

        table.setPosition(DISPLAY_WIDTH / 2f, DISPLAY_HEIGHT / 2f);

        TextButton menuButton = new TextButton("Menu", game.skin, "small");
        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.input.setInputProcessor(game.menu.stage);
                game.setScreen(game.menu); //No need to reinitialize the menu since it's not possible to have added an animation
            }
        });
        menuButton.setPosition(100, 100);
        stage.addActor(menuButton);

        Table nameArea = new Table();
        Label nameLabel = new Label("Name: ", game.skin);
        TextField nameField = new TextField(animation.getName(), game.skin);
        nameArea.add(nameLabel);
        nameArea.add(nameField);
        nameArea.row();
        table.add(nameArea);
        table.row().pad(10);

        imageLabel = new Label("", game.skin);
        table.add(imageLabel).colspan(3);
        table.row();

        File[] countries = new File[animation.getCountries().size()];
        for (int i = 0; i < animation.getCountries().size(); i++) {
            countries[i] = new File(animation.getCountries().get(i));
        }
        addCountriesButton = new MultipleFileButton("Select Countries", game.skin, "small");
        addCountriesButton.files = countries;
        table.add(addCountriesButton.getTextButton()).height(40);
        table.row();

        countriesLabel = new Label("Countries: ", game.skin);
        table.add(countriesLabel);
        table.row();

        TextButton submitButton = new TextButton("Submit", game.skin, "small");
        submitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean animationExists = false;
                String name = nameField.getText();
                List<String> countriesPaths = Arrays.stream(addCountriesButton.files).map(File::getPath).collect(Collectors.toList());
                Pair<Boolean, String> inputCheck = checkInput(nameField.getText(), countriesPaths);
                if (inputCheck.getFirst()) {
                    for (Animation existingAnimation : FileHandler.INSTANCE.getAnimations()) {
                        if (existingAnimation.getName().equals(name)) {
                            System.out.println("Using existing animation: " + name);
                            existingAnimation.setName(nameField.getText());
                            existingAnimation.setCountries(countriesPaths);
                            animationExists = true;

                            AnimationScreen screen = new AnimationScreen(game, existingAnimation);
                            game.setScreen(screen);
                            break;
                        }
                    }
                    if (!animationExists) {
                        System.out.println("Created New Animation");
                        Animation newAnimation = new Animation(nameField.getText(), Arrays.stream(addCountriesButton.files).map(File::getPath).collect(Collectors.toList()));
                        FileHandler.INSTANCE.createNewAnimation(newAnimation);
                        AnimationScreen screen = new AnimationScreen(game, newAnimation);
                        game.setScreen(screen);
                    }
                } else {
                    warningLabel.setText(inputCheck.getSecond());
                }
            }
        });
        table.add(submitButton).height(40);
        table.row().pad(10);

        warningLabel = new Label("", game.skin);
        warningLabel.setColor(Color.RED);
        table.add(warningLabel);

        init();
    }

    public static class MultipleFileButton {
        private File[] files;
        private final TextButton textButton;

        public MultipleFileButton(String text, Skin skin, String styleName) {
            textButton = new TextButton(text, skin, styleName);
            textButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    new Thread(() -> {
                        Desktop desktop = Desktop.getDesktop();
                        try {
                            desktop.open(new File("assets"));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }).start();
                }
            });
        }

        public TextButton getTextButton() {
            return textButton;
        }

        public File[] getFiles() {
            return files;
        }

        public void setFiles(File[] files) {
            this.files = files;
        }
    }

    public Pair<Boolean, String> checkInput(String name, List<String> countriesPaths) {
        if (name.isEmpty()) {
            return new kotlin.Pair<>(false, "Name cannot be empty");
        }
        if (countriesPaths.size() > 8) {
            return new Pair<>(false, "There cannot be more than 8 countries");
        }
        return new Pair<>(true, "");
    }

    public void init() {
        Gdx.input.setInputProcessor(stage);
        stage.addActor(table);
    }
    @Override
    public void render(float delta) {
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        countriesLabel.setText(Arrays.toString(addCountriesButton.getFiles()));

        stage.act();
        stage.draw();
    }
    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
