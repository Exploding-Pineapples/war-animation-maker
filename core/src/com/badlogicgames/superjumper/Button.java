package com.badlogicgames.superjumper;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;

public class Button {
    private Rectangle frame;
    private Texture texture;
    private String name;

    public Button (Rectangle f, String n, Texture t) {
        frame = f;
        texture = t;
        name = n;
    }

    public Button (Rectangle f, String n) {
        frame = f;
        name = n;
    }

    public boolean clicked(int x, int y) {
        return frame.contains(x, y);
    }

    public String getName() {
        return name;
    }
}
