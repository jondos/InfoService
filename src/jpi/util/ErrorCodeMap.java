package jpi.util;
import java.util.*;

/** 
 * Klasse die ein Mapping Http-Fehler-Code zu Fehlertext realisiert.
 * 
 * @author Andreas Mueller
 */
public class ErrorCodeMap 
{
    static private ErrorCodeMap errorCodeMap=null;
    static private HashMap m_map; 
    private ErrorCodeMap()
    { 
        m_map = new HashMap();
        m_map.put(new Integer(200),"OK");
        m_map.put(new Integer(411),"Length Required");
        m_map.put(new Integer(400),"Bad Request");
        m_map.put(new Integer(404),"Not Found");
        m_map.put(new Integer(409),"Conflict");
        m_map.put(new Integer(413),"Entity To Long");
        m_map.put(new Integer(500),"Internal Server Error");
        m_map.put(new Integer(505),"HTTP Version Not Supported");
    }
    public static ErrorCodeMap getInstance()
    {
        if(errorCodeMap==null)
            errorCodeMap= new ErrorCodeMap();
        return errorCodeMap;
    }
    /** 
     * Liefert die Fehlerbeschriebung zum Fehlercode.
     * 
     * @param Code Fehlercode 
     * @return Fehlertext 
     */
    public static String getDescription(int Code)
    {
        String description;
        if ((description=(String)m_map.get(new Integer(Code)))==null)
            description="Unkown";
        return description; 
        
    }
}
