package ibis.ipl.impl.stacking.cc.manager.impl;

import ibis.ipl.impl.stacking.cc.CCIbis;
import ibis.ipl.impl.stacking.cc.manager.CCManagerImpl;
import ibis.ipl.impl.stacking.cc.manager.Connection;
import java.util.Random;

public class RandomCCManagerImpl extends CCManagerImpl {

    /*
     * When caching, the recv port doesn't know we will cache the connection.
     */
    private boolean heKnows = false;
    private Random r;

    public RandomCCManagerImpl(CCIbis ibis, int maxConns) {
        super(ibis, maxConns);
        r = new Random();
    }

    @Override
    protected Connection cacheOneConnectionFor(Connection conn) {
        /*
         * Nothing to cache. Wait until some live connections arive.
         */
        while (!canCache()
                && (!canceledReservations.contains(conn))
                && super.fullConns()) {
            try {
                if(logger.isDebugEnabled()) {
                logger.debug("Lock will be released:"
                        + " waiting for: "
                        + "a live connection to be available for caching"
                        + " OR "
                        + "an empty slot"
                        + " OR "
                        + "for a cancelation.");
                }
                super.gotSpaceCondition.await();
                if(logger.isDebugEnabled()) {
                logger.debug("Lock reaquired.");
                }
            } catch (InterruptedException ignoreMe) {
            }
        }

        if (canceledReservations.contains(conn)
                || !super.fullConns()) {
            return null;
        }
        
        int idx = r.nextInt(aliveConns.size());
        Connection con = aliveConns.get(idx);
        con.cache(heKnows);
        aliveConns.remove(con);
        cachedConns.add(con);
        return con;
    }
}
