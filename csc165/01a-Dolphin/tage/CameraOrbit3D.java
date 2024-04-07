package tage;

import org.joml.*;
import java.lang.Math;
import net.java.games.input.Event;
import tage.input.*;
import tage.input.action.*;


/**
* CameraOrbit3D is a moveable Camera around a given GameObject.
* <br>
* It holds references to the: Engine, Camera being controlled, GameObject
* and its azimuth, elevation, and its distance to the Camera.
* <ul>
* <li> Engine
* <li> Camera being controlled
* <li> GameObject and its azimuth, elevation, and its distance to the Camera.
* </ul>
* <p>
* Each CameraOrbit3D object is associated with a GameObject and its always facing that GameObject.
* Camera can be moved in a circular fashion up/down and left/right around the object.
* Camera can be zoomed in/out of the object.
* @author Gabriele Nicula
*/

public class CameraOrbit3D {
    private Engine engine;
    private Camera camera; // the camera being controlled
    private GameObject avatar; // the target avatar the camera looks at
    private float cameraAzimuth; // rotation around target Y axis
    private float cameraElevation; // elevation of camera above target
    private float cameraRadius; // distance between camera and target
    private static final float SMOOTHNESS_FACTOR = 0.005f;

    /** instantiates a CameraOrbit3D with a Camera object, GameObject, String name, and Engine*/
    public CameraOrbit3D(Camera cam, GameObject av,
            String gpName, Engine e) {
        engine = e;
        camera = cam;
        avatar = av;
        cameraAzimuth = 0.0f; // start BEHIND and ABOVE the target
        cameraElevation = 20.0f; // elevation is in degrees
        cameraRadius = 3.5f; // distance from camera to avatar
        setupInputs(gpName);
        updateCameraPosition();
    } 

    /** Associates named controller gp axis rx/ry and buttons 2/3 with azimuth, elevate, and zoom camera controls. */
    private void setupInputs(String gp) {
        OrbitAzimuthAction azmAction = new OrbitAzimuthAction();
        OrbitElevationAction elevAction = new OrbitElevationAction();
        OrbitZoomAction zoomIn = new OrbitZoomAction(-0.0002f);
        OrbitZoomAction zoomOut = new OrbitZoomAction(0.0002f);
        InputManager im = engine.getInputManager();
        im.associateAction(gp,
                net.java.games.input.Component.Identifier.Axis.RX, azmAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(gp,
                net.java.games.input.Component.Identifier.Axis.RY, elevAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(gp, 
				net.java.games.input.Component.Identifier.Button._2, zoomIn,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(gp, 
				net.java.games.input.Component.Identifier.Button._3, zoomOut,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }

    /** 
     * Compute the camera’s azimuth, elevation, and distance, relative to
     * the target in spherical coordinates, then convert to world Cartesian
     * coordinates and set the camera position from that.
     */
    public void updateCameraPosition() {
        Vector3f avatarRot = avatar.getWorldForwardVector();
        double avatarAngle = Math.toDegrees((double)
            avatarRot.angleSigned(new Vector3f(0,0,-1), new Vector3f(0,1,0)));
        float totalAz = cameraAzimuth - (float)avatarAngle;
        double theta = Math.toRadians(cameraAzimuth);
        double phi = Math.toRadians(cameraElevation);
        float x = cameraRadius * (float)(Math.cos(phi) * Math.sin(theta));
        float y = cameraRadius * (float)(Math.sin(phi));
        float z = cameraRadius * (float)(Math.cos(phi) * Math.cos(theta));
        camera.setLocation(new
            Vector3f(x,y,z).add(avatar.getWorldLocation()));
        camera.lookAt(avatar);
    }
    
    /** Adds a box physics object in the physics world, with specified mass, matrix transform, and size.
	*  <br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*  The size is specified using a 3-element array of type double, with desired dimensions in X, Y, and Z.<br>
	*  Also adds a corresponding renderable object if displaying the physics world has been enabled.
	*/
    private class OrbitAzimuthAction extends AbstractInputAction {
        public void performAction(float time, Event event) {
            float rotAmount;
            if (event.getValue() < -0.2) {
                rotAmount = -SMOOTHNESS_FACTOR;
            } else {
                if (event.getValue() > 0.2) {
                    rotAmount = SMOOTHNESS_FACTOR;
                } else {
                    rotAmount = 0.0f;
                }
            }
            cameraAzimuth += rotAmount * time;
            cameraAzimuth = cameraAzimuth % 360;
            updateCameraPosition();
        }
    }

    /** Implements a TAGE AbstractInputAction which changes the elevation of the CameraOrbit3D Object. */
    private class OrbitElevationAction extends AbstractInputAction {
        public void performAction(float time, Event event) {
            float elevAmount;
            if (event.getValue() < -0.2) {
                elevAmount = -SMOOTHNESS_FACTOR;
            } else {
                if (event.getValue() > 0.2) {
                    elevAmount = SMOOTHNESS_FACTOR;
                } else {
                    elevAmount = 0.0f;
                }
            }
            cameraElevation += elevAmount * time;
            // Limit camera to not go over the object on the other side
            if (cameraElevation >= 90) {
                cameraElevation = 89;
            } else if (cameraElevation <= -90) {
                cameraElevation = -89;
            }
            updateCameraPosition();
        }
    }
    /** Implements a TAGE AbstractInputAction which zooms the CameraOrbit3D object in and out from the GameObject */
    private class OrbitZoomAction extends AbstractInputAction {
        private float factor;

        public OrbitZoomAction(float factor) {
            this.factor = factor;
        }

        public void performAction(float time, Event event) {
            float zoomSpeed = time * factor;
            cameraRadius += zoomSpeed;
            // Limit zoom in to near center of the object
            if (cameraRadius <= 0.1f) {
                cameraRadius = 0.1f;
            }
        }
    }
}
