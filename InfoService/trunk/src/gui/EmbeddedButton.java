package gui;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class EmbeddedButton extends JButton {
  
  private boolean m_hasFocus;
    
  public EmbeddedButton() {
    setBorderPainted(false);
    setContentAreaFilled(false);
    setFocusPainted(false);
    m_hasFocus = false;
    final EmbeddedButton buttonInstance = this;
    addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent a_focusEvent) {
        synchronized (buttonInstance) {
          setBorderPainted(true);
          m_hasFocus = true;
        }
      }
      public void focusLost(FocusEvent a_focusEvent) {
        synchronized (buttonInstance) {
          if (getModel().isRollover() == false) {
            setBorderPainted(false);
          }
          m_hasFocus = false;
        }
      }
    });
    addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent a_changeEvent) {
        synchronized (buttonInstance) {
          ButtonModel buttonModel = getModel();
          if (buttonModel.isPressed() == true) {
            setBorderPainted(true);
            setContentAreaFilled(true);
          }
          else {
            if (buttonModel.isRollover() == true) {
              setBorderPainted(true);
              setContentAreaFilled(false);
            }
            else {
              if (m_hasFocus == false) {
                setBorderPainted(false);
              }
              else {
                setBorderPainted(true);
              }
              setContentAreaFilled(false);
            }
          }
        }
      }
    });
  } 
  
  
  public EmbeddedButton(String a_buttonLabel) {
    this();
    this.setText(a_buttonLabel);
  }

}