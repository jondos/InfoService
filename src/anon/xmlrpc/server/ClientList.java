package anon.xmlrpc.server;

import java.util.Hashtable;
import java.security.SecureRandom;

import anon.AnonChannel;
class ClientList
	{
		private Hashtable m_hashtableClients;
		private SecureRandom m_Random;

		public ClientList()
			{
				m_hashtableClients=new Hashtable();
				m_Random=new SecureRandom();
			}

		synchronized int addNewClient()
			{
				int id=m_Random.nextInt();
				while(m_hashtableClients.containsKey(new Integer(id)))
					id=m_Random.nextInt();
				ClientEntry c=new ClientEntry(id);
				m_hashtableClients.put(new Integer(id),c);
				return id;
			}

		synchronized ClientEntry getClient(Integer clientid)
			{
				return (ClientEntry)m_hashtableClients.get(clientid);
			}


	}

class ClientEntry
	{
		private int m_id;
		private Hashtable m_hashtableChannels;
		ClientEntry(int id)
			{
				m_id=id;
				m_hashtableChannels=new Hashtable();
			}

		public int hashCode()
			{
				return m_id;
			}

		public void addChannel(AnonChannel c)
			{
				m_hashtableChannels.put(new Integer(c.hashCode()),c);
			}

		public AnonChannel getChannel(Integer id)
			{
				return (AnonChannel)m_hashtableChannels.get(id);
			}

	}
