package tage.input.action;

import a2.MyGame;
import net.java.games.input.Event;
import tage.Camera;
/*
 * @author Gabriele Nicula
 */

import tage.GameObject;

/**
 * YawActionK turn the dolphin left/right around the world Y axis.
 * This input action works with keyboard.
 */
public class YawActionK extends AbstractInputAction {
    // MyGame object where GameObjects are placed in.
    private MyGame game;
    // Dolphin GameObject which moves.
    private GameObject dolph;
    // Camera object that is slaved to GameObject dolph.
    private Camera camera;
    // scaling factor for smoothness
    private float speed,
            scale = 0.0002f; // scale for smoothness
    // yaw_direction: -- 1 (left) or -1 (right)
    private int yaw_direction;

    /** <code> YawActionK() </code> is invoked to execute this input action.
     * the direction is either 1 (left) or -1 (right)
     * 
     * @param game
     * @param direction 1 for left, -1 for right
     */
    public YawActionK(MyGame game, int direction) {
        this.game = game;
        this.dolph = game.getDolphin();
        this.camera = game.getMyCamera();
        this.yaw_direction = direction;
    }

    @Override
    public void performAction(float time, Event evt) {
        speed = scale * time * yaw_direction;

        dolph.yaw(speed);
        // For A2 camera is always off dolphin
        if (game.onDolphinCam())
        {
            game.setOnDolphinCam();
        }

    }
}