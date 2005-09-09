package jpi.db;
import java.util.*;

/**
 * Data container for an account's balance
 *
 * @author Andreas Mueller, Bastian Voigt
 */
public class Balance
{
    public long deposit;
    public long spent;

    // timestamps
    public java.sql.Timestamp timestamp;
    public java.sql.Timestamp validTime;

    // payment confirmations
    public Vector confirms;

    public Balance( long deposit, long spent,
		    java.sql.Timestamp timestamp,
		    java.sql.Timestamp validTime,
		    Vector confirms )
    {
        this.deposit = deposit;
        this.spent = spent;
				this.timestamp = timestamp;
				this.validTime = validTime;
        this.confirms = confirms;
    }
}
