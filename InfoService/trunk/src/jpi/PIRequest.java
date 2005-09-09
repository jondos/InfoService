package jpi;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Data container for a http request.
 * Used by PIPaypalServer, PIServer, PIUserHttpServer
 *
 * @author Andreas Mueller
 */
public class PIRequest
{
	public String method;
	public String url;
	public String getData;
	public int contentLength;
	public byte[] data;

	// cache for getAttribute()
	private Hashtable m_attribs = null;

	public PIRequest()
	{
		method = null;
		url = null;
		contentLength = -1;
		data = null;
	}

	/** stores the request POST parameters names and values in an internal hashtable
	 * @deprecated this is only needed for paypal and can now be removed
	 * @author Bastian Voigt
	 * @date 04 July 2003
	 */
	private boolean buildHashtable()
	{
		if (method.equalsIgnoreCase("POST"))
		{
			if (data.length == 0)
			{
				return false;
			}
			m_attribs = new Hashtable(30);
			StringTokenizer tok = new StringTokenizer(new String(data), "&");
			String temp = null, a_name = null, a_val = null;
			int index = 0;
			while (tok.hasMoreTokens())
			{
				temp = tok.nextToken();
				index = temp.indexOf('=');
				a_name = temp.substring(0, index);
				a_val = temp.substring(index + 1, temp.length());
				m_attribs.put(a_name, a_val);
			}
			return true;
		}
		else if (method.equalsIgnoreCase("GET"))
		{
			if (getData.length() == 0)
			{
				return false;
			}
			m_attribs = new Hashtable(30);
			StringTokenizer tok = new StringTokenizer(getData, "&");
			String temp = null, a_name = null, a_val = null;
			int index = 0;
			while (tok.hasMoreTokens())
			{
				temp = tok.nextToken();
				index = temp.indexOf('=');
				a_name = temp.substring(0, index);
				a_val = temp.substring(index + 1, temp.length());
				m_attribs.put(a_name, a_val);
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	/** Gets a parameter by name.
	 *
	 * If method is POST, the parameters are read from the request's
	 * data section.  If method is GET, the parameters are read from
	 * getData (everything in the URL that comes after the '?'
	 * character
	 *
	 * Fixme: error handling
	 * @param name Attribute's name
	 * @return Attribute value or null if the attribute does not exist
	 * @author Bastian Voigt
	 * @date 01 July 2003
	 */
	public String getParameter(String name)
	{
		if (m_attribs == null)
		{
			if (!buildHashtable())
			{
				return null;
			}
		}
		return (String) m_attribs.get(name);
	}

	/** Returns an enumeration of all parameter names
	 * Works only if method is POST.
	 * @author Bastian Voigt
	 * @date 04 July 2003
	 */
	public Enumeration getParameterNames()
	{
		if (m_attribs == null)
		{
			if (!buildHashtable())
			{
				return null;
			}
		}
		return m_attribs.keys();
	}

}
