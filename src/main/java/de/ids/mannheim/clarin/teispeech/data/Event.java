package de.ids.mannheim.clarin.teispeech.data;

/**
 * Events in the timeline
 *
 * Events are counted, as their only feature is to be distinct.
 */
public abstract class Event {

    protected static int lastEvent = 0;
    protected int nr;

    public abstract String mkTime();

    /**
     * a reference to the event
     *
     * @return XML-style reference
     */
    public String mkTimeRef() {
        return "#" + mkTime();
    };
}