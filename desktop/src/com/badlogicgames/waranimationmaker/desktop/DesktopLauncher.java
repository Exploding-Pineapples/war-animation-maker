package com.badlogicgames.waranimationmaker.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogicgames.waranimationmaker.WarAnimationMaker;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Super Jumper";
		config.width = 1920;
		config.height = 1080;
		config.forceExit = false;
		new LwjglApplication(new WarAnimationMaker(), config);
	}
}
