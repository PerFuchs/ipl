package ibis.rmi.server;

import ibis.ipl.*;
import ibis.io.*;
import ibis.rmi.*;

public class Stub extends RemoteStub {

	transient protected int stubID;
	transient protected SendPort send;
	transient protected ReceivePort reply;
	protected ReceivePortIdentifier skeletonPortId;

	public Stub() {};
    
	public void init(SendPort s, ReceivePort r, int id, ReceivePortIdentifier rpi) {
	
		stubID = id;
		send = s;
		reply = r;
//	setRef(this, new UnicastRef(rpi));
		skeletonPortId = rpi;
	}
    
	//serialize & deserialize

	public final void initSend() throws IbisIOException {
	    if (send == null) {
		send = RTS.createSendPort();
		send.connect(skeletonPortId);
		reply = RTS.createReceivePort();
		reply.enableConnections();

		WriteMessage wm = send.newMessage();
		wm.writeInt(-1);
		wm.writeInt(0);
		wm.writeObject(reply.identifier());
		wm.send();
		wm.finish();

		ReadMessage rm = reply.receive();
		stubID = rm.readInt();
		String stubType = (String) rm.readObject();
		rm.finish();		
	    }
	}
}
