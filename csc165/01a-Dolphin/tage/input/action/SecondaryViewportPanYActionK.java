/*
 * @author Gabriele Nicula
 */

 package tage.input.action;

 import a2.MyGame;
import net.java.games.input.Event;
 import tage.Camera;
 import tage.GameObject;

 
 /**
  * SecondaryViewportPanYActionK implements across the y-axis on the "minimap" viewport.
  * This InputAction works with the keyboard.
  */
 public class SecondaryViewportPanYActionK extends AbstractInputAction {
     // MyGame object where GameObjects are placed in.
     private MyGame game;
     // Camera object that is positioned for the viewport
     private Camera camera;
     // Speed at which action can occur.
     private float speed;
     // Scaling factor to slow or speed up rate at which actions can occur. Also 
     // has direction for joystick usage.
     private float direction_and_scale;
 
     /** <code> SecondaryViewportPanYActionK() </code> is invoked to execute this input action.
      * @param game -- MyGame object where movement is happening.
      * @param factor -- scaling factor that controls rate at which actions can occur.
      */
     public SecondaryViewportPanYActionK(MyGame game, float factor) {
         this.game = game;
         this.camera = game.getMyViewportCamera();
         this.direction_and_scale = factor;
     }
 
     @Override
     public void performAction(float time, Event evt) {
         speed = direction_and_scale * time;
        
         camera.setLocation(camera.getLocation().add(0.0f, 0.0f, speed));
     }
 }