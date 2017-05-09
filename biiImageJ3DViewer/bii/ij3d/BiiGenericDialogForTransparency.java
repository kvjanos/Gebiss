package bii.ij3d;

import java.awt.Frame;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.KeyEvent;

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import bii.ij3d.Executer.SliderAdjuster;

public class BiiGenericDialogForTransparency extends GenericDialog {

	private Executer.SliderAdjuster transp_adjuster;
	private Content c;
	private Image3DUniverse univ;

	public BiiGenericDialogForTransparency(String title, Executer.SliderAdjuster transp_adjuster, Content c, Image3DUniverse univ) {
		super(title);
		this.transp_adjuster = transp_adjuster;
		this.c = c;
		this.univ = univ;
	}

	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode(); 
		IJ.setKeyDown(keyCode); 
		if (keyCode==KeyEvent.VK_ENTER && textArea1==null) {
			if(!transp_adjuster.go)
				transp_adjuster.start();
			TextField input = (TextField)e.getSource();
			String text = input.getText();
			try {
				int value = Integer.parseInt(text);
				transp_adjuster.exec(value, c, univ);
				Prefs.set("segmentingassistant.transparency", value);
			} catch (Exception exception) {
				// ignore intermediately invalid number
			}
		}
	}
}
