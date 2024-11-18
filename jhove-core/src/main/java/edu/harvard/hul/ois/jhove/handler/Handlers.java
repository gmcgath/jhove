package edu.harvard.hul.ois.jhove.handler;

public class Handlers {
    private Handlers() {
        throw new AssertionError("Instantiating utility class...");
    }

    public static final String makeInfoLink(final String reportingModule, final String jhoveId) {
        return String.format("https://github.com/openpreserve/jhove/wiki/%s-Messages#%s",
                reportingModule, jhoveId.toLowerCase());
    }
}
