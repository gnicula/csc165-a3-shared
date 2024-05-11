package a2;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;

public class TemporaryGameObject extends GameObject {
    private float lifetime = 0;

    public TemporaryGameObject(GameObject r, ObjShape s, TextureImage t) 
	{	
        super(r, s, t);
	}

    public float getLifetime() {
        return lifetime;
    }
    
    public void setLifetime(float life) {
        lifetime = life;
    }
    
}
