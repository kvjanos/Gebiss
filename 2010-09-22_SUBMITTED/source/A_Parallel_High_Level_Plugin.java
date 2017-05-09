import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.io.Opener;
import java.util.concurrent.atomic.AtomicInteger;

public class A_Parallel_High_Level_Plugin implements PlugIn {

    public void run(String arg) {

	final ImagePlus dot_blot = new Opener().openURL("http://rsb.info.nih.gov/ij/images/Dot_Blot.jpg");


	final int starting_threshold = 190;
	final int ending_threshold = 255;
	final int n_tests = ending_threshold - starting_threshold + 1;
	final AtomicInteger ai = new AtomicInteger(starting_threshold);

	// store all result images here
	final ImageProcessor[] results = new ImageProcessor[n_tests];

	final Thread[] threads = newThreadArray();

	for (int ithread = 0; ithread < threads.length; ithread++) {

	    // Concurrently run in as many threads as CPUs

	    threads[ithread] = new Thread() {

	        public void run() {

		    // Each thread processes a few items in the total list
		    // Each loop iteration within the run method has a unique 'i' number to work with
		    // and to use as index in the results array:

		    for (int i = ai.getAndIncrement(); i <= ending_threshold; i = ai.getAndIncrement()) {
		        // 'i' is the lower bound of the threshold window
			ImageProcessor ip = dot_blot.getProcessor().duplicate();
			ip.setMinAndMax(i, 255);
		    	ImagePlus imp = new ImagePlus("Threshold " + i, ip);
			// Run the plugins on the new image:
			IJ.run(imp, "Convert to Mask", "");
			IJ.run(imp, "Analyze Particles...", "size=800-20000 circularity=0.00-1.00 show=Outlines");
			// The above results in a newly opened image, with the unique name "Drawing of Threshold " + i
			// cleanup:
			imp.flush();
			// Capture and store resulting image (WindowManager.getImage is a synchronized, thread-safe method)
			ImagePlus res = WindowManager.getImage("Drawing of Threshold " + i);
			results[i] = res.getProcessor();
			res.getWindow().setVisible(false);
		    }
		}
	    };
	}

	startAndJoin(threads);

        // now the results array is full. Just show them in a stack:
	final ImageStack stack = new ImageStack(dot_blot.getWidth(), dot_blot.getHeight());
	for (int i=0; i< results.length; i++) {
            stack.addSlice(Integer.toString(i), results[i]);
	}
        new ImagePlus("Results", stack).show();
    }

    /** Create a Thread[] array as large as the number of processors available.
    * From Stephan Preibisch's Multithreading.java class. See:
    * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
    */
    private Thread[] newThreadArray() {
        int n_cpus = Runtime.getRuntime().availableProcessors();
    	return new Thread[n_cpus];
    }

    /** Start all given threads and wait on each of them until all are done.
    * From Stephan Preibisch's Multithreading.java class. See:
    * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
    */
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

