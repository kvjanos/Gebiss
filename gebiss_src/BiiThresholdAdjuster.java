

import ij.plugin.frame.ThresholdAdjuster;

/**
 *
 * @author janoskv
 */
public class BiiThresholdAdjuster extends ThresholdAdjuster {

    public boolean done;
    public void close() {
            super.close();
            done = true;
	}

}
