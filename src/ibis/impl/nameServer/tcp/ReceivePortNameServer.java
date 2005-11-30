/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.connect.IbisSocketFactory;

import ibis.ipl.IbisRuntimeException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

class ReceivePortNameServer extends Thread implements Protocol {

    private Hashtable ports;

    Hashtable requestedPorts;

    boolean finishSweeper = false;

    private ServerSocket serverSocket;

    private DataInputStream in;

    private DataOutputStream out;

    private boolean silent;

    private static class Port {
        String ibisName;
        byte[] port;

        public Port(String ibisName, byte[] port) {
            this.ibisName = ibisName;
            this.port = port;
        }
    }

    private static class PortLookupRequest {
        private final Socket s;

        private final DataInputStream myIn;

        private final DataOutputStream myOut;

        private final String[] names;

        private final byte[][] ports;

        private final long timeout;
               
        private final boolean allowPartialResults;

        int unknown;

        private boolean done = false;

        PortLookupRequest(Socket s, DataInputStream in, DataOutputStream out,
                String[] names, byte[][] ports, long timeout, int unknown, 
                boolean allowPartialResults) {
            this.s = s;
            this.myIn = in;
            this.myOut = out;
            this.names = names;
            this.ports = ports;
            this.timeout = timeout;
            this.unknown = unknown;
            this.allowPartialResults = allowPartialResults;
        }
        
        public void addPort(String name, byte [] id) { 
         
            for (int j = 0; j < names.length; j++) {
                if (names[j].equals(name)) {
                    ports[j] = id;
                    unknown--;
                    if (unknown == 0) {
                        writeResult();
                    }
                    break;
                }
            }
        }        

        public long checkTimeToWait(long current) { 
                       
            if (timeout == 0) {
                // No timeout
                return 0;                
            }
                
            if (timeout <= current) {
                // Time has expired
                writeResult();
                return -1;
            } 

            // Some time left
            return timeout - current;
        }
        
        private void writeResult(){
            
            if (done) throw new Error("Trying to return port lookup result " 
                    + "twice!");
            
            try {
                if (unknown == 0 || allowPartialResults) {                 
                    myOut.writeByte(PORT_KNOWN);
                    for (int i = 0; i < ports.length; i++) {
                        
                        if (ports[i] == null) { 
                            myOut.writeInt(0);
                        } else {                         
                            myOut.writeInt(ports[i].length);
                        
                            if (ports[i].length > 0) {                     
                                myOut.write(ports[i]);
                            }
                        }
                    }
                } else { 
                    myOut.writeByte(PORT_UNKNOWN);
                    myOut.writeInt(unknown);
                    for (int j = 0; j < ports.length; j++) {
                        if (ports[j] == null) {
                            myOut.writeUTF(names[j]);
                        }
                    }
                } 
                
            } catch (Throwable e) { 

                System.err.println("PortLookupRequest failed to return result "
                        + " to " + s + "got IOException" + e);
                e.printStackTrace(System.err);
                
            } finally {
                done = true;
                NameServer.closeConnection(myIn, myOut, s);
            }            
        }
    }

    ReceivePortNameServer(boolean silent, IbisSocketFactory socketFactory)
            throws IOException {
        ports = new Hashtable();
        requestedPorts = new Hashtable();
        this.silent = silent;
        serverSocket = socketFactory.createServerSocket(0, null, true, null);
        setName("ReceivePort Name Server");
        start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    private void handlePortNew() throws IOException {

        Port storedId;
        byte[] id;

        String ibisName = in.readUTF();
        String name = in.readUTF();
        int len = in.readInt();
        id = new byte[len];
        in.readFully(id, 0, len);

        /* Check wheter the name is in use. */
        storedId = (Port) ports.get(name);

        if (storedId != null) {
            out.writeByte(PORT_REFUSED);
        } else {
            out.writeByte(PORT_ACCEPTED);
            addPort(name, id, ibisName);
        }
    }

    //gosia
    private void handlePortRebind() throws IOException {

        byte[] id;

        String ibisName = in.readUTF();
        String name = in.readUTF();
        int len = in.readInt();
        id = new byte[len];
        in.readFully(id, 0, len);

        /* Don't check whether the name is in use. */
        out.writeByte(PORT_ACCEPTED);
        addPort(name, id, ibisName);
    }

    private void addPort(String name, byte[] id, String ibisName) {
        
        ports.put(name, new Port(ibisName, id));
        
        ArrayList v = null;
            
        synchronized (requestedPorts) {
            v = (ArrayList) requestedPorts.remove(name);
            
            if (v != null) {
                // TODO: MOVE OUT OF SYNC BLOCK ? 
                for (int i = 0; i < v.size(); i++) {
                    PortLookupRequest p = (PortLookupRequest) v.get(i);
                    p.addPort(name, id);
                }
            }
        }
    }

    private void handlePortList() throws IOException {

        ArrayList goodNames = new ArrayList();

        String pattern = in.readUTF();
        Enumeration names = ports.keys();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (name.matches(pattern)) {
                goodNames.add(name);
            }
        }

        out.writeByte(goodNames.size());
        for (int i = 0; i < goodNames.size(); i++) {
            out.writeUTF((String) goodNames.get(i));
        }
    }

    //end gosia	

    private class RequestSweeper extends Thread {
        
        private long checkTimeOuts(ArrayList v, long current, long timeout) { 
            // synchronized on requestedPorts
            
            for (int i = v.size() - 1; i >= 0; i--) {
                PortLookupRequest p = (PortLookupRequest) v.get(i);
                
                long t = p.checkTimeToWait(current);
                                                
                if (t == -1) {
                    // Timeout has expired
                    v.remove(i);
                } else if (t > 0 && t < timeout) {
                    // Some time left and is shorter thn the others
                    timeout = t;                    
                } // else, no timeout is set
            }
            
            return timeout;
        } 
                
        private long checkTimeOuts() { 
            // synchronized on requestedPorts
            
            long timeout = 1000000L;                
            long current = System.currentTimeMillis();
                                   
            Enumeration names = requestedPorts.keys();
                
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                ArrayList v = (ArrayList) requestedPorts.get(name);
                    
                if (v != null) {
                    timeout = checkTimeOuts(v, current, timeout);
                    
                    if (v.size() == 0) {
                        requestedPorts.remove(name);
                    }    
                }
            }
                
            if (timeout < 100) {
                timeout = 100;
            }
            
            return timeout;            
        }
        
        public void run() {
            
            while (true) {
                synchronized (requestedPorts) {                    
                    if (finishSweeper) {
                        return;
                    }
                
                    long timeout = checkTimeOuts();
                   
                    try {
                        requestedPorts.wait(timeout);                        
                    } catch (InterruptedException e) {
                        // ignored
                    }   
                }                   
            }
        }
    }

    private void handlePortLookup(Socket s) throws IOException {

        boolean allowPartialResults = in.readBoolean();
        int count = in.readInt();
        String[] names = new String[count];
        byte[][] prts = new byte[count][];
        int unknown = 0;

        for (int i = 0; i < count; i++) {
            Port p;
            names[i] = in.readUTF();
            p = (Port) ports.get(names[i]);
            if (p == null) {
                unknown++;
                prts[i] = null;
            } else {
                prts[i] = p.port;
            }
        }

        long timeout = in.readLong();

        if (timeout != 0) {
            timeout += System.currentTimeMillis();
        }
                
        PortLookupRequest p = new PortLookupRequest(s, in, out, names, prts,
                timeout, unknown, allowPartialResults);

        if (unknown == 0) {            
            // TODO: clean this up ?
            p.writeResult();
            return;
        }

        synchronized (requestedPorts) {
            for (int i = 0; i < count; i++) {
                if (prts[i] == null) {
                    ArrayList v = (ArrayList) requestedPorts.get(names[i]);
                    if (v == null) {
                        v = new ArrayList();
                        requestedPorts.put(names[i], v);
                    }
                    v.add(p);
                }
            }
            if (timeout != 0) {
                requestedPorts.notify();
            }
        }
    }

    private void handlePortFree() throws IOException {
        Port id;

        String name = in.readUTF();

        id = (Port) ports.get(name);

        if (id == null) {
            out.writeByte(1);
        }
        ports.remove(name);
        out.writeByte(0);
    }

    private void handlePortKill() throws IOException {
        int cnt = in.readInt();
        String[] names = new String[cnt];

        for (int i = 0; i < cnt; i++) {
            names[i] = in.readUTF();
        }

        ArrayList v = new ArrayList();

        Enumeration portnames = ports.keys();
        while (portnames.hasMoreElements()) {
            String name = (String) portnames.nextElement();
            Port p = (Port) ports.get(name);
            for (int i = 0; i < cnt; i++) {
                if (p.ibisName.equals(names[i])) {
                    v.add(name);
                    break;
                }
            }
        }
        for (int i = 0; i < v.size(); i++) {
            String name = (String) v.get(i);
            ports.remove(name);
        }
        out.writeInt(0);
        out.flush();
    }

    public void run() {

        Socket s;
        boolean stop = false;
        int opcode;

        RequestSweeper p = new RequestSweeper();
        p.setDaemon(true);
        p.start();

        while (!stop) {

            try {
                s = serverSocket.accept();
            } catch (Exception e) {
                throw new IbisRuntimeException(
                        "ReceivePortNameServer: got an error ", e);
            }

            in = null;
            out = null;
            boolean mustClose = true;

            try {
                in = new DataInputStream(
                        new BufferedInputStream(s.getInputStream(), 4096));
                out = new DataOutputStream(
                        new BufferedOutputStream(s.getOutputStream(), 4096));

                opcode = in.readByte();

                switch (opcode) {
                case (PORT_NEW):
                    handlePortNew();
                    break;

                case (PORT_REBIND):
                    handlePortRebind();
                    break;

                case (PORT_LIST):
                    handlePortList();
                    break;

                case (PORT_FREE):
                    handlePortFree();
                    break;

                case (PORT_LOOKUP):
                    mustClose = false;
                    handlePortLookup(s);
                    break;

                case (PORT_KILL):
                    handlePortKill();
                    break;

                case (PORT_EXIT):
                    synchronized (requestedPorts) {
                        finishSweeper = true;
                        requestedPorts.notifyAll();
                    }
                    serverSocket.close();
                    return;
                default:
                    if (! silent) {
                        System.err.println("ReceivePortNameServer: got an illegal "
                                + "opcode " + opcode);
                    }
                }
            } catch (Exception e1) {
                if (! silent) {
                    System.err.println("Got an exception in "
                            + "ReceivePortNameServer.run " + e1 + ", continuing");
                    // e1.printStackTrace();
                }
            } finally {
                if (mustClose) {
                    NameServer.closeConnection(in, out, s);
                }
            }
        }
    }
}
