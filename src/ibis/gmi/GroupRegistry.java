package ibis.group;

import ibis.ipl.*;
import java.util.Hashtable;
import java.util.Vector;

final class GroupRegistry implements GroupProtocol {

    Hashtable groups;
    int groupNumber;

    int combinedInvocationID = 0;
    Hashtable combinedInvocations = new Hashtable();
    Vector combinedInvocations2 = new Vector();
    
    public GroupRegistry() {
	groups = new Hashtable();
	groupNumber = 0;
    }
        
    private synchronized void newGroup(String groupName, int groupSize, int rank, int ticket, String type) throws IbisException, IbisIOException {
           
	WriteMessage w;

	w = Group.unicast[rank].newMessage();
	w.writeByte(REGISTRY_REPLY);
	w.writeInt(ticket);

	if (groups.contains(groupName)) { 
	    w.writeByte(CREATE_FAILED);
	} else { 
	    groups.put(groupName, new GroupRegistryData(groupName, groupNumber++, groupSize, type));
	    w.writeByte(CREATE_OK);
	}

	w.send();
	w.finish();		
    } 

    private synchronized void joinGroup(String groupName, long memberID, int rank, int ticket, String [] interfaces) throws IbisException, IbisIOException { 	

	WriteMessage w;

	GroupRegistryData e = (GroupRegistryData) groups.get(groupName);

	if (e == null) { 
	    w = Group.unicast[rank].newMessage();
	    w.writeByte(REGISTRY_REPLY);
	    w.writeInt(ticket);
	    w.writeByte(JOIN_UNKNOWN);
	    w.send();
	    w.finish();		

	} else if (e.joined == e.groupSize) {

	    w = Group.unicast[rank].newMessage();
	    w.writeByte(REGISTRY_REPLY);
	    w.writeInt(ticket);
	    w.writeByte(JOIN_FULL);
	    w.send();
	    w.finish();		

	} else {

	    boolean found = false;

	    for (int i=0;i<interfaces.length;i++) { 
		if (e.type.equals(interfaces[i])) { 
		    found = true;
		    break;
		}
	    }
	    
	    if (!found) {
		w = Group.unicast[rank].newMessage();
		w.writeByte(REGISTRY_REPLY);
		w.writeInt(ticket);
		w.writeByte(JOIN_WRONG_TYPE);
		w.send();
		w.finish();	
	    }
	    
	    e.memberIDs[e.joined] = memberID;
	    e.ranks[e.joined]     = rank;
	    e.tickets[e.joined]   = ticket;
	    e.joined++;

	    if (e.joined == e.groupSize) { 
		for (int i=0;i<e.groupSize;i++) { 
		    
		    w = Group.unicast[e.ranks[i]].newMessage();
		    w.writeByte(REGISTRY_REPLY);
		    w.writeInt(e.tickets[i]);
		    w.writeByte(JOIN_OK);
		    w.writeInt(e.groupNumber);
		    w.writeObject(e.memberIDs);
		    w.send();
		    w.finish();		

		    e.ranks[i]   = 0;
		    e.tickets[i] = 0;
		} 
		e.ranks   = null;
		e.tickets = null;
	    }			
	}
    }

    private synchronized void barrierGroup(String groupName, int rank, int ticket) throws IbisException, IbisIOException { 	

	WriteMessage w;

	GroupRegistryData e = (GroupRegistryData) groups.get(groupName);

	e.b_ranks[e.barrier]   = rank;
	e.b_tickets[e.barrier] = ticket;
	e.barrier++;

	if (e.barrier == e.groupSize) { 
	    for (int i=0;i<e.groupSize;i++) { 
		
		w = Group.unicast[e.b_ranks[i]].newMessage();
		w.writeByte(REGISTRY_REPLY);
		w.writeInt(e.b_tickets[i]);
		w.writeByte(BARRIER_OK);
		w.send();
		w.finish();		
		
		e.b_ranks[i]   = 0;
		e.b_tickets[i] = 0;
	    } 						
	    e.b_ranks   = null;
	    e.b_tickets = null;		
	    e.ready = true;
	}
    }

    private synchronized void findGroup(String groupName, int rank, int ticket) throws IbisException, IbisIOException { 	

	WriteMessage w;

	GroupRegistryData e = (GroupRegistryData) groups.get(groupName);

	if (e == null) { 
	    w = Group.unicast[rank].newMessage();
	    w.writeByte(REGISTRY_REPLY);
	    w.writeInt(ticket);
	    w.writeByte(GROUP_UNKOWN);
	    w.send();
	    w.finish();		

	} else {

	    w = Group.unicast[rank].newMessage();
	    w.writeByte(REGISTRY_REPLY);
	    w.writeInt(ticket);

	    if (!e.ready) { 
		w.writeByte(GROUP_NOT_READY);
	    } else { 
		w.writeByte(GROUP_OK);
		w.writeObject(e.type);
		w.writeInt(e.groupNumber);
		w.writeObject(e.memberIDs);
	    }
	    w.send();
	    w.finish();		
	}
    }

    private void defineCombinedInvocation(ReadMessage r) throws IbisIOException {
	String name;
	String method;
	int rank;
	int cpu;
	int ticket;
	int size;
	int mode;
	int groupID;
        
	groupID = r.readInt();
	cpu = r.readInt();
	ticket = r.readInt();
	name = (String) r.readObject();
	method = (String) r.readObject();
	rank = r.readInt();
	size = r.readInt();
	mode = r.readInt();
	r.finish();

	String id = name + "?" + method + "?" + groupID;;
	CombinedInvocationInfo inf;

	WriteMessage w = Group.unicast[cpu].newMessage();
	w.writeByte(REGISTRY_REPLY);
	w.writeInt(ticket);

	synchronized(this) {
	    inf = (CombinedInvocationInfo) combinedInvocations.get(id);
	    if (inf == null) {
		inf = new CombinedInvocationInfo(combinedInvocationID++, groupID, method, name, mode, size);
		combinedInvocations.put(id, inf);
		combinedInvocations2.addElement(inf);
	    }

	    if (inf.mode != mode || inf.size != size) {
		w.writeByte(COMBINED_FAILED);
		w.writeObject("Inconsistent combined invocation");
		return;
	    }

	    if (inf.present == size) {
		w.writeByte(COMBINED_FAILED);
		w.writeObject("Combined invocation full");
		return;
	    }
	}

	inf.addAndWaitUntilFull(rank, cpu);

	w.writeByte(COMBINED_OK);
	w.writeObject(inf);
	w.send();
	w.finish();
    }
    
    public void handleMessage(ReadMessage r) { 

	try { 

	    byte opcode;
	    
	    int rank;
	    String name;
	    String type;
	    String [] interfaces;
	    int size;
	    int ticket;
	    int number;
	    long memberID;
	    
	    opcode = r.readByte();
	    
	    switch (opcode) { 
	    case CREATE_GROUP:
		rank = r.readInt();		
		ticket = r.readInt();
		name = (String) r.readObject();
		type = (String) r.readObject();
		size = r.readInt();
		r.finish();
		
		if (Group.DEBUG) { 
		    System.out.println(Group._rank + ": Got a CREATE_GROUP(" + name + ", " + type + ", " + size + ") from " + rank + " ticket(" + ticket +")");
		}
		
		newGroup(name, size, rank, ticket, type);				

		if (Group.DEBUG) { 
		    System.out.println(Group._rank + ": CREATE_GROUP(" + name + ", "  + type + ", " + size + ") from " + rank + " HANDLED");
		}
		break;
		
	    case JOIN_GROUP:
		rank = r.readInt();
		ticket = r.readInt();
		name = (String) r.readObject();
		interfaces = (String []) r.readObject();
		memberID = r.readLong();
		r.finish();
		
		if (Group.DEBUG) { 
		    System.out.println(Group._rank + ": Got a JOIN_GROUP(" + name + ", " + interfaces + ") from " + rank);
		}
		
		joinGroup(name, memberID, rank, ticket, interfaces);
		break;		
		
	    case BARRIER_GROUP:
		rank = r.readInt();
		ticket = r.readInt();
		name = (String) r.readObject();
		r.finish();
		
		if (Group.DEBUG) { 
		    System.out.println(Group._rank + ": Got a BARRIER_GROUP(" + name + ")");
		}
		
		barrierGroup(name, rank, ticket);
		break;		

	    case FIND_GROUP:
		rank = r.readInt();
		ticket = r.readInt();
		name = (String) r.readObject();
		r.finish();

		if (Group.DEBUG) { 
		    System.out.println(Group._rank + ": Got a FIND_GROUP(" + name + ")");
		}
		
		findGroup(name, rank, ticket);
		break;		

	    case DEFINE_COMBINED:
		defineCombinedInvocation(r);
		break;
	    }	       
		
	} catch (Exception e) {
	    System.out.println(Group._rank + ": Error in GroupRegistry " + e);
	    System.exit(1);
	}
    }        
}
