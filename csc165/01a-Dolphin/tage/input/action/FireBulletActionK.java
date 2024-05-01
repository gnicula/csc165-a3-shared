/*
 * @author Gabriele Nicula
 */

package tage.input.action;

import a2.MyGame;
import net.java.games.input.Event;
import tage.Camera;
import tage.GameObject;

/**
 * FireMissileAction implements shooting bullets from the avatar.
 * This InputAction works with the keyboard.
 */
public class FireBulletActionK extends AbstractInputAction {
    // MyGame object where GameObjects are placed in.
    private MyGame game;
    // Missile GameObject which moves.
    private GameObject bullet;
    // Speed at which action can occur.
    private float speed;
    // Scaling factor to slow or speed up rate at which actions can occur.
    private float direction_and_scale;

    /** <code> FireBulletActionK() </code> is invoked to execute this input action.
     * @param game -- MyGame object where movement is happening.
     * @param factor -- scaling factor that controls rate at which actions can occur.
     */
    public FireBulletActionK(MyGame game, float factor) {
        this.game = game;
        this.direction_and_scale = factor;
    }

    @Override
    public void performAction(float time, Event evt) {
        speed = direction_and_scale * time;

        game.fireBullet(speed);
    }
}