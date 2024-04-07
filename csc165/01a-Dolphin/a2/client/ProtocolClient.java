package a2.client;

import a2.MyGame;
import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

// import tage.rml.Matrix4f;

import tage.*;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	
	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException 
	{	super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}
	
	public UUID getID() { return id; }
	
	@Override
	protected void processPacket(Object message)
	{	String strMessage = (String)message;
		System.out.println("message received -->" + strMessage);
		if (strMessage == null && !strMessage.trim().isEmpty()) {
			return;
		}
		String[] messageTokens = strMessage.split(",");
		
		// Game specific protocol to handle the message
		if(messageTokens.length > 0)
		{
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if(messageTokens[0].compareTo("join") == 0)
			{	if(messageTokens[1].compareTo("success") == 0)
				{	System.out.println("join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
				}
				if(messageTokens[1].compareTo("failure") == 0)
				{	System.out.println("join failure confirmed");
					game.setIsConnected(false);
			}	}
			
			// Handle BYE message
			// Format: (bye,remoteId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	// remove ghost avatar with id = remoteId
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}
			
			// Handle CREATE message
			// Format: (create,remoteId,x,y,z)
			// AND
			// Handle DETAILS_FOR message
			// Format: (dsfr,remoteId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0))
			{	// create a new ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));

				try
				{	ghostManager.createGhostAvatar(ghostID, ghostPosition);
				}	catch (IOException e)
				{	System.out.println("error creating ghost avatar");
				}
			}

			if (messageTokens[0].compareTo("createMissile") == 0)
			{
				UUID missileId = UUID.fromString(messageTokens[1]);

				Vector3f missilePosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				try {
					ghostManager.createGhostMissile(missileId, missilePosition);
				} catch (IOException e) {
					System.out.println("Error creating ghost missile");
				}
			}

			// Handle Missile MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("moveMissile") == 0)
			{
				// move a Missile
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f missilePosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				ghostManager.updateGhostMissile(ghostID, missilePosition);
			}

			// Handle Missile ROTATION message
			// Format: (rotate,remoteId,a00,a01,a02,a03,a10,a11 ...)
			if (messageTokens[0].compareTo("rotateMissile") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				float[][] values = new float[4][4];
				
				// Parse out the position into a Matrix4f
				Matrix4f missileRotation = new Matrix4f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]),
					Float.parseFloat(messageTokens[5]),
					Float.parseFloat(messageTokens[6]),
					Float.parseFloat(messageTokens[7]),
					Float.parseFloat(messageTokens[8]),
					Float.parseFloat(messageTokens[9]),
					Float.parseFloat(messageTokens[10]),
					Float.parseFloat(messageTokens[11]),
					Float.parseFloat(messageTokens[12]),
					Float.parseFloat(messageTokens[13]),
					Float.parseFloat(messageTokens[14]),
					Float.parseFloat(messageTokens[15]),
					Float.parseFloat(messageTokens[16]),
					Float.parseFloat(messageTokens[17])
				);

				ghostManager.rotateGhostMissile(ghostID, missileRotation);
			}


			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition());
			}
			
			// Handle MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("move") == 0)
			{
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				ghostManager.updateGhostAvatar(ghostID, ghostPosition);
			}

			// Handle ROTATE message
			// Format: (rotate,remoteId,a00,a01,a02,a03,a10,a11 ...)
			if (messageTokens[0].compareTo("rotate") == 0)
			{
				// rotate the ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				float[][] values = new float[4][4];
				
				// Parse out the position into a Matrix4f
				Matrix4f ghostRotation = new Matrix4f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]),
					Float.parseFloat(messageTokens[5]),
					Float.parseFloat(messageTokens[6]),
					Float.parseFloat(messageTokens[7]),
					Float.parseFloat(messageTokens[8]),
					Float.parseFloat(messageTokens[9]),
					Float.parseFloat(messageTokens[10]),
					Float.parseFloat(messageTokens[11]),
					Float.parseFloat(messageTokens[12]),
					Float.parseFloat(messageTokens[13]),
					Float.parseFloat(messageTokens[14]),
					Float.parseFloat(messageTokens[15]),
					Float.parseFloat(messageTokens[16]),
					Float.parseFloat(messageTokens[17])
				);

				// values[0][0] = Float.parseFloat(messageTokens[2]);
				// values[0][1] = Float.parseFloat(messageTokens[3]);
				// values[0][2] = Float.parseFloat(messageTokens[4]);
				// values[0][3] = Float.parseFloat(messageTokens[5]);
				// values[1][0] = Float.parseFloat(messageTokens[6]);
				// values[1][1] = Float.parseFloat(messageTokens[7]);
				// values[1][2] = Float.parseFloat(messageTokens[8]);
				// values[1][3] = Float.parseFloat(messageTokens[9]);
				// values[2][0] = Float.parseFloat(messageTokens[10]);
				// values[2][1] = Float.parseFloat(messageTokens[11]);
				// values[2][2] = Float.parseFloat(messageTokens[12]);
				// values[2][3] = Float.parseFloat(messageTokens[13]);
				// values[3][0] = Float.parseFloat(messageTokens[14]);
				// values[3][1] = Float.parseFloat(messageTokens[15]);
				// values[3][2] = Float.parseFloat(messageTokens[16]);
				// values[3][3] = Float.parseFloat(messageTokens[17]);

				// Matrix4f ghostRotation = Matrix4f.createFrom(values);
				
				ghostManager.rotateGhostAvatar(ghostID, ghostRotation);
			}

		}
	}
	
	// The initial message from the game client requesting to join the 
	// server. localId is a unique identifier for the client. Recommend 
	// a random UUID.
	// Message Format: (join,localId)
	
	public void sendJoinMessage()
	{	try 
		{	sendPacket(new String("join," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the client is leaving the server. 
	// Message Format: (bye,localId)

	public void sendByeMessage()
	{	try 
		{	sendPacket(new String("bye," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the client�s Avatar�s position. The server 
	// takes this message and forwards it to all other clients registered 
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessage(Vector3f position)
	{	try 
		{	String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the local avatar's position. The server then 
	// forwards this message to the client with the ID value matching remoteId. 
	// This message is generated in response to receiving a WANTS_DETAILS message 
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the local avatar has changed position.  
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the position.

	// ------------- Avatar Messages -------------------//

	public void sendMoveMessage(Vector3f position)
	{	try 
		{	String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}

	public void sendRotationMessage(Matrix4f rotation) {
		try {
			String message = new String("rotate," + id.toString());
			float[] buffer = new float[16];
			rotation.get(buffer);
			message += "," + buffer[0];
			message += "," + buffer[1];
			message += "," + buffer[2];
			message += "," + buffer[3];
			message += "," + buffer[4];
			message += "," + buffer[5];
			message += "," + buffer[6];
			message += "," + buffer[7];
			message += "," + buffer[8];
			message += "," + buffer[9];
			message += "," + buffer[10];
			message += "," + buffer[11];
			message += "," + buffer[12];
			message += "," + buffer[13];
			message += "," + buffer[14];
			message += "," + buffer[15];

			// message += "," + rotation.value(0,0);
			// message += "," + rotation.value(0,1);
			// message += "," + rotation.value(0,2);
			// message += "," + rotation.value(0,3);
			// message += "," + rotation.value(1,0);
			// message += "," + rotation.value(1,1);
			// message += "," + rotation.value(1,2);
			// message += "," + rotation.value(1,3);
			// message += "," + rotation.value(2,0);
			// message += "," + rotation.value(2,1);
			// message += "," + rotation.value(2,2);
			// message += "," + rotation.value(2,3);
			// message += "," + rotation.value(3,0);
			// message += "," + rotation.value(3,1);
			// message += "," + rotation.value(3,2);
			// message += "," + rotation.value(3,3);

			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ------------- Missile Messages -------------------//

	public void sendCreateMissileMessage(Vector3f position)
	{	try 
		{	String message = new String("createMissile," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}

	public void sendMoveMissileMessage(Vector3f position) {
		try 
		{	String message = new String("moveMissile," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
		}
	}

	public void sendMissileRotationMessage(Matrix4f rotation) {
		try {	
			String message = new String("rotateMissile," + id.toString());
			float[] buffer = new float[16];
			rotation.get(buffer);
			message += "," + buffer[0];
			message += "," + buffer[1];
			message += "," + buffer[2];
			message += "," + buffer[3];
			message += "," + buffer[4];
			message += "," + buffer[5];
			message += "," + buffer[6];
			message += "," + buffer[7];
			message += "," + buffer[8];
			message += "," + buffer[9];
			message += "," + buffer[10];
			message += "," + buffer[11];
			message += "," + buffer[12];
			message += "," + buffer[13];
			message += "," + buffer[14];
			message += "," + buffer[15];

			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
