package a2.client;

import a2.MyGame;
import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;

public class GhostManager
{
	private MyGame game;
	private HashMap<UUID, GhostAvatar> ghostAvatars = new HashMap<UUID, GhostAvatar>();
	private HashMap<UUID, GhostNPC> ghostNPCS = new HashMap<UUID, GhostNPC>();
	private HashMap<UUID, GhostMarker> ghostMarkers = new HashMap<UUID, GhostMarker>();
	private HashMap<UUID, GhostMissile> ghostMissiles = new HashMap<UUID, GhostMissile>();
	private HashMap<UUID, GhostBullet> ghostBullets = new HashMap<UUID, GhostBullet>();

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame)vfrg;
	}
	
	public void createGhostAvatar(UUID id, Vector3f position, int textureId) throws IOException
	{	
		System.out.println("adding ghost with ID --> " + id);
		ObjShape s = game.getGhostShape();
		TextureImage t = game.getGhostTexture(textureId);
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, position);
		Matrix4f initialScale = (new Matrix4f()).scaling(0.05f);
		newAvatar.setLocalScale(initialScale);
		ghostAvatars.put(id, newAvatar);
	}
	
	public void removeGhostAvatar(UUID id)
	{	
		GhostAvatar ghostAvatar = ghostAvatars.get(id);
		if(ghostAvatar != null)
		{	game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		}
		else
		{	System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	public void updateGhostAvatar(UUID id, Vector3f position)
	{	
		GhostAvatar ghostAvatar = ghostAvatars.get(id);
		if (ghostAvatar != null)
		{	ghostAvatar.setPosition(position);
		}
		else
		{	System.out.println("tried to update ghost avatar position, but unable to find ghost in list");
		}
	}

	public void rotateGhostAvatar(UUID id, Matrix4f rotate)
	{	
		GhostAvatar ghostAvatar = ghostAvatars.get(id);
		if (ghostAvatar != null)
		{	ghostAvatar.setRotation(rotate);
		}
		else
		{	System.out.println("tried to update ghost avatar rotation, but unable to find ghost in list");
		}
	}

	public void createGhostMarker(UUID id, Vector3f position) throws IOException
	{
		ObjShape marker = game.getMarkerShape();
		TextureImage markerTexture = game.getMarkerTexture();
		GhostMarker newMarker = new GhostMarker(id, marker, markerTexture, position);
		Matrix4f initialScale = (new Matrix4f().scaling(0.1f));
		newMarker.setLocalScale(initialScale);
		ghostMarkers.put(id, newMarker);
	}

	public void updateGhostMarker(UUID id, Vector3f position) 
	{
		GhostMarker ghostMarker = ghostMarkers.get(id);
		if (ghostMarker != null) {
			ghostMarker.setPosition(position);
		} else {
			System.out.println("Tried to update ghost Marker position, but unable to find ghost in list.");
		}
	}

	public void rotateGhostMarker(UUID id, Matrix4f rotate) {
		GhostMarker ghostMarker = ghostMarkers.get(id);
		if (ghostMarker != null)
		{	ghostMarker.setRotation(rotate);
		}
		else
		{	System.out.println("tried to update ghost Marker rotation, but unable to find ghost in list");
		}
	}

	public void createGhostMissile(UUID id, Vector3f position) throws IOException
	{
		ObjShape missile = game.getMissileShape();
		TextureImage missileTexture = game.getMissileTexture();
		GhostMissile newMissile = new GhostMissile(id, missile, missileTexture, position);
		Matrix4f initialScale = (new Matrix4f().scaling(0.1f));
		newMissile.setLocalScale(initialScale);
		ghostMissiles.put(id, newMissile);
	}

	public void updateGhostMissile(UUID id, Vector3f position) 
	{
		GhostMissile ghostMissile = ghostMissiles.get(id);
		if (ghostMissile != null) {
			ghostMissile.setPosition(position);
		} else {
			System.out.println("Tried to update ghost missile position, but unable to find ghost in list.");
		}
	}

	public void rotateGhostMissile(UUID id, Matrix4f rotate) {
		GhostMissile ghostMissile = ghostMissiles.get(id);
		if (ghostMissile != null)
		{	ghostMissile.setRotation(rotate);
		}
		else
		{	System.out.println("tried to update ghost missile rotation, but unable to find ghost in list");
		}
	}

	public void createGhostBullet(UUID id, Vector3f position) throws IOException
	{
		ObjShape bullet = game.getBulletShape();
		TextureImage bulletTexture = game.getBulletTexture();
		GhostBullet newBullet = new GhostBullet(id, bullet, bulletTexture, position);
		Matrix4f initialScale = (new Matrix4f().scaling(.3f));
		newBullet.setLocalScale(initialScale);
		ghostBullets.put(id, newBullet);
	}

	public void updateGhostBullet(UUID id, Vector3f position)
	{
		GhostBullet ghostBullet = ghostBullets.get(id);
		if (ghostBullet != null) {
			ghostBullet.setPosition(position);
		} else {
			System.out.println("Tried to update ghost bullet position, but unable to find ghost in list.");
		}
	}

	public void rotateGhostBullet(UUID id, Matrix4f rotate) {
		GhostBullet ghostBullet = ghostBullets.get(id);
		if (ghostBullet != null) {
			ghostBullet.setRotation(rotate);
		} else {
			System.out.println("Tried to update ghost bullet rotation, but unable to find ghost in list.");
		}
	}

	// public void createGhostNPC(UUID id, Vector3f position) throws IOException {
	// 	ObjShape ghostNPC = game.getGhostNPCShape();
	// 	TextureImage ghostNPCTexture = game.getGhostNPCTexture();
		

	// }
}
