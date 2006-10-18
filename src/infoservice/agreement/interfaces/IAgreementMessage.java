package infoservice.agreement.interfaces;

import org.w3c.dom.Document;

import anon.infoservice.IDistributable;

public interface IAgreementMessage extends IDistributable
{

    /**
     * Creates an XML representation of this message.
     * 
     * @return The Docunment:
     */
    public Document toXML();

    public boolean isSignatureOK();

    public String getHashKey();

    public String getConsensusId();

    public String toString();

    public void setSignatureOk(boolean a_ok);

    public int getMessageType();

    public void setXmlDocument(Document a_node);

    public void setLastCommonRandom(String string);

}
