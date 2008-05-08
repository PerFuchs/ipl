package ibis.ipl.impl.mx;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class MxAddress implements Serializable {

	private static final long serialVersionUID = 5057056202239491976L;

	static MxAddress fromBytes(byte[] bytes) throws Exception {
		//TODO generate an address from the implementation data of an ibis identifier
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.order(ByteOrder.BIG_ENDIAN);
		
		//FIXME Maybe we shouldn't use characters directly (UTF problems?)
		if(buf.getChar() != 'm' || buf.getChar() != 'x') {
			throw new Exception("Not an Mx Address");
		}
		return new MxAddress(buf.getLong(), buf.getInt());
	}
	
	final long nicId;
	final int endpointId;

	protected MxAddress(String hostname, int endpoint_id) {
		this.nicId = JavaMx.getNicId(hostname);
		this.endpointId = endpoint_id;
	}
	
	protected MxAddress(long nic_id, int endpoint_id) {
		this.nicId = nic_id;
		this.endpointId = endpoint_id;
	}

//	@Override
	public String toString() {
		return "mx::" + Long.toHexString(nicId) + "<" + endpointId + ">";
	}

	public byte[] toBytes() {
		byte[] bytes = new byte[2 * Character.SIZE + Long.SIZE + Integer.SIZE];
		
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.order(ByteOrder.BIG_ENDIAN);
		//FIXME Maybe we shouldn't use characters directly (UTF problems?)
		buf.putChar('m');
		buf.putChar('x');
		buf.putLong(nicId);
		buf.putLong(endpointId);
		
		return bytes;
	}
	
}
