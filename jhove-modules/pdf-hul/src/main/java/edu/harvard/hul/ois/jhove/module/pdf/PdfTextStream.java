package edu.harvard.hul.ois.jhove.module.pdf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PdfTextStream {
    private final byte[] streamData;

    public PdfTextStream(PdfStream pdfStream, final RandomAccessFile raf) throws IOException {
        Stream stream = pdfStream.getStream();
        stream.initRead(raf);
        this.streamData = stream.getRawData();
    }

    public void validate() throws PdfInvalidException {
        String chars = new String(this.streamData, StandardCharsets.UTF_8);
        if (isTextStream(chars) && !isHexStream(chars)) {
            int textStart = chars.indexOf("BT");
            int parenthesesCount = 0;
            char lastChar = 00;
            boolean parenthesesFound = false;
            for (int i = textStart; i < chars.lastIndexOf("ET"); i++) {
                if (isReverseSolidus(lastChar)) {
                    // Ignore characters that are escaped, these may be legitimate unbalanced parentheses
                    lastChar = chars.charAt(i);
                    continue;
                }
                if (chars.charAt(i) == '(') {
                    parenthesesFound = true;
                    parenthesesCount++;
                } else if (chars.charAt(i) == ')') {
                    parenthesesFound = true;
                    parenthesesCount--;
                }
                lastChar = chars.charAt(i);
            }
            if (parenthesesCount != 0 || !parenthesesFound) {
                // The number of opening and closing braces don't match
                // This is a sign that the text stream is malformed
                throw new PdfInvalidException(MessageConstants.PDF_HUL_164); // PDF-HUL-123
            }
        }
    }

    private boolean isReverseSolidus(final char toValidate) {
        // Returns true if the character is a reverse solidus, false otherwise
        return toValidate == '\\';
    }

    private boolean isTextStream(final String toValidate) {
        // Check if the stream is a text stream
        if (toValidate.contains("BT") && toValidate.contains("ET")) {
            // It contains both a begin and end text operator, get the first occurrence of BT and the last occurrence of ET
            int firstBt = toValidate.indexOf("BT");
            int lastEt = toValidate.lastIndexOf("ET");
            if ((firstBt < lastEt) &&
                    (firstBt == 0 || (Arrays.stream(Tokenizer.WHITESPACES).anyMatch(Character.valueOf(toValidate.charAt(firstBt -1))::equals))) &&
                    (Arrays.stream(Tokenizer.WHITESPACES).anyMatch(Character.valueOf(toValidate.charAt(lastEt -1))::equals))) {
                // Checks that the first occurrence of BT is before the last occurrence of ET, AND that the BT is the first char in the string OR
                // the character before BT is a whitespace character, AND that the character before ET is a whitespace character.
                // If so then return true
                return true;
            }
        }
        return false;
    }

    private boolean isHexStream(final String toValidate) {
        // Check if the stream is a hex text stream, denoted by the presence of < and > rather than ( and )
        char lastChar = 00;
        for (int i = toValidate.indexOf("BT"); i < toValidate.lastIndexOf("ET"); i++) {
            // Work between the BT and ET operators
            if (toValidate.charAt(i) == '(') {
                // We found an opening parenthesis, check if it's escaped
                if (!isReverseSolidus(lastChar)) {
                    // If it's not escaped then it's a literal text string comprised of characters
                    return false;
                }
            } else if (toValidate.charAt(i) == '<') {
                // We found an opening angle bracket, check if it's escaped
                if (!isReverseSolidus(lastChar)) {
                    // If it's not escaped then it's a hex string comprised of pairs of hexadecimal digits
                    return true;
                }
            }
            lastChar = toValidate.charAt(i);
        }
        // If we reach this point then the stream is not a hex stream
        return false;
    }

}
