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
/*
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */
package rmi;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import JAPModel;
/**
 * Creates the RMI-Server so that the Client can connect.
 * Note: You need to run RMIRegistry first on your system
 */
import JAPDebug;
final public class JAPAnonServiceRMIServer 
	{
    JAPAnonServiceInterface jasi = null;

    public JAPAnonServiceRMIServer(JAPModel myJAPModel) 
			{
        try 
					{
						jasi = new JAPAnonServiceImpl(myJAPModel);
					} catch (Exception e) 
					{
						JAPDebug.out(JAPDebug.NET,JAPDebug.EXCEPTION,"Error creating JAPAnonServicImpl: "+e.getMessage());
           // e.printStackTrace();
					}
        try 
					{
						//!!Ok - first creating and than geting seams to be stupid
					  //But SUN's RMI will return a registry (get), even if there is no registry! 
						JAPDebug.out(JAPDebug.NET,JAPDebug.DEBUG,"Try RMI bind...");
						Registry reg=null;
						try
							{
								JAPDebug.out(JAPDebug.NET,JAPDebug.DEBUG,"Try creating RMI-Registry...");
								reg=LocateRegistry.createRegistry(1099);
							}
						catch(Exception re)
							{
								JAPDebug.out(JAPDebug.NET,JAPDebug.EXCEPTION,"Error creating RMI-Registry -- Try getting...");
								reg=LocateRegistry.getRegistry(1099);
							}
						
						JAPDebug.out(JAPDebug.NET,JAPDebug.DEBUG,"RMI-Registry getting succesful");
						reg.rebind("AnonService",jasi);
						JAPDebug.out(JAPDebug.NET,JAPDebug.DEBUG,"RMI bind successful");
					} 
				catch (Exception e) 
					{
						JAPDebug.out(JAPDebug.NET,JAPDebug.EXCEPTION,"RMI bind Exception: "+e.getMessage());
					}
			}

    public void quitServer() 
			{
        try 
					{
						Registry reg=LocateRegistry.getRegistry(1099);
						reg.unbind("AnonService");
						JAPDebug.out(JAPDebug.NET,JAPDebug.DEBUG,"RMI unbind successful");
					}
        catch (java.rmi.NotBoundException nbe)     {}
        catch (java.rmi.RemoteException re)        {}
			}
}