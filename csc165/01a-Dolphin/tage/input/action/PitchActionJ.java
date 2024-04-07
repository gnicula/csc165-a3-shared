/*
 * @author Gabriele Nicula
 */

 package tage.input.action;

import a2.MyGame;
import net.java.games.input.Event;
import tage.Camera;
import tage.GameObject;

/**
 * PitchActionJ implements pitch up/down of the Game object.
 * This InputAction works with the controller.
 */
public class PitchActionJ extends AbstractInputAction {
    // MyGame object where GameObjects are placed in.
    private MyGame game;
    // Dolphin GameObject which moves.
    private GameObject dolph;
    // Camera object that is slaved to GameObject dolph.
    private Camera camera;
    // Speed at which action can occur.
    private float speed;
    // Scaling factor to slow or speed up rate at which actions can occur. 
    // Also positive nose up, negative nose down
    private float scale; 

    /** <code> PitchActionJ() </code> is invoked to execute this input action.
     * @param game -- MyGame object where movement is happening.
     * @param factor -- scaling factor that controls rate at which actions can occur.
     */
    public PitchActionJ(MyGame game, float factor) {
        this.game = game;
        this.dolph = game.getDolphin();
        this.camera = game.getMyCamera();
        this.scale = factor;
    }

    @Override
    public void performAction(float time, Event evt) {
        float keyValue = evt.getValue();
        if (keyValue > -.2 && keyValue < .2)
        {
            return; // deadzone
        }
        float advance_direction = (keyValue >= .2) ? -1 : 1;
        speed = scale * time * advance_direction;

        dolph.pitch(speed);
        // For A2 camera is always off dolphin
        if (game.onDolphinCam())
        {
            game.setOnDolphinCam();
        }

    }
}