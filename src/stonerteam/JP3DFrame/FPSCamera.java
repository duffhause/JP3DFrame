package stonerteam.JP3DFrame;

import java.awt.event.MouseEvent;

import com.jogamp.math.Vec3f;


// I never want to program a camera again
public class FPSCamera {
	// Camera vectors
	public Vec3f position = new Vec3f(0,0,0);
	public Vec3f look = new Vec3f(0,0,1);
	private float yaw = 90;
	private float pitch = 0;
	public float speed = 0.15f;
	
	//Mouse drag stuff
	private int[] initialPosition =  new int[2];
	private int dragWeight = 10;
	
	private Vec3f getDirectionVector() {
		Vec3f direction;
		direction = look.copy().sub(position);
		direction.normalize();
		direction.mul(speed);
		return direction;
	}
	
	private Vec3f getRightVector() {
		Vec3f direction = getDirectionVector();
		Vec3f right = new Vec3f(direction.z(), 0, -direction.x());
		right.normalize();;
		right.mul(speed);
		return right;
	}
	
	public void forward() {
		Vec3f dir = getDirectionVector();
		position.add(dir);
		look.add(dir);
	}
	
	public void backward() {
		Vec3f dir = getDirectionVector();
		position.sub(dir);
		look.sub(dir);
	}
	
	public void left() {
		Vec3f right = getRightVector();
		position.add(right);
		look.add(right);
	}
	
	public void right() {
		Vec3f right = getRightVector();
		position.sub(right);
		look.sub(right);
	}
	
	public void drag (MouseEvent e) {
		float[] drag = new float[] {
				e.getX() - initialPosition[0],
				e.getY() - initialPosition[1],
		};
		setInitialClick(e);
		
		drag[0] /= dragWeight;
		drag[1] /= dragWeight;
		
		yaw += drag[0];
		pitch -= drag[1];
		
		pitch = Math.max(-89, Math.min(89, pitch));
		yaw %= 360.0f;
		if (yaw < 0)
		    yaw += 360.0f;
		
		float yawRad = (float) Math.toRadians(yaw);
		float pitchRad = (float) Math.toRadians(pitch);
		
		float lookX = (float) (Math.cos(pitchRad) * Math.cos(yawRad));
	    float lookY = (float) Math.sin(pitchRad);
	    float lookZ = (float) (Math.cos(pitchRad) * Math.sin(yawRad));

	    look.setX(position.x() + lookX);
	    look.setY(position.y() + lookY);
	    look.setZ(position.z() + lookZ);
	}
	
	public void setInitialClick (MouseEvent e) {
		initialPosition[0] = e.getX();
		initialPosition[1] = e.getY();
	}
	
}
