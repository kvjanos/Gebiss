package bii.ij3d;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.WindowManager;
import ij.gui.GUI;

import bii.voltex.VoltexGroup;
import bii.orthoslice.OrthoGroup;

import javax.media.j3d.Transform3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import bii.voltex.VolumeRenderer;

public class BiiImageJ3DViewer implements PlugIn {

	private static int threshold;
	private static boolean isThresholdSet = false;
	private static boolean wasCancelled = false;

	public static void main(String[] args) {
		  new ij.ImageJ();
		  IJ.runPlugIn("ij3d.ImageJ3DViewer", "");
	}

	public void run(String arg) {
		ImagePlus image = WindowManager.getCurrentImage();
		try {
			Image3DUniverse univ = new Image3DUniverse();
			univ.show();
			GUI.center(univ.getWindow());
			int type = -1;
			// only when there is an image and we are not called
			// from a macro
			//if(image != null && !IJ.isMacro())
				//univ.getExecuter().addContent(image, type);

		} catch(Exception e) {
			StringBuffer buf = new StringBuffer();
			StackTraceElement[] st = e.getStackTrace();
			buf.append("An unexpected exception occurred. \n" + 
				"Please mail me the following lines if you \n"+
				"need help.\n" + 
				"bene.schmid@gmail.com\n   \n");
			buf.append(e.getClass().getName()  + ":" + 
						e.getMessage() + "\n");
			for(int i = 0; i < st.length; i++) {
				buf.append(
					"    at " + st[i].getClassName() + 
					"." + st[i].getMethodName() + 
					"(" + st[i].getFileName() + 
					":" + st[i].getLineNumber() + 
					")\n");
			}
			new ij.text.TextWindow("Error", buf.toString(), 500, 400);
		}
	}

	private static Image3DUniverse getUniv() {
		if(Image3DUniverse.universes.size() > 0)
			return Image3DUniverse.universes.get(0);
		return null;
	}

	// View menu
	public static void resetView() {
		Image3DUniverse univ = getUniv();
		if(univ != null) univ.resetView();
	}

	public static void startAnimate() {
		Image3DUniverse univ = getUniv();
		if(univ != null) univ.startAnimation();
	}

	public static void stopAnimate() {
		Image3DUniverse univ = getUniv();
		if(univ != null) univ.pauseAnimation();
	}

	public static void record360() {
		Image3DUniverse univ = getUniv();
		if(univ == null)
			return;
		ImagePlus movie = univ.record360();
		if(movie != null)
			movie.show();
	}

	public static void startFreehandRecording() {
		Image3DUniverse univ = getUniv();
		if(univ != null) univ.startFreehandRecording();
	}

	public static void stopFreehandRecording() {
		Image3DUniverse univ = getUniv();
		if(univ == null)
			return;
		ImagePlus movie = univ.stopFreehandRecording();
		if(movie != null)
			movie.show();
	}

	public static void close() {
		threshold = 0;
		isThresholdSet = false;
		wasCancelled = false;
		Image3DUniverse univ = getUniv();
		if(univ != null) {
			univ.close();
		}
	}

	public static void select(String name) {
		Image3DUniverse univ = getUniv();
		if(univ != null) univ.select(
			(Content)univ.getContent(name));
	}

	// Contents menu
	public static void add(String image, String c, String name,
		String th, String r, String g, String b,
		String resamplingF, String type) {

		Image3DUniverse univ = getUniv();
		ImagePlus grey = WindowManager.getImage(image);
		Color3f color = ColorTable.getColor(c);

		int factor = getInt(resamplingF);
		int thresh = getInt(th);
		boolean[] channels = new boolean[]{getBoolean(r),
						getBoolean(g), 
						getBoolean(b)};
		int ty = getInt(type);
		univ.addContent(grey, color, 
			name, thresh, channels, factor, ty);
	}

	public static void addVolume(String image, String c, String name,
			String r, String g, String b, String resamplingF) {

		Image3DUniverse univ = getUniv();
		ImagePlus grey = WindowManager.getImage(image);
		Color3f color = ColorTable.getColor(c);

		int factor = getInt(resamplingF);
		boolean[] channels = new boolean[]{getBoolean(r),
						getBoolean(g), 
						getBoolean(b)};
		univ.addVoltex(grey, color, name, 0, channels, factor);
	}

	public static void addOrthoslice(String image, String c, String name,
			String r, String g, String b, String resamplingF) {

		Image3DUniverse univ = getUniv();
		ImagePlus grey = WindowManager.getImage(image);
		Color3f color = ColorTable.getColor(c);

		int factor = getInt(resamplingF);
		boolean[] channels = new boolean[]{getBoolean(r),
						getBoolean(g), 
						getBoolean(b)};
		univ.addOrthoslice(grey, color, name, 0, channels, factor);
	}

	public static void delete() {
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null) {
			univ.removeContent(univ.getSelected().getName());
		}
	}


	// Individual content's menu
	public static void setSlices(String x, String y, String z) {
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null && 
			univ.getSelected().getType() == Content.ORTHO) {

			OrthoGroup vg = (OrthoGroup)univ.
						getSelected().getContent();
			vg.setSlice(VolumeRenderer.X_AXIS, getInt(x));
			vg.setSlice(VolumeRenderer.Y_AXIS, getInt(y));
			vg.setSlice(VolumeRenderer.Z_AXIS, getInt(z));
		}
	}

	public static void fillSelection() {
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null && 
			univ.getSelected().getType() == Content.VOLUME) {

			VoltexGroup vg = (VoltexGroup)univ.
						getSelected().getContent();
			ImageCanvas3D canvas = (ImageCanvas3D)univ.getCanvas();
			vg.fillRoi(canvas, canvas.getRoi(), (byte)0);
		}
	}

	public static void lock() {
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setLocked(true);
		}
	}

	public static void unlock() {
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setLocked(false);
		}
	}

	public static void setChannels(String red, String green, String blue) {
		Image3DUniverse univ = getUniv();
		boolean r = Boolean.valueOf(red).booleanValue();
		boolean g = Boolean.valueOf(green).booleanValue();
		boolean b = Boolean.valueOf(blue).booleanValue();
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setChannels(new boolean[]{r, g, b});
		}
	}

	public static void setColor(String red, String green, String blue) {
		Image3DUniverse univ = getUniv();
		if(univ == null || univ.getSelected() == null)
			return;
		Content sel = univ.getSelected();
		try {
			float r = getInt(red) / 256f;
			float g = getInt(green) / 256f;
			float b = getInt(blue) / 256f;
			if(univ != null && univ.getSelected() != null) {
				sel.setColor(new Color3f(r, g, b));
			}
		} catch(NumberFormatException e) {
			sel.setColor(null);
		}
	}

	public static void setTransparency(String t) {
		Image3DUniverse univ = getUniv();
		float tr = Float.parseFloat(t);
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setTransparency(tr);
		}
	}

	public static void setCoordinateSystem(String s) {
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().showCoordinateSystem(
				getBoolean(s));
		}
	}

	public static void setThreshold(String s) {
		threshold = getInt(s);
		isThresholdSet = true;
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setThreshold(getInt(s));
		}
	}
		
	public static void presetThreshold(String s) {
		threshold = getInt(s);
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setThreshold(getInt(s));
		}
	}
		
	public static boolean isThresholdSet() {
		return isThresholdSet;
	}
		
	public static int getThreshold() {
		return threshold;
	}
	
	public static boolean wasCancelled() {
		return wasCancelled;
	}
	
	public static void setCancelled() {
		wasCancelled = true;
	}
	
	public static void applyTransform(String transform) {
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null) {
			String[] s = ij.util.Tools.split(transform);
			float[] m = new float[s.length];
			for(int i = 0; i < s.length; i++) {
				m[i] = Float.parseFloat(s[i]);
			}
			univ.getSelected().applyTransform(new Transform3D(m));
		}
	}

	public static void resetTransform() {
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null) {
			univ.getSelected().setTransform(new Transform3D());
		}
	}

	public static void saveTransform(String transform, String path) {
		String[] s = ij.util.Tools.split(transform);
		float[] m = new float[s.length];
		for(int i = 0; i < s.length; i++) {
			m[i] = Float.parseFloat(s[i]);
		}
		new bii.math3d.Transform_IO().saveAffineTransform(m);
	}

	public static void setTransform(String transform) {
		Image3DUniverse univ = getUniv();
		if(univ != null && univ.getSelected() != null) {
			String[] s = ij.util.Tools.split(transform);
			float[] m = new float[s.length];
			for(int i = 0; i < s.length; i++) {
				m[i] = Float.parseFloat(s[i]);
			}
			univ.getSelected().setTransform(new Transform3D(m));
		}
	}

	public static void setLocation(int x, int y) {
		getUniv().getWindow().setLocation(x, y);
	}
	
	public static void changeThreshold() {
		Image3DUniverse univ = getUniv();
		univ.getExecuter().changeThreshold(univ.getSelected());
	}
	
	public static void displayAsSurface() {
		Image3DUniverse univ = getUniv();
		univ.getExecuter().displayAs(univ.getSelected(), Content.SURFACE);
	}
	
	public static void unshadeSurface() {
		Image3DUniverse univ = getUniv();
		Content c = univ.getSelected();
		univ.getExecuter().setShaded(c, false);
		((Image3DMenubar) univ.getMenuBar()).getShadedCheckbox().setState(false);
	}
	
	private static int getInt(String s) {
		return Integer.parseInt(s);
	}

	private static boolean getBoolean(String s) {
		return new Boolean(s).booleanValue();
	}
}
