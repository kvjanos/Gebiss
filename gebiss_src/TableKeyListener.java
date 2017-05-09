import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

public class TableKeyListener implements KeyListener {
	
	private JTable table;
	
	public TableKeyListener(JTable table) {
		this.table = table;
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_F2) {
			try {
	    		table.editCellAt(table.getSelectedRow(), table.getSelectedColumn()); 
		      	JTextComponent textComp = (JTextField)table.getEditorComponent();
		      	Highlighter.HighlightPainter myHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(textComp.getSelectionColor());
		      	Highlighter hilite = textComp.getHighlighter();
		      	Document doc = textComp.getDocument();
		      	String text = doc.getText(0, doc.getLength());
				hilite.addHighlight(0, text.length(), myHighlightPainter);
				e.consume();
			} catch (BadLocationException ble) {}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		table.editCellAt(table.getSelectedRow(), table.getSelectedColumn()); 
      	JTextComponent textComp = (JTextField)table.getEditorComponent();
		Highlighter hilite = textComp.getHighlighter();
		Highlighter.Highlight[] hilites = hilite.getHighlights();
 
		for (int i = 0; i < hilites.length; i++) {
			if (hilites[i].getPainter() instanceof DefaultHighlighter.DefaultHighlightPainter) {
				textComp.setText("");
				textComp.requestFocus();
				break;
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		keyReleased(e);
	}
}
