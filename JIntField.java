import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class JIntField extends JTextField {

    public JIntField() {
    }

    public JIntField(String string) {
	super(string);
    }

    protected Document createDefaultModel() {
	return new IntDocument();
    }

    static class IntDocument extends PlainDocument {
	
	public void insertString(
	    int          offSet,
	    String       string,
	    AttributeSet attributeSet)
	
	    throws BadLocationException {
	     
	    if (string == null) {
		return;
	    }
	    boolean intError = false;
	    try{
		Integer.valueOf(string);
	    } catch(Exception e) {
		intError = true;
	    }
	    if (!intError) super.insertString(offSet,string,attributeSet);
	}
    }
}
