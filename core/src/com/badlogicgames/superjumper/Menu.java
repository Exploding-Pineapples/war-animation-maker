package com.badlogicgames.superjumper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_WIDTH;

public class Menu extends ScreenAdapter implements InputProcessor {
    WarAnimationMaker game;
    Button openFile;
    boolean loading;

    public Menu(WarAnimationMaker game) {
        this.game = game;
        openFile = new Button(new Rectangle(DISPLAY_WIDTH/2F, DISPLAY_HEIGHT/2F, 100, 30), "Open File", Color.ORANGE);
        loading = false;
    }

    @Override
    public void render(float delta) {
        game.batcher.begin();

        game.gl.glClearColor(0, 0, 0, 1);
        game.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.bitmapFont.getData().setScale(1.0f);
        game.bitmapFont.setColor(Color.ORANGE);
        game.bitmapFont.draw(game.batcher, "Loading", DISPLAY_WIDTH/2F, DISPLAY_HEIGHT/2F);
        game.batcher.end();

        openFile.draw(game);
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
    public boolean touchDown(int x, int y, int pointer, int button) {
        if (openFile.clicked(x, y)) {
            System.out.println("Clicked button at " + x + ", " + y);

            Gdx.input.setInputProcessor(game.loadingScreen);
            game.setScreen(game.loadingScreen);

            loading = true;
            return true;
        }
        System.out.println("Clicked " + x + ", " + y);
        return false;
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