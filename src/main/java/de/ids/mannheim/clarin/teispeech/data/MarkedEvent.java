package de.ids.mannheim.clarin.teispeech.data;

/**
 * an {@link Event} where temporal overlap between different turns occurs.
 */

public class MarkedEvent extends Event {
    private final String mark;
    private static int lastEvent = 0;

    public MarkedEvent(String mark) {
        nr = ++lastEvent;
        this.mark = mark;
    }

    @Override
    public String mkTime() {
        return "M_" + nr;
    }

    /**
     * generate ID for end time of marked event
     *
     * @return
     */
    public String mkEndTime() {
        return "ME_" + nr;
    }

    public String mkEndTimeRef() {
        return "#" + mkEndTime();
    }

    public String getMark() {
        return mark;
    };

}
