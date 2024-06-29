package com.badlogicgames.superjumper;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_HEIGHT;
import static com.badlogicgames.superjumper.WarAnimationMaker.DISPLAY_WIDTH;

public class Button {
    private Rectangle frame;
    private Texture texture;
    private String name;
    private Color textcolor;

    public Button (Rectangle f, String n, Color tcolor) {
        frame = f;
        name = n;
        textcolor = tcolor;
    }

    public Button (Rectangle f, String n, Texture t) {
        frame = f;
        texture = t;
        name = n;
        textcolor = Color.WHITE;
    }

    public Button (Rectangle f, String n) {
        frame = f;
        name = n;
        textcolor = Color.WHITE;
    }

    public boolean clicked(int x, int y) {
        return frame.contains(x, y);
    }

    public String getName() {
        return name;
    }

    public void draw(WarAnimationMaker game) {
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.rect(frame.x, frame.y, frame.width, -frame.height);
        game.shapeRenderer.end();

        game.batcher.begin();
        game.bitmapFont.getData().setScale(1.0f);
        game.bitmapFont.setColor(textcolor);
        game.bitmapFont.draw(game.batcher, name, frame.x + 5, frame.y - 5);
        game.batcher.end();
    }
}
