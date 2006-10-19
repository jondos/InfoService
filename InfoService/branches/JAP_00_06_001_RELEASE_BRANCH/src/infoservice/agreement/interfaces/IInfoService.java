package infoservice.agreement.interfaces;

public interface IInfoService
{

    public abstract String getIdentifier();

    public abstract void handleMessage(IAgreementMessage a_message);

    public void multicastMessage(IAgreementMessage a_message);

    public void sendMessageTo(String a_id, IAgreementMessage a_echoMessage);

    public void notifyAgreement(Long a_newCommonRandomSeed);

    public int getNumberOfAllInfoservices();
}