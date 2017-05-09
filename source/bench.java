

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.awt.*;

import ij.plugin.*;
import ij.plugin.filter.*;

import java.text.NumberFormat;
import java.util.*;  // System.out.println() -hez
import java.io.*;  // saving output as textfile-hoz

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import ij.io.*;
import ij.plugin.filter.Analyzer;
//import ij.plugin.Histogram;
//import java.lang.String;
/*import ij.process.FloatStatistics;
import ij.process.TypeConverter;*/
import ij.gui.*;

public class bench
{ImagePlus imp;
 ImageProcessor ip; 
 
 ImageStatistics stats = null;
    private static String title1 = "";
    private static String title2 = "";
    private static boolean createWindow = true;
    private static boolean bench3D = true;
	private Gebiss_ sa = null;
	private Gebiss_.BenchFrame frame = null;
	private static DefaultTableModel model = null;
	private int curRow = -1;
	private int curCol = -1;
	
    int Width;
    int Height;
    int x;
    int y;
    int i;
    int j;
    int a;
    int b;
    int m;
    int n;
    int TN;
    int nBins1;
    int nBins2;
    int size1;
    int size2;
    int stk1_max;
    int stk2_max;
    int[] stkgrays1;
    int[] stkgrays2;
    int[][] pict1; // img1 2D matrix
    int[][] pict2; // img2 2D matrix
    int PixVal1;
    int PixVal2;
    int[] grays1; // img1 (MS) labels' gray intesity values
    int[] grays2; // img2 (GT) labels' gray intesity values
    int[] gtarea; // img2 labels' area values
    int pxval1;
    int pxval2;
    int I1_max; // object number in img1
    int I2_max; // object number in img2
    int val;
    int[] s_I1;
    int[] s_I2;
    int[] FP; // false positives
    int[][] TP; // true positives
    int[] FN; // false negatives
    float [][] TPR; // true positive rate
    float [][] FNR; // false negative rate
    float [] TNR; // true negative rate,
    float [] FPR; // true negative rate
    //float [] AOR; // area_overlap_ratio: ratio of MS area overlapped with GT area
    int[][] MRG; // merge img1 objects identifier numbers
    int [] FP_MS_obj; // labels of FP MS objects  To label FP MS objects.
    int [] FN_GT_obj; // labels of FN GT objects  To label FN GT objects.
    int t_FP_MS_obj = 0; // total number of FP MS objects To label FP MS objects.
    int t_FN_GT_obj = 0; // total number of FN GT objects To label FN GT objects.
    int[][] SPL; // split img2 objects identifier numbers
    int[][] PT; // rotation of TP array (GT obj=n, MS obj=m): transpose of the matrix TP[n][m] to PT[m][n]
    float [] stk_t_fpr; // total fpr of all benchmarks in the stack
    float [] stk_t_fnr; // total fnr of all benchmarks in the stack
    float [] stk_f; // F-measure values of all benchmarks in the stack    
    float tt_fp = 0;    // Total number of FP pixels per 3D stack (total total)
    float tt_tp = 0;    // Total number of TP pixels per 3D stack (total total)
    float tt_fn = 0;    // Total number of FN pixels per 3D stack (total total)
    float ttn = 0;      // Total number of TN pixels per 3D stack (total total)
    int [] GTlabels; // GT labels' gray intesity values
	int [] MSlabels; // MS labels' gray intesity values

    public bench(Gebiss_ sa, Gebiss_.BenchFrame frame) {
    	this.sa = sa;
    	this.frame = frame;
    }
    
    public int setup(String arg, ImagePlus imp) {
    this.imp = imp;
    return PlugInFilter.DOES_16+PlugInFilter.DOES_8G; // DOES_ALL;
    }

    
    public void run(ImageProcessor ip) {
    	
        // Load the 2 labeled images based on
        // /home/janoskv/ImageJ_NetBeans/source/ij/plugin/ImageCalculator.java
        int[] wList = WindowManager.getIDList();
        if (wList == null) {
            IJ.noImage();
            return;
        }
        IJ.register(ImageCalculator.class);
        String[] titles = new String[wList.length];
        for (int i = 0; i < wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp != null) {
                titles[i] = imp.getTitle();
            } else {
                titles[i] = "";
            }
        }
        GenericDialog gd = new GenericDialog("Benchmark", IJ.getInstance());
        String defaultItem;
        if (title1.equals("")) {
            defaultItem = titles[0];
        } else {
            defaultItem = title1;
        }
        gd.addChoice("Machine segmentation (MS) image or stack:", titles, defaultItem);          //"Image1:"
        if (title2.equals("")) {
            defaultItem = titles[0];
        } else {
            defaultItem = title2;
        }
        gd.addChoice("Ground truth (GT) image or stack:", titles, defaultItem); // "Image2:"
        gd.addCheckbox("3D Benchmark", true);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        int index1 = gd.getNextChoiceIndex();
        title1 = titles[index1];
        int index2 = gd.getNextChoiceIndex();
        bench3D = gd.getNextBoolean();
        //createWindow = gd.getNextBoolean();
        title2 = titles[index2];        
        final ImagePlus img1 = WindowManager.getImage(wList[index1]);
        final ImagePlus img2 = WindowManager.getImage(wList[index2]);

  
        
        // Get the number of stack slices
        int NbSlices1 = img1.getImageStackSize();
        int NbSlices2 = img2.getImageStackSize();
        if (NbSlices1!=NbSlices2){
            IJ.showMessage("Error. Stack slice numbers are not equal...");
            return;
        }
        int NbSlices = NbSlices1;

        if (img1.getHeight() != img2.getHeight()){
            IJ.showMessage("Error. GT and MS image heights are not equal...");
            return;
        }
        
        if (img1.getWidth() != img2.getWidth()){
            IJ.showMessage("Error. GT and MS image widths are not equal...");
            return;
        }
        
        // 3D benchmarking
        if (bench3D == true){
        
            model = (DefaultTableModel) frame.getTable().getModel();
            for(int i=0;i<model.getRowCount(); i++) {
            	   model.removeRow(i);
            	}

        /*
        //Bench3D(img1,img2,NbSlices);
               
        //IJ.run("Duplicate...", "title1 title=MSbin duplicate range=1-NbSlices"); 
        //IJ.run("Convert to Mask", "  black");
        
        //ImageStack MSstk = img1.getStack();
        ////ImageProcessor MSstk = img1.getProcessor();        
        //ImageProcessor MSbin = MSstk.duplicate();
        //////MSbin.setAutoThreshold(1,1);
        //MSbin.threshold(2);
        //ImagePlus impMSbin = new ImagePlus("MS_bin",MSbin);
        //impMSbin.show();
        
        //ImagePlus MSstk = IJ.getImage(); 
        
        ImagePlus MSbin = (new Duplicater()).duplicateStack(img1, "MSbin");  
        
        ImageProcessor MSstk = MSbin.getProcessor();
        MSstk = IJ.getImage().getProcessor();
        //ImagePlus MSstkImp = new ImagePlus("MSbin",MSstk.duplicate());
        ImagePlus MSstkImp = (new Duplicater()).duplicateStack(MSbin, "MSbin");
        MSstk.resetMinAndMax();
        MSstk.setThreshold(0.8, 1000000, FloatProcessor.RED_LUT);
        
        MSstkImp.show();
 
        //ImagePlus MSbin = (new Duplicater()).duplicateStack(img1, "MSbin");
        //ImageProcessor MSip = IJ.getImage().getProcessor();
        //MSip.multiply(1000000.0);
        //ImagePlus MSstkImp = new ImagePlus("MSbin",MSip.duplicate());
        //IJ.run(GTstkImp, "Invert", "stack");
         */
        
        final ImagePlus MSstkImp = (new Duplicater()).duplicateStack(img1, "MSbin");
        ImageProcessor MSstk = MSstkImp.getProcessor();
        MSstk.resetMinAndMax();
        double thr1 = ip.getMinThreshold();
        double thr2 = ip.getMaxThreshold();
        MSstk.setThreshold( thr1, thr2, FloatProcessor.NO_LUT_UPDATE); // 0.8, 10000000, FloatProcessor.RED_LUT
        IJ.run(MSstkImp, "Convert to Mask",  " ");
        IJ.run(MSstkImp, "Invert", "stack");
        IJ.run(MSstkImp, "Invert LUT", "");
        //MSstkImp.show(); 
        
        
        final ImagePlus GTstkImp = (new Duplicater()).duplicateStack(img2, "GTbin");
        ImageProcessor GTstk = GTstkImp.getProcessor();
        GTstk.resetMinAndMax();
        double thr3 = ip.getMinThreshold();
        double thr4 = ip.getMaxThreshold();
        GTstk.setThreshold( thr3, thr4, FloatProcessor.NO_LUT_UPDATE); // 0.8, 10000000, FloatProcessor.RED_LUT
        IJ.run(GTstkImp, "Convert to Mask", " "); //  "  black"
        IJ.run(GTstkImp, "Invert", "stack");
        IJ.run(GTstkImp, "Invert LUT", "");
        //GTstkImp.show();   
        
        final ImageCalculator calc = new ImageCalculator(); 
    	Thread[] threads = new Thread[3];
        class FNbinThread extends Thread {
        		private ImagePlus FNbin;
        	   public void run() {
   	            //calc.calculate("Subtract create stack", GTstkImp, MSstkImp);
   	        	FNbin = calc.run("Subtract create stack", GTstkImp, MSstkImp);
   	            FNbin.setTitle("FNbin"); // Rename image window
   	            //IJ.run(FNbin, "Invert LUT", "");
   	            IJ.run(FNbin, "Invert", "stack");
   	            //FNbin.show();
        	   }
        	   ImagePlus getImagePlus() {
        		   return FNbin;
        	   }
        	}
    	threads[0] = new FNbinThread();
        
        class FPbinThread extends Thread {
    		private ImagePlus FPbin;
    	   public void run() {
	            //calc.calculate("Subtract create stack", GTstkImp, MSstkImp);
	        	FPbin = calc.run("Subtract create stack", GTstkImp, MSstkImp);
	            FPbin.setTitle("FNbin"); // Rename image window
	            //IJ.run(FNbin, "Invert LUT", "");
	            IJ.run(FPbin, "Invert", "stack");
	            //FNbin.show();
    	   }
    	   ImagePlus getImagePlus() {
    		   return FPbin;
    	   }
    	}
    	threads[1] = new FPbinThread();
        
        class TPbinThread extends Thread {
    		private ImagePlus TPbin;
    	   public void run() {
	            TPbin = calc.run("AND create stack", GTstkImp, MSstkImp);
	            TPbin.setTitle("TPbin"); // Rename image window
	            IJ.run(TPbin, "Invert", "stack");        
	            //TPbin.show();
    	   }
    	   ImagePlus getImagePlus() {
    		   return TPbin;
    	   }
    	}
    	threads[2] = new TPbinThread();

    	startAndJoin(threads);
        ImagePlus FNbin = ((FNbinThread) threads[0]).getImagePlus();
        ImagePlus FPbin = ((FPbinThread) threads[1]).getImagePlus();
        ImagePlus TPbin = ((TPbinThread) threads[2]).getImagePlus();

    	SortedMap levelsGT = getHistogramForLabels(img2);
        Iterator iterGT = levelsGT.keySet().iterator();
        SortedMap levelsMS = getHistogramForLabels(img1);
        Iterator iterMS = levelsMS.keySet().iterator();
        
        // ClearOutside method a Clear_outside.txt macro /media/disk2/macro/Clear_outside_orig_bin_v4.1.txt        
        // -> FN_labeled_GT
        ImagePlus FNLabeledGT = (new Duplicater()).duplicateStack(img2, "FN_labeled_GT");       
        ClearOutside(FNbin, FNLabeledGT); // FNbin=bin, GT_labeled=orig -> FN_labeled_GT
        FNLabeledGT.show();
        SortedMap levels1 = getHistogramForLabels(WindowManager.getCurrentImage());

    	// -> FP_labeled_MS
        ImagePlus FPLabeledMS = (new Duplicater()).duplicateStack(img1, "FP_labeled_MS");
        ClearOutside(FPbin, FPLabeledMS); // FPbin=bin, MS_labeled=orig -> FP_labeled_MS
        FPLabeledMS.show();
        SortedMap levels2 = getHistogramForLabels(WindowManager.getCurrentImage());

        // -> TP_labeled_MS
        ImagePlus TPLabeledMS = (new Duplicater()).duplicateStack(img1, "TP_labeled_MS");       
        ClearOutside(TPbin, TPLabeledMS); // TPbin=bin, MS_labeled=orig -> TP_labeled_MS
        TPLabeledMS.show();
        SortedMap levels3 = getHistogramForLabels(WindowManager.getCurrentImage());

        // -> TP_labeled_GT
        ImagePlus TPLabeledGT = (new Duplicater()).duplicateStack(img2, "TP_labeled_GT"); 
        ClearOutside(TPbin, TPLabeledGT); // TPbin=bin, GT_labeled=orig -> TP_labeled_GT
        TPLabeledGT.show();
        SortedMap levels4 = getHistogramForLabels(WindowManager.getCurrentImage());
        
//      TO DO: Fill up the arrays

        frame.getTable().setAutoCreateColumnsFromModel(false);
        model.addColumn("GT label");
        model.addColumn("GT");
        model.addColumn("TP(GT)");
        model.addColumn("FN(GT)");
        model.addColumn("r");
        model.addColumn("FN object");
        model.addColumn("");
        model.addColumn("MS label");
        model.addColumn("MS");
        model.addColumn("TP(MS)");
        model.addColumn("FP(MS)");
        model.addColumn("p");
        model.addColumn("FP object");
        model.addColumn("F-value");

        int curRow = 0;

        System.out.println("getting r...");
        //Iterator iter4 = levels4.keySet().iterator();
        while(iterGT.hasNext()) {
        	Integer keyGT = (Integer) iterGT.next();
        	Hist count4 = (Hist) levels4.get(keyGT);
        	if (count4 == null) levels4.put(keyGT, new Hist(0));
        	count4 = (Hist) levels4.get(keyGT);
        	Hist count1 = (Hist) levels1.get(keyGT);
        	if (count1 == null) levels1.put(keyGT, new Hist(0));
        	count1 = (Hist) levels1.get(keyGT);
        	float r = (float)count4.getCount() / ((float)count4.getCount() + (float)(count1 == null? 0 : count1.getCount()));
        	count4.setSecondValue(r);
        	if (count4.getCount() == 0 && count1.getCount() > 0) count4.setFourthValue(keyGT);
	    	System.out.println("keyGT="+keyGT+",count="+count4.getCount()+",level1="+count1.getCount()+",r="+count4.getSecondValue()+",FN obj="+count4.getFourthValue());
	        setTableCell(curRow, 0, keyGT+"");
	        setTableCell(curRow, 1, ((Hist)levelsGT.get(keyGT)).getCount()+"");
	        setTableCell(curRow, 2, count4.getCount()+"");
	        setTableCell(curRow, 3, count1.getCount()+"");
	        setTableCell(curRow, 4, count4.getSecondValue()+"");
	        setTableCell(curRow, 5, count4.getFourthValue()+"");
	        curRow++;
        }
        
        curRow = 0;

        System.out.println("getting p...");
        //Iterator iter3 = levels3.keySet().iterator();
        while(iterMS.hasNext()) {
        	Integer keyMS = (Integer) iterMS.next();
        	Hist count3 = (Hist) levels3.get(keyMS);
        	if (count3 == null) levels3.put(keyMS, new Hist(0));
        	count3 = (Hist) levels3.get(keyMS);
        	Hist count2 = (Hist) levels2.get(keyMS);
        	if (count2 == null) levels2.put(keyMS, new Hist(0));
        	count2 = (Hist) levels2.get(keyMS);
        	float p = (float)count3.getCount() / ((float)count3.getCount() + (float)(count2 == null? 0 : count2.getCount()));
        	count3.setSecondValue(p);
        	if (count3.getCount() == 0 && count2 != null && count2.getCount() > 0) count3.setFourthValue(keyMS);
	    	System.out.println("keyMS="+keyMS+",count="+count3.getCount()+",level2="+count2.getCount()+",p="+count3.getSecondValue()+",FP obj="+count3.getFourthValue());
	        setTableCell(curRow, 7, keyMS+"");
	        setTableCell(curRow, 8, ((Hist)levelsMS.get(keyMS)).getCount()+"");
	        setTableCell(curRow, 9, count3.getCount()+"");
	        setTableCell(curRow, 10, count2.getCount()+"");
	        setTableCell(curRow, 11, count3.getSecondValue()+"");
	        setTableCell(curRow, 12, count3.getFourthValue()+"");
	        curRow++;
        }
        
    	System.out.println("getting F...");
        Iterator iterP = levels3.values().iterator();
        while(iterP.hasNext()) {
        	Hist hist3 = (Hist) iterP.next();
            Iterator iterR = levels4.values().iterator();
            while(iterR.hasNext()) {
            	Hist hist4 = (Hist) iterR.next();
            	if (hist3.getCount() == hist4.getCount()) {
            		float F = ((float)2 * hist3.getSecondValue() * (float)hist4.getSecondValue()) / (float)(hist3.getSecondValue() + hist4.getSecondValue());
            		hist3.setFValue(F);
            		hist4.setFValue(F);
            		System.out.println("count="+hist3.getCount()+",F="+F+",p="+hist3.getSecondValue()+",r="+hist4.getSecondValue());
            	}
            }
        }
        
        iterMS = levelsMS.keySet().iterator();
        curRow = 0;
        while(iterMS.hasNext()) {
        	Integer keyMS = (Integer) iterMS.next();
        	Hist count3 = (Hist) levels3.get(keyMS);
	        setTableCell(curRow, 13, count3.getFValue()+"");
	        curRow++;
        }

        frame.getTable().setAutoCreateColumnsFromModel(true);
        } // End of: if (bench3D == true)
        
//        IJ.run("Histogram");        
        
//        Histogram hist = new Histogram();
//        hist.run("");
 
        if (bench3D == false) clearResult();
        
        StackObjNumberCalc(img1, img2); 
        
       stk_t_fpr = new float[NbSlices];
       stk_t_fnr = new float[NbSlices];
       stk_f = new float[NbSlices];
      
       // *** PERFORM THESE ON ALL SLICES IN STACK ***
	     long time1 = System.currentTimeMillis();
	    	Thread[] threads = new Thread[NbSlices];
      for (int z_=0; z_<NbSlices; z_++) {
    	  final int z = z_;
    	  final int NbSlices_ = NbSlices;
  	    threads[z] = new Thread() {

	        public void run() {
       int z1=z+1;
       img1.setSlice(z1);
       img2.setSlice(z1);
       int bitDepth1 = img1.getBitDepth();
       int bitDepth2 = img2.getBitDepth();
        // Fill up the two 2D arrays with pixel values
        // (= convert the 2 labeled images into 2D arrays).
        Width = img1.getWidth();
        Height = img1.getHeight();
        pict1 = new int[Width][Height];
        pict2 = new int[Width][Height];
        I1_max = 0;
        I2_max = 0;
        TN = 0;
        int[] hist1 = null;
        int[] hist2 = null;
                
//                ij.process.FloatStatistics.getStatistics(ip, a, arg2)
                
//                 Determine the max number of labels (gray values) based on the bitdepth of the image. 
//                 GRAY16=short image, GRAY8=byte image (GRAY32=float)
//                if (img1.getType()==ImagePlus.GRAY32){
//                    nBinsFl1 = 2^32; //65536 //2147483647
//                }
                        
		    	grays1 = getHistogram(img1, z1, bitDepth1);
		    	I1_max = size1 = grays1.length;

//               // 32 BITES KEP OBJEKTUM SZAMANAK MEGHATAROZASA (HISZTOGRAM)
//               // minta: ij.process.ShortProcessor.java 858-888. sora, ij.process.StackStatistics.java 68-90.       
//                else if (bitDepth1==32){                 
//                            int[] hist3 = new int[2^31-1];
//                            for (y=1; y<Height;y++) {
//                                for (x=1; x<Width;x++) { 
//                            for (int x=roiY; y<(roiY+roiHeight); y++) {
//                                    int i = y*width + roiX;
//                                    for (int x=roiX; x<(roiX+roiWidth); x++)
//                                            	histogram[pixels[i++]&0xffff]++;
//		}
                ////////////////////////////////////
        
//                            // ITT MEGOLDANI 32 BITES HISZTOGRAM BEOLVASASAT!!!
//                            ImageStatistics stats1 = img1.getStatistics( Analyzer.getMeasurements() );
//                            hist1 = (int[]) stats1.histogram;
 
                            /*FloatProcessor flp = (FloatProcessor)ip.convertToFloat();
                            flp = new FloatProcessor(Width,Height);
                            hist1 = (int[]) flp.getHistogram(); */
                            
                            /*FloatStatistics stats1 = (FloatStatistics) img1.getStatistics(Analyzer.getMeasurements());                            
                            hist1 = (int[]) stats1.histogram;
                             */                           
//                            }
        
                
                
				grays2 = getHistogram(img2, z1, bitDepth2);
				I2_max = size2 = grays2.length;
				/*
                if (bitDepth2==8){
                            ImageStatistics stats2 = img2.getStatistics( Analyzer.getMeasurements() );
                            hist2 = (int[]) stats2.histogram;}
        
		else if (bitDepth2==16){
                            ImageStatistics stats2 = img2.getStatistics( Analyzer.getMeasurements() );
                           hist2 = (int[]) stats2.histogram16;}
		else if (bitDepth2==32){
			grays2 = getHistogram(img2, z1, bitDepth2);
			I2_max = size2 = grays2.length;
            }*/
                
//                else if (bitDepth1==32){                               
//                          // ITT MEGOLDANI 32 BITES HISZTOGRAM BEOLVASASAT!!!
//                            ImageStatistics stats2 = img2.getStatistics( Analyzer.getMeasurements() );
//                            hist2 = (int[]) stats2.histogram;
//                }

                // int nBins = ip.getHistogramSize(); // Default 256 at 8bit image 
                // int[] hist1 = ip.getHistogram(); // Returns histogram only for the current img.
        
//                int[] hist1 = img1.getProcessor().getHistogram(); // Does not work for 32 bit image.
//                int[] hist2 = img2.getProcessor().getHistogram();           
                
//                FloatStatistics fltimg1 = new FloatStatistics(img1);

				/*
                if (bitDepth1!=32) {
                // Determine the number of labels in img1
                for (int gr=1; gr<size1;gr++){ // nBins1 gr=0 is background, that is skipped
                    if (hist1[gr]>0){
                        I1_max=I1_max+1;    
                    }
                }            
                grays1 = new int[I1_max];
                }

                if (bitDepth2!=32) {
                // Determine the number of labels in img2
                for (int gr=1; gr<size2;gr++){ // nBins2 gr=0 is background, that is skipped
                    if (hist2[gr]>0){
                        I2_max=I2_max+1;    
                    }
                }            
                grays2 = new int[I2_max];  
                }

                if (bitDepth1!=32) {
        // Fill up grays1[] with img1 label values
        i=0;
        for (int gr=1; gr<size1;gr++){ // nBins1 gr=0 is background, that is skipped
            if (hist1[gr]>0){
                setlabel1(i,gr);
                i=i+1;
            }
        }
                }

                if (bitDepth2!=32) {
        // Fill up grays2[] with img2 label values
        // Fill GT labels with GT area (gtarea) values
        i=0;
        gtarea = new int[grays2.length];
        for (int gr=1; gr<size2;gr++){ // nBins2 gr=0 is background, that is skipped
            if (hist2[gr]>0){
                setlabel2(i,gr);
                int a = hist2[gr];
                setarea(a,i);
                i=i+1;
            }
        }
                }
                */
                
				/*
                String str1 = "";
                for (int i1 = 0; i1 < grays1.length; i1++) {
                	if (bitDepth1!=32) str1 = str1 + grays1[i1] + ", ";
                	else str1 = str1 + Float.intBitsToFloat(grays1[i1]) + ", ";
                }
                IJ.write("grays1: " + str1);
                String str2 = "";
                for (int i2 = 0; i2 < grays2.length; i2++) {
                	if (bitDepth2!=32) str2 = str2 + grays2[i2] + ", ";
                	else str2 = str2 + Float.intBitsToFloat(grays2[i2]) + ", ";
                }
                IJ.write("grays2: " + str2);
                */
                
        //Load the image1 in a 2 dimension array
        // 3D Object Counter.java line 166 alapjan
        for (y=1; y<Height;y++) {
            for (x=1; x<Width;x++) {             
                int [] tmp1=img1.getPixel(x, y); // extract the pixel intensity
                int [] tmp2=img2.getPixel(x, y);
                PixVal1=tmp1[0];
                PixVal2=tmp2[0];  
                setvalue1(x,y,PixVal1);         // fill the 2D array with the pixel intensity
                setvalue2(x,y,PixVal2);
            }
        }
        
//        int [][] pict3;
//        pict3 = new int [Width][Height]; 
//        int [][] pict4;
//        pict4 = new int [Width][Height];
//        for (x=1; x<Width;x++) { 
//            for (y=1; y<Height;y++) {            
//        pict3 = img1.getProcessor().getIntArray();
//        pict4 = img2.getProcessor().getIntArray();
//            }
//        }

     // Compare the 2 arrays "pixel by pixel" and 
     // fill TP, FP, TN, FN arrays.
     s_I1 = new int [I1_max];       // number of labels (objects) at img1
     s_I2 = new int [I2_max];       // number of labels (objects) at img2
     FP = new int [I1_max];         // number of false positive pixels
     TP = new int [I1_max][I2_max]; // number of true positive pixels
     FN = new int [I2_max];         // number of false negative pixels
     

     // *** PIXEL LEVEL ***
     // 1 to 1 mapping
     for (b=1; b<Height;b++) {          // y coord of pixels
         long t1 = System.currentTimeMillis();
            for (a=1; a<Width;a++) {    // x coord of pixels
                pxval1 = (pict1[a][b]);
                pxval2 = (pict2[a][b]);

                // *** Number of TN pixels ***                      
                if (pxval1==0 && pxval2==0) {
                    TN = TN + 1;    // Number of overlapped background (TN) pixels
                }

                // *** Number of FP pixels ***
                for (i=0; i<I1_max;i++) {   // For all objects in img1.  
                    int gray1=grays1[i];    // Object number i (i-edik) labeled as intensity 'grays1[i]'.
                    // An i labeled img1 object has how many FP   
                    // (=undersegmented [area oversegmentation]) pixels? Pixel number per img1 object).
                    if (pxval1==gray1) { 
                       s_I1[i]=s_I1[i]+1;   // Sizes of img1 objects in pixel.
                       if (pxval2==0) {     // img2 has a background pixel at Img1 object pixel's place. 
                          FP[i]=FP[i]+1;    // Number of FP pixels of img1 object labeled i
                        }
                    }
                }

                // *** Number of TP pixels per object ***
                for (n=0; n<I1_max;n++) {
                  int gray1=grays1[n];          // Number n (n-edik) object labeled as intensity 'grays1[n]'.  
                    for (m=0; m<I2_max;m++) {   // For all objects in img2.
                        int gray2=grays2[m];    // Object number m (m-edik) labeled as intensity 'grays2[m]'.                              
                        if (pxval1==gray1 && pxval2==gray2) {
                            TP[n][m]=TP[n][m]+1; // Number of overlapped (TP) pixels 
                                                 // of img1 object labeled n and img2 object labeled m
                        }
                    }
                }

                //  *** Number of FN pixels ***
                for (j=0; j<I2_max;j++) {   // For all objects in img2.
                    int gray2=grays2[j];    // Object number j (j-edik) labeled as intensity 'grays2[j]'.

                    // A j labeled img2 object has how many FN  
                    // (=oversegmented [area undersegmentation]) pixels? Pixel number per img1 object).
                    if (pxval2==gray2) { 
                       s_I2[j]=s_I2[j]+1;   // Sizes of img2 objects in pixel.
                       if (pxval1==0) {     // img2 has a background pixel at Img1 object pixel's place. 
                          FN[j]=FN[j]+1;    // Number of FN pixels of img2 object labeled j.
                        }
                    }
                }
            }
            long t2 = System.currentTimeMillis();
            IJ.showStatus("Scanning in progress. " + b + "/" + Height + " . " + (t2-t1) + " ms/row");
            IJ.showProgress(b, Height);     // Show progress at ImageJ main progress bar.
        }

        // Rates calculation         
        TPR = new float [I1_max][I2_max];   // array of true positive rate values
        FNR = new float [I1_max][I2_max];   // array of false negative rate values
        TNR = new float [I1_max];           // array of true negative rate values
        FPR = new float [I1_max];           // array of false positive rate values
        float tprval = 0;
        float tnrval = 0;
        float fprval = 0;
        float fnrval = 0;
        float fn = 0;
        float tn = TN;
        ttn = ttn + tn;
        float t_tp = 0; // total TP pixels per image
        float t_fn = 0; // total FN pixels per image
        float t_fp = 0; // total FN pixels per image


        for (m = 0; m < I2_max; m++) {  // For all objects in img2.
            fn = FN[m];
            t_fn = t_fn + fn;           // Total number of FN pixels per image
        }
        tt_fn = tt_fn + t_fn;       // Total number of FN pixels per 3D stack (total total)


        for (n = 0; n < I1_max; n++) {      // For all objects in img1.
            for (m = 0; m < I2_max; m++) {  // For all objects in img2.
                float tp_val = TP[n][m];
                t_tp = t_tp + tp_val;           // Total number of TP pixels per image (= total foreground overlap pixels per image)
                float fn_val = FN[m];
//                    t_fn = t_fn + fn;           // Total number of FN pixels per image
                if (TP[n][m] == 0 && FN[m] == 0) {   // To avoid division by zero
                    continue;
                } else {
                    // Fill up TPR (True Positive Rate) array
                    tprval = tp_val / (tp_val + fn_val);    // TP pixel number of object PER GT foreground pixel number of object
                    tpr(n, m, tprval);
                }
            }
            
            /*
            // Area Overlap Ratio
            AOR = new float[grays2.length];
            for (m = 0; m < I2_max; m++) {      // For all objects in img1.
            	for (n = 0; n < I1_max; n++){  // For all objects in img2.
                    float tp_val = TP[n][m];
                    if (TP[n][m] == 0) {
                        continue;
                    } else {
                        // Fill up AOR (area_overlap_ratio) array
                        int gtaval = gtarea[m];
                        float aorval = tp_val/gtaval;
                        aor(m, aorval);
                    }
                }
            }
            */

            // TNR True Negative Rate
            // FPR False Positive Rate
            float fp = FP[n];
            t_fp = t_fp + fp;       // Total number of FP pixels per image            
            tnrval = tn / (tn + fp);
            fprval = fp / (fp + tn);
            tnr(n, tnrval);
            fpr(n, fprval);
        }
        tt_tp = tt_tp + t_tp;           // Total number of TP pixels per 3D stack (total total)
        tt_fp = tt_fp + t_fp;   // Total number of FP pixels per 3D stack (total total)

         // TPR True Positive Rate per image
         float t_tpr = t_tp/(t_tp+t_fn); // Overlapped foreground area compared to total MS foreground area

         // FNR False Negative Rate per image
         float t_fnr = 0;
         t_fnr = t_fn/(t_tp+t_fn); // "Area undersegmented" compared to total MS foreground area
         stk_t_fnr[z]= t_fnr;

         // TNR True Negative Rate per image
         float t_tnr = tn/(tn+t_fp); // Overlapped background area compared to total MS background area

         // FPR False Positive Rate per image
         float t_fpr = 0;
         t_fpr = t_fp/(t_fp+tn); // "Area oversegmented" compared to total MS foreground area
         stk_t_fpr[z]= t_fpr;

         // Recall (="fedes")        
         float r = t_tp/(t_tp+t_fn); // Ratio between the corretly detected pixels (TP) to the total GT foreground pixels.

         // Precision (="pontossag")
         float p = t_tp/(t_tp+t_fp); // Ratio between the corretly detected pixels (TP) to the total MS foreground pixels.
         
         // F-measure
         float f = 0;
         f = 2*p*r/(p+r);
         stk_f[z]= f;
         
         // absolute spatial accuracy e(k) = FP+FN
         float ek = t_fp+t_fn;
         
         // relative spatial accuracy e'(k) = e(k)/(TP+FP)+(TP+FN)
         float eek = ek/(t_tp+t_fp+t_tp+t_fn);
         
         // objective spatial accuracy
         float vk = 1-eek;

        // *** OBJECT LEVEL ***
        // Many to One (merge, undersegmentation)
        MRG = new int[I1_max][I2_max];      // Labels of GT objects that overlap with merged MS objects.
        FP_MS_obj = new int[I1_max];        // Labels of FP MS objects. To label FP MS objects.        
        int merge = 0;                      // Number of merged objects. 
        int MS_tp_sum;                      // Sum of tp pixels in a row (MS obj) to find MS obj. with only fp pixels.  To label FP MS objects.
        t_FP_MS_obj = 0;                    // Restore the counter of total FP MS obj counter for stack.  To label FP MS objects.
        for (n = 0; n < I1_max; n++) {      // For all objects in img1.
            int m_obj = 0;
            MS_tp_sum = 0;                  // Initially the total tp pixel number in a row supposed to be 0. To label FP MS objects.
            for (m = 0; m < I2_max; m++) {  // For all objects in img2.
                int tp = TP[n][m];          // Here it browses through the TP_table line by line, (MS object by MS object).
                MS_tp_sum = MS_tp_sum + tp; // Sum of tp pixels in a row (MS obj).  To label FP MS objects.
                if (tp > 0) {   
                    m_obj = m_obj + 1;      
                } else {                    
                    continue;
                }
                if (m_obj == 2) {           // If there is a row (MS obj) that has overlap with 2 GT objects, then
                    merge = merge + 1;      // increase the counter of merged objects, and...
                    for (int mrg_gt_obj = 0; mrg_gt_obj < I2_max; mrg_gt_obj++){    // ...rescan that MS row
                        int tp_mrg = TP[n][mrg_gt_obj];
                        if (tp_mrg > 0) {                                           
                            MRG[n][mrg_gt_obj] = grays2[mrg_gt_obj];} // Change from 0 to GT label number at the MRG[MSobj](GTobj] table.                    
                    }
                    continue;
                }
            }
            //  To label FP MS objects.
            if (MS_tp_sum > 0) continue;        // not FP MS object. 
            else{           // MS_tp_sum == 0 if a tp table row (MS obj) has no overlap with any GT obj.
                FP_MS_obj[n] = FP_MS_obj[n]+1;  // Then set that FP_MS_obj[] value 1 from 0...
                t_FP_MS_obj = t_FP_MS_obj+1;    // ... and increase the total number of t_FP_MS_obj.
            }                 
        }
        // One to Many (split, oversegmentation)                  
        // Transpose of the matrix TP[n][m] to PT[m][n]
        PT = new int[I2_max][I1_max];
        for (n = 0; n < I1_max; n++) {     // For all objects in img1.
            for (m = 0; m < I2_max; m++) { // For all objects in img2.
                int tp = TP[n][m];
                pt(m, n, tp);
            }
        }                        
        SPL = new int[I2_max][I1_max];       // SPL[][] is filled with the transposed values to be scannable
        FN_GT_obj = new int[I2_max];         // Labels of FN GT objects. To label FN GT objects.
        int split = 0;                       // number of split objects
        int GT_tp_sum;                       // Sum of tp pixels in a row (GT obj in transposed TP (=PT) table) to find GT obj. with only fn pixels.  To label FN GT objects.
        t_FN_GT_obj = 0;                     // Restore the counter of total FN GT obj counter for stack.  To label FN GT objects.
        for (m = 0; m < I2_max; m++) {       // For all objects in img1.
            int n_obj = 0;
            GT_tp_sum = 0;                   // Initially the total tp pixel number in a row supposed to be 0. To label FN GT objects.
            for (n = 0; n < I1_max; n++) {   // For all objects in img2
                int pt = PT[m][n];
                GT_tp_sum = GT_tp_sum + pt;  // Sum of tp pixels in a row (GT obj in transposed TP (=PT) table).  To label FN GT objects.
                if (pt > 0) {
                    n_obj = n_obj + 1;
                } else {
                    continue;
                }
                if (n_obj == 2) {
                    split = split + 1;
                    for (int spl_gt_obj = 0; spl_gt_obj < I1_max; spl_gt_obj++){    // ...rescan that MS column (transposed row)
                        int tp_spl = PT[m][spl_gt_obj];
                        if (tp_spl > 0) {                                           
                            SPL[m][spl_gt_obj] = grays2[m];} // grays2[spl_gt_obj] // Change from 0 to the split GT obj label number at the SPL[GTobj][MSobj] (transposed)table.                    
                    }
                    continue;
                
                }
            }
            //  To label FN GT objects.
            if (GT_tp_sum > 0) continue;        // Not FN GT object. 
            else{           // GT_tp_sum == 0 if a tp table row (GT obj in transposed TP (=PT) table) has no overlap with any MS obj.
                FN_GT_obj[m] = FN_GT_obj[m]+1;  // Then set that FN_GT_obj[] value 1 from 0...
                t_FN_GT_obj = t_FN_GT_obj+1;    // ... and increase the total number of t_FN_GT_obj.
            }
        }
        
        // Print out results into Result window
        
        //model = (DefaultTableModel) frame.getTable().getModel();
        int curRow = 0;
        if (bench3D == false) {
        model = (DefaultTableModel) frame.getTable().getModel();
        /*
        for(int i=0;i<model.getRowCount(); i++) {
        	model.removeRow(i);
        	}*/
        }


        // *** PIXEL (Whole Image) LEVEL ***
        // 1 to 1 mapping, Many to One, One to Many
        //IJ.write("Slice number: " + z1 + " / " + NbSlices + "\n"); 
        if (bench3D == false) frame.getTable().setAutoCreateColumnsFromModel(false);
        if (bench3D == false) model.addColumn("Slice number: " + z1 + " / " + NbSlices_ + "\n");
        //for (int c = 0; c < z1; c++) frame.getTable().getColumnModel().getColumn(c).setPreferredWidth(400);
        //IJ.write("Number of Ground Truth (GT) objects in this slice: " + I2_max + "\n"); 
        if (bench3D == false) setTableCell(curRow++, z, "Number of Ground Truth (GT) objects in this slice: " + I2_max + "\n");
        //IJ.write("Number of Machine Segmentation (MS) objects in this slice: " + I1_max + "\n"); 
        if (bench3D == false) setTableCell(curRow++, z, "Number of Machine Segmentation (MS) objects in this slice: " + I1_max + "\n");
        
        // Many to One (merged, undersegmentation)
        if (merge > 0) {
            //IJ.write("Number of merged MS objects: " + merge + "\n"); 
        	if (bench3D == false) setTableCell(curRow++, z, "Number of merged MS objects: " + merge + "\n");
            
            //IJ.write("Labels of merged MS and GT objects: " + "\n");
            for (n = 0; n < I1_max; n++) {
                int m_obj = 0;
                for (m = 0; m < I2_max; m++) {
                  int mrg_gt_label = MRG[n][m];
                  if (mrg_gt_label > 0) {               // (MS object by MS object). 
                    m_obj = m_obj + 1;      
                } else continue;                
                if (m_obj == 2) {       
                    //merge = merge + 1;      // increase the counter of merged objects, and
                    for (int mrg_gt_obj = 0; mrg_gt_obj < I2_max; mrg_gt_obj++){    // rescan that MS row
                        int mrg_gt_labels_all = MRG[n][mrg_gt_obj];
                        String label2 = mrg_gt_labels_all + "";
                        if (bitDepth2==32) label2 = String.valueOf(Float.intBitsToFloat(mrg_gt_labels_all));
                        if (mrg_gt_labels_all > 0) { 
                            int mrg_ms_label = grays1[n];
                            String label1 = mrg_ms_label + "";
                            if (bitDepth1==32) label1 = String.valueOf(Float.intBitsToFloat(mrg_ms_label));
                            //IJ.write("Merged MS obj label: " + mrg_ms_label + " Overlapped with GT obj label: " + mrg_gt_labels_all + "\n");
                            if (bench3D == false) setTableCell(curRow++, z, "Merged MS obj label: " + label1 + " Overlapped with GT obj label: " + label2 + "\n");
                        }
                    }
                    continue;  // jumps to the next MS row
                }
                }
            }           
 
           

//            // Save MRG[n] table (2D matrix): merged MS objects' id list. 
//            SaveDialog sd_mrg = new SaveDialog("Save MRG table ...", "MRG_table_",".csv");
//            String directory_mrg =  sd_mrg.getDirectory();
//            String fileName_mrg = sd_mrg.getFileName();
//            if (fileName_mrg==null) return;
//            IJ.showStatus("Saving: " + directory_mrg + fileName_mrg);
//            this.ip = ip;
//            MRGwrite(directory_mrg, fileName_mrg);
            
        } else {
            //IJ.write("There is no merged object.");
        	if (bench3D == false) setTableCell(curRow++, z, "There is no merged object.");
        }

        // One to Many (split, oversegmentation)
        if (split > 0) { 
            //IJ.write("Number of split GT objects: " + split + "\n");
        	if (bench3D == false) setTableCell(curRow++, z, "Number of split GT objects: " + split + "\n");
            
            //IJ.write("Labels of split GT objects: " + "\n");
            for (n = 0; n < I2_max; n++) {
            int s_obj = 0;
                for (m = 0; m < I1_max; m++) {
                  int spl_gt_label = SPL[n][m];
                  if (spl_gt_label > 0) {               // (MS object by MS object). 
                    s_obj = s_obj + 1;      
                } else continue;                
            if (s_obj == 2) {       
                for (int spl_gt_obj = 0; spl_gt_obj < I1_max; spl_gt_obj++){    // rescan that MS row
                    int spl_gt_labels_all = SPL[n][spl_gt_obj];
                    String label2 = spl_gt_labels_all + "";
                    if (bitDepth2==32) label2 = String.valueOf(Float.intBitsToFloat(spl_gt_labels_all));
                    if (spl_gt_labels_all > 0) { 
                        int spl_ms_label = grays1[spl_gt_obj];
                        String label1 = spl_ms_label + "";
                        if (bitDepth1==32) label1 = String.valueOf(Float.intBitsToFloat(spl_ms_label));
                        //IJ.write("Split MS obj label: " + spl_ms_label + " Overlapped with GT obj label: " + spl_gt_labels_all + "\n");
                        if (bench3D == false) setTableCell(curRow++, z, "Split MS obj label: " + label1 + " Overlapped with GT obj label: " + label2 + "\n");
                    }
                }
                continue;  // jumps to the next MS row
            }
            }
            }
            
            
//            for (n=0; n<SPL.length;n++) {
//                int spl_label=grays2[n];
//                if (SPL[n]==1) IJ.write(spl_label + "\n");            
//            }            

//            // Save SPL[m] table (2D matrix): split GT objects' id list. 
//            SaveDialog sd_spl = new SaveDialog("Save SPL table ...", "SPL_table_",".csv");
//            String directory_spl =  sd_spl.getDirectory();
//            String fileName_spl = sd_spl.getFileName();
//            if (fileName_spl==null) return;
//            IJ.showStatus("Saving: " + directory_spl + fileName_spl);
//            this.ip = ip;
//            SPLwrite(directory_spl, fileName_spl);
            
        }else{
            //IJ.write("There is no split object." + "\n" );
        	if (bench3D == false) setTableCell(curRow++, z, "There is no split object." + "\n");
        }
        
        // Total number and labels of FP MS objects
        if (t_FP_MS_obj > 0){
            //IJ.write("Number of False Positive MS objects: " + t_FP_MS_obj + "\n");
        	if (bench3D == false) setTableCell(curRow++, z, "Number of False Positive MS objects: " + t_FP_MS_obj + "\n");
            //IJ.write("Labels of False Positive MS objects: " + "\n");
        	if (bench3D == false) setTableCell(curRow++, z, "Labels of False Positive MS objects: " + "\n");
            for (n = 0; n < FP_MS_obj.length; n++) {
                int fp_ms_label=grays1[n];
                if (FP_MS_obj[n]==1) {
                    String str = fp_ms_label + "";
                    if (bitDepth1==32) str = String.valueOf(Float.intBitsToFloat(fp_ms_label));
                	//IJ.write(fp_ms_label + "\n");            
                    if (bench3D == false) setTableCell(curRow++, z, str + "\n");
                }
            } 
        }else{
            //IJ.write("There is no False Positive MS object." + "\n" );
        	if (bench3D == false) setTableCell(curRow++, z, "There is no False Positive MS object." + "\n");
        }
        
        // Total number and labels of FN GT objects
        if (t_FN_GT_obj > 0){
            //IJ.write("Number of False Negative MS objects: " + t_FN_GT_obj + "\n");
        	if (bench3D == false) setTableCell(curRow++, z, "Number of False Negative MS objects: " + t_FN_GT_obj + "\n");
            //IJ.write("GT Labels of False Negative MS objects: " + "\n");
        	if (bench3D == false) setTableCell(curRow++, z, "GT Labels of False Negative MS objects: " + "\n");
            for (n = 0; n < FN_GT_obj.length; n++) {
                int fn_gt_label=grays2[n];
                if (FN_GT_obj[n]==1) {
                    String str = fn_gt_label + "";
                    if (bitDepth2==32) str = String.valueOf(Float.intBitsToFloat(fn_gt_label));
                	//IJ.write(fn_gt_label + "\n");            
                    if (bench3D == false) setTableCell(curRow++, z, str + "\n");
                }
            } 
        }else{
            //IJ.write("There is no False Negative MS object." + "\n" );
        	if (bench3D == false) setTableCell(curRow++, z, "There is no False Negative MS object." + "\n");
        }
        
//        IJ.write("False Positive (FP) pixels' total number: " + t_fp + "\n");   // "total area oversegmentation"
//        IJ.write("False Positive Rate (FPR): " + t_fpr + "\n");                 // "total area oversegmentation"
//        IJ.write("False Negative (FN) pixels' total number: " + t_fn + "\n");   // "total area undersegmentation"
//        IJ.write("False Negative Rate (FNR): " + t_fnr + "\n");                 // "total area undersegmentation"
//        IJ.write("True Positive Rate (TPR): " + t_tpr + "\n");
//        IJ.write("Recall (= TPR): " + r + "\n");
//        IJ.write("Precision: " + p + "\n");
//        IJ.write("F-measure: " + f + "\n" + " " + "\n");
//        IJ.write("absolute spatial accuracy: " + ek + "\n");
//        IJ.write("relative spatial accuracy: "+ eek + "\n");
//        IJ.write("*** objective spatial accuracy (~ F-measure): "+ vk + "\n");
     
			}
	    };
        
    } // CLOSE of (int z=1; z<=NbSlices; z++) [stack cycle]
  	startAndJoin(threads);
      long time2 = System.currentTimeMillis();
      System.out.println("time2 - time1: " + (time2 - time1));
      
      if (bench3D == false) {
    	  frame.getTable().setAutoCreateColumnsFromModel(true);
          for (int c = 0; c < NbSlices; c++) frame.getTable().getColumnModel().getColumn(c).setPreferredWidth(400);
      }
          
        // 3D measures
        float tt_tpr = tt_tp/(tt_tp+tt_fn); // 3D, stack (total total) true positive rate (=recall=sensitivity)
        float tt_fpr = tt_fp/(tt_fp+ttn);   // 3D, stack (total total) false positive rate
        float tt_fnr = tt_fn/(tt_tp+tt_fn); // 3D, stack (total total) false negative rate
     
        float ttp = tt_tp/(tt_tp+tt_fp);    // 3D, stack (total total) p
        float ttr = tt_tp/(tt_tp+tt_fn);    // 3D, stack (total total) r    
        float tf = 2*ttp*ttr/(ttp+ttr);     // 3D, stack (total total) F-measure
        //frame.getTprLabel().setText("Total True Positive Rate (TPR, recall, sensitivity) of the stack: " + tt_tpr + " " + "\n");
        //IJ.write("*** Total False Positive Rate (FPR) of the stack: " + tt_fpr + " ***" + "\n");
        //frame.getFprLabel().setText("Total False Positive Rate (FPR) of the stack: " + tt_fpr + " " + "\n");
        if (bench3D == false) frame.getFprLabel().setText("Precision: p = " + ttp + " " + "\n");
        //IJ.write("*** Total False Negative Rate (FNR) of the stack: " + tt_fnr + " ***" + "\n");
        //frame.getFnrLabel().setText("Total False Negative Rate (FNR) of the stack: " + tt_fnr + " " + "\n"); 
        if (bench3D == false) frame.getFnrLabel().setText("Sensitivity: r = " + ttr + " " + "\n");
        //IJ.write("*** Total F-measure of the stack: " + tf  + " ***" + "\n");
        if (bench3D == false) frame.getFmLabel().setText("F-measure: F = " + tf + "\n");
        
    
        // *** OBJECT LEVEL ***
        // In all cases: 1 to 1 mapping AND One to Many AND Many to One
        // Save TP table (2D matrix) into .csv file
//        SaveDialog sd = new SaveDialog("Save TP table ...", "TP_table_",".csv");
//        String directory =  sd.getDirectory();
//        String fileName = sd.getFileName();
//        if (fileName==null) return;
//        IJ.showStatus("Saving: " + directory + fileName);
//        this.ip = ip;
//        TPwrite(directory, fileName);

      /* 
        // Save TPR table (2D matrix) into .csv file
        SaveDialog sd_tpr = new SaveDialog("Save TPR table ...", "TPR_table_",".csv");
        String directory_tpr =  sd_tpr.getDirectory();
        String fileName_tpr = sd_tpr.getFileName();
        if (fileName_tpr==null) return;
        IJ.showStatus("Saving: " + directory_tpr + fileName_tpr);
        this.ip = ip;
        TPRwrite(directory_tpr, fileName_tpr);
        
        
        // In case 1 to 1 mapping ONLY
        if (split == 0 && merge == 0) {
        // Save FN table (1D matrix) into .csv file
        SaveDialog sd_fn = new SaveDialog("Save FN table ...", "FN_table_",".csv");
        String directory_fn =  sd_fn.getDirectory();
        String fileName_fn = sd_fn.getFileName();
        if (fileName_fn==null) return;
        IJ.showStatus("Saving: " + directory_fn + fileName_fn);
        this.ip = ip;
        FNwrite(directory_fn, fileName_fn); 

        // Save FP table (1D matrix) into .csv file
        SaveDialog sd_fp = new SaveDialog("Save FP table ...", "FP_table_",".csv");
        String directory_fp =  sd_fp.getDirectory();
        String fileName_fp = sd_fp.getFileName();
        if (fileName_fp==null) return;
        IJ.showStatus("Saving: " + directory_fp + fileName_fp);
        this.ip = ip;
        FPwrite(directory_fp, fileName_fp);
        }
        
//        // Save FNR table (2D matrix) into .csv file
//        SaveDialog sd_fnr = new SaveDialog("Save FNR table ...", "FNR_table_",".csv");
//        String directory_fnr =  sd_fnr.getDirectory();
//        String fileName_fnr = sd_fnr.getFileName();
//        if (fileName_fnr==null) return;
//        IJ.showStatus("Saving: " + directory_fnr + fileName_fnr);
//        this.ip = ip;
//        FNRwrite(directory_fnr, fileName_fnr);

//            // Save FPR table (1D matrix) into .csv file
//            SaveDialog sd_fpr = new SaveDialog("Save FPR table ...", "FPR_table_",".csv");
//            String directory_fpr =  sd_fpr.getDirectory();
//            String fileName_fpr = sd_fpr.getFileName();
//            if (fileName_fpr==null) return;
//            IJ.showStatus("Saving: " + directory_fpr + fileName_fpr);
//            this.ip = ip;
//            FPRwrite(directory_fpr, fileName_fpr);
*/
        StackCombiner sc = new StackCombiner();        
        sc.run(title1);
//        int[] wListNew = WindowManager.getIDList();
//        ImagePlus img3 = WindowManager.getImage(wListNew[index1]);
        
    }
    
    //** Calculate and print out the total GT and MS object numbers */
    public void StackObjNumberCalc(ImagePlus img1, ImagePlus img2) {
        
        //Calculate the total img1 object numbers, and list the object labels into stkgrays1[] 
        StackStatistics stk1 = new StackStatistics(img1);
        int[] stkhist1 = null;
        size1 = stk1.histogram.length;
        stkhist1 = new int[size1];
        stkhist1 = stk1.histogram;         
        i=0;
        stkgrays1 = new int [stk1.histogram.length]; // nBins1
        for (int gr=1; gr<stk1.histogram.length;gr++){ // gr=0 is background, that is skipped
            if (stkhist1[gr]>0){
                setstacklabel1(i,gr);
                stk1_max=stk1_max+1;
                i=i+1;
            }            
        }   
        
        //Calculate the total img2 object numbers, and list the object labels into stkgrays2[]
        StackStatistics stk2 = new StackStatistics(img2);
        int[] stkhist2 = null;
        size2 = stk2.histogram.length;
        stkhist2 = new int[size2];
        stkhist2 = stk2.histogram;         
        i=0;
        stkgrays2 = new int [stk2.histogram.length]; // nBins1
        for (int gr=1; gr<stk2.histogram.length;gr++){ // gr=0 is background, that is skipped
            if (stkhist2[gr]>0){
                setstacklabel2(i,gr);
                stk2_max=stk2_max+1;
                i=i+1;
            }            
        }  
        //IJ.write("Total number of Ground Truth (GT) objects in stack: " + stk2_max + "\n"); 
        if (bench3D == false) frame.getGtLabel().setText("Total number of Ground Truth (GT) objects in stack: " + stk2_max + "\n");
        //IJ.write("Total number of Machine Segmentation (MS) objects in stack: " + stk1_max + "\n" + "\n");        
        if (bench3D == false) frame.getMsLabel().setText("Total number of Machine Segmentation (MS) objects in stack: " + stk1_max + "\n" + "\n");
    }
    
    // Lists the total stack1 object labels gray intensities into stkgrays1[]
    public void setstacklabel1(int m, int val){ // val=gray intensity of label
        stkgrays1[m] = val;
    }
    
    // Lists the total stack2 object labels gray intensities into stkgrays2[]
    public void setstacklabel2(int m, int val){ // val=gray intensity of label
        stkgrays2[m] = val;
    }
    
    // /home/janoskv/imaging_folder_kozos-bol/2007-07-07_HistoneGFP_2_2hr_z1to70_t1to10.ics_eredeti_WeeChoo-tol/benchmark/peti_email_2Darray_matrix.txt alapjan
    // fill the 2D array with the pixel intensity
    public void setvalue1(int m, int n, int val){ // m=x n=y
        pict1[m][n] = val;
    }
    
    public void setvalue2(int m, int n, int val){ // m=x n=y
        pict2[m][n] = val;
    }
    
    // Lists the img1 (MS) object labels gray intensities into grays1[]
    public void setlabel1(int m, int val){ // val=gray intensity of label
        grays1[m] = val;
    }
    
    // Lists the img2 (GT) object labels gray intensities into grays2[]
    public void setlabel2(int n, int val){ // val=gray intensity of label
        grays2[n] = val;
    }
    
    // Lists the img2 (GT) object area values into gtarea[]
    public void setarea(int n, int val){ // val=gray intensity of label
        gtarea[val] = n;
    }

    public int getvalue1(int m, int n){
        return pict1[m][n];
    }
    
    public int getvalue2(int m, int n){
        return pict2[m][n];
    }
    
    // Fill the TPR array.
    public void tpr(int n, int m, float val){
        TPR[n][m] = val;
    }
    
    // Fill the TNR array.
    public void tnr(int n, float val){
        TNR[n] = val;
    }
    
    // Fill the FPR array.
    public void fpr(int n, float val){
        FPR[n] = val;
    }
    
    // Fill the FNR array.
    public void fnr(int n, int m, float val){
        FNR[n][m] = val;
    }
    
    /*
    // Fill the AOR (area_overlap_ratio) array.
    public void aor(int m, float val){
        AOR[m] = val;
    }
    */
    
    public void pt(int m, int n, int val){ // m=x n=y
        PT[m][n] = val;
    }
    
    
    protected void TPwrite(String dir, String filename)
    {
      try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(dir+filename));
        for (n=0; n<I1_max;n++) { 
          for (m=0; m<I2_max;m++) { 
            bw.write(new Integer(TP[n][m]).toString());
            if (m<I2_max-1) bw.write("\t");
          }
          if (n<I1_max-1) bw.write("\n");
        }      
        bw.close();
      } catch (Exception e) {
        IJ.error("write", e.getMessage());
        return;
      }
    }
     
    protected void TPRwrite(String dir, String filename)
    {
      try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(dir+filename));
        for (n=0; n<I1_max;n++) { 
          for (m=0; m<I2_max;m++) { 
            bw.write(new Float(TPR[n][m]).toString());
            if (m<I2_max-1) bw.write("\t");
          }
          if (n<I1_max-1) bw.write("\n");
        }      
        bw.close();
      } catch (Exception e) {
        IJ.error("write", e.getMessage());
        return;
      }
    }
    
    protected void FNwrite(String dir, String filename)
    {
      try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(dir+filename));
        for (n=0; n<I2_max;n++) {           
            bw.write(new Float(FN[n]).toString());          
            bw.write("\n");
        }      
        bw.close();
      } catch (Exception e) {
        IJ.error("write", e.getMessage());
        return;
      }
    }
    
//    protected void FNRwrite(String dir, String filename)
//    {
//      try {
//        BufferedWriter bw = new BufferedWriter(new FileWriter(dir+filename));
//        for (n=0; n<I1_max;n++) { 
//          for (m=0; m<I2_max;m++) {
//            bw.write(new Float(FNR[n][m]).toString());
//            if (m<I2_max-1) bw.write("\t");
//          }
//          if (n<I1_max-1) bw.write("\n");
//        }      
//        bw.close();
//      } catch (Exception e) {
//        IJ.error("write", e.getMessage());
//        return;
//      }
//    }
     
    protected void FPwrite(String dir, String filename)
    {
      try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(dir+filename));
        for (n=0; n<I1_max;n++) {           
            bw.write(new Float(FP[n]).toString());          
            bw.write("\n");
        }      
        bw.close();
      } catch (Exception e) {
        IJ.error("write", e.getMessage());
        return;
      }
    }
    
//    protected void FPRwrite(String dir, String filename)
//    {
//      try {
//        BufferedWriter bw = new BufferedWriter(new FileWriter(dir+filename));
//        for (n=0; n<I1_max;n++) {           
//            // kerekiteni 4 tizedesre!
//            bw.write(new Float(FPR[n]).toString());
////            bw.write(new Float(IJ.d2s(FPR[n])).toString()); 
//            bw.write("\n");
//        }      
//        bw.close();
//      } catch (Exception e) {
//        IJ.error("write", e.getMessage());
//        return;
//      }
//    }
    
//    protected void MRGwrite(String dir, String filename)
//    {
//      try {
//        BufferedWriter bw = new BufferedWriter(new FileWriter(dir+filename));
//        for (n=0; n<I1_max;n++) {           
//            bw.write(new Float(MRG[n]).toString());          
//            bw.write("\n");
//        }      
//        bw.close();
//      } catch (Exception e) {
//        IJ.error("write", e.getMessage());
//        return;
//      }
//    }
    
//    protected void SPLwrite(String dir, String filename)
//    {
//      try {
//        BufferedWriter bw = new BufferedWriter(new FileWriter(dir+filename));
//        for (m=0; m<I2_max;m++) {           
//            bw.write(new Float(SPL[m]).toString());          
//            bw.write("\n");
//        }      
//        bw.close();
//      } catch (Exception e) {
//        IJ.error("write", e.getMessage());
//        return;
//      }
//    }
     
	private void setTableCell(int row, int col, String text)
	{
        if (model.getRowCount() == row) {
        	if (col == 0) model.addRow(new String[]{text});
        	else {
        		model.addRow(new String[]{""});
            	((Vector<String>) model.getDataVector().elementAt(row)).set(col, text);
        	}
        }
        else ((Vector<String>) model.getDataVector().elementAt(row)).set(col, text);
	}	

	private void clearResult()
	{
        frame.getGtLabel().setText("");
        frame.getMsLabel().setText("");
        frame.getFprLabel().setText("");
        frame.getFnrLabel().setText("");
        frame.getFmLabel().setText("");
        frame.getTable().setModel(new DefaultTableModel());
        
        frame.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
	            if (e.getValueIsAdjusting()) return;
	            if (frame.getTable().getSelectionModel().getLeadSelectionIndex() < 0) return;
	            updateROI();
			}
        });
        
        frame.getTable().getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
	            if (e.getValueIsAdjusting()) return;
	            if (frame.getTable().getSelectionModel().getLeadSelectionIndex() < 0) return;
	            updateROI();
			}
        });
	}

	private void updateROI()
	{
		if (frame.getTable().getSelectionModel().getLeadSelectionIndex() == curRow &&
				frame.getTable().getColumnModel().getSelectionModel().getLeadSelectionIndex() == curCol) {
			return;
		}
		curRow = frame.getTable().getSelectionModel().getLeadSelectionIndex();
		curCol = frame.getTable().getColumnModel().getSelectionModel().getLeadSelectionIndex();
//        IJ.write("row: " + curRow);
//        IJ.write("col: " + curCol);
//        IJ.write("value: " + frame.getTable().getModel().getValueAt(curRow, curCol));                
        int slice = curCol+1;
        int[] wList = WindowManager.getIDList();
        ImagePlus imp = WindowManager.getImage(wList[0]);
        imp.setSlice(slice); //Based on ij.plugin.Animator.java line 272, for Hyperstack see line 270 there

        
	//sa.updateROI();
	}

	private int[] getHistogram(ImagePlus img, int sliceNum, int bitDepth)
	{
		Object obj = img.getImageStack().getPixels(sliceNum);
		float[] pixels = bitDepth == 32 ? (float[]) obj : null;
		short[] pixels_short = bitDepth == 16 ? (short[]) obj : null;
		byte[] pixels_byte = bitDepth == 8 ? (byte[]) obj : null;
		Rectangle rect = imp.getProcessor().getRoi();
		int roiX = (int) rect.getX();
		int roiY = (int) rect.getY();
		int roiWidth = (int) rect.getWidth();
		int roiHeight = (int) rect.getHeight();

		Set hist32 = new TreeSet();
		for (int y=roiY; y<(roiY+roiHeight); y++) {
			int i = y*roiWidth + roiX;
			for (int x=roiX; x<(roiX+roiWidth); x++) {
				int v = 0;
				if (bitDepth == 32) v = Float.floatToIntBits(pixels[i++]);
				if (bitDepth == 16) v = pixels_short[i++];
				if (bitDepth == 8) {
					v = pixels_byte[i++];
					if (v < 0) v += 256;
				}
				if (v > 0) hist32.add(v);
			}
		}

        Object[] hist32A = hist32.toArray();
        int[] levels = new int[hist32A.length];
        for (int i=0; i<hist32A.length; i++)
        	levels[i] = (Integer) hist32A[i];
        
		return levels;
	}
	
	private SortedMap getHistogramForLabels(ImagePlus img)
	{
		int sliceNum = img.getImageStackSize();
		int bitDepth = img.getBitDepth();
		SortedMap hist = new TreeMap();
		
		for (int n = 1; n < sliceNum+1; n++) {
			Object obj = img.getImageStack().getPixels(n);
			float[] pixels = bitDepth == 32 ? (float[]) obj : null;
			short[] pixels_short = bitDepth == 16 ? (short[]) obj : null;
			byte[] pixels_byte = bitDepth == 8 ? (byte[]) obj : null;
			Rectangle rect = img.getProcessor().getRoi();
			int roiX = (int) rect.getX();
			int roiY = (int) rect.getY();
			int roiWidth = (int) rect.getWidth();
			int roiHeight = (int) rect.getHeight();
	
			for (int y=roiY; y<(roiY+roiHeight); y++) {
				int i = y*roiWidth + roiX;
				for (int x=roiX; x<(roiX+roiWidth); x++) {
					int v = 0;
					if (bitDepth == 32) v = (int) pixels[i++];
					if (bitDepth == 16) v = pixels_short[i++];
					if (bitDepth == 8) {
						v = pixels_byte[i++];
						if (v < 0) v += 256;
					}
					if (v > 0) {
						Hist count = (Hist) hist.get(v);
						if (count == null) hist.put(v, new Hist(1));
						else count.setCount(count.getCount() + 1);
					}
				}
			}
		}
		
    	System.out.println("hist.size(): " + hist.size());
        Iterator iter = hist.keySet().iterator();
        while(iter.hasNext()) {
        	Integer key = (Integer) iter.next();
        	Hist count = (Hist) hist.get(key);
	    	System.out.println("key="+key+",count="+count.getCount());
        }

    	return hist;
	}
	
    //	Implementing Clear_outside.txt macro /media/disk2/macro/Clear_outside_orig_bin_v4.1.txt
    public void ClearOutside(ImagePlus bin, ImagePlus orig) {
    	
    	// Clear binary edges to avoid the detection of large (full slice-sized) selection as no selection.
    	IJ.setForegroundColor(255, 255, 255); 
    	IJ.run(bin,	"Select All", "");
    	IJ.run(bin,	"Draw", "stack");
    	
    	IJ.setBackgroundColor(0, 0, 0);
    	IJ.setForegroundColor(0, 0, 0);   	
    	IJ.run(bin,	"Select None", "");
    	for (int p = 1; p <= bin.getStackSize(); p++) {
        //	 Binary stack's foreground must be black, background must be white.
    		bin.setSlice(p);
    		bin.killRoi(); // IJ.run(bin,	"Select None", "");
    		IJ.run(bin,	"Create Selection", "");
    		Roi roi = bin.getRoi(); // int roiX = roi.getBounds().x; int roiY = roi.getBounds().y; int roiW = roi.getBounds().width; int roiH = roi.getBounds().height;

    		if (roi == null){	// Skip empty slices with no selection.
    			orig.setSlice(p);
    	    	IJ.run(orig, "Select All", "");
    	    	IJ.run(orig, "Fill", "slice");
    	    	IJ.run(orig, "Select None", "");    			
    		} else {
        		bin.setSlice(p);
        		IJ.run(bin,	"Select None", "");
        		IJ.run(bin,	"Create Selection", "");
    			orig.setSlice(p);
    			//orig.show();
    			orig.restoreRoi(); // IJ.run(orig, "Restore Selection", "");    			
    			IJ.run(orig, "Clear Outside", "slice");
    			IJ.run(orig, "Select None", "");
    		}   		

    	}
    }
	
    public static void startAndJoin(Thread[] threads)
    {
        for (int ithread = 0; ithread < threads.length; ++ithread)
        {
            threads[ithread].setPriority(Thread.NORM_PRIORITY);
            threads[ithread].start();
        }

        try
        {   
            for (int ithread = 0; ithread < threads.length; ++ithread)
                threads[ithread].join();
        } catch (InterruptedException ie)
        {
            throw new RuntimeException(ie);
        }
    }
}