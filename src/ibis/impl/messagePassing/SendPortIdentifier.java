package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;

final class SendPortIdentifier implements ibis.ipl.SendPortIdentifier,
    java.io.Serializable {

    String name;
    String type;
    int cpu;
    int port;
    IbisIdentifier ibisId;
    transient byte[] serialForm;


    SendPortIdentifier(String name, String type)
	    throws IbisIOException {

	synchronized (Ibis.myIbis) {
	    port = Ibis.myIbis.sendPort++;
	}
	this.name = name;
	this.type = type;
	this.ibisId = (IbisIdentifier)Ibis.myIbis.identifier();
	cpu = Ibis.myIbis.myCpu;
	makeSerialForm();
    }


    private void makeSerialForm() throws IbisIOException {
	serialForm = SerializeBuffer.writeObject(this);
    }


    byte[] getSerialForm() throws IbisIOException {
	if (serialForm == null) {
	    makeSerialForm();
	}
	return serialForm;
    }


    public boolean equals(ibis.ipl.SendPortIdentifier other) {
	    if (other == this) return true;
	    
	    if (other instanceof SendPortIdentifier) {
		    SendPortIdentifier o = (SendPortIdentifier)other;
		    return cpu == o.cpu && port == o.port;
	    }
	    
	    return false;
    }

    public String name() {
	if (name != null) {
	    return name;
	}

	return "anonymous";
    }

    public String type() {
	return type;
    }

    public ibis.ipl.IbisIdentifier ibis() {
	return ibisId;
    }

    public String toString() {
	return ("(SendPortIdent: name \"" + name + "\" type \"" + type +
		"\" cpu " + cpu + " port " + port + ")");
    }
}
