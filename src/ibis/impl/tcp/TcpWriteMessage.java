package ibis.impl.tcp;

import ibis.io.SerializationOutputStream;
import ibis.io.SplitterException;
import ibis.ipl.IbisError;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

final class TcpWriteMessage implements WriteMessage {
    private TcpSendPort sport;
    private SerializationOutputStream out;
    private boolean connectionAdministration;
    private long before;
    boolean isFinished = false;

    TcpWriteMessage(
	    TcpSendPort p,
	    SerializationOutputStream out,
	    boolean connectionAdministration) {
	this.connectionAdministration = connectionAdministration;
	sport = p;
	this.out = out;
	if (Config.STATS) {
	    before = sport.dummy.getCount();
	}
    }

    // if we keep connectionAdministration, forward exception to upcalls / downcalls.
    // otherwise, rethrow the exception to the user.
    private void forwardLosses(SplitterException e) throws IOException {
	// System.err.println("connection lost!");

	// Inform the port
	for(int i=0; i<e.count(); i++) {
	    sport.lostConnection(e.getStream(i), e.getException(i), connectionAdministration);
	}

	if(!connectionAdministration) { // otherwise an upcall /downcall was/will be done
	    throw e;
	}
    }

    public SendPort localPort() {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	return sport;
    }

    public int send() {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	return 0;
    }

    public long finish() throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	isFinished = true;
	try {
	    out.reset();
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
	try {
	    out.flush();
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
	sport.finishMessage();
	if (Config.STATS) {
	    long after = sport.dummy.getCount();
	    long retval = after - before;
	    sport.count += retval;
	    before = after;
	    return retval;
	}
	return 0;
    }

    public void finish(IOException e) {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	isFinished = true;
	try {
	    out.reset();
	} catch (SplitterException e2) {
	    for(int i=0; i<e2.count(); i++) {
		sport.lostConnection(e2.getStream(i), e2.getException(i), connectionAdministration);
	    }
	} catch (IOException e3) {
	    //IGNORE
	}

	try {
	    out.flush();
	} catch (SplitterException e2) {
	    for(int i=0; i<e2.count(); i++) {
		sport.lostConnection(e2.getStream(i), e2.getException(i), connectionAdministration);
	    }
	} catch (IOException e3) {
	    //IGNORE
	}

	before = sport.dummy.getCount();
	sport.finishMessage();
    }

    public void reset() throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.reset();
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void sync(int ticket) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.flush();
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeBoolean(boolean value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeBoolean(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeByte(byte value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeByte(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeChar(char value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeChar(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeShort(short value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeShort(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeInt(int value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeInt(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeLong(long value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeLong(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeFloat(float value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeFloat(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeDouble(double value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeDouble(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeString(String value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeUTF(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeObject(Object value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeObject(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(boolean [] value) throws IOException {
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(byte [] value) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(char [] value) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(short [] value) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(int [] value) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(long [] value) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(float [] value) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(double [] value) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(Object [] value) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(boolean [] value, int offset, int size) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value, offset, size);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(byte [] value, int offset, int size) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value, offset, size);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(char [] value, int offset, int size) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value, offset, size);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(short [] value, int offset, int size) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value, offset, size);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(int [] value, int offset, int size) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value, offset, size);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(long [] value, int offset, int size) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value, offset, size);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(float [] value, int offset, int size) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value, offset, size);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(double [] value, int offset, int size) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value, offset, size);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }

    public void writeArray(Object [] value, int offset, int size) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Writing data to a message that was already finished");
	}
	try {
	    out.writeArray(value, offset, size);
	} catch (SplitterException e) {
	    forwardLosses(e);
	}
    }
}