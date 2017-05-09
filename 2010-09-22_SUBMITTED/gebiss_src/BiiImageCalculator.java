

import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;
import ij.measure.Calibration;

public class BiiImageCalculator {

	static void doStackOperation(ImagePlus img1, ImagePlus img2) {
		int size1 = img1.getStackSize();
		int size2 = img2.getStackSize();
		if (size1>1 && size2>1 && size1!=size2) {
			IJ.error("Image Calculator", "'Image1' and 'image2' must be stacks with the same\nnumber of slices, or 'image2' must be a single image.");
			return;
		}
		int mode = Blitter.ADD;
		ImageWindow win = img1.getWindow();
		if (win!=null)
			WindowManager.setCurrentWindow(win);
		Undo.reset();
		ImageStack stack1 = img1.getStack();
		StackProcessor sp = new StackProcessor(stack1, img1.getProcessor());
		try {
			if (size2==1)
				sp.copyBits(img2.getProcessor(), 0, 0, mode);
			else
				sp.copyBits(img2.getStack(), 0, 0, mode);
		}
		catch (IllegalArgumentException e) {
			IJ.error("\""+img1.getTitle()+"\": "+e.getMessage());
			return;
		}
		img1.setStack(null, stack1);
		if (img1.getType()!=ImagePlus.GRAY8) {
			img1.getProcessor().resetMinAndMax();
		}
		img1.updateAndDraw();
	}

	ImageProcessor createNewImage(ImageProcessor ip1, ImageProcessor ip2) {
		int width = Math.min(ip1.getWidth(), ip2.getWidth());
		int height = Math.min(ip1.getHeight(), ip2.getHeight());
		ImageProcessor ip3 = ip1.createProcessor(width, height);
		ip3.insert(ip1, 0, 0);
		return ip3;
	}

	static ImagePlus duplicateStack(ImagePlus img1) {
		Calibration cal = img1.getCalibration();
		ImageStack stack1 = img1.getStack();
		int width = stack1.getWidth();
		int height = stack1.getHeight();
		int n = stack1.getSize();
		ImageStack stack2 = img1.createEmptyStack();
		try {
			for (int i=1; i<=n; i++) {
				ImageProcessor ip1 = stack1.getProcessor(i);
				ip1.resetRoi(); 
				ImageProcessor ip2 = ip1.crop();
				stack2.addSlice(stack1.getSliceLabel(i), ip2);
			}
		}
		catch(OutOfMemoryError e) {
			stack2.trim();
			stack2 = null;
			return null;
		}
		ImagePlus img3 = new ImagePlus("Result of "+img1.getShortTitle(), stack2);
		img3.setCalibration(cal);
		if (img3.getStackSize()==n) {
			int[] dim = img1.getDimensions();
			img3.setDimensions(dim[2], dim[3], dim[4]);
			if (img1.isComposite()) {
				img3 = new CompositeImage(img3, 0);
				((CompositeImage)img3).copyLuts(img1);
			}
			if (img1.isHyperStack())
				img3.setOpenAsHyperStack(true);
		}
		return img3;
	}
	
}
