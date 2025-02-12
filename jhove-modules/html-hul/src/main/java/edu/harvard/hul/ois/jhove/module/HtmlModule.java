/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment Copyright 2004-2007 by
 * JSTOR and the President and Fellows of Harvard College
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 **********************************************************************/

package edu.harvard.hul.ois.jhove.module;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import edu.harvard.hul.ois.jhove.Agent;
import edu.harvard.hul.ois.jhove.AgentType;
import edu.harvard.hul.ois.jhove.Document;
import edu.harvard.hul.ois.jhove.DocumentType;
import edu.harvard.hul.ois.jhove.ErrorMessage;
import edu.harvard.hul.ois.jhove.ExternalSignature;
import edu.harvard.hul.ois.jhove.Identifier;
import edu.harvard.hul.ois.jhove.IdentifierType;
import edu.harvard.hul.ois.jhove.InfoMessage;
import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.ModuleBase;
import edu.harvard.hul.ois.jhove.Property;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.Signature;
import edu.harvard.hul.ois.jhove.SignatureType;
import edu.harvard.hul.ois.jhove.SignatureUseType;
import edu.harvard.hul.ois.jhove.TextMDMetadata;
import edu.harvard.hul.ois.jhove.module.html.Html3_2DocDesc;
import edu.harvard.hul.ois.jhove.module.html.Html4_01FrameDocDesc;
import edu.harvard.hul.ois.jhove.module.html.Html4_01StrictDocDesc;
import edu.harvard.hul.ois.jhove.module.html.Html4_01TransDocDesc;
import edu.harvard.hul.ois.jhove.module.html.Html4_0FrameDocDesc;
import edu.harvard.hul.ois.jhove.module.html.Html4_0StrictDocDesc;
import edu.harvard.hul.ois.jhove.module.html.Html4_0TransDocDesc;
import edu.harvard.hul.ois.jhove.module.html.HtmlCharStream;
import edu.harvard.hul.ois.jhove.module.html.HtmlDocDesc;
import edu.harvard.hul.ois.jhove.module.html.JHDoctype;
import edu.harvard.hul.ois.jhove.module.html.JHElement;
import edu.harvard.hul.ois.jhove.module.html.JHOpenTag;
import edu.harvard.hul.ois.jhove.module.html.JHXmlDecl;
import edu.harvard.hul.ois.jhove.module.html.MessageConstants;
import edu.harvard.hul.ois.jhove.module.html.ParseException;
import edu.harvard.hul.ois.jhove.module.html.ParseHtml;
import edu.harvard.hul.ois.jhove.module.html.Token;
import edu.harvard.hul.ois.jhove.module.html.TokenMgrError;
import edu.harvard.hul.ois.jhove.module.xml.HtmlMetadata;

/**
 * Module for identification and validation of HTML files.
 *
 * HTML is different from most of the other documents in that sloppy
 * construction is practically assumed in the specification. This module attempt
 * to report as many errors as possible and recover reasonably from errors. To
 * do this, there is more heuristic behavior built into this module than into
 * the more straightforward ones.
 *
 * XHTML is recognized by this module, but is handed off to the XML module for
 * processing. If the XML module is missing (which it shouldn't be if you've
 * installed the JHOVE application without modifications), this won't be able to
 * deal with XHTML files.
 *
 * HTML should be placed ahead of XML in the module order. If the XML module
 * sees an XHTML file first, it will recognize it as XHTML, but won't be able to
 * report the complete properties.
 *
 * The HTML module uses code created with the JavaCC parser generator and
 * lexical analyzer generator. There is apparently a bug in JavaCC which causes
 * blank lines not to be counted in certain cases, causing lexical errors to be
 * reported with incorrect line numbers.
 *
 * @author Gary McGath
 *
 */
public class HtmlModule extends ModuleBase {

    /******************************************************************
     * PRIVATE CLASS FIELDS.
     ******************************************************************/
    private static final String TRANSITIONAL = "Transitional";
    private static final String STRICT = "Strict";
    private static final String FRAMESET = "Frameset";
    private static final String HTML_4_0 = "HTML 4.0";
    private static final String HTML_4_01 = "HTML 4.01";
    private static final String XHTML_1_0 = "XHTML 1.0";
    private static final String XHTML_1_1_STR = "XHTML 1.1";

    private static final String NAME = "HTML-hul";
    private static final String RELEASE = "1.4.5";
    private static final int[] DATE = { 2025, 02, 05 };
    private static final String[] FORMAT = { "HTML" };
    private static final String COVERAGE = "HTML 3.2, HTML 4.0 Strict,"
            + "HTML 4.0 Transitional, HTML 4.0 Frameset, "
            + "HTML 4.01 Strict, HTML 4.01 Transitional, HTML 4.01 Frameset"
            + "XHTML 1.0 Strict, XHTML 1.0 Transitional, XHTML 1.0 Frameset"
            + "XHTML 1.1";

    private static final String[] MIMETYPE = { "text/html" };
    private static final String WELLFORMED = "An HTML file is well-formed "
            + "if it meets the criteria defined in the HTML 3.2 specification "
            + "(W3C Recommendation, 14-Jan-1997), "
            + "the HTML 4.0 specification (W3C Recommendation, 24-Apr-1998, "
            + "the HTML 4.01 specification (W3C Recommendation, 24-Dec-1999, "
            + "the XHTML 1.0 specification (W3C Recommendation, 26-Jan-2000, "
            + "revised 1-Aug-2002, "
            + "or the XHTML 1.1 specification (W3C Recommendation, 31-May-2001";
    private static final String VALIDITY = "An HTML file is valid if it is "
            + "well-formed and has a valid DOCTYPE declaration.";
    private static final String REPINFO = "Languages, title, META tags, "
            + "frames, links, scripts, images, citations, defined terms, "
            + "abbreviations, entities, Unicode entity blocks";
    private static final String NOTE = "";
    private static final String RIGHTS = "Copyright 2004-2007 by JSTOR and "
            + "the President and Fellows of Harvard College. "
            + "Released under the GNU Lesser General Public License.";

    /******************************************************************
     * PRIVATE INSTANCE FIELDS.
     ******************************************************************/

    /* Doctype extracted from document */
    protected String _doctype;

    /* Constants for the recognized flavors of HTML */
    public static final int HTML_3_2 = 1, HTML_4_0_STRICT = 2,
            HTML_4_0_FRAMESET = 3, HTML_4_0_TRANSITIONAL = 4,
            HTML_4_01_STRICT = 5, HTML_4_01_FRAMESET = 6,
            HTML_4_01_TRANSITIONAL = 7, XHTML_1_0_STRICT = 8,
            XHTML_1_0_TRANSITIONAL = 9, XHTML_1_0_FRAMESET = 10, XHTML_1_1 = 11;

    /* Profile names, matching the above indices */
    private static final String[] PROFILENAMES = { null, null, // there are no
                                                               // profiles for
                                                               // HTML 3.2
            STRICT, FRAMESET, TRANSITIONAL, STRICT, FRAMESET, TRANSITIONAL,
            STRICT, FRAMESET, TRANSITIONAL, null // there
                                                 // are no
                                                 // profiles
                                                 // for
                                                 // XHTML
                                                 // 1.1
    };

    /* Version names, matching the above indices */
    private static final String[] VERSIONNAMES = { null, "HTML 3.2", HTML_4_0,
            HTML_4_0, HTML_4_0, HTML_4_01, HTML_4_01, HTML_4_01, XHTML_1_0,
            XHTML_1_0, XHTML_1_0, XHTML_1_1_STR };

    /* Flag to know if the property TextMDMetadata is to be added */
    protected boolean _withTextMD = false;
    /* Hold the information needed to generate a textMD metadata fragment */
    protected TextMDMetadata _textMD;

    /******************************************************************
     * CLASS CONSTRUCTOR.
     ******************************************************************/
    /**
     * Instantiate an <tt>HtmlModule</tt> object.
     */
    public HtmlModule() {
        super(NAME, RELEASE, DATE, FORMAT, COVERAGE, MIMETYPE, WELLFORMED,
                VALIDITY, REPINFO, NOTE, RIGHTS, false);

        _vendor = Agent.harvardInstance();

        /* HTML 3.2 spec */
        Document doc = new Document("HTML 3.2 Reference Specification",
                DocumentType.REPORT);
        Agent w3cAgent = Agent.newW3CInstance();
        doc.setPublisher(w3cAgent);

        Agent dRaggett = new Agent.Builder("Dave Raggett", AgentType.OTHER)
                .build();
        doc.setAuthor(dRaggett);

        doc.setDate("1997-01-14");
        doc.setIdentifier(
                new Identifier("http://www.w3c.org/TR/REC-html32-19970114",
                        IdentifierType.URL));
        _specification.add(doc);

        /* HTML 4.0 spec */
        doc = new Document("HTML 4.0 Specification", DocumentType.REPORT);
        doc.setPublisher(w3cAgent);
        doc.setAuthor(dRaggett);
        Agent leHors = new Agent.Builder("Arnaud Le Hors", AgentType.OTHER)
                .build();
        doc.setAuthor(leHors);
        Agent jacobs = new Agent.Builder("Ian Jacobs", AgentType.OTHER).build();
        doc.setAuthor(jacobs);
        doc.setDate("1998-04-24");
        doc.setIdentifier(
                new Identifier("http://www.w3.org/TR/1998/REC-html40-19980424/",
                        IdentifierType.URL));
        _specification.add(doc);

        /* HTML 4.01 spec */
        doc = new Document("HTML 4.01 Specification", DocumentType.REPORT);
        doc.setPublisher(w3cAgent);
        doc.setAuthor(dRaggett);
        doc.setAuthor(leHors);
        doc.setAuthor(jacobs);
        doc.setDate("1999-12-24");
        doc.setIdentifier(new Identifier(
                "http://www.w3.org/TR/1999/REC-html401-19991224/",
                IdentifierType.URL));
        _specification.add(doc);

        /* XHTML 1.0 spec */
        doc = new Document(
                "XHTML(TM) 1.0 The Extensible HyperText Markup Language "
                        + "(Second Edition)",
                DocumentType.REPORT);
        doc.setPublisher(w3cAgent);
        doc.setDate("2002-08-01");
        doc.setIdentifier(new Identifier("http://www.w3.org/TR/xhtml1/",
                IdentifierType.URL));
        _specification.add(doc);

        /* XHTML 1.1 spec */
        doc = new Document(" XHTML(TM) 1.1 - Module-based XHTML",
                DocumentType.REPORT);
        doc.setPublisher(w3cAgent);
        doc.setDate("2001-05-31");
        doc.setIdentifier(new Identifier(
                "http://www.w3.org/TR/2001/REC-xhtml11-20010531/",
                IdentifierType.URL));
        _specification.add(doc);

        /*
         * XHTML 2.0 spec -- NOT included yet; this is presented in
         * "conditionalized-out" form just as a note for future expansion.
         * if (false) {
         * doc = new Document("XHTML 2.0, W3C Working Draft",
         * DocumentType.OTHER);
         * doc.setPublisher(w3cAgent);
         * doc.setDate("22-07-2004");
         * doc.setIdentifier(new Identifier(
         * "http://www.w3.org/TR/2004/WD-xhtml2-20040722/",
         * IdentifierType.URL));
         * _specification.add(doc);
         * }
         */

        Signature sig = new ExternalSignature(".html", SignatureType.EXTENSION,
                SignatureUseType.OPTIONAL);
        _signature.add(sig);
        sig = new ExternalSignature(".htm", SignatureType.EXTENSION,
                SignatureUseType.OPTIONAL);
        _signature.add(sig);
    }

    /**
     * Parse the content of a purported HTML stream digital object and store the
     * results in RepInfo.
     *
     *
     * @param stream
     *                   An InputStream, positioned at its beginning, which is
     *                   generated from the object to be parsed. If multiple calls
     *                   to
     *                   <code>parse</code> are made on the basis of a nonzero value
     *                   being returned, a new InputStream must be provided each
     *                   time.
     *
     * @param info
     *                   A fresh (on the first call) RepInfo object which will be
     *                   modified to reflect the results of the parsing If multiple
     *                   calls to <code>parse</code> are made on the basis of a
     *                   nonzero
     *                   value being returned, the same RepInfo object should be
     *                   passed
     *                   with each call.
     *
     * @param parseIndex
     *                   Must be 0 in first call to <code>parse</code>. If
     *                   <code>parse</code> returns a nonzero value, it must be
     *                   called
     *                   again with <code>parseIndex</code> equal to that return
     *                   value.
     *
     * @return parseInt
     */
    @Override
    public int parse(InputStream stream, RepInfo info, int parseIndex) {
        if (parseIndex != 0) {
            // Coming in with parseIndex = 1 indicates that we've determined
            // this is XHTML; so we invoke the XML module to parse it.
            // If parseIndex is 100, this is the first invocation of the
            // XML module, so we call it with 0; otherwise we call it with
            // the value of parseIndex.
            if (isXmlAvailable()) {
                edu.harvard.hul.ois.jhove.module.XmlModule xmlMod = new edu.harvard.hul.ois.jhove.module.XmlModule();
                if (parseIndex == 100) {
                    parseIndex = 0;
                }
                xmlMod.setApp(_app);
                xmlMod.setBase(_je);
                xmlMod.setDefaultParams(_defaultParams);
                try {
                    xmlMod.applyDefaultParams();
                } catch (Exception e) {
                    // really shouldn't happen
                }
                xmlMod.setXhtmlDoctype(_doctype);
                return xmlMod.parse(stream, info, parseIndex);
            }
            // The XML module shouldn't be missing from any installation,
            // but someone who really wanted to could remove it. In
            // that case, you deserve what you get.
            info.setMessage(new ErrorMessage(
                    MessageConstants.JHOVE_1));
            info.setWellFormed(false); // Treat it as completely wrong
            return 0;
        }
        /* parseIndex = 0, first call only */
        _doctype = null;
        // Test if textMD is to be generated
        if (_defaultParams != null) {
            Iterator iter = _defaultParams.iterator();
            while (iter.hasNext()) {
                String param = (String) iter.next();
                if ("withtextmd=true".equalsIgnoreCase(param)) {
                    _withTextMD = true;
                }
            }
        }

        initParse();
        info.setFormat(_format[0]);
        info.setMimeType(_mimeType[0]);
        info.setModule(this);

        if (_textMD == null || parseIndex == 0) {
            _textMD = new TextMDMetadata();
        }
        /*
         * We may have already done the checksums while converting a temporary
         * file.
         */
        setupDataStream(stream, info);

        ParseHtml parser;
        HtmlMetadata metadata = null;
        HtmlCharStream cstream;
        try {
            cstream = new HtmlCharStream(_dstream, "ISO-8859-1");
            parser = new ParseHtml(this, cstream);
        } catch (UnsupportedEncodingException e) {
            info.setMessage(new ErrorMessage(
                    MessageConstants.JHOVE_2, e.getMessage()));
            info.setWellFormed(false);
            return 0; // shouldn't happen!
        }
        int type = 0;
        try {
            List elements = parser.HtmlDoc();
            if (elements.isEmpty()) {
                // Consider an empty document bad
                info.setWellFormed(false);
                info.setMessage(new ErrorMessage(
                        MessageConstants.JHOVE_3));
                return 0;
            }
            type = checkDoctype(elements);
            if (type < 0) {
                info.setWellFormed(false);
                info.setMessage(new ErrorMessage(
                        MessageConstants.HTML_HUL_15));
                return 0;
            }
            /*
             * Check if there is at least one html, head, body or title tag. A
             * plain text document might be interpreted as a single PCDATA,
             * which is in some ethereal sense well-formed HTML, but it's
             * pointless to consider it such. It might also use angle brackets
             * as a text delimiter, and that shouldn't count as HTML either.
             */
            boolean hasElements = false;
            Iterator iter = elements.iterator();
            while (iter.hasNext()) {
                Object o = iter.next();
                if (o instanceof JHOpenTag) {
                    String name = ((JHOpenTag) o).getName();
                    if ("html".equals(name) || "head".equals(name)
                            || "body".equals(name) || "title".equals(name)) {
                        hasElements = true;
                    }
                    break;
                }
            }
            if (!hasElements) {
                info.setMessage(new ErrorMessage(
                        MessageConstants.HTML_HUL_17));
                info.setWellFormed(false);
                return 0;
            }

            // CRLF from HtmlCharStream ...
            String lineEnd = cstream.getKindOfLineEnd();
            if (lineEnd == null) {
                info.setMessage(
                        new InfoMessage(MessageConstants.HTML_HUL_23));
                _textMD.setLinebreak(TextMDMetadata.NILL);
            } else if ("CR".equalsIgnoreCase(lineEnd)) {
                _textMD.setLinebreak(TextMDMetadata.LINEBREAK_CR);
            } else if ("LF".equalsIgnoreCase(lineEnd)) {
                _textMD.setLinebreak(TextMDMetadata.LINEBREAK_LF);
            } else if ("CRLF".equalsIgnoreCase(lineEnd)) {
                _textMD.setLinebreak(TextMDMetadata.LINEBREAK_CRLF);
            }

            if (type == 0) {
                /*
                 * If we can't find a doctype, it still might be XHTML if the
                 * elements start with an XML declaration and the root element
                 * is "html"
                 */
                switch (seemsToBeXHTML(elements)) {
                    case 0: // Not XML
                        break; // fall through
                    case 1: // XML but not HTML
                        info.setMessage(new ErrorMessage(
                                MessageConstants.HTML_HUL_14));
                        info.setWellFormed(false);
                        return 0;
                    case 2: // probably XHTML
                        return 100;
                    default:
                        break;
                }
                info.setMessage(new ErrorMessage(
                        MessageConstants.HTML_HUL_16));
                info.setValid(false);
                // But keep going
            }

            HtmlDocDesc docDesc = null;
            switch (type) {
                case HTML_3_2:

                case HTML_4_0_FRAMESET:
                    docDesc = new Html4_0FrameDocDesc();
                    _textMD.setMarkup_basis("HTML");
                    _textMD.setMarkup_basis_version("4.0");
                    break;
                case HTML_4_0_TRANSITIONAL:
                    docDesc = new Html4_0TransDocDesc();
                    _textMD.setMarkup_basis("HTML");
                    _textMD.setMarkup_basis_version("4.0");
                    break;
                case HTML_4_0_STRICT:
                    docDesc = new Html4_0StrictDocDesc();
                    _textMD.setMarkup_basis("HTML");
                    _textMD.setMarkup_basis_version("4.0");
                    break;
                case HTML_4_01_FRAMESET:
                    docDesc = new Html4_01FrameDocDesc();
                    _textMD.setMarkup_basis("HTML");
                    _textMD.setMarkup_basis_version("4.01");
                    break;
                case HTML_4_01_TRANSITIONAL:
                    docDesc = new Html4_01TransDocDesc();
                    _textMD.setMarkup_basis("HTML");
                    _textMD.setMarkup_basis_version("4.01");
                    break;
                case HTML_4_01_STRICT:
                    docDesc = new Html4_01StrictDocDesc();
                    _textMD.setMarkup_basis("HTML");
                    _textMD.setMarkup_basis_version("4.01");
                    break;
                case XHTML_1_0_STRICT:
                case XHTML_1_0_TRANSITIONAL:
                case XHTML_1_0_FRAMESET:
                case XHTML_1_1:
                    // Force a second call to parse as XML. 100 is a
                    // magic code for the first XML call.
                    return 100;
            }
            _textMD.setMarkup_language(_doctype);
            if (docDesc == null) {
                info.setMessage(new InfoMessage(
                        MessageConstants.HTML_HUL_22));
                docDesc = new Html3_2DocDesc();
            }
            docDesc.validate(elements, info);
            metadata = docDesc.getMetadata();

            // Try to get the charset from the meta Content
            if (metadata.getCharset() != null) {
                _textMD.setCharset(metadata.getCharset());
            } else {
                _textMD.setCharset(TextMDMetadata.CHARSET_ISO8859_1);
            }
            String textMDEncoding = _textMD.getCharset();
            if (textMDEncoding.contains("UTF")) {
                _textMD.setByte_order(_bigEndian ? TextMDMetadata.BYTE_ORDER_BIG
                        : TextMDMetadata.BYTE_ORDER_LITTLE);
                _textMD.setByte_size("8");
                _textMD.setCharacter_size("variable");
            } else {
                _textMD.setByte_order(_bigEndian ? TextMDMetadata.BYTE_ORDER_BIG
                        : TextMDMetadata.BYTE_ORDER_LITTLE);
                _textMD.setByte_size("8");
                _textMD.setCharacter_size("1");
            }
        } catch (ParseException e) {
            Token t = e.currentToken;
            info.setMessage(new ErrorMessage(
                    MessageConstants.HTML_HUL_18,
                    "Line = " + t.beginLine + ", column = " + t.beginColumn));
            info.setWellFormed(false);
        } catch (TokenMgrError f) {
            info.setMessage(new ErrorMessage(
                    MessageConstants.HTML_HUL_19,
                    f.getLocalizedMessage()));
            info.setWellFormed(false);
        }

        if (info.getWellFormed() == RepInfo.FALSE) {
            return 0;
        }

        if (type != 0) {
            if (PROFILENAMES[type] != null) {
                info.setProfile(PROFILENAMES[type]);
            }
            info.setVersion(VERSIONNAMES[type]);
        }

        if (metadata != null) {
            Property property = metadata
                    .toProperty(_withTextMD ? _textMD : null);
            if (property != null) {
                info.setProperty(property);
            }
        }

        // Set the checksums in the report if they're calculated
        setChecksums(this._ckSummer, info);

        return 0;
    }

    /**
     * Check if the digital object conforms to this Module's internal signature
     * information.
     *
     * HTML is one of the most ill-defined of any open formats, so checking a
     * "signature" really means using some heuristics. The only required tag is
     * TITLE, but that could occur well into the file. So we look for any of
     * three strings -- taking into account case-independence and white space --
     * within the first sigBytes bytes, and call that a signature check.
     *
     * @param file
     *               A File object for the object being parsed
     * @param stream
     *               An InputStream, positioned at its beginning, which is
     *               generated from the object to be parsed
     * @param info
     *               A fresh RepInfo object which will be modified to reflect the
     *               results of the test
     *
     * @throws IOException
     */
    @Override
    public void checkSignatures(File file, InputStream stream, RepInfo info)
            throws IOException {
        info.setFormat(_format[0]);
        info.setMimeType(_mimeType[0]);
        info.setModule(this);
        char[][] sigtext = new char[3][];
        sigtext[0] = "<!DOCTYPE HTML".toCharArray();
        sigtext[1] = "<HTML".toCharArray();
        sigtext[2] = "<TITLE".toCharArray();
        int[] sigstate = { 0, 0, 0 };
        JhoveBase jb = getBase();
        int sigBytes = jb.getSigBytes();
        int bytesRead = 0;
        boolean eof = false;
        DataInputStream dstream = new DataInputStream(stream);
        while (!eof && bytesRead < sigBytes) {
            try {
                int ch = readUnsignedByte(dstream, this);
                char chr = Character.toUpperCase((char) ch);
                ++bytesRead;
                if (Character.isWhitespace(chr)) {
                    continue; // ignore all whitespace
                }
                for (int i = 0; i < 3; i++) {
                    int ss = sigstate[i];
                    char[] st = sigtext[i];
                    if (chr == st[ss]) {
                        ++sigstate[i];
                        if (sigstate[i] == st.length) {
                            // One of the sig texts matches!
                            info.setSigMatch(_name);
                            return;
                        }
                    } else
                        sigstate[i] = 0;
                }
            } catch (EOFException e) {
                eof = true;
            }
        }
        // If we fall through, there was no sig match
        info.setWellFormed(false);

    }

    /*
     * Check if there is a DOCTYPE at the start of the elements list. If there
     * is, return the appropriate version string. If the DOCTYPE says it isn't
     * HTML, trust it and call this document ill-formed by returning -1. If
     * there is no DOCTYPE, or an unrecognized one, return 0.
     */
    protected int checkDoctype(List elements) {
        JHElement firstElem = (JHElement) elements.get(0);
        if (firstElem instanceof JHXmlDecl && elements.size() >= 2) {
            firstElem = (JHElement) elements.get(1);
        }
        if (!(firstElem instanceof JHDoctype)) {
            return 0; // no DOCTYPE found
        }
        List dt = ((JHDoctype) firstElem).getDoctypeElements();
        if (dt.size() < 3) {
            return 0;
        }
        try {
            // Is DOCTYPE case sensitive? Assume not.
            String str = ((String) dt.get(0)).toUpperCase();
            if (!"HTML".equals(str)) {
                // It's not HTML
                return -1;
            }
            str = ((String) dt.get(1)).toUpperCase();
            if (!"PUBLIC".equals(str)) {
                return 0;
            }
            str = stripQuotes(((String) dt.get(2)).toUpperCase());
            _doctype = str;
            if (null != str)
                switch (str) {
                    case "-//W3C//DTD HTML 3.2 FINAL//EN":
                    case "-//W3C//DTD HTML 3.2//EN":
                        return HTML_3_2;
                    case "-//W3C//DTD HTML 4.0//EN":
                        return HTML_4_0_STRICT;
                    case "-//W3C//DTD HTML 4.0 TRANSITIONAL//EN":
                        return HTML_4_0_TRANSITIONAL;
                    case "-//W3C//DTD HTML 4.0 FRAMESET//EN":
                        return HTML_4_0_FRAMESET;
                    case "-//W3C//DTD HTML 4.01//EN":
                        return HTML_4_01_STRICT;
                    case "-//W3C//DTD HTML 4.01 TRANSITIONAL//EN":
                        return HTML_4_01_TRANSITIONAL;
                    case "-//W3C//DTD HTML 4.01 FRAMESET//EN":
                        return HTML_4_01_FRAMESET;
                    case "-//W3C//DTD XHTML 1.0 STRICT//EN":
                        return XHTML_1_0_STRICT;
                    case "-//W3C//DTD XHTML 1.0 TRANSITIONAL//EN":
                        return XHTML_1_0_TRANSITIONAL;
                    case "-//W3C//DTD XHTML 1.0 FRAMESET//EN":
                        return XHTML_1_0_FRAMESET;
                    case "-//W3C//DTD XHTML 1.1//EN":
                        return XHTML_1_1;
                    default:
                        break;
                }
        } catch (Exception e) {
            // Really shouldn't happen, but if it does we've got
            // a bad doctype
            return 0;
        }
        return 0;
    }

    /*
     * See if this document, even if it lacks a doctype, is most likely XHTML.
     * The test is that the document starts with an XML declaration and has
     * "html" for its first tag.
     *
     * Returns: 0 if there's no XML declaration 1 if there's an XML declaration
     * but no html tag; in this case it's probably some other kind of XML 2 if
     * there's an XML declaration and an html tag
     */
    protected int seemsToBeXHTML(List elements) {
        JHElement elem;
        try {
            elem = (JHElement) elements.get(0);
            if (!(elem instanceof JHXmlDecl)) {
                return 0;
            }
            Iterator iter = elements.iterator();
            while (iter.hasNext()) {
                elem = (JHElement) iter.next();
                if (elem instanceof JHOpenTag) {
                    JHOpenTag tag = (JHOpenTag) elem;
                    return ("html".equals(tag.getName()) ? 2 : 1);
                }
            }
        } catch (Exception e) {
            return 0; // document must be really empty
        }
        return 1;
    }

    /*
     * Remove quotes from the beginning and end of a string. If it doesn't have
     * quotes in both places, leave it alone.
     */
    protected String stripQuotes(String str) {
        int len = str.length();
        if (str.charAt(0) == '"' && str.charAt(len - 1) == '"') {
            return str.substring(1, len - 1);
        }
        return str;
    }

    /*
     * Checks if the XML module is available.
     */
    protected static boolean isXmlAvailable() {
        try {
            Class.forName("edu.harvard.hul.ois.jhove.module.XmlModule");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
