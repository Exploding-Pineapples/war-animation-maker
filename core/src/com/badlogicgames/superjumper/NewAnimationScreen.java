package com.badlogicgames.superjumper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogicgames.superjumper.models.Animation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.badlogic.gdx.Gdx.gl;
import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_WIDTH;
import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_HEIGHT;

public class NewAnimationScreen extends ScreenAdapter implements InputProcessor {
    WarAnimationMaker game;
    public Stage stage;
    public Label imageLabel;
    public Label countriesLabel;
    public FileButton selectImageButton;
    public MultipleFileButton addCountriesButton;
    Table table;
    ArrayList<String> countries;
    ArrayList<Actor> elements;

    public NewAnimationScreen(WarAnimationMaker game, Animation animation) {
        this.game = game;
        stage = new Stage();
        table = new Table();
        elements = new ArrayList<>();

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

        selectImageButton = new FileButton("Select Background Image", game.skin, "small");
        selectImageButton.setPath(new File(animation.getPath()));
        table.add(selectImageButton.getTextButton()).height(40);
        table.row();

        imageLabel = new Label("", game.skin);
        table.add(imageLabel).colspan(3);
        table.row();

        File[] countries = new File[animation.getCountries().size()];
        for (int i = 0; i < animation.getCountries().size(); i++) {
            countries[i] = new File(animation.getCountries().get(i));
        }
        addCountriesButton = new MultipleFileButton("Select Countries", game.skin, "small");
        addCountriesButton.paths = countries;
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
                for (Animation existingAnimation : FileHandler.INSTANCE.getAnimations()) {
                    if (existingAnimation.getName().equals(animation.getName())) {
                        existingAnimation.setPath(selectImageButton.getPath().getPath());
                        existingAnimation.setName(nameField.getText());
                        existingAnimation.setCountries(Arrays.stream(addCountriesButton.paths).map(File::getPath).collect(Collectors.toList()));
                        animationExists = true;
                        break;
                    }
                }
                if (!animationExists) {
                    Animation newAnimation = new Animation(selectImageButton.getPath().getPath(), nameField.getText(), Arrays.stream(addCountriesButton.paths).map(File::getPath).collect(Collectors.toList()));
                    FileHandler.INSTANCE.createNewAnimation(newAnimation);
                    Screen screen = new Screen(game, newAnimation);
                    game.setScreen(screen);
                    Gdx.input.setInputProcessor(screen);
                }
            }
        });
        table.add(submitButton).height(40);

        init();
    }

    public static class MultipleFileButton {
        private File[] paths;
        private final TextButton textButton;

        public MultipleFileButton(String text, Skin skin, String styleName) {
            textButton = new TextButton(text, skin, styleName);
            textButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    new Thread(() -> {
                        JFileChooser chooser = new JFileChooser();
                        chooser.setCurrentDirectory(new File("assets"));
                        chooser.setMultiSelectionEnabled(true);
                        JFrame f = new JFrame();
                        f.setVisible(true);
                        f.toFront();
                        f.setVisible(false);
                        int res = chooser.showOpenDialog(f);
                        f.dispose();
                        if (res == JFileChooser.APPROVE_OPTION) {
                            File[] files = chooser.getSelectedFiles();
                            for (int i = 0; i < files.length; i++) {
                                files[i] = new File(new File("C:\\Users\\User\\Documents\\Projects\\war-animation-maker").toURI().relativize(files[i].toURI()).getPath());
                            }
                            paths = files;
                        }
                    }).start();
                }
            });
        }

        public TextButton getTextButton() {
            return textButton;
        }

        public File[] getPaths() {
            return paths;
        }

        public void setPaths(File[] paths) {
            this.paths = paths;
        }
    }


    public static class FileButton {
        private File path;
        private final TextButton textButton;

        public FileButton(String text, Skin skin, String styleName) {
            textButton = new TextButton(text, skin, styleName);
            textButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    new Thread(() -> {
                        JFileChooser chooser = new JFileChooser(new File("assets"));
                        JFrame f = new JFrame();
                        f.setVisible(true);
                        f.toFront();
                        f.setVisible(false);
                        int res = chooser.showOpenDialog(f);
                        f.dispose();
                        if (res == JFileChooser.APPROVE_OPTION) {
                            path = new File(new File("C:\\Users\\User\\Documents\\Projects\\war-animation-maker").toURI().relativize(chooser.getSelectedFile().toURI()).getPath());
                        }
                    }).start();
                }
            });
        }

        public File getPath() {
            return path;
        }

        public void setPath(File path) {
            this.path = path;
        }

        public TextButton getTextButton() {
            return textButton;
        }
    }

    public void init() {
        Gdx.input.setInputProcessor(stage);
        stage.addActor(table);
    }
    @Override
    public void render(float delta) {
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        File imagePath = selectImageButton.getPath();
        if (imagePath != null) {
            imageLabel.setText(imagePath.getPath());
        } else {
            imageLabel.setText("null");
        }

        countriesLabel.setText(Arrays.toString(addCountriesButton.getPaths()));

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
