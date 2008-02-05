package ibis.impl.messagePassing;

import ibis.io.SunSerializationInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Receiver-side stub for a SendPort that performs Sun serialization
 */
final class SerializeShadowSendPort extends ShadowSendPort {

    java.io.ObjectInput obj_in;

    private int synchost;

    // private
    private final static int UNCONNECTED = 0;
    private final static int CONNECTING = UNCONNECTED + 1;
    private final static int DISCONNECTING = CONNECTING + 1;
    private final static int CONNECTED = CONNECTING + 1;

    private int connectState = UNCONNECTED;


    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    SerializeShadowSendPort(ReceivePortIdentifier rId,
			    SendPortIdentifier sId,
			    int startSeqno,
			    int group,
			    int groupStartSeqno,
			    int syncer)
	    throws IOException {
	super(rId, sId, startSeqno, group, groupStartSeqno);

	Ibis.myIbis.checkLockOwned();

	this.synchost = syncer;
    }



    ReadMessage getMessage(int seqno) throws IOException {
	if (DEBUG) {
	    if (obj_in == null || connectState != CONNECTED) {
		System.err.println(this + ": OOOOOPS getMessage(), cachedMessage " + cachedMessage + " obj_in " + obj_in + " connectState " + connectState);
	    }
	}

	ReadMessage msg = cachedMessage;

	if (DEBUG) {
	    System.err.println(this + ": Get a Serialize ReadMessage ");
	}

	if (msg != null) {
	    if (connectState != CONNECTED) {
		System.err.println(this + ": OOOOOPS getMessage(), cachedMessage nonnull but connectState " + connectState + " (i.e. not connected)");
	    }
	    cachedMessage = null;

	} else {
	    msg = new SerializeReadMessage(this, receivePort);
	    if (DEBUG) {
		System.err.println(Thread.currentThread() + ": Create a -sun- ReadMessage " + msg); 
	    }
	}

	msg.msgSeqno = seqno;

	return msg;
    }


    void disconnect() throws IOException {
	connectState = UNCONNECTED;
	obj_in = null;
    }


    /**
     * Sun serialization expects a standard {@link java.io.InputStream}
     * for its input, which {@link ByteInputStream} is not.
     * We provide a converter class.
     */
    private static class InputStream extends java.io.InputStream {

	ByteInputStream in;

	InputStream(ByteInputStream in) {
	    this.in = in;
	}

	public int read() throws IOException {
	    return in.read();
	}

	public int read(byte[] b) throws IOException {
	    return in.read(b);
	}

	public int read(byte[] b, int off, int len) throws IOException {
	    return in.read(b, off, len);
	}

	public int available() throws IOException {
	    return in.available();
	}

    }


    boolean checkStarted(ReadMessage msg) throws IOException {

	if (DEBUG) {
	    System.err.println(this + ": checkStarted(msg=" + msg
				+ ") connectState " + connectState
				+ " obj_in " + obj_in);
	}

	if (connectState == CONNECTED) {
	    return true;
	}

	connectState = CONNECTING;

	in.setMsgHandle(msg);

	Ibis.myIbis.unlock();
	try {
	    obj_in = new SunSerializationInputStream(new BufferedInputStream(new InputStream(in)));
	} finally {
	    Ibis.myIbis.lock();
	}

	if (DEBUG) {
	    System.err.println(Thread.currentThread() + " ShadowSendPort "
		    + this + " has created ObjectInputStream " + obj_in);
	    System.err.println("Clear the message " + msg + " handle 0x"
		    + Integer.toHexString(msg.fragmentFront.msgHandle)
		    + " that contains the ObjectStream init stuff");
	}
	msg.clear();
	tickReceive();

	connectState = CONNECTED;

	sendConnectAck(ident.cpu, synchost, true);

	if (DEBUG) {
	    System.err.println(this +": handled connect, msg " + msg
		    + " syncer " + Integer.toHexString(synchost)
		    + " startSeqno " + messageCount
		    + " groupStartSeqno " + groupStartSeqno);
	}

	return false;
    }

}