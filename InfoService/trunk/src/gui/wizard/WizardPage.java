package gui.wizard;

/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */

public interface WizardPage
{
// called when this page is current
void activated (WizardHost host);
// called when this page is no longer current
void deactivated (WizardHost host);
// JComponent getPageComponent(WizardHost host);
}