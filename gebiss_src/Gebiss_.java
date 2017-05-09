

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilter;

import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.io.FileInfo;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.io.TiffEncoder;
import ij.plugin.frame.ContrastAdjuster;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackConverter;
import bii.ij3d.BiiImageJ3DViewer;
import bii.ij3d.behaviors.ViewPlatformTransformer;

import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Collections;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.ArrayList;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import net.sf.ij_plugins.im3d.grow.ConnectedThresholdGrowerPlugin;

import sun.awt.WindowClosingListener;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Component;
import java.awt.Point;
import java.awt.Scrollbar;
import java.awt.Adjustable;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class Gebiss_ implements PlugInFilter
{
	private final static String TAB_DELIM = "\t";
	private final static String PREFS_SAVE_ROI_DIRECTORY = "segmentingassistant.saveroi.dir";
	private final static String PREFS_VIEW_ISOSURFACE_FLAG = "segmentingassistant.viewisosurface.flag";
	private final static String PREFS_USE_MEDIAN_FILTER_FLAG = "segmentingassistant.usemedianfilter.flag";
	private final static String PREFS_VOXEL_DEPTH = "segmentingassistant.voxeldepth";
	private final static String PREFS_SEGMENTATION_THR1 = "segmentingassistant.segmentation.thr1";
	private final static String PREFS_SEGMENTATION_ZSLICE = "segmentingassistant.segmentation.zslice";
	private final static String PREFS_SEGMENTATION_THR2 = "segmentingassistant.segmentation.thr2";
	private final static String PREFS_SEGMENTATION_FILE = "segmentingassistant.segmentation.file";
	
	private static SegmentingAssistantFrame frame = null;
	private static ObjectGTFrame frameObjectGT = null;
	private static ObjectSubsetFrame frameObjectSubset = null;
	private static SegmentingAssistantFrameTop frameTop = null;
	private static BenchFrame frameBench = null;
	private static JTabbedPane jtp = null;
	
	private ImagePlus imp;
	private ImagePlus impFuzzy;
	private ImageProcessor ip;
	// this is a flag to indicate whether to perform ROI update when X, Y, Min Level or
	// Max Level slider is changed
	private boolean canUpdateROI = true;
	private boolean showLoadParameter = true;
	private boolean debug = false;
	private boolean resetRoiList = false;
	private String windowTitle;
	
	private static Gebiss_ gebiss;
	
	public int setup( String arg, ImagePlus imp )
	{
                gebiss = this;
                if ( imp == null )
		{
			IJ.showMessage( "No image, please open an image first!!!" );
			return DONE;
		}
             
		//windowTitle = WindowManager.getCurrentWindow().getTitle();
		//int lastSpaceIndex = windowTitle.lastIndexOf(" ");
		//windowTitle = windowTitle.substring(0, lastSpaceIndex);
   		//windowTitle = IJ.getImage().getShortTitle();
   		windowTitle = IJ.getImage().getTitle();
		//System.out.println(IJ.getImage().getShortTitle());
		
		if ( debug )
			IJ.write( "SegmentingAssistant.setup..." );
		
		loadFuzzyImage();

		this.imp = imp;
		//IJ.selectWindow(windowTitle);
		//IJ.run("8-bit");
		//IJ.run("Median...", "radius=3 stack");
		MouseWheelListener mvl[] = WindowManager.getCurrentWindow().getMouseWheelListeners();
		for (MouseWheelListener dmvl : mvl) {
			WindowManager.getCurrentWindow().removeMouseWheelListener(dmvl);
		}

		IJ.run("Create Shortcut... ", "command=[Image Calculator...] shortcut=y");
		
		String depth = Prefs.get( PREFS_VOXEL_DEPTH, null );
		if (depth != null) IJ.run("Properties...", "voxel_depth="+depth);

		return DOES_ALL;
	}
	
	// this is called by Groundtruth plugin
	public void run( ImagePlus imp )
	{
		this.imp = imp;
		showLoadParameter =false;
		
		/*
		if ( RoiManager.getInstance() == null || RoiManager.getInstance().getCount() == 0 )
		{
			IJ.showMessage( "Warning", "No ROI created!" );
		}
		 */
		
		imp.killRoi();
		
		makeWindow( true );
	}

	public void run( ImageProcessor ip )
	{
		this.ip = ip;
		if ( debug )
			IJ.write( "SegmentingAssistant.run..." );

		int measurements = Analyzer.getMeasurements();
		// defined in Set Measurements dialog
		measurements |= Measurements.RECT; // make sure centroid is included
		measurements |= Measurements.MIN_MAX; // make sure min_max is included
		Analyzer.setMeasurements( measurements );
		
		imp.killRoi();
		
		// create and pop up a new ROI Manager window, subsequently use the static method
		// RoiManager.getInstance() to retrieve back the same object.
		new RoiManager();

		makeWindow( false );
	}

	/** Leong Poh - 20071105 
	 * This method will attempt find all the matched time point/frame result
	 * from the given input string, and return the last matched string.
	 * The pattern must match "tXXzXX", where XX denotes any number of digit.
	 * e.g
	 * "nothing" returns ""
	 * "Part1_t01z01_Part2_T02Z02" returns "T02Z02"
	 * "GroundTruthSegmentingAssistantParameters_t03z03" returns "t03z03"
	 * @param string Input string.
	 * @return Last matched string, or empty String if no match found.
	 */
	private String parseLastTimePointAndFrameString( String string )
	{
		Matcher matcher = Pattern.compile( "[tT](\\d+)[zZ](\\d+)" ).matcher(
				string );
		String result = "";
		// loop till the last matched
		while ( matcher.find() )
		{
			result = string.substring( matcher.start(), matcher.end() );
		}
		return result;
	}

	private boolean outline( String filename )
	{
		if ( debug )
			IJ.write( "SegmentingAssistant.outline..." );
		if ( debug )
			IJ.write( "trying to make a WandMM..." );

		if ( debug )
			IJ.write( "made a WandMM..." );

		RoiManager myRoiManager = RoiManager.getInstance();
		if ( myRoiManager != null )
		{
			myRoiManager.close();
			myRoiManager = new RoiManager();
		}
		
		( ( ResultTableModel )frame.table.getModel() ).clear();
		
		// KVJ innen kezdodik Caleb kodja a parameterfajl beolvasasra ReadFile.java-bol
		// a "/home/janoskv/test" a parameterfajl, ez eredetileg futtatasi parameterkent lett megadva
		//String filename = System.getProperty( "user.home" ) + File.separator
		//		+ "GroundTruthSegmentingAssistantParameters";
		//filename = "C:\\Users\\cheoklp\\workspace\\ImageJ\\src\\resource\\GroundTruthSegmentingAssistantParameters";
		
		frame.roisPerSlice = new int[imp.getImageStackSize()];
		frame.curRoiTableBarValue = WindowManager.getCurrentImage().getCurrentSlice() - 1;
    	frame.x = new int[imp.getImageStackSize()][];
    	frame.y = new int[imp.getImageStackSize()][];
    	frame.min = new double[imp.getImageStackSize()][];
    	frame.max = new double[imp.getImageStackSize()][];
		ArrayList<Integer> xList = new ArrayList<Integer>();
		ArrayList<Integer> yList = new ArrayList<Integer>();
		ArrayList<Double> minList = new ArrayList<Double>();
		ArrayList<Double> maxList = new ArrayList<Double>();
		boolean isNewFormat = false;
		double current_z = 1;
		double z = 0;

		try
		{
			//		System.out.println("Processing args[0]..."+args[0]);
			//System.out.println( "filename: " + filename );
			BufferedReader br = new BufferedReader( new FileReader( filename ) ); // Ide beepiteni hogy a "/home/janoskv/test" parameterfajlt olvassa be.
			// first row is header, read and discard
			String line = br.readLine();
			if (line.indexOf("z") >= 0) isNewFormat = true;
			while ( ( line = br.readLine() ) != null )
			{
				StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
				while ( st.hasMoreTokens() )
				{
					// first column is index number, discard
					st.nextToken();
					long min_level_manual = Math.round( Double.parseDouble( st.nextToken() ) );
					long max_level_manual = Math.round( Double.parseDouble( st.nextToken() ) );
					long x = Math.round( Double.parseDouble( st.nextToken() ) ); //(int)x_centroid;
					long y = Math.round( Double.parseDouble( st.nextToken() ) ); //(int)y_centroid;
					//System.out.println(" min : "+min_level_manual+" max: "+max_level_manual+" x: "+x+" y: "+y);
					
					WandMM wand = null;
					if (isNewFormat) {
						z = Math.round( Double.parseDouble( st.nextToken() ) );
						
						imp.setSlice((int)z-1);
						impFuzzy.setSlice((int)z-1);
						wand = new WandMM( impFuzzy.getProcessor() );
						imp.killRoi();
						wand.npoints = 0;

						if (z == current_z) {
							xList.add(new Integer(Long.toString(x)));
							yList.add(new Integer(Long.toString(y)));
							minList.add(new Double(Long.toString(min_level_manual)));
							maxList.add(new Double(Long.toString(max_level_manual)));
						}
						else {
							int[] currentSlice_x = new int[xList.size()];
							int[] currentSlice_y = new int[yList.size()];
							double[] currentSlice_min = new double[minList.size()];
							double[] currentSlice_max = new double[maxList.size()];
				
							for (int i = 0; i < xList.size(); i++) {
								currentSlice_x[i] = xList.get(i);
								currentSlice_y[i] = yList.get(i);
								currentSlice_min[i] = minList.get(i);
								currentSlice_max[i] = maxList.get(i);
							}
					    	frame.x[(int)current_z-1] = currentSlice_x;
					    	frame.y[(int)current_z-1] = currentSlice_y;
					    	frame.min[(int)current_z-1] = currentSlice_min;
					    	frame.max[(int)current_z-1] = currentSlice_max;
							frame.roisPerSlice[(int)current_z-1] = xList.size();

							xList = new ArrayList<Integer>();
							yList = new ArrayList<Integer>();
							minList = new ArrayList<Double>();
							maxList = new ArrayList<Double>();
							xList.add(new Integer(Long.toString(x)));
							yList.add(new Integer(Long.toString(y)));
							minList.add(new Double(Long.toString(min_level_manual)));
							maxList.add(new Double(Long.toString(max_level_manual)));
							current_z = z;
						}						
					}
					else {
						xList.add(new Integer(Long.toString(x)));
						yList.add(new Integer(Long.toString(y)));
						minList.add(new Double(Long.toString(min_level_manual)));
						maxList.add(new Double(Long.toString(max_level_manual)));
					}
				
					//wand.autoOutline( x, y, (int)max_level, (int)max_level ); // KVJ: Ez a SegmentingAssistant lelke. mukodeset ld. kek fuzet 2006. IV. 24-nel!
					wand.autoOutline( ( int )x, ( int )y, ( int )min_level_manual, ( int )max_level_manual ); //KVJ "(int)min_level" es "(int)max_level" helyett
					
					//				String s = ":[" + x + "," + y + "]: ("
					//				    + wand.npoints + ":" + wand.xpoints[0] + "," + wand.ypoints[0] + ")";
					/* if ( wand.npoints < 3 ) {					
					    IJ.write( "wand.autoOutline failed" + s );
					    //return false;
						continue;
					} */
					if (z-1 == frame.curRoiTableBarValue) {
						Roi roi = new PolygonRoi( wand.xpoints, wand.ypoints, wand.npoints, impFuzzy, Roi.POLYGON );
						imp.setRoi( roi );
						RowData rowData = new RowData( roi, ( int )min_level_manual, ( int )max_level_manual, ( int )x, ( int )y );
						roi.setName( frame.roiNF.format( frame.table.getRowCount() + 1 ) );
						( ( ResultTableModel )frame.table.getModel() ).addRow( rowData );
					}
					//				Rectangle r = roi.getBoundingRect();
					/*if ( r.width < min_width || r.height < min_height ) {
					// KVJ
						//	    IJ.write( "wand object too small:" + r.width + "x" + r.height + " " + s );
						//return false;
						continue;
					} 
					if ( wand.xpoints[0] - x > xd ) { 
						// KVJ
						//	    IJ.write( "wand too far?" + s );
						//return false;
						continue;
					} */
					
					// *****************************************
					// *** KVJ: Innen folytatni! Ide jon a ROI manager-es resz, hogy az Add gombbal hozzadja objektumkent a ROI listahoz
					// *****************************************
					if (z-1 == frame.curRoiTableBarValue) {
						if ( myRoiManager == null )
						{
							myRoiManager = new RoiManager();
						}
						myRoiManager.runCommand( "add" );
					}
				} //KVJ: Ez a "while (st.hasMoreTokens())" bezaroja
			}
			br.close();

			int[] currentSlice_x = new int[xList.size()];
			int[] currentSlice_y = new int[yList.size()];
			double[] currentSlice_min = new double[minList.size()];
			double[] currentSlice_max = new double[maxList.size()];

			for (int i = 0; i < xList.size(); i++) {
				currentSlice_x[i] = xList.get(i);
				currentSlice_y[i] = yList.get(i);
				currentSlice_min[i] = minList.get(i);
				currentSlice_max[i] = maxList.get(i);
			}

			if (isNewFormat) {
		    	frame.x[(int)current_z-1] = currentSlice_x;
		    	frame.y[(int)current_z-1] = currentSlice_y;
		    	frame.min[(int)current_z-1] = currentSlice_min;
		    	frame.max[(int)current_z-1] = currentSlice_max;
				frame.roisPerSlice[(int)current_z-1] = xList.size();
			} else {
		    	frame.x[frame.curRoiTableBarValue] = currentSlice_x;
		    	frame.y[frame.curRoiTableBarValue] = currentSlice_y;
		    	frame.min[frame.curRoiTableBarValue] = currentSlice_min;
		    	frame.max[frame.curRoiTableBarValue] = currentSlice_max;
				frame.roisPerSlice[frame.curRoiTableBarValue] = xList.size();				
			}
		} // KVJ: Ez a "try" bezaroja
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		/*
		String filenameROISet;
		if ( imp.getStack().getSliceLabel( imp.getCurrentSlice() ) != null )
		{
			filenameROISet = imp.getStack().getSliceLabel(
					imp.getCurrentSlice() );
		} else
		{
			filenameROISet = imp.getOriginalFileInfo().fileName;
		}

		myRoiManager
				.runCommand(
						"save",
						"/home/janoskv/imaging_folder_kozos-bol/2007-07-07_HistoneGFP_2_2hr_z1to70_t1to10.ics_eredeti_WeeChoo-tol/roi/RoiSetSegmentingAssistantAUTO/t06RoiSet/"
								+ parseLastTimePointAndFrameString( filenameROISet )
								+ "RoiSet.zip" );
		 */

		imp.setActivated();

		//NewImage myNewImage = new NewImage();
		//myNewImage.createByteImage( "t05_z_SegmAssist_ROI_Mask", 1024, 1024, 1,
		//		NewImage.FILL_WHITE ); //(java.lang.String title, int width, int height, int slices, int options) options = FILL_WHITE=4 ld.: http://rsb.info.nih.gov/ij/developer/api/constant-values.html#ij.gui.NewImage.FILL_WHITE
		//myRoiManager.runCommand( "fill" );

		/*
		( ( ResultTableModel )frame.table.getModel() ).clear();
		ResultTableModel model = ( ResultTableModel )frame.table.getModel();
		int roiNumber = 1;
		for (int i = 0; i < frame.roisPerSlice[frame.curRoiTableBarValue]; i++) {
			int z = frame.curRoiTableBarValue;
			int x = frame.x[z][i];
			int y = frame.y[z][i];
			int min = (int) Math.round(frame.min[z][i]);
			int max = (int) Math.round(frame.max[z][i]);
				
			Roi roi = createROI( x, y, min, max );
			if ( frame.table.getModel().getRowCount() >0 )
			{
				roiNumber = Integer.parseInt( ( ( Roi )model.getValueAt( model.getRowCount() - 1, 0 ) ).getName() ) + 1;
			}
			roi.setName( frame.roiNF.format( roiNumber ) );
			model.addRow( new RowData( roi, min, max, x, y ) );
		}
		*/
		frame.updateROIManager();
		frame.roiTableBar.setValue((int)z-1);
		
		return true;
	} 

	/** Adds the specified ROI to the list. The third argument ('n') will 
	be used form the first part of the ROI lable if it is >= 0. */
	/*    public void add(ImagePlus imp, Roi roi, int n) {
	if (roi==null) return;
	String label = getLabel(imp, roi, n);
	if (label==null) return;
	list.add(label);
	roi.setName(label);
	roiCopy = (Roi)roi.clone();
	Calibration cal = imp.getCalibration();
	if (cal.xOrigin!=0.0 || cal.yOrigin!=0.0) {
	    Rectangle r = roiCopy.getBounds();
	    roiCopy.setLocation(r.x-(int)cal.xOrigin, r.y-(int)cal.yOrigin);
	}
	rois.put(label, roiCopy);
	}
	 */

	public void updateROI()
	{
		updateROI( frame.xSlider.getValue(), frame.ySlider.getValue(), frame.minLevelSlider.getValue(), frame.maxLevelSlider.getValue() );
	}

	/** 20071106
	 * This method will try to create a ROI on the specified canvas based on
	 * the given x, y, min and max.
	 * 
	 * @param x Horizontal centroid
	 * @param y Vertical centroid
	 * @param min Min level
	 * @param max Max level
	 */
	private void updateROI( int x, int y, int min, int max )
	{
		updateROI( createROI( x, y, min, max ) );
	}
	
	private void updateROI( Roi roi )
	{
		imp.setRoi( roi );
		imp.setActivated();
	}
	
	private Roi createROI( int x, int y, int min, int max )
	{
		WandMM w = new WandMM( imp.getProcessor() );
		w.npoints = 0;
		w.autoOutline( x, y, min, max );
		
		PolygonRoi roi = new PolygonRoi( w.xpoints, w.ypoints, w.npoints, imp, Roi.TRACED_ROI );
		Roi.setColor( Color.RED );
		
		return roi;
	}
	
	private Roi createFuzzyROI( int x, int y, int min, int max, Color color )
	{
		impFuzzy.setSlice(imp.getCurrentSlice());
		WandMM w = new WandMM( impFuzzy.getProcessor() );
		w.npoints = 0;
		w.autoOutline( x, y, min, max );
		
		PolygonRoi roi = new PolygonRoi( w.xpoints, w.ypoints, w.npoints, impFuzzy, Roi.TRACED_ROI );
		Roi.setColor( color );
		
		return roi;
	}
	
	private void disposeFrameTop() 
	{
		frameTop.setVisible( false );
		frameTop.dispose();
		frameTop = null;		
	}
	
	private void makeWindow( boolean canUpdateUI )
	{
		if ( debug )
			IJ.write( "SegmentingAssistant.makeWindow..." );
		
		if ( frame != null )
		{
			//frame.dispose();
		}
		
		if (frameTop == null) {
			frameTop = new SegmentingAssistantFrameTop();
			frameTop.addWindowListener(new WindowAdapter()
			{
				public void windowClosing( WindowEvent e ) 
				{
					disposeFrameTop();
				}
	        });
			Toolkit tk = Toolkit.getDefaultToolkit ();
		    Dimension screen = tk.getScreenSize ();	
			frameTop.setSize(750, 700);
			//frameTop.setLocation((int) screen.getWidth()-900, (int) screen.getHeight()-700);
	        Point location = WindowManager.getCurrentWindow().getLocation();
			int width = WindowManager.getCurrentWindow().getWidth();
			frameTop.setLocation(location.x + width, location.y);
			frameTop.setVisible(true);
		}
		//frame = new SegmentingAssistantFrame();
		
		if ( canUpdateUI )
		{
			frame.updateUI();
		}
	}

	/**
	 * This key listener can only be registered by JTextField.
	 * The listener will restrict the text field for numbers and enter key event.
	 * Once user hit enter, it will validate that the number must be within slider
	 * range, and update slider value only if its value is different from text field.
	 * 
	 */
	private class TextKeyListener extends KeyAdapter
	{
		private JSlider slider;

		public TextKeyListener( JSlider slider )
		{
			this.slider = slider;
		}

		public void keyTyped( KeyEvent ke )
		{
			// only accept 0-9 or ENTER key, otherwise the key will be consumed
			if ( !( ( ke.getKeyChar() >= KeyEvent.VK_0 && ke.getKeyChar() <= KeyEvent.VK_9 ) || ke
					.getKeyChar() == KeyEvent.VK_ENTER ) )
			{
				ke.consume();
				return;
			}
		}

		public void keyPressed( KeyEvent ke )
		{
			JTextField text = ( JTextField )ke.getSource();
			String string = text.getText();

			if ( text.getText() != null && text.getText().length() > 0
					&& ke.getKeyChar() == KeyEvent.VK_ENTER )
			{
				try
				{
					int i = Integer.parseInt( text.getText() );
					if ( i < slider.getMinimum() )
					{
						text.setText( String.valueOf( slider.getMinimum() ) );
						text.updateUI();
					}
					else if ( i > slider.getMaximum() )
					{
						text.setText( String.valueOf( slider.getMaximum() ) );
						text.updateUI();
					}
					slider.setValue( i );
				}
				catch ( NumberFormatException nfe )
				{
					// reset to previous value
					text.setText( string );
					text.updateUI();
				}
			}
		}
	}
	
	/**
	 * This mouse listener is to register to the ImageCanvas from ImageJ.
	 * When user clicked on the image, the X & Y coordinates will be captured and
	 * used to update the Segmenting Assistant window's X & Y sliders. 
	 */
	private class ImageWindowMouseListener extends MouseAdapter
	{
		public void mouseClicked( MouseEvent me )
		{
			// only respond to left mouse click
			if (  me.getButton() != MouseEvent.BUTTON1 )
			{
				return;
			}
			// Tell the ImageWindowAdjustmentListener to reinstantiate the roiAtStack.
			resetRoiList = true;
			
			ImageCanvas canvas = ( ImageCanvas )me.getSource();
			int x = canvas.offScreenX( me.getX() );
			int y = canvas.offScreenY( me.getY() );
			// if left mouse click with control key, get the ROI from result
			// table, where the X & Y is in the polygon
			if ( me.isControlDown() )
			{
				ResultTableModel model = ( ResultTableModel )frame.table.getModel();
				
				for ( int i = 0 ; i < model.getRowCount(); i++ )
				{
					RowData rowData = model.getRow( i );
					if ( rowData.getRoi().getPolygon().contains( x, y ) )
					{
						rowData.getRoi().setInstanceColor( Color.GREEN );
						frame.table.getSelectionModel().setSelectionInterval( i, i );
						canUpdateROI = false;
						frame.getXSlider().setValue( ( int )Math.round( rowData.getX() ) );
						frame.getYSlider().setValue( ( int )Math.round( rowData.getY() ) );
						frame.minLevelSlider.setValue( ( int )Math.round( rowData.getMin() ) );
						frame.maxLevelSlider.setValue( ( int )Math.round( rowData.getMax() ) );
						canUpdateROI = true;
						drawFuzzyRoi(Color.GREEN);
						//if (impFuzzy == null) updateROI( rowData.getRoi() );
						rowData.getRoi().setInstanceColor( Color.RED );
						break;
					}
				}
			}
			// else construct new ROI based on the X & Y location
			else
			{
				canUpdateROI = false;
				frame.getXSlider().setValue( x );
				frame.getYSlider().setValue( y );
				canUpdateROI = true;

				drawFuzzyRoi(Color.RED);
				//if (impFuzzy == null && Toolbar.getToolId() != Toolbar.POINT)  {
					//updateROI();
				//}
			}
			
			canvas.requestFocus();
		}
	}
	
	private class ObjectImageWindowMouseListener extends MouseAdapter
	{
		private int pressedX;
		private int pressedY;
		private int releasedX;
		private int releasedY;
		private int seedX;
		private int seedY;
		private String title_sel_1;
		private String title_sel_2;
		
		public void mousePressed( MouseEvent me )
		{
			if (me.isControlDown()) return;
			pressedX = me.getX();
			pressedY = me.getY();
		}
		public void mouseReleased( MouseEvent me )
		{
			if (me.isControlDown()) return;
			releasedX = me.getX();
			releasedY = me.getY();
			
			if (pressedX < seedX && seedX < releasedX && pressedY < seedY && seedY < releasedY) {
				getThreshold();
			}
		}
		
		public void mouseClicked( MouseEvent me )
		{
			// only respond to left mouse click
			if (  me.getButton() != MouseEvent.BUTTON1 ) return;
			if ( me.isControlDown()  && jtp.getSelectedIndex()==1)
			{
				seedX = me.getX();
				seedY = me.getY();
				
				ImageCanvas canvas = ( ImageCanvas )me.getSource();
				int x = canvas.offScreenX( me.getX() );
				int y = canvas.offScreenY( me.getY() );
				frameObjectGT.xTextField.setText(x + "");
				frameObjectGT.yTextField.setText(y + "");
				frameObjectGT.zTextField.setText(WindowManager.getCurrentImage().getCurrentSlice() + "");

		        IJ.setTool(Toolbar.RECTANGLE);

		        if (pressedX < seedX && seedX < releasedX && pressedY < seedY && seedY < releasedY) {
					getThreshold();
				}
			}
		}

		private void getThreshold()
		{
			final boolean viewIsosurface = frameObjectGT.viewIsosurface;
			//IJ.run("8-bit");
			//IJ.run("Median...", "radius=3 stack");
			
			title_sel_1 = IJ.getImage().getShortTitle() + "-sel-1";
			if (viewIsosurface) title_sel_2 = IJ.getImage().getShortTitle() + "-sel-2";
			frameObjectGT.minTextField.setText("");
			IJ.run("Duplicate...", "title=" + title_sel_1 + " duplicate");
			IJ.selectWindow(title_sel_1);
			IJ.run("8-bit");
			//IJ.run("Median...", "radius=3 stack");
			ImagePlus sel1 = WindowManager.getCurrentImage();
			//sel1.hide();
	        int nbSlices = sel1.getStackSize();
			int sliceNum = Integer.parseInt(frameObjectGT.zTextField.getText());
	        sel1.setSlice(sliceNum);
	        IJ.run("Brightness/Contrast...");
	        IJ.run("Enhance Contrast", "saturated=0.5");
	        IJ.run("Apply LUT", "stack");
	        Frame frame = WindowManager.getFrame("B&C");
            ((ContrastAdjuster)frame).close();
	        
			if (viewIsosurface) {
				IJ.selectWindow(windowTitle);
				IJ.run("Duplicate...", "title=" + title_sel_2 + " duplicate");
				IJ.selectWindow(title_sel_2);
				IJ.run("8-bit");
				if (frameTop.useMedianFilter == true) {
					IJ.run("Median...", "radius=3 stack");
				}
				//ImagePlus sel2 = WindowManager.getCurrentImage();
				//sel2.hide();
			}
			//IJ.run("BII");
			new BiiImageJ3DViewer().run(null);
			int ijX = Prefs.getInt("ij.x",-99);
			int ijY = Prefs.getInt("ij.y",-99);
			BiiImageJ3DViewer.setLocation(300, ijY + 100);
			BiiImageJ3DViewer.add(title_sel_1, "None", title_sel_1, "0", "true", "true", "true", "1", "0");
			BiiImageJ3DViewer.select(title_sel_1);
			BiiImageJ3DViewer.setTransparency("0.35");

			if (viewIsosurface) {
				BiiImageJ3DViewer.add(title_sel_2, "None", title_sel_2, "0", "true", "true", "true", "1", "0");
				BiiImageJ3DViewer.select(title_sel_2);
				BiiImageJ3DViewer.lock();
			}
			
			ObjectResultTableModel model = ( ObjectResultTableModel )frameObjectGT.table.getModel();
			ArrayList<Integer> mins = new ArrayList<Integer>();
		    for(int i=0; i<model.getRowCount(); i++) {
		    	mins.add(new Integer(model.getValueAt(i, 1) + ""));
		    }
		    Collections.sort(mins);
		    int median = 0;
		    if (mins.size() % 2 == 0)
		      {
		    	int i = mins.size() / 2;
		    	if (i > 0) {
			    	median = mins.get(i-1) + mins.get(i);
			    	median = Math.round((float)median/2);
		    	}
		      }
		      else
		      {
		        median = mins.get(mins.size() / 2);
		      }
		    BiiImageJ3DViewer.presetThreshold(median + "");
		    BiiImageJ3DViewer.changeThreshold();

			if (viewIsosurface) {
				BiiImageJ3DViewer.displayAsSurface();
				BiiImageJ3DViewer.select(title_sel_2);
				BiiImageJ3DViewer.setColor("255", "0", "0");
				BiiImageJ3DViewer.select(title_sel_2);
				BiiImageJ3DViewer.unshadeSurface();
			}
			
			new Thread() {
                @Override
                public void run() {
                	while (true) {
                		try {
                			if (BiiImageJ3DViewer.isThresholdSet()) {
                				break;
                			}
                			if (BiiImageJ3DViewer.wasCancelled()) {
                				break;
                			}
                			Thread.sleep(1000);
                		}
                		catch (InterruptedException e) {}
                	}
                	
                	if (BiiImageJ3DViewer.isThresholdSet()) {
	    				frameObjectGT.minTextField.setText(BiiImageJ3DViewer.getThreshold() + "");
	
	    				ObjectResultTableModel model = ( ObjectResultTableModel )frameObjectGT.table.getModel();
	    				model.addRow( new ObjectRowData( frameObjectGT.table.getModel().getRowCount() + 1,
	    						Integer.parseInt(frameObjectGT.minTextField.getText()),
	    						Integer.parseInt(frameObjectGT.maxTextField.getText()),
	    						Integer.parseInt(frameObjectGT.xTextField.getText()),
	    						Integer.parseInt(frameObjectGT.yTextField.getText()),
	    						Integer.parseInt(frameObjectGT.zTextField.getText()),
	    						0));
	    				
	    				frameObjectGT.table.getSelectionModel().addListSelectionListener( new ListSelectionListener()
	    				{
	    					public void valueChanged( ListSelectionEvent lse )
	    					{
	    						frameObjectGT.highlightRow(lse);
	    					}
	    				});
	    				frameObjectGT.table.setRowSelectionInterval(model.getRowCount()-1, model.getRowCount()-1);
                	}
    				
                	BiiImageJ3DViewer.close();
    				IJ.selectWindow(title_sel_1);
                    IJ.run("Close");
        			if (viewIsosurface) {
	    				IJ.selectWindow(title_sel_2);
	                    IJ.run("Close");
        			}
                    //IJ.setTool(Toolbar.RECTANGLE);
                    
                    pressedX = 0;
                    pressedY = 0;
                    releasedX = 0;
                    releasedY = 0;
                    seedX = 0;
                    seedY = 0;
                    title_sel_1 = "";
                }
            }.start();
		}
	}

	/**
	 * This mouse wheel listener is to register to the ImageCanvas from ImageJ.
	 * When user rotate the wheel, it will change the Segmenting Assistant
	 * window's Min Level slider. 
	 */
	private class ImageWindowWheelMouseListener implements MouseWheelListener
	{
		public void mouseWheelMoved( MouseWheelEvent mwe )
		{
			int value = 0;
			/*
			if ( !mwe.isControlDown() )
			{
				return;
			}
			 */
			if(mwe.isShiftDown())
				value = (mwe.getWheelRotation()*20) + frame.minLevelSlider.getValue();
			else if(mwe.isControlDown())
				value = (mwe.getWheelRotation()*50) + frame.minLevelSlider.getValue();
			else
				value = mwe.getWheelRotation() + frame.minLevelSlider.getValue();
			if ( value >= frame.minLevelSlider.getMinimum() && value <= frame.minLevelSlider.getMaximum() )
			{
				frame.minLevelSlider.setValue( value );
			}
		}
	}
	
	
	/**
	 * This listener listens to scrollbars. Note: this applies to imageWindow not Canvas
	 * as canvas is the internal section of the window. When a scrollbar is clicked/drag it will
	 * activate the listener. After which base on current ROI value, will try to find
	 * the new ROI base on the same min and max pixel values and within a certain range of x,y.	
	 */
	private class ImageWindowAdjustmentListener implements AdjustmentListener {
		Adjustable source;
		int orient;
    	Roi finalRoi;
        // This method is called whenever the value of a scrollbar is changed,
        // either by the user or programmatically.
        public void adjustmentValueChanged(AdjustmentEvent evt) {
        	//Set the slice number using AdjustmentEvent value which equals to slice number.
        	//If you don't set the slice number, you will get mix results as sometimes compute using previous slice.
        	IJ.getImage().setSlice(evt.getValue());
            source = evt.getAdjustable();
            // Since we need to calculate change in ROI for all scrollbar events
            // We do not need to determine the type of event that occured.
            // Determine which scrollbar fired the event
            orient = source.getOrientation();
            if (orient == Adjustable.HORIZONTAL) {
            	if((evt.getAdjustmentType() == AdjustmentEvent.TRACK && !evt.getValueIsAdjusting())
            		|| (evt.getAdjustmentType() == AdjustmentEvent.UNIT_DECREMENT || evt.getAdjustmentType() == AdjustmentEvent.UNIT_INCREMENT)) {
            		int x = frame.xSlider.getValue();
                	int y = frame.ySlider.getValue();
                	int min = frame.minLevelSlider.getValue();
                	int max = frame.maxLevelSlider.getValue();
            		finalRoi = createROI(x, y, min, max);
            		updateROI(finalRoi);
            		//System.out.println(x + "\t" + y + "\t" + min + "\t" + max + "\t" + finalRoi.getLength());
            	}
            }
        }
	}
	
	private class SegmentingAssistantFrameTop extends JFrame implements WindowListener, KeyListener
	{
		private String lastKeyName = "";
		private boolean useMedianFilter = Boolean.parseBoolean(Prefs.get( PREFS_USE_MEDIAN_FILTER_FLAG, null ));
		
		private void makeFuzzyStack8bit() {
			//IJ.selectWindow("fuzzy_stack");
			//IJ.run("8-bit");
			new StackConverter(impFuzzy).convertToGray8();
		}
		
		private void validateInput(KeyEvent e) {
		    char c = e.getKeyChar();
		    if (!((Character.isDigit(c) ||
		      (c == KeyEvent.VK_BACK_SPACE) ||
		      (c == KeyEvent.VK_DELETE)))) {
		        getToolkit().beep();
		        e.consume();
		    }
		}
		
		private ArrayList<Centroid> extractCentroids(String filename) throws Exception {
			BufferedReader br = new BufferedReader( new FileReader( filename ) ); 
			// first row is header, read and discard
			String line1 = br.readLine();
			if (!line1.contains("Volume\tSurface\tIntensity\tCentre X\tCentre Y\tCentre Z\tCentre int X\tCentre int Y\tCentre int Z"))
				throw new RuntimeException("invalid format");
			String line = "";
			int lineNum = 0;
			ArrayList<Centroid> list = new ArrayList<Centroid>();
			while ( ( line = br.readLine() ) != null )
			{
				StringTokenizer st = new StringTokenizer( line );
				while ( st.hasMoreTokens() )
				{
					// discard
					String tok1 = st.nextToken();
					String tok2 = st.nextToken();
					String tok3 = st.nextToken();
					String tok4 = st.nextToken();
					
					Centroid centroid = new Centroid(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
					list.add(centroid);
					break;
				}
			}
			br.close();
			return list;
		}
		
		private void setMenuBar(int tabNum) {
			if (tabNum == 0) setMenuBar0();
			if (tabNum == 1) setMenuBar1();
			if (tabNum == 2) setMenuBar2();
			if (tabNum == 3) setMenuBar3();
		}
		
		private void setMenuBar3() {
			impFuzzy.hide();
			
			JMenuBar menuBar = new JMenuBar();
			JMenu dummyMenu = new JMenu(" ");
			dummyMenu.setEnabled(false);
	        menuBar.add(dummyMenu);
	        setJMenuBar(menuBar);
		}
	
		private void setMenuBar2() {
			impFuzzy.show();
			makeFuzzyStack8bit();
			
			JMenuBar menuBar = new JMenuBar();
	        JMenu processMenu = new JMenu("Process");
	        menuBar.add(processMenu);
	        setJMenuBar(menuBar);

	        final JMenuItem diffThresholdAction = new JMenuItem("Segmentation");
	        diffThresholdAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					final JDialog dialog = new JDialog();
					GridLayout gl = new GridLayout(5, 1);
					dialog.getContentPane().setLayout(new GridLayout(5, 1));
					JPanel panel1 = new JPanel();
					panel1.setBorder(new EmptyBorder(5, 5, 5, 5));
					dialog.getContentPane().add(panel1);
					JPanel panel2 = new JPanel();
					panel2.setBorder(new EmptyBorder(5, 5, 5, 5));
					dialog.getContentPane().add(panel2);
					JPanel panel3 = new JPanel();
					panel3.setBorder(new EmptyBorder(5, 5, 5, 5));
					dialog.getContentPane().add(panel3);
					JPanel panel4 = new JPanel();
					panel4.setBorder(new EmptyBorder(5, 5, 5, 5));
					dialog.getContentPane().add(panel4);
					JPanel panel5 = new JPanel();
					dialog.getContentPane().add(panel5);

					JLabel label1 = new JLabel("Threshold less than z slice demarcation value: ");
					String defaultThr1 = Prefs.get( PREFS_SEGMENTATION_THR1, null );
					if (defaultThr1 == null) defaultThr1 = "0";
					final JTextField tf1 = new JTextField(defaultThr1, 4);

					tf1.addKeyListener(new KeyAdapter() {
						@Override
						  public void keyTyped(KeyEvent e) {
							validateInput(e);
						  }

			        });
					
					panel1.setLayout(new BorderLayout());
					panel1.add(label1, BorderLayout.CENTER);
					panel1.add(tf1, BorderLayout.EAST);
					JLabel label2 = new JLabel("Z slice demarcation value: ");
					String defaultZSlice = Prefs.get( PREFS_SEGMENTATION_ZSLICE, null );
					if (defaultZSlice == null) defaultZSlice = "0";
					final JTextField tf2 = new JTextField(defaultZSlice, 4);

					tf2.addKeyListener(new KeyAdapter() {
						@Override
						  public void keyTyped(KeyEvent e) {
							validateInput(e);
						  }

			        });
					
					panel2.setLayout(new BorderLayout());
					panel2.add(label2, BorderLayout.CENTER);
					panel2.add(tf2, BorderLayout.EAST);
					JLabel label3 = new JLabel("Threshold more than z slice demarcation value: ");
					String defaultThr2 = Prefs.get( PREFS_SEGMENTATION_THR2, null );
					if (defaultThr2 == null) defaultThr2 = "0";
					final JTextField tf3 = new JTextField(defaultThr2, 4);

					tf3.addKeyListener(new KeyAdapter() {
						@Override
						  public void keyTyped(KeyEvent e) {
							validateInput(e);
						  }

			        });
					
					panel3.setLayout(new BorderLayout());
					panel3.add(label3, BorderLayout.CENTER);
					panel3.add(tf3, BorderLayout.EAST);
					panel4.setLayout(new BorderLayout());
	        		panel4.add(new JLabel("Centroid input file: "), BorderLayout.WEST);
					final JTextField inputFile = new JTextField(4);
					inputFile.setEditable(false);

					String inputFilename = Prefs.get( PREFS_SEGMENTATION_FILE, null );
					if (inputFilename == null) inputFilename = "";
					inputFile.setText(inputFilename);
					
	        		panel4.add(inputFile, BorderLayout.CENTER);
	        		JButton findFileButton = new JButton("...");
	        		panel4.add(findFileButton, BorderLayout.EAST);
	        		
	        		findFileButton.addActionListener(new ActionListener() {
	    	            public void actionPerformed(ActionEvent arg0) {
	    					JFileChooser fc = new JFileChooser();
	    					
	    					// load the directory from ImageJ preference list
	    					String dir = Prefs.get( PREFS_SAVE_ROI_DIRECTORY, null );
	    					if ( dir != null )
	    					{
	    						fc.setCurrentDirectory( new File( dir ) );
	    					}

	    					// Only allow for single file selection.
	    					fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
	    					fc.setMultiSelectionEnabled( true );
	    					
	    					FileNameFilter CSVfilter = new FileNameFilter("csv", "Comma Separated Values (*.csv)");
	                        fc.addChoosableFileFilter(CSVfilter);
	                        fc.setFileFilter(fc.getAcceptAllFileFilter());
	    					
	    					// Show the file chooser dialog and update to the text field if any file has
	    					// been selected.
	    					if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION )
	    					{
	    						// save the directory to ImageJ preference list
	    						Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getCurrentDirectory().getAbsolutePath() );
	    						inputFile.setText(fc.getSelectedFile().getAbsolutePath());
	    					}
	    	            }
	    	        });

	        		JButton cancelButton = new JButton( "Cancel" );
	        		panel5.add(cancelButton);

					cancelButton.addActionListener( new ActionListener()
					{
						public void actionPerformed( ActionEvent ae )
						{
							dialog.dispose();
						}
					});
					
	        		JButton setButton = new JButton( "Set" );
	        		panel5.add(setButton);
	        		
	        		setButton.addActionListener( new ActionListener()
					{
						public void actionPerformed( ActionEvent ae )
						{
							int thr1 = Integer.parseInt(tf1.getText());
							int zSlice = Integer.parseInt(tf2.getText());
							int thr2 = Integer.parseInt(tf3.getText());
    						Prefs.set( PREFS_SEGMENTATION_THR1, thr1 );
    						Prefs.set( PREFS_SEGMENTATION_ZSLICE, zSlice );
    						Prefs.set( PREFS_SEGMENTATION_THR2, thr2 );
    						Prefs.set( PREFS_SEGMENTATION_FILE, inputFile.getText() );
							String filename = inputFile.getText();

							ArrayList<Centroid> centroids = null;
							try {
								centroids = extractCentroids(filename);
							}
							catch (Exception e) {
								IJ.showMessage("Please specify a valid file.");
								e.printStackTrace();
								dialog.dispose();
								return;
							}

							if (centroids.size() == 0) {
								dialog.dispose();
								return;
							}

							IJ.selectWindow("fuzzy_stack");

							Centroid c = centroids.get(0);
							int min = 0;
							if (c.getZ() < zSlice) min = thr1;
							else min = thr2;
			        		String arg = "x="+c.getX()+" y="+c.getY()+" z="+c.getZ()+" min="+min+" max=255";
			        		System.out.println("0: " + arg);
							IJ.run("Connected Threshold Grower ...", arg);
			        		//ImagePlus ip = WindowManager.getCurrentImage();
							ImagePlus ip = ConnectedThresholdGrowerPlugin.getRegion();
			        		ip.setTitle("Region1");
			        		
			        		for (int i = 1; i < centroids.size(); i++) {
								IJ.selectWindow("fuzzy_stack");
								Centroid c1 = centroids.get(i);
								
								if (c1.getZ() < zSlice) min = thr1;
								else min = thr2;
									
				        		String arg1 = "x="+c1.getX()+" y="+c1.getY()+" z="+c1.getZ()+" min="+min+" max=255";
				        		System.out.println(i + ": " + arg1);
								IJ.run("Connected Threshold Grower ...", arg1);
				        		//ImagePlus ip1 = WindowManager.getCurrentImage();
								ImagePlus ip1 = ConnectedThresholdGrowerPlugin.getRegion();
				        		ip1.setTitle("Region2");
								//IJ.run("Image Calculator...", "image1=Region1 operation=Add image2=Region2 stack");
				        		BiiImageCalculator.doStackOperation(ip, ip1);
								ip1.close();
								
								//if (i > 10) break;
								
								IJ.showStatus("Segmented " + i + " of " + centroids.size() + " centroids");
								IJ.showProgress(i, centroids.size());
			        		}
			        		
			        		ip.show();
			        		dialog.dispose();
						}
					});
	        		
					dialog.pack();
					dialog.setVisible( true );
	            }
	        });
	        processMenu.add(diffThresholdAction);
		}
	
		private void setMenuBar1() {
			impFuzzy.hide();
			makeFuzzyStack8bit();

			JMenuBar menuBar = new JMenuBar();
			JMenu fileMenu = new JMenu("File");
	        JMenu processMenu = new JMenu("Process");
	        JMenu viewMenu = new JMenu("View");
	        menuBar.add(fileMenu);
	        menuBar.add(processMenu);
	        menuBar.add(viewMenu);
	        setJMenuBar(menuBar);

	        final JMenuItem loadParameterAction = new JMenuItem("Load Parameter");
	        loadParameterAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					JFileChooser fc = new JFileChooser();
					
					// load the directory from ImageJ preference list
					String dir = Prefs.get( PREFS_SAVE_ROI_DIRECTORY, null );
					if ( dir != null )
					{
						fc.setCurrentDirectory( new File( dir ) );
					}

					// Only allow for single file selection.
					fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
					fc.setMultiSelectionEnabled( true );
					
					FileNameFilter CSVfilter = new FileNameFilter("csv", "Comma Separated Values (*.csv)");
                    fc.addChoosableFileFilter(CSVfilter);
					
					// Show the file chooser dialog and update to the text field if any file has
					// been selected.
					if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION )
					{
						// save the directory to ImageJ preference list
						Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getCurrentDirectory().getAbsolutePath() );
						frameObjectGT.loadParameter(fc.getSelectedFile().getAbsolutePath());
					}
	            }
	        });
	        fileMenu.add(loadParameterAction);

	        final JMenuItem saveParameterAction = new JMenuItem("Save Parameter");
	        saveParameterAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					frameObjectGT.saveParameter();
	            }
	        });
	        fileMenu.add(saveParameterAction);

	        fileMenu.addSeparator();

	        final JMenuItem exitAction = new JMenuItem("Exit");
	        exitAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
	            	doExit();
	            	//frameObjectGT.saveParameter();
					frameTop.setVisible( false );
					frameTop.dispose();
					frameTop = null;
	            }
	        });
	        fileMenu.add(exitAction);

	        final JCheckBoxMenuItem medianFilterAction = new JCheckBoxMenuItem("Use 3px Median Filter");
	        medianFilterAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
	            	useMedianFilter = medianFilterAction.getState();
	            	Prefs.set( PREFS_USE_MEDIAN_FILTER_FLAG, useMedianFilter );
	            }
	        });
	        medianFilterAction.setSelected(Boolean.parseBoolean(Prefs.get( PREFS_USE_MEDIAN_FILTER_FLAG, null )));
	        processMenu.add(medianFilterAction);
	        
	        final JMenuItem binStackAction = new JMenuItem("Binary Stack");
	        binStackAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK, false));
	        binStackAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
	        		int currentSlice = WindowManager.getCurrentImage().getCurrentSlice();
					if ( frameObjectGT.table.getRowCount() == 0 )
					{
						IJ.showMessage("No row to perform Connected Threshold Grower!");
						return;
					}
					if ( frameObjectGT.table.getRowCount() > 1 && frameObjectGT.table.getSelectedRow() < 0)
					{
						IJ.showMessage("No row selected to perform Connected Threshold Grower!");
						return;
					}
					int i = frameObjectGT.table.getSelectedRow() < 0? 0 : frameObjectGT.table.getSelectedRow();
					int j = frameObjectGT.table.getSelectedColumn() < 0? 0 : frameObjectGT.table.getSelectedColumn();
    				ObjectResultTableModel model = ( ObjectResultTableModel )frameObjectGT.table.getModel();
					int x = model.getRow(i).getSeedX();
					int y = model.getRow(i).getSeedY();
					int z = model.getRow(i).getSeedZ();
					int min = model.getRow(i).getMin();
					int max = model.getRow(i).getMax();
					String arg = "x="+x+" y="+y+" z="+z+" min="+min+" max="+max;

					impFuzzy.show();
					IJ.selectWindow("fuzzy_stack");
    				IJ.run("Connected Threshold Grower ...", arg);
					ConnectedThresholdGrowerPlugin.getRegion().show();
    	        	IJ.getImage().setSlice(currentSlice);
    	        	frameObjectGT.table.selectAll();
    	        	frameObjectGT.table.setRowSelectionInterval(i, i);
    	        	frameObjectGT.table.setColumnSelectionInterval(j, j);
    	        	impFuzzy.hide();
	            }
	        });
	        processMenu.add(binStackAction);

	        final JMenuItem splitAction = new JMenuItem("3D Split");
	        splitAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) 
	            {
	        		int[] wList = WindowManager.getIDList();
	        		String[] titles = new String[wList.length];
	        		for (int i=0; i<wList.length; i++) {
	        			ImagePlus imp = WindowManager.getImage(wList[i]);
	        			if (imp!=null)
	        				titles[i] = imp.getTitle();
	        			else
	        				titles[i] = "";
	        		}

	        		GenericDialog gd = new GenericDialog("3D Split", IJ.getInstance());
	        		String defaultItem = titles[0];
	        		gd.addChoice("Original stack (8 bits):", titles, defaultItem);
	        		gd.addChoice("Binary stack of selected object:", titles, defaultItem);
	        		gd.showDialog();
	        		if (gd.wasCanceled())
	        			return;
	        		int index1 = gd.getNextChoiceIndex();
	        		String title1 = titles[index1];
	        		int index2 = gd.getNextChoiceIndex();
	        		String title2 = titles[index2];

					IJ.selectWindow(title1);
					IJ.run("Duplicate...", "title=orig-1 duplicate");
					IJ.run("8-bit");
	    	        
	    	        String macroString = 
	    	        	"setForegroundColor(255,255,255);" +
	    	        	"selectWindow(\"" + title2 + "\");" +
	    	        	"run(\"Select All\");" +
	    	        	"run(\"Draw\", \"stack\");" +
	    	        	"setBackgroundColor(1,1,1);" +
	    	        	"setForegroundColor(1,1,1);" +
	    	        	"ImageWidth = getWidth(); ImageHeight = getHeight();" +
	    	        	"setBatchMode(true);" +
	    	        	"run(\"Select None\");" +
	    	        	"for (p=1; p<=nSlices; p++){" +
	    	        	"selectWindow(\"" + title2 + "\");" +
	    	        	"setSlice(p); run(\"Select None\");" +
	    	        	"run(\"Create Selection\");" +
	    	        	"getSelectionBounds(x, y, width, height);" +
	    	        	"if (x == 0 && y == 0 && width == ImageWidth && height == ImageHeight){" +
	    	        	"selectWindow(\"orig-1\"); setSlice(p);" +
	    	        	"run(\"Select All\");" +
	    	        	"run(\"Fill\", \"slice\");" +
	    	        	"run(\"Select None\");" +
	    	        	"} else {" +
	    	        	"selectWindow(\"orig-1\"); setSlice(p);" +
	    	        	"run(\"Restore Selection\");" +
	    	        	"run(\"Clear Outside\", \"slice\");" +
	    	        	"run(\"Select None\");" +
	    	        	"}" +
	    	        	"}" +
	    	        	"setBatchMode(false)";
	    	        IJ.runMacro(macroString);

					//IJ.run("BII");
					new BiiImageJ3DViewer().run(null);
					BiiImageJ3DViewer.add("orig-1", "None", "orig-1", "0", "true", "true", "true", "1", "0");
	            }
	        });
	        processMenu.add(splitAction);

	        final JMenuItem labelingAction = new JMenuItem("Labeling");
	        labelingAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
                	// activate ObjectCounter3D.java
					ObjectCounter3D labeling = new ObjectCounter3D();                                        
					labeling.setupGUI("");
					labeling.run();
	            }
	        });
	        processMenu.add(labelingAction);

	        final JCheckBoxMenuItem isosurfaceAction = new JCheckBoxMenuItem("Add Isosurface");
	        isosurfaceAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
	            	frameObjectGT.viewIsosurface = isosurfaceAction.getState();
	            	Prefs.set( PREFS_VIEW_ISOSURFACE_FLAG, frameObjectGT.viewIsosurface );
	            }
	        });
	        isosurfaceAction.setSelected(frameObjectGT.viewIsosurface);
	        viewMenu.add(isosurfaceAction);

	        final JMenuItem voxelDepthAction = new JMenuItem("Set Voxel Depth");
	        voxelDepthAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					final JDialog dialog = new JDialog();
					JLabel voxelDepthLabel = new JLabel("Voxel Depth");
					final JTextField voxelDepthField = new JTextField(Prefs.get( PREFS_VOXEL_DEPTH, null ), 10);
					JButton cancelButton = new JButton( "Cancel" );
					JButton setButton = new JButton( "Set" );
					JPanel mainPanel = new JPanel();
					JPanel subPanel = new JPanel();
					
					setButton.addActionListener( new ActionListener()
					{
						public void actionPerformed( ActionEvent ae )
						{
							String depth = voxelDepthField.getText();
							IJ.run("Properties...", "voxel_depth="+depth);
			            	Prefs.set( PREFS_VOXEL_DEPTH, depth );
							dialog.dispose();
						}
					});
					
					cancelButton.addActionListener( new ActionListener()
					{
						public void actionPerformed( ActionEvent ae )
						{
							dialog.dispose();
						}
					});
					
					dialog.setTitle( "Set Voxel Depth" );
					dialog.getContentPane().setLayout( new BorderLayout() );
					
					mainPanel.add( voxelDepthLabel );
					mainPanel.add( voxelDepthField );
					subPanel.add( cancelButton );
					subPanel.add( setButton );
					
					dialog.getContentPane().add( mainPanel, BorderLayout.CENTER );
					dialog.getContentPane().add( subPanel, BorderLayout.SOUTH );
					
					dialog.pack();
					dialog.setVisible( true );
	            }
	        });
	        viewMenu.add(voxelDepthAction);
		}
	
		private void setMenuBar0() {
			impFuzzy.hide();
			
			JMenuBar menuBar = new JMenuBar();
			JMenu fileMenu = new JMenu("File");
	        JMenu processMenu = new JMenu("Process");
	        menuBar.add(fileMenu);
	        menuBar.add(processMenu);
	        setJMenuBar(menuBar);
	        
	        final JMenuItem newAction = new JMenuItem("New");
	        newAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					(new Thread(new Runnable() {
						public void run() {
							newAction.setEnabled(false);
							// activate ground truth
							Groundtruth gt = new Groundtruth(windowTitle);                                        
							gt.setup("", Gebiss_.this.imp);
							gt.run( ip );
							newAction.setEnabled(true);
						}
					})).start();
	            }
	        });
	        fileMenu.add(newAction);
	        
	        fileMenu.addSeparator();

	        final JMenuItem loadParameterAction = new JMenuItem("Load Parameter");
	        loadParameterAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
                    //loadFuzzyImage();
                    
					JFileChooser fc = new JFileChooser();
					
					// load the directory from ImageJ preference list
					String dir = Prefs.get( PREFS_SAVE_ROI_DIRECTORY, null );
					if ( dir != null )
					{
						fc.setCurrentDirectory( new File( dir ) );
					}

					// Only allow for single file selection.
					fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
					fc.setMultiSelectionEnabled( true );
					
					FileNameFilter CSVfilter = new FileNameFilter("csv", "Comma Separated Values (*.csv)");
                    fc.addChoosableFileFilter(CSVfilter);
                    //FileNameFilter TXTfilter = new FileNameFilter("txt", "Text (Tab delimited)(*.txt)");
					//fc.addChoosableFileFilter(TXTfilter);
					//FileNameFilter XLSfilter = new FileNameFilter("xls", "Microsoft Excel Workbook (Tab delimited)(*.xls)");
					//fc.addChoosableFileFilter(XLSfilter);                                        
					
					// Show the file chooser dialog and update to the text field if any file has
					// been selected.
					if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION )
					{
						// save the directory to ImageJ preference list
						Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getCurrentDirectory().getAbsolutePath() );

						Gebiss_.this.outline( fc.getSelectedFile().getAbsolutePath() );
					}
	            }
	        });
	        fileMenu.add(loadParameterAction);

	        final JMenuItem saveParameterAction = new JMenuItem("Save Parameter");
	        saveParameterAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					frame.saveParameter();
	            }
	        });
	        fileMenu.add(saveParameterAction);

	        fileMenu.addSeparator();

	        final JMenuItem loadBinRoiAction = new JMenuItem("Load ROI (Bin)");
	        loadBinRoiAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					JFileChooser fc = new JFileChooser();
					
					// load the directory from ImageJ preference list
					String dir = Prefs.get( PREFS_SAVE_ROI_DIRECTORY, null );
					if ( dir != null )
					{
						fc.setCurrentDirectory( new File( dir ) );
					}
					
					fc.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
					fc.setMultiSelectionEnabled( true );
					
					FileNameFilter ROIfilter = new FileNameFilter("roi", "Binary values (*.roi)");
                    fc.addChoosableFileFilter(ROIfilter);
                    FileNameFilter ZIPfilter = new FileNameFilter("zip", "Contains ROIs (*.zip)");
					fc.addChoosableFileFilter(ZIPfilter);
					
					// Show the file chooser dialog and update to the text field if any file has
					// been selected.
					if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION )
					{
						// save the directory to ImageJ preference list
						Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getCurrentDirectory().getAbsolutePath() );
						
						if ( RoiManager.getInstance() != null )
						{
							RoiManager.getInstance().close();
							new RoiManager();
						}
						
						WindowManager.getCurrentImage().killRoi();
						File files[] = fc.getSelectedFiles();
						if (files.length == 1 && files[0].isDirectory()) frame.loadNewFiles(files[0]);
						else frame.loadOldFiles(files);
					}
	            }
	        });
	        fileMenu.add(loadBinRoiAction);

	        final JMenuItem saveBinRoiAction = new JMenuItem("Save ROI (Bin)");
	        saveBinRoiAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					// do nothing if no ROI
					if ( frame.table.getRowCount() == 0 )
					{
						IJ.showMessage( "No ROI to save!" );
						return;
					}
					
					JFileChooser fc = new JFileChooser();

					// load the directory from ImageJ preference list
					String dir = Prefs.get( PREFS_SAVE_ROI_DIRECTORY, null );
					if ( dir != null )
					{
						fc.setCurrentDirectory( new File( dir ) );
					}
					
					// since the table is in single row selection mode, if no row
					// row is selected, assume is to save all
					if ( frame.table.getSelectedRowCount() == 1 )
					{
						Roi roi = ( ( ResultTableModel )frame.table.getModel() ).getRow( frame.table.getSelectedRow() ).getRoi();
						fc.setDialogTitle( "Save Single ROI (Select File)" );
						fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
						fc.setMultiSelectionEnabled( false );
						fc.setSelectedFile( new File( fc.getCurrentDirectory() + File.separator + roi.getName() + ".roi" ) );
						
						if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION )
						{
							// Shows a overwrite dialog box to prompt for overwriting if file exist.
				    		if( frame.saveDialog(fc.getSelectedFile()) == 1)
				    			return;

				    		// save the directory to ImageJ preference list
							Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getCurrentDirectory().getAbsolutePath() );

							RoiEncoder re = new RoiEncoder( fc.getSelectedFile().getAbsolutePath() );
							try
							{
								re.write( roi );
							}
							catch ( IOException ioe )
							{
								IJ.error( "ROI Manager", "Error saving ROI file " + fc.getSelectedFile().getAbsolutePath() + File.separator + roi.getName() + ".roi\n" + ioe.getMessage() );
							}
						}
					}
					else
					{
						ResultTableModel model = ( ResultTableModel )frame.table.getModel();
						fc.setDialogTitle( "Save All ROIs (Select Directory)" );
						fc.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
						fc.setMultiSelectionEnabled( false );
						
						if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION )
						{
							// save the directory to ImageJ preference list
							Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getSelectedFile().getAbsolutePath() );

							if ( fc.getSelectedFile().exists() )
							{
								for ( int z = 0; z < frame.roisPerSlice.length; z++ )
								{
									if (frame.roisPerSlice[z] == 0) continue;
										
									try {
										String outputFile = fc.getSelectedFile().getAbsolutePath() + File.separator + "RoiSet_z" + (z+1) + ".zip";
									    FileOutputStream fout = new FileOutputStream(outputFile);
									    ZipOutputStream zout = new ZipOutputStream(fout);
										for ( int i = 0; i < frame.roisPerSlice[z]; i++ )
										{
											Roi roi = createROI( frame.x[z][i], frame.y[z][i], (int) frame.min[z][i], (int) frame.max[z][i] );
											roi.setName( frame.roiNF.format( i+1 ) );
											String filename = fc.getSelectedFile().getAbsolutePath() + File.separator + "RoiSet_z" + (z+1) + "_" + roi.getName() + ".roi";
											String shortFilename = "RoiSet_z" + (z+1) + "_" + roi.getName() + ".roi";
											
											// Shows a overwrite dialog box to prompt for overwriting if file exist.
											if( frame.saveDialog( new File( filename ) ) == 1)
												continue;
											
											RoiEncoder re = new RoiEncoder( filename );
											
											try
											{
												re.write( roi );
												ZipEntry ze = new ZipEntry(shortFilename);
											    FileInputStream fin = new FileInputStream(filename);
											    try {
											      zout.putNextEntry(ze);
											      for (int c = fin.read(); c != -1; c = fin.read()) {
											        zout.write(c);
											      }
											    } finally {
											      fin.close();
											    }
											    File f = new File(filename);
											    f.delete();
											}
											catch ( IOException ioe )
											{
												IJ.error( "ROI Manager", "Error saving ROI file " + filename );
											}
										}
									    zout.close();
									}
									catch ( IOException ioe )
									{
									}
								}
							}
						}
					}
	            }
	        });
	        fileMenu.add(saveBinRoiAction);

	        final JMenuItem saveRoiTextAction = new JMenuItem("Save ROI (Text)");
	        saveRoiTextAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					// do nothing if no ROI
					if ( frame.table.getRowCount() == 0 )
					{
						IJ.showMessage( "No ROI to save!" );
						return;
					}
					
					ListSelectionModel lsm = frame.table.getSelectionModel();
					JFileChooser fc = new JFileChooser();
					
					// load the directory from ImageJ preference list
					String dir = Prefs.get( PREFS_SAVE_ROI_DIRECTORY, null );
					if ( dir != null )
					{
						fc.setCurrentDirectory( new File( dir ) );
					}
					
					ResultTableModel model = ( ResultTableModel )frame.table.getModel();
					FileNameFilter filter = new FileNameFilter("txt", "Text (Tab delimited)(*.txt)");
					fc.addChoosableFileFilter(filter);
					
					//if isSelectionEmpty false, means user selected a roll, therefor saving a single ROI.
					if ( !lsm.isSelectionEmpty() )
					{
						Roi roi = model.getRow( lsm.getMinSelectionIndex() ).getRoi();
						fc.setDialogTitle("Save Single ROI (Select File)");
						fc.setSelectedFile(new File(IJ.getImage().getShortTitle().toUpperCase() + "_ROI" + roi.getName() + ".txt"));
					    if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION ) {
					    	try{
								// Shows a overwrite dialog box to prompt for overwriting if file exist.
					    		if( frame.saveDialog(fc.getSelectedFile()) == 1)
					    			return;

					    		// save the directory to ImageJ preference list
					    		Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getCurrentDirectory().getAbsolutePath() );
					    		
					    		FileWriter fw = new FileWriter(fc.getSelectedFile());
								fw.write( String.valueOf( roi.getPolygon().npoints ) );							
								for ( int i = 0; i < roi.getPolygon().npoints; i++ )
								{
									fw.write( "\n" + roi.getPolygon().xpoints[i] + TAB_DELIM + roi.getPolygon().ypoints[i] );
								}
								fw.close();
					    	}
					    	catch(IOException ioe) {
					    		ioe.printStackTrace();
					    	}
					    }
					}//else will need to save all the ROIs.
					else {
						fc.setDialogTitle("Save All ROIs (Select File)");
						fc.setSelectedFile(new File(IJ.getImage().getShortTitle().toUpperCase() + "_AllROIs.txt"));
					    if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION ) {
			                try{
								// Shows a overwrite dialog box to prompt for overwriting if file exist.
					    		if( frame.saveDialog(fc.getSelectedFile()) == 1)
					    			return;
					    		
								// save the directory to ImageJ preference list
								Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getSelectedFile().getAbsolutePath() );

						    	FileWriter fw = new FileWriter(fc.getSelectedFile());
								for ( int i = 0; i < model.getRowCount(); i++ )
								{
									Roi roi = model.getRow( i ).getRoi();
									fw.write( String.valueOf( roi.getName() ) );
									if ( i + 1 < model.getRowCount() )
									{
										fw.write( TAB_DELIM );
										fw.write( TAB_DELIM );
									}
								}
								fw.write( "\n" );
								// this max npoint is to trace the maximum npoint from all the roi,
								// this is used to predetermined how many rows to be appended to the text
								int maxNPoints = 0;
								for ( int i = 0; i < model.getRowCount(); i++ )
								{
									Roi roi = model.getRow( i ).getRoi();
									maxNPoints = Math.max( maxNPoints, roi.getPolygon().npoints );
									fw.write( String.valueOf( roi.getPolygon().npoints ) );
									if ( i + 1 < model.getRowCount() )
									{
										fw.write( TAB_DELIM );
										fw.write( TAB_DELIM );
									}
								}
								for ( int i = 0; i < maxNPoints; i++ )
								{
									fw.write( "\n" );
									for ( int j = 0; j < model.getRowCount(); j++ )
									{
										Roi roi = model.getRow( j ).getRoi();
										if ( i < roi.getPolygon().xpoints.length )
										{
											fw.write( roi.getPolygon().xpoints[i] + TAB_DELIM + roi.getPolygon().ypoints[i] );
										}
										else
										{
											fw.write( TAB_DELIM );
										}
										if ( j + 1 < model.getRowCount() )
										{
											fw.write( TAB_DELIM );
										}
									}
								}
								fw.close();
			                }
			                catch (IOException ioe) {
			                	ioe.printStackTrace();
			                }
					    }
					}
	            }
	        });
	        fileMenu.add(saveRoiTextAction);

	        fileMenu.addSeparator();

	        final JMenuItem printRoiAction = new JMenuItem("Show current ROI vertex coords");
	        printRoiAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					ListSelectionModel lsm = frame.table.getSelectionModel();
					
					if ( lsm.isSelectionEmpty() )
					{
						IJ.error( "No row selected!" );
						return;
					}
					
					ResultTableModel model = ( ResultTableModel )frame.table.getModel();
					Roi roi = model.getRow( lsm.getMinSelectionIndex() ).getRoi();
					final JDialog dialog = new JDialog();
					JTextArea textArea = new JTextArea( 20, 20 );
					JScrollPane scrollPane = new JScrollPane( textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
					JButton closeButton = new JButton( "Close" );
					JPanel mainPanel = new JPanel();
					JPanel subPanel = new JPanel();
					
					closeButton.addActionListener( new ActionListener()
					{
						public void actionPerformed( ActionEvent ae )
						{
							dialog.dispose();
						}
					});
					
					dialog.setTitle( roi.getName() );
					dialog.getContentPane().setLayout( new FlowLayout() );
					mainPanel.setLayout( new BorderLayout() );
					//textArea.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
					
					subPanel.add( closeButton );
					
					mainPanel.add( scrollPane, BorderLayout.CENTER );
					mainPanel.add( subPanel, BorderLayout.SOUTH );
					
					dialog.getContentPane().add( mainPanel );
					
					//System.out.println( roi.getPolygon().npoints );
					textArea.append( String.valueOf( roi.getPolygon().npoints ) );
					
					for ( int i = 0; i < roi.getPolygon().npoints; i++ )
					{
						//System.out.println( roi.getPolygon().xpoints[i] + "," + roi.getPolygon().ypoints[i] );
						textArea.append( "\n" + roi.getPolygon().xpoints[i] + TAB_DELIM + roi.getPolygon().ypoints[i] );
					}
					
					textArea.setCaretPosition( 0 );
					
					dialog.pack();
					dialog.setVisible( true );
	            }
	        });
	        fileMenu.add(printRoiAction);

	        final JMenuItem printAllRoiAction = new JMenuItem("Show All ROI vertex coords on Current Slice");
	        printAllRoiAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					ResultTableModel model = ( ResultTableModel )frame.table.getModel();
					final JDialog dialog = new JDialog();
					JTextArea textArea = new JTextArea( 20, 80 );
					JScrollPane scrollPane = new JScrollPane( textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
					JButton closeButton = new JButton( "Close" );
					JPanel mainPanel = new JPanel();
					JPanel subPanel = new JPanel();
					
					closeButton.addActionListener( new ActionListener()
					{
						public void actionPerformed( ActionEvent ae )
						{
							dialog.dispose();
						}
					});
					
					dialog.setTitle( "All ROIs" );
					dialog.getContentPane().setLayout( new FlowLayout() );
					mainPanel.setLayout( new BorderLayout() );
					//textArea.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
					
					subPanel.add( closeButton );
					
					mainPanel.add( scrollPane, BorderLayout.CENTER );
					mainPanel.add( subPanel, BorderLayout.SOUTH );
					
					dialog.getContentPane().add( mainPanel );
					
					for ( int i = 0; i < model.getRowCount(); i++ )
					{
						Roi roi = model.getRow( i ).getRoi();
						textArea.append( String.valueOf( roi.getName() ) );
						if ( i + 1 < model.getRowCount() )
						{
							textArea.append( TAB_DELIM );
							textArea.append( TAB_DELIM );
						}
					}
					textArea.append( "\n" );
					// this max npoint is to trace the maximum npoint from all the roi,
					// this is used to predetermined how many rows to be appended to the text
					int maxNPoints = 0;
					for ( int i = 0; i < model.getRowCount(); i++ )
					{
						Roi roi = model.getRow( i ).getRoi();
						maxNPoints = Math.max( maxNPoints, roi.getPolygon().npoints );
						textArea.append( String.valueOf( roi.getPolygon().npoints ) );
						if ( i + 1 < model.getRowCount() )
						{
							textArea.append( TAB_DELIM );
							textArea.append( TAB_DELIM );
						}
					}
					for ( int i = 0; i < maxNPoints; i++ )
					{
						textArea.append( "\n" );
						for ( int j = 0; j < model.getRowCount(); j++ )
						{
							Roi roi = model.getRow( j ).getRoi();
							if ( i < roi.getPolygon().xpoints.length )
							{
								textArea.append( roi.getPolygon().xpoints[i] + TAB_DELIM + roi.getPolygon().ypoints[i] );
							}
							else
							{
								textArea.append( TAB_DELIM );
							}
							if ( j + 1 < model.getRowCount() )
							{
								textArea.append( TAB_DELIM );
							}
						}
					}
					
					textArea.setCaretPosition( 0 );
					
					dialog.pack();
					dialog.setVisible( true );
	            }
	        });
	        fileMenu.add(printAllRoiAction);

	        fileMenu.addSeparator();

	        final JMenuItem exitAction = new JMenuItem("Exit");
	        exitAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
	            	doExit();
					//frame.saveParameter();
					frameTop.setVisible( false );
					frameTop.dispose();
					frameTop = null;
	            }
	        });
	        fileMenu.add(exitAction);

	        final JCheckBoxMenuItem medianFilterAction = new JCheckBoxMenuItem("Use 3px Median Filter");
	        medianFilterAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
	            	useMedianFilter = medianFilterAction.getState();
	            	Prefs.set( PREFS_USE_MEDIAN_FILTER_FLAG, useMedianFilter );
	            }
	        });
	        medianFilterAction.setSelected(Boolean.parseBoolean(Prefs.get( PREFS_USE_MEDIAN_FILTER_FLAG, null )));
	        processMenu.add(medianFilterAction);
	        
	        final JMenuItem maskImageAction = new JMenuItem("Create Binary Mask");
	        maskImageAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
					Color fc = Toolbar.getForegroundColor();
					Toolbar.setForegroundColor(Color.BLACK);
					
					int currentSlice = frame.roiTableBar.getValue();
					
					for (int i = 0; i < imp.getImageStackSize(); i++)
					{
						frame.roiTableBar.setValue(i);
						frame.updateROIManager();
						ImagePlus newImage = NewImage.createByteImage( "ROI Mask", imp.getWidth(), imp.getHeight(), 1, NewImage.FILL_WHITE );
						newImage.show();
						if ( RoiManager.getInstance() != null )
						{
							//RoiManager.getInstance().runCommand( "fill" );
							ImagePlus imp = WindowManager.getCurrentImage();
							ImageProcessor ip = imp.getProcessor();
							for (int j = 0; j < frame.roisPerSlice[i]; j++) {
								int x = frame.x[i][j];
								int y = frame.y[i][j];
								int min = (int) Math.round(frame.min[i][j]);
								int max = (int) Math.round(frame.max[i][j]);
									
								Roi roi = createFuzzyROI( x, y, min, max, Color.RED );
								ip.fillPolygon(roi.getPolygon());
							}
						}
					}
					
					if (imp.getImageStackSize() > 1) IJ.run("Images to Stack");
					//WindowManager.getCurrentWindow().setTitle("ROI Mask");
					Toolbar.setForegroundColor(fc);
					
					frame.roiTableBar.setValue(currentSlice);
					IJ.selectWindow("Stack");
					IJ.getImage().setSlice(currentSlice + 1);
	            }
	        });
	        processMenu.add(maskImageAction);

	        final JMenuItem labelingAction = new JMenuItem("Labeling");
	        labelingAction.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
                	// activate ObjectCounter3D.java
					ObjectCounter3D labeling = new ObjectCounter3D();                                        
					labeling.setupGUI("");
					labeling.run();
	            }
	        });
	        processMenu.add(labelingAction);
		}
		
	    public SegmentingAssistantFrameTop() {
	        
	        setTitle("Groundtruth Editing & Benchmarking for Image Stack Segmentation"); // Gebiss // Ground Truth Creation and Benchmark
	        setMenuBar(0);
	        jtp = new JTabbedPane();
	        getContentPane().add(jtp);
	        if (frame == null) frame = new SegmentingAssistantFrame();
	        if (frameObjectGT == null) frameObjectGT = new ObjectGTFrame();
	        if (frameObjectSubset == null) frameObjectSubset = new ObjectSubsetFrame();
	        if (frameBench == null) frameBench = new BenchFrame();
	        jtp.addTab("Slice-based GT Specification", frame); //"Segmenting Assistant"
	        jtp.addTab("Object-based GT Specification", frameObjectGT);
	        jtp.addTab("Object Subset GT Specification", frameObjectSubset);
	        jtp.addTab("Benchmark", frameBench);
	        jtp.addChangeListener(new ChangeListener() {
	            // This method is called whenever the selected tab changes
	            public void stateChanged(ChangeEvent evt) {
	                JTabbedPane pane = (JTabbedPane)evt.getSource();
	                // Get current tab
	                int sel = pane.getSelectedIndex();
	    	        setMenuBar(sel);
	            }
	        });
	        addWindowListener(this);
	        addKeyListener(this);
	        setFocusable(true);
	    }

		@Override
		public void windowActivated(WindowEvent e) {}
		@Override
		public void windowClosed(WindowEvent e) {}
		@Override
		public void windowClosing(WindowEvent e) {
			doExit();
		}
		@Override
		public void windowDeactivated(WindowEvent e) {}
		@Override
		public void windowDeiconified(WindowEvent e) {}
		@Override
		public void windowIconified(WindowEvent e) {}
		@Override
		public void windowOpened(WindowEvent e) {}

		@Override
		public void keyPressed(KeyEvent e) {
		     String keyname= e.getKeyText(e.getKeyCode());
		     System.out.println(keyname);
			if (lastKeyName.equals("Ctrl") && keyname.equals("Y")) {
				IJ.run("Image Calculator...");
			}
			lastKeyName = keyname;

			try {
                int sel = jtp.getSelectedIndex();
   		     	if (sel != 1) return;
   		     	if (keyname.startsWith("NumPad-")) keyname = keyname.substring(7);
				int i = Integer.parseInt( keyname );
				int r = frameObjectGT.table.getSelectedRow();
				frameObjectGT.table.setValueAt(keyname, r, 6);
			}
			catch ( NumberFormatException nfe )
			{
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	}
	
	public class BenchFrame extends JPanel
	{
		private JLabel gtLabel;
		private JLabel msLabel;
		private JTextField fprLabel;
		private JTextField fnrLabel;
		private JTextField fmLabel;
		private JTable table;
		
	    public BenchFrame() 
	    {
	        setLayout(new BorderLayout());
	        
	        JPanel buttonPanel = new JPanel();
	        buttonPanel.setLayout(new GridLayout(0, 5));
			final JButton runButton = new JButton( "Run" );
			runButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent ae )
				{
					(new Thread(new Runnable() {
						public void run() {
							runButton.setEnabled(false);
							bench b = new bench(Gebiss_.this, Gebiss_.frameBench);                                        
							b.setup("", Gebiss_.this.imp);
							b.run( ip );
							runButton.setEnabled(true);
						}
					})).start();
				} 
			});
			buttonPanel.add(runButton);
			
	        JPanel labelPanel = new JPanel();
	        labelPanel.setLayout(new GridLayout(5, 1));
	        
	        gtLabel = new JLabel();
	        labelPanel.add(gtLabel);

	        msLabel = new JLabel();
	        labelPanel.add(msLabel);
	        
	        fprLabel = new JTextField();
	        fprLabel.setEditable(false);
	        fprLabel.setBorder(null);
	        labelPanel.add(fprLabel);
	        
	        fnrLabel = new JTextField();
	        fnrLabel.setEditable(false);
	        fnrLabel.setBorder(null);
	        labelPanel.add(fnrLabel);
	        
	        fmLabel = new JTextField();
	        fmLabel.setEditable(false);
	        fmLabel.setBorder(null);
	        labelPanel.add(fmLabel);
	        
			// Table panel
			JPanel tablePanel = new JPanel();
		    table = new JTable(new DefaultTableModel());			
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setRowSelectionAllowed(true);
			table.setColumnSelectionAllowed(true);

			JScrollPane scrollPane = new JScrollPane( table , JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);			
			scrollPane.setPreferredSize(new Dimension(500,500));
			tablePanel.add( scrollPane );	
			
	        add(buttonPanel, BorderLayout.NORTH);
	        add(labelPanel, BorderLayout.CENTER);
	        add(tablePanel, BorderLayout.SOUTH);	        			
	    }

		public JLabel getGtLabel() {
			return gtLabel;
		}

		public JLabel getMsLabel() {
			return msLabel;
		}

		public JTextField getFprLabel() {
			return fprLabel;
		}

		public JTextField getFnrLabel() {
			return fnrLabel;
		}

		public JTextField getFmLabel() {
			return fmLabel;
		}

		public JTable getTable() {
			return table;
		}		
	}
	
	/**
	 * 
	 */
	private class SegmentingAssistantFrame extends JPanel
	{
		private final String HIDE_ALL_ROIS_TEXT = "Hide All ROIs";
		private final String SHOW_ALL_ROIS_TEXT = "Show All ROIs";
		private final JButton showAllROIsButton;
		
		private JTextField xTextField;
		private JTextField yTextField;
		private JTextField minTextField;
		private JTextField maxTextField;
		private JSlider xSlider;
		private JSlider ySlider;
		private JSlider minLevelSlider;
		private JSlider maxLevelSlider;
		private JTable table;
		private NumberFormat roiNF = NumberFormat.getInstance();
		private JPanel rightPanel;
		
		private int[][] x;
		private int[][] y;
		private double[][] min;
		private double[][] max;
		private int[] roisPerSlice;
		private int curRoiTableBarValue;
		private JScrollBar roiTableBar;
		
		private void saveParameter() {
			JFileChooser fc = new JFileChooser();
			String outputFormatter = "";
			
			// load the directory from ImageJ preference list
			String dir = Prefs.get( PREFS_SAVE_ROI_DIRECTORY, null );
			if ( dir != null )
			{
				fc.setCurrentDirectory( new File( dir ) );
			}
			
			ResultTableModel model = ( ResultTableModel )table.getModel();
			FileNameFilter CSVfilter = new FileNameFilter("csv", "Comma Separated Values (*.csv)");
                                fc.addChoosableFileFilter(CSVfilter);
                                //FileNameFilter TXTfilter = new FileNameFilter("txt", "Text (Tab delimited)(*.txt)");
			//fc.addChoosableFileFilter(TXTfilter);
			//FileNameFilter XLSfilter = new FileNameFilter("xls", "Microsoft Excel Workbook (Tab delimited)(*.xls)");
			//fc.addChoosableFileFilter(XLSfilter);                                        
			fc.setDialogTitle("Save Parameter (Select File)");
			fc.setSelectedFile(new File(IJ.getImage().getShortTitle().toUpperCase() + "_Parameters.xls"));                                        
			fc.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent e) {
					if(JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equalsIgnoreCase(e.getPropertyName())) {
						if((e.getNewValue().getClass()).getSimpleName().equalsIgnoreCase("FileNameFilter")) {
							String ext = ((FileNameFilter)e.getNewValue()).getExtension();
							JFileChooser fc = (JFileChooser)e.getSource();
							fc.setSelectedFile(new File(IJ.getImage().getShortTitle().toUpperCase() + "_Parameters." + ext));
						}
					}
				}
			});	
			if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION ) {
	            try{
					// Shows a overwrite dialog box to prompt for overwriting if file exist.
			    	if( saveDialog(fc.getSelectedFile()) == 1)
			    		return;					    		
					// save the directory to ImageJ preference list
					Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getSelectedFile().getAbsolutePath() );

				    FileWriter fw = new FileWriter(fc.getSelectedFile());
				    outputFormatter = "\tMin\tMax\tBX\tBY\tz\n"; // "\tMin\tMax\tBX\tBY\n"
				    /*
					    for(int i=0; i<model.getRowCount(); i++) {
				    	outputFormatter += (i+1) + "\t";
				    	outputFormatter += Math.round(model.getRow(i).getMin()) + "\t";
				    	outputFormatter += Math.round(model.getRow(i).getMax()) + "\t";
				    	outputFormatter += Math.round(model.getRow(i).getX()) + "\t";
				    	outputFormatter += Math.round(model.getRow(i).getY()) + "\t";
				    	outputFormatter += (curRoiTableBarValue+1) + "\n";
				    }
				    */
				    int count = 1;
					    for(int z=0; z<roisPerSlice.length; z++) {
						    for(int i=0; i<roisPerSlice[z]; i++) {
					    	outputFormatter += count + "\t";
					    	outputFormatter += Math.round(min[z][i]) + "\t";
					    	outputFormatter += Math.round(max[z][i]) + "\t";
					    	outputFormatter += Math.round(x[z][i]) + "\t";
					    	outputFormatter += Math.round(y[z][i]) + "\t";
					    	outputFormatter += (z+1) + "\n";
					    	count++;
						    }
				    }
				    fw.write(outputFormatter);
					fw.close();
	               }
	            catch (IOException ioe) {
	                ioe.printStackTrace();
	            }
			}					
		}
		
	    private void drawROI() {
			drawFuzzyRoi(Color.RED);
			//if (impFuzzy == null) updateROI( xSlider.getValue(), ySlider.getValue(),
					//minLevelSlider.getValue(), maxLevelSlider.getValue() );
	    }

	    private void loadRoi(RoiDecoder rd, String filename, boolean firstTime) throws IOException {
			Roi roi = null;
			
			try
			{
				roi = rd.getRoi();
			}
			catch ( NullPointerException npe )
			{
				npe.printStackTrace();
				IJ.error( "Unable to load ROI " + filename + '!' );
				return;
			}
			
			int x = 0;
			int y = 0;
			int min = 0;
			//int max = maxLevelSlider.getMaximum();
			int max = 0;
			
			if ( roi.getPolygon().npoints > 0 )
			{
				x = roi.getPolygon().xpoints[0];
				y = roi.getPolygon().ypoints[0];
				if ( imp.getType() == ImagePlus.GRAY32 ){
					min = ( int )Float.intBitsToFloat( imp.getProcessor().getPixel( roi.getPolygon().xpoints[0], roi.getPolygon().ypoints[0] ) );
					max = ( int )Float.intBitsToFloat( imp.getProcessor().getPixel( roi.getPolygon().xpoints[0], roi.getPolygon().ypoints[0] ) );
				}else {
					min = imp.getProcessor().getPixel(roi.getPolygon().xpoints[0], roi.getPolygon().ypoints[0]);
					max = imp.getProcessor().getPixel(roi.getPolygon().xpoints[0], roi.getPolygon().ypoints[0]);
				}
			}
			for ( int j = 1; j < roi.getPolygon().npoints; j++ )
			{
				x = Math.min( x, roi.getPolygon().xpoints[j] );
				y = Math.min( y, roi.getPolygon().ypoints[j] );
				if ( imp.getType() == ImagePlus.GRAY32 )
				{
					//min += ( int )Float.intBitsToFloat( imp.getProcessor().getPixel( roi.getPolygon().xpoints[j], roi.getPolygon().ypoints[j] ) );
					min = Math.min(min, ( int )Float.intBitsToFloat( imp.getProcessor().getPixel( roi.getPolygon().xpoints[j], roi.getPolygon().ypoints[j] ) ));
					max = Math.max(max, ( int )Float.intBitsToFloat( imp.getProcessor().getPixel( roi.getPolygon().xpoints[j], roi.getPolygon().ypoints[j] ) ));
				}
				else
				{
					//min += imp.getProcessor().getPixel( roi.getPolygon().xpoints[j], roi.getPolygon().ypoints[j] );
					min = Math.min(min, imp.getProcessor().getPixel(roi.getPolygon().xpoints[j], roi.getPolygon().ypoints[j]));
					max = Math.max(max, imp.getProcessor().getPixel(roi.getPolygon().xpoints[j], roi.getPolygon().ypoints[j]));
				}
			}
			
			if ( roi.getPolygon().npoints > 0 )
			{
				//min = min/roi.getPolygon().npoints;
			}
			
			try {
				int startIndex = filename.lastIndexOf("_z") + 2;
				int lastIndex = filename.lastIndexOf("_");
				curRoiTableBarValue = Integer.parseInt(filename.substring(startIndex, lastIndex)) - 1;
				roiTableBar.setValue(curRoiTableBarValue);
			} catch (Exception e) {}
				
			canUpdateROI = false;
			xSlider.setValue( x );
			ySlider.setValue( y );
			minLevelSlider.setValue( min );
			maxLevelSlider.setValue( max );
			canUpdateROI = true;
			RowData rowData = new RowData( roi, min, max, x, y );
			roi.setName( frame.roiNF.format( frame.table.getRowCount() + 1 ) );
			( ( ResultTableModel )frame.table.getModel() ).addRow( rowData );
			updateROI( roi );
			
			if ( RoiManager.getInstance() == null )
			{
				new RoiManager();
			}
			RoiManager.getInstance().runCommand( "add" );

			ResultTableModel model = (ResultTableModel) table.getModel();
			roisPerSlice[curRoiTableBarValue] = table.getModel().getRowCount();
			
			if (firstTime) {
		    	frame.x[curRoiTableBarValue] = new int[table.getModel().getRowCount()];
		    	frame.y[curRoiTableBarValue] = new int[table.getModel().getRowCount()];
		    	frame.min[curRoiTableBarValue] = new double[table.getModel().getRowCount()];
		    	frame.max[curRoiTableBarValue] = new double[table.getModel().getRowCount()];
			} else {
		    	frame.x[curRoiTableBarValue] = new int[frame.x.length+1];
		    	frame.y[curRoiTableBarValue] = new int[frame.y.length+1];
		    	frame.min[curRoiTableBarValue] = new double[frame.min.length+1];
		    	frame.max[curRoiTableBarValue] = new double[frame.max.length+1];
			}
			for (int i = 0; i < table.getModel().getRowCount(); i++) {
				frame.x[curRoiTableBarValue][i] = (int) Math.round(model.getRow(i).getX());
				frame.y[curRoiTableBarValue][i] = (int) Math.round(model.getRow(i).getY());
				frame.min[curRoiTableBarValue][i] = model.getRow(i).getMin();
				frame.max[curRoiTableBarValue][i] = model.getRow(i).getMax();
			}
	    }

	    private void loadOldFiles(File files[]) {
			( ( ResultTableModel )frame.table.getModel() ).clear();
			boolean firstTime = true;
			for ( int i = 0; i < files.length; i++ )
			{
				RoiDecoder rd = new RoiDecoder( files[i].getAbsolutePath() );
				try
				{
					loadRoi(rd, files[i].getAbsolutePath(), firstTime);
					firstTime = false;
				}
				catch ( IOException ioe )
				{
					IJ.error( "Failed to load ROI for " + files[i].getAbsolutePath() + '!' );
					ioe.printStackTrace();
				}
			}
	    }

	    private void loadNewFiles(File dir) {
	    	File[] files = dir.listFiles();
			for ( int z = 0; z < files.length; z++ )
			{
				if (!files[z].getName().endsWith("zip")) continue;
				try
				{
					( ( ResultTableModel )frame.table.getModel() ).clear();
					FileInputStream fin = new FileInputStream(files[z]);
					ZipInputStream zin = new ZipInputStream(fin);
				    ZipEntry ze = null;
				    boolean firstTime = true;
				    while ((ze = zin.getNextEntry()) != null) {
				        FileOutputStream fout = new FileOutputStream(dir + File.separator + ze.getName());
				        for (int c = zin.read(); c != -1; c = zin.read()) {
				        	fout.write(c);
				        }
				        zin.closeEntry();
				        fout.close();

				        String filename = dir + File.separator + ze.getName();
						RoiDecoder rd = new RoiDecoder(filename);
						loadRoi(rd, filename, firstTime);
						firstTime = false;
						File f = new File(filename);
						f.delete();
				    }
				    zin.close();
				}
				catch ( IOException ioe )
				{
					IJ.error( "Failed to load ROI for " + files[z].getAbsolutePath() + '!' );
				}
			}
	    }

	    public SegmentingAssistantFrame()
		{
			roisPerSlice = new int[imp.getImageStackSize()];
			curRoiTableBarValue = WindowManager.getCurrentImage().getCurrentSlice() - 1;
	    	x = new int[imp.getImageStackSize()][];
	    	y = new int[imp.getImageStackSize()][];
	    	min = new double[imp.getImageStackSize()][];
	    	max = new double[imp.getImageStackSize()][];

			//setTitle( "Segmenting Assistant" );

			roiNF.setMinimumIntegerDigits( 4 );
			roiNF.setGroupingUsed( false );

			// Create a label for the x position slider:
			JLabel xLabel = new JLabel( "StartX:", JLabel.LEFT ); //"Horizontal Centroid::"
			xTextField = new JTextField( 4 );
			// Create the x position slider:
			xSlider = new JSlider( JSlider.HORIZONTAL );
			xSlider.setPaintTicks( true );
			xSlider.setPaintLabels( true );
			xSlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );
			xTextField.addKeyListener( new TextKeyListener( xSlider ) );
			xSlider.addChangeListener( new ChangeListener()
			{
				public void stateChanged( ChangeEvent e )
				{
					if ( xTextField.getText().length() == 0
							|| Integer.parseInt( xTextField.getText() ) != xSlider
							.getValue() )
					{
						xTextField.setText( String.valueOf( xSlider.getValue() ) );
					}
					if ( !canUpdateROI )
					{
						return;
					}
					drawROI();
				}
			});
			
			// Create a label for the y position slider:
			JLabel yLabel = new JLabel( "StartY: ", JLabel.LEFT ); //"Vertical Centroid:: "
			yTextField = new JTextField( 4 );
			// Create the y position slider:
			ySlider = new JSlider( JSlider.HORIZONTAL );
			ySlider.setPaintTicks( true );
			ySlider.setPaintLabels( true );
			ySlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );
			yTextField.addKeyListener( new TextKeyListener( ySlider ) );
			ySlider.addChangeListener( new ChangeListener()
			{
				public void stateChanged( ChangeEvent e )
				{
					if ( yTextField.getText().length() == 0
							|| Integer.parseInt( yTextField.getText() ) != ySlider
							.getValue() )
					{
						yTextField.setText( String.valueOf( ySlider.getValue() ) );
					}
					if ( !canUpdateROI )
					{
						return;
					}
					drawROI();
				}
			});
			
			// Create a label for the min level slider:
			JLabel minLevelLabel = new JLabel( "Min Level: ", JLabel.LEFT );
			minTextField = new JTextField( 4 );
			
			// Create a label for the max level slider:
			JLabel maxLevelLabel = new JLabel( "Max Level: ", JLabel.LEFT );
			maxTextField = new JTextField( 4 );

			setSliders();

			// add mouse listener to image window, if not added before
			MouseListener ml[] = WindowManager.getCurrentWindow().getCanvas().getMouseListeners();
			boolean hasMouseListener = false;
			for ( int i = 0; i < ml.length; i++ )
			{
				if ( ml[i] instanceof ImageWindowMouseListener )
				{
					hasMouseListener = true;
					break;
				}
			}
			if ( !hasMouseListener )
			{
				WindowManager.getCurrentWindow().getCanvas().addMouseListener( new ImageWindowMouseListener() );
			}
			// add mouse wheel listener to image window, if not added before
			MouseWheelListener mwl[] = WindowManager.getCurrentWindow().getCanvas().getMouseWheelListeners();
			boolean hasMouseWheelListener = false;
			for ( int i = 0; i < mwl.length; i++ )
			{
				if ( mwl[i] instanceof ImageWindowWheelMouseListener )
				{
					hasMouseWheelListener = true;
					break;
				}
			}
			if ( !hasMouseWheelListener )
			{
				WindowManager.getCurrentWindow().getCanvas().addMouseWheelListener( new ImageWindowWheelMouseListener() );
			}
			for (Component comp : WindowManager.getCurrentWindow().getComponents()) {
				if (comp instanceof Scrollbar) {
					Scrollbar scb = (Scrollbar)comp;
					if(scb.getOrientation() == Scrollbar.HORIZONTAL)
					{
						AdjustmentListener als[] = scb.getAdjustmentListeners();
						boolean hasAdjustmentListener = false;
						for ( AdjustmentListener al : als )
						{
							if ( al instanceof ImageWindowAdjustmentListener )
							{
								hasAdjustmentListener = true;
								break;
							}
						}
						// don't add more than once
						if ( !hasAdjustmentListener )
						{
							scb.addAdjustmentListener(new ImageWindowAdjustmentListener());
						}
					}
				}
			}
			/* to be removed, should be no need for ROI Manager window in future
			// add listener to the ROI Manager's list
			if ( RoiManager.getInstance() != null )
			{
				RoiManager.getInstance().getList().addItemListener( new ItemListener()
				{
					public void itemStateChanged( ItemEvent ie )
					{
						if ( ie.getStateChange() == ItemEvent.SELECTED )
						{
							String selectedItem = ( ( List )ie.getSource() ).getSelectedItem();
							if ( selectedItem == null )
							{
								return;
							}
							Roi roi = ( Roi )RoiManager.getInstance().getROIs().get( selectedItem );
							int x = roi.getBounds().x + ( roi.getBounds().width / 2 );
							int y = roi.getBounds().y + ( roi.getBounds().height / 2 );
							int value = imp.getProcessor().getPixel( x, y );
							
							if ( imp.getType() == ImagePlus.GRAY32 )
							{
								value = ( int )Float.intBitsToFloat( value );
							}
							
							canUpdateROI = false;
							xSlider.setValue( x );
							ySlider.setValue( y );
							minLevelSlider.setValue( value );
							maxLevelSlider.setValue( value );
							canUpdateROI = true;
						}
					}
				});
			}
			 */
			
			// adding buttons
			final JButton addROIButton = new JButton( "Add ROI" );
			addROIButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent ae )
				{
					Roi roi = createROI( xSlider.getValue(), ySlider.getValue(), minLevelSlider.getValue(), maxLevelSlider.getValue() );
					ResultTableModel model = ( ResultTableModel )table.getModel();
					int roiNumber = 1;
					
					if ( table.getModel().getRowCount() >0 )
					{
						roiNumber = Integer.parseInt( ( ( Roi )model.getValueAt( model.getRowCount() - 1, 0 ) ).getName() ) + 1;
					}
					roi.setName( roiNF.format( roiNumber ) );
					model.addRow( new RowData( roi, minLevelSlider.getValue(), maxLevelSlider.getValue(), xSlider.getValue(), ySlider.getValue() ) );
                                        
					roisPerSlice[curRoiTableBarValue] = table.getModel().getRowCount();
			    	x[curRoiTableBarValue] = new int[table.getModel().getRowCount()];
			    	y[curRoiTableBarValue] = new int[table.getModel().getRowCount()];
			    	min[curRoiTableBarValue] = new double[table.getModel().getRowCount()];
			    	max[curRoiTableBarValue] = new double[table.getModel().getRowCount()];
					for (int i = 0; i < table.getModel().getRowCount(); i++) {
				    	x[curRoiTableBarValue][i] = (int) Math.round(model.getRow(i).getX());
				    	y[curRoiTableBarValue][i] = (int) Math.round(model.getRow(i).getY());
				    	min[curRoiTableBarValue][i] = model.getRow(i).getMin();
				    	max[curRoiTableBarValue][i] = model.getRow(i).getMax();
					}

			    	// Update the "Show All ROIs" view
			    	updateROIManager();
					ImageCanvas ic = imp.getCanvas();
					ic.setShowAllROIs( ic.getShowAllROIs() );
					imp.draw();
                                        
			    	/*
					if ( RoiManager.getInstance() != null )
					{
						ResultsTable rt = ResultsTable.getResultsTable();
						int counter = rt.getCounter();
						
						rt.incrementCounter();
						rt.setValue( "Min", counter, minLevelSlider.getValue() );
						rt.setValue( "Max", counter, maxLevelSlider.getValue() );
						rt.setValue( "BX", counter, xSlider.getValue() );
						rt.setValue( "BY", counter, ySlider.getValue() );
						//IJ.getTextPanel().setResultsTable( rt );
						IJ.getTextPanel().append( rt.getRowAsString( counter ) );
						
						RoiManager.getInstance().actionPerformed( ae );
					}
					 */
				}
			});
			
			final JButton updateROIButton = new JButton( "Update ROI" );
			updateROIButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent ae )
				{
					ListSelectionModel lsm = table.getSelectionModel();
					
					if ( lsm.isSelectionEmpty() )
					{
						IJ.error( "No row selected!" );
						return;
					}
					
					Roi roi = createROI( xSlider.getValue(), ySlider.getValue(), minLevelSlider.getValue(), maxLevelSlider.getValue() );
					ResultTableModel model = ( ResultTableModel )table.getModel();
					RowData rowData = model.getRow( lsm.getMinSelectionIndex() );
					
					roi.setName( rowData.getRoi().getName() );
					model.updateRow( lsm.getMinSelectionIndex(), new RowData( roi, minLevelSlider.getValue(), maxLevelSlider.getValue(), xSlider.getValue(), ySlider.getValue() ) );
					
					for (int i = 0; i < table.getModel().getRowCount(); i++) {
				    	x[curRoiTableBarValue][i] = (int) Math.round(model.getRow(i).getX());
				    	y[curRoiTableBarValue][i] = (int) Math.round(model.getRow(i).getY());
				    	min[curRoiTableBarValue][i] = model.getRow(i).getMin();
				    	max[curRoiTableBarValue][i] = model.getRow(i).getMax();
					}
					
					// Update the "Show All ROIs" view
					updateROIManager();
					ImageCanvas ic = imp.getCanvas();
					ic.setShowAllROIs( ic.getShowAllROIs() );
					imp.draw();
                                        
					/*
					if ( RoiManager.getInstance() != null )
					{
						int selectedIndex = RoiManager.getInstance().getList().getSelectedIndex();
						ResultsTable rt = ResultsTable.getResultsTable();
						
						if ( selectedIndex > -1 && selectedIndex < rt.getCounter() )
						{
							rt.setValue( "Min", selectedIndex, minLevelSlider.getValue() );
							rt.setValue( "Max", selectedIndex, maxLevelSlider.getValue() );
							rt.setValue( "BX", selectedIndex, xSlider.getValue() );
							rt.setValue( "BY", selectedIndex, ySlider.getValue() );
							//IJ.getTextPanel().setResultsTable( rt );
							//IJ.getTextPanel().resetSelection();
							IJ.getTextPanel().setLine( selectedIndex, rt.getRowAsString( selectedIndex ) );
							RoiManager.getInstance().actionPerformed( ae );
						}
					}
					 */
				}
			});
			
			final JButton deleteROIButton = new JButton( "Delete ROI" );
			deleteROIButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent ae )
				{
					ListSelectionModel lsm = table.getSelectionModel();
					
					if ( lsm.isSelectionEmpty() )
					{
						IJ.error( "No row selected!" );
						return;
					}
					
					Roi roi = createROI( xSlider.getValue(), ySlider.getValue(), minLevelSlider.getValue(), maxLevelSlider.getValue() );
					ResultTableModel model = ( ResultTableModel )table.getModel();
					
					model.deleteRow( lsm.getMinSelectionIndex() );
                                        
					roisPerSlice[curRoiTableBarValue] = table.getModel().getRowCount();
			    	x[curRoiTableBarValue] = new int[table.getModel().getRowCount()];
			    	y[curRoiTableBarValue] = new int[table.getModel().getRowCount()];
			    	min[curRoiTableBarValue] = new double[table.getModel().getRowCount()];
			    	max[curRoiTableBarValue] = new double[table.getModel().getRowCount()];
					for (int i = 0; i < table.getModel().getRowCount(); i++) {
				    	x[curRoiTableBarValue][i] = (int) Math.round(model.getRow(i).getX());
				    	y[curRoiTableBarValue][i] = (int) Math.round(model.getRow(i).getY());
				    	min[curRoiTableBarValue][i] = model.getRow(i).getMin();
				    	max[curRoiTableBarValue][i] = model.getRow(i).getMax();
					}

					// Update the "Show All ROIs" view
					updateROIManager();
					ImageCanvas ic = imp.getCanvas();
					ic.setShowAllROIs( ic.getShowAllROIs() );
					imp.draw();
				}
			});
			
			showAllROIsButton = new JButton( "Show All ROIs" );
			showAllROIsButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent ae )
				{
					updateROIManager();
					ImageCanvas ic = imp.getCanvas();
					ic.setShowAllROIs( !ic.getShowAllROIs() );
					imp.draw();
					if (ic.getShowAllROIs()) showAllROIsButton.setText(HIDE_ALL_ROIS_TEXT);
					if (!ic.getShowAllROIs()) showAllROIsButton.setText(SHOW_ALL_ROIS_TEXT);
					if ( table.getModel().getRowCount() < 1 ) showAllROIsButton.setText(SHOW_ALL_ROIS_TEXT);
				}
			});
			
			// Use a slightly 10 pt font for the buttons:
			Font buttonFont = addROIButton.getFont().deriveFont( ( float )10.0 );
			addROIButton.setFont( buttonFont );
			updateROIButton.setFont( buttonFont );
			deleteROIButton.setFont( buttonFont );
			showAllROIsButton.setFont( buttonFont );
			
			// Add all the UI components to the content pane:
			rightPanel = new JPanel();
			rightPanel.setLayout( new BoxLayout( rightPanel, BoxLayout.Y_AXIS ) );
			rightPanel.setBorder( BorderFactory.createEmptyBorder( 10, 0, 10, 10 ) );
			JPanel xPanel = new JPanel();
			xPanel.add( xLabel );
			xPanel.add( xTextField );
			rightPanel.add( xSlider );
			rightPanel.add( xPanel );
			JPanel yPanel = new JPanel();
			yPanel.add( yLabel );
			yPanel.add( yTextField );
			rightPanel.add( ySlider );
			rightPanel.add( yPanel );
			JPanel minPanel = new JPanel();
			minPanel.add( minLevelLabel );
			minPanel.add( minTextField );
			rightPanel.add( minLevelSlider );
			rightPanel.add( minPanel );
			JPanel maxPanel = new JPanel();
			maxPanel.add( maxLevelLabel );
			maxPanel.add( maxTextField );
			rightPanel.add( maxLevelSlider );
			rightPanel.add( maxPanel );
			
			JPanel roi1Panel = new JPanel( new GridLayout( 0, 1 ) );
			roi1Panel.add( addROIButton );
			roi1Panel.add( updateROIButton );
			roi1Panel.add( deleteROIButton );
			roi1Panel.add( showAllROIsButton );
			rightPanel.add( roi1Panel );
			
			// Center panel
			JPanel centerPanel = new JPanel();
			centerPanel.setLayout(new BorderLayout());
			table = new JTable( new ResultTableModel() );
			table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			TableColumn col = table.getColumnModel().getColumn(0);
			col.setPreferredWidth(50);
			col = table.getColumnModel().getColumn(1);
			col.setPreferredWidth(45);
			col = table.getColumnModel().getColumn(2);
			col.setPreferredWidth(45);
			col = table.getColumnModel().getColumn(3);
			col.setPreferredWidth(45);
			col = table.getColumnModel().getColumn(4);
			col.setPreferredWidth(45);
			//table.setFillsViewportHeight( true );
			table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			table.getTableHeader().setReorderingAllowed( false );
			for ( int i = 0; i < table.getColumnCount(); i++ )
			{
				table.getColumn( table.getColumnName( i ) ).setCellRenderer( new ResultTableCellRenderer( i == 0 ? JLabel.CENTER : JLabel.TRAILING ) );
			}
			table.getSelectionModel().addListSelectionListener( new ListSelectionListener()
			{
				public void valueChanged( ListSelectionEvent lse )
				{
					ListSelectionModel lsm = ( ListSelectionModel )lse.getSource();
					if ( !lsm.isSelectionEmpty() )
					{
						RowData rowData = ( ( ResultTableModel )table.getModel() ).getRow( lsm.getMinSelectionIndex() );
						if ( rowData != null )
						{
							canUpdateROI = false;
							minLevelSlider.setValue( ( int )Math.round( rowData.getMin() ) );
							maxLevelSlider.setValue( ( int )Math.round( rowData.getMax() ) );
							xSlider.setValue( ( int )Math.round( rowData.getX() ) );
							ySlider.setValue( ( int )Math.round( rowData.getY() ) );
							canUpdateROI = true;
							drawFuzzyRoi(Color.RED);
							//if (impFuzzy == null) updateROI();
						}
					}
				}
			});

			table.addKeyListener(new TableKeyListener(table));

			JScrollPane scrollPane = new JScrollPane( table , JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);			
			scrollPane.setPreferredSize(new Dimension(250,485));
			centerPanel.add( scrollPane, BorderLayout.CENTER );
			roiTableBar = new JScrollBar(JScrollBar.HORIZONTAL);
			roiTableBar.setValues(0, 1, 0, imp.getImageStackSize());
			roiTableBar.addAdjustmentListener(new AdjustmentListener() {

				public void adjustmentValueChanged(AdjustmentEvent e) {
					int z = e.getValue();
					/*
					if (Math.abs(z-curRoiTableBarValue) > 1) {
						if (z > curRoiTableBarValue) {
							roiTableBar.setValue(curRoiTableBarValue+1);
						} else {
							roiTableBar.setValue(curRoiTableBarValue-1);
						}
					}
					*/
					curRoiTableBarValue = roiTableBar.getValue();
                    IJ.selectWindow(windowTitle);                            
                    IJ.setSlice(z+1);

					if (roisPerSlice == null) return;
					z = roiTableBar.getValue();
					if (frame == null) return;
					ResultTableModel model = ( ResultTableModel )frame.table.getModel();
			        int rows = model.getRowCount();
			        for (int j = 0; j < rows; j++)	
			        {      
						model.deleteRow(0);
			        }
			        
			        for (int j = 0; j < roisPerSlice[z]; j++)	// scans all ROIs on the slice
			        {      
						//Roi roi = createROI( x[z][j], y[z][j], (int) min[z][j], (int) max[z][j] );
						Roi roi = createFuzzyROI( x[z][j], y[z][j], (int) min[z][j], (int) max[z][j], Color.RED );
						int roiNumber = 1;
						if ( frame.table.getModel().getRowCount() >0 )
						{
							roiNumber = Integer.parseInt( ( ( Roi )model.getValueAt( model.getRowCount() - 1, 0 ) ).getName() ) + 1;
						}
						roi.setName( frame.roiNF.format( roiNumber ) );
						model.addRow( new RowData( roi, (int) min[z][j], (int) max[z][j], x[z][j], y[z][j] ) );
			        }
			        
					updateROIManager();
					ImageCanvas ic = imp.getCanvas();
					ic.setShowAllROIs( !ic.getShowAllROIs() );
					ic.setShowAllROIs( !ic.getShowAllROIs() );
					imp.draw();
					
					setSliders();
				}
				
			});

			for (Component comp : WindowManager.getCurrentWindow().getComponents()) {
				if (comp instanceof Scrollbar) {
					final Scrollbar scb = (Scrollbar)comp;
					scb.addAdjustmentListener(new AdjustmentListener() {

						public void adjustmentValueChanged(AdjustmentEvent e) {
							frame.roiTableBar.setValue(scb.getValue() - 1);

							frame.updateROIManager();
							ImageCanvas ic = Gebiss_.this.imp.getCanvas();
							ic.setShowAllROIs( !ic.getShowAllROIs() );
							ic.setShowAllROIs( !ic.getShowAllROIs() );
							Gebiss_.this.imp.draw();
						}
						
					});
					roiTableBar.setValue(scb.getValue() - 1);
				}
			}

			centerPanel.add( roiTableBar, BorderLayout.SOUTH );
			 
			setLayout( new BorderLayout( 5, 5 ) );
			add( centerPanel, BorderLayout.CENTER );
			add( rightPanel, BorderLayout.EAST );
			
			/*
			final JButton deleteROIButton = new JButton( "Delete ROI" );
			deleteROIButton.setActionCommand( "Delete" );
			
			final JButton outlineButton = new JButton( "Outline" );
			outlineButton.setActionCommand( "Outline" );

			final JButton newROIButton = new JButton( "Set New ROI" );
			newROIButton.setActionCommand( "Set New ROI" );

			final JButton clearButton = new JButton( "Clear ROI" );
			clearButton.setActionCommand( "Clear ROI" );
			final JButton clearOutsideButton = new JButton( "Clear Outside ROI" );
			clearOutsideButton.setActionCommand( "Clear Outside ROI" );
			final JButton undoButton = new JButton( "Undo" );
			undoButton.setActionCommand( "Undo" );

			final JButton drawButton = new JButton( "Draw" );
			drawButton.setActionCommand( "Draw" );
			final JButton fillButton = new JButton( "Fill" );
			fillButton.setActionCommand( "Fill" );
			final JButton nextButton = new JButton( "Next Slice" );
			undoButton.setActionCommand( "Next Slice" );
			final JButton previousButton = new JButton( "Previous Slice" );
			undoButton.setActionCommand( "Previous Slice" );
			 */
                        // 20071106 Leong Poh
			// Register for mouse events: 

			/*
			deleteROIButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent ae )
				{
					if ( RoiManager.getInstance() != null )
					{
						int selectedIndex = RoiManager.getInstance().getList().getSelectedIndex();
						ResultsTable rt = ResultsTable.getResultsTable();
						
						if ( selectedIndex > -1 && selectedIndex < rt.getCounter() )
						{
							//rt.deleteRow( selectedIndex );
							IJ.getTextPanel().clearSelection( selectedIndex, rt.getRowAsString( selectedIndex ) );
							RoiManager.getInstance().actionPerformed( ae );
						}
					}
				}
			} );
			 */

			/*
			outlineButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( debug )
						IJ.write( "Button pressed: " + e.getActionCommand() );
					outline();
				}
			} );
			newROIButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( debug )
						IJ.write( "Button pressed: " + e.getActionCommand() );
					initialize();
					minLevelSlider.setValue( ( int )min_level );
					minLevelLabel.setText( "Min Level: "
							+ minLevelSlider.getValue() );
					maxLevelSlider.setValue( ( int )max_level );
					maxLevelLabel.setText( "Max Level: "
							+ maxLevelSlider.getValue() );
					xSlider.setValue( ( int )x_centroid );
					xLabel.setText( "Horizontal Centroid: " + xSlider.getValue() );
					ySlider.setValue( ( int )y_centroid );
					yLabel.setText( "Vertical Centroid: " + ySlider.getValue() );
				}
			} );
			clearButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( debug )
						IJ.write( "Button pressed: " + e.getActionCommand() );
					IJ.run( "Clear" );
				}
			} );
			clearOutsideButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( debug )
						IJ.write( "Button pressed: " + e.getActionCommand() );
					IJ.run( "Clear Outside" );
				}
			} );
			undoButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( debug )
						IJ.write( "Button pressed: " + e.getActionCommand() );
					IJ.run( "Undo" );
				}
			} );

			drawButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( debug )
						IJ.write( "Button pressed: " + e.getActionCommand() );
					IJ.run( "Draw" );
					outline();
				}
			} );
			fillButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( debug )
						IJ.write( "Button pressed: " + e.getActionCommand() );
					// Insert a cycle to analyze all slices in the stack
					for ( int slice = 1; slice <= stack.getSize(); slice++ )
					{
						if ( imp.getCurrentSlice() < stack.getSize() )
						{
							imp.setSlice( imp.getCurrentSlice() + 1 );
							outline();
							//Thread.sleep(700); //700 in millsecond 
							// Run RGB_Measure to get statistics of each slice's ROI
							IJ.runPlugIn( "RGB_Measure", "" );
						}
					}
				}
			} );
			nextButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( debug )
						IJ.write( "Button pressed: " + e.getActionCommand() );
					if ( imp.getCurrentSlice() < stack.getSize() )
					{
						imp.setSlice( imp.getCurrentSlice() + 1 );
						outline();
					}
				}
			} );
			previousButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( debug )
						IJ.write( "Button pressed: " + e.getActionCommand() );
					if ( imp.getCurrentSlice() > 0 )
					{
						imp.setSlice( imp.getCurrentSlice() + 1 );
						outline();
					}
				}
			} );
			addWindowListener( new WindowAdapter()
			{
				public void windowActivated( WindowEvent we )
				{
					System.out.println("window activated");
					IJ.getInstance().toFront();
					if ( WindowManager.getCurrentWindow() != null )
					{
						WindowManager.getCurrentWindow().toFront();
					}
					//super.windowActivated( e );
				}
			});
			 */
			
			//updateUI();
			//pack();
			setVisible( true );			
		}
		
	    private void setSliders() {
			ImageStatistics stats = imp.getStatistics( Analyzer.getMeasurements() );
			int x_centroid = ( int )stats.xCentroid;
			int y_centroid = ( int )stats.yCentroid;
			int min_level = ( int )stats.min;
			int max_level = ( int )stats.max;
			int image_min = ( int )stats.min;
			int image_max = ( int )stats.max;
			
			// set X slider & text values
			xSlider.setMinimum( 0 );
			xSlider.setMaximum( imp.getWidth() );
			xSlider.setValue( x_centroid );
			xSlider.setMajorTickSpacing( imp.getWidth() / 4 );
			xSlider.setMinorTickSpacing( imp.getWidth() / 16 );
			xTextField.setText( String.valueOf( xSlider.getValue() ) );
			
			// set Y slider & text values
			ySlider.setMinimum( 0 );
			ySlider.setMaximum( imp.getHeight() );
			ySlider.setValue( y_centroid );
			ySlider.setMajorTickSpacing( imp.getHeight() / 4 );
			ySlider.setMinorTickSpacing( imp.getHeight() / 16 );
			yTextField.setText( String.valueOf( ySlider.getValue() ) );
			
			// set Min slider & text values
			minLevelSlider = new JSlider( JSlider.HORIZONTAL );
			minLevelSlider.setPaintTicks( true );
			minLevelSlider.setPaintLabels( true );
			minLevelSlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );
			minTextField.addKeyListener( new TextKeyListener( minLevelSlider ) );
			minLevelSlider.addChangeListener( new ChangeListener()
			{
				public void stateChanged( ChangeEvent e )
				{
					if ( minTextField.getText().length() == 0
							|| Integer.parseInt( minTextField.getText() ) != minLevelSlider
							.getValue() )
					{
						minTextField.setText( String.valueOf( minLevelSlider
								.getValue() ) );
					}
					if ( !canUpdateROI )
					{
						return;
					}
					drawROI();
				}
			});
			minLevelSlider.setMinimum( image_min );
			minLevelSlider.setMaximum( image_max );
			minLevelSlider.setValue( min_level );
			minLevelSlider.setMajorTickSpacing( ( image_max - image_min ) / 2 );
			minLevelSlider.setMinorTickSpacing( ( image_max - image_min ) / 4 );
			minTextField.setText( String.valueOf( minLevelSlider.getValue() ) );
			if (rightPanel != null) {
				rightPanel.remove(4);
				rightPanel.add(minLevelSlider, 4);
				rightPanel.updateUI();
			}
			
			// set Min slider & text values
			maxLevelSlider = new JSlider( JSlider.HORIZONTAL );
			maxLevelSlider.setPaintTicks( true );
			maxLevelSlider.setPaintLabels( true );
			maxLevelSlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );
			maxTextField.addKeyListener( new TextKeyListener( maxLevelSlider ) );
			maxLevelSlider.addChangeListener( new ChangeListener()
			{
				public void stateChanged( ChangeEvent e )
				{
					if ( maxTextField.getText().length() == 0
							|| Integer.parseInt( maxTextField.getText() ) != maxLevelSlider
							.getValue() )
					{
						maxTextField.setText( String.valueOf( maxLevelSlider
								.getValue() ) );
					}
					if ( !canUpdateROI )
					{
						return;
					}
					drawROI();
				}
			});
			maxLevelSlider.setMinimum( image_min );
			maxLevelSlider.setMaximum( image_max );
			maxLevelSlider.setValue( max_level );
			maxLevelSlider.setMajorTickSpacing( ( image_max - image_min ) / 2 );
			maxLevelSlider.setMinorTickSpacing( ( image_max - image_min ) / 4 );
			maxTextField.setText( String.valueOf( maxLevelSlider.getValue() ) );
			if (rightPanel != null) {
				rightPanel.remove(6);
				rightPanel.add(maxLevelSlider, 6);
				rightPanel.updateUI();
			}
	    }
	    
		public void updateUI()
		{
			if (table == null) return;
			
			//Roi rois[] = RoiManager.getInstance().getRoisAsArray();
			ResultsTable rt = Analyzer.getResultsTable();
			( ( ResultTableModel )table.getModel() ).clear();

			for ( int i = 0; i < rt.getCounter(); i++ )
			{
				double min = rt.getValue( "Min", i );
				double max = rt.getValue( "Max", i );
				double x = rt.getValue( "BX", i );
				double y = rt.getValue( "BY", i );
				WandMM w = new WandMM( imp.getProcessor() );

				w.npoints = 0;
				w.autoOutline( ( int )Math.round( x ), ( int )Math.round( y ), ( int )Math.round( min ), ( int )Math.round( max ) );
				PolygonRoi roi = new PolygonRoi( w.xpoints, w.ypoints, w.npoints, imp, Roi.TRACED_ROI );
				roi.setName( roiNF.format( table.getRowCount() + 1 ) );
				RowData rowData = new RowData( roi, rt.getValue( "Min", i ), rt.getValue( "Max", i ), rt.getValue( "BX", i ), rt.getValue( "BY", i ) );
				( ( ResultTableModel )table.getModel() ).addRow( rowData );
			}
		}

		public JSlider getXSlider()
		{
			return xSlider;
		}

		public JSlider getYSlider()
		{
			return ySlider;
		}
		
		private void updateROIManager()
		{
			RoiManager roiManager = RoiManager.getInstance();
			
			if ( roiManager == null )
			{
				roiManager = new RoiManager();
			}
			
			List list = roiManager.getList();
			Hashtable<String, Roi> hashtable = roiManager.getROIs();
			ResultTableModel model = ( ResultTableModel )table.getModel();
			
			list.removeAll();
			hashtable.clear();
			
			for ( int i = 0; i < model.getRowCount(); i++ )
			{
				Roi roi = model.getRow( i ).getRoi();
				list.add( roi.getName() );
				hashtable.put( roi.getName(), roi );
			}
			
			roiManager.repaint();
		}
		
		//Pops up a dialog box requesting user input, in this case Yes/No.
		private int saveDialog(File file){
			int n=0;
    		if(file.exists()) {
    			Object[] options = {"Yes", "No"};
    			n = JOptionPane.showOptionDialog(frame,
    					file + " already exists.\nDo you want to replace it?",
    				    "Saving Warning",
    				    JOptionPane.YES_NO_OPTION,
    				    JOptionPane.WARNING_MESSAGE,
    				    null,
    				    options,
    				    options[1]);
    		}
    		return n;
		}

	}

	private class ObjectSubsetFrame extends JPanel
	{
	}

	private class ObjectGTFrame extends JPanel
	{
		private final String HIDE_ALL_ROIS_TEXT = "Hide All ROIs";
		private final String SHOW_ALL_ROIS_TEXT = "Show All ROIs";
		
		private JTextField xTextField;
		private JTextField yTextField;
		private JTextField zTextField;
		private JTextField minTextField;
		private JTextField maxTextField;
		private JTextField classTextField;
		private JTable table;
		private NumberFormat roiNF = NumberFormat.getInstance();
		
		private boolean viewIsosurface = Boolean.parseBoolean(Prefs.get( PREFS_VIEW_ISOSURFACE_FLAG, null ));
		
		private void loadParameter( String filename )
		{
			ObjectResultTableModel model = (ObjectResultTableModel) frameObjectGT.table.getModel();
			model.clear();
			try
			{
				BufferedReader br = new BufferedReader( new FileReader( filename ) ); 
				// first row is header, read and discard
				String line1 = br.readLine();
				String line = "";
				int lineNum = 0;
				while ( ( line = br.readLine() ) != null )
				{
					StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
					while ( st.hasMoreTokens() )
					{
						// first column is index number, discard
						String tok = st.nextToken();
						
						if (line1.contains("Min\tMax\tSeedX\tSeedY\tSeedZ\tClass")) {
		    				model.addRow( new ObjectRowData( model.getRowCount() + 1,
		    						Integer.parseInt(st.nextToken()),
		    						Integer.parseInt(st.nextToken()),
		    						Integer.parseInt(st.nextToken()),
		    						Integer.parseInt(st.nextToken()),
		    						Integer.parseInt(st.nextToken()),
		    						Integer.parseInt(st.nextToken()) ));
						} else if (line1.contains("Centre X\tCentre Y\tCentre Z")) {
							st.nextToken();
							st.nextToken();
							st.nextToken();
		    				model.addRow( new ObjectRowData( model.getRowCount() + 1,
		    						0,
		    						255,
		    						Math.round(Float.parseFloat(st.nextToken())),
		    						Math.round(Float.parseFloat(st.nextToken())),
		    						Math.round(Float.parseFloat(st.nextToken())),
		    						0 ));
		    				st.nextToken();
		    				st.nextToken();
		    				st.nextToken();
						}
					}
				}
				br.close();
			} 
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		} 

		private void highlightRow(ListSelectionEvent lse) {
			ListSelectionModel lsm = ( ListSelectionModel )lse.getSource();
			if ( !lsm.isSelectionEmpty() )
			{
				ObjectRowData rowData = ( ( ObjectResultTableModel )frameObjectGT.table.getModel() ).getRow( lsm.getMinSelectionIndex() );
				if ( rowData != null )
				{
					frameObjectGT.minTextField.setText(rowData.getMin()+"");
					frameObjectGT.maxTextField.setText(rowData.getMax()+"");
					frameObjectGT.xTextField.setText(rowData.getSeedX()+"");
					frameObjectGT.yTextField.setText(rowData.getSeedY()+"");
					frameObjectGT.zTextField.setText(rowData.getSeedZ()+"");
					frameObjectGT.classTextField.setText(rowData.getClassNum()+"");

			        IJ.setTool(Toolbar.POINT);

			        imp.setSlice(rowData.getSeedZ());

    	        	ImageCanvas canvas = imp.getCanvas();
					int x = canvas.screenX( rowData.getSeedX() );
					int y = canvas.screenY( rowData.getSeedY() );
			        
			        imp.createNewRoi(x, y);
				}
			}
		}
		
		private void saveParameter() {
			JFileChooser fc = new JFileChooser();
			String outputFormatter = "";
			
			// load the directory from ImageJ preference list
			String dir = Prefs.get( PREFS_SAVE_ROI_DIRECTORY, null );
			if ( dir != null )
			{
				fc.setCurrentDirectory( new File( dir ) );
			}
			
			ObjectResultTableModel model = ( ObjectResultTableModel )table.getModel();
			FileNameFilter CSVfilter = new FileNameFilter("csv", "Comma Separated Values (*.csv)");
			fc.addChoosableFileFilter(CSVfilter);
			fc.setDialogTitle("Save Parameter (Select File)");
			fc.setSelectedFile(new File(IJ.getImage().getShortTitle().toUpperCase() + "_Parameters.csv"));                                        
			fc.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent e) {
					if(JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equalsIgnoreCase(e.getPropertyName())) {
						if((e.getNewValue().getClass()).getSimpleName().equalsIgnoreCase("FileNameFilter")) {
							String ext = ((FileNameFilter)e.getNewValue()).getExtension();
							JFileChooser fc = (JFileChooser)e.getSource();
							fc.setSelectedFile(new File(IJ.getImage().getShortTitle().toUpperCase() + "_Parameters." + ext));
						}
					}
				}
			});	
			if ( fc.showDialog( frameTop, "Select" ) == JFileChooser.APPROVE_OPTION ) {
	            try{
					// Shows a overwrite dialog box to prompt for overwriting if file exist.
			    	if( saveDialog(fc.getSelectedFile()) == 1)
			    		return;					    		
					// save the directory to ImageJ preference list
					Prefs.set( PREFS_SAVE_ROI_DIRECTORY, fc.getSelectedFile().getAbsolutePath() );

				    FileWriter fw = new FileWriter(fc.getSelectedFile());
				    outputFormatter = "\tMin\tMax\tSeedX\tSeedY\tSeedZ\tClass\n";
				    for(int i=0; i<model.getRowCount(); i++) {
					    	outputFormatter += model.getValueAt(i, 0) + "\t";
					    	outputFormatter += model.getValueAt(i, 1) + "\t";
					    	outputFormatter += model.getValueAt(i, 2) + "\t";
					    	outputFormatter += model.getValueAt(i, 3) + "\t";
					    	outputFormatter += model.getValueAt(i, 4) + "\t";
					    	outputFormatter += model.getValueAt(i, 5) + "\t";
					    	outputFormatter += model.getValueAt(i, 6) + "\n";
				    }
				    fw.write(outputFormatter);
					fw.close();
	               }
	            catch (IOException ioe) {
	                ioe.printStackTrace();
	            }
			}					
		}
		
	    public ObjectGTFrame()
		{
			//setTitle( "Segmenting Assistant" );

			roiNF.setMinimumIntegerDigits( 8 );
			roiNF.setGroupingUsed( false );

			// Create a label for the x position slider:
			JLabel xLabel = new JLabel( "SeedX:", JLabel.LEFT ); //"Horizontal Centroid::"
			xTextField = new JTextField( 8 );
			
			// Create a label for the y position slider:
			JLabel yLabel = new JLabel( "SeedY: ", JLabel.LEFT ); //"Vertical Centroid:: "
			yTextField = new JTextField( 8 );
			
			// Create a label for the y position slider:
			JLabel zLabel = new JLabel( "SeedZ: ", JLabel.LEFT ); //"Vertical Centroid:: "
			zTextField = new JTextField( 8 );
			
			// Create a label for the min level slider:
			JLabel minLevelLabel = new JLabel( "Min: ", JLabel.LEFT );
			minTextField = new JTextField( 8 );
			
			// Create a label for the max level slider:
			JLabel maxLevelLabel = new JLabel( "Max: ", JLabel.LEFT );
			maxTextField = new JTextField( 8 );
			maxTextField.setText("256");
			
			JLabel classLevelLabel = new JLabel( "Class: ", JLabel.LEFT );
			classTextField = new JTextField( 8 );
			
			// add mouse listener to image window, if not added before
			MouseListener ml[] = WindowManager.getCurrentWindow().getCanvas().getMouseListeners();
			boolean hasMouseListener = false;
			for ( int i = 0; i < ml.length; i++ )
			{
				if ( ml[i] instanceof ObjectImageWindowMouseListener )
				{
					hasMouseListener = true;
					break;
				}
			}
			if ( !hasMouseListener )
			{
				//ImageWindow iw = WindowManager.getCurrentWindow();
				imp.getWindow().getCanvas().addMouseListener( new ObjectImageWindowMouseListener() );
				//System.out.println(WindowManager.getWindowCount());
				//WindowManager.getCurrentWindow().getCanvas().addMouseListener( new ObjectImageWindowMouseListener() );
			}
			// add mouse wheel listener to image window, if not added before
			MouseWheelListener mwl[] = WindowManager.getCurrentWindow().getCanvas().getMouseWheelListeners();
			boolean hasMouseWheelListener = false;
			for ( int i = 0; i < mwl.length; i++ )
			{
				if ( mwl[i] instanceof ImageWindowWheelMouseListener )
				{
					hasMouseWheelListener = true;
					break;
				}
			}
			if ( !hasMouseWheelListener )
			{
				WindowManager.getCurrentWindow().getCanvas().addMouseWheelListener( new ImageWindowWheelMouseListener() );
			}
			for (Component comp : WindowManager.getCurrentWindow().getComponents()) {
				if (comp instanceof Scrollbar) {
					Scrollbar scb = (Scrollbar)comp;
					if(scb.getOrientation() == Scrollbar.HORIZONTAL)
					{
						AdjustmentListener als[] = scb.getAdjustmentListeners();
						boolean hasAdjustmentListener = false;
						for ( AdjustmentListener al : als )
						{
							if ( al instanceof ImageWindowAdjustmentListener )
							{
								hasAdjustmentListener = true;
								break;
							}
						}
						// don't add more than once
						if ( !hasAdjustmentListener )
						{
							scb.addAdjustmentListener(new ImageWindowAdjustmentListener());
						}
					}
				}
			}
			
			// adding buttons
			final JButton addROIButton = new JButton( "Add Row" );
			addROIButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent ae )
				{
					ObjectResultTableModel model = ( ObjectResultTableModel )table.getModel();
					int min = 0;
					try {min = Integer.parseInt(frameObjectGT.minTextField.getText());
					} catch (NumberFormatException e) {}
					int max = 255;
					try {max = Integer.parseInt(frameObjectGT.maxTextField.getText());
					} catch (NumberFormatException e) {}
					int x = 0;
					try {x = Integer.parseInt(frameObjectGT.xTextField.getText());
					} catch (NumberFormatException e) {}
					int y = 0;
					try {y = Integer.parseInt(frameObjectGT.yTextField.getText());
					} catch (NumberFormatException e) {}
					int z = 0;
					try {z = Integer.parseInt(frameObjectGT.zTextField.getText());
					} catch (NumberFormatException e) {}
					int c = 0;
					try {c = Integer.parseInt(frameObjectGT.classTextField.getText());
					} catch (NumberFormatException e) {}

    				model.addRow( new ObjectRowData( frameObjectGT.table.getModel().getRowCount() + 1,
    						min, max, x, y, z, c));
				}
			});
			
			final JButton updateROIButton = new JButton( "Update Row" );
			updateROIButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent ae )
				{
					ListSelectionModel lsm = table.getSelectionModel();
					
					if ( lsm.isSelectionEmpty() )
					{
						IJ.error( "No row selected!" );
						return;
					}
					
					int min = 0;
					try {min = Integer.parseInt(frameObjectGT.minTextField.getText());
					} catch (NumberFormatException e) {}
					int max = 255;
					try {max = Integer.parseInt(frameObjectGT.maxTextField.getText());
					} catch (NumberFormatException e) {}
					int x = 0;
					try {x = Integer.parseInt(frameObjectGT.xTextField.getText());
					} catch (NumberFormatException e) {}
					int y = 0;
					try {y = Integer.parseInt(frameObjectGT.yTextField.getText());
					} catch (NumberFormatException e) {}
					int z = 0;
					try {z = Integer.parseInt(frameObjectGT.zTextField.getText());
					} catch (NumberFormatException e) {}
					int c = 0;
					try {c = Integer.parseInt(frameObjectGT.classTextField.getText());
					} catch (NumberFormatException e) {}

					ObjectResultTableModel model = ( ObjectResultTableModel )table.getModel();
					model.updateRow( lsm.getMinSelectionIndex(), new ObjectRowData( lsm.getMinSelectionIndex(), min, max, x, y, z, c ) );
				}
			});
			
			final JButton deleteROIButton = new JButton( "Delete Row" );
			deleteROIButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent ae )
				{
					ListSelectionModel lsm = table.getSelectionModel();
					
					if ( lsm.isSelectionEmpty() )
					{
						IJ.error( "No row selected!" );
						return;
					}
					
					ObjectResultTableModel model = ( ObjectResultTableModel )table.getModel();
					model.deleteRow( lsm.getMinSelectionIndex() );
				}
			});
			
			// Use a slightly 10 pt font for the buttons:
			Font buttonFont = deleteROIButton.getFont().deriveFont( ( float )10.0 );
			addROIButton.setFont( buttonFont );
			updateROIButton.setFont( buttonFont );
			deleteROIButton.setFont( buttonFont );
			
			// Add all the UI components to the content pane:
			JPanel rightPanel = new JPanel();
			rightPanel.setLayout( new BoxLayout( rightPanel, BoxLayout.Y_AXIS ) );
			rightPanel.setBorder( BorderFactory.createEmptyBorder( 10, 0, 10, 10 ) );
			JPanel xPanel = new JPanel();
			xPanel.add( xLabel );
			xPanel.add( xTextField );
			rightPanel.add( xPanel );
			JPanel yPanel = new JPanel();
			yPanel.add( yLabel );
			yPanel.add( yTextField );
			rightPanel.add( yPanel );
			JPanel zPanel = new JPanel();
			zPanel.add( zLabel );
			zPanel.add( zTextField );
			rightPanel.add( zPanel );
			JPanel minPanel = new JPanel();
			minPanel.add( minLevelLabel );
			minPanel.add( minTextField );
			rightPanel.add( minPanel );
			JPanel maxPanel = new JPanel();
			maxPanel.add( maxLevelLabel );
			maxPanel.add( maxTextField );
			rightPanel.add( maxPanel );
			JPanel classPanel = new JPanel();
			classPanel.add( classLevelLabel );
			classPanel.add( classTextField );
			rightPanel.add( classPanel );
			
			JPanel roi1Panel = new JPanel( new GridLayout( 7, 1 ) );
			roi1Panel.add( addROIButton );
			roi1Panel.add( updateROIButton );
			roi1Panel.add( deleteROIButton );
			rightPanel.add( roi1Panel );
			
			// Center panel
			JPanel centerPanel = new JPanel();
			centerPanel.setLayout(new BorderLayout());
			table = new JTable( new ObjectResultTableModel() );
			table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			TableColumn col = table.getColumnModel().getColumn(0);
			col.setPreferredWidth(50);
			col = table.getColumnModel().getColumn(1);
			col.setPreferredWidth(45);
			col = table.getColumnModel().getColumn(2);
			col.setPreferredWidth(45);
			col = table.getColumnModel().getColumn(3);
			col.setPreferredWidth(45);
			col = table.getColumnModel().getColumn(4);
			col.setPreferredWidth(45);
			//table.setFillsViewportHeight( true );
			table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			table.getTableHeader().setReorderingAllowed( false );
			for ( int i = 0; i < table.getColumnCount(); i++ )
			{
				table.getColumn( table.getColumnName( i ) ).setCellRenderer( new ResultTableCellRenderer( i == 0 ? JLabel.CENTER : JLabel.TRAILING ) );
			}
			table.getSelectionModel().addListSelectionListener( new ListSelectionListener()
			{
				public void valueChanged( ListSelectionEvent lse )
				{
					highlightRow(lse);
				}
			});
			table.addKeyListener(new TableKeyListener(table));

			JScrollPane scrollPane = new JScrollPane( table , JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);			
			scrollPane.setPreferredSize(new Dimension(250,485));
			centerPanel.add( scrollPane, BorderLayout.CENTER );

			for (Component comp : WindowManager.getCurrentWindow().getComponents()) {
				if (comp instanceof Scrollbar) {
					final Scrollbar scb = (Scrollbar)comp;
					scb.addAdjustmentListener(new AdjustmentListener() {

						public void adjustmentValueChanged(AdjustmentEvent e) {
							frame.roiTableBar.setValue(scb.getValue() - 1);

							frame.updateROIManager();
							ImageCanvas ic = Gebiss_.this.imp.getCanvas();
							ic.setShowAllROIs( !ic.getShowAllROIs() );
							ic.setShowAllROIs( !ic.getShowAllROIs() );
							Gebiss_.this.imp.draw();
						}
						
					});
				}
			}

			setLayout( new BorderLayout( 5, 5 ) );
			add( centerPanel, BorderLayout.CENTER );
			add( rightPanel, BorderLayout.EAST );
			
			ImageStatistics stats = imp.getStatistics( Analyzer.getMeasurements() );
			int x_centroid = ( int )stats.xCentroid;
			int y_centroid = ( int )stats.yCentroid;
			int min_level = ( int )stats.min;
			int max_level = ( int )stats.max;
			int image_min = ( int )stats.min;
			int image_max = ( int )stats.max;
			
			//updateUI();
			//pack();
			setVisible( true );			
		}
		
		public void updateUI()
		{
			if (table == null) return;
			
			//Roi rois[] = RoiManager.getInstance().getRoisAsArray();
			ResultsTable rt = Analyzer.getResultsTable();
			( ( ResultTableModel )table.getModel() ).clear();

			for ( int i = 0; i < rt.getCounter(); i++ )
			{
				double min = rt.getValue( "Min", i );
				double max = rt.getValue( "Max", i );
				double x = rt.getValue( "BX", i );
				double y = rt.getValue( "BY", i );
				WandMM w = new WandMM( imp.getProcessor() );

				w.npoints = 0;
				w.autoOutline( ( int )Math.round( x ), ( int )Math.round( y ), ( int )Math.round( min ), ( int )Math.round( max ) );
				PolygonRoi roi = new PolygonRoi( w.xpoints, w.ypoints, w.npoints, imp, Roi.TRACED_ROI );
				roi.setName( roiNF.format( table.getRowCount() + 1 ) );
				RowData rowData = new RowData( roi, rt.getValue( "Min", i ), rt.getValue( "Max", i ), rt.getValue( "BX", i ), rt.getValue( "BY", i ) );
				( ( ResultTableModel )table.getModel() ).addRow( rowData );
			}
		}

		private void updateROIManager()
		{
			RoiManager roiManager = RoiManager.getInstance();
			
			if ( roiManager == null )
			{
				roiManager = new RoiManager();
			}
			
			List list = roiManager.getList();
			Hashtable<String, Roi> hashtable = roiManager.getROIs();
			ResultTableModel model = ( ResultTableModel )table.getModel();
			
			list.removeAll();
			hashtable.clear();
			
			for ( int i = 0; i < model.getRowCount(); i++ )
			{
				Roi roi = model.getRow( i ).getRoi();
				list.add( roi.getName() );
				hashtable.put( roi.getName(), roi );
			}
			
			roiManager.repaint();
		}
		
		//Pops up a dialog box requesting user input, in this case Yes/No.
		private int saveDialog(File file){
			int n=0;
    		if(file.exists()) {
    			Object[] options = {"Yes", "No"};
    			n = JOptionPane.showOptionDialog(frame,
    					file + " already exists.\nDo you want to replace it?",
    				    "Saving Warning",
    				    JOptionPane.YES_NO_OPTION,
    				    JOptionPane.WARNING_MESSAGE,
    				    null,
    				    options,
    				    options[1]);
    		}
    		return n;
		}
	}

	//=================================================================================
	// WandMM.java
	// 
	// This class implements something like ImageJ's wand (tracing) tool.
	// The difference is that this one is intended to work with all image
	// types, not just byte and 8 bit color images.
	//
	//=================================================================================
	private class WandMM
	{
		boolean debug = false;

		static final int UP = 0, DOWN = 1, UP_OR_DOWN = 2, LEFT = 3, RIGHT = 4,
				LEFT_OR_RIGHT = 5, NA = 6;

		// The number of points in the generated outline:
		public int npoints;
		private int maxPoints = 1000;

		// The x-coordinates of the points in the outline:
		public int[] xpoints = new int[maxPoints];
		// The y-coordinates of the points in the outline:
		public int[] ypoints = new int[maxPoints];

		private ImageProcessor wandip;

		private int width, height;
		private float lowerThreshold, upperThreshold;

		// Construct a Wand object from an ImageProcessor:
		public WandMM( ImageProcessor ip )
		{
			if ( debug )
				IJ.write( "WandMM..." );

			wandip = ip;

			width = ip.getWidth();
			height = ip.getHeight();
			if ( debug )
				IJ.write( "WandMM middle pixel = "
						+ ip.getPixelValue( 128, 128 ) );
			if ( debug )
				IJ.write( "done with constructor" );
		}

		private boolean inside( int x, int y )
		{
			//if ( debug ) IJ.write("WandMM.inside...");
			float value;
			if ( debug )
				IJ.write( "WandMM.getPixel(x,y) = "
						+ wandip.getPixelValue( x, y ) );
			//value =  getPixel( x, y );
			value = wandip.getPixelValue( x, y );
			return ( value >= lowerThreshold ) && ( value <= upperThreshold );
		}

		// Are we tracing a one pixel wide line?
		boolean isLine( int xs, int ys )
		{
			if ( debug )
				IJ.write( "WandMM.isLine..." );

			int r = 5;
			int xmin = xs;
			int xmax = xs + 2 * r;
			if ( xmax >= width )
				xmax = width - 1;
			int ymin = ys - r;
			if ( ymin < 0 )
				ymin = 0;
			int ymax = ys + r;
			if ( ymax >= height )
				ymax = height - 1;
			int area = 0;
			int insideCount = 0;
			for ( int x = xmin; ( x <= xmax ); x++ )
				for ( int y = ymin; y <= ymax; y++ )
				{
					area++;
					if ( inside( x, y ) )
						insideCount++;
				}
			if ( IJ.debugMode )
				IJ.write( ( ( ( double )insideCount ) / area >= 0.75 ? "line "
						: "blob " )
						+ insideCount
						+ " "
						+ area
						+ " "
						+ IJ.d2s( ( ( double )insideCount ) / area ) );
			return ( ( double )insideCount ) / area >= 0.75;
		}

		// Traces an object defined by lower and upper threshold
		// values. The boundary points are stored in the public xpoints
		// and ypoints fields.
		public void autoOutline( int startX, int startY, int lower, int upper )
		{
			if ( debug )
				IJ.write( "WandMM.autoOutline..." );

			int x = startX;
			int y = startY;
			int direction;
			lowerThreshold = lower;
			upperThreshold = upper;
			if ( inside( x, y ) )
			{
				do
				{
					x++;
				} while ( inside( x, y ) && x < width );
				if ( !inside( x - 1, y - 1 ) ) // upper left
					direction = RIGHT;
				else if ( inside( x, y - 1 ) ) // upper right
					direction = LEFT;
				else
					direction = DOWN;
				if ( x >= width )
					return;
			} else
			{
				do
				{
					x++;
				} while ( !inside( x, y ) && x < width );
				direction = UP;
				if ( x >= width )
					return;
			}
			traceEdge( x, y, direction );
		}

		void traceEdge( int xstart, int ystart, int startingDirection )
		{
			maxPoints = 1000;
			xpoints = new int[maxPoints];
			ypoints = new int[maxPoints];
			if ( debug )
				IJ.write( "WandMM.traceEdge..." );

			int[] table = {
			// 1234, 1=upper left pixel,  2=upper right, 3=lower left, 4=lower right
					NA, // 0000, should never happen
					RIGHT, // 000X,
					DOWN, // 00X0
					RIGHT, // 00XX
					UP, // 0X00
					UP, // 0X0X
					UP_OR_DOWN, // 0XX0 Go up or down depending on current direction
					UP, // 0XXX
					LEFT, // X000
					LEFT_OR_RIGHT, // X00X  Go left or right depending on current direction
					DOWN, // X0X0
					RIGHT, // X0XX
					LEFT, // XX00
					LEFT, // XX0X
					DOWN, // XXX0
					NA, // XXXX Should never happen
			};
			int index;
			int newDirection;
			int x = xstart;
			int y = ystart;
			int direction = startingDirection;
			int[] xtemp;
			int[] ytemp;

			boolean UL = inside( x - 1, y - 1 ); // upper left
			boolean UR = inside( x, y - 1 ); // upper right
			boolean LL = inside( x - 1, y ); // lower left
			boolean LR = inside( x, y ); // lower right
			//xpoints[0] = x;
			//ypoints[0] = y;
			int count = 0;
			//IJ.write("");
			//IJ.write(count + " " + x + " " + y + " " + direction + " " + insideValue);

			// the array is to keep track of all the x and y that are assigned to the xpoints and ypoints together with their newDirection
			ArrayList<ThreePoints> points = new ArrayList<ThreePoints>();
			
			do
			{
				index = 0;
				if ( LR )
					index |= 1;
				if ( LL )
					index |= 2;
				if ( UR )
					index |= 4;
				if ( UL )
					index |= 8;
				newDirection = table[index];
				if ( newDirection == UP_OR_DOWN )
				{
					if ( direction == RIGHT )
						newDirection = UP;
					else
						newDirection = DOWN;
				}
				if ( newDirection == LEFT_OR_RIGHT )
				{
					if ( direction == UP )
						newDirection = LEFT;
					else
						newDirection = RIGHT;
				}
				if ( newDirection != direction )
				{
					// is there a need to exclude those x and y are both equal to 0?
					//if ( !( x == 0 && y == 0 ) )
					{
						ThreePoints p = new ThreePoints( x, y, newDirection );
						// if same x, y and newDirection has been registered, no need to trace as the program will repeat to trace the same
						// points (end up in infinite loop) until it run out of memory
						if ( points.contains( p ) )
						{
							break;
						}
						else
						{
							points.add( p );
						}
					}
					xpoints[count] = x;
					ypoints[count] = y;
					count++;
					if ( count == xpoints.length )
					{
						xtemp = new int[maxPoints*2];
						ytemp = new int[maxPoints*2];
						System.arraycopy( xpoints, 0, xtemp, 0, maxPoints );
						System.arraycopy( ypoints, 0, ytemp, 0, maxPoints );
						xpoints = xtemp;
						ypoints = ytemp;
						maxPoints *= 2;
					}
					//if (count<10) IJ.write(count + " " + x + " " + y + " " + newDirection + " " + index);
				}
				switch ( newDirection )
				{
				case UP:
					y = y - 1;
					LL = UL;
					LR = UR;
					UL = inside( x - 1, y - 1 );
					UR = inside( x, y - 1 );
					break;
				case DOWN:
					y = y + 1;
					UL = LL;
					UR = LR;
					LL = inside( x - 1, y );
					LR = inside( x, y );
					break;
				case LEFT:
					x = x - 1;
					UR = UL;
					LR = LL;
					UL = inside( x - 1, y - 1 );
					LL = inside( x - 1, y );
					break;
				case RIGHT:
					x = x + 1;
					UL = UR;
					LL = LR;
					UR = inside( x, y - 1 );
					LR = inside( x, y );
					break;
				}
				direction = newDirection;
			}
			// the additional check for newDirection is to prevent the code to go into infinite loop when the value is NA.
			while ( ( x != xstart || y != ystart || direction != startingDirection ) && newDirection != NA );
			npoints = count;
		}
	}
	
	/**
	 * This class stores 3 integers and treat any ThreePoints object with the same x, y, and z values as equal.
	 */
	private class ThreePoints
	{
		private int x;
		private int y;
		private int z;
		
		public ThreePoints( int x, int y, int z )
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		/*
		 * Return true if all the corresponding x, y and z values are equal
		 */
		public boolean equals( Object obj )
		{
			if ( obj instanceof ThreePoints )
			{
				ThreePoints tp = ( ThreePoints )obj;
				return tp.x == x && tp.y == y && tp.z == z;
			}
			
			return false;
		}
	}
	
	private class ResultTableModel extends AbstractTableModel
	{
		public final String headerNames[] = new String[]{ "ROI ID", "Min", "Max", "StartX", "StartY" };
		private Vector<RowData> dataVector;
		private NumberFormat nf;
		
		public ResultTableModel()
		{
			nf = NumberFormat.getNumberInstance();
			nf.setMinimumFractionDigits( 0 );
			nf.setMaximumFractionDigits( 0 );
			nf.setGroupingUsed( false );
			dataVector = new Vector<RowData>();
		}
		
		public String getColumnName( int columnIndex )
		{
			return headerNames[columnIndex];
		}
		
		public int getColumnCount()
		{
			return headerNames.length;
		}

		public int getRowCount()
		{
			return dataVector.size();
		}

		public Object getValueAt( int rowIndex, int columnIndex )
		{
			if ( dataVector.size() > rowIndex )
			{
				RowData rowData = dataVector.get( rowIndex );
				
				switch ( columnIndex )
				{
					case 0:
						return rowData.getRoi();
					case 1:
						return nf.format( rowData.getMin() );
					case 2:
						return nf.format( rowData.getMax() );
					case 3:
						return nf.format( rowData.getX() );
					case 4:
						return nf.format( rowData.getY() );
					default:
						break;
				}
			}
			return null;
		}
		
		public void setValueAt( Object value, int rowIndex, int columnIndex )
		{
			double dValue;
			
			if ( value == null )
			{
				return;
			}
			try
			{
				dValue = Double.parseDouble( ( String )value );
			}
			catch ( Exception e )
			{
				return;
			}
			
			if ( dataVector.size() > rowIndex )
			{
				RowData rowData = dataVector.get( rowIndex );
				
				switch ( columnIndex )
				{
					case 1:
						rowData.setMin( dValue );
						Gebiss_.this.frame.minTextField.setText( String.valueOf( ( int )Math.round( dValue ) ) );
						Gebiss_.this.frame.minLevelSlider.setValue( ( int )Math.round( dValue ) );
						break;
					case 2:
						rowData.setMax( dValue );
						Gebiss_.this.frame.maxTextField.setText( String.valueOf( ( int )Math.round( dValue ) ) );
						Gebiss_.this.frame.maxLevelSlider.setValue( ( int )Math.round( dValue ) );
						break;
					case 3:
						rowData.setX( dValue );
						Gebiss_.this.frame.xTextField.setText( String.valueOf( ( int )Math.round( dValue ) ) );
						Gebiss_.this.frame.xSlider.setValue( ( int )Math.round( dValue ) );
						break;
					case 4:
						rowData.setY( dValue );
						Gebiss_.this.frame.yTextField.setText( String.valueOf( ( int )Math.round( dValue ) ) );
						Gebiss_.this.frame.ySlider.setValue( ( int )Math.round( dValue ) );
						break;
					default:
						break;
				}
			}
			fireTableCellUpdated( rowIndex, columnIndex );
		}
		
		public boolean isCellEditable( int rowIndex, int columnIndex )
		{
			return columnIndex > 0;
		}
		
		public void addRow( RowData rowData )
		{
			dataVector.add( rowData );
			fireTableRowsInserted( dataVector.size(), dataVector.size() );
		}
		
		public boolean updateRow( int rowIndex, RowData rowData )
		{
			if ( dataVector.size() <= rowIndex )
			{
				return false;
			}
			dataVector.set( rowIndex, rowData );
			fireTableRowsUpdated( rowIndex, rowIndex );
			return true;
		}
		
		public boolean deleteRow( int rowIndex )
		{
			if ( dataVector.size() <= rowIndex )
			{
				return false;
			}
			dataVector.remove( rowIndex );
//			fireTableRowsDeleted( rowIndex, rowIndex );
			
			// rename the ROI name according to how it is ordered in the table
			for ( int i = 0; i < dataVector.size(); i++ )
			{
				dataVector.get( i ).getRoi().setName( frame.roiNF.format( i + 1 ) );
			}
			
			fireTableDataChanged();
			return true;
		}
		
		public RowData getRow( int rowIndex )
		{
			if ( dataVector.size() > rowIndex )
			{
				return dataVector.get( rowIndex );
			}
			return null;
		}
		
		public void clear()
		{
			dataVector.removeAllElements();
			fireTableDataChanged();
		}
	}
	
	private class ObjectResultTableModel extends AbstractTableModel
	{
		public final String headerNames[] = new String[]{ "ID", "Min", "Max", "SeedX", "SeedY", "SeedZ", "Class (0-9)" };
		private Vector<ObjectRowData> dataVector;
		private NumberFormat nf;
		
		public ObjectResultTableModel()
		{
			nf = NumberFormat.getNumberInstance();
			nf.setMinimumFractionDigits( 0 );
			nf.setMaximumFractionDigits( 0 );
			nf.setGroupingUsed( false );
			dataVector = new Vector<ObjectRowData>();
		}
		
		public String getColumnName( int columnIndex )
		{
			return headerNames[columnIndex];
		}
		
		public int getColumnCount()
		{
			return headerNames.length;
		}

		public int getRowCount()
		{
			return dataVector.size();
		}

		public Object getValueAt( int rowIndex, int columnIndex )
		{
			if ( dataVector.size() > rowIndex )
			{
				ObjectRowData rowData = dataVector.get( rowIndex );
				
				switch ( columnIndex )
				{
					case 0:
						return nf.format( rowData.getIndex() );
					case 1:
						return nf.format( rowData.getMin() );
					case 2:
						return nf.format( rowData.getMax() );
					case 3:
						return nf.format( rowData.getSeedX() );
					case 4:
						return nf.format( rowData.getSeedY() );
					case 5:
						return nf.format( rowData.getSeedZ() );
					case 6:
						return nf.format( rowData.getClassNum() );
					default:
						break;
				}
			}
			return null;
		}
		
		public void setValueAt( Object value, int rowIndex, int columnIndex )
		{
			int iValue;
			
			if ( value == null )
			{
				return;
			}
			try
			{
				iValue = Integer.parseInt( ( String )value );
			}
			catch ( Exception e )
			{
				return;
			}
			
			if ( dataVector.size() > rowIndex )
			{
				ObjectRowData rowData = dataVector.get( rowIndex );
				
				switch ( columnIndex )
				{
					case 1:
						rowData.setMin( iValue );
						Gebiss_.this.frameObjectGT.minTextField.setText( String.valueOf( iValue ) );
						break;
					case 2:
						rowData.setMax( iValue );
						Gebiss_.this.frameObjectGT.maxTextField.setText( String.valueOf( iValue ) );
						break;
					case 3:
						rowData.setSeedX( iValue );
						Gebiss_.this.frameObjectGT.xTextField.setText( String.valueOf( iValue ) );
						break;
					case 4:
						rowData.setSeedY( iValue );
						Gebiss_.this.frameObjectGT.yTextField.setText( String.valueOf( iValue ) );
						break;
					case 5:
						rowData.setSeedZ( iValue );
						Gebiss_.this.frameObjectGT.zTextField.setText( String.valueOf( iValue ) );
						break;
					case 6:
						rowData.setClassNum( iValue );
						Gebiss_.this.frameObjectGT.classTextField.setText( String.valueOf( iValue ) );
						break;
					default:
						break;
				}
			}
			fireTableCellUpdated( rowIndex, columnIndex );
		}
		
		public boolean isCellEditable( int rowIndex, int columnIndex )
		{
			return columnIndex > 0;
		}
		
		public void addRow( ObjectRowData rowData )
		{
			dataVector.add( rowData );
			fireTableRowsInserted( dataVector.size(), dataVector.size() );
		}
		
		public boolean updateRow( int rowIndex, ObjectRowData rowData )
		{
			if ( dataVector.size() <= rowIndex )
			{
				return false;
			}
			dataVector.set( rowIndex, rowData );
			fireTableRowsUpdated( rowIndex, rowIndex );
			return true;
		}
		
		public boolean deleteRow( int rowIndex )
		{
			if ( dataVector.size() <= rowIndex )
			{
				return false;
			}
			dataVector.remove( rowIndex );
//			fireTableRowsDeleted( rowIndex, rowIndex );
			
			fireTableDataChanged();
			return true;
		}
		
		public ObjectRowData getRow( int rowIndex )
		{
			if ( dataVector.size() > rowIndex )
			{
				return dataVector.get( rowIndex );
			}
			return null;
		}
		
		public void clear()
		{
			dataVector.removeAllElements();
			fireTableDataChanged();
		}
	}
	
	private class ResultTableCellRenderer extends DefaultTableCellRenderer
	{
		private NumberFormat nf;
		
		public ResultTableCellRenderer( int halign )
		{
			setHorizontalAlignment( halign );
			nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits( 3 );
			nf.setMaximumFractionDigits( 3 );
			nf.setGroupingUsed( false );
		}
		
		public void setValue( Object value )
		{
			if ( value == null )
			{
				setText( "" );
			}
			else if ( value instanceof Roi )
			{
				setText( ( ( Roi )value ).getName() );
			}
			else if ( value instanceof Double )
			{
				setText( nf.format( value ) );
			}
			else
			{
				super.setText( value.toString() );
			}
		}
	}
	
	private class RowData
	{
		private Roi roi;
		private double min;
		private double max;
		private double x;
		private double y;
		
		public RowData( Roi roi, double min, double max, double x, double y )
		{
			this.roi = roi;
			this.min = min;
			this.max = max;
			this.x = x;
			this.y = y;
		}

		public Roi getRoi()
		{
			return roi;
		}
		
		public void setRoi( Roi roi )
		{
			this.roi = roi;
		}
		
		public double getMin()
		{
			return min;
		}
		
		public void setMin( double min )
		{
			this.min = min;
		}
		
		public double getMax()
		{
			return max;
		}
		
		public void setMax( double max )
		{
			this.max = max;
		}
		
		public double getX()
		{
			return x;
		}
		
		public void setX( double x )
		{
			this.x = x;
		}
		
		public double getY()
		{
			return y;
		}
		
		public void setY( double y )
		{
			this.y = y;
		}
	}
	
	private class ObjectRowData
	{
		private int index;
		private int min;
		private int max;
		private int seedx;
		private int seedy;
		private int seedz;
		private int classNum;
		
		public ObjectRowData( int index, int min, int max, int seedx, int seedy, int seedz, int classNum )
		{
			this.index = index;
			this.min = min;
			this.max = max;
			this.seedx = seedx;
			this.seedy = seedy;
			this.seedz = seedz;
			this.classNum = classNum;
		}

		public int getIndex()
		{
			return index;
		}
		
		public void setIndex( int index )
		{
			this.index = index;
		}
		
		public int getMin()
		{
			return min;
		}
		
		public void setMin( int min )
		{
			this.min = min;
		}
		
		public int getMax()
		{
			return max;
		}
		
		public void setMax( int max )
		{
			this.max = max;
		}
		
		public int getSeedX()
		{
			return seedx;
		}
		
		public void setSeedX( int seedx )
		{
			this.seedx = seedx;
		}
		
		public int getSeedY()
		{
			return seedy;
		}
		
		public void setSeedY( int seedy )
		{
			this.seedy = seedy;
		}

		public int getSeedZ()
		{
			return seedz;
		}
		
		public void setSeedZ( int seedz )
		{
			this.seedz = seedz;
		}

		public int getClassNum()
		{
			return classNum;
		}
		
		public void setClassNum( int classNum )
		{
			this.classNum = classNum;
		}
	}
	
	private class FileNameFilter extends javax.swing.filechooser.FileFilter {
		String fileExtension;
		String description;
		
		public FileNameFilter(String fileExtension, String description) {
			this.fileExtension = fileExtension;
			this.description = description;
		}
		public boolean accept(File file) {
			String fileName = file.getName();
			return fileName.endsWith("." + fileExtension);
		}
		public String getDescription() {
			return description;
		}
		public String getExtension() {
			return fileExtension;
		}
	}
	
    private void loadFuzzyImage() {
		IJ.run("Select None");
		IJ.run("Duplicate...", "title=fuzzy_stack duplicate");
		//IJ.run("8-bit");
		if (Boolean.parseBoolean(Prefs.get( PREFS_USE_MEDIAN_FILTER_FLAG, null )) == true) {
			IJ.run("Median...", "radius=3 stack");
		}
		impFuzzy = WindowManager.getCurrentImage();
		impFuzzy.hide();
		IJ.selectWindow(windowTitle);
	}
    
    private void drawFuzzyRoi(Color color) {
		//if (impFuzzy != null && frame != null) { 
		if (frame != null) { 
			Roi roi = createFuzzyROI( frame.xSlider.getValue(), frame.ySlider.getValue(), frame.minLevelSlider.getValue(), frame.maxLevelSlider.getValue(), color );
			imp.setRoi( roi );
			imp.setActivated();
		}
    }

    public static Gebiss_ getInstance() {
        return gebiss;
    }

    public void addROIs(int[][] x, int[][] y, double[][] min, double[][] max, int[] roisPerSlice) 
    {
    	frame.x = new int[roisPerSlice.length][];
    	frame.y = new int[roisPerSlice.length][];
    	frame.min = new double[roisPerSlice.length][];
    	frame.max = new double[roisPerSlice.length][];
    	frame.roisPerSlice = new int[roisPerSlice.length];
    	
        for (int z = 0; z < roisPerSlice.length; z++)	// scans all z slices
        {      
        	frame.x[z] = new int[roisPerSlice[z]];
        	frame.y[z] = new int[roisPerSlice[z]];
        	frame.min[z] = new double[roisPerSlice[z]];
        	frame.max[z] = new double[roisPerSlice[z]];
        	frame.roisPerSlice[z] = roisPerSlice[z];

	        for (int j = 0; j < roisPerSlice[z]; j++)	// scans all ROIs on the slice
	        {      
				frame.x[z][j] = x[z][j];
				frame.y[z][j] = y[z][j];
				frame.min[z][j] = min[z][j];
				frame.max[z][j] = max[z][j];

	        	if (z != roisPerSlice.length-1) continue;

	        	//if (x[z][j]==0 && y[z][j]==0 && min[z][j]==0 && max[z][j]==0) continue;
	        	//System.out.println("z: "+z+" j: "+j+" x: "+x[z][j]+" y: "+y[z][j]+" min: "+min[z][j]+" max: "+max[z][j]);                                
				Roi roi = createROI( x[z][j], y[z][j], (int) min[z][j], (int) max[z][j] );
				ResultTableModel model = ( ResultTableModel )frame.table.getModel();
				int roiNumber = 1;
				if ( frame.table.getModel().getRowCount() >0 )
				{
					roiNumber = Integer.parseInt( ( ( Roi )model.getValueAt( model.getRowCount() - 1, 0 ) ).getName() ) + 1;
				}
				roi.setName( frame.roiNF.format( roiNumber ) );
				model.addRow( new RowData( roi, (int) min[z][j], (int) max[z][j], x[z][j], y[z][j] ) );
	        }

			frame.roiTableBar.setValue(roisPerSlice.length-1);
        }

        IJ.selectWindow("original");      
        frame.updateROIManager();
		ImageCanvas ic = imp.getCanvas();
		ic.setShowAllROIs( true );
		imp.draw();
		frame.showAllROIsButton.setText(frame.HIDE_ALL_ROIS_TEXT);

        IJ.selectWindow(windowTitle);
    }
    
    private void doExit() {
		int index = jtp.getSelectedIndex();
		if (index == 0) frame.saveParameter();
		if (index == 1) frameObjectGT.saveParameter();

		imp = null;
		impFuzzy.close();
		impFuzzy = null;
		ip = null;
		canUpdateROI = true;
		showLoadParameter = true;
		debug = false;
		resetRoiList = false;
		windowTitle = null;

		jtp = null;
		frameObjectGT.setVisible( false );
		//frameObjectGT.dispose();
		frameObjectGT = null;
		frameObjectSubset.setVisible( false );
		//frameObjectSubset.dispose();
		frameObjectSubset = null;
		//frameTop = null;
		frameBench.setVisible( false );
		//frameBench.dispose();
		frameBench = null;
		frame.setVisible( false );
		frame = null;

		RoiManager myRoiManager = RoiManager.getInstance();
		if ( myRoiManager != null )
		{
			myRoiManager.close();
		}
    }
}
