

package a2;

import tage.*;
import tage.shapes.*;
import tage.input.InputManager; // input management
import tage.input.action.*;
import tage.networking.IGameConnection.ProtocolType;
import tage.nodeControllers.RotationController;
import tage.nodeControllers.StretchController;
import net.java.games.input.Controller;
import tage.audio.*;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.JBullet.*;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
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

/** Houses main game loop and logic.
 * @author Gabriele Nicula, Keegan Rhoads
 * @return
 */
public class MyGame extends VariableFrameRateGame {
	private static Engine engine;

	private boolean paused = false;
	private boolean offDolphinCam = true; // default camera off dolphin
	private boolean axesRenderState = true;
	private boolean inSelectionScreen = true;
	private boolean[] visitedSites = new boolean[4]; // default initialized to false
	private int counter = 0;
	private int gameOver = 0;
	private int frameCounter = 0;
	private int remainingMissiles = 5;
	private int remainingMarkers = 5;
	private int skyBoxID;
	private int selectedAvatar = 1;
	private double lastFrameTime, currFrameTime, elapsTime;
	private ArrayList<GameObject> movingObjects = new ArrayList<GameObject>();
	private ArrayList<GameObject> movingBullets = new ArrayList<GameObject>();
	private ArrayList<GameObject> movingEnemies = new ArrayList<GameObject>();
	private ArrayList<GameObject> movingMarkers = new ArrayList<GameObject>();
	private ArrayList<GameObject> laserMarkers = new ArrayList<GameObject>();
	private AnimatedShape enemyShape;
	private GameObject dol, selectAvatar1, selectAvatar2, base, torus, sphere, sphereSatellite, plane, groundPlane,
			wAxisX, wAxisY, wAxisZ, manual, magnet, missileObj, tower, reloadingStation, bullet, marker, laser, targeter;
	private ObjShape dolS, planeS, sphereS, groundPlaneS, wAxisLineShapeX, wAxisLineShapeY, 
			wAxisLineShapeZ, manualS, magnetS, worldObj, missileShape, towerS, bulletS, markerS, laserS, targeterS;
	private TextureImage doltx1, doltx2, brick, grass, red, assignt, enemyTexture, metal, water, 
			torusWater, fur, terrainTexture, terrainHeightMap, missile, towerTexture, tracer, bombTex, laserTex;
	private Light light1, light2, sphereLight;
	private Camera myCamera, myViewportCamera;
	private CameraOrbit3D orbitController;
	private NodeController selectionRotateController;
	private ArrayList<NodeController> controllerArr = new ArrayList<NodeController>();
	private InputManager inputManager;
	private Vector3f location; // world object location
	private Vector3f forward; // n-vector/z-axis
	private Vector3f up; // v-vector/y-axis
	private Vector3f right; // u-vector/x-axis

	private IAudioManager audioMgr;
	private Sound bugChitterSound, selectAvatarSound, cannonSound, jetIdleSound, jetABSound, missileSound, comonaBGMSound,
			zeroBGMSound;
	public boolean isAfterBurnerOn = false;

	private PhysicsEngine physicsEngine;
	private PhysicsObject markerP, planeP;
	private ArrayList<PhysicsObject> groundTiles = new ArrayList<PhysicsObject>();
	public double[] tempTransform;

	private float mass, radius, height;
	private float[] upPhys = {0,1,0};
	private float[] gravity = {0f, -5f, 0f};

	private boolean running = true;
	private float vals[] = new float[16];

	private GhostManager gm;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	private float bugHeightAdjust = 0.5f;

	private final int WindowSizeX = 2000;
	private final int WindowSizeY = 1000;

	/**
	 * Seys up the server and {@link GhostManager}.
	 * @param serverAddress
	 * @param serverPort
	 * @param protocol
	 * 
	 */
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
		sphereS = new Sphere();
		laserS = new ImportedModel("beam.obj");
		markerS = new ImportedModel("marker.obj");
		towerS = new ImportedModel("towertest.obj");
		dolS = new ImportedModel("f15.obj");
		bulletS = new ImportedModel("bullet.obj");
		enemyShape = new AnimatedShape("bugarino.rkm", "bugarino.rks");
		enemyShape.loadAnimation("WALK", "bugarino_walk.rka");
		enemyShape.loadAnimation("IDLE", "bugarino_idle.rka");
		System.out.println("about to load terrain shape");
		groundPlaneS = new TerrainPlane(1000);
		System.out.println("loaded terrain shape");
		manualS = new MyManualObject();
		magnetS = new MyMagnetObject();
		missileShape = new ImportedModel("asca.obj");


		float lineLength = 1.0f;
		Vector3f worldOrigin = new Vector3f(0f, 0f, 0f);
		Vector3f lineX = new Vector3f(lineLength, 0f, 0f);
		Vector3f lineY = new Vector3f(0f, lineLength, 0f);
		Vector3f lineZ = new Vector3f(0f, 0f, -lineLength);
		Vector3f targeterLine = new Vector3f(0f, 0f, 500f);

		wAxisLineShapeX = new Line(worldOrigin, lineX);
		wAxisLineShapeY = new Line(worldOrigin, lineY);
		wAxisLineShapeZ = new Line(worldOrigin, lineZ);
		targeterS = new Line(worldOrigin, targeterLine);
	}

	@Override
	public void loadTextures() {
		laserTex = new TextureImage("laser.jpg");
		bombTex = new TextureImage("bomb.jpg");
		tracer = new TextureImage("tracer.jpg");
		doltx1 = new TextureImage("F15A.jpg");
		doltx2 = new TextureImage("F15F.jpg");
		towerTexture = new TextureImage("towertexturetest.jpg");
		grass = new TextureImage("grass1.jpg");
		red = new TextureImage("red.jpg");
		assignt = new TextureImage("assign1.png");
		enemyTexture = new TextureImage("bug_uv.jpg");
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

	@Override
	public void loadSounds() {
		AudioResource bugSoundResource;
		audioMgr = engine.getAudioManager();
		// https://pixabay.com/sound-effects/critters-creeping-32760/ "Critters creeping" - Pixabay
		bugSoundResource = audioMgr.createAudioResource("assets/sounds/creepy_alien.wav", AudioResourceType.AUDIO_SAMPLE);
		bugChitterSound = new Sound(bugSoundResource, SoundType.SOUND_EFFECT, 25, true);
		bugChitterSound.initialize(audioMgr);
		bugChitterSound.setMaxDistance(5.0f);
		bugChitterSound.setMinDistance(1.0f);
		bugChitterSound.setRollOff(100.0f);

		// https://opengameart.org/content/toom-click - CC BY 4.0 DEED
		AudioResource selectAvatarSoundResource = audioMgr.createAudioResource(
			"assets/sounds/toom_click.wav", AudioResourceType.AUDIO_SAMPLE);
		selectAvatarSound = new Sound(
			selectAvatarSoundResource, SoundType.SOUND_EFFECT, 35, false);
		selectAvatarSound.initialize(audioMgr);

		// https://www.youtube.com/watch?v=SN4jfxckQiM "M61 Vulcan  BRRRRRT Sound" - Century
		AudioResource cannonSoundResource = audioMgr.createAudioResource("assets/sounds/cannon.wav",
			AudioResourceType.AUDIO_SAMPLE);
		cannonSound = new Sound(
			cannonSoundResource, SoundType.SOUND_EFFECT, 30, false);
		cannonSound.initialize(audioMgr);

		// https://www.youtube.com/watch?v=jQIwmKEZby4 "F-15 VARIABLE AREA NOZZLE" - KOSKEI AEROSPACE
		AudioResource jetIdleSoundResource = audioMgr.createAudioResource("assets/sounds/jetIdle.wav",
			AudioResourceType.AUDIO_SAMPLE);
		jetIdleSound = new Sound(
			jetIdleSoundResource, SoundType.SOUND_EFFECT, 10, false);
		jetIdleSound.initialize(audioMgr);

		// https://www.youtube.com/watch?v=TBi3sUvmOd4 " F-15 & F-16 Full Afterburner Test on the Ground (Afterburner Run)" - USA Military Channel
		AudioResource jetABSoundResource = audioMgr.createAudioResource("assets/sounds/jetAB.wav",
			AudioResourceType.AUDIO_SAMPLE);
		jetABSound = new Sound(
			jetABSoundResource, SoundType.SOUND_EFFECT, 12, false);
		jetABSound.initialize(audioMgr);

		// https://soundbible.com/1794-Missle-Launch.html "Missle Launch" -  Kibblesbob
		AudioResource missileSoundResource = audioMgr.createAudioResource("assets/sounds/missileSound.wav",
			AudioResourceType.AUDIO_SAMPLE);
		missileSound = new Sound(
			missileSoundResource, SoundType.SOUND_EFFECT, 10, false);
		missileSound.initialize(audioMgr);

		// https://pixabay.com/music/metal-melodic-metal-186403/ "Melodic Metal" - AudioDollar
		AudioResource comonaBGMResource = audioMgr.createAudioResource("assets/sounds/melodicMetal.wav",
			AudioResourceType.AUDIO_SAMPLE);
		comonaBGMSound = new Sound(
			comonaBGMResource, SoundType.SOUND_MUSIC, 15, true);
		comonaBGMSound.initialize(audioMgr);

		// https://pixabay.com/music/rock-crag-hard-rock-14401/ "Crag - Hard Rock" AlexGrohl
		AudioResource zeroBGMResource = audioMgr.createAudioResource("assets/sounds/cragHR.wav",
			AudioResourceType.AUDIO_SAMPLE);
		zeroBGMSound = new Sound(
			zeroBGMResource, SoundType.SOUND_MUSIC, 15, true);
		zeroBGMSound.initialize(audioMgr);
	}

	public void buildEnemyObjects(int numEnemies) {
		Matrix4f initialTranslation, initialScale;
		for (int i = 0; i < numEnemies; ++i) {
			GameObject enemy = new GameObject(GameObject.root(), enemyShape, enemyTexture);
			double ranAngle = Math.random() * 360;
			float ranX = (float)Math.cos(ranAngle) * 20.0f;
			float ranZ = (float)Math.sin(ranAngle) * 20.0f;
			initialScale = (new Matrix4f()).scale(0.25f);
			enemy.setLocalScale(initialScale);
			initialTranslation = (new Matrix4f()).translation(ranX, bugHeightAdjust, ranZ);
			enemy.setLocalTranslation(initialTranslation);
			enemy.getRenderStates().setModelOrientationCorrection(new Matrix4f().rotationY((float)java.lang.Math.toRadians(-90.0f)));
			//enemy.getRenderStates().hasLighting(true);
			//enemy.getRenderStates().isEnvironmentMapped(true);
			movingEnemies.add(enemy);
		}
	}

	@Override
	public void buildObjects() {
		// build the ground plane on X-Z
		System.out.println("about to create terain object");
		groundPlane = new GameObject(GameObject.root(), groundPlaneS, terrainTexture);
		System.out.println("created terain object");
		groundPlane.setHeightMap(terrainHeightMap);
		Matrix4f initialScaleT = (new Matrix4f()).scaling(20.0f);
		groundPlane.setLocalScale(initialScaleT);
		Matrix4f initialTranslationT = (new Matrix4f()).translation(0, 0, 0.0f);
		groundPlane.setLocalTranslation(initialTranslationT);
		groundPlane.setIsTerrain(true);

		// build avatar selection
		selectAvatar1 = new GameObject(GameObject.root(), dolS, doltx1);
		selectAvatar1.setLocalTranslation((new Matrix4f()).translation(-0.5f, 15.25f, 0.25f));
		selectAvatar1.setLocalScale((new Matrix4f()).scaling(0.1f));
		selectAvatar1.getRenderStates()
				.setModelOrientationCorrection((new Matrix4f()).rotationY((float) java.lang.Math.toRadians(90.0f)));

		selectAvatar2 = new GameObject(GameObject.root(), dolS, doltx2);
		selectAvatar2.setLocalTranslation((new Matrix4f()).translation(0.5f, 15.25f, 0.25f));
		selectAvatar2.setLocalScale((new Matrix4f()).scaling(0.1f));
		selectAvatar2.getRenderStates()
				.setModelOrientationCorrection((new Matrix4f()).rotationY((float) java.lang.Math.toRadians(270.0f)));

		// build dolphin in the center of the window
		dol = new GameObject(GameObject.root(), dolS, doltx1);
		Matrix4f initialTranslationD = (new Matrix4f()).translation(0f, 15.0f, 0f);
		// TODO: check why pitch and roll enables a visualization bug after
		// long playing times.
		Matrix4f initialScaleD = (new Matrix4f()).scaling(0.05f);
		dol.setLocalTranslation(initialTranslationD);
		dol.setLocalScale(initialScaleD);

		// build our base at the center of the map.
		base = new GameObject(GameObject.root(), towerS, towerTexture);
		Matrix4f initialTranslationB = (new Matrix4f()).translation(0f, 10.5f, 0.5f);
		Matrix4f initialScaleB = (new Matrix4f()).scaling(0.10f);
		base.setLocalTranslation(initialTranslationB);
		base.setLocalScale(initialScaleB);

		// build sphere for lights on base
		sphere = new GameObject(GameObject.root(), sphereS, red);
		Matrix4f initialTranslationSp = (new Matrix4f()).translation(0.5f, 5.12f, 0.295f);
		Matrix4f initialScaleSp = (new Matrix4f()).scaling(0.08f);
		sphere.setLocalTranslation(initialTranslationSp);
		sphere.setLocalScale(initialScaleSp);
		// Create a hierarchical system
		sphere.setParent(base);
		sphere.propagateTranslation(true);
		sphere.propagateRotation(false);

		// build a reloading station for missiles.
		reloadingStation = new GameObject(GameObject.root(), missileShape, missile);
		Matrix4f initialTranslationM = (new Matrix4f()).translation(15.0f, 15.0f, 15.0f);
		Matrix4f initialScaleM = (new Matrix4f()).scaling(0.25f);
		reloadingStation.setLocalTranslation(initialTranslationM);
		reloadingStation.setLocalScale(initialScaleM);

		buildEnemyObjects(5);

		// Build World Axis Lines (X, Y, Z) in the center of the window
		wAxisX = new GameObject(GameObject.root(), wAxisLineShapeX);
		wAxisY = new GameObject(GameObject.root(), wAxisLineShapeY);
		wAxisZ = new GameObject(GameObject.root(), wAxisLineShapeZ);
		targeter = new GameObject(dol, targeterS);
		targeter.applyParentRotationToPosition(true);

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

		sphereLight = new Light();
		sphereLight.setLocation(sphere.getWorldLocation());
		sphereLight.setType(Light.LightType.POSITIONAL);
		sphereLight.setSpecular(1.0f, 0f, 0.5f);
		sphereLight.setDiffuse(1.0f, 0f, 0.5f);
		Vector3f beaconDirection = new Vector3f(0f, -1.0f, -1.0f);
		sphereLight.setDirection(beaconDirection);
		sphereLight.setCutoffAngle(20.0f);
		sphereLight.setConstantAttenuation(0f);
		sphereLight.setLinearAttenuation(0.25f);
		(engine.getSceneGraph()).addLight(sphereLight);
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
		enemyShape.playAnimation("WALK", 0.2f, AnimatedShape.EndType.LOOP, 0);
		inputManager = engine.getInputManager();
		String gamepadName = inputManager.getFirstGamepadName();
		// Get all our controllers and print their info: name, type
		ArrayList<Controller> controllers = inputManager.getControllers();
		for (Controller controller : controllers) {
			System.err.println("Controller: " + controller.getName());
			System.err.println("Type: " + controller.getType());
		}

		// ---------- Setting up physics ----------
		
		physicsEngine = (engine.getSceneGraph()).getPhysicsEngine();
		physicsEngine.setGravity(gravity);
		// --- create physics world ---
		Matrix4f translation = (new Matrix4f()).translate(0, 10, 0);
		tempTransform = toDoubleArray(translation.get(vals));
		planeP = (engine.getSceneGraph()).addPhysicsStaticPlane(
		tempTransform, upPhys, 0.0f);
		planeP.setBounciness(.8f);
		// groundPlane.setPhysicsObject(planeP);

		// For debugging physics
		//engine.enableGraphicsWorldRender();
		//engine.enablePhysicsWorldRender();

		// ---------- Setting up sound -----------
		setEarParameters();
		for (int i = 0; i < movingEnemies.size(); ++i) {
			bugChitterSound.setLocation(movingEnemies.get(i).getLocalLocation());
			bugChitterSound.play();
		}

		// ------------- positioning the camera -------------
		myCamera = engine.getRenderSystem().getViewport("MAIN").getCamera();

		// CameraOrbit3D initialization
		orbitController = new CameraOrbit3D(myCamera, dol, gamepadName, engine);
		
		// Initialize our nodeControllers for each target object
		selectionRotateController = new RotationController(engine, new Vector3f(0,1,1), 0.001f);
		selectionRotateController.addTarget(selectAvatar1);
		selectionRotateController.enable();
		(engine.getSceneGraph()).addNodeController(selectionRotateController);

		NodeController rotController1 = new RotationController(engine, new Vector3f(0,1,0), 0.002f);
		rotController1.addTarget(reloadingStation);
		controllerArr.add(rotController1);
		rotController1.enable();
		for (NodeController n : controllerArr) {
			(engine.getSceneGraph()).addNodeController(n);
		}

		PitchActionK pitchUp = new PitchActionK(this, 0.0002f);
		PitchActionK pitchDown = new PitchActionK(this, -0.0002f);
		CameraPitchActionJ CameraPitchJ = new CameraPitchActionJ(this);
		PitchActionJ pitchJ = new PitchActionJ(this, 0.0004f);
		ForwardBackActionK moveForward = new ForwardBackActionK(this, 0.0003f);
		ForwardBackActionK moveBackward = new ForwardBackActionK(this, -0.0002f);
		ForwardBackActionJ moveJ = new ForwardBackActionJ(this, 0.0004f);
		YawActionK leftYaw = new YawActionK(this, 1);
		YawActionK rightYaw = new YawActionK(this, -1);
		RollActionK leftRoll = new RollActionK(this, -1);
		RollActionK rightRoll = new RollActionK(this, 1);
		RollActionJ rollJ = new RollActionJ(this, 0.0004f);
		

		// A2 New actions
		SecondaryViewportZoomActionK zoomOut = new SecondaryViewportZoomActionK(this, 0.0008f);
		SecondaryViewportZoomActionK zoomIn = new SecondaryViewportZoomActionK(this, -0.0008f);
		SecondaryViewportPanXActionK panLeft = new SecondaryViewportPanXActionK(this, -0.0008f);
		SecondaryViewportPanXActionK panRight = new SecondaryViewportPanXActionK(this, 0.0008f);
		SecondaryViewportPanYActionK panUp = new SecondaryViewportPanYActionK(this, -0.0008f);
		SecondaryViewportPanYActionK panDown = new SecondaryViewportPanYActionK(this, 0.0008f);

		// A3 New Actions
		FireMissileActionK fireMissile = new FireMissileActionK(this, 0.0005f);
		FireBulletActionK fireBullet = new FireBulletActionK(this, .5f);
		DropMarkerActionK dropMarker = new DropMarkerActionK(this, .0005f);
		CycleAvatarSelectionAction cycleAvatar = new CycleAvatarSelectionAction(this);
		SelectAvatarAction selectAvatar = new SelectAvatarAction(this);

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
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.Z, 
				cycleAvatar, 
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.X, 
				selectAvatar, 
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.C,
				fireBullet,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.V,
				dropMarker,
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		
		
		// Now bind X, Y, YRot to joystick/game controller
		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.Y,
				pitchJ, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.RZ,
				CameraPitchJ, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._4,
				leftYaw, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._5,
				rightYaw, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.Z,
				moveJ, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.X,
				rollJ, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._0,
				fireMissile, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._1,
				fireBullet, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		inputManager.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._7,
				dropMarker, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		setupNetworking();
		printControls();
	}
	/** Sets up {@link IAudioManager} parameters for the Avatar.
	 * 
	*/
	public void setEarParameters()
	{
		myCamera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		audioMgr.getEar().setLocation(dol.getWorldLocation());
		audioMgr.getEar().setOrientation(myCamera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
	}
	/** Returns frames per second based on how many frames have occurred in the elapsed time. @return*/
	private float getFramesPerSecond() {
		return (float) (frameCounter / elapsTime);
	}

	/** String formatting for a {@link Vector3f} to truncate it for the hud elements.
	 * 
	 * @param v
	 * @return
	*/
	private String printVector3f(Vector3f v) {
		return String.format("x:%2.3f, y:%2.3f, z:%2.3f", v.x(), v.y(), v.z());
	}
	/** Function to lay out all of the hud elements.
	 * 
	 * @param elapsedFramesPerSecond
	 *
	*/
	private void arrangeHUD(float elapsedFramesPerSecond) {
		// build and set HUD
		int elapsTimeSec = Math.round((float) elapsTime);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String counterStr = Integer.toString(counter);
		counterStr =  gameOver == 2 ? "Score = " + " You Lose!" : "Score = " + counterStr + " You Win!";
		// String dispStr1 = "Time = " + elapsTimeStr + " " + elapsedFramesPerSecond;
		String dispStr1 = "Time: " + elapsTimeStr + " Missiles: " + remainingMissiles + " Markers: " + remainingMarkers;
		String dispStr2 = "Pos: " + printVector3f(dol.getWorldLocation());
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
		float elapsedFramesPerSecond = getFramesPerSecond();

		if (inSelectionScreen) {
			inputManager.update(elapsedFramesPerSecond);
			// processNetworking((float)elapsTime);
			Vector3f hudSelectColor = new Vector3f(0, 1, 0);
			String selDispStr = "Press 'x' to accept. Press 'z' to cycle through available avatars. Current avatar = ";
			Viewport selViewport = (engine.getRenderSystem()).getViewport("MAIN");
			if (selectedAvatar == 1) {
				(engine.getHUDmanager()).setHUD2(selDispStr + "1", hudSelectColor, (int) selViewport.getRelativeLeft(),
						(int) selViewport.getRelativeBottom());
			} else if (selectedAvatar == 2) {
				(engine.getHUDmanager()).setHUD2(selDispStr + "2", hudSelectColor, (int) selViewport.getRelativeLeft(),
						(int) selViewport.getRelativeBottom());
			}
		} else {
			Vector3f dolcoords = dol.getWorldLocation();
			Vector3f dolFwd = dol.getLocalForwardVector();
			Vector3f newLocation = dolcoords.add(dolFwd.mul(0.0006f * elapsedFramesPerSecond));
			dol.setLocalLocation(newLocation);
			arrangeHUD(elapsedFramesPerSecond);
			inputManager.update(elapsedFramesPerSecond);
			orbitController.updateCameraPosition();
			if (isAfterBurnerOn) {
				if (jetABSound.getIsPlaying() == false) {
					jetABSound.setLocation(dolcoords);
					jetIdleSound.stop();
					jetABSound.play();
				}
			} else {
				if (jetIdleSound.getIsPlaying() == false) {
					jetIdleSound.setLocation(dolcoords);
					jetABSound.stop();
					jetIdleSound.play();
				}
			}

			if (selectedAvatar == 1) {
				// comonaBGMSound.setLocation(dol.getWorldLocation());
				if (comonaBGMSound.getIsPlaying() == false)
				{
					comonaBGMSound.play();
				}
			} else if (selectedAvatar == 2) {
				if (zeroBGMSound.getIsPlaying() == false)
				{
					zeroBGMSound.play();
				}
			}
			checkForMissileReloading();
			updateMovingObjects(elapsedFramesPerSecond);
			updateMovingBullets(elapsedFramesPerSecond);
			
			// avatarGroundCollision();
			protClient.sendRotationMessage(dol.getWorldRotation());
			protClient.sendMoveMessage(dol.getWorldLocation()); //TODO optimiz?e this message
			processNetworking((float)elapsTime);
			frameCounter++;

			// update physics
			if (running) {
				AxisAngle4f aa = new AxisAngle4f();
				Matrix4f mat = new Matrix4f();
				Matrix4f mat2 = new Matrix4f().identity();
				Matrix4f mat3 = new Matrix4f().identity();
				checkForCollisions();

				physicsEngine.update((float)elapsTime);
				for (GameObject go:engine.getSceneGraph().getGameObjects()) { 
					if (go.getPhysicsObject() != null) { 
						// set translation
						mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
						mat2.set(3,0,mat.m30());
						mat2.set(3,1,mat.m31());
						mat2.set(3,2,mat.m32());
						go.setLocalTranslation(mat2);
						// set rotation
						mat.getRotation(aa);
						mat3.rotation(aa);
						go.setLocalRotation(mat3);
					} 
				}
			} 
		

			// update sound
			setEarParameters();
			for (int i = 0; i < movingEnemies.size(); ++i) {
				bugChitterSound.setLocation(movingEnemies.get(i).getLocalLocation());
			}
			audioMgr.getEar().setLocation(dol.getWorldLocation());
			audioMgr.getEar().setOrientation(myCamera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		Vector3f loc, fwd, newLocation;
		switch (e.getKeyCode()) {
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
			case KeyEvent.VK_5:
				System.out.println("starting physics");
				running = true;
				break;
			case KeyEvent.VK_6:
				System.out.println("Turning off Tower Light");
				sphereLight.setLinearAttenuation(10f);
				break;
			case KeyEvent.VK_7:
				System.out.println("Turning on Tower Light");
				sphereLight.setLinearAttenuation(0.25f);
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
	/** Returns the main {@link Camera} object.
	 * 
	 * @return
	*/
	public Camera getMyCamera() {
		return myCamera;
	}
	/** Returns the main viewport camera object.
	 * 
	 * @return
	*/
	public Camera getMyViewportCamera() {
		return myViewportCamera;
	}

	// For A2 this should always return false.
	/** For dismounting and mounting the dolphin.
	 * Returns a boolean.
	 * 
	 * @return
	 */
	public boolean onDolphinCam() {
		return !offDolphinCam;
	}

	// No calls are made to this method for A2
	/**  Sets the camera in a fixed position above and behind the dolphin if the avatar is mounted.
	 * 
	 * 
	*/
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
	/**  Sets the camera in a fixed position above and behind the dolphin when the Avatar dismounts.
	 *
	 * The camera will no longer follow the dolphin.
	 * 	
	 * 
	 */
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
	/** Returns the main avatar game object (internally referred to as "dolphin")
	 * 
	 */
	public GameObject getDolphin() {
		return dol;
	}

	/** Used when the dolphin collects magnets. Adds them as children to the manual object required in A2.
	 * 
	 * @param n_magnet
	 *
	 */
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

	/** Used when the dolphin is close to magnets to check if they should be picked up.
	 * 
	 * 
	 * @param gObject
	 * @return
	 */
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
	/** Updates the dolphin score based on whether they have visited specific sites in A2.
	 * 
	 * 
	 * 
	 */
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

	/** Toggles node controllers required in A2.
	 * 
	 * 
	 * 
	 */
	private void toggleNodeControllers() {
		for (int i = 0; i < visitedSites.length; ++i) {
			if (visitedSites[i] && !controllerArr.get(i).isEnabled()) {
				controllerArr.get(i).enable();
			}
		}
	}
	/** Creates laser objects, usually when a laserMarker has been created. This laser can damage enemies.
	 * @param loc
	 * 
	*/
	public void createLaserObjects(Vector3f loc) {
		GameObject laser = new TemporaryGameObject(GameObject.root(), laserS, laserTex);
		Matrix4f initialTranslation = (new Matrix4f()).translation(
			loc.x(), loc.y(), loc.z());
		Matrix4f initialScale = (new Matrix4f()).scaling(.8f);
		laser.setLocalTranslation(initialTranslation);
		laser.setLocalScale(initialScale);
		laserMarkers.add(laser);
	}
	/** Creates a marker physics object that inherits the avatar's location and direction.
	 * If the player has remaining markers, drop a marker. Otherwise, do nothing. Does not damage enemies on its own.
	 * @param speed
	 * 
	 */
	public void dropMarker(float speed) {
		if (remainingMarkers > 0) {
			GameObject markerObject = new TemporaryGameObject(GameObject.root(), markerS, bombTex);
			// missileObject.setParent(GameObject.root());
			Vector3f dolLocation = dol.getWorldLocation();
			Vector3f dolDirection = dol.getLocalForwardVector();
	
			Matrix4f initialTranslation = (new Matrix4f()).translation(
				dolLocation.x(), dolLocation.y()-0.02f, dolLocation.z());
			Matrix4f initialScale = (new Matrix4f()).scaling(0.07f);
			markerObject.setLocalTranslation(initialTranslation);
			markerObject.setLocalScale(initialScale);
			
			markerObject.setLocalRotation(dol.getLocalRotation());
			movingMarkers.add(markerObject);
			// markerObject.getRenderStates().setModelOrientationCorrection(
			// 	(new Matrix4f()).rotationY((float)java.lang.Math.toRadians(90.0f)));

			// Convert avatar's transform to double array
			// tempTransform = toDoubleArray(initialTranslation.rotateY(
			// 	(float)java.lang.Math.toRadians(270.0f)).get(vals));
			tempTransform = toDoubleArray(initialTranslation.get(vals));
			mass = 1.5f;
			radius = 0.1f;
			height = 0.35f;
			markerP = (engine.getSceneGraph()).addPhysicsCapsuleZ(mass, tempTransform, radius, height);
			markerP.setBounciness(0.1f);
			markerObject.setPhysicsObject(markerP);

			// Now create the terrain counterpart physics object
			tempTransform = toDoubleArray((new Matrix4f()).translation(dolLocation.x(),
				groundPlane.getHeight(dolLocation.x(), dolLocation.z()), dolLocation.z()).get(vals));

			// public PhysicsObject addPhysicsCylinder(float mass, double[] transform, float radius, float height)			
			PhysicsObject cylinderP = (engine.getSceneGraph()).addPhysicsCylinder(
				0.0f, tempTransform, 2.5f*radius, height/5.0f);
			groundTiles.add(cylinderP);
			Vector3f markerLoc = markerObject.getWorldLocation();
			protClient.sendCreateMarkerMessage(markerObject.getWorldLocation());

			// --remainingMarkers;
		}
	}

	/** Creates a fast moving projectile originating at the avatar. This missile can collide and destroy enemies. Inherits avatars direction and location.
	 * Collisons are handled via {@link updateMovingObjects}.
	 * @param speed
	 * 
	*/
	public void fireMissile(float speed) {
		if (remainingMissiles > 0) {
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

			missileSound.setLocation(dolLocation);
			missileSound.play();

			protClient.sendCreateMissileMessage(missileObject.getWorldLocation());
			--remainingMissiles;
		}
	}
	/** Creates many fast moving projectiles originating at the avatar. These bullets can collide and destroy enemies. Inherits avatars direction and location.
	 * Collisons are handled via {@link updateMovingObjects}.
	 * @param speed
	 * 
	*/
	public void fireBullet(float speed) {
		GameObject bulletObject = new GameObject(GameObject.root(), bulletS, tracer);
		Vector3f dolLocation = dol.getWorldLocation();
		Vector3f dolDirection = dol.getLocalForwardVector();

		Matrix4f initialTranslation = (new Matrix4f()).translation(dolLocation.x(), dolLocation.y(), dolLocation.z());
		Matrix4f initialScale = (new Matrix4f()).scaling(.3f);
		bulletObject.setLocalTranslation(initialTranslation);
		bulletObject.setLocalScale(initialScale);
		bulletObject.setLocalRotation(dol.getLocalRotation());
		movingBullets.add(bulletObject);
		protClient.sendCreateBulletMessage(bulletObject.getWorldLocation());

		cannonSound.setLocation(dol.getWorldLocation());
		cannonSound.play();
	}
	/** Handles moving bullets and sends corresponding messages to the server to render them for other clients.
	 * @param elapsedFramesPerSecond
	 * 
	*/
	private void updateMovingBullets(float elapsedFramesPerSecond) {
		// First perform the scheduled object moves
		for (GameObject go: movingBullets) {
			go.moveForwardBack(0.007f*elapsedFramesPerSecond, new Vector3f());
			protClient.sendMoveBulletMessage(go.getWorldLocation());
			protClient.sendBulletRotationMessage(go.getWorldRotation());
			
		} 
		ListIterator<GameObject> iterMoving = movingBullets.listIterator();
		while (iterMoving.hasNext()) {
			GameObject goMoving = iterMoving.next();
			float ballisticRange = dol.getWorldLocation().sub(goMoving.getWorldLocation()).length();
			ListIterator<GameObject> iterEnemies = movingEnemies.listIterator();
			while (iterEnemies.hasNext()) {
				GameObject goEnemy = iterEnemies.next();
				float distance = goEnemy.getWorldLocation().sub(goMoving.getWorldLocation()).length();
				// Remove objects that collided.
				if (distance < 0.4f) {
					GameObject.root().removeChild(goEnemy);
					GameObject.root().removeChild(goMoving);
					iterMoving.remove();
					iterEnemies.remove();
					engine.getSceneGraph().removeGameObject(goEnemy);
					engine.getSceneGraph().removeGameObject(goMoving);
				}
			}
			if (ballisticRange > 25.0f) {
				GameObject.root().removeChild(goMoving);
				iterMoving.remove();
				engine.getSceneGraph().removeGameObject(goMoving);
			} 
		}
	}
	/** Handles movements for moving entities, such as missiles, bullets, and enemies, and sends corresponding messages to the server to render them for other clients.
	 * Enemies are not synchronized across clients, and all clients have their own enemies to shoot.
	 * 
	 * @param elapsedFramesPerSecond
	 * 
	*/
	private void updateMovingObjects(float elapsedFramesPerSecond) {
		// First perform the scheduled object moves
		enemyShape.updateAnimation();
		for (GameObject go: movingObjects) {
			go.moveForwardBack(0.002f*elapsedFramesPerSecond, new Vector3f());
			protClient.sendMoveMissileMessage(go.getWorldLocation());
			protClient.sendMissileRotationMessage(go.getWorldRotation());
			
		}

		for (GameObject go: new ArrayList<GameObject>(movingMarkers)) {
			go.moveForwardBack(0.002f*elapsedFramesPerSecond, new Vector3f());
			protClient.sendMoveMarkerMessage(go.getWorldLocation());
			protClient.sendMarkerRotationMessage(go.getWorldRotation());
			((TemporaryGameObject)go).setLifetime(((TemporaryGameObject)go).getLifetime() + elapsedFramesPerSecond);
			if (((TemporaryGameObject)go).getLifetime() > 10000) {
				GameObject.root().removeChild(go);
				movingMarkers.remove(go);
				createLaserObjects(go.getWorldLocation());
			}
		}

		for (GameObject go: new ArrayList<GameObject>(laserMarkers)) {
			((TemporaryGameObject)go).setLifetime(((TemporaryGameObject)go).getLifetime() + elapsedFramesPerSecond);
			if (((TemporaryGameObject)go).getLifetime() > 5000) {
				GameObject.root().removeChild(go);
				laserMarkers.remove(go);
			}
		}

		for (GameObject go: movingEnemies) {
			go.lookAt(base);
			
			setObjectHeightAtLocation(go);
			if (go.getWorldLocation().sub(base.getWorldLocation()).length() < 2) {
				//enemyShape.stopAnimation();
				//enemyShape.playAnimation("IDLE", 0.2f, AnimatedShape.EndType.LOOP, 0);
				gameOver = 2;
			}
			else{
				go.moveForwardBack(0.0001f*elapsedFramesPerSecond, new Vector3f());
			}
		}
		// Now we have to check for collisions
		ListIterator<GameObject> iterMoving = movingObjects.listIterator();
		while (iterMoving.hasNext()) {
			GameObject goMoving = iterMoving.next();
			float missileRange = dol.getWorldLocation().sub(goMoving.getWorldLocation()).length();
			ListIterator<GameObject> iterEnemies = movingEnemies.listIterator();
			while (iterEnemies.hasNext()) {
				GameObject goEnemy = iterEnemies.next();
				float distance = goEnemy.getWorldLocation().sub(goMoving.getWorldLocation()).length();
				// Remove objects that collided.
				if (distance < 0.4f) {
					GameObject.root().removeChild(goEnemy);
					GameObject.root().removeChild(goMoving);
					iterMoving.remove();
					iterEnemies.remove();
					engine.getSceneGraph().removeGameObject(goEnemy);
					engine.getSceneGraph().removeGameObject(goMoving);
				}  
			}
			if (missileRange > 40.0f) {
				GameObject.root().removeChild(goMoving);
				iterMoving.remove();
				engine.getSceneGraph().removeGameObject(goMoving);
			}
		} 
	}
	/** Checks if the avatar has collided with the ground and corrects location.
	 * 
	 * 
	*/
	public void avatarGroundCollision() {
		Vector3f loc = dol.getWorldLocation();
		float height = groundPlane.getHeight(loc.x(), loc.z());
		Matrix4f initialTranslation = (new Matrix4f()).translation(0, 15.0f, 0);
		if (Math.abs(loc.y() - height) < 0.1 || (loc.y() - height < 0.1) ) {
			dol.setLocalTranslation(initialTranslation);
		}
	}
	/** Wraps the {@link Terrain} heightmap {@link getHeight} function to easily update height locations for {@link GameObject}.
	 * 
	 *
	*/
	public void setObjectHeightAtLocation(GameObject go) {
		Vector3f loc = go.getWorldLocation();
		float height = groundPlane.getHeight(loc.x(), loc.z());
		go.setLocalLocation(new Vector3f(loc.x(), height + bugHeightAdjust, loc.z()));
	}
	/**
	 * Checks for proximity to reloading station to replenish missiles.
	 * 
	 */
	private void checkForMissileReloading() {
		if (dol.getWorldLocation().sub(reloadingStation.getWorldLocation()).length() < 1f) {
			remainingMissiles = 5;
		}
	}

	/**
	 * Returns which avatar the player has selected.
	 * @return
	 */
	public int getSelectedAvatar() {
		return selectedAvatar;
	}
	/**
	 * Sets the avatar the player has selected.
	 * @return
	 */
	public void setSelectedAvatar(int a) {
		if (a == 1) {
			selectedAvatar = 1;
			selectAvatarSound.play();
			selectAvatar1.setLocalScale((new Matrix4f()).scaling(0.15f));
			selectAvatar2.setLocalScale((new Matrix4f()).scaling(0.1f));
			selectionRotateController.removeTarget(selectAvatar2);
			selectionRotateController.addTarget(selectAvatar1);
			dol.setTextureImage(doltx1);
		} else if (a == 2) {
			selectedAvatar = 2;
			selectAvatarSound.play();
			selectAvatar1.setLocalScale((new Matrix4f()).scaling(0.1f));
			selectAvatar2.setLocalScale((new Matrix4f()).scaling(0.15f));
			selectionRotateController.removeTarget(selectAvatar1);
			selectionRotateController.addTarget(selectAvatar2);
			dol.setTextureImage(doltx2);
		}
	}

	public boolean isSelectionScreen() {
		return inSelectionScreen;
	}

	public void closeAvatarSelection() {
		selectionRotateController.disable();
		selectionRotateController.removeTarget(selectedAvatar == 1 ? selectAvatar1 : selectAvatar2);
		GameObject.root().removeChild(selectAvatar1);
		GameObject.root().removeChild(selectAvatar2);
		inSelectionScreen = false;
	}

	private void checkForCollisions() { 
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;
		dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		for (int i=0; i<manifoldCount; i++) {
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
				for (int j = 0; j < manifold.getNumContacts(); j++) { 
					contactPoint = manifold.getContactPoint(j);
					if (contactPoint.getDistance() < 0.0f) { 
						System.out.println("---- hit between " + obj1 + " and " + obj2);
						break;
					} 
				} 
		} 
	}

	private float[] toFloatArray(double[] arr) { 
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) { 
			ret[i] = (float)arr[i];
		}
		return ret;
	}

	private double[] toDoubleArray(float[] arr) { 
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) { 
			ret[i] = (double)arr[i];
		}
		return ret;
	}

	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() { return dolS; }
	// TODO: send texture number through networking message
	public TextureImage getGhostTexture(int textureId) { 
		if (textureId == 1) 
		{
			return doltx1;
		} else {
			return doltx2;
		}
	}
	public ObjShape getMarkerShape() { return markerS; }
	public TextureImage getMarkerTexture() { return bombTex; }
	public ObjShape getMissileShape() { return missileShape; }
	public TextureImage getMissileTexture() { return missile; }
	public ObjShape getBulletShape() { return bulletS ;}
	public TextureImage getBulletTexture() {return tracer; }
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
	@Override public void shutdown()
	{	
		System.out.println("shutting down");
		if(protClient != null && isClientConnected == true)
		{	
			protClient.sendByeMessage();
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
		System.out.println("\tCycle through avatar selection:\t\t\tz");
		System.out.println("\tSelect current avatar:\t\t\tx");
		System.out.println("\tMove Forward:\t\t\t\tW, Gamepad Right Trigger");
		System.out.println("\tMove Backward:\t\t\t\tS, Gamepad Left Trigger");
		System.out.println("\tYaw Left:\t\t\t\tA, Gamepad Button 5 Left Bumper");
		System.out.println("\tYaw Right:\t\t\t\tD, Gamepad Button 6 Right Bumper");
		System.out.println("\tRoll Left:\t\t\t\tQ, Gamepad Left Joystick X-Axis left");
		System.out.println("\tRoll Right:\t\t\t\tE, Gamepad Left Joystick X-Axis right");
		System.out.println("\tPitch Up:\t\t\t\tUp Arrow, Gamepad Left Joystick Y-Axis down");
		System.out.println("\tPitch Down:\t\t\t\tDown Arrow, Gamepad Left Joystick Y-Axis up");
		System.out.println("\tCamera Rotate Left:\t\t\tLeft Arrow, Gamepad Right Joystick RX-axis left");
		System.out.println("\tCamera Rotate Right:\t\t\tRight Arrow, Gamepad Right Joystick RX-axis right");
		System.out.println("\tCamera Elevation Up:\t\t\tR, Gamepad Right Joystick RY-axis up");
		System.out.println("\tCamera Elevation Down:\t\t\tF, Gamepad Right Joystick RY-axis down");
		System.out.println("\tCamera Zoom In:\t\t\t\tGamepad Button 3");
		System.out.println("\tCamera Zoom Out:\t\t\tGamepad Button 4");
		System.out.println("\tFire Main Gun:\t\t\t\tGamepad Button 2");
		System.out.println("\tFire Missile:\t\t\t\tGamepad Button 1");
		System.out.println("\tDrop Marker:\t\t\t\tGamepad Button 8");
		System.out.println("\tPan Mini-Map Up:\t\t\t0");
		System.out.println("\tPan Mini-Map Right:\t\t\tP");
		System.out.println("\tPan Mini-Map Down:\t\t\tL");
		System.out.println("\tPan Mini-Map Right:\t\t\tO");
		System.out.println("\tMini-Map Zoom In:\t\t\t]");
		System.out.println("\tMini-Map Zoom Out:\t\t\t[");
		System.out.println("\tToggle World Axes On/Off:\t\t1");
		System.out.println("\tJet Wireframe On:\t\t\t2");
		System.out.println("\tJet Wireframe Off:\t\t\t3");
		System.out.println("\tStarting physics:\t\t\t5 (On by Default)");
		System.out.println("\tTurning off Tower Light:\t\t6");
		System.out.println("\tTurning on Tower Light:\t\t\t7");
		System.out.println("\tExit Game:\t\t\t\tESC");
	}
}
