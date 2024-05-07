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
public class CycleAvatarSelectionAction extends AbstractInputAction {
    // MyGame main object.
    private MyGame game;

    public CycleAvatarSelectionAction(MyGame game) {
        this.game = game;
    }
    
    @Override
    public void performAction(float time, Event e) {
        if (game.isSelectionScreen()) {
            if (game.getSelectedAvatar() == 0 || game.getSelectedAvatar() == 1) {
                game.setSelectedAvatar(2);
            } else {
                game.setSelectedAvatar(1);
            }
        }
    }
}