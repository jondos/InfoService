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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.MediaTracker;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.swing.ImageIcon;
import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;


final class JAPUtil
	{
		public static int applyJarDiff(String oldJAR, String newJAR, String diffJAR)
			{
				try
					{
						ZipFile zold=null;	
						ZipInputStream zdiff=null;
						ZipOutputStream znew=null;
						ZipEntry ze=null;
			// geting old names
						zold=new ZipFile(oldJAR);
						Hashtable oldnames=new Hashtable();
						Enumeration e=zold.entries();
						while(e.hasMoreElements())
							{
								ze=(ZipEntry)e.nextElement();
								oldnames.put(ze.getName(),ze.getName());
							}
						zdiff=new ZipInputStream(new FileInputStream(diffJAR));
						znew=new ZipOutputStream(new FileOutputStream(newJAR));		
						znew.setLevel(9);
						byte[] b=new byte[5000];
						while((ze=zdiff.getNextEntry())!=null)
							{
								ZipEntry zeout=new ZipEntry(ze.getName());
								if(!ze.getName().equalsIgnoreCase("META-INF/INDEX.JD"))
									{
										System.out.println(ze.getName());
										oldnames.remove(ze.getName());
										int s=-1;
										zeout.setTime(ze.getTime());
										zeout.setComment(ze.getComment());
										zeout.setExtra(ze.getExtra());
										zeout.setMethod(ze.getMethod());
										if(ze.getSize()!=-1)
											zeout.setSize(ze.getSize());
										if(ze.getCrc()!=-1)
											zeout.setCrc(ze.getCrc());
										znew.putNextEntry(zeout);
										while((s=zdiff.read(b,0,5000))!=-1)
											{
												znew.write(b,0,s);
											}
										znew.closeEntry();
									}
								else
									{
										BufferedReader br=new BufferedReader(new InputStreamReader(zdiff));
										String s=null;
										while((s=br.readLine())!=null)
											{
												StringTokenizer st=new StringTokenizer(s);
												s=st.nextToken();
												if(s.equalsIgnoreCase("remove"))
													oldnames.remove(st.nextToken());
												else if(s.equalsIgnoreCase("move"))
													System.out.println("move "+st.nextToken());
												else
													System.out.println("unkown: "+s);										
											}
									}
								zdiff.closeEntry();
							}
						e=oldnames.elements();
						while(e.hasMoreElements())
							{
								String s=(String)e.nextElement();
								System.out.println(s);
								ze=zold.getEntry(s);
								ZipEntry zeout=new ZipEntry(ze.getName());
								zeout.setTime(ze.getTime());
								zeout.setComment(ze.getComment());
								zeout.setExtra(ze.getExtra());
								zeout.setMethod(ze.getMethod());
								if(ze.getSize()!=-1)
									zeout.setSize(ze.getSize());
								if(ze.getCrc()!=-1)
									zeout.setCrc(ze.getCrc());
								znew.putNextEntry(zeout);
								System.out.println("Getting in..");
								InputStream in=zold.getInputStream(ze);
								int l=-1;
								System.out.println("Reading..");
								try{
								while((l=in.read(b,0,5000))!=-1)
									{
										znew.write(b,0,l);
								}}
								catch(Exception er)
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
			catch(Throwable e)
				{
					e.printStackTrace();
					return -1;
				}
			return 0;
		}
		
		
		
		
	public static boolean isPort(int port)
		{
			if((port<1)||(port>65536))
				return false;
			return true;
		}
	

	public static int parseNodeInt(Element parent,String name,int defaultValue)
		{
			int i=defaultValue;
			if(parent!=null)
				try	
					{
						NodeList nl=parent.getElementsByTagName(name);
						i=Integer.parseInt(nl.item(0).getFirstChild().getNodeValue());
					}
				catch(Exception e)
					{
					}
			return i;
		}

	public static int parseElementAttrInt(Element e,String attr,int defaultValue)
		{
			int i=defaultValue;
			if(e!=null)
				try	
					{
						Attr at=e.getAttributeNode(attr);
						i=Integer.parseInt(at.getValue());
					}
				catch(Exception ex)
					{
					}
			return i;
		}

	public static boolean parseNodeBoolean(Node n,boolean defaultValue)
		{
			boolean b=defaultValue;
			if(n!=null)
				try	
					{
						String tmpStr=n.getNodeValue();
						if(tmpStr.equalsIgnoreCase("true"))
							b=true;
						else if(tmpStr.equalsIgnoreCase("false"))
							b=false;
					}
				catch(Exception e)
					{
					}
			return b;
		}

	public static String parseNodeString(Node n,String defaultValue)
		{
			String s=defaultValue;
			if(n!=null)
				try	
					{
						s=n.getNodeValue();
					}
				catch(Exception e)
					{
					}
			return s;
		}
	/** Loads an Image from a File or a Resource.
	 *	@param strImage the Resource or filename of the Image
	 *	@param sync true if the loading is synchron, false if it should be asynchron
	 */
	public static ImageIcon loadImageIcon(String strImage, boolean sync) 
		{
			//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPModel:Image "+strImage+" loading...");
			boolean finished = false;
			ImageIcon img = null;
			// this is necessary to make shure that the images are loaded when contained in a JAP.jar
			try 
				{
					img = new ImageIcon(JAPUtil.class.getResource(strImage));
				}
			catch (Exception e)
				{
					img = null;
				}
			// ...otherwise
			if (img == null)
				{
					img = new ImageIcon(strImage);
				}
			if ((sync == false) || (img == null)) {
				finished = true;
			}
			while(finished!=true)
				{
					int status = img.getImageLoadStatus();
					if ( (status & MediaTracker.ABORTED) != 0 ) {
						//JAPDebug.out(JAPDebug.ERR,JAPDebug.GUI,"JAPModel:Loading of image "+strImage+" aborted!");
						finished = true;
						}
					if ( (status & MediaTracker.ERRORED) != 0 ) {
						//JAPDebug.out(JAPDebug.ERR,JAPDebug.GUI,"JAPModel:Error loading image "+strImage+"!");
						finished = true;
					}
					if ( (status & MediaTracker.COMPLETE) != 0) {
						finished = true;
					}
				}
		return img;
	}
	
	public static void centerFrame(Window f) {
		Dimension screenSize = f.getToolkit().getScreenSize();
		try //JAVA 1.1
			{
				Dimension ownSize = f.getSize();
				f.setLocation((screenSize.width-ownSize.width )/2,(screenSize.height-ownSize.height)/2);
			}
		catch(Error e) //JAVA 1.0.2
			{
				Dimension ownSize = f.size();
				f.locate((screenSize.width-ownSize.width )/2,(screenSize.height-ownSize.height)/2);
			}
	}

		public static void upRightFrame(Window f) 
			{
				Dimension screenSize = f.getToolkit().getScreenSize();
				Dimension ownSize = f.getSize();
				f.setLocation((screenSize.width-ownSize.width ) , 0 );
			}
	
    public static void setPerfectTableSize(JTable table,Dimension maxDimension) 
			{
				TableModel tableModel = table.getModel();
				int perfectWidth  = 0;
				int perfectHeight = 0;
				// the Table uses the minimum height to draw itself, weird...
				// so we set the perfect heigt as the smallest column height
				int minimunColunmHeight = 0;
				for (int i = 0; i < tableModel.getColumnCount(); i++)
					{
						TableColumn column  = table.getColumnModel().getColumn(i);
						TableCellRenderer headerRenderer=column.getHeaderRenderer();
						int headerWidth     = column.getPreferredWidth();
						int columnHeight    = 0;
						if(headerRenderer!=null)
							{
								Component component = headerRenderer.getTableCellRendererComponent(null,column.getHeaderValue(),false,false,0,0);
								headerWidth     = component.getPreferredSize().width;
								columnHeight    = component.getPreferredSize().height;
							}	
						if (tableModel.getRowCount() > 0)
							{			
								// look at every entry
								TableCellRenderer tableCellRenderer = table.getDefaultRenderer(tableModel.getColumnClass(i));
								int cellWidth = 0;
								for (int row = 0; row < tableModel.getRowCount(); row++) 
									{
										Object object        = tableModel.getValueAt(row,i);
										Component component						 = tableCellRenderer.getTableCellRendererComponent(table,object,false,false,row,i);	
										cellWidth            = Math.max(cellWidth,component.getPreferredSize().width);
										columnHeight        += component.getPreferredSize().height;
									}
								int preferredColumnWidth = Math.max(headerWidth,cellWidth);
								column.setPreferredWidth(preferredColumnWidth);
								perfectWidth += preferredColumnWidth;
		    				if (minimunColunmHeight == 0) minimunColunmHeight = columnHeight;
		    				else minimunColunmHeight = Math.min(minimunColunmHeight,columnHeight);
							}
					}
		// add some space for scrollbar,... (+ 30)
		perfectWidth  = Math.min(maxDimension.width ,perfectWidth + 30);
		perfectHeight = Math.min(maxDimension.height,minimunColunmHeight);
		table.setPreferredScrollableViewportSize(new Dimension(perfectWidth, perfectHeight));
    }    
}
