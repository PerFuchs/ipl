package ibis.ipl.impl.messagePassing;

class ReadFragment {

    int		msgHandle;
    int		msgSize;
    ReadMessage msg;
    ReadFragment next;
    boolean	lastFrag;

    ReadFragment() {
	next = null;
    }

    void clear() {
	msg.in.resetMsg(msgHandle);
    }

}
