package edu.harvard.hul.ois.jhove.module.pdf;

import java.io.IOException;

import edu.harvard.hul.ois.jhove.messages.JhoveMessages;

/**
 * Simple class that is the a prototype of a proper header parser class. The aim
 * was to introduce a simple version check for the PDF/A minor version number,
 * see {@link PdfHeader#isVersionValid()}, while not changing anything else
 * through over ambition.
 *
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1 Created 8 Mar 2018:00:46:39
 */

public final class PdfHeader {
    public static final String PDF_HEADER_PREFIX = "PDF-"; //$NON-NLS-1$
    public static final String PDF_VER1_HEADER_PREFIX = PDF_HEADER_PREFIX + "1."; //$NON-NLS-1$
    public static final String PDF_VER2_HEADER_PREFIX = PDF_HEADER_PREFIX + "2."; //$NON-NLS-1$
    public static final String PDF_1_SIG_HEADER = "%" + PDF_VER1_HEADER_PREFIX; //$NON-NLS-1$
    public static final String PDF_2_SIG_HEADER = "%" + PDF_VER2_HEADER_PREFIX; //$NON-NLS-1$
    public static final String POSTSCRIPT_HEADER_PREFIX = "!PS-Adobe-"; //$NON-NLS-1$

    public static final int MAX_VALID_MAJOR_VERSION = 2;

    private final PdfVersion version;
    private final boolean isPdfACompilant;

    /**
     * Factory method for {@link PdfHeader} that parses a new instance using the
     * supplied {@link Parser} instance.
     *
     * @param parser
     *               the {@link Parser} instance that will be used to parse header
     *               details
     * @return a new {@link PdfHeader} instance derived using the supplied
     *         {@link Parser} or <code>null</code> when no header could be found
     *         and parsed.
     * @throws PdfException
     */
    public static PdfHeader parseHeader(final Parser parser) throws PdfException {
        Token token = null;
        String value = null;
        boolean isPdfACompliant = false;
        PdfVersion version = null;

        /* Parse file header. */
        for (;;) {
            final long offset = parser.getOffset();
            if (offset > 1024) {
                throw new PdfMalformedException(
                        JhoveMessages.getMessageInstance(MessageConstants.PDF_HUL_137,
                                "Header not found in first 1024 bytes"),
                        offset); // PDF-HUL-137
            }

            try {
                token = parser.getNext(1024L);
            } catch (final IOException ee) {
                throw new PdfMalformedException(
                        JhoveMessages.getMessageInstance(MessageConstants.PDF_HUL_160, ee.getMessage()),
                        offset); // PDF-HUL-137
            } catch (PdfException e) {
                throw new PdfMalformedException(
                        MessageConstants.PDF_HUL_137,
                        offset); // PDF-HUL-137
            }

            if (token == null) {
                throw new PdfMalformedException(
                        JhoveMessages.getMessageInstance(MessageConstants.PDF_HUL_137, "Header not found"),
                        offset); // PDF-HUL-137
            }

            if (token instanceof Comment) {
                value = ((Comment) token).getValue();
                if ((value.indexOf(PDF_HEADER_PREFIX) == 0) || (value.indexOf(POSTSCRIPT_HEADER_PREFIX) == 0)) {
                    version = PdfVersion.fromHeaderLine(value, offset);
                    break;
                }
            }
        }

        try {
            isPdfACompliant = isTokenPdfACompliant(parser.getNext());
        } catch (final Exception excep) {
            // Most likely a ClassCastException on a non-comment
            isPdfACompliant = false;
        }
        // Check for PDF/A conformance. The next item must be
        // a comment with four characters, each greater than 127
        return new PdfHeader(version, isPdfACompliant);
    }

    /**
     * Creates a new {@link PdfHeader} instance using the passed parameters.
     *
     * @param versionString
     *                        the version number from the PDF Header, should be of
     *                        form
     *                        <code>1.x</code> where x should be of the range 0-7.
     * @param isPdfaCompliant
     *                        boolean flag indicating if the PDF/A is compliant or
     *                        non
     *                        compliant with JHOVE's PDF/A profile.
     * @return a {@link PdfHeader} instance initialised using
     *         <code>versionString</code> and <code>isPdfaCompliant</code>.
     * @throws NullPointerException
     *                              when parameter <code>versionString</code> is
     *                              null.
     */
    static PdfHeader fromValues(final PdfVersion version,
            final boolean isPdfaCompliant) {
        if (version == null)
            throw new NullPointerException(
                    "Parameter version can not be null.");
        return new PdfHeader(version, isPdfaCompliant);
    }

    private static boolean isTokenPdfACompliant(final Token token) {
        final String cmt = ((Comment) token).getValue();
        final char[] cmtArray = cmt.toCharArray();
        int ctlcnt = 0;
        for (int i = 0; i < 4; i++) {
            if (cmtArray[i] > 127) {
                ctlcnt++;
            }
        }
        return (ctlcnt > 3);
    }

    /**
     *
     */
    private PdfHeader(final PdfVersion version,
            final boolean isPdfaCompliant) {
        this.version = version;
        this.isPdfACompilant = isPdfaCompliant;
    }

    /**
     * @return the version string parsed from the PDF Header
     */
    public String getVersionString() {
        return this.version.toString();
    }

    /**
     * @return true if the header is considered PDF/A compliant, otherwise false
     */
    public boolean isPdfACompliant() {
        return this.isPdfACompilant;
    }
}
