package gui;

import javax.swing.JSlider;

public class JAPConfRoutingSlider extends JSlider {
  
  private boolean m_changeEventsEnabled;
  
  public JAPConfRoutingSlider() {
    super(0, 0, 0);
    setMinorTickSpacing(1);
    setMajorTickSpacing(1);    
    setPaintTicks(false);
    setSnapToTicks(true);    
    setPaintLabels(true);
    m_changeEventsEnabled = true;
  }
  
  
  public void setChangeEventsEnabled(boolean a_changeEventsEnabled) {
    synchronized (this) {
      m_changeEventsEnabled = a_changeEventsEnabled;
    }
  }
  
  
  protected void fireStateChanged() {
    synchronized (this) {
      if (m_changeEventsEnabled) {
        super.fireStateChanged();
      }
    }
  }

}