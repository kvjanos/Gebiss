

import ij.*;
import ij.ImagePlus.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.util.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;

public class ObjectCounter3D implements PlugIn, AdjustmentListener, TextListener, Runnable {
    Vector sliders;
    Vector value;       

    public static int minSize_default = 10;
    public static int maxSize_default = Integer.MAX_VALUE;
    public static boolean showParticles_default = true;
    public static boolean showEdges_default = false;
    public static boolean showCentres_default = false;
    public static boolean showCentresInt_default = false;
    public static int DotSize_default = 3;
    public static boolean showNumbers_default = false;
    public static boolean new_results_default = true;
    public static int FontSize_default = 12;
    public static boolean summary_default = true;    
    
    int ThrVal;
    int minSize;
    int maxSize;
    boolean showParticles;
    boolean showEdges;
    boolean showCentres;
    boolean showCentresInt;
    int DotSize;
    boolean showNumbers;
    boolean new_results;
    int FontSize;
    boolean summary;

    int Width;
    int Height;
    int NbSlices;
    int arrayLength;
    String imgtitle;
    int PixVal;

    boolean[] thr; // true if above threshold
    int[] pict; // original pixel values
    int[] tag;
    boolean[] surf;
    int ID;
    int[] IDarray;
    double [][] Paramarray;

    ImagePlus img;
    ImageProcessor ip;
    ImageStack stackParticles;
    ImageStack stackEdges;
    ImageStack stackCentres;
    ImageStack stackCentresInt;
    ImagePlus Particles;
    ImagePlus Edges;
    ImagePlus Centres;
    ImagePlus CentresInt;
    ResultsTable rt;
    Thread thread;

    public void run(String arg) {
        if (! setupGUI(arg)) return;
        analyze();
    }

    public boolean setupGUI(String arg) {
        img = WindowManager.getCurrentImage();
        if (img==null){
            IJ.noImage();
            return false;
        } else if (img.getStackSize() == 1) {
            IJ.error("Stack required");
            return false;
        } else if (img.getType() != ImagePlus.GRAY8 && img.getType() != ImagePlus.GRAY16 ) {
            // In order to support 32bit images, pict[] must be changed to float[], and  getPixel(x, y); requires a Float.intBitsToFloat() conversion
            IJ.error("8 or 16 bit greyscale image required");
            return false;
        }
        Width=img.getWidth();
        Height=img.getHeight();
        NbSlices=img.getStackSize();
        arrayLength=Width*Height*NbSlices;
        imgtitle = img.getTitle();

        ip=img.getProcessor();
        ThrVal=ip.getAutoThreshold();
        ip.setThreshold(ThrVal,Math.pow(2,16),ImageProcessor.RED_LUT);
        img.setSlice((int)NbSlices/2);
        img.updateAndDraw();

        GenericDialog gd=new GenericDialog("3D objects counter");
        gd.addSlider("Threshold: ",ip.getMin(), ip.getMax(),ThrVal);
        gd.addSlider("Slice: ",1, NbSlices,(int) NbSlices/2);
        sliders=gd.getSliders();
        ((Scrollbar)sliders.elementAt(0)).addAdjustmentListener(this);
        ((Scrollbar)sliders.elementAt(1)).addAdjustmentListener(this);
        value = gd.getNumericFields();
        ((TextField)value.elementAt(0)).addTextListener(this);
        ((TextField)value.elementAt(1)).addTextListener(this);
        gd.addNumericField("Min number of voxels: ",minSize_default,0);
        gd.addNumericField("Max number of voxels: ",Math.min(maxSize_default, Height*Width*NbSlices),0);
        gd.addCheckbox("New_Results Table", new_results_default);
        gd.addMessage("Show:");
        gd.addCheckbox("Particles",showParticles_default);
        gd.addCheckbox("Edges",showNumbers_default);
        gd.addCheckbox("Geometrical centre", showCentres_default);
        gd.addCheckbox("Intensity based centre", showCentresInt_default);
        gd.addNumericField("Dot size",DotSize_default,0);
        gd.addCheckbox("Numbers",showNumbers_default);
        gd.addNumericField("Font size",FontSize_default,0);
        gd.addMessage("");
        gd.addCheckbox("Log summary", summary_default);
        gd.showDialog();

        if (gd.wasCanceled()){
            ip.resetThreshold();
            img.updateAndDraw();
            return false;
        }

        ThrVal=(int) gd.getNextNumber();
        gd.getNextNumber();
        minSize=(int) gd.getNextNumber();   minSize_default = minSize;
        maxSize=(int) gd.getNextNumber();   maxSize_default = maxSize;
        new_results=gd.getNextBoolean();    new_results_default = new_results;
        showParticles=gd.getNextBoolean();  showParticles_default = showParticles;
        showEdges=gd.getNextBoolean();      showEdges_default = showEdges;
        showCentres=gd.getNextBoolean();    showCentres_default = showCentres;
        showCentresInt=gd.getNextBoolean(); showCentresInt_default = showCentresInt;
        DotSize=(int)gd.getNextNumber();    DotSize_default = DotSize;
        showNumbers=gd.getNextBoolean();    showNumbers_default = showNumbers;
        FontSize=(int)gd.getNextNumber();   FontSize_default = FontSize;
        summary=gd.getNextBoolean();        summary_default = summary;

        IJ.register(ObjectCounter3D.class); // static fields preserved when plugin is restarted
        //Reset the threshold
        ip.resetThreshold();
        img.updateAndDraw();
        return true;
    }

    void analyze() {
        IJ.showStatus("3D Objects Counter");
        long start=System.currentTimeMillis();
        int x, y, z;
        int xn, yn, zn;
        int i, j, k, arrayIndex, offset;
        int voisX = -1, voisY = -1, voisZ = -1;
        int maxX = Width-1, maxY=Height-1;

        int index;
        int val;
        double col;

        int minTag;
        int minTagOld;

        pict=new int [Height*Width*NbSlices];
        thr=new boolean [Height*Width*NbSlices];
        tag=new int [Height*Width*NbSlices];
        surf=new boolean [Height*Width*NbSlices];
        Arrays.fill(thr,false);
        Arrays.fill(surf,false);

        //Load the image in a one dimension array
        ImageStack stack = img.getStack();
        arrayIndex=0;
        for (z=1; z<=NbSlices; z++) {
            ip = stack.getProcessor(z);
            for (y=0; y<Height;y++) {
                for (x=0; x<Width;x++) {
                    PixVal=ip.getPixel(x, y);
                    pict[arrayIndex]=PixVal;
                    if (PixVal>ThrVal){
                        thr[arrayIndex]=true;
                    }
                   arrayIndex++;
                }
            }
        }

        //First ID attribution
        int tagvois;
        ID=1;
        arrayIndex=0;
        for (z=1; z<=NbSlices; z++){
            for (y=0; y<Height; y++){
                for (x=0; x<Width; x++){
                    if (thr[arrayIndex]){
                        tag[arrayIndex]=ID;
                        minTag=ID;
                        i=0;
                        //Find the minimum tag in the neighbours pixels
                        for (voisZ=z-1;voisZ<=z+1;voisZ++){
                            for (voisY=y-1;voisY<=y+1;voisY++){
                                for (voisX=x-1;voisX<=x+1;voisX++){
                                    if (withinBounds(voisX, voisY, voisZ)) {
                                        offset=offset(voisX, voisY, voisZ);
                                        if (thr[offset]){
                                            i++;
                                            tagvois = tag[offset];
                                            if (tagvois!=0 && tagvois<minTag) minTag=tagvois;
                                        }
                                    } 
                                }
                            }
                        }
                        if (i!=27) surf[arrayIndex]=true;
                        tag[arrayIndex]=minTag;
                        if (minTag==ID){
                            ID++;
                        }
                    }
                    arrayIndex++;
                }
            }
            IJ.showStatus("Finding structures");
            IJ.showProgress(z,NbSlices);
        }
        ID++;

        //Minimization of IDs=connection of structures
        // SLOWEST PART !!!!
        arrayIndex=0;
        for (z=1; z<=NbSlices; z++){
            for (y=0; y<Height; y++){
                for (x=0; x<Width; x++){
                    if (thr[arrayIndex]){
                        minTag=tag[arrayIndex];
                        //Find the minimum tag in the neighbours pixels
                        for (voisZ=z-1;voisZ<=z+1;voisZ++){
                            for (voisY=y-1;voisY<=y+1;voisY++){
                                for (voisX=x-1;voisX<=x+1;voisX++){
                                    if (withinBounds(voisX, voisY, voisZ)) {
                                        offset=offset(voisX, voisY, voisZ);
                                        if (thr[offset]){
                                            tagvois = tag[offset];
                                            if (tagvois!=0 && tagvois<minTag) minTag=tagvois;
                                        }
                                    }
                                }
                            }
                        }
                        //Replacing tag by the minimum tag found
                        for (voisZ=z-1;voisZ<=z+1;voisZ++){
                            for (voisY=y-1;voisY<=y+1;voisY++){
                                for (voisX=x-1;voisX<=x+1;voisX++){
                                    if (withinBounds(voisX, voisY, voisZ)) {
                                        offset=offset(voisX, voisY, voisZ);
                                        if (thr[offset]){
                                            tagvois = tag[offset];
                                            if (tagvois!=0 && tagvois!=minTag) replacetag(tagvois,minTag);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    arrayIndex++;
                }
            }
            IJ.showStatus("Connecting structures");
            IJ.showProgress(z,NbSlices);
        }

        //Parameters determination 0:volume; 1:surface; 2:intensity; 3:barycenter x; 4:barycenter y; 5:barycenter z; 6:barycenter x int; 7:barycenter y int; 8:barycenter z int
        arrayIndex=0;
        Paramarray=new double [ID][9];
        for (z=1; z<=NbSlices; z++){
            for (y=0; y<Height; y++){
                for (x=0; x<Width; x++){
                    index=tag[arrayIndex];
                    val=pict[arrayIndex];
                    Paramarray[index][0]++;
                    if (surf[arrayIndex]) Paramarray[index][1]++;
                    Paramarray[index][2]+=val;
                    Paramarray[index][3]+=x;
                    Paramarray[index][4]+=y;
                    Paramarray[index][5]+=z;
                    Paramarray[index][6]+=x*val;
                    Paramarray[index][7]+=y*val;
                    Paramarray[index][8]+=z*val;
                    arrayIndex++;
                }
            }
            IJ.showStatus("Retrieving structures' parameters");
            IJ.showProgress(z,NbSlices);
        }
        double volume, intensity;
        for (i=0;i<ID;i++){
            volume = Paramarray[i][0];
            intensity = Paramarray[i][2];
            if (volume>=minSize && volume<=maxSize) {
                if (volume!=0){
                    Paramarray[i][2] /= volume;
                    Paramarray[i][3] /= volume;
                    Paramarray[i][4] /= volume;
                    Paramarray[i][5] /= volume;
                }
                if (intensity!=0){
                    Paramarray[i][6] /= intensity;
                    Paramarray[i][7] /= intensity;
                    Paramarray[i][8] /= intensity;
                }
            } else {
                for (j=0;j<9;j++) Paramarray[i][j]=0;
            }
            IJ.showStatus("Calculating barycenters' coordinates");
            IJ.showProgress(i,ID);
        }

        //Log data
        if (new_results) {
            rt=new ResultsTable();
        } else {
            rt = ResultsTable.getResultsTable();
        }
        IDarray=new int[ID];

        String[] head={"Volume","Surface","Intensity","Centre X","Centre Y","Centre Z","Centre int X","Centre int Y","Centre int Z"};
        for (i=0; i<head.length; i++) rt.setHeading(i,head[i]);

        k=1;
        for (i=1;i<ID;i++){
            if (Paramarray[i][0]!=0){
                rt.incrementCounter();
                IDarray[i]=k;
                for (j=0; j<9; j++) rt.addValue(j,Paramarray[i][j]);
                k++;
            }
        }
        if (new_results) {
            rt.show("Results from "+imgtitle);
        } else {
            //if (! IJ.isResultsWindow()) IJ.showResults();
            rt.show("Results");
        }

        if (showParticles){
            Particles=NewImage.createShortImage("Particles "+imgtitle,Width,Height,NbSlices,0);
            Particles.show();
            IJ.run("Grays");
            stackParticles=Particles.getStack();
            Particles.setCalibration(img.getCalibration());
        }

        if (showEdges){
            Edges=NewImage.createShortImage("Edges "+imgtitle,Width,Height,NbSlices,0);
            Edges.show();
            IJ.run("Grays");
            stackEdges=Edges.getStack();
            Edges.setCalibration(img.getCalibration());
        }

        if (showCentres){
            Centres=NewImage.createShortImage("Geometrical Centres "+imgtitle,Width,Height,NbSlices,0);
            Centres.show();
            IJ.run("Grays");
            stackCentres=Centres.getStack();
            Centres.setCalibration(img.getCalibration());
        }

        if (showCentresInt){
            CentresInt=NewImage.createShortImage("Intensity based centres "+imgtitle,Width,Height,NbSlices,0);
            CentresInt.show();
            IJ.run("Grays");
            stackCentresInt=CentresInt.getStack();
            CentresInt.setCalibration(img.getCalibration());
        }
        arrayIndex=0;
        for (z=1; z<=NbSlices; z++){
            for (y=0; y<Height; y++){
                for (x=0; x<Width; x++){
                    if (thr[arrayIndex]){
                        index=tag[arrayIndex];
                        if (Paramarray[index][0]>=minSize && Paramarray[index][0]<=maxSize){
                            if (showParticles){
                                ip=stackParticles.getProcessor(z);
                                col=IDarray[index];
                                ip.setValue(col);
                                ip.drawPixel(x, y);
                            }
                            if (showEdges && surf[arrayIndex]){
                                ip=stackEdges.getProcessor(z);
                                col=IDarray[index];
                                ip.setValue(col);
                                ip.drawPixel(x, y);
                            }
                            if (showCentres){
                                ip=stackCentres.getProcessor((int)Math.round(Paramarray[index][5]));
                                col=IDarray[index];
                                ip.setValue(col);
                                ip.setLineWidth(DotSize);
                                ip.drawDot((int)Math.round(Paramarray[index][3]), (int)Math.round(Paramarray[index][4]));
                            }
                            if (showCentresInt){
                                ip=stackCentresInt.getProcessor((int)Math.round(Paramarray[index][8]));
                                col=IDarray[index];
                                ip.setValue(col);
                                ip.setLineWidth(DotSize);
                                ip.drawDot((int)Math.round(Paramarray[index][6]), (int)Math.round(Paramarray[index][7]));
                            }
                        }
                    }
                    arrayIndex++;
                }
            }

            IJ.showStatus("Building images");
            IJ.showProgress(z,NbSlices);
        }
        if (showNumbers){
            Font font = new Font("SansSerif", Font.PLAIN, FontSize);
            for (i=0;i<rt.getCounter();i++){
                if (showParticles){
                    ip=stackParticles.getProcessor((int)rt.getValue(5,i));
                    ip.setFont(font);
                    ip.setValue(rt.getCounter());//Math.pow(2,16));
                    ip.drawString(""+(i+1),(int)rt.getValue(3,i),(int)rt.getValue(4,i));
                }
                if (showEdges){
                    ip=stackEdges.getProcessor((int)rt.getValue(5,i));
                    ip.setFont(font);
                    ip.setValue(rt.getCounter());//Math.pow(2,16));
                    ip.drawString(""+(i+1),(int)rt.getValue(3,i),(int)rt.getValue(4,i));
                }
                if (showCentres){
                    ip=stackCentres.getProcessor((int)rt.getValue(5,i));
                    ip.setFont(font);
                    ip.setValue(rt.getCounter());//Math.pow(2,16));
                    ip.drawString(""+(i+1),(int)rt.getValue(3,i),(int)rt.getValue(4,i));
                }
                if (showCentresInt){
                    ip=stackCentresInt.getProcessor((int)rt.getValue(5,i));
                    ip.setFont(font);
                    ip.setValue(rt.getCounter());//Math.pow(2,16));
                    ip.drawString(""+(i+1),(int)rt.getValue(3,i),(int)rt.getValue(4,i));
                }
                IJ.showStatus("Drawing numbers");
                IJ.showProgress(i,rt.getCounter());
            }
        }

        if (showParticles){
            Particles.getProcessor().setMinAndMax(1,rt.getCounter());
            Particles.updateAndDraw();
        }

        if (showEdges){
            Edges.getProcessor().setMinAndMax(1,rt.getCounter());
            Edges.updateAndDraw();
        }

        if (showCentres){
            Centres.getProcessor().setMinAndMax(1,rt.getCounter());
            Centres.updateAndDraw();
        }

        if (showCentresInt){
            CentresInt.getProcessor().setMinAndMax(1,rt.getCounter());
            CentresInt.updateAndDraw();
        }

        if (summary){
            double TtlVol=0;
            double TtlSurf=0;
            double TtlInt=0;
            for (i=0; i<rt.getCounter();i++){
                TtlVol+=rt.getValueAsDouble(0,i);
                TtlSurf+=rt.getValueAsDouble(1,i);
                TtlInt+=rt.getValueAsDouble(2,i);
            }
            ResultsTable summaryTable;
            IJ.log(imgtitle+", "+(int)TtlVol+", "+(int)TtlSurf+", "+(int)TtlInt);
        }
        IJ.showStatus("Nb of particles: "+rt.getCounter());
        IJ.showProgress(2,1);
        IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");
    }

    public boolean withinBounds(int m,int n,int o) {
        return (m >= 0 && m < Width && n >=0 && n < Height && o > 0 && o <= NbSlices );
    }

    public int offset(int m,int n,int o) {
        return m+n*Width+(o-1)*Width*Height;
    }

    public void replacetag(int m,int n){
        for (int i=0; i<tag.length; i++) if (tag[i]==m) tag[i]=n;
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        ThrVal=((Scrollbar)sliders.elementAt(0)).getValue();
        ip.setThreshold(ThrVal,Math.pow(2,16),ImageProcessor.RED_LUT);
        img.setSlice(((Scrollbar)sliders.elementAt(1)).getValue());
        img.updateAndDraw();
    }

    public void textValueChanged(TextEvent e) {
        ((Scrollbar)sliders.elementAt(0)).setValue((int) Tools.parseDouble(((TextField)value.elementAt(0)).getText()));
        ((Scrollbar)sliders.elementAt(1)).setValue((int) Tools.parseDouble(((TextField)value.elementAt(1)).getText()));
        if ((int) Tools.parseDouble(((TextField)value.elementAt(1)).getText())>NbSlices){
            ((Scrollbar)sliders.elementAt(1)).setValue(NbSlices);
            ((TextField)value.elementAt(1)).setText(""+NbSlices);
        }
        if ((int) Tools.parseDouble(((TextField)value.elementAt(1)).getText())<1){
            ((Scrollbar)sliders.elementAt(1)).setValue(1);
            ((TextField)value.elementAt(1)).setText("1");
        }
        ThrVal=((Scrollbar)sliders.elementAt(0)).getValue();
        ip.setThreshold(ThrVal,Math.pow(2,16),ImageProcessor.RED_LUT);
        img.setSlice(((Scrollbar)sliders.elementAt(1)).getValue());
        img.updateAndDraw();
    }

    public ImagePlus getParticles() {
        return Particles;
    }

    public ImagePlus getEdges() {
        return Edges;
    }

    public ResultsTable getResults() {
        return rt;
    }

    public Thread runThread(ImagePlus img, int ThrVal, int minSize, int maxSize, boolean showParticles, boolean showEdges, boolean showCentres, boolean showCentresInt, boolean new_results) {
        this.img = img;
        this.ThrVal = ThrVal;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.showParticles = showParticles;
        this.showEdges = showEdges;
        this.showCentres = showCentres;
        this.showCentresInt = showCentresInt;
        this.new_results = new_results;
        this.summary = false;
        thread = new Thread(Thread.currentThread().getThreadGroup(), this, "Object_Counter3D " + img.getTitle());
        thread.start();
        return thread;
    }
    public void run() {
        Width=img.getWidth();
        Height=img.getHeight();
        NbSlices=img.getStackSize();
        imgtitle = img.getTitle();
        analyze();
    }
}

