package tage.nodeControllers;
import tage.*;
import org.joml.*;

/**
* A StretchController is a node controller that, when enabled, causes any object
* it is attached to to scale on specified axis.
* @author Gabriele Nicula
*/

public class StretchController extends NodeController {
    private float scaleRate = .0006f;
    private float cycleTime = 2000.0f;
    private float totalTime = 0.0f;
    private float direction = 1.0f;
    private boolean flipAxis = false;
    private Matrix4f curScale, newScale;
    private Engine engine;

    /** Creates a StretchController with an Engine and a cycletime */
    public StretchController(Engine e, float ctime) {
        super();
        cycleTime = ctime;
        engine = e;
        newScale = new Matrix4f();
    }

    /** Applies the StretchController to a GameObject */
    public void apply(GameObject go) {
        float elapsedTime = super.getElapsedTime();
        totalTime += elapsedTime / 1000.0f;
        if (totalTime > cycleTime) {
            direction = -direction;
            totalTime = 0.0f;
            if (direction > 0) {
                flipAxis = !flipAxis;
            }
        }
        curScale = go.getLocalScale();
        float scaleAmt = 1.0f + direction * scaleRate * elapsedTime;
        if (!flipAxis) {
            // scale on the x axis
            newScale.scaling(curScale.m00() * scaleAmt, curScale.m11(), curScale.m22());
        } else {
            // scale on the z axis
            newScale.scaling(curScale.m00(), curScale.m11(), curScale.m22() * scaleAmt);
        }
        
        go.setLocalScale(newScale);
    }
}
