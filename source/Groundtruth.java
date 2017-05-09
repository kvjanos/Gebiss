
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.Measurements;
import ij.text.TextWindow;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.plugin.frame.ThresholdAdjuster;
import ij.process.ImageProcessor;
import ij.gui.*;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import java.awt.Polygon;
import ij.process.ImageStatistics;
import ij.measure.ResultsTable;
import java.io.*; // For saving output as textfile

public class Groundtruth
{
	ImagePlus imp;
        private ImagePlus imptmpbinary;
        private ImagePlus imptmpmedian;
        private ImagePlus imporiginal16;
        
        int[][] strtx; 
        int[][] strty; // imp.getNSlices()
        double[][] minlevel;
        double[][] maxlevel;
        int[] roisPerSlice;
        String windowTitle;
        
        public Groundtruth(String windowTitle) {
        	this.windowTitle = windowTitle;
        }
        
	public int setup( String arg, ImagePlus imp )
	{
		if ( imp == null )
		{
			IJ.showMessage( "No image, please open an image first!!!" );
			return PlugInFilter.DONE;
		}
		this.imp = imp;
                		
		if ( RoiManager.getInstance() != null )
		{
			RoiManager.getInstance().close();
			new RoiManager();
		}
		
		if ( IJ.getTextPanel() != null )
		{
			Analyzer.resetCounter();
			( ( TextWindow )IJ.getTextPanel().getParent() ).dispose();
			IJ.setTextPanel( null );
		}
		
		return PlugInFilter.DOES_ALL;
	}

	public void run( ImageProcessor ip )
	{
               
                strtx = new int[imp.getImageStackSize()][65536];//[65536]; // z slice no. and 65536 max obj(roi) no. per slice 
                strty = new int[imp.getImageStackSize()][65536];//[65536]; // imp.getNSlices()
                minlevel = new double[imp.getImageStackSize()][65536];//[65536];
                maxlevel = new double[imp.getImageStackSize()][65536];//[65536];
                
                // if image is not 8 bit grey scale, convert it
            
		if ( imp.isLocked() )
		{
			imp.unlock();
		}
		
                //imp.setTitle("original");
                
		// use pixel as default unit
                // run macro Analyze/Set Scale...
		new Interpreter().run( "run(\"Set Scale...\", \"distance=0 known=1 pixel=1 unit=pixel\");" );
		
		
                convertImageType();
		/*
		if ( imp.getType() != ImagePlus.GRAY8 )
		{
			imp.unlock();
			new Converter().run( "8-bit" );
		}
		 */
		
		// apply threshold
		if ( imp.isLocked() )
		{
			imp.unlock();
		}                
                // Hides ThresholdAdjuster window ("Red","Black & White", "Over/Under")
//		new ThresholdAdjuster().close();                                           
//                IJ.run("Threshold...");
                
				IJ.selectWindow(windowTitle);
				int[] dimensions = WindowManager.getCurrentImage().getDimensions();
				IJ.makeRectangle(0, 0, dimensions[0], dimensions[1]);
                IJ.run("Duplicate...", "title=tmpmedian duplicate");
                imptmpmedian = WindowManager.getCurrentImage();
                //imptmpmedian.hide();
                IJ.run("Duplicate...", "title=original16 duplicate");
                imporiginal16 = WindowManager.getCurrentImage();                
                //imporiginal16.hide();
                IJ.run("Duplicate...", "title=original duplicate");
                
                // prepare original stack for Selection Draw
                IJ.selectWindow("original");
                //IJ.run("8-bit");
                IJ.setForegroundColor(255, 255, 255); //IJ.runMacro("setColor("65535");"); //imp.setColor(255); //65535
                
                WindowManager.setTempCurrentImage(imptmpmedian);
                //imptmpmedian.hide();
                //IJ.selectWindow("tmpmedian");                
                String thr[] = new String[] {"Manual", "IsoData", "Otsu"};
                GenericDialog gd = new GenericDialog("Select a method ");
                  gd.addCheckbox("Median filter",true);
                  gd.addNumericField("radius", 3, 1);
                  double medradius = gd.getNextNumber();
                  gd.addChoice("Threshold method", thr,thr[0]);
                  gd.showDialog();
                  if(gd.wasCanceled()) {
                   IJ.error("PlugIn canceled!");
                   return;} 
                  int choice = gd.getNextChoiceIndex(); 
                  //gd.setVisible(false);
                
                // Apply median filter
                if (gd.getNextBoolean() == true) {
                    IJ.run("Median...", "radius="+medradius+" stack"); // imptmpmedian
                    IJ.selectWindow(1);
                    IJ.run("Median...", "radius="+medradius+" stack"); // tmpmedian
                    IJ.selectWindow(3);
                    IJ.run("Median...", "radius="+medradius+" stack"); // original16
                    IJ.selectWindow("original");
                    IJ.run("Median...", "radius="+medradius+" stack");
                    WindowManager.setTempCurrentImage(imptmpmedian);
                }
                
                // Manual Global Thresholding
                if (choice == 0) {
                    convertImageType(); 
                    
                    IJ.selectWindow("tmpmedian");
                    IJ.setSlice(1);
                    
                    //Manual Thresholding: set the global Thr
                    BiiThresholdAdjuster ta = new BiiThresholdAdjuster();
                    while (!ta.done) {
                        try {Thread.sleep(1000);}
                        catch (Exception e) {}
                    }
                    
                    IJ.selectWindow("original16");                            
                    IJ.setSlice(1);
                    IJ.selectWindow("tmpmedian");
                    IJ.setSlice(1);

                	roisPerSlice = new int[imp.getImageStackSize()];
                    int last = imp.getImageStackSize()-1; // last slice number
                    for (int z = 0; z < imp.getImageStackSize(); z++){      // scans all z slices
                        
                        // Set the original stack to the next slice, except the first and last slice                        
                        if ( imp.getImageStackSize() > 0 && z > 0 && z != last) // if stack and greater than the first slice and not the last slice
                            {                            
                                imp.setSlice( z+1 );
                                //System.out.println(imp.getSlice());
                                IJ.selectWindow("original16");                            
                                IJ.setSlice(z+1); //IJ.run("Next Slice [>]");
                                IJ.selectWindow("tmpmedian");
                                IJ.setSlice(z+1); //IJ.run("Next Slice [>]");
                            }
                        if ( z == last )
                            {                            
                                imp.setSlice(imp.getImageStackSize());
                                //System.out.println(imp.getSlice());
                                IJ.selectWindow("original16");                            
                                IJ.setSlice(imp.getImageStackSize()); //IJ.run("Next Slice [>]");
                                IJ.selectWindow("tmpmedian");
                                IJ.setSlice(imp.getImageStackSize()); //IJ.run("Next Slice [>]");
                            }                      

                        
                        // Calculate StartX, StartY, Min, Max parameters for each ROI in the stack
                        // and generate a preview stack
                        CoordExtract(z);
                    }
                } 
                
                // IsoData
                if (choice == 1) {
                //IJ.run("setAutoThreshold");   
                //ip.setAutoThreshold(1,0); // 0:"ISODATA", 1:ISODATA2; 0:RED_LUT 2:"NO_LUT_UPDATE"                    
                                        
                    int last = imp.getImageStackSize()-1; // last slice number
                    for (int z = 0; z < imp.getImageStackSize(); z++){      // scans all z slices  
                        
                    // Set the original stack to the next slice, except the first and last slice                        
                        if ( imp.getImageStackSize() > 0 && z > 0 && z != last) // if stack and greater than the first slice and not the last slice
                            {                            
                                imp.setSlice( z+1 );
                                //System.out.println(imp.getSlice());
                                IJ.selectWindow("original");                            
                                IJ.setSlice(z+1); //IJ.run("Next Slice [>]");
                                IJ.selectWindow("tmpmedian");
                                IJ.setSlice(z+1); //IJ.run("Next Slice [>]");
                            }
                        if ( z == last )
                            {                            
                                imp.setSlice(imp.getImageStackSize());
                                //System.out.println(imp.getSlice());
                                IJ.selectWindow("original");                            
                                IJ.setSlice(imp.getImageStackSize()); //IJ.run("Next Slice [>]");
                                IJ.selectWindow("tmpmedian");
                                IJ.setSlice(imp.getImageStackSize()); //IJ.run("Next Slice [>]");
                            }
                        
                        // IsoData thresholding
                        new ThresholdAdjuster().close();
                        
                        // Calculate StartX, StartY, Min, Max parameters for each ROI in the stack
                        // and generate a preview stack
                        CoordExtract(z);                                                
                    }
                }
                  
                if (choice == 2) {
                //Otsu
                    
                    int last = imp.getImageStackSize()-1; // last slice number
                    for (int z = 0; z < imp.getImageStackSize(); z++){      // scans all z slices  
                        
                    // Set the original stack to the next slice, except the first and last slice                        
                        if ( imp.getImageStackSize() > 0 && z > 0 && z != last) // if stack and greater than the first slice and not the last slice
                            {                            
                                imp.setSlice( z+1 );
                                //System.out.println(imp.getSlice());
                                IJ.selectWindow("original");                            
                                IJ.setSlice(z+1); //IJ.run("Next Slice [>]");
                                IJ.selectWindow("tmpmedian");
                                IJ.setSlice(z+1); //IJ.run("Next Slice [>]");
                            }
                        if ( z == last )
                            {                            
                                imp.setSlice(imp.getImageStackSize());
                                //System.out.println(imp.getSlice());
                                IJ.selectWindow("original");                            
                                IJ.setSlice(imp.getImageStackSize()); //IJ.run("Next Slice [>]");
                                IJ.selectWindow("tmpmedian");
                                IJ.setSlice(imp.getImageStackSize()); //IJ.run("Next Slice [>]");
                            }
                        
                        // Otsu thresholding
                        //new ThresholdAdjuster().close();
                        
                        // Calculate StartX, StartY, Min, Max parameters for each ROI in the stack
                        // and generate a preview stack
                        CoordExtract(z);
                    }
                }
                  
                IJ.selectWindow("tmpmedian");
                //IJ.run("Close");

                               
//		// set the default analyzer measurement values (Analyze/Set Measurements)
//		int measurements = Analyzer.getMeasurements();
//		int tempMeasurements = 0;
//		int precision = Analyzer.getPrecision();
//		
//		tempMeasurements |= Measurements.MIN_MAX;
//		tempMeasurements |= Measurements.RECT;
////                tempMeasurements |= imp.HEIGHT/2;
//                
//		
//		Analyzer.setMeasurements( tempMeasurements );
//		Analyzer.setPrecision( 0 );
//		
//		// run the particle analyzer
//		//new Executer( "Analyze Particles...", null );
//                
//		if ( imp.isLocked() )
//		{
//			imp.unlock();
//		}
//		ParticleAnalyzer pa = new ParticleAnalyzer( 0, tempMeasurements, Analyzer.getResultsTable(), 0, 0 );
//		if ( pa.setup( "", imp ) == PlugInFilter.DONE )
//		{
//			return;
//		}
//		pa.run( imp.getProcessor() );
//		 /*
//		if ( !pa.analyze( imp ) )
//		{
//			IJ.error( "Particle Analyzer", "Validation error!" );
//		}
//			IJ.runPlugIn( "ij.plugin.filter.ParticleAnalyzer", "" );
//		  */
//		
//		// revert back the image to original, and convert it to 8 bit grey scale
//		// if it is not
//		// set the ImagePlus changes to false to prevent confirmation dialog to popup
//		imp.changes = false;
//		imp.revert();
//
//		if ( imp.isLocked() )
//		{
//			imp.unlock();
//		}
//		convertImageType();
//		/*
//		if ( imp.getType() != ImagePlus.GRAY8 )
//		{
//			new Converter().run( "8-bit" );
//		}
//		 */
//		
                
                
		// activate the Gebiss (segmentation assistant)
                //Gebiss_ Gebiss_().run(imporiginal16);
                //Gebiss_ gb = new Gebiss_();
                Gebiss_ gb = Gebiss_.getInstance();
             
                //gb.run(imporiginal16);
		gb.addROIs(strtx, strty, minlevel, maxlevel, roisPerSlice);
                
//		SegmentingAssistant_ sa = new SegmentingAssistant_();
//		sa.run( imp );
//		//sa.makeWindow();

//		// revert back the original analyzer measurement
//		Analyzer.setMeasurements( measurements );
//		Analyzer.setPrecision( precision );
		
		//imp.close();
		//new Opener().open( imp.getOriginalFileInfo().directory + imp.getOriginalFileInfo().fileName );
	}
	
	private void convertImageType()
	{
		switch ( imp.getType() )
		{
			case ImagePlus.COLOR_RGB:
				//new Converter().run( "32-bit" );
				IJ.runPlugIn( "ij.plugin.Converter", "32-bit" );
				break;
			case ImagePlus.COLOR_256:
				//new Converter().run( "8-bit" );
				IJ.runPlugIn( "ij.plugin.Converter", "8-bit" );
				break;
			default:
				break;
		}
	}
        
        private void CoordExtract(int z) {
                       
                // size filter                
                IJ.run("Analyze Particles...", "size=100-Infinity circularity=0.00-1.00 show=Nothing clear add");//include
                IJ.newImage("tmpbinary", "8-bit White", 1024, 1024, 1);
                imptmpbinary = WindowManager.getCurrentImage();
                //System.out.println("imptmpbinary: " + imptmpbinary);
                //imptmpbinary.isVisible();
                IJ.setForegroundColor(0, 0, 0);
                IJ.runMacro("roiManager(\"Fill\")");             

                // get StartX, StartY, Min level, Max level of all ROIs
                IJ.selectWindow("tmpbinary");
                // Set to measure Min level & Max level (min) on the original slice (redirect=original)
                IJ.run("Set Measurements...", "min redirect=tmpmedian decimal=0"); // display redirect=tmpmediangray
                IJ.run("Analyze Particles...", "size=0-Infinity circularity=0.00-1.00 show=Nothing clear add"); //include                                                       
                RoiManager roiManager=RoiManager.getInstance(); 
                int roiN = roiManager.getCount();       // the number of ROIs on the slice
                if (roiN == 0){
                    IJ.selectWindow("tmpbinary");
                    IJ.run("Close");
                    return;
                }
                Roi[] rois = roiManager.getRoisAsArray();
                	roisPerSlice[z] = roiN;
                    for (int j = 0; j < roiN; j++){                     // scans all ROIs on the slice
                        Roi polygon = rois[j];                        
                        Polygon proi = polygon.getPolygon();
                        int[] xCoords = proi.xpoints;
                        int[] yCoords = proi.ypoints;                         
                        int minX = imp.getWidth();
                        int minY = imp.getHeight();
                        int maxY = 0;
                        for (int i = 0; i < proi.npoints; i++) {        // scans all vertex coords of the ROI                            
                            if (minX > xCoords[i]) minX = xCoords[i];
                            if (minY > yCoords[i]) minY = yCoords[i];
                            if (maxY < yCoords[i]) maxY = yCoords[i];
                        }
                        strtx[z][j] = (minX - 1) ;  // StartX = the smallest x coordinate of a ROI minus 1
                        strty[z][j] = maxY - ((maxY - minY)/2); // StartY = at half of the ROI's height
                        
                        // get MinLevel, MaxLevel of all ROIs
                        ResultsTable rt = ResultsTable.getResultsTable();
                        //minlevel[z][j] = ij.plugin.frame.ThresholdAdjuster.minThreshold;
                        minlevel[z][j] = rt.getValue("Min", j);                        
                        maxlevel[z][j] = rt.getValue("Max", j);               

//System.out.println("z: "+z+" j: "+j+" x: "+strtx[z][j]+" y: "+strty[z][j]+" min: "+minlevel[z][j]+" max: "+maxlevel[z][j]);                                
						// !!! INNEN FOLYTATNI 
                        // !!! save strtx[][],strty[][] min, max as "project file": bench.java alapjan sorokba es egy plusz oszlop a Z-nek
                        //BufferedWriter bw = new BufferedWriter(new FileWriter(dir+filename));

                    }
                
                    // Based on /mnt/disk2/2007-07-19/SelMacro.txt
                        IJ.selectWindow("tmpbinary");
                        IJ.run("Create Selection");
                        IJ.selectWindow("original16");
                        if (z==0) IJ.setSlice(1);
                        IJ.run("Restore Selection");
                        IJ.setForegroundColor(255, 255, 255);
                        IJ.run("Draw");
                        IJ.run("Select None");
                        // No need to save, the stack can be saved with ImageJ if needed
                        
                        IJ.selectWindow("tmpbinary");
                        IJ.run("Close");
                        //System.out.println("break");

                }
        
}