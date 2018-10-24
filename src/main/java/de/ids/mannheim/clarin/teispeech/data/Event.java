package de.ids.mannheim.clarin.teispeech.data;

public abstract class Event {

    protected static int lastEvent = 0;
    protected int nr;

    public abstract String mkTime();

    public String mkTimeRef() {
        return "#" + mkTime();
    };
}
