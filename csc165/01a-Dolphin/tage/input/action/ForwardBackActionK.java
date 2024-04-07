/*
 * @author Gabriele Nicula
 */

package tage.input.action;

import a2.MyGame;
import net.java.games.input.Event;
import tage.Camera;
import tage.GameObject;

/**
 * ForwardBackAction implements moving forward or backward the Game object.
 * This InputAction works with the keyboard.
 */
public class ForwardBackActionK extends AbstractInputAction {
    // MyGame object where GameObjects are placed in.
    private MyGame game;
    // Dolphin GameObject which moves.
    private GameObject dolph;
    // Camera object that is slaved to GameObject dolph.
    private Camera camera;
    // Speed at which action can occur.
    private float speed;
    // Scaling factor to slow or speed up rate at which actions can occur. Also 
    // has direction for joystick usage.
    private float direction_and_scale;

    /** <code> ForwardBackActionK() </code> is invoked to execute this input action.
     * @param game -- MyGame object where movement is happening.
     * @param factor -- scaling factor that controls rate at which actions can occur.
     */
    public ForwardBackActionK(MyGame game, float factor) {
        this.game = game;
        this.dolph = game.getDolphin();
        this.camera = game.getMyCamera();
        this.direction_and_scale = factor;
    }

    @Override
    public void performAction(float time, Event evt) {
        speed = direction_and_scale * time;

        dolph.moveForwardBack(speed, camera.getLocation());
        // game.setAvatarHeightAtLocation();
        game.updateGhost(dolph);
        // For A2 camera is always off dolphin
        if (game.onDolphinCam())
        {
            game.setOnDolphinCam();
        }
    }
}