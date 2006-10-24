/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.impl.nameServer.NSProps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import smartsockets.virtual.VirtualSocket;
import smartsockets.virtual.VirtualSocketAddress;
import smartsockets.virtual.VirtualSocketFactory;

public class KeyChecker implements Protocol {

    private static Logger logger
            = ibis.util.GetLogger.getLogger(KeyChecker.class.getName());

    private String poolName;
    private VirtualSocketAddress address;
    private int sleep;
    private VirtualSocketFactory socketFactory; 
    
    public KeyChecker(VirtualSocketFactory factory, String poolName, 
            VirtualSocketAddress address, int sleep) {
        this.socketFactory = factory;
        this.poolName = poolName;
        this.address = address;
        this.sleep = sleep;        
    }
    
    private void check(String poolName, VirtualSocketAddress address)
            throws IOException {
        
        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        try {
            s = socketFactory.createClientSocket(address, 0, null);

            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream()));
            
            logger.debug("KeyChecker: contacting nameserver");
            
            if (poolName == null) {
                out.writeByte(IBIS_CHECKALL);
            } else {
                out.writeByte(IBIS_CHECK);
                out.writeUTF(poolName);
            }
            out.flush();

            in = new DataInputStream(
                    new BufferedInputStream(s.getInputStream()));

            int opcode = in.readByte();

            if (logger.isDebugEnabled()) {
                logger.debug("KeyChecker: nameserver reply, opcode "
                        + opcode);
            }
        } finally {
            VirtualSocketFactory.close(s, out, in);
        }
    }
    
    public void run() {
        do {
            if (sleep != 0) {
                try {
                    Thread.sleep(sleep * 1000);
                } catch(Exception e) {
                    // ignored
                }
            }
            try {
                check(poolName, address);
            } catch(Exception e) {
                // Ignored
            }
        } while (sleep != 0);
    }
    
    static void check(VirtualSocketFactory factory, String poolName, 
            VirtualSocketAddress address, int sleep) throws IOException {
        
    }
    
    private static void check(String poolName, String serverHost,
            String portString, int sleep) throws IOException {
        Properties p = System.getProperties();

        if (serverHost == null) {
            serverHost = p.getProperty(NSProps.s_host);
        }
        
        if (serverHost == null) {
            logger.fatal("KeyChecker: no nameserver host specified");
            System.exit(1);
        }

        if (poolName == null) {
            poolName = p.getProperty(NSProps.s_key);
        }

        if (portString == null) {
            portString = p.getProperty(NSProps.s_port);
        }

        int port = NameServer.TCP_IBIS_NAME_SERVER_PORT_NR;

        if (portString != null) {
            try {
                port = Integer.parseInt(portString);
                logger.debug("Using nameserver port: " + port);
            } catch (Exception e) {
                logger.error("illegal nameserver port: "
                        + portString + ", using default");
            }
        }

        // Use the default socket factory 
        VirtualSocketFactory factory = 
            VirtualSocketFactory.createSocketFactory();

        VirtualSocketAddress address = 
            VirtualSocketAddress.partialAddress(serverHost, port);
        
        new KeyChecker(factory, poolName, address, sleep).run();
    }
    
    public static void main(String args[]) throws IOException {
        String serverHost = null;
        String poolName = null;
        String portString = null;
        int sleep = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-key")) {
                poolName = args[++i];
            } else if (args[i].equals("-host") || args[i].equals("-ns")) {
                serverHost = args[++i];
            } else if (args[i].equals("-port")) {
                portString = args[++i];
            } else if (args[i].equals("-sleep")) {
                sleep = Integer.parseInt(args[++i]);
            }
        }

        check(poolName, serverHost, portString, sleep);
    }


}
