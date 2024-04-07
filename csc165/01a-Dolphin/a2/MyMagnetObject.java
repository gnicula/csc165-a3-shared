/*
 * @author Gabriele Nicula
 */

package a2;

import tage.*;
import tage.shapes.*;
import org.joml.*;

public class MyMagnetObject extends ManualObject {
    // indexed ObjShape definitions in TAGE must be defined as Vector3f/2f
    // the indexes themselves are stored in an int array
    private Vector3f[] vertices = new Vector3f[3];
    private Vector2f[] texcoords = new Vector2f[3];
    private Vector3f[] normals = new Vector3f[3];
    private int[] indices = new int[] { 0, 1, 2 };

    public MyMagnetObject() {
        super();
        vertices[0] = (new Vector3f()).set(-1.0f, -1.0f, 0.0f);
        vertices[1] = (new Vector3f()).set(1.0f, -1.0f, 0.0f);
        vertices[2] = (new Vector3f()).set(0.0f, 1.0f, 0.0f);
        texcoords[0] = (new Vector2f()).set(0f, 0f);
        texcoords[1] = (new Vector2f()).set(.5f, 1f);
        texcoords[2] = (new Vector2f()).set(.5f, 0f);
        normals[0] = (new Vector3f()).set(0f, 0f, 1f);
        normals[1] = (new Vector3f()).set(0f, 0f, 1f);
        normals[2] = (new Vector3f()).set(0f, 0f, 1f);

        setNumVertices(3);
        setVerticesIndexed(indices, vertices);
        setTexCoordsIndexed(indices, texcoords);
        setNormalsIndexed(indices, normals);
        setMatAmb(Utils.goldAmbient());
        setMatDif(Utils.goldDiffuse());
        setMatSpe(Utils.goldSpecular());
        setMatShi(Utils.goldShininess());
    }
}