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
package anon.server.impl;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Attr;

public class XMLUtil
	{
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

		public static int parseNodeInt(Node n,int defaultValue)
			{
				int i=defaultValue;
				String s=parseNodeString(n,null);
				if(s!=null)
					try
						{
							i=Integer.parseInt(s);
						}
					catch(Exception e)
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
								if(n.getNodeType()==n.ELEMENT_NODE)
									n=n.getFirstChild();
								s=n.getNodeValue();
							}
						catch(Exception e)
							{
							}
					return s;
				}

		public static Node getFirstChildByName(Node n,String name)
			{
				try
					{
						Node child=n.getFirstChild();
						while(child!=null)
							{
								if(child.getNodeName().equals(name))
									return child;
								child=child.getNextSibling();
							}
					}
				catch(Exception e)
					{
					}
				return null;
			}

		public static void setNodeValue(Node n,String text)
			{
				n.appendChild(n.getOwnerDocument().createTextNode(text));
			}
	}