package jpi;

import anon.util.IXMLEncodable;
import anon.pay.xml.XMLErrorMessage;
/**
 * Datencontainer f\uFFFDr eine Bezahlinstanz-Http-Antwort.
 *
 * @author Andreas Mueller
 */
public class PIAnswer
{
    private int m_iStatusCode;
    private IXMLEncodable m_Content;
	private int m_iType;
	public final static int TYPE_ERROR=-1;
	public final static int TYPE_CLOSE=1;
	public final static int TYPE_CHALLENGE_REQUEST=2;

   //Bi --> JAP
	public final static int TYPE_ACCOUNT_CERTIFICATE=3;
	public final static int TYPE_AUTHENTICATION_SUCCESS=4;
	public final static int TYPE_TRANSFER_CERTIFICATE=5;
	public final static int TYPE_BALANCE=6;

	//Bi --> AI
	public final static int TYPE_PAYOFF=7;
	public final static int TYPE_SETTLE=8;
	public final static int TYPE_CONFIRM=9;
	public final static int TYPE_ACCOUNT_SNAPSHOT=10;

   public PIAnswer(int type, IXMLEncodable content)
    {
        m_Content=content;
		m_iType=type;
    }

	public static PIAnswer getErrorAnswer(int errorCode)
	{
		IXMLEncodable err = new XMLErrorMessage(errorCode);
		return new PIAnswer(TYPE_CLOSE, err);
	}

	public static PIAnswer getErrorAnswer(int errorCode, String msg)
	{
		IXMLEncodable err = new XMLErrorMessage(errorCode, msg);
		return new PIAnswer(TYPE_CLOSE, err);
	}



	public int getType()
	{
		return m_iType;
	}


	public IXMLEncodable getContent()
	{
		return m_Content;
	}
}
