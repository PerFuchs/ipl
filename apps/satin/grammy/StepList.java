// File: $Id$

/**
 * Maintains a fixed-length list of compression steps in order of decreasing
 * gain.
 */

public class StepList {
    private Step steps[];
    private int ix;

    public StepList( int n )
    {
        steps = new Step[n];
    }

    /**
     * Given a compression step, adds it to the list, possibly
     * removing an existing one that is less interesting.
     */
    public void add( Step s )
    {
        int gain = s.getGain();

        if( ix<steps.length ){
            steps[ix++] = s;
        }
        else {
            int i = ix-1;

            if( steps[i].getGain()>=gain ){
                // The new one does not even improve on the worst
                // one in the list. Go away.
                return;
            }
            steps[i] = s;
        }

        // Now move the entry up until it has reached its rightful
        // position.
        int pos = ix;
        while( pos>1 ){
            pos--;
            if( steps[pos].getGain()>steps[pos-1].getGain() ){
                // Move the entry up.
                Step tmp = steps[pos];
                steps[pos] = steps[pos-1];
                steps[pos-1] = tmp;
            }
            else {
                break;
            }
        }
    }

    /**
     * Returns an array containing all the steps.
     */
    public Step[] toArray()
    {
	Step res[] = new Step[ix];
	System.arraycopy( steps, 0, res, 0, ix );
        return res;
    }

    /** Returns the length of the step list. */
    public int getLength() { return ix; }
}
