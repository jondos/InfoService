package infoservice.agreement.simulator;

public class GoodInfoService extends AInfoService
{

    public GoodInfoService(String id, String prefix, int nodeID)
    {
        super(id, prefix, nodeID);
    }

    public boolean isEvil()
    {
        return false;
    }

}
