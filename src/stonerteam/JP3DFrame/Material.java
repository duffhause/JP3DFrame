package stonerteam.JP3DFrame;

import stonerteam.P3D.Chunks.ShaderChunk;
import stonerteam.P3D.Chunks.ShaderIntegerParameterChunk;
import stonerteam.P3D.Chunks.ShaderTextureParameterChunk;

class Material {
	public ShaderChunk shader;
	public String textureName;
	public int uvMode;
	public int blendMode;
	
	
	Material(ShaderChunk shader) {
		this.shader = shader;
		this.textureName = shader.getChildren(ShaderTextureParameterChunk.class).get(0).Value;
		
		for (ShaderIntegerParameterChunk intP : shader.getChildren(ShaderIntegerParameterChunk.class)) {
			if (intP.Param.equals("UVMD")) {
				this.uvMode = intP.Value;
			}
			
			if (intP.Param.equals("BLMD")) {
				this.blendMode = intP.Value;
			}
		}	
	}
}
