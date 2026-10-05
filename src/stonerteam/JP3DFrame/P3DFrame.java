package stonerteam.JP3DFrame;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.jogamp.math.Matrix4f;
import com.jogamp.math.Vec3f;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;

import stonerteam.P3D.P3D;
import stonerteam.P3D.P3DChunk;
import stonerteam.P3D.Chunks.BoundingBoxChunk;
import stonerteam.P3D.Chunks.ColourListChunk;
import stonerteam.P3D.Chunks.DynaPhysChunk;
import stonerteam.P3D.Chunks.ImageChunk;
import stonerteam.P3D.Chunks.ImageDataChunk;
import stonerteam.P3D.Chunks.IndexListChunk;
import stonerteam.P3D.Chunks.InstStaticPhysChunk;
import stonerteam.P3D.Chunks.InstanceListChunk;
import stonerteam.P3D.Chunks.MeshChunk;
import stonerteam.P3D.Chunks.NormalListChunk;
import stonerteam.P3D.Chunks.OldPrimitiveGroupChunk;
import stonerteam.P3D.Chunks.PositionListChunk;
import stonerteam.P3D.Chunks.ScenegraphBranchChunk;
import stonerteam.P3D.Chunks.ScenegraphChunk;
import stonerteam.P3D.Chunks.ScenegraphDataChunk;
import stonerteam.P3D.Chunks.ScenegraphTransformChunk;
import stonerteam.P3D.Chunks.ShaderChunk;
import stonerteam.P3D.Chunks.StaticEntityChunk;
import stonerteam.P3D.Chunks.TextureChunk;
import stonerteam.P3D.Chunks.UVListChunk;

public class P3DFrame  implements GLEventListener, KeyListener {
	private GLU glu = new GLU();
	final GLCanvas canvas;
	
	public FPSCamera camera = new FPSCamera();
	
	private P3DChunk p3droot = new P3DChunk(new int[] {0x50, 0x33, 0x44, 0xff});
	private P3DChunk terraroot = new P3DChunk(new int[] {0x50, 0x33, 0x44, 0xff});
	public String terraPath = null;
	Stack<String> fileQueue = new Stack<String>();
	private Map<String, Texture> textureMap;
	private Map<String, Material> shaderMap;
	
	// Caches
	private List<StaticEntityChunk> staticEntityRenderOrderSorted = new ArrayList<StaticEntityChunk>();
	
	public P3DFrame (int sizeX, int sizeY, P3DChunk p3d){
		if (p3d != null) {
			this.p3droot = p3d;
		}
		
		textureMap = new HashMap<String, Texture>();
		shaderMap = new HashMap<String, Material>();
		
		final GLProfile profile = GLProfile.get(GLProfile.GL2);
		GLCapabilities capabilities = new GLCapabilities(profile);
		capabilities.setDoubleBuffered(true);
		capabilities.setDepthBits(24);
		capabilities.setAlphaBits(8);
		canvas = new GLCanvas(capabilities);
		
		canvas.addGLEventListener(this);
		canvas.addKeyListener(this);
		canvas.setSize(900, 600);
		canvas.setMinimumSize(new Dimension(0, 0));
		canvas.setPreferredSize(new Dimension(sizeX, sizeY));
		
		canvas.addMouseListener(new MouseListener() {
			@Override
			public void mousePressed(MouseEvent e) {
				camera.setInitialClick(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			
		});
		canvas.addMouseMotionListener(new MouseMotionListener () {

			@Override
			public void mouseDragged(MouseEvent e) {
				camera.drag(e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {}
		});
		
		final FPSAnimator animator = new FPSAnimator(this.canvas, 300,true);
		animator.start();
	}
	
	public P3DFrame(int sizeX, int sizeY) {
	    this(sizeX, sizeY, null);
	}

	public P3DChunk getP3D() {
		return p3droot;
	}
	
	public P3DChunk getTERRA() {
		return terraroot;
	}
	
	public GLCanvas getCanvas() {
		return this.canvas;
	}
	
	public void processShaders(P3DChunk p3dchunk) {
		loadTextures(p3dchunk);
		for (ShaderChunk shader : p3dchunk.getChildren(ShaderChunk.class)) {
			shaderMap.put(shader.ShaderName, new Material(shader));
		}
	}
	
	public void loadTextures(P3DChunk p3dchunk) {
		for (TextureChunk tex : p3dchunk.getChildren(TextureChunk.class)) {
			ImageChunk img = tex.getChildren(ImageChunk.class).get(0);
			ImageDataChunk imgData = img.getChildren(ImageDataChunk.class).get(0);
			textureMap.put(tex.Name, P3DConversion.DataToTexture(imgData.getData()));
		}
	}
	
	public void loadTerra(String filepath) {
		terraPath = filepath;
		terraroot = P3D.ReadP3D(filepath);
		loadTextures(terraroot);
	}
	
	private void loadP3d(String filepath) {
		p3droot = P3D.ReadP3D(filepath);
		staticEntityRenderOrderSorted.clear();
		processShaders(p3droot);
		
		resetCamera();
	}
	
	public void resetCamera() {
		Vec3f highest = null;
		Vec3f lowest = null;
		
		for (StaticEntityChunk se : p3droot.getChildren(StaticEntityChunk.class)){
			for (MeshChunk mesh : se.getChildren(MeshChunk.class)){
				List<BoundingBoxChunk> boundingBoxs = mesh.getChildren(BoundingBoxChunk.class);
				if (boundingBoxs.size() == 0)
					continue;
				BoundingBoxChunk bounds = boundingBoxs.get(0);
				
				Vec3f currentHigh = new Vec3f(-bounds.high[0], bounds.high[1],bounds.high[2]);
				Vec3f currentLow = new Vec3f(-bounds.low[0], bounds.low[1],bounds.low[2]);
			
				
				if (highest == null) {
			        highest = new Vec3f(currentHigh.x(), currentHigh.y(), currentHigh.z());
			        lowest = new Vec3f(currentLow.x(), currentLow.y(), currentLow.z());
			        continue;
			    }
				
				if (currentHigh.x() > highest.x()) {
				    highest.setX(currentHigh.x());
				}
				if (currentHigh.y() > highest.y()) {
				    highest.setY(currentHigh.y());
				}
				if (currentHigh.z() > highest.z()) {
				    highest.setZ(currentHigh.z());
				}
	
				if (currentLow.x() < lowest.x()) {
				    lowest.setX(currentLow.x());
				}
				if (currentLow.y() < lowest.y()) {
				    lowest.setY(currentLow.y());
				}
				if (currentLow.z() < lowest.z()) {
				    lowest.setZ(currentLow.z());
				}
			}

		}
		
		//set camera
		float x = (highest.x() + lowest.x()) / 2;
		float y = (highest.y() + lowest.y()) / 2;
		float z = (highest.z() + lowest.z()) / 2;
		
		this.camera.position = new Vec3f(x,y,z);
	}
	
	public void requestP3DLoad(String filepath) {
		// P3D files cannot be directly loaded from outside of the opengl context, in order to properly load and access TextureIOs
			// Adding filepath to a stack, so it can be properly loaded later a function thats within the gl context
		fileQueue.add(filepath);
	}
	
	public void drawOldPrimitiveGroup(GL2 gl, OldPrimitiveGroupChunk opg) {

		// Get required polygon data
		PositionListChunk positionList = opg.getChildren(PositionListChunk.class).get(0);
		IndexListChunk indexList = opg.getChildren(IndexListChunk.class).get(0);
		UVListChunk UVList = opg.getChildren(UVListChunk.class).get(0);
		
		// Get Material data
		Material mat = shaderMap.get(opg.ShaderName);
		
		// Apply relevant texture
		Texture texture = textureMap.get(mat.textureName);
		
		if (mat.blendMode == P3D.BLENDMODE_NONE) {
			gl.glDisable(GL2.GL_BLEND);
		} else if (mat.blendMode == P3D.BLENDMODE_ALPHA) {
			gl.glEnable(GL2.GL_BLEND);
			gl.glBlendFunc(
		        GL2.GL_SRC_ALPHA,
		        GL2.GL_ONE_MINUS_SRC_ALPHA
		    );
		    gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPEAT); 
		}
		
		if (mat.alphaTest) {
			gl.glEnable(GL2.GL_ALPHA_TEST);
			gl.glAlphaFunc(GL2.GL_GREATER, 0.5f);
		} else {
			gl.glDisable(GL2.GL_ALPHA_TEST);
		}
		
		if (texture != null) {
			// Enable textures
			gl.glEnable(GL2.GL_TEXTURE_2D); 
			texture.enable(gl);
			texture.bind(gl);
		} else {
			gl.glDisable(GL2.GL_TEXTURE_2D);
		}
		
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, mat.uvMode == P3D.UV_REPEAT ? GL2.GL_REPEAT : GL2.GL_CLAMP);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, mat.uvMode == P3D.UV_REPEAT ? GL2.GL_REPEAT : GL2.GL_CLAMP);
		
		
		
		// Begin drawing polygons
		if (opg.PrimitiveType == P3D.PRIMITIVE_TRIANGLES) {
			gl.glBegin( GL2.GL_TRIANGLES );
		} else if (opg.PrimitiveType == P3D.PRIMITIVE_TRIANGLE_STRIP) {
			gl.glBegin( GL2.GL_TRIANGLE_STRIP );
		}
		
		boolean hasVertColours = false;
		List<ColourListChunk> colourListList = opg.getChildren(ColourListChunk.class);
		ColourListChunk colourList = null;
		if (colourListList.size() > 0) {
			hasVertColours = true;
			colourList = colourListList.get(0);
		}
		
		boolean hasNormals = false;
		List<NormalListChunk> normalListList = opg.getChildren(NormalListChunk.class);
		NormalListChunk normalList = null;
		if (normalListList.size() > 0) {
			hasNormals = true;
			normalList = normalListList.get(0);
		}	
		
		for (int index : indexList.Indices) {

			if (hasNormals) {
				gl.glNormal3f(
					-normalList.Normals[index][0], 
					normalList.Normals[index][1], 
					normalList.Normals[index][2] 
			    );
			}

			gl.glTexCoord2d(UVList.UVs[index][0],UVList.UVs[index][1]);
			if (hasVertColours) {
			    gl.glColor4f(
		    		colourList.Colours[index].R / 255.0f,
		    		colourList.Colours[index].G / 255.0f,
		    		colourList.Colours[index].B / 255.0f,
		    		colourList.Colours[index].A / 255.0f
		    	);
			}
			
			gl.glVertex3f( 
				-positionList.Positions[index][0], 
				positionList.Positions[index][1], 
				positionList.Positions[index][2] 
			);
		}
		// End drawing
		gl.glEnd();
	}
	
	private void drawStaticEntities(GL2 gl, int blmd) {
		for (StaticEntityChunk se : staticEntityRenderOrderSorted) {
			for (MeshChunk mesh : se.getChildren(MeshChunk.class)) {
				for (OldPrimitiveGroupChunk opg : mesh.getChildren(OldPrimitiveGroupChunk.class)) {
					if (shaderMap.get(opg.ShaderName).blendMode == blmd)
						drawOldPrimitiveGroup(gl, opg);
				}
			}
		}
	}
	
	private void drawMesh(GL2 gl, MeshChunk mesh) {
		for (OldPrimitiveGroupChunk opg : mesh.getChildren(OldPrimitiveGroupChunk.class)) {
			drawOldPrimitiveGroup(gl, opg);
		}
	}
	
	private MeshChunk meshDotTransformationMatrix(MeshChunk mesh, float[]transformationMatrix4x4) {
		Matrix4f transformation = new Matrix4f(transformationMatrix4x4);
		
		MeshChunk newMesh = (MeshChunk) mesh.copy();
		for (OldPrimitiveGroupChunk opg : newMesh.getChildren(OldPrimitiveGroupChunk.class)) {
			PositionListChunk positions = opg.getChildren(PositionListChunk.class).get(0);
			for (int i=0; i<positions.NumOfVerticies; i++) {
				Vec3f pos = new Vec3f(
					positions.Positions[i][0],
					positions.Positions[i][1],
					positions.Positions[i][2]
				);
				
				Vec3f newV = transformation.mulVec3f(pos);
				positions.Positions[i][0] = newV.x();
				positions.Positions[i][1] = newV.y();
				positions.Positions[i][2] = newV.z();
			}
		}
	
		return newMesh;
		
	}
	
	public void drawInstStaticPhys(GL2 gl) {
		for (InstStaticPhysChunk isp : p3droot.getChildren(InstStaticPhysChunk.class)) {
			MeshChunk mesh = isp.getChildren(MeshChunk.class).get(0);
			
			InstanceListChunk instanceList = isp.getChildren(InstanceListChunk.class).get(0);
			ScenegraphChunk scenegraph = instanceList.getChildren(ScenegraphChunk.class).get(0);
			ScenegraphDataChunk oldScenegraphRoot = scenegraph.getChildren(ScenegraphDataChunk.class).get(0);
			ScenegraphBranchChunk scenegraphRoot = oldScenegraphRoot.getChildren(ScenegraphBranchChunk.class).get(0);
			ScenegraphTransformChunk scenegraphTransform = scenegraphRoot.getChildren(ScenegraphTransformChunk.class).get(0);
			
			if (scenegraphTransform.Children.size() == 0)
				continue;
			
			for (ScenegraphTransformChunk trans : scenegraphTransform.getChildren(ScenegraphTransformChunk.class)) {			
				drawMesh(gl, meshDotTransformationMatrix(mesh, trans.transformMatrix4x4));
			}
			
		}
	}
	
	public void drawDynaPhys(GL2 gl) {
		for (DynaPhysChunk dp : p3droot.getChildren(DynaPhysChunk.class)) {
			MeshChunk mesh = dp.getChildren(MeshChunk.class).get(0);
			
			InstanceListChunk instanceList = dp.getChildren(InstanceListChunk.class).get(0);
			ScenegraphChunk scenegraph = instanceList.getChildren(ScenegraphChunk.class).get(0);
			ScenegraphDataChunk oldScenegraphRoot = scenegraph.getChildren(ScenegraphDataChunk.class).get(0);
			ScenegraphBranchChunk scenegraphRoot = oldScenegraphRoot.getChildren(ScenegraphBranchChunk.class).get(0);
			ScenegraphTransformChunk scenegraphTransform = scenegraphRoot.getChildren(ScenegraphTransformChunk.class).get(0);
			
			if (scenegraphTransform.Children.size() == 0)
				continue;
			
			for (ScenegraphTransformChunk trans : scenegraphTransform.getChildren(ScenegraphTransformChunk.class)) {			
				drawMesh(gl, meshDotTransformationMatrix(mesh, trans.transformMatrix4x4));
			}
			
		}
	}
	
	public void drawP3d(GL2 gl, int context) {
		if (context == RENDER_STATIC_ENTITY) {
			if (staticEntityRenderOrderSorted.size() == 0) {
				staticEntityRenderOrderSorted.addAll(p3droot.getChildren(StaticEntityChunk.class));

				//Using bubble sort, better 'rithm should be used later perhaps
				for (int i=0; i<staticEntityRenderOrderSorted.size(); i++) {
					for (int ii=0; ii<staticEntityRenderOrderSorted.size()-1; ii++) {
						if (staticEntityRenderOrderSorted.get(ii).RenderOrder > staticEntityRenderOrderSorted.get(ii+1).RenderOrder) {
							StaticEntityChunk tmp = staticEntityRenderOrderSorted.get(ii);
							staticEntityRenderOrderSorted.set(ii, staticEntityRenderOrderSorted.get(ii+1));
							staticEntityRenderOrderSorted.set(ii+1, tmp);
						}
					}
				}
			}
			
			drawStaticEntities(gl, P3D.BLENDMODE_NONE);
			drawStaticEntities(gl, P3D.BLENDMODE_ALPHA);
		} else if (context == RENDER_INST_STATIC_PHYS) {
			drawInstStaticPhys(gl);
			drawDynaPhys(gl);
		}
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		
		// Files have to be loaded whilst inside of the GL context
		while (!fileQueue.isEmpty()) {
			
			String filepath = fileQueue.pop();
			
			String filename = Paths.get(filepath).getFileName().toString().toLowerCase();
			
			if (filename.endsWith("terra.p3d")) {
				this.loadTerra(filepath);
			} else {
				this.dispose(drawable);
				this.loadP3d(filepath);
			}

			System.out.println("Loaded P3D");
		}
		
		final GL2 gl = drawable.getGL().getGL2();
		//Enable depth perception
		gl.glEnable( GL2.GL_DEPTH_TEST );
		gl.glDepthFunc( GL2.GL_LEQUAL );
		
		// Clear The Screen And The Depth Buffer 
		gl.glClear (GL2.GL_COLOR_BUFFER_BIT |  GL2.GL_DEPTH_BUFFER_BIT );
		
		// Reset The View	
		gl.glLoadIdentity();
		
		//Translate view
		glu.gluLookAt(
				camera.position.x(), camera.position.y(), camera.position.z(),
				camera.look.x(), camera.look.y(), camera.look.z(),
			0,1,0
		);
		
		// Begin drawing p3d objects
		drawP3d(gl, RENDER_STATIC_ENTITY);	
		drawP3d(gl, RENDER_INST_STATIC_PHYS);	
		
		gl.glFlush(); 	
	}
	
	@Override
	public void init(GLAutoDrawable drawable) {
		System.out.println("JOGL Init");
		
		// Resizing the frame will actually dispose the gl frame and create a new one with new dimensions
			// This means every time the frame is initiated we must reload all the texture data even if p3d file is the same
		loadTextures(terraroot);
		processShaders(p3droot); 
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		int texDisposeCount = 0;
		for (Map.Entry<String, Texture> entry : textureMap.entrySet()) {
		    Texture texture = entry.getValue();

		    if (texture != null) {
		        texture.destroy(drawable.getGL().getGL2());
		        texDisposeCount += 1;
		    }
		}
		System.out.println(String.format("Disposed %d textures", texDisposeCount));

		textureMap.clear();
		shaderMap.clear();
	}
	
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();
		
		final float h = (float) width / (float) height;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		
		glu.gluPerspective(45.0f, h, 0.01f, 1000.0);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		
	}
	
	@Override
	public void keyPressed(KeyEvent arg0) {
		int key = arg0.getKeyCode();
				
		switch (key) {
			case KeyEvent.VK_W:
				camera.forward();
				break;
			case KeyEvent.VK_S:
				camera.backward();
				break;
			case KeyEvent.VK_A:
				camera.left();
				break;
			case KeyEvent.VK_D:
				camera.right();
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {}
	
	@Override
	public void keyTyped(KeyEvent e) {}

	
	// Constants
	public final int RENDER_STATIC_ENTITY = 1;
	public final int RENDER_INST_STATIC_PHYS = 2;
}
