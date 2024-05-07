/*
 * @author Gabriele Nicula
 */

 package tage.input.action;

 import a2.MyGame;
 import net.java.games.input.Event;
  
 /**
  * Allows selection of a different textured avatar for multiplayer.
  * This InputAction works with the keyboard only.
  */
 public class SelectAvatarAction extends AbstractInputAction {
     // MyGame main object.
     private MyGame game;
 
     public SelectAvatarAction(MyGame game) {
         this.game = game;
     }
     
     @Override
     public void performAction(float time, Event e) {
         if (game.isSelectionScreen()) {
             game.closeAvatarSelection();
         }
     }
 }