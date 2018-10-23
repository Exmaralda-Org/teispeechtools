package de.ids.mannheim.clarin.teispeech.data;

public class MarkedEvent extends Event {
    private static int lastEvent = 0;
    public MarkedEvent() {
        nr = lastEvent++;
    }
    @Override
    public String mkTime() {
            return "M_" + nr;
    }
    public String mkEndTime() {
        return "ME_" + nr;
    }

}
