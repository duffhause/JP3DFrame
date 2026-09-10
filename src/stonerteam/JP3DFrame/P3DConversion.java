package stonerteam.JP3DFrame;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

public class P3DConversion {
	public static Texture DataToTexture (int[] data) {
		int len = data.length;
		byte[] imgBytes = new byte[len-4];

		for (int i = 4; i < len; i++) {
			imgBytes[i-4] = (byte) data[i];
		}
		
		
		try {
			Texture texture = TextureIO.newTexture(
				    new ByteArrayInputStream(imgBytes),
				    false,
				    TextureIO.PNG
				);
			
			return texture;
		} catch (GLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
