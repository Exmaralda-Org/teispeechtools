package de.ids.mannheim.clarin.teispeech.data;

/**
 * an {@link Event} where temporal overlap between different turns occurs.
 */

public class MarkedEvent extends Event {
    private static int lastEvent = 0;
    private final String mark;

    public MarkedEvent(String mark) {
        nr = lastEvent++;
        this.mark = mark;
    }

    @Override
    public String mkTime() {
        return "M_" + nr;
    }

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
