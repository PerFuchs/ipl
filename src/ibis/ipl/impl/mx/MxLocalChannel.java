package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class MxLocalChannel implements ReadChannel, WriteChannel {

	private static Logger logger = Logger.getLogger(MxLocalChannel.class);
	private static final int BYTE_BUFFER_SIZE2 = 7 * Config.PRIMITIVE_BUFFER_SIZE + SendBuffer.SIZEOF_HEADER + SendBuffer.SIZEOF_PADDING;
	
	private boolean closed = false;
	
	//The channel capacity
	private ByteBuffer buffer;
	
	MxLocalChannel() {
		logger.debug("Local channel created");
		buffer = ByteBuffer.allocate(BYTE_BUFFER_SIZE2); //We don't need a direct buffer here
	}
	
	/* Common: */
	
	public synchronized void close() {
		//logger.debug("Local channel closed");
		closed = true;
		notifyAll();
	}

	/* ReadChannel methods: */
	
	public synchronized boolean isOpen() {
		return !closed;
	}

	public synchronized int poll() throws ClosedChannelException {
		//logger.debug("poll");
		int result = buffer.position();
		if(closed && result == 0) {
			return -1;
		}
		return result = buffer.position();
	}

	public synchronized int poll(long timeout) throws ClosedChannelException {
		//logger.debug("poll(timeout)");
		long deadline = System.currentTimeMillis() + timeout;
		long time;
		int result = buffer.position();
		while (result == 0) {
			if(closed) {
				return -1;
			}
			time = System.currentTimeMillis();
			if(timeout != 0 && time > deadline) {
				return 0;
			}
			try {
				wait(deadline - time);
			} catch (InterruptedException e) {
				// ignore
			}
			result = buffer.position();
		}
		return result;
	}

	public int read(ByteBuffer dest, long timeout) throws IOException {
		logger.debug("read");
		int size = poll(timeout);
		if (size >= 0) {
			buffer.flip();
			dest.put(buffer);
			// empty buffer and notify blocked write methods
			buffer.clear();
			synchronized(this) {
				notifyAll();
			}
			return size;
		}
		return size;
	}

	/* WriteChannel methods */
	
	public void flush() throws IOException {
		logger.debug("flush");
		//empty implementation
	}

	public boolean isFinished() throws IOException {
		//empty implementation
		return true;
	}

	public synchronized void write(ByteBuffer src) throws IOException {
		logger.debug("write");
		SendBuffer sb = SendBuffer.get();
		
		src.mark();
		try {
			sb.bytes.put(src);
		} catch (BufferOverflowException e) {
			throw new IOException("ByteBuffer too large to send");
		}
		write(sb);
		
		src.reset();
		SendBuffer.recycle(sb);
		notifyAll();
	}

	public synchronized void write(SendBuffer src) throws IOException {
		logger.debug("write SendBuffer");
		while(buffer.remaining() < src.remaining()) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
		logger.debug("Space free in buffer");
		src.mark();
		for(ByteBuffer buf : src.byteBuffers) {
			buffer.put(buf);
		}
		src.reset();
		notifyAll();
		logger.debug("Write finished");
	}



}