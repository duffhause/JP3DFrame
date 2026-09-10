package stonerteam.JP3DFrame;

import stonerteam.P3D.Chunks.ShaderChunk;
import stonerteam.P3D.Chunks.ShaderIntegerParameterChunk;
import stonerteam.P3D.Chunks.ShaderTextureParameterChunk;

class Material {
	public ShaderChunk shader;
	public String textureName;
	public int uvMode;
	public int blendMode;
	public boolean alphaTest;
	
	
	Material(ShaderChunk shader) {
		this.shader = shader;
		this.textureName = shader.getChildren(ShaderTextureParameterChunk.class).get(0).value;
		
		for (ShaderIntegerParameterChunk intP : shader.getChildren(ShaderIntegerParameterChunk.class)) {
			if (intP.param.equals("UVMD")) {
				this.uvMode = intP.value;
			} else if (intP.param.equals("BLMD")) {
				this.blendMode = intP.value;
			} else if (intP.param.equals("ATST")) {
				this.alphaTest = intP.value > 0;
			}
		}	
	}
}
