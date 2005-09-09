package jpi.db;
import java.util.*;

/**
 * Data container class for an account snapshot
 *
 * @author Andreas Mueller
 */
public class AccountSnapshot
{
    public long creditMax;
    public long credit;
    public long costs;

    public AccountSnapshot(long creditMax, long credit, long costs)
    {
        this.creditMax=creditMax;
        this.credit=credit;
        this.costs=costs;
    }
}
