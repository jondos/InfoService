package gui.wizard;

/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */

public interface WizardHost
{
//set the number of total steps of the Wizard
void setTotalSteps(int numberOfSteps);
//set all the Buttons enabled/disabled which are needed during the Wizard takes its Action
void setFinishEnabled(boolean enabled);
void setNextEnabled(boolean enabled);
void setCancelEnabled(boolean enabled);
void setBackEnabled(boolean enabled);
void setHelpEnabled(boolean enabled);
// what Type of is the WizardHost launched?
//JFrame getParentFrame();
}