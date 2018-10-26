package de.ids.mannheim.clarin.teispeech.data;

/**
 * an {@link Event} ending a turn
 */
public class EndEvent extends Event {
    public EndEvent() {
        nr = lastEvent;
    }

    @Override
    public String mkTime() {
        return "E_" + Integer.toString(nr);
    }

}
