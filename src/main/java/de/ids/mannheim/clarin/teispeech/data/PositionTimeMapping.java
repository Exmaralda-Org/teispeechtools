package de.ids.mannheim.clarin.teispeech.data;

/**
 * a time anchor in an XML document
 *
 * @author Thomas Schmidt
 */
class PositionTimeMapping {

    /**
     * the position in the XML file
     */
    public final int position;

    /**
     * an identifier for the time, e.g. a measurement
     */
    public final String timeID;

    public PositionTimeMapping(int position, String timeID) {
        this.position = position;
        this.timeID = timeID;
    }

}
