package gui.wizard;

import java.awt.Dialog;
/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation: TUD
 * @author Jens
 * @version 1.0
 */

public interface Wizard
{
//determine Number and Order of the WizardPages
 WizardPage invokeWizard(WizardHost host);
//user's clicked finish --> do the work
 void finish(WizardPage currentPage, WizardHost host);
//user's clicked next --> return next WizardPage
 WizardPage next(WizardPage currentPage, WizardHost host);
//user's clicked help --> make Help Dialog
 void help(WizardPage currentPage, WizardHost host);
//get the number of total steps
 void initTotalSteps(int totalSteps);
//user's clicked finish or cancel, wizard has completed
 void wizardCompleted();
// String getWizardTitle();
}