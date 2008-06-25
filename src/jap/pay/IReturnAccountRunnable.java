package jap.pay;

import gui.dialog.WorkerContentPane;
import anon.pay.PayAccount;

interface IReturnAccountRunnable extends WorkerContentPane.IReturnRunnable
{
	public PayAccount getAccount();
}