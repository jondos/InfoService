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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.MediaTracker;
import java.awt.Toolkit;
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
import org.w3c.dom.Document;
import HTTPClient.Codecs;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPCertificateException;
import gui.SimpleFileFilter;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final public class JAPUtil
{
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

	public static boolean isPort(int port)
	{
		if ( (port < 1) || (port > 65536))
		{
			return false;
		}
		return true;
	}

	/** Writes a XML-Document to an Output-Stream. Since writing was not standardzieds
	 * since JAXP 1.1 different Methods are tried
	 */
	public static String XMLDocumentToString(Document doc)
	{
		ByteArrayOutputStream out = null;
		try
		{
			out = new ByteArrayOutputStream();
			try //For JAXP 1.0.1 Referenc Implementation (shipped with JAP)
			{
				( (com.sun.xml.tree.XmlDocument) doc).write(out);
			}
			catch (Throwable t1)
			{
				try
				{ //For JAXP 1.1 (for Instance Apache Crimson/Xalan shipped with Java 1.4)
					//This seams to be realy stupid and compliecated...
					//But if the do a simple t.transform(), a NoClassDefError is thrown, if
					//the new JAXP1.1 is not present, even if we NOT call saveXMLDocument, but
					//calling any other method within JAPUtil.
					//Dont no why --> maybe this has something to to with Just in Time compiling ?
					Object t =
						javax.xml.transform.TransformerFactory.newInstance().newTransformer();
					javax.xml.transform.Result r = new javax.xml.transform.stream.StreamResult(out);
					javax.xml.transform.Source s = new javax.xml.transform.dom.DOMSource(doc);

					//this is to simply invoke t.transform(s,r)
					Class c = t.getClass();
					Method m = null;
					Method[] ms = c.getMethods();
					for (int i = 0; i < ms.length; i++)
					{
						if (ms[i].getName().equals("transform"))
						{
							m = ms[i];
							Class[] params = m.getParameterTypes();
							if (params.length == 2)
							{
								break;
							}
						}
					}
					Object[] p = new Object[2];
					p[0] = s;
					p[1] = r;
					m.invoke(t, p);
				}
				catch (Throwable t2)
				{
					return null;
				}
			}
		}
		catch (Throwable t2)
		{
			return null;
		}
		return out.toString();
	}

	/** Loads an Image from a File or a Resource.
	 *	@param strImage the Resource or filename of the Image
	 *	@param sync true if the loading is synchron, false if it should be asynchron
	 */
	public static ImageIcon loadImageIcon(String strImage, boolean sync)
	{
		ImageIcon img = null;

		// get color depth
		int colordepth = Toolkit.getDefaultToolkit().getColorModel().getPixelSize();

		String imageFilename;
		// try loading the lowcolor images
		if (colordepth <= 16)
		{
			imageFilename = JAPConstants.IMGPATHLOWCOLOR + strImage;
			try
			{
				// this is necessary to make shure that the images are loaded when contained in a JAP.jar
				img = new ImageIcon(Class.forName("JAP").getResource(imageFilename));
			}
			catch (Exception e)
			{
				try
				{
					//we have to chek, if the file exist, because new ImageIcon(String) will always success!!!
					if ( (new File(imageFilename)).canRead())
					{
						img = new ImageIcon(imageFilename);
					}
				}
				catch (Exception e1)
				{
				}
			}
		}
		// if loading of lowcolor images was not successful or
		//    we have to load the hicolor images
		if (img == null)
		{
			imageFilename = JAPConstants.IMGPATHHICOLOR + strImage;
			try
			{
				// this is necessary to make shure that the images are loaded when contained in a JAP.jar
				img = new ImageIcon(Class.forName("JAP").getResource(imageFilename));
			}
			catch (Exception e)
			{
				img = new ImageIcon(imageFilename);
			}
		}

		if (sync && img != null)
		{
			int statusBits = MediaTracker.ABORTED | MediaTracker.ERRORED | MediaTracker.COMPLETE;
			for (; ; )
			{
				int status = img.getImageLoadStatus();
				if ( (status & statusBits) != 0)
				{
					break;
				}
				else
				{
					Thread.yield();
				}
			}
		}
		return img;
	}

	/** Loads an Ressource from a File or a Archive-Resource.
	 *	@param strRessource the Resourcename or Filename
	 *  @return null if Ressource could not be loaded
	 *  @return contents of the ressource otherwise
	 */
	public static byte[] loadRessource(String strRessource)
	{
		try
		{
			InputStream in = null;
			int len = 0;
			try
			{
				// this is necessary to make shure that the images are loaded when contained in a JAP.jar
				in = Class.forName("JAP").getResourceAsStream(strRessource);
				len = in.available();
			}
			catch (Exception e)
			{
				in = null;
			}
			if (in == null)
			{
				try
				{
					//we have to chek, if the file exist, because new new File will always success!!!
					File f = new File(strRessource);
					if (f.canRead())
					{
						in = new FileInputStream(f);
						len = (int) f.length();
					}
					else
						return null;
				}
				catch (Exception e1)
				{
					return null;
				}
			}
			byte[] tmp = new byte[len];
			if (in.read(tmp) != len)
			{
				return null;
			}
			return tmp;
		}
		catch (Throwable t)
		{
			return null;
		}
	}

	public static void centerFrame(Window f)
	{
		Dimension screenSize = f.getToolkit().getScreenSize();
		try //JAVA 1.1
		{
			Dimension ownSize = f.getSize();
			f.setLocation( (screenSize.width - ownSize.width) / 2, (screenSize.height - ownSize.height) / 2);
		}
		catch (Error e) //JAVA 1.0.2
		{
			Dimension ownSize = f.size();
			f.locate( (screenSize.width - ownSize.width) / 2, (screenSize.height - ownSize.height) / 2);
		}
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
		dialog.show();
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

	public static String getProxyAuthorization(String user, String passwd)
	{
		String tmpPasswd = Codecs.base64Encode(user + ":" + passwd);
		return "Proxy-Authorization: Basic " + tmpPasswd + "\r\n";
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

	public static JAPCertificate openCertificate(Window jf)
	{
		File file = showFileDialog(jf).getSelectedFile();
		JAPCertificate t_cert = null;
		if (file != null)
		{
			try
			{
				byte[] buff = new byte[ (int) file.length()];
				FileInputStream fin = new FileInputStream(file);
				fin.read(buff);
				fin.close();

				t_cert = JAPCertificate.getInstance(buff);
			}
			catch (Exception e1)
			{
				t_cert = null;
			}
		}
		return t_cert;
	}
}
