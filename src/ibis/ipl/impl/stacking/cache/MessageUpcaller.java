package ibis.ipl.impl.stacking.cache;

import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cache.CacheReadMessage.CacheReadUpcallMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class handles message upcalls.
 */
public final class MessageUpcaller implements MessageUpcall {
   
    /*
     * The user defined message upcaller.
     */
    MessageUpcall upcaller;
    CacheReceivePort port;
    
    boolean wasLastPart;
    
    CacheReadMessage currentLogicalMsg;
    final Object currentLogicalMsgLock;
    boolean currentLogicalMsgFinished;
    /*
     * If the current true message has been or not emptied.
     */
    boolean messageDepleted;

    public MessageUpcaller(MessageUpcall upcaller, CacheReceivePort port) {
        this.upcaller = upcaller;
        this.port = port;
        currentLogicalMsgLock = new Object();
    }

    @Override
    public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
        CacheManager.log.log(Level.INFO, "Got message upcall.\n");

            messageDepleted = false;
            boolean isLastPart = m.readBoolean();
            
            CacheManager.log.log(Level.INFO, "\nisLastPart={0}", isLastPart);
            
            // this is a logically new message.
            if (wasLastPart) {
                currentLogicalMsg = new CacheReadUpcallMessage(m, port);
                currentLogicalMsgFinished = false;
            }

            /*
             * Feed the buffer of the DataInputStream with the data from this
             * message. the format of the message is: (bufSize, byte[bufSize]
             * buffer);
             */
            currentLogicalMsg.offerToBuffer(m);

            if (wasLastPart) {
                // this is a logically new message.
                upcaller.upcall(currentLogicalMsg);
                synchronized(currentLogicalMsgLock) {
                    currentLogicalMsgFinished = true;
                    currentLogicalMsgLock.notify();
                }
            }
            
            wasLastPart = isLastPart;

            /*
             * Block until all data from m has been removed
             * or until the user upcall is done.
             */
            synchronized (currentLogicalMsgLock) {
                while (!messageDepleted && !currentLogicalMsgFinished) {
                    try {
                        currentLogicalMsgLock.wait();
                    } catch (InterruptedException ignoreMe) {
                    }
                }
            }
            // now `m` will be finished at the exit from this method.
            m.finish();
    }
}
