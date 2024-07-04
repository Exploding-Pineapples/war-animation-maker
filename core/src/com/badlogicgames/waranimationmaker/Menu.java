package com.badlogicgames.waranimationmaker;

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
import com.badlogicgames.waranimationmaker.models.Animation;
import org.jetbrains.annotations.NotNull;

import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH;

public class Menu extends ScreenAdapter implements InputProcessor {
    WarAnimationMaker game;
    Stage stage;
    Skin skin;
    Table table = new Table();


    public Menu(WarAnimationMaker game) {
        table.clear();

        skin = game.skin;
        this.game = game;
        table.setPosition(DISPLAY_WIDTH/2f, DISPLAY_HEIGHT/2f);

        stage = new Stage();

        Table titleTable = new Table();
        titleTable.setPosition(DISPLAY_WIDTH/2f, DISPLAY_HEIGHT - 100);
        Label titleLabel = new Label("War Animation Maker", skin);
        titleLabel.setPosition(DISPLAY_WIDTH/2f, DISPLAY_HEIGHT - 100);
        titleTable.add(titleLabel);
        stage.addActor(titleTable);

        TextButton newAnimationButton = new TextButton("New Animation", skin, "small");
        newAnimationButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new NewAnimationScreen(game, new Animation("")));
            }
        });
        table.add(newAnimationButton).colspan(4);
        table.row().pad(10);

        Gdx.input.setInputProcessor(stage);

        FileHandler.INSTANCE.load();
        FileHandler.INSTANCE.save();

        Label animationTitle = new Label("Animations", skin);
        table.add(animationTitle).colspan(4);
        table.row().pad(10).height(40);

        for (Animation animation : FileHandler.INSTANCE.getAnimations()) {
            TextButton deleteButton = getDeleteButton(animation);
            Label animationLabel = new Label(animation.getName(), skin);
            TextButton openButton = getOpenButton(animation);
            TextButton editButton = getEditButton(animation);

            table.add(deleteButton);
            table.add(animationLabel);
            table.add(openButton);
            table.add(getEditButton(animation));

            table.row().pad(10).height(40);
        }

        stage.addActor(table);
    }

    private @NotNull TextButton getOpenButton(Animation animation) {
        TextButton textButton = new TextButton("Open", skin, "small");
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                LoadingScreen loadingScreen = new LoadingScreen(game, animation);
                Gdx.input.setInputProcessor(loadingScreen);
                game.setScreen(loadingScreen);
            }
        });
        return textButton;
    }

    private @NotNull TextButton getDeleteButton(Animation animation) {
        TextButton textButton = new TextButton("Delete", skin, "small");
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FileHandler.INSTANCE.deleteAnimation(animation);
                game.menu = new Menu(game);
                game.setScreen(game.menu);
            }
        });
        return textButton;
    }

    private @NotNull TextButton getEditButton(Animation animation) {
        TextButton textButton = new TextButton("Edit", skin, "small");
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                NewAnimationScreen newAnimationScreen = new NewAnimationScreen(game, animation);
                game.setScreen(newAnimationScreen);
            }
        });
        return textButton;
    }

    @Override
    public void render(float delta) {
        game.gl.glClearColor(0, 0, 0, 1);
        game.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act();
        stage.draw();
    }

    @Override
    public boolean keyDown(int i) {
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int b) {
        return true;
    }

    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        return false;
    }

    @Override
    public boolean mouseMoved(int i, int i1) {
        return false;
    }

    @Override
    public boolean scrolled(float v, float v1) {
        return false;
    }
}