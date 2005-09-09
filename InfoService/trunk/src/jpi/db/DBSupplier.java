package jpi.db;

import jpi.Configuration;
import java.security.interfaces.*;
import java.util.*;
import java.io.*;
import java.sql.*;
import org.postgresql.*;
/** 
 * Klasse die ein Objekt liefert, welches {@link DBInterface} implementiert. 
 * Zur Zeit ist nur der Zugriff auf eine Postgres-Datenbank implementiert.
 * Zur Unterstützung einer anderen Datenbank (z.B. MySQL) sind diese Klasse
 * und gegebenenfalls {@link DataBase} anzupassen.
 * 
 * @author Andreas Mueller
 */
public abstract class DBSupplier 
{
    private static Connection con=null;


    /** 
     * Initialisiert die Verbindung zur Datenbank.
     * 
     * @param databaseHost Datenbank-Host 
     * @param databasePort Datenbank-Port 
     * @param databaseName Datenbank-Name 
     * @param userName Datenbanknutzername 
     * @param password Passwort 
     * @throws Exception 
     */
    public static synchronized void initDataBase( String databaseHost, int databasePort, 
						String databaseName, String userName, 
						String password )
				throws Exception
		{
			if (con!=null) closeDataBase();
			Class.forName("org.postgresql.Driver"); //check if driver class is present
			con = DriverManager.getConnection( "jdbc:postgresql://"+databaseHost+":"+
							databasePort+"/"+databaseName, userName, password );
    }

    /** 
     * Liefert ein Objekt welches {@link DBInterface} implementiert.
     * 
     * @return {@link DataBase} 
     * @throws Exception 
     */
    public static synchronized DBInterface getDataBase() throws Exception
    {
        if (con==null) throw new Exception("no connection to database");
        return (DBInterface) new DataBase(con);
    }

    /** 
     * Schließt die Datenbank.
     * 
     * @throws Exception 
     */
    public static synchronized void closeDataBase() throws Exception
    {
        con.close();
    }
}
