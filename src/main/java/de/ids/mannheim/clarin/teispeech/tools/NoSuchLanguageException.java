package de.ids.mannheim.clarin.teispeech.tools;

public class NoSuchLanguageException extends Exception {
    private static final long serialVersionUID = 7897383565099486726L;

    public NoSuchLanguageException(String message) {
        super(message);
    }
}
