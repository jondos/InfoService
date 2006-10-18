package infoservice.agreement;

/**
 * @author LERNGRUPPE An enum substitute for message types.
 */
public class AgreementMessageTypes
{
    public static final int MESSAGE_TYPE_INIT = 0;

    public static final int MESSAGE_TYPE_ECHO = 1;

    public static final int MESSAGE_TYPE_COMMIT = 2;

    public static final int MESSAGE_TYPE_REJECT = 3;

    public static final int MESSAGE_TYPE_CONFIRMATION = 4;

    public static String getTypeAsString(int type)
    {
        switch (type)
        {
            case 0:
                return "INIT";
            case 1:
                return "ECHO";
            case 2:
                return "COMMIT";
            case 3:
                return "REJECT";
            case 4:
                return "CONFIRMATION";
        }
        return "NO_TYPE";
    }

}
