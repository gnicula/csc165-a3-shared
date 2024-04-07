/*
 * @author Gabriele Nicula
 */

package tage.input.action;

import a2.MyGame;
import net.java.games.input.Event;
import tage.Camera;
import tage.GameObject;

/**
 * RollActionK rolls the dolphin left/right around its moving direction.
 * This InputAction works with the keyboard.
 */
public class RollActionK extends AbstractInputAction {
    // MyGame object where GameObjects are placed in.
    private MyGame game;
    // Dolphin GameObject which moves.
    private GameObject dolph;
    // Camera object that is slaved to GameObject dolph.
    private Camera camera;
    // Scaling factor to slow or speed up rate at which actions can occur.  
    private float speed,
            scale = 0.0002f; // scale for smoothness

    /**
     * yaw direction: 1 (left) or -1 (right)
     */
    private int roll_direction;

    /** <code> RollActionK() </code> is invoked to execute this input action.
     * the direction is either 1 (left) or -1 (right)
     * @param game -- MyGame object where movement is happening.
     * @param direction -- 1 for left, -1 for right
     */
    public RollActionK(MyGame game, int direction) {
        this.game = game;
        this.dolph = game.getDolphin();
        this.camera = game.getMyCamera();
        this.roll_direction = direction;
    }

    @Override
    public void performAction(float time, Event evt) {
        speed = scale * time * roll_direction;

        dolph.roll(speed);
        // For A2 camera is always off dolphin
        if (game.onDolphinCam())
        {
            game.setOnDolphinCam();
        }

    }
}