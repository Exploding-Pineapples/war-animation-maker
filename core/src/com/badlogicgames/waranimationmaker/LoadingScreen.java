package com.badlogicgames.waranimationmaker;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogicgames.waranimationmaker.models.Animation;

import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH;

public class LoadingScreen extends ScreenAdapter implements InputProcessor {
    WarAnimationMaker game;
    Animation animation;
    boolean loading;

    public LoadingScreen(WarAnimationMaker game, Animation animation) {
        this.game = game;
        this.animation = animation;
        loading = false;
    }

    @Override
    public void render(float delta) {
        game.batcher.begin();

        game.gl.glClearColor(0, 0, 0, 1);
        game.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.batcher.setShader(game.fontShader);
        game.fontShader.setUniformf("outlineDistance", 0.5f);
        GlyphLayout layout = new GlyphLayout();
        layout.setText(game.bitmapFont, "Loading...");
        game.bitmapFont.draw(game.batcher, layout, DISPLAY_WIDTH/2F - layout.width / 2, DISPLAY_HEIGHT/2F - layout.height / 2);
        game.batcher.setShader(null);
        game.batcher.end();

        if (loading) {
            AnimationScreen screen = new AnimationScreen(game, animation);
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