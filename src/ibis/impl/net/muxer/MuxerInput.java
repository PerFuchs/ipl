package ibis.ipl.impl.net.muxer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetBufferedInput;
import ibis.ipl.impl.net.NetBufferFactory;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetIbisException;

public abstract class MuxerInput extends NetBufferedInput implements Runnable {

    /**
     * Indicate whether a separate thread is desired to perform all polling
     * of the MuxerInput.
     */
    protected static final boolean USE_POLLER_THREAD = false;

    private int			pollerThreads;

    private Thread		poller;

    protected int		max_mtu       =    0;
    protected int		min_mtu       =    Integer.MAX_VALUE;

    private int			max_ever_mtu = -1;

    private int			liveConnections;

    private int			upcallReceivers;

    private final static int	defaultPollTimeout = 30;	// ms.
    private int			pollTimeout = defaultPollTimeout;



    /**
     * @constructor.
     *
     * Call this from all subclass constructors.
     */
    protected MuxerInput(NetPortType portType,
			 NetDriver   driver,
			 NetIO       up,
			 String      context) {
	super(portType, driver, up, context);
	mtu   	     =    0;
	headerLength = NetConvert.INT_SIZE;

	/* ... please be patient, we'll find out how setup works *
	String s = null;
	if ((s = getProperty("PollingTimeout")) != null) {
		pollTimeout = Integer.valueOf(s).intValue();
	}
	* but only ma~nana */

	if (USE_POLLER_THREAD) {
	    poller = new Thread(this);
	    poller.setDaemon(true);
	    poller.setName("UDP multiplexer poller");
	    poller.start();
	}
    }


    private void receive() throws NetIbisException {
	NetReceiveBuffer buffer = receiveByteBuffer(max_mtu);
	int rKey = NetConvert.readInt(buffer.data, buffer.base);
	MuxerQueue q = locateQueue(rKey);
	if (Driver.DEBUG) {
	    System.err.println("Receive downcall UDP packet len " + buffer.length + " data " + buffer.data + "; key " + rKey + " /bound to " + q);
	}
	q.enqueue(buffer);
    }


    /**
     * @method
     *
     * @param timeout poll timeout in msec. 0 signifies indefinite timeout.
     */
    abstract protected Integer poll(int timeout) throws NetIbisException;


    /**
     * @{inheritDoc}
     */
    public Integer poll(boolean block) throws NetIbisException {

	if (! USE_POLLER_THREAD) {
	    synchronized (this) {
		pollerThreads++;
	    }
	}

	Integer spn = poll(block ? 0 : pollTimeout);

	if (! USE_POLLER_THREAD) {
	    synchronized (this) {
		pollerThreads--;
	    }
	}

	return spn;
    }


    /**
     * @method
     *
     * Test whether some other thread is busy polling or blocking.
     * If so, return immediately.
     * If not, perform poll(block).
     *
     * @param block If no other thread is polling, perform a poll.
     *        This parameter indicates whether it is to be a blocking
     *        poll or a nonblocking poll.
     */
    protected Integer attemptPoll(boolean block) throws NetIbisException {
	boolean proceed;
	Integer r = null;

	synchronized (this) {
	    proceed = (pollerThreads == 0);
	    if (proceed) {
		pollerThreads++;
	    }
	}
if (! proceed) System.err.print(proceed ? "v" : "-");

	if (proceed) {
	    if ((r = poll(block ? 0 : pollTimeout)) != null) {
		receive();
	    }

	    synchronized (this) {
		pollerThreads--;
	    }
	}

	return r;
    }



    /**
     * @method
     *
     * This should be called before setupConnection in the subclass, before
     * any communication takes place.
     */
    protected MuxerQueue createQueue(Integer spn) {
	MuxerQueue q = new MuxerQueue(this, spn);
	registerQueue(q);

	return q;
    }


    public void startQueue(MuxerQueue queue, NetBufferFactory factory)
	    throws NetIbisException {
	if (max_ever_mtu == -1) {
	    max_ever_mtu = max_mtu;
	} else if (max_mtu > max_ever_mtu) {
	    throw new NetIbisException("Cannot increase mtu beyond " + max_ever_mtu);
	}
	factory.setMaximumTransferUnit(max_ever_mtu);
	queue.setBufferFactory(factory);
	this.factory = factory;
	if (upcallReceivers++ == 0) {
	    synchronized (this) {
		notify();
	    }
	}
    }


    public void disconnect(MuxerQueue q) throws NetIbisException {
	if (Driver.DEBUG) {
	    Thread.dumpStack();
	    System.err.println("Now disconnect localQueue " + q.localKey() + " liveConnections was " + liveConnections());
	}
	releaseQueue(q);
	if (USE_POLLER_THREAD) {
	    poller.interrupt();
	}
    }


    private MuxerKeyHash keyHash = new MuxerKeyHash();

    synchronized
    protected void registerQueue(MuxerQueue q) {
	keyHash.registerKey(q);
	liveConnections++;
    }

    synchronized
    protected MuxerQueue locateQueue(int n) {
	return (MuxerQueue)keyHash.locateKey(n);
    }

    synchronized
    protected void releaseQueue(MuxerQueue key) throws NetIbisException {
	keyHash.releaseKey(key);
	liveConnections--;
	if (liveConnections == 0) {
	    free();
	}
    }


    protected int liveConnections() {
	return liveConnections;
    }


    /**
     * The poller thread.
     */
    public void run() {
	synchronized (this) {
	    while (upcallReceivers == 0) {
		try {
		    wait();
		} catch (InterruptedException e) {
		    // Just go on waiting
		}
	    }
	}

	if (Driver.DEBUG) {
	    System.err.println(this + ": poller thread runs");
	}

	Thread me = Thread.currentThread();

	while (liveConnections > 0) {
	    try {
		if (Driver.DEBUG) {
		    System.err.println(this + ": poller thread does a blocking receive");
		}
		while (poll(0) == null) {
		    /* try again */
		}

		receive();

	    } catch (Exception e) {
		System.err.println("************************ Poller thread handles exception " + e);
	    }
	}

	System.err.println(this + ": poller thread quits");
    }

}
