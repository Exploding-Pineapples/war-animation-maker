package com.badlogicgames.superjumper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;

import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_WIDTH;

public class LoadingScreen extends ScreenAdapter implements InputProcessor {
    WarAnimationMaker game;
    boolean loading;

    public LoadingScreen(WarAnimationMaker game) {
        this.game = game;
        loading = false;
    }

    @Override
    public void render(float delta) {
        System.out.println("Loading");
        game.batcher.begin();

        game.gl.glClearColor(0, 0, 0, 1);
        game.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.bitmapFont.getData().setScale(1.0f);
        game.bitmapFont.setColor(Color.WHITE);
        game.bitmapFont.draw(game.batcher, "Loading", DISPLAY_WIDTH/2F, DISPLAY_HEIGHT/2F);
        game.batcher.end();

        if (loading) {
            Screen screen = new Screen(game);
            Gdx.input.setInputProcessor(screen);
            game.setScreen(screen);
            System.out.println("Loaded screen");
        }

        loading = true;
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