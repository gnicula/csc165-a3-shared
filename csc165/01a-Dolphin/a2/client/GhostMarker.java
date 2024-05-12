package a2.client;

import a2.MyGame;
import java.util.UUID;

import tage.*;
import org.joml.*;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.
/**
 * This class takes care of keeping track of markers dropped by other clients so that they are seen by all users.
 * @return
 */
public class GhostMarker extends GameObject
{
	UUID uuid;

	public GhostMarker(UUID id, ObjShape s, TextureImage t, Vector3f p) 
	{	super(GameObject.root(), s, t);
		uuid = id;
		setPosition(p);
	}
	
	public UUID getID() { return uuid; }
	public void setPosition(Vector3f m) { setLocalLocation(m); }
	public Vector3f getPosition() { return getWorldLocation(); }
	public void setRotation(Matrix4f m) { setLocalRotation(m); }
	public Matrix4f getRotation() { return getWorldRotation(); }
}
