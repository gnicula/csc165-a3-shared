/*
 * @author Gabriele Nicula
 */

package tage.input.action;

import a2.MyGame;
import net.java.games.input.Event;
import tage.Camera;
import tage.GameObject;

/**
 * PitchActionJ nose up/down around local side axis.
 * This InputAction works with the game controller.
 */
public class CameraPitchActionJ extends AbstractInputAction {
    // MyGame object where GameObjects are placed in.
    private MyGame game;
    // Dolphin GameObject which moves.
    private GameObject dolph;
    // Camera object that is slaved to GameObject dolph.
    private Camera camera;
    // Speed at which action can occur.
    // Scaling factor to slow or speed up rate at which actions can occur.
    private float speed, scale = 0.0002f;

    /** <code> ForwardBackActionJ() </code> is invoked to execute this input action.
     * 
     * @param game -- MyGame object where movement is happening.
     */
    public CameraPitchActionJ(MyGame game) {
        this.game = game;
        this.dolph = game.getDolphin();
        this.camera = game.getMyCamera();
    }

    @Override
    public void performAction(float time, Event evt) {
        float keyValue = evt.getValue();
        if (keyValue > -.2 && keyValue < .2)
        {
            return; // deadzone
        }
        float turn_direction = (keyValue >= .2) ? -1 : 1;
        speed = scale * time * turn_direction;

        dolph.pitch(speed);
        // For A2 camera is always off dolphin
        if (game.onDolphinCam())
        {
            game.setOnDolphinCam();
        }
    }
}