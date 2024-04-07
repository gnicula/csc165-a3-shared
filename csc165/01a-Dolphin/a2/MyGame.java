/*
 * @author Gabriele Nicula
 */

package a2;

import tage.*;
import tage.shapes.*;
import tage.input.InputManager; // input management
import tage.input.action.*;
import tage.networking.IGameConnection.ProtocolType;
import tage.nodeControllers.RotationController;
import tage.nodeControllers.StretchController;
import net.java.games.input.Controller;

import java.util.ArrayList;
import java.util.ListIterator;
import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import org.joml.*;

import a2.client.GhostAvatar;
import a2.client.GhostManager;
import a2.client.ProtocolClient;

public class MyGame extends VariableFrameRateGame {
	private static Engine engine;

	private boolean paused = false;
	private boolean offDolphinCam = true; // default camera off dolphin
	private boolean axesRenderState = true;
	private boolean[] visitedSites = new boolean[4]; // default initialized to false
	private int counter = 0;
	private int gameOver = 0;
	private int frameCounter = 0;
	private int skyBoxID;
	private double lastFrameTime, currFrameTime, elapsTime;
	private ArrayList<GameObject> movingObjects = new ArrayList<GameObject>();
	private ArrayList<GameObject> movingEnemies = new ArrayList<GameObject>();
	private GameObject dol, base, torus, sphere, sphereSatellite, plane, groundPlane,
			wAxisX, wAxisY, wAxisZ, manual, magnet, missileObj, tower;
	private ObjShape dolS, cubS, torusS, enemyShape, planeS, groundPlaneS, wAxisLineShapeX, wAxisLineShapeY, 
			wAxisLineShapeZ, manualS, magnetS, worldObj, missileShape, towerS;
	private TextureImage doltx, brick, grass, corvette, assignt, enemyTexture, metal, water, 
			torusWater, fur, terrainTexture, terrainHeightMap, missile, towerTexture;
	private Light light1, light2;
	private Camera myCamera, myViewportCamera;
	private CameraOrbit3D orbitController;
	private ArrayList<NodeController> controllerArr = new ArrayList<NodeController>();
	private InputManager inputManager;
	private Vector3f location; // world object location
	private Vector3f forward; // n-vector/z-axis
	private Vector3f up; // v-vector/y-axis
	private Vector3f right; // u-vector/x-axis

	private GhostManager gm;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	private final int WindowSizeX = 2000;
	private final int WindowSizeY = 1000;

	
	public MyGame(String serverAddress, int serverPort, String protocol)
	{	super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args) {
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadSkyBoxes() {
		skyBoxID = (engine.getSceneGraph()).loadCubeMap("daylightSky");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(skyBoxID);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void loadShapes() {
		towerS = new ImportedModel("towertest.obj");
		dolS = new ImportedModel("f15.obj");
		// dolS = new AnimatedShape("drone.rkm", "drone.rks");
		// ((AnimatedShape)dolS).loadAnimation("WALK", "drone_flying.rka");
		cubS = new Cube();
		// torusS = new Torus();
		enemyShape = new Sphere();
		// planeS = new Plane();
		// groundPlaneS = new Plane();
		System.out.println("about to load terrain shape");
		// groundPlaneS = new ImportedModel("terrain.obj");
		groundPlaneS = new TerrainPlane(1000);
		System.out.println("loaded terrain shape");
		// groundPlaneS = new TerrainPlane(1000);
		manualS = new MyManualObject();
		magnetS = new MyMagnetObject();
		missileShape = new ImportedModel("asca.obj");


		float lineLength = 1.0f;
		Vector3f worldOrigin = new Vector3f(0f, 0f, 0f);
		Vector3f lineX = new Vector3f(lineLength, 0f, 0f);
		Vector3f lineY = new Vector3f(0f, lineLength, 0f);
		Vector3f lineZ = new Vector3f(0f, 0f, -lineLength);

		wAxisLineShapeX = new Line(worldOrigin, lineX);
		wAxisLineShapeY = new Line(worldOrigin, lineY);
		wAxisLineShapeZ = new Line(worldOrigin, lineZ);
	}

	@Override
	public void loadTextures() {
		doltx = new TextureImage("F15A.jpg");
		towerTexture = new TextureImage("towertexturetest.jpg");
		grass = new TextureImage("grass1.jpg");
		corvette = new TextureImage("corvette1.jpg");
		assignt = new TextureImage("assign1.png");
		enemyTexture = new TextureImage("gold1.jpg");
		metal = new TextureImage("magnet1.jpg");
		missile = new TextureImage("missile.jpg");
		// https://www.pexels.com/photo/body-of-water-261403/
		// water = new TextureImage("water.jpg");
		terrainTexture = new TextureImage("surface1.png");
		terrainHeightMap = new TextureImage("aworldheightmap.png");
		// https://www.pexels.com/photo/aerial-shot-of-blue-water-3894157/
		torusWater = new TextureImage("waterTorus.jpg");
		// https://www.pexels.com/photo/brown-thick-fur-7232502/
		fur = new TextureImage("fur1.jpg");
	}

	public void buildEnemyObjects(int numEnemies) {
		Matrix4f initialTranslation, initialScale;
		for (int i = 0; i < numEnemies; ++i) {
			GameObject enemy = new GameObject(GameObject.root(), enemyShape, enemyTexture);
			double ranAngle = Math.random() * 360;
			float ranX = (float)Math.cos(ranAngle) * 30.0f;
			float ranZ = (float)Math.sin(ranAngle) * 30.0f;
			initialScale = (new Matrix4f()).scale(0.5f);
			enemy.setLocalScale(initialScale);
			initialTranslation = (new Matrix4f()).translation(ranX, 0, ranZ);
			enemy.setLocalTranslation(initialTranslation);
			movingEnemies.add(enemy);
		}
	}

	@Override
	public void buildObjects() {
		// Local temporaries for building objects
		Matrix4f initialTranslation, initialScale;

		// build the ground plane on X-Z
		System.out.println("about to create terain object");
		groundPlane = new GameObject(GameObject.root(), groundPlaneS, terrainTexture);
		System.out.println("created terain object");
		groundPlane.setHeightMap(terrainHeightMap);
		initialScale = (new Matrix4f()).scaling(20.0f);
		groundPlane.setLocalScale(initialScale);
		initialTranslation = (new Matrix4f()).translation(0, 0, 0.0f);
		groundPlane.setLocalTranslation(initialTranslation);
		groundPlane.setIsTerrain(true);

		// groundPlane.getRenderStates().setTiling(1);
		// groundPlane.getRenderStates().hasLighting(true);
		
		// build dolphin in the center of the window
		dol = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = (new Matrix4f()).translation(0, 15.0f, 0);
		initialScale = (new Matrix4f()).scaling(0.05f);
		dol.setLocalTranslation(initialTranslation);
		dol.setLocalScale(initialScale);

		// build our base at the center of the map.
		base = new GameObject(GameObject.root(), towerS, towerTexture);
		initialTranslation = (new Matrix4f()).translation(0, 10.5f, 0.5f);
		initialScale = (new Matrix4f()).scaling(0.10f);
		base.setLocalTranslation(initialTranslation);
		base.setLocalScale(initialScale);

		// A2 requirement - add a hierarchical relationship
		// GameObject cubSatellite = new GameObject(cub, imported, fur);
		// cubSatellite.getRenderStates().setTiling(1);
		// cubSatellite.setLocalScale((new Matrix4f()).scaling(0.25f));
		// cubSatellite.setLocalTranslation((new Matrix4f()).translation(0.75f, 0.5f, 0));
		// cubSatellite.propagateTranslation(true);
		// cubSatellite.propagateRotation(true);
		// cubSatellite.applyParentRotationToPosition(true);

		// build a watery torus at the left side of the window
		// torus = new GameObject(GameObject.root(), torusS, torusWater);
		// torus.getRenderStates().setTiling(1);
		// initialTranslationTorus = (new Matrix4f()).translation(-4, 0.11f, -4);
		// initialScaleTorus = (new Matrix4f()).scaling(0.5f);
		// torus.setLocalTranslation(initialTranslationTorus);
		// torus.setLocalScale(initialScaleTorus);


		// build a sphere logo textured at the right side of the window
		// sphere = new GameObject(GameObject.root(), sphereS, corvette);
		// initialTranslationSphere = (new Matrix4f()).translation(4, 0.2f, 4);
		// initialScaleSphere = (new Matrix4f()).scaling(0.5f);
		// sphere.setLocalTranslation(initialTranslationSphere);
		// sphere.setLocalScale(initialScaleSphere);
		// now create a example of a hierarchical relationship by adding 
		// a smaller sphereSatellite to our sphere object.
		// sphereSatellite = new GameObject(sphere, sphereS, grass);
		// sphereSatellite.setLocalScale((new Matrix4f()).scaling(0.25f));
		// sphereSatellite.setLocalTranslation((new Matrix4f()).translation(0.75f, 0.5f, 0));
		// sphereSatellite.propagateTranslation(true);
		// sphereSatellite.propagateRotation(true);
		// sphereSatellite.applyParentRotationToPosition(true);

		// build a plane textured at the left side of the window
		// plane = new GameObject(GameObject.root(), planeS, assignt);
		// initialTranslationPlane = (new Matrix4f()).translation(-4, .01f, 4);
		// initialScalePlane = (new Matrix4f()).scaling(0.75f);
		// plane.setLocalTranslation(initialTranslationPlane);
		// plane.setLocalScale(initialScalePlane);

		// build my manual object
		// manual = new GameObject(GameObject.root(), manualS, gold);
		// initialTranslationManual = (new Matrix4f()).translation(0, 1.75f, -4);
		// initialScaleManual = (new Matrix4f()).scaling(0.4f);
		// manual.setLocalTranslation(initialTranslationManual);
		// manual.setLocalScale(initialScaleManual);
		// manual.getRenderStates().hasLighting(true);	

		buildEnemyObjects(5);

		// Build World Axis Lines (X, Y, Z) in the center of the window
		wAxisX = new GameObject(GameObject.root(), wAxisLineShapeX);
		wAxisY = new GameObject(GameObject.root(), wAxisLineShapeY);
		wAxisZ = new GameObject(GameObject.root(), wAxisLineShapeZ);

		// Set world axis colors (red, green, blue) - X, Y, Z respectively
		wAxisX.getRenderStates().setColor(new Vector3f(1.0f, 0, 0));
		wAxisY.getRenderStates().setColor(new Vector3f(0, 1.0f, 0));
		wAxisZ.getRenderStates().setColor(new Vector3f(0, 0, 1.0f));

	}

	@Override
	public void initializeLights() {
		Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);
		light2 = new Light();
		light2.setLocation(new Vector3f(-5.0f, 4.0f, -2.0f));
		(engine.getSceneGraph()).addLight(light2);
	}

	@Override
	public void createViewports() {
		(engine.getRenderSystem()).addViewport("MAIN",0,0,1f,1f);
		(engine.getRenderSystem()).addViewport("RIGHT", .75f, 0, .25f, .25f);

		Viewport rightVp = (engine.getRenderSystem()).getViewport("RIGHT");

		myViewportCamera = rightVp.getCamera();
		rightVp.setHasBorder(true);
		rightVp.setBorderWidth(4);
		rightVp.setBorderColor(0.0f, 1.0f, 0.0f);

		myViewportCamera.setLocation(new Vector3f(0, 5, 0));
		myViewportCamera.setU(new Vector3f(1, 0, 0));
		myViewportCamera.setV(new Vector3f(0, 0, -1));
		myViewportCamera.setN(new Vector3f(0, -1, 0));
	}

	@Override
	public void initializeGame() {
		currFrameTime = System.currentTimeMillis();
		lastFrameTime = currFrameTime;
		elapsTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(WindowSizeX, WindowSizeY);

		inputManager = engine.getInputManager();
		String gamepadName = inputManager.getFirstGamepadName();
		// Get all our controllers and print their info: name, type
		ArrayList<Controller> controllers = inputManager.getControllers();
		for (Controller controller : controllers) {
			System.err.println("Controller: " + controller.getName());
			System.err.println("Type: " + controller.getType());
			// if (controller.getType().toString() == "Gamepad") {
			// 	gamepadName = controller.getName();
			// }
		}

		// ------------- positioning the camera -------------
		myCamera = engine.getRenderSystem().getViewport("MAIN").getCamera();
		// myCamera.setLocation(new Vector3f(0, 0, 5.0f));

		// CameraOrbit3D initialization
		orbitController = new CameraOrbit3D(myCamera, dol, gamepadName, engine);
		
		// Initialize our nodeControllers for each target object
		// NodeController rotController1 = new RotationController(engine, new Vector3f(0,1,0), 0.002f);
		// rotController1.addTarget(base);
		// controllerArr.add(rotController1);
		// NodeController rotController2 = new RotationController(engine, new Vector3f(0,1,0), 0.002f);
		// rotController2.addTarget(torus);
		// controllerArr.add(rotController2);
		// NodeController rotController3 = new RotationController(engine, new Vector3f(0,1,0), 0.002f);
		// rotController3.addTarget(sphere);
		// controllerArr.add(rotController3);
		// NodeController rotController4 = new RotationController(engine, new Vector3f(0,1,0), 0.002f);
		// rotController4.addTarget(plane);
		// controllerArr.add(rotController4);
		// put all object node controllers in an array for indexed access
		// when the objects are visited
		// for (NodeController n : controllerArr) {
		// 	(engine.getSceneGraph()).addNodeController(n);
		// }
		// Initialize a second node controller (dual axis stretch) on the pyramid manual object
		// NodeController stretchController1 = new StretchController(engine, 2.5f);
		// stretchController1.addTarget(manual);
		// stretchController1.enable();
		// (engine.getSceneGraph()).addNodeController(stretchController1);


		PitchActionK pitchUp = new PitchActionK(this, 0.0002f);
		PitchActionK pitchDown = new PitchActionK(this, -0.0002f);
		CameraPitchActionJ CameraPitchJ = new CameraPitchActionJ(this);
		PitchActionJ pitchJ = new PitchActionJ(this, 0.0004f);
		ForwardBackActionK moveForward = new ForwardBackActionK(this, 0.0004f);
		ForwardBackActionK moveBackward = new ForwardBackActionK(this, -0.0004f);
		ForwardBackActionJ moveJ = new ForwardBackActionJ(this, 0.0004f);
		YawActionK leftYaw = new YawActionK(this, 1);
		YawActionK rightYaw = new YawActionK(this, -1);
		YawActionK leftYawController = new YawActionK(this, 1);
		YawActionK rightYawController = new YawActionK(this, -1);
		// YawActionJ XYaw = new YawActionJ(this);
		RollActionK leftRoll = new RollActionK(this, -1);
		RollActionK rightRoll = new RollActionK(this, 1);

		// A2 New actions
		SecondaryViewportZoomActionK zoomOut = new SecondaryViewportZoomActionK(this, 0.0008f);
		SecondaryViewportZoomActionK zoomIn = new SecondaryViewportZoomActionK(this, -0.0008f);
		SecondaryViewportPanXActionK panLeft = new SecondaryViewportPanXActionK(this, -0.0008f);
		SecondaryViewportPanXActionK panRight = new SecondaryViewportPanXActionK(this, 0.0008f);
		SecondaryViewportPanYActionK panUp = new SecondaryViewportPanYActionK(this, -0.0008f);
		SecondaryViewportPanYActionK panDown = new SecondaryViewportPanYActionK(this, 0.0008f);

		// A3 New Actions
		FireMissileActionK fireMissile = new FireMissileActionK(this, 0.0005f);

		// Bind keyboard keys W, S, A, D, Q, E, UP, DOWN to their actions
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.UP,
				pitchUp,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.DOWN,
				pitchDown,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.W,
				moveForward,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.S,
				moveBackward,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.A,
				leftYaw,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.D,
				rightYaw,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.Q,
				leftRoll,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.E,
				rightRoll,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		//A2 New Actions
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.LBRACKET,
				zoomOut,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.RBRACKET,
				zoomIn,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.O,
				panLeft,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.P,
				panRight,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key._0,
				panUp,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.L,
				panDown,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// A3 New Actions
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.SPACE, 
				fireMissile, 
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// Now bind X, Y, YRot to joystick/game controller
		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.Y,
				pitchJ, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.RZ,
				CameraPitchJ, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._5,
				leftYawController, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._6,
				rightYawController, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.Z,
				moveJ, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		
		setupNetworking();
		printControls();
		// ((AnimatedShape)dolS).playAnimation("WALK", 0.5f, AnimatedShape.EndType.LOOP, 0);
	}

	private float getFramesPerSecond() {
		return (float) (frameCounter / elapsTime);
	}

	private String printVector3f(Vector3f v) {
		return String.format("x:%2.3f, y:%2.3f, z:%2.3f", v.x(), v.y(), v.z());
	}

	private void arrangeHUD() {
		// build and set HUD
		int elapsTimeSec = Math.round((float) elapsTime);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String counterStr = Integer.toString(counter);
		counterStr =  gameOver == 2 ? "Score = " + " You Lose!" : "Score = " + counterStr + " You Win!";
		String dispStr1 = "Time = " + elapsTimeStr + " " + counterStr;
		String dispStr2 = "Pos = " + printVector3f(dol.getWorldLocation());
		Vector3f hud1Color = new Vector3f(1, 0, 0);
		Vector3f hud2Color = new Vector3f(0, 0, 1);
		int hud1x, hud1y, hud2x, hud2y;
		float mainViewportAbsoluteLeft = engine.getRenderSystem().getViewport("MAIN").getActualLeft();
		// System.out.println("actual left: " + mainViewportAbsoluteLeft);
		float mainViewportAbsoluteBottom = engine.getRenderSystem().getViewport("MAIN").getActualBottom();
		// System.out.println("actual bottom: " + mainViewportAbsoluteBottom);
		float secondaryViewportAbsoluteLeft = engine.getRenderSystem().getViewport("RIGHT").getActualLeft();
		// System.out.println("viewport2 actual left: " + secondaryViewportAbsoluteLeft);
		float secondaryViewportAbsoluteBottom = engine.getRenderSystem().getViewport("RIGHT").getActualBottom();
		// System.out.println("viewport2 actual bottom: " + secondaryViewportAbsoluteBottom);

		hud1x = (int)(mainViewportAbsoluteLeft) + 10;
		hud1y = 10;
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, hud1x, hud1y);

		hud2x = (int)(secondaryViewportAbsoluteLeft) + 10;
		hud2y = 10;
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, hud2x, hud2y);
	}

	@Override
	public void update() {
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		if (!paused) {
			elapsTime += (currFrameTime - lastFrameTime) / 1000.0;
		}

		arrangeHUD();
		float elapsedFramesPerSecond = getFramesPerSecond();
		inputManager.update(elapsedFramesPerSecond);
		orbitController.updateCameraPosition();
		updateMovingObjects(elapsedFramesPerSecond);
		// ((AnimatedShape)dolS).updateAnimation();
		// updateDolphinScore();
		toggleNodeControllers();
		// avatarGroundCollision();
		protClient.sendRotationMessage(dol.getWorldRotation());
		protClient.sendMoveMessage(dol.getWorldLocation()); //TODO optimiz?e this message
		processNetworking((float)elapsTime);
		frameCounter++;
		// float heightOffset = groundPlane.getHeight(0, 0);
		Vector3f dolcoords = dol.getWorldLocation();
		// System.out.println(dolcoords.x() + " " + dolcoords.y() + " " + dolcoords.z());
	}

	@Override
	public void keyPressed(KeyEvent e) {
		Vector3f loc, fwd, newLocation;
		switch (e.getKeyCode()) {
			// case KeyEvent.VK_C:
			// counter++;
			// AddMagnetToManualObject();
			// break;
			case KeyEvent.VK_1:
				axesRenderState = !axesRenderState;
				if (axesRenderState) {
					wAxisX.getRenderStates().enableRendering();
					wAxisY.getRenderStates().enableRendering();
					wAxisZ.getRenderStates().enableRendering();
				} else {
					wAxisX.getRenderStates().disableRendering();
					wAxisY.getRenderStates().disableRendering();
					wAxisZ.getRenderStates().disableRendering();
				}
				break;
			case KeyEvent.VK_2:
				dol.getRenderStates().setWireframe(true);
				break;
			case KeyEvent.VK_3:
				dol.getRenderStates().setWireframe(false);
				break;
			case KeyEvent.VK_4:
				(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0, 0, 0));
				offDolphinCam = true;
				break;
			// Assignment A2, disable on/off dolphin Camera setting
			// Camera is now an OrbitController3D and is always off dolphin
			// case KeyEvent.VK_SPACE:
			// 	if (offDolphinCam) {
			// 		setOnDolphinCam();
			// 	} else {
			// 		setOffDolphinCam();
			// 	}
			// 	break;
		}
		super.keyPressed(e);
	}

	public Camera getMyCamera() {
		return myCamera;
	}

	public Camera getMyViewportCamera() {
		return myViewportCamera;
	}

	// For A2 this should always return false.
	public boolean onDolphinCam() {
		return !offDolphinCam;
	}

	// No calls are made to this method for A2
	public void setOnDolphinCam() {
		float hopOnDistance = -1.75f;
		float upDistance = 0.5f;
		location = dol.getWorldLocation();
		forward = dol.getWorldForwardVector();
		up = dol.getWorldUpVector();
		right = dol.getWorldRightVector();
		myCamera.setU(right);
		myCamera.setV(up);
		myCamera.setN(forward);
		myCamera.setLocation(location.add(up.mul(upDistance))
				.add(forward.mul(hopOnDistance)));

		offDolphinCam = false;
	}

	// No calls are made to this method for A2
	public void setOffDolphinCam() {
		float hopOffDistance = -2.0f;
		float upDistance = 1.0f;
		location = dol.getWorldLocation();
		forward = dol.getWorldForwardVector();
		up = dol.getWorldUpVector();
		right = dol.getWorldRightVector();
		myCamera.setU(right);
		myCamera.setV(up);
		myCamera.setN(forward);
		myCamera.setLocation(location.add(up.mul(upDistance))
				.add(forward.mul(hopOffDistance)));

		offDolphinCam = true;
	}

	public GameObject getDolphin() {
		return dol;
	}

	public void AddMagnetToManualObject(int n_magnet) {
		Matrix4f initialTranslationMagnet, initialScaleMagnet, initialRotationMagnet;
		// build the magnet object
		magnet = new GameObject(GameObject.root(), magnetS, metal);
		magnet.setParent(dol);
		magnet.propagateRotation(true);
		magnet.propagateTranslation(true);
		magnet.applyParentRotationToPosition(true);
		initialRotationMagnet = (new Matrix4f()).rotate(90.0f, new Vector3f(0.0f, 1.0f, 0.0f));
		initialTranslationMagnet = (new Matrix4f()).translation(
				0.0f, 0.02f - n_magnet* 0.01f, -(n_magnet * 0.04f + 0.4f));
		initialScaleMagnet = (new Matrix4f()).scaling(0.075f);
		magnet.setLocalRotation(initialRotationMagnet);
		magnet.setLocalTranslation(initialTranslationMagnet);
		magnet.setLocalScale(initialScaleMagnet);
		magnet.getRenderStates().hasLighting(true);
		
		NodeController magnetSpinController = new RotationController(engine, new Vector3f(0,1,0), 0.01f);
		magnetSpinController.addTarget(magnet);
		magnetSpinController.enable();
		engine.getSceneGraph().addNodeController(magnetSpinController);
	}

	public boolean checkDolphinNearObject(GameObject gObject) {
		Vector3d distanceToObj = new Vector3d(0, 0, 0);
		distanceToObj.x = (Math.abs(
				dol.getWorldLocation().x() - gObject.getWorldLocation().x()));
		distanceToObj.y = (Math.abs(
				dol.getWorldLocation().y() - gObject.getWorldLocation().y()));
		distanceToObj.z = (Math.abs(
				dol.getWorldLocation().z() - gObject.getWorldLocation().z()));

		double distance = distanceToObj.length();
		return (distance < 0.5) ? true : false;
	}

	private void updateDolphinScore() {
		// Check for each object (cub, torus, sphere, plane) and update visited state
		if (!visitedSites[0]) {
			visitedSites[0] = checkDolphinNearObject(base);
		}
		if (!visitedSites[1]) {
			visitedSites[1] = checkDolphinNearObject(torus);
		}
		if (!visitedSites[2]) {
			visitedSites[2] = checkDolphinNearObject(sphere);
		}
		if (!visitedSites[3]) {
			visitedSites[3] = checkDolphinNearObject(plane);
		}
		// Update the score and add magnet(s) if site(s) were visited
		int old_counter = counter;
		counter = (visitedSites[0] ? 1 : 0) + (visitedSites[1] ? 1 : 0)
				+ (visitedSites[2] ? 1 : 0) + (visitedSites[3] ? 1 : 0);
		if (counter > old_counter) {
			for (int i = old_counter; i < counter; ++i) {
				AddMagnetToManualObject(i);
			}
		}
	}

	private void toggleNodeControllers() {
		for (int i = 0; i < visitedSites.length; ++i) {
			if (visitedSites[i] && !controllerArr.get(i).isEnabled()) {
				controllerArr.get(i).enable();
			}
		}
	}

	public void fireMissile(float speed) {
		GameObject missileObject = new GameObject(GameObject.root(), missileShape, missile);
		// missileObject.setParent(GameObject.root());
		Vector3f dolLocation = dol.getWorldLocation();
		Vector3f dolDirection = dol.getLocalForwardVector();

		Matrix4f initialTranslation = (new Matrix4f()).translation(dolLocation.x(), dolLocation.y(), dolLocation.z());
		Matrix4f initialScale = (new Matrix4f()).scaling(0.1f);
		missileObject.setLocalTranslation(initialTranslation);
		missileObject.setLocalScale(initialScale);

		// missileObject.setLocation(dol.getWorldLocation());
		missileObject.setLocalRotation(dol.getLocalRotation());
		// missileObject.lookAt(dolDirection.x(), dolDirection.y(), dolDirection.z());
		movingObjects.add(missileObject);
		protClient.sendCreateMissileMessage(missileObject.getWorldLocation());
	}

	private void updateMovingObjects(float elapsedFramesPerSecond) {
		// First perform the scheduled object moves
		for (GameObject go: movingObjects) {
			go.moveForwardBack(0.001f*elapsedFramesPerSecond, new Vector3f());
			protClient.sendMoveMissileMessage(go.getWorldLocation());
			protClient.sendMissileRotationMessage(go.getWorldRotation());
		}
		for (GameObject go: movingEnemies) {
			go.lookAt(base);
			go.moveForwardBack(0.0001f*elapsedFramesPerSecond, new Vector3f());
			setObjectHeightAtLocation(go);
			if (go.getWorldLocation().sub(base.getWorldLocation()).length() < 0.1) {
				gameOver = 2;
			}
		}
		// Now we have to check for collisions
		ListIterator<GameObject> iterMoving = movingObjects.listIterator();
		while (iterMoving.hasNext()) {
			GameObject goMoving = iterMoving.next();
			ListIterator<GameObject> iterEnemies = movingEnemies.listIterator();
			while (iterEnemies.hasNext()) {
				GameObject goEnemy = iterEnemies.next();
				float distance = goEnemy.getWorldLocation().sub(goMoving.getWorldLocation()).length();
				// Remove objects that collided.
				if (distance < 0.2f) {
					GameObject.root().removeChild(goEnemy);
					GameObject.root().removeChild(goMoving);
					iterMoving.remove();
					iterEnemies.remove();
					engine.getSceneGraph().removeGameObject(goEnemy);
					engine.getSceneGraph().removeGameObject(goMoving);
				}
			}
			// TODO: Check if out of boundary and remove object
		} 
	}

	public void avatarGroundCollision() {
		Vector3f loc = dol.getWorldLocation();
		float height = groundPlane.getHeight(loc.x(), loc.z());
		Matrix4f initialTranslation = (new Matrix4f()).translation(0, 15.0f, 0);
		if (Math.abs(loc.y() - height) < 0.1 || (loc.y() - height < 0.1) ) {
			dol.setLocalTranslation(initialTranslation);
		}
	}

	public void setObjectHeightAtLocation(GameObject go) {
		Vector3f loc = go.getWorldLocation();
		float height = groundPlane.getHeight(loc.x(), loc.z());
		go.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));
	}


	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() { return dolS; }
	public TextureImage getGhostTexture() { return doltx; }
	public ObjShape getMissileShape() { return missileShape; }
	public TextureImage getMissileTexture() { return missile; }
	public ObjShape getGhostNPCShape() { return enemyShape; }
	public TextureImage getGhostNPCTexture() { return enemyTexture; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	
	private void setupNetworking()
	{	isClientConnected = false;	
		try 
		{	protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} 	catch (UnknownHostException e) 
		{	e.printStackTrace();
		}	catch (IOException e) 
		{	e.printStackTrace();
		}
		if (protClient == null)
		{	System.out.println("missing protocol host");
		}
		else
		{	// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}
	
	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() { return dol.getWorldLocation(); }

	public void setIsConnected(boolean value) { this.isClientConnected = value; }
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{	protClient.sendByeMessage();
			}
		}
	}

	public void updateGhost(GameObject go) {
		protClient.sendMoveMessage(go.getWorldLocation());
	}

	private void printControls() {
		System.out.println("****************************************");
		System.out.println("Gabriele Nicula, CSC 165 - Assignment #3");
		System.out.println("****************************************\n");
		System.out.println("**************************");
		System.out.println("Gamepad and Key Bindings:");
		System.out.println("**************************\n");
		System.out.println("\tMove Forward:\t\t\t\tW, Gamepad Left Joystick Y-axis up");
		System.out.println("\tMove Backward:\t\t\t\tS, Gamepad Left Joystick Y-axis down");
		System.out.println("\tYaw Left:\t\t\t\tA, Gamepad Left Joystick X-axis left");
		System.out.println("\tYaw Right:\t\t\t\tD, Gamepad Left Joystick X-axis right");
		System.out.println("\tRoll Left:\t\t\t\tQ");
		System.out.println("\tRoll Right:\t\t\t\tE");
		System.out.println("\tPitch Up:\t\t\t\tUp Arrow, Gamepad Button 0");
		System.out.println("\tPitch Down:\t\t\t\tDown Arrow, Gamepad Button 1");
		System.out.println("\tCamera Rotate Left:\t\t\tGamepad Right Joystick RX-axis left");
		System.out.println("\tCamera Rotate Right:\t\t\tGamepad Right Joystick RX-axis right");
		System.out.println("\tCamera Elevation Up:\t\t\tGamepad Right Joystick RY-axis up");
		System.out.println("\tCamera Elevation Down:\t\t\tGamepad Right Joystick RY-axis down");
		System.out.println("\tCamera Zoom In:\t\t\t\tGamepad Button 2");
		System.out.println("\tCamera Zoom Out:\t\t\tGamepad Button 3");
		System.out.println("\tPan Mini-Map Up:\t\t\t0");
		System.out.println("\tPan Mini-Map Right:\t\t\tP");
		System.out.println("\tPan Mini-Map Down:\t\t\tL");
		System.out.println("\tPan Mini-Map Right:\t\t\tO");
		System.out.println("\tMini-Map Zoom In:\t\t\t]");
		System.out.println("\tMini-Map Zoom Out:\t\t\t[");
		System.out.println("\tToggle World Axes On/Off:\t\t1");
		System.out.println("\tDolphin Wireframe On:\t\t\t2");
		System.out.println("\tDolphin Wireframe Off:\t\t\t3");
		System.out.println("\tExit Game:\t\t\t\tESC");
	}
}
