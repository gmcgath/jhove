package edu.harvard.hul.ois.jhove.module.pdf;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.harvard.hul.ois.jhove.messages.JhoveMessages;

final class PdfVersion {
    final PdfVersion.PdfMajorVersions majorVersion;
    final int minorVersion;

    enum PdfMajorVersions {
        PDF_2(2, 0, 0),
        PDF_1(1, 0, 7);

        final int version;
        final int maxMinorVersion;
        final int minMinorVersion;

        static final String getMajorVersionRange() {
            return String.format("[%d-%d]", PDF_1.version, PDF_2.version);
        }

        static final PdfVersion.PdfMajorVersions fromMajorVersion(final int majorVersion) throws PdfInvalidException {
            for (final PdfVersion.PdfMajorVersions version : PdfMajorVersions.values()) {
                if (version.version == majorVersion) {
                    return version;
                }
            }
            throw new PdfInvalidException(JhoveMessages.getMessageInstance(MessageConstants.PDF_HUL_161,
                    String.format("PDF major version must be in the range %s, major version number: %d detected",
                            getMajorVersionRange(), majorVersion)));
        }

        private PdfMajorVersions(final int version, final int minMinorVersion, final int maxMinorVersion) {
            this.version = version;
            this.minMinorVersion = minMinorVersion;
            this.maxMinorVersion = maxMinorVersion;
        }

        final String getMinorVersionRange() {
            return String.format("%d.[%d-%d]", this.version, this.minMinorVersion, this.maxMinorVersion);
        }

        final boolean isMinorValid(final int minorVersion) {
            return minorVersion >= this.minMinorVersion && minorVersion <= this.maxMinorVersion;
        }
    }

    static final Pattern versionPattern = Pattern.compile("(\\d)\\.(\\d)");

    static final PdfVersion fromVersionString(final String version) throws PdfInvalidException {
        final Matcher matcher = versionPattern.matcher(version);
        try {
            if (matcher.find()) {
                final PdfVersion.PdfMajorVersions majorVersion = PdfVersion.PdfMajorVersions
                        .fromMajorVersion(Integer.parseInt(matcher.group(1)));
                final int minorVersion = Integer.parseInt(matcher.group(2));
                checkMinorVersion(majorVersion, minorVersion);
                return new PdfVersion(majorVersion, minorVersion);
            }
        } catch (final NumberFormatException nfe) {
            /**
             * Fall throuh to throw the below, same as if the regex didn't match
             */
        }
        throw new PdfInvalidException(
                JhoveMessages.getMessageInstance(MessageConstants.PDF_HUL_162, "Invalid: " + version));
    }

    static final void checkMinorVersion(final PdfVersion.PdfMajorVersions majorVersion, final int minorVersion)
            throws PdfInvalidException {
        if (!majorVersion.isMinorValid(minorVersion)) {
            throw new PdfInvalidException(JhoveMessages.getMessageInstance(MessageConstants.PDF_HUL_148,
                    String.format("PDF %d has legal minor version range: %s, minor version number %d detected",
                            majorVersion.version, majorVersion.getMinorVersionRange(), minorVersion)));
        }
    }

    static final PdfVersion fromHeaderLine(final String headerLine, final long offset)
            throws PdfMalformedException, PdfInvalidException {
        String version = headerLine;
        if ((headerLine.indexOf(PdfHeader.POSTSCRIPT_HEADER_PREFIX) == 0)
                && headerLine.indexOf(PdfHeader.PDF_HEADER_PREFIX) >= PdfHeader.POSTSCRIPT_HEADER_PREFIX.length()) {
            version = headerLine.substring(headerLine.indexOf(PdfHeader.PDF_HEADER_PREFIX));
        }
        if (version.indexOf(PdfHeader.PDF_HEADER_PREFIX) == 0) {
            version = parseSubstring(headerLine, 4, 7, offset);
        }
        return fromVersionString(version);
    }

    static final String parseSubstring(final String toParse, final int begin, final int end, final long offset)
            throws PdfMalformedException {
        try {
            return toParse.substring(begin, end);
        } catch (final IndexOutOfBoundsException e) {
            throw new PdfMalformedException(
                    JhoveMessages.getMessageInstance(MessageConstants.PDF_HUL_155.getId(),
                            MessageFormat.format(MessageConstants.PDF_HUL_155.getMessage(), toParse)),
                    offset); // PDF-HUL-155
        }
    }

    private PdfVersion(final PdfVersion.PdfMajorVersions majorVersion, final int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    @Override
    public String toString() {
        return String.format("%d.%d", this.majorVersion.version, this.minorVersion);
    }
}