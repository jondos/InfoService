/*
 Copyright (c) 2000, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
  may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package jap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import anon.crypto.JAPCertificate;
import gui.SimpleFileFilter;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import gui.GUIUtils;
import gui.*;

/**
 * This class contains static utility functions for Jap
 */
public final class JAPUtil
{

	/** Returns the desired unit for this amount of Bytes (Bytes, kBytes, MBytes,GBytes)*/
	public static String formatBytesValueOnlyUnit(long c)
	{
		if (c < 10000)
		{
			return JAPMessages.getString("Byte");
		}
		else if (c < 1000000)
		{
			return JAPMessages.getString("kByte");
		}
		else if (c < 1000000000)
		{
			return JAPMessages.getString("MByte");
		}
		return JAPMessages.getString("GByte");
	}

	/** Returns a formated number which respects different units (Bytes, kBytes, MBytes, GBytes)*/
	public static String formatBytesValueWithoutUnit(long c)
	{
		DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(JAPController.getLocale());
		double d = c;
		if (c < 10000)
		{
			df.applyPattern("#,####");
		}
		else if (c < 1000000)
		{
			d /= 1000.0;
			df.applyPattern("#,##0.0");
		}
		else if (c < 1000000000)
		{
			d /= 1000000.0;
			df.applyPattern("#,##0.00");
		}
		else
		{
			d /= 1000000000.0;
			df.applyPattern("#,##0.000");
		}
		return df.format(d);
	}

	/**
	 * Formats a number of bytes in human-readable kB/MB/GB format
	 * with 2 decimal places
	 *
	 * @param bytes long number of bytes
	 * @return String the formatted string
	 */
	public static String formatBytesValue(long bytes)
	{
		return formatBytesValueWithoutUnit(bytes) + " " + formatBytesValueOnlyUnit(bytes);
	}

	public static int applyJarDiff(File fileOldJAR, File fileNewJAR,
								   byte[] diffJAR)
	{
		try
		{
			ZipFile zold = null;
			ZipInputStream zdiff = null;
			ZipOutputStream znew = null;
			ZipEntry ze = null;
			// geting old names
			zold = new ZipFile(fileOldJAR);
			Hashtable oldnames = new Hashtable();
			Enumeration e = zold.entries();
			while (e.hasMoreElements())
			{
				ze = (ZipEntry) e.nextElement();
				oldnames.put(ze.getName(), ze.getName());
			}
			// it shouldn't be a FileStream but an ByteArrayStream or st like that
			//zdiff=new ZipInputStream(new FileInputStream(diffJAR));
			zdiff = new ZipInputStream(new ByteArrayInputStream(diffJAR));
			znew = new ZipOutputStream(new FileOutputStream(fileNewJAR));
			znew.setLevel(9);
			byte[] b = new byte[5000];
			while ( (ze = zdiff.getNextEntry()) != null)
			{
				ZipEntry zeout = new ZipEntry(ze.getName());
				if (!ze.getName().equalsIgnoreCase("META-INF/INDEX.JD"))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: " + ze.getName());
					oldnames.remove(ze.getName());
					int s = -1;
					zeout.setTime(ze.getTime());
					zeout.setComment(ze.getComment());
					zeout.setExtra(ze.getExtra());
					zeout.setMethod(ze.getMethod());
					if (ze.getSize() != -1)
					{
						zeout.setSize(ze.getSize());
					}
					if (ze.getCrc() != -1)
					{
						zeout.setCrc(ze.getCrc());
					}
					znew.putNextEntry(zeout);
					while ( (s = zdiff.read(b, 0, 5000)) != -1)
					{
						znew.write(b, 0, s);

					}
					znew.closeEntry();
				}
				else
				{
					BufferedReader br = new BufferedReader(new InputStreamReader(zdiff));
					String s = null;
					while ( (s = br.readLine()) != null)
					{
						StringTokenizer st = new StringTokenizer(s);
						s = st.nextToken();
						if (s.equalsIgnoreCase("remove"))
						{
							s = st.nextToken();
							LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: remove " + s);
							oldnames.remove(s);
						}
						else if (s.equalsIgnoreCase("move"))
						{
							LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: move " + st.nextToken());
						}
						else
						{
							LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: unkown: " + s);
						}
					}
				}
				zdiff.closeEntry();
			}
			e = oldnames.elements();
			while (e.hasMoreElements())
			{
				String s = (String) e.nextElement();
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, s);
				ze = zold.getEntry(s);
				ZipEntry zeout = new ZipEntry(ze.getName());
				zeout.setTime(ze.getTime());
				zeout.setComment(ze.getComment());
				zeout.setExtra(ze.getExtra());
				zeout.setMethod(ze.getMethod());
				if (ze.getSize() != -1)
				{
					zeout.setSize(ze.getSize());
				}
				if (ze.getCrc() != -1)
				{
					zeout.setCrc(ze.getCrc());
				}
				znew.putNextEntry(zeout);
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: Getting in..");
				InputStream in = zold.getInputStream(ze);
				int l = -1;
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: Reading..");
				try
				{
					while ( (l = in.read(b, 0, 5000)) != -1)
					{
						znew.write(b, 0, l);
					}
				}
				catch (Exception er)
				{
					er.printStackTrace(System.out);
				}
				in.close();
				znew.closeEntry();

			}

			znew.finish();
			znew.flush();
			znew.close();
			zold.close();
			zdiff.close();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			return -1;
		}
		return 0;
	}

	/** Loads an Image from a File or a Resource.
	 *	@param strImage the Resource or filename of the Image
	 *	@param sync true if the loading is synchron, false if it should be asynchron
	 * @deprecated use GUIUtils.loadImageIcon instead
	 */
	public static ImageIcon loadImageIcon(String strImage, boolean sync)
	{
		return GUIUtils.loadImageIcon(strImage, sync);
	}

	/**
	 *
	 * @param f Window
	 * @deprecated use GUIUtils.centerFrame(Window) instead
	 */
	public static void centerFrame(Window f)
	{
		GUIUtils.centerOnScreen(f);
	}

	public static void upRightFrame(Window f)
	{
		Dimension screenSize = f.getToolkit().getScreenSize();
		Dimension ownSize = f.getSize();
		f.setLocation( (screenSize.width - ownSize.width), 0);
	}

	public static void showMessageBox(JFrame parent, String messageID, String titleID,
									  int jOptionPaneMessageType)
	{
		JOptionPane pane = new JOptionPane(JAPMessages.getString(messageID), jOptionPaneMessageType);
		JDialog dialog = pane.createDialog(parent, JAPMessages.getString(titleID));
		dialog.setFont(JAPController.getDialogFont());
		dialog.setVisible(true);
	}

	/** Sets the mnemonic charcter of a component. The character must be set
	 *  in the properties file under a name that is given in mnPropertyString.
	 */
	public static void setMnemonic(AbstractButton bt, String mn)
	{
		if ( (bt == null) || (mn == null) || (mn.equals("")))
		{
			return;
		}
		bt.setMnemonic(mn.charAt(0));
	}

	public static void setPerfectTableSize(JTable table, Dimension maxDimension)
	{
		TableModel tableModel = table.getModel();
		int perfectWidth = 0;
		int perfectHeight = 0;
		// the Table uses the minimum height to draw itself, weird...
		// so we set the perfect heigt as the smallest column height
		int minimunColunmHeight = 0;
		for (int i = 0; i < tableModel.getColumnCount(); i++)
		{
			TableColumn column = table.getColumnModel().getColumn(i);
			TableCellRenderer headerRenderer = column.getHeaderRenderer();
			int headerWidth = column.getPreferredWidth();
			int columnHeight = 0;
			if (headerRenderer != null)
			{
				Component component = headerRenderer.getTableCellRendererComponent(null,
					column.getHeaderValue(), false, false, 0, 0);
				headerWidth = component.getPreferredSize().width;
				columnHeight = component.getPreferredSize().height;
			}
			if (tableModel.getRowCount() > 0)
			{
				// look at every entry
				TableCellRenderer tableCellRenderer = table.getDefaultRenderer(tableModel.getColumnClass(i));
				int cellWidth = 0;
				for (int row = 0; row < tableModel.getRowCount(); row++)
				{
					Object object = tableModel.getValueAt(row, i);
					Component component = tableCellRenderer.getTableCellRendererComponent(table, object, false, false,
						row, i);
					cellWidth = Math.max(cellWidth, component.getPreferredSize().width);
					columnHeight += component.getPreferredSize().height;
				}
				int preferredColumnWidth = Math.max(headerWidth, cellWidth);
				column.setPreferredWidth(preferredColumnWidth);
				perfectWidth += preferredColumnWidth;
				if (minimunColunmHeight == 0)
				{
					minimunColunmHeight = columnHeight;
				}
				else
				{
					minimunColunmHeight = Math.min(minimunColunmHeight, columnHeight);
				}
			}
		}
		// add some space for scrollbar,... (+ 30)
		perfectWidth = Math.min(maxDimension.width, perfectWidth + 30);
		perfectHeight = Math.min(maxDimension.height, minimunColunmHeight);
		table.setPreferredScrollableViewportSize(new Dimension(perfectWidth, perfectHeight));
	}

	public static String readLine(InputStream inputStream) throws Exception
	{
		String returnString = "";
		try
		{
			int byteRead = inputStream.read();
			while (byteRead != 10 && byteRead != -1)
			{
				if (byteRead != 13)
				{
					returnString += (char) byteRead;
				}
				byteRead = inputStream.read();
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		return returnString;
	}

	public static JFileChooser showFileDialog(Window jf)
	{
		SimpleFileFilter active = null;
		JFileChooser fd2 = new JFileChooser();
		fd2.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd2.addChoosableFileFilter(active = new SimpleFileFilter());
		if (active != null)
		{
			fd2.setFileFilter(active);
		}
		fd2.setFileHidingEnabled(false);
		fd2.showOpenDialog(jf);
//										File m_fileCurrentDir = fd2.getCurrentDirectory();
		return fd2;
	}

	/** Shows a file open dialog and tries to read a certificate. Returns null, if the user canceld
	 * the open request. Throws IOException if certificate could not be readed or decoded.
	 */

	public static JAPCertificate openCertificate(Window jf) throws IOException
	{
		File file = showFileDialog(jf).getSelectedFile();
		JAPCertificate t_cert = null;
		if (file != null)
		{
			t_cert = JAPCertificate.getInstance(file);
			if (t_cert == null)
			{
				throw new IOException("Could not create certificate!");
			}
		}
		return t_cert;
	}

	/**
	 * formats a timestamp in an easily readable format.
	 * @param date Timestamp
	 * @param withTime boolean if true, the date+time is returned, otherwise date only.
	 */
	public static String formatTimestamp(Timestamp date, boolean withTime)
	{
		return formatTimestamp(date, withTime, null);
	}

	public static String formatTimestamp(Timestamp date, boolean withTime, String a_language)
	{
		SimpleDateFormat sdf;
		if (a_language.equalsIgnoreCase("en"))
		{
			if (withTime)
			{
				sdf = new SimpleDateFormat("dd/MM/yy - HH:mm");
			}
			else
			{
				sdf = new SimpleDateFormat("dd/MM/yy");
			}
			return sdf.format(date);
		}
		else
		{
		if (withTime)
		{
			sdf = new SimpleDateFormat("dd.MM.yyyy - HH:mm");
		}
		else
		{
			sdf = new SimpleDateFormat("dd.MM.yyyy");
		}
		return sdf.format(date);
		}
	}

	/**
	 * Since JDK 1.1.8 does not provide String.replaceAll(),
	 * this is an equivalent method.
	 */
	public static String replaceAll(String a_source, String a_toReplace, String a_replaceWith)
	{
		int position;

		while ( (position = a_source.indexOf(a_toReplace)) != -1)
		{
			int position2 = a_source.indexOf(a_replaceWith);
			if (a_replaceWith.indexOf(a_toReplace) != -1)
			{
				position2 += a_replaceWith.indexOf(a_toReplace);
			}
			if (position == position2)
			{
				break;
			}
			String before = a_source.substring(0, position);
			String after = a_source.substring(position + a_toReplace.length(), a_source.length());
			a_source = before + a_replaceWith + after;
		}

		return a_source;
	}
}
