import java.awt.Rectangle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;

import javax.swing.table.DefaultTableModel;

public class SliceThread extends Thread {

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
    static float [] stk_t_fpr; // total fpr of all benchmarks in the stack
    static float [] stk_t_fnr; // total fnr of all benchmarks in the stack
    static float [] stk_f; // F-measure values of all benchmarks in the stack    
    static float tt_fp = 0;    // Total number of FP pixels per 3D stack (total total)
    static float tt_tp = 0;    // Total number of TP pixels per 3D stack (total total)
    static float tt_fn = 0;    // Total number of FN pixels per 3D stack (total total)
    static float ttn = 0;      // Total number of TN pixels per 3D stack (total total)
    int [] GTlabels; // GT labels' gray intesity values
	int [] MSlabels; // MS labels' gray intesity values
	
	int z;
	static int NbSlices;
	static ImagePlus img1;
	static ImagePlus img2;
	static ImagePlus imp;
	static boolean bench3D;
	static Gebiss_.BenchFrame frame;
	static DefaultTableModel model;

	static void reset(int NbSlices_, ImagePlus img1_, ImagePlus img2_, boolean bench3D_, Gebiss_.BenchFrame frame_, ImagePlus imp_, float[] stk_t_fpr_, float[] stk_t_fnr_, float[] stk_f_) {
		tt_fp = 0;
		tt_tp = 0;
		tt_fn = 0;
		ttn = 0;
		NbSlices = NbSlices_;
		img1 = img1_;
		img2 = img2_;
		imp = imp_;
		bench3D = bench3D_;
		frame = frame_;
		stk_t_fpr = stk_t_fpr_;
		stk_t_fnr = stk_t_fnr_;
		stk_f = stk_f_;
	}
	
	SliceThread(int z) {
		this.z = z;
	}
	
	static float getTT_TP() {
		return tt_tp;
	}
	
	static float getTT_FP() {
		return tt_fp;
	}
	
	static float getTT_FN() {
		return tt_fn;
	}
	
	static float getTTN() {
		return ttn;
	}
	
	public void run() {
		int bitDepth1 = img1.getBitDepth();
		int bitDepth2 = img2.getBitDepth();
		synchronized (SliceThread.class) {
	    int z1=z+1;
		img1.setSlice(z1);
		img2.setSlice(z1);
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
		        
		//        ij.process.FloatStatistics.getStatistics(ip, a, arg2)
		        
		//         Determine the max number of labels (gray values) based on the bitdepth of the image. 
		//         GRAY16=short image, GRAY8=byte image (GRAY32=float)
		//        if (img1.getType()==ImagePlus.GRAY32){
		//            nBinsFl1 = 2^32; //65536 //2147483647
		//        }
		                
		    	grays1 = getHistogram(img1, z1, bitDepth1);
		    	if (z == 60) for (int a1 = 0; a1 < grays1.length; a1++) System.out.println("grays1: " + grays1[a1]);
		    	I1_max = size1 = grays1.length;
		
		//       // 32 BITES KEP OBJEKTUM SZAMANAK MEGHATAROZASA (HISZTOGRAM)
		//       // minta: ij.process.ShortProcessor.java 858-888. sora, ij.process.StackStatistics.java 68-90.       
		//        else if (bitDepth1==32){                 
		//                    int[] hist3 = new int[2^31-1];
		//                    for (y=1; y<Height;y++) {
		//                        for (x=1; x<Width;x++) { 
		//                    for (int x=roiY; y<(roiY+roiHeight); y++) {
		//                            int i = y*width + roiX;
		//                            for (int x=roiX; x<(roiX+roiWidth); x++)
		//                                    	histogram[pixels[i++]&0xffff]++;
		//}
		        ////////////////////////////////////
		
		//                    // ITT MEGOLDANI 32 BITES HISZTOGRAM BEOLVASASAT!!!
		//                    ImageStatistics stats1 = img1.getStatistics( Analyzer.getMeasurements() );
		//                    hist1 = (int[]) stats1.histogram;
		
		                    /*FloatProcessor flp = (FloatProcessor)ip.convertToFloat();
		                    flp = new FloatProcessor(Width,Height);
		                    hist1 = (int[]) flp.getHistogram(); */
		                    
		                    /*FloatStatistics stats1 = (FloatStatistics) img1.getStatistics(Analyzer.getMeasurements());                            
		                    hist1 = (int[]) stats1.histogram;
		                     */                           
		//                    }
		
		        
		        
				grays2 = getHistogram(img2, z1, bitDepth2);
		    	if (z == 60) for (int a1 = 0; a1 < grays2.length; a1++) System.out.println("grays2: " + grays2[a1]);
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
		        
		//        else if (bitDepth1==32){                               
		//                  // ITT MEGOLDANI 32 BITES HISZTOGRAM BEOLVASASAT!!!
		//                    ImageStatistics stats2 = img2.getStatistics( Analyzer.getMeasurements() );
		//                    hist2 = (int[]) stats2.histogram;
		//        }
		
		        // int nBins = ip.getHistogramSize(); // Default 256 at 8bit image 
		        // int[] hist1 = ip.getHistogram(); // Returns histogram only for the current img.
		
		//        int[] hist1 = img1.getProcessor().getHistogram(); // Does not work for 32 bit image.
		//        int[] hist2 = img2.getProcessor().getHistogram();           
		        
		//        FloatStatistics fltimg1 = new FloatStatistics(img1);
		
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
		    	if (z == 60) System.out.println("img1.getSlice() before pict1: " + img1.getSlice());
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
		} // end of synchronization
		
		//int [][] pict3;
		//pict3 = new int [Width][Height]; 
		//int [][] pict4;
		//pict4 = new int [Width][Height];
		//for (x=1; x<Width;x++) { 
		//    for (y=1; y<Height;y++) {            
		//pict3 = img1.getProcessor().getIntArray();
		//pict4 = img2.getProcessor().getIntArray();
		//    }
		//}
		
		// Compare the 2 arrays "pixel by pixel" and 
		// fill TP, FP, TN, FN arrays.
		s_I1 = new int [I1_max];       // number of labels (objects) at img1
		s_I2 = new int [I2_max];       // number of labels (objects) at img2
		FP = new int [I1_max];         // number of false positive pixels
		TP = new int [I1_max][I2_max]; // number of true positive pixels
		FN = new int [I2_max];         // number of false negative pixels
		

		SortedSet<Integer> pxvals1 = new TreeSet<Integer>();
		SortedSet<Integer> pxvals2 = new TreeSet<Integer>();
		// *** PIXEL LEVEL ***
		// 1 to 1 mapping
		for (b=1; b<Height;b++) {          // y coord of pixels
		 long t1 = System.currentTimeMillis();
		    for (a=1; a<Width;a++) {    // x coord of pixels
		        pxval1 = (pict1[a][b]);
		        pxval2 = (pict2[a][b]);
		        pxvals1.add(pxval1);
		        pxvals2.add(pxval2);
		
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
		    		        //if (z == 60) System.out.println("z: " + z + " m: " + m + " n: " + n + " a: " + a + " b: " + b + " pxval1: " + pxval1 + " pxval2: " + pxval2);
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
        if (z == 60) System.out.println("pxvals1: " + pxvals1);
        if (z == 60) System.out.println("pxvals2: " + pxvals2);
		
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
		        if (z == 60 && tp_val > 0) System.out.println("z: " + z + " m: " + m + " n: " + n + " tp_val: " + tp_val);
		        t_tp = t_tp + tp_val;           // Total number of TP pixels per image (= total foreground overlap pixels per image)
		        float fn_val = FN[m];
		//            t_fn = t_fn + fn;           // Total number of FN pixels per image
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
		if (z == 60) System.out.println(z + " t_tp: " + t_tp);
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
		//if (bench3D == false) model.addColumn("Slice number: " + z1 + " / " + NbSlices + "\n");
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
		
		   
		
		//    // Save MRG[n] table (2D matrix): merged MS objects' id list. 
		//    SaveDialog sd_mrg = new SaveDialog("Save MRG table ...", "MRG_table_",".csv");
		//    String directory_mrg =  sd_mrg.getDirectory();
		//    String fileName_mrg = sd_mrg.getFileName();
		//    if (fileName_mrg==null) return;
		//    IJ.showStatus("Saving: " + directory_mrg + fileName_mrg);
		//    this.ip = ip;
		//    MRGwrite(directory_mrg, fileName_mrg);
		    
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
		    
		    
		//    for (n=0; n<SPL.length;n++) {
		//        int spl_label=grays2[n];
		//        if (SPL[n]==1) IJ.write(spl_label + "\n");            
		//    }            
		
		//    // Save SPL[m] table (2D matrix): split GT objects' id list. 
		//    SaveDialog sd_spl = new SaveDialog("Save SPL table ...", "SPL_table_",".csv");
		//    String directory_spl =  sd_spl.getDirectory();
		//    String fileName_spl = sd_spl.getFileName();
		//    if (fileName_spl==null) return;
		//    IJ.showStatus("Saving: " + directory_spl + fileName_spl);
		//    this.ip = ip;
		//    SPLwrite(directory_spl, fileName_spl);
		    
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
		
		//IJ.write("False Positive (FP) pixels' total number: " + t_fp + "\n");   // "total area oversegmentation"
		//IJ.write("False Positive Rate (FPR): " + t_fpr + "\n");                 // "total area oversegmentation"
		//IJ.write("False Negative (FN) pixels' total number: " + t_fn + "\n");   // "total area undersegmentation"
		//IJ.write("False Negative Rate (FNR): " + t_fnr + "\n");                 // "total area undersegmentation"
		//IJ.write("True Positive Rate (TPR): " + t_tpr + "\n");
		//IJ.write("Recall (= TPR): " + r + "\n");
		//IJ.write("Precision: " + p + "\n");
		//IJ.write("F-measure: " + f + "\n" + " " + "\n");
		//IJ.write("absolute spatial accuracy: " + ek + "\n");
		//IJ.write("relative spatial accuracy: "+ eek + "\n");
		//IJ.write("*** objective spatial accuracy (~ F-measure): "+ vk + "\n");
	}
	
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
    
    public void setvalue1(int m, int n, int val){ // m=x n=y
        pict1[m][n] = val;
    }
    
    public void setvalue2(int m, int n, int val){ // m=x n=y
        pict2[m][n] = val;
    }
    
    public void pt(int m, int n, int val){ // m=x n=y
        PT[m][n] = val;
    }
    
}
