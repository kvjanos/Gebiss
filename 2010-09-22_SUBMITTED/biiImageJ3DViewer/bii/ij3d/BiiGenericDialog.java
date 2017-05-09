package bii.ij3d;

import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.KeyEvent;

import ij.IJ;
import ij.gui.GenericDialog;
import bii.ij3d.Executer.SliderAdjuster;

public class BiiGenericDialog extends GenericDialog {

	private Executer.SliderAdjuster thresh_adjuster;
	private Content c;
	private Image3DUniverse univ;
	
	public BiiGenericDialog(String title, Executer.SliderAdjuster thresh_adjuster, Content c, Image3DUniverse univ) {
		super(title);
		this.thresh_adjuster = thresh_adjuster;
		this.c = c;
		this.univ = univ;
	}

	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode(); 
		IJ.setKeyDown(keyCode); 
		if (keyCode==KeyEvent.VK_ENTER && textArea1==null) {
			if(!thresh_adjuster.go)	thresh_adjuster.start();

			Object source = e.getSource();
			for (int i=0; i<numberField.size(); i++) {
				if (source==numberField.elementAt(i)) {
					TextField tf = (TextField)numberField.elementAt(i);
					thresh_adjuster.exec(Integer.parseInt(tf.getText()), c, univ);
					//System.out.println("tf.setText: " + tf.getText());
				}
			}
			
		} 
	}
}
