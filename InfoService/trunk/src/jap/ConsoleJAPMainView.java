package jap;

import javax.swing.JPanel;
import java.awt.Window;

import logging.*;

/**
 *
 * @author Rolf Wendolsky
 */
public class ConsoleJAPMainView implements IJAPMainView
{
	/**
	 * addStatusMsg
	 *
	 * @param msg String
	 * @param type int
	 * @param bAutoRemove boolean
	 * @return int
	 * @todo Diese gui.IStatusLine-Methode implementieren
	 */
	public int addStatusMsg(String msg, int type, boolean bAutoRemove)
	{
		LogHolder.log(LogLevel.ALERT, LogType.MISC, msg);
		return 0;
	}

	public void valuesChanged(boolean bSync)
	{
	}

	/**
	 * channelsChanged
	 *
	 * @param channels int
	 * @todo Diese anon.proxy.IProxyListener-Methode implementieren
	 */
	public void channelsChanged(int channels)
	{
	}

	public void packetMixed(long a_totalBytes)
	{
	}

	public void connectionError()
	{
		LogHolder.log(LogLevel.ALERT, LogType.NET, "Disconnected because of connection error!");
	}

	/**
	 * connectionEstablished
	 *
	 * @todo Diese anon.AnonServiceEventListener-Methode implementieren
	 */
	public void connectionEstablished()
	{
		LogHolder.log(LogLevel.ALERT, LogType.NET, "Connected!");
	}

	/**
	 * create
	 *
	 * @param bWithPay boolean
	 * @todo Diese jap.IJAPMainView-Methode implementieren
	 */
	public void create(boolean bWithPay)
	{
	}

	/**
	 * disableSetAnonMode
	 *
	 * @todo Diese jap.IJAPMainView-Methode implementieren
	 */
	public void disableSetAnonMode()
	{
	}

	/**
	 * doSynchronizedUpdateValues
	 *
	 * @todo Diese jap.IJAPMainView-Methode implementieren
	 */
	public void doSynchronizedUpdateValues()
	{
	}

	/**
	 * getMainPanel
	 *
	 * @return JPanel
	 * @todo Diese jap.IJAPMainView-Methode implementieren
	 */
	public JPanel getMainPanel()
	{
		return null;
	}

	/**
	 * localeChanged
	 *
	 * @todo Diese jap.IJAPMainView-Methode implementieren
	 */
	public void localeChanged()
	{
	}

	/**
	 * registerViewIconified
	 *
	 * @param viewIconified Window
	 * @todo Diese jap.IJAPMainView-Methode implementieren
	 */
	public void registerViewIconified(Window viewIconified)
	{
	}

	/**
	 * removeStatusMsg
	 *
	 * @param id int
	 * @todo Diese gui.IStatusLine-Methode implementieren
	 */
	public void removeStatusMsg(int id)
	{
	}

	/**
	 * Called if some bytes are transferred.
	 *
	 * @param bytes either total amount or delta of transferred bytes
	 * @param protocolType the protocol to which the bytes are belonging
	 */
	public void transferedBytes(long bytes, int protocolType)
	{
	}
}
