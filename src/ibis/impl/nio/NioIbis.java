/* $Id: NioIbis.java 5175 2007-03-07 13:06:34Z ndrost $ */

package ibis.impl.nio;

import ibis.impl.IbisIdentifier;
import ibis.ipl.CapabilitySet;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.Upcall;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class NioIbis extends ibis.impl.Ibis {

    static final String prefix = "ibis.impl.nio.";

    static final String s_spi = prefix + "spi";

    static final String s_rpi = prefix + "rpi";

    static final String[] props = { s_spi, s_rpi };

    private static final Logger logger
            = Logger.getLogger("ibis.impl.nio.NioIbis");

    ChannelFactory factory;

    private HashMap<ibis.ipl.IbisIdentifier, InetSocketAddress> addresses
        = new HashMap<ibis.ipl.IbisIdentifier, InetSocketAddress>();

    private SendReceiveThread sendReceiveThread = null;

    public NioIbis(RegistryEventHandler r, CapabilitySet p, Properties tp) {

        super(r, p, tp, null);
        properties.checkProperties(prefix, props, null, true);
    }

    protected byte[] getData() throws IOException {

        factory = new TcpChannelFactory(this);

        InetSocketAddress myAddress = factory.getAddress();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(myAddress);
        out.close();

        return bos.toByteArray();
    }

    public void left(ibis.ipl.IbisIdentifier id) {
        super.left(id);
        synchronized(addresses) {
            addresses.remove(id);
        }
    }

    public void died(ibis.ipl.IbisIdentifier id) {
        super.died(id);
        synchronized(addresses) {
            addresses.remove(id);
        }
    }

    protected void quit() {
        try {
            if (factory != null) {
                factory.quit();
            }

            if (sendReceiveThread != null) {
                factory.quit();
            }
        } catch(Throwable e) {
            // ignored
        }
        logger.info("NioIbis" + ident + " DE-initialized");
    }

    synchronized SendReceiveThread sendReceiveThread() throws IOException {
        if (sendReceiveThread == null) {
            sendReceiveThread = new SendReceiveThread();
        }
        return sendReceiveThread;
    }

    InetSocketAddress getAddress(IbisIdentifier id) throws IOException {
        InetSocketAddress idAddr;
        synchronized(addresses) {
            idAddr = addresses.get(id);
            if (idAddr == null) {
                ObjectInputStream in = new ObjectInputStream(
                        new java.io.ByteArrayInputStream(
                                id.getImplementationData()));
                try {
                    idAddr = (InetSocketAddress) in.readObject();
                } catch(ClassNotFoundException e) {
                    throw new IOException("Could not get address from " + id);
                }
                in.close();
                addresses.put(id, idAddr);
            }
        }
        return idAddr;
    }

    protected ibis.ipl.SendPort doCreateSendPort(CapabilitySet tp,
            String name, SendPortDisconnectUpcall cU,
            boolean connectionDowncalls) throws IOException {
        return new NioSendPort(this, tp, name, connectionDowncalls, cU);
    }

    protected ibis.ipl.ReceivePort doCreateReceivePort(CapabilitySet tp,
            String name, Upcall u, ReceivePortConnectUpcall cU,
            boolean connectionDowncalls) throws IOException {

        int receivePortImplementation;

        if (tp.hasCapability("receiveport.blocking")) {
            return new BlockingChannelNioReceivePort(this, tp, name, u,
                    connectionDowncalls, cU);
        }
        if (tp.hasCapability("receiveport.nonblocking")) {
            return new NonBlockingChannelNioReceivePort(this, tp, name, u,
                    connectionDowncalls, cU);
        }
        if (tp.hasCapability("receiveport.thread")) {
            return new ThreadNioReceivePort(this, tp, name, u,
                    connectionDowncalls, cU);
        }
        if (tp.hasCapability(CONNECTION_ONE_TO_ONE)) {
            return new BlockingChannelNioReceivePort(this, tp, name, u,
                    connectionDowncalls, cU);
        }
        return new NonBlockingChannelNioReceivePort(this, tp, name, u,
                connectionDowncalls, cU);
    }
}
