package ibis.ipl.impl.stacking.cc;

import ibis.ipl.WriteMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CCWriteMessage implements WriteMessage {
    
    private final static Logger logger = 
            LoggerFactory.getLogger(CCWriteMessage.class);
    
    /*
     * SendPort used to generate base WriteMessages.
     */
    final CCSendPort port;
    /*
     * Number of bytes written by this CCWriteMessage.
     */
    long bytes;

    public CCWriteMessage(CCSendPort sendPort) throws IOException {        
        this.port = sendPort;
        if(logger.isDebugEnabled()) {
        logger.debug("Created CCWriteMessage; writing to {}",
                Arrays.asList(sendPort.connectedTo()));
        }
        this.port.serOut.reset(true);
    }

    private void checkNotFinished() throws IOException {
        if (this.port.currentMsg == null) {
            throw new IOException(
                    "Operating on a message that was already finished");
        }
    }

    @Override
    public ibis.ipl.SendPort localPort() {
        return port;
    }

    @Override
    public int send() throws IOException {
        checkNotFinished();
        return 0;
    }

    @Override
    public void reset() throws IOException {
        port.serOut.reset();
    }

    @Override
    public long bytesWritten() {
        return port.dataOut.bytesWritten();
    }

    @Override
    public int capacity() throws IOException {
        return -1;
    }

    @Override
    public int remaining() throws IOException {
        return -1;
    }

    @Override
    public void sync(int ticket) throws IOException {
        checkNotFinished();
        port.serOut.flush();
    }

    @Override
    public void flush() throws IOException {
        checkNotFinished();
        port.serOut.flush();
    }

    @Override
    public long finish() throws IOException {
        checkNotFinished();
        
        if(logger.isDebugEnabled()) {
        logger.debug("Finishing a write message from"
                + " {}", this.port.identifier());
        }
        
        port.serOut.flush();
        port.dataOut.close();
        
        if(logger.isDebugEnabled()) {
        logger.debug("Finished writing a message from {}.",
                port.identifier());
        }

        synchronized(port.messageLock) {
            port.currentMsg = null;
            port.messageLock.notifyAll();
        }
        return bytesWritten();
    }

    @Override
    public void finish(IOException e) {
        try {
            checkNotFinished();
        } catch (IOException ex) {
            // ignored
        }
        
        try {
            port.serOut.flush();
        } catch (Throwable e2) {
            // ignored
        }

        try {
            port.dataOut.close();
        } catch (IOException ignoreMe) {
        }
        
        if(logger.isDebugEnabled()) {
        logger.debug("{} has finished a write message.",
                port.identifier());
        }

        synchronized (port.messageLock) {
            port.currentMsg = null;
            port.messageLock.notifyAll();
        }
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        checkNotFinished();
        port.serOut.writeBoolean(value);
    }

    @Override
    public void writeByte(byte value) throws IOException {
        checkNotFinished();
        port.serOut.writeByte(value);
    }

    @Override
    public void writeChar(char value) throws IOException {
        checkNotFinished();
        port.serOut.writeChar(value);
    }

    @Override
    public void writeShort(short value) throws IOException {
        checkNotFinished();
        port.serOut.writeShort(value);
    }

    @Override
    public void writeInt(int value) throws IOException {
        checkNotFinished();
        port.serOut.writeInt(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        checkNotFinished();
        port.serOut.writeLong(value);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        checkNotFinished();
        port.serOut.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        checkNotFinished();
        port.serOut.writeDouble(value);
    }

    @Override
    public void writeString(String value) throws IOException {
        checkNotFinished();
        port.serOut.writeString(value);
    }

    @Override
    public void writeObject(Object value) throws IOException {
        checkNotFinished();
        port.serOut.writeObject(value);
    }

    @Override
    public void writeArray(boolean[] value) throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value);
    }

    @Override
    public void writeArray(byte[] value) throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value);
    }

    @Override
    public void writeArray(char[] value) throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value);
    }

    @Override
    public void writeArray(short[] value) throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value);
    }

    @Override
    public void writeArray(int[] value) throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value);
    }

    @Override
    public void writeArray(long[] value) throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value);
    }

    @Override
    public void writeArray(float[] value) throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value);
    }

    @Override
    public void writeArray(double[] value) throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value);
    }

    @Override
    public void writeArray(Object[] value) throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value);
    }

    @Override
    public void writeArray(boolean[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(byte[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(char[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(short[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(int[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(long[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(float[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(double[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(Object[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        port.serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        checkNotFinished();
        port.serOut.writeByteBuffer(value);
    }
}
