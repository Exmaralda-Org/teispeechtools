package de.ids.mannheim.clarin.teispeech.data;

/**
 * Events in the timeline
 *
 * Events are counted, as their only feature is to be distinct.
 *
 * @author bfi
 */
public abstract class Event {

    protected static int lastEvent = 0;
    protected int nr;

    /**
     * generate ID for the Event
     *
     * @return the id
     */
    public abstract String mkTime();

    /**
     * a reference to the event
     *
     * @return XML-style reference
     */
    public String mkTimeRef() {
        return mkTime();
    };

}
