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
package anon.xmlrpc;
import com.tm.xmlrpc.Serializer;
import com.tm.xmlrpc.SerializerFactory;
import java.util.Hashtable;
import java.util.Vector;
import java.io.Serializable;
/** This class had to be made because of bugs in the JAVA RMI Compiler
 *  It provides all features of AnonServerDBEntry
 */
final public class MixCascade implements Serializer {
    private String host;
    private int    port;
    private String name;

		static final long serialVersionUID = 8477495015849363L;

		public MixCascade()
			{
			};
		public MixCascade (String n,String h, int p) {
        host = h;
        port = p;
        name=n;
    }

		public Object deserialize(Object param,Class c)
			{
				if(param instanceof Hashtable)
					{
						Hashtable hash = (Hashtable)param;
						String name = (String)hash.get("name");
						String host = (String)hash.get("host");
						Integer port = (Integer)hash.get("port");
						return new MixCascade(name, host,port.intValue());
					}
				else
					{
						Vector v=(Vector)param;
						MixCascade[] cascades=new MixCascade[v.size()];
						for(int i=0;i<cascades.length;i++)
						  {
								cascades[i]=(MixCascade)deserialize(v.elementAt(i),anon.xmlrpc.MixCascade.class);
							}
						return cascades;
					}
			}

		public String serialize(String name, Object param)
			{
				if(param instanceof anon.xmlrpc.MixCascade)
					{
						MixCascade cascade = (MixCascade)param;

						StringBuffer buffer = new StringBuffer();

						Serializer strSerializer = SerializerFactory.
							getInstance().getSerializer(String.class);
						Serializer intSerializer = SerializerFactory.
							getInstance().getSerializer(Integer.class);

						buffer.append("<value><struct>");

						buffer.append("<member><name>name</name>");
						buffer.append(strSerializer.serialize("", cascade.getName()));
						buffer.append("</member><member><name>host</name>");
						buffer.append(strSerializer.serialize("", cascade.getHost()));
						buffer.append("</member><member><name>port</name>");
						buffer.append(intSerializer.serialize("", new Integer(cascade.getPort())));
						buffer.append("</member></struct></value>");
						// Return the buffer as a String.
						return buffer.toString();
					}
				else
					{
						MixCascade[] cascades = (MixCascade[])param;
						StringBuffer buffer = new StringBuffer();
						buffer.append("<value><array><data>");
						for(int i=0;i<cascades.length;i++)
							{
								buffer.append(serialize(name,cascades[i]));
							}
						buffer.append("</data></array></value>");
						return buffer.toString();
					}
			}

    public String getName()
    {
        return name;//host+":"+Integer.toString(port);
    }

    public int getPort(){
        return port;
    }

    public String getHost(){
        return host;
    }
}