/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2003-2009 by JSTOR and the President and Fellows of Harvard College
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 **********************************************************************/

package edu.harvard.hul.ois.jhove.handler;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.harvard.hul.ois.jhove.AESAudioMetadata;
import edu.harvard.hul.ois.jhove.Agent;
import edu.harvard.hul.ois.jhove.App;
import edu.harvard.hul.ois.jhove.Checksum;
import edu.harvard.hul.ois.jhove.Document;
import edu.harvard.hul.ois.jhove.HandlerBase;
import edu.harvard.hul.ois.jhove.Identifier;
import edu.harvard.hul.ois.jhove.InternalSignature;
import edu.harvard.hul.ois.jhove.Message;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.NisoImageMetadata;
import edu.harvard.hul.ois.jhove.OutputHandler;
import edu.harvard.hul.ois.jhove.Property;
import edu.harvard.hul.ois.jhove.PropertyArity;
import edu.harvard.hul.ois.jhove.PropertyType;
import edu.harvard.hul.ois.jhove.Rational;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.Signature;
import edu.harvard.hul.ois.jhove.SignatureType;
import edu.harvard.hul.ois.jhove.TextMDMetadata;
import edu.harvard.hul.ois.jhove.Utils;
import edu.harvard.hul.ois.jhove.messages.JhoveMessages;

/**
 * OutputHandler for XML output.
 *
 * @see <a
 *      href=
 *      "http://schema.openpreservation.org/ois/xml/xsd/jhove/jhove.xsd">Schema
 *      for JHOVE XML output</a>
 */
public class XmlHandler extends edu.harvard.hul.ois.jhove.HandlerBase

{
    /******************************************************************
     * PRIVATE CLASS FIELDS.
     ******************************************************************/

    /** Thread safe formatter for doubles */
    private static final ThreadLocal<NumberFormat> formatters = new ThreadLocal<NumberFormat>() {
        @Override
        protected NumberFormat initialValue() {
            NumberFormat _format = NumberFormat.getInstance(Locale.ROOT);
            _format.setGroupingUsed(false);
            _format.setMinimumFractionDigits(0);

            return _format;
        }
    };

    /** Handler name. */
    private static final String NAME = "XML";

    /** Handler release identifier. */
    private static final String RELEASE = "1.13";

    /** Handler release date. */
    private static final int[] DATE = { 2024, 11, 19 };

    /** Handler informative note. */
    private static final String NOTE = "This output handler is defined by the XML Schema "
            + "https://schema.openpreservation.org/ois/xml/xsd/jhove/jhove.xsd";

    /** Handler rights statement. */
    private static final String RIGHTS = "Derived from software Copyright 2004-2011 "
            + "by the President and Fellows of Harvard College. "
            + "Version 1.8 release by Open Preservation Foundation. "
            + "Released under the GNU Lesser General Public License.";

    /** Localized line separator character. */
    private final static String EOL = System.getProperty("line.separator");

    /** Schema version. */
    private static final String SCHEMA_VERSION = "1.9";

    /******************************************************************
     * PRIVATE INSTANCE FIELDS.
     ******************************************************************/

    /* Sample rate. */
    private double _sampleRate;
    /** Reporting module */
    private String reportingModule = "";

    /******************************************************************
     * CLASS CONSTRUCTOR.
     ******************************************************************/

    /**
     * Creates an XmlHandler.
     */
    public XmlHandler() {
        super(NAME, RELEASE, DATE, NOTE, RIGHTS);
        _vendor = Agent.harvardInstance();
    }

    /** Constructor for use by subclasses. */
    public XmlHandler(String name, String release, int[] date, String note,
            String rights) {
        super(name, release, date, note, rights);
        _vendor = Agent.harvardInstance();
    }

    /******************************************************************
     * PUBLIC INSTANCE METHODS.
     ******************************************************************/

    /**
     * Outputs minimal information about the application
     */
    @Override
    public void show() {
        _level--;
    }

    /**
     * Outputs detailed information about the application, including
     * configuration, available modules and handlers, etc.
     */
    @Override
    public void show(App app) {
        String margin = getIndent(++_level);
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";

        _writer.println(margin + elementStart("app"));
        String[][] attrs = { { "date", date.format(_je.getDate()) } };
        _writer.println(margn2 + element("api", attrs, app.getRelease()));
        String configFile = _je.getConfigFile();
        if (configFile != null) {
            _writer.println(margn2 + element("configuration", configFile));
        }
        String s = _je.getSaxClass();
        if (s != null) {
            _writer.println(margn2 + element("saxParser", s));
        }
        s = _je.getJhoveHome();
        if (s != null) {
            _writer.println(margn2 + element("jhoveHome", s));
        }
        s = _je.getEncoding();
        if (s != null) {
            _writer.println(margn2 + element("encoding", s));
        }
        s = _je.getTempDirectory();
        if (s != null) {
            _writer.println(margn2 + element("tempDirectory", s));
        }
        _writer.println(margn2
                + element("bufferSize", Integer.toString(_je.getBufferSize())));
        _writer.println(margn2 + elementStart("modules"));
        Iterator<String> iter = _je.getModuleMap().keySet().iterator();
        while (iter.hasNext()) {
            Module module = _je.getModule(iter.next());
            String[][] attr2 = { { "release", module.getRelease() } };
            _writer.println(margn3 + element("module", attr2, module.getName()));
        }
        _writer.println(margn2 + elementEnd("modules"));
        _writer.println(margn2 + elementStart("outputHandlers"));
        iter = _je.getHandlerMap().keySet().iterator();
        while (iter.hasNext()) {
            OutputHandler handler = _je.getHandler(iter.next());
            String[][] attr2 = { { "release", handler.getRelease() } };
            _writer.println(margn3
                    + element("outputHandler", attr2, handler.getName()));
        }
        _writer.println(margn2 + elementEnd("outputHandlers"));
        _writer.println(margn2 + element("usage", app.getUsage()));
        _writer.println(margn2 + element("rights", app.getRights()));
        _writer.println(margin + elementEnd("app"));
        _level--;
    }

    /**
     * Outputs information about the OutputHandler specified in the parameter
     */
    @Override
    public void show(OutputHandler handler) {
        String margin = getIndent(++_level);
        String margn2 = margin + " ";
        _writer.println(margin + elementStart("handler"));
        _writer.println(margn2 + element("name", handler.getName()));
        _writer.println(margn2 + element("release", handler.getRelease()));
        _writer.println(margn2
                + element("date", date.format(handler.getDate())));
        List<Document> list = handler.getSpecification();
        int n = list.size();
        if (n > 0) {
            _writer.println(margn2 + elementStart("specifications"));
            ++_level;
            for (int i = 0; i < n; i++) {
                showDocument(list.get(i));
            }
            --_level;
            _writer.println(margn2 + elementEnd("specifications"));
        }
        Agent vendor = handler.getVendor();
        if (vendor != null) {
            showAgent(vendor, "Vendor");
        }
        String s;
        if ((s = handler.getNote()) != null) {
            _writer.println(margn2 + element("note", s));
        }
        if ((s = handler.getRights()) != null) {
            _writer.println(margn2 + element("rights", s));
        }
        _writer.println(margin + elementEnd("handler"));
        _level--;
    }

    /**
     * Outputs information about a Module
     */
    @Override
    public void show(Module module) {
        String margin = getIndent(++_level);
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";

        _writer.println(margin + elementStart("module"));
        _writer.println(margn2 + element("name", module.getName()));
        _writer.println(margn2 + element("release", module.getRelease()));
        _writer.println(margn2
                + element("date", HandlerBase.date.format(module.getDate())));
        String[] ss = module.getFormat();
        if (ss.length > 0) {
            _writer.println(margn2 + elementStart("formats"));
            for (int i = 0; i < ss.length; i++) {
                _writer.println(margn3 + element("format", ss[i]));
            }
            _writer.println(margn2 + elementEnd("formats"));
        }
        String s = module.getCoverage();
        if (s != null) {
            _writer.println(margn2 + element("coverage", s));
        }
        ss = module.getMimeType();
        if (ss.length > 0) {
            _writer.println(margn2 + elementStart("mimeTypes"));
            for (int i = 0; i < ss.length; i++) {
                _writer.println(margn3 + element("mimeType", ss[i]));
            }
            ;
            _writer.println(margn2 + elementEnd("mimeTypes"));
        }
        List<Signature> sigList = module.getSignature();
        if (!sigList.isEmpty()) {
            _writer.println(margn2 + elementStart("signatures"));
            ++_level;
            for (Signature sig : sigList) {
                showSignature(sig);
            }
            _level--;
            _writer.println(margn2 + elementEnd("signatures"));
        }
        List<Document> docList = module.getSpecification();
        if (!docList.isEmpty()) {
            _writer.println(margn2 + elementStart("specifications"));
            ++_level;
            for (Document doc : docList) {
                showDocument(doc);
            }
            --_level;
            _writer.println(margn2 + elementEnd("specifications"));
        }
        List<String> featList = module.getFeatures();
        if (featList != null && !featList.isEmpty()) {
            _writer.println(margn2 + elementStart("features"));
            for (String feat : featList) {
                _writer.println(margn3 + element("feature", feat));
            }
            _writer.println(margn2 + elementEnd("features"));
        }
        _writer.println(margn2 + elementStart("methodology"));
        if ((s = module.getWellFormedNote()) != null) {
            _writer.println(margn3 + element("wellFormed", s));
        }
        if ((s = module.getValidityNote()) != null) {
            _writer.println(margn3 + element("validity", s));
        }
        if ((s = module.getRepInfoNote()) != null) {
            _writer.println(margn3 + element("repInfo", s));
        }
        _writer.println(margn2 + elementEnd("methodology"));
        Agent vendor = module.getVendor();
        if (vendor != null) {
            showAgent(vendor, "Vendor");
        }
        if ((s = module.getNote()) != null) {
            _writer.println(margn2 + element("note", s));
        }
        if ((s = module.getRights()) != null) {
            _writer.println(margn2 + element("rights", s));
        }
        _writer.println(margin + elementEnd("module"));
        _level--;
    }

    /**
     * Outputs the information contained in a RepInfo object
     */
    @Override
    public void show(RepInfo info) {
        String margin = getIndent(++_level);
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";

        Module module = info.getModule();
        _logger.info("Reporting RepInfo");
        if (_je.getSignatureFlag()) {
            _logger.info("Checking signatures only");
        }
        String[][] attrs = { { "uri", cleanURIString(info.getUri()) } };
        _writer.println(margin + elementStart("repInfo", attrs));
        if (module != null) {
            String[][] attr2 = { { "release", module.getRelease() },
                    { "date", date.format(module.getDate()) } };
            this.reportingModule = module.getName();
            _writer.println(margn2
                    + element("reportingModule", attr2, this.reportingModule));
        }
        Date date = info.getCreated();
        if (date != null) {
            _writer.println(margn2 + element("created", toDateTime(date)));
        }
        date = info.getLastModified();
        if (date != null) {
            _writer.println(margn2 + element("lastModified", toDateTime(date)));
        }
        long size = info.getSize();
        if (size > -1) {
            _writer.println(margn2 + element("size", Long.toString(size)));
        }
        String s = info.getFormat();
        if (s != null) {
            _writer.println(margn2 + element("format", s));
        }
        s = info.getVersion();
        if (s != null) {
            _writer.println(margn2 + element("version", s));
        }
        String wfStr;
        if (!_je.getSignatureFlag()) {
            switch (info.getWellFormed()) {
                case RepInfo.TRUE:
                    wfStr = "Well-Formed";
                    break;

                case RepInfo.FALSE:
                    wfStr = "Not well-formed";
                    break;

                default:
                    wfStr = "Unknown";
                    break;
            }
            // If it's well-formed, append validity information
            if (info.getWellFormed() == RepInfo.TRUE) {
                switch (info.getValid()) {
                    case RepInfo.TRUE:
                        wfStr += " and valid";
                        break;

                    case RepInfo.FALSE:
                        wfStr += ", but not valid";
                        break;

                    // case UNDETERMINED: add nothing
                }
            }
            _logger.info("Validity/WF status: " + wfStr);
            _writer.println(margn2 + element("status", wfStr));
        } else {
            // If we aren't checking signatures, we still need to say something.
            switch (info.getWellFormed()) {
                case RepInfo.TRUE:
                    wfStr = "Well-Formed";
                    break;

                default:
                    wfStr = "Not well-formed";
                    break;
            }
            _writer.println(margn2 + element("status", wfStr));
        }

        List<String> sigMatches = info.getSigMatch();
        if (!sigMatches.isEmpty()) {
            _writer.println(margn2 + elementStart("sigMatch"));
            _level++;
            for (String sigMatch : sigMatches) {
                _writer.println(margn2
                        + element("module", sigMatch));
            }
            _level--;
            _writer.println(margn2 + elementEnd("sigMatch"));
        }

        List<Message> messages = info.getMessage();
        if (!messages.isEmpty()) {
            _writer.println(margn2 + elementStart("messages"));
            _level++;
            for (Message message : messages) {
                showMessage(message);
            }
            _level--;
            _writer.println(margn2 + elementEnd("messages"));
        }
        s = info.getMimeType();
        if (s != null) {
            _writer.println(margn2 + element("mimeType", s));
        }

        List<String> profiles = info.getProfile();
        if (!profiles.isEmpty()) {
            _writer.println(margn2 + elementStart("profiles"));
            for (String profile : profiles) {
                _writer.println(margn3
                        + element("profile", profile));
            }
            _writer.println(margn2 + elementEnd("profiles"));
        }

        Map<String, Property> map = info.getProperty();
        if (map != null && !map.isEmpty()) {
            _writer.println(margn2 + elementStart("properties"));
            for (Entry<String, Property> entry : map.entrySet()) {
                showProperty(entry.getValue());
            }
            _writer.println(margn2 + elementEnd("properties"));
        }

        List<Checksum> checksums = info.getChecksum();
        if (!checksums.isEmpty()) {
            _writer.println(margn2 + elementStart("checksums"));
            _level++;
            for (Checksum checksum : checksums) {
                showChecksum(checksum);
            }
            _level--;
            _writer.println(margn2 + elementEnd("checksums"));
        }

        s = info.getNote();
        if (s != null) {
            _writer.println(margn2 + element("note", s));
        }

        _writer.println(margin + elementEnd("repInfo"));
        _level--;
    }

    /******************************************************************
     * PRIVATE INSTANCE METHODS.
     ******************************************************************/

    protected void showAgent(Agent agent, String label) {
        String margin = getIndent(++_level);
        String margn2 = margin + " ";

        String[][] attrs = { { "type", label } };
        _writer.println(margin + elementStart("agent", attrs));
        _writer.println(margn2 + element("name", agent.getName()));
        _writer.println(margn2 + element("type", agent.getType().toString()));
        String s = agent.getAddress();
        if (s != null) {
            _writer.println(margn2 + element("address", s));
        }
        if ((s = agent.getTelephone()) != null) {
            _writer.println(margn2 + element("telephone", s));
        }
        if ((s = agent.getFax()) != null) {
            _writer.println(margn2 + element("fax", s));
        }
        if ((s = agent.getEmail()) != null) {
            _writer.println(margn2 + element("email", s));
        }
        if ((s = agent.getWeb()) != null) {
            _writer.println(margn2 + element("web", s));
        }
        _writer.println(margin + elementEnd("agent"));
        _level--;
    }

    protected void showChecksum(Checksum checksum) {
        String margin = getIndent(++_level);

        String[][] attrs = { { "type", checksum.getType().toString() } };
        _writer.println(margin
                + element("checksum", attrs, checksum.getValue()));

        _level--;
    }

    protected void showDocument(Document document) {
        String margin = getIndent(++_level);
        String margn2 = margin + " ";

        // String [][] attrs = { {"type", label} };
        _writer.println(margin + elementStart("specification"));
        _writer.println(margn2 + element("title", document.getTitle()));
        _writer.println(margn2 + element("type", document.getType().toString()));
        List<Agent> list1 = document.getAuthor();
        int n = list1.size();
        if (n > 0) {
            _writer.println(margn2 + elementStart("authors"));
            ++_level;
            for (int i = 0; i < n; i++) {
                showAgent(list1.get(i), "Author");
            }
            _level--;
            _writer.println(margn2 + elementEnd("authors"));
        }
        List<Agent> list2 = document.getPublisher();
        n = list2.size();
        if (n > 0) {
            ++_level;
            _writer.println(margn2 + elementStart("publishers"));
            for (int i = 0; i < n; i++) {
                showAgent(list2.get(i), "Publisher");
            }
            _writer.println(margn2 + elementEnd("publishers"));
            _level--;
        }
        String s = document.getEdition();
        if (s != null) {
            _writer.println(margn2 + element("edition", s));
        }
        if ((s = document.getDate()) != null) {
            _writer.println(margn2 + element("date", s));
        }
        if ((s = document.getEnumeration()) != null) {
            _writer.println(margn2 + element("enumeration", s));
        }
        if ((s = document.getPages()) != null) {
            _writer.println(margn2 + element("pages", s));
        }
        List<Identifier> list3 = document.getIdentifier();
        n = list3.size();
        if (n > 0) {
            _writer.println(margn2 + elementStart("identifiers"));
            ++_level;
            for (int i = 0; i < n; i++) {
                showIdentifier(list3.get(i));
            }
            _level--;
            _writer.println(margn2 + elementEnd("identifiers"));
        }
        if ((s = document.getNote()) != null) {
            _writer.println(margn2 + element("note", s));
        }
        _writer.println(margin + elementEnd("specification"));
        _level--;
    }

    /**
     * Do the final output. This should be in a suitable format for including
     * multiple files between the header and the footer, and the XML of the
     * header and footer must balance out.
     */
    @Override
    public void showFooter() {
        String margin = getIndent(_level--);
        _writer.println(margin + elementEnd("jhove"));

        _writer.flush();
    }

    /**
     * Do the initial output. This should be in a suitable format for including
     * multiple files between the header and the footer, and the XML of the
     * header and footer must balance out.
     */
    @Override
    public void showHeader() {
        String margin = getIndent(++_level);
        String margn2 = margin + " ";

        if (_encoding != null) {
            _writer.println(margin + xmlDecl(_encoding));
        } else {
            _writer.println(margin + xmlDecl());
        }

        String[][] attrs = {
                { "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" },
                { "xmlns",
                        "http://schema.openpreservation.org/ois/xml/ns/jhove" },
                {
                        "xsi:schemaLocation",
                        "http://schema.openpreservation.org/ois/xml/ns/jhove "
                                + "https://schema.openpreservation.org/ois/xml/xsd/jhove/"
                                + SCHEMA_VERSION + "/jhove.xsd" },
                { "name", _app.getName() }, { "release", _app.getRelease() },
                { "date", HandlerBase.date.format(_app.getDate()) } };
        _writer.println(margin + elementStart("jhove", attrs));
        _writer.println(margn2 + element("date", toDateTime(new Date())));
    }

    protected void showIdentifier(Identifier identifier) {
        String margin = getIndent(++_level);
        String margn2 = margin + " ";

        _writer.println(margin + elementStart("identifier"));
        _writer.println(margn2 + element("value", identifier.getValue()));
        _writer.println(margn2
                + element("type", identifier.getType().toString()));
        String note = identifier.getNote();
        if (note != null) {
            _writer.println(margn2 + element("note", note));
        }
        _writer.println(margin + elementEnd("identifier"));
        _level--;
    }

    protected void showMessage(Message message) {
        String margin = getIndent(++_level);
        String[][] attrs = new String[5][];
        boolean hasAttr = false;
        attrs[0] = new String[] { "subMessage", null };
        attrs[1] = new String[] { "offset", null };
        attrs[2] = new String[] { "severity", null };
        attrs[3] = new String[] { "id", null };
        attrs[4] = new String[] { "infoLink", null };

        String submsg = message.getSubMessage();
        if (submsg != null) {
            attrs[0][1] = submsg;
            hasAttr = true;
        }
        long offset = message.getOffset();
        if (offset > -1) {
            attrs[1][1] = Long.toString(offset);
            hasAttr = true;
        }
        if (!message.getPrefix().isEmpty()) {
            attrs[2][1] = message.getPrefix().toLowerCase();
            hasAttr = true;
        }
        String id = message.getJhoveMessage().getId();
        if (!(id == null || id.isEmpty() || id.equals(JhoveMessages.NO_ID))) {
            attrs[3][1] = message.getId();
            attrs[4][1] = Handlers.makeInfoLink(this.reportingModule, id);
            hasAttr = true;
        }

        if (hasAttr) {
            _writer.println(margin
                    + element("message", attrs, message.getMessage()));
        } else {
            _writer.println(margin + element("message", message.getMessage()));
        }
        _level--;
    }

    protected void showSignature(Signature signature) {
        String margin = getIndent(++_level);
        String margin1 = margin + " ";
        String sigValue;

        _writer.println(margin + elementStart("signature"));
        if (signature.isStringValue()) {
            sigValue = signature.getValueString();
        } else {
            sigValue = signature.getValueHexString();
        }
        _writer.println(margin1
                + element("type", signature.getType().toString()));
        _writer.println(margin1 + element("value", sigValue));
        if (signature.getType().equals(SignatureType.MAGIC)) {
            if (((InternalSignature) signature).hasFixedOffset()) {
                _writer.println(margin1
                        + element(
                                "offset",
                                "0x"
                                        + Integer
                                                .toHexString(((InternalSignature) signature)
                                                        .getOffset())));
            }
        }
        String note = signature.getNote();
        if (note != null) {
            _writer.println(margin1 + element("note", note));

        }
        String use = signature.getUse().toString();
        if (use != null) {
            _writer.println(margin1 + element("use", use));
        }
        _writer.println(margin + elementEnd("signature"));
        _level--;
    }

    /* Do special conversions on values as needed. */
    protected String valueToString(Object obj) {
        if (obj instanceof Date) {
            return toDateTime((Date) obj);
        }
        return obj.toString();
    }

    protected void showProperty(Property property) {
        PropertyArity arity = property.getArity();
        PropertyType type = property.getType();

        // If the property would generate an empty element, don't output it,
        // as this could result in a schema violation.
        if (Utils.isPropertyEmpty(property, arity))
            return;

        String margin = getIndent(++_level);
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";

        boolean valueIsProperty = PropertyType.PROPERTY.equals(type);
        boolean valueIsNiso = PropertyType.NISOIMAGEMETADATA.equals(type);
        boolean valueIsAes = PropertyType.AESAUDIOMETADATA.equals(type);
        boolean valueIsTextMD = PropertyType.TEXTMDMETADATA.equals(type);

        String[][] propAttrs = new String[2][];
        propAttrs[0] = new String[] { "arity", arity.toString() };
        propAttrs[1] = new String[] { "type", type.toString() };
        _writer.println(margn2 + elementStart("property"));
        _writer.println(margn3 + element("name", property.getName()));
        _writer.println(margn3 + elementStart("values", propAttrs));
        if (arity.equals(PropertyArity.SCALAR)) {
            /* Just a single value */
            if (valueIsProperty) {
                showProperty((Property) property.getValue());
            } else if (valueIsNiso) {
                _writer.println(margn4 + elementStart("value"));
                showNisoImageMetadata((NisoImageMetadata) property.getValue());
                _writer.println(margn4 + elementEnd("value"));
            } else if (valueIsAes) {
                _writer.println(margn4 + elementStart("value"));
                showAESAudioMetadata((AESAudioMetadata) property.getValue());
                _writer.println(margn4 + elementEnd("value"));
            } else if (valueIsTextMD) {
                _writer.println(margn4 + elementStart("value"));
                showTextMDMetadata((TextMDMetadata) property.getValue());
                _writer.println(margn4 + elementEnd("value"));
            } else {
                _writer.println(margn4
                        + element("value", property.getValue().toString()));
            }
        } else if (arity.equals(PropertyArity.LIST)) {
            List propList = (List) property.getValue();
            ListIterator iter = propList.listIterator();
            while (iter.hasNext()) {
                Object val = iter.next();
                if (valueIsProperty) {
                    showProperty((Property) val);
                } else if (valueIsNiso) {
                    _writer.println(margn4 + elementStart("value"));
                    showNisoImageMetadata((NisoImageMetadata) property
                            .getValue());
                    _writer.println(margn4 + elementEnd("value"));
                } else if (valueIsAes) {
                    _writer.println(margn4 + elementStart("value"));
                    showAESAudioMetadata((AESAudioMetadata) property.getValue());
                    _writer.println(margn4 + elementEnd("value"));
                } else if (valueIsTextMD) {
                    _writer.println(margn4 + elementStart("value"));
                    showTextMDMetadata((TextMDMetadata) property.getValue());
                    _writer.println(margn4 + elementEnd("value"));
                } else {
                    _writer.println(margn4
                            + element("value", valueToString(val)));
                }
            }
        } else if (arity.equals(PropertyArity.MAP)) {
            /*
             * For a map, the key is the "key" attribute of its corresponding
             * value
             */
            Map propMap = (Map) property.getValue();
            Iterator keyIter = propMap.keySet().iterator();
            while (keyIter.hasNext()) {
                Object key = keyIter.next();
                String keystr = key.toString();
                Object val = propMap.get(key);
                String[][] attrs = new String[1][];
                String[] keyAttr = new String[2];
                keyAttr[0] = "key";
                keyAttr[1] = keystr;
                attrs[0] = keyAttr;
                if (valueIsProperty) {
                    Property pval = (Property) val;
                    // If the key equals the property name, suppress the key
                    if (pval.getName().equals(keystr)) {
                        _writer.print(margn4 + elementStart("value"));
                    } else {
                        _writer.print(margn4 + elementStart("value", attrs));
                    }
                    showProperty(pval);
                    _writer.println(margn4 + elementEnd("value"));
                } else if (valueIsNiso) {
                    _writer.println(margn4 + elementStart("value"));
                    showNisoImageMetadata((NisoImageMetadata) val);
                    _writer.println(margn4 + elementEnd("value"));
                } else if (valueIsAes) {
                    _writer.println(margn4 + elementStart("value"));
                    showAESAudioMetadata((AESAudioMetadata) val);
                    _writer.println(margn4 + elementEnd("value"));
                } else if (valueIsTextMD) {
                    _writer.println(margn4 + elementStart("value"));
                    showTextMDMetadata((TextMDMetadata) val);
                    _writer.println(margn4 + elementEnd("value"));
                } else {
                    _writer.println(margn4
                            + element("value", attrs, valueToString(val)));
                }
            }
        } else if (arity.equals(PropertyArity.SET)) {
            Set propSet = (Set) property.getValue();
            Iterator iter = propSet.iterator();
            while (iter.hasNext()) {
                Object val = iter.next();
                if (valueIsProperty) {
                    showProperty((Property) val);
                } else {
                    _writer.println(margn4
                            + element("value", valueToString(val)));
                }
            }
        } else if (arity.equals(PropertyArity.ARRAY)) {
            showArrayProperty(property, margn4);
        }
        _writer.println(margn3 + elementEnd("values"));
        _writer.println(margn2 + elementEnd("property"));
        --_level;
    }

    /*
     * The array property has so many special cases of its own that we break it
     * out of showProperty
     */
    protected void showArrayProperty(Property property, String margin) {
        boolean[] boolArray = null;
        byte[] byteArray = null;
        char[] charArray = null;
        java.util.Date[] dateArray = null;
        double[] doubleArray = null;
        float[] floatArray = null;
        int[] intArray = null;
        long[] longArray = null;
        Object[] objArray = null;
        Property[] propArray = null;
        short[] shortArray = null;
        String[] stringArray = null;
        Rational[] rationalArray = null;
        NisoImageMetadata[] nisoArray = null;
        AESAudioMetadata[] aesArray = null;
        TextMDMetadata[] textMDArray = null;
        int n = 0;

        PropertyType propType = property.getType();
        if (PropertyType.BOOLEAN.equals(propType)) {
            boolArray = (boolean[]) property.getValue();
            n = boolArray.length;
        } else if (PropertyType.BYTE.equals(propType)) {
            byteArray = (byte[]) property.getValue();
            n = byteArray.length;
        } else if (PropertyType.CHARACTER.equals(propType)) {
            charArray = (char[]) property.getValue();
            n = charArray.length;
        } else if (PropertyType.DATE.equals(propType)) {
            dateArray = (java.util.Date[]) property.getValue();
            n = dateArray.length;
        } else if (PropertyType.DOUBLE.equals(propType)) {
            doubleArray = (double[]) property.getValue();
            n = doubleArray.length;
        } else if (PropertyType.FLOAT.equals(propType)) {
            floatArray = (float[]) property.getValue();
            n = floatArray.length;
        } else if (PropertyType.INTEGER.equals(propType)) {
            intArray = (int[]) property.getValue();
            n = intArray.length;
        } else if (PropertyType.LONG.equals(propType)) {
            longArray = (long[]) property.getValue();
            n = longArray.length;
        } else if (PropertyType.OBJECT.equals(propType)) {
            objArray = (Object[]) property.getValue();
            n = objArray.length;
        } else if (PropertyType.SHORT.equals(propType)) {
            shortArray = (short[]) property.getValue();
            n = shortArray.length;
        } else if (PropertyType.STRING.equals(propType)) {
            stringArray = (String[]) property.getValue();
            n = stringArray.length;
        } else if (PropertyType.RATIONAL.equals(propType)) {
            rationalArray = (Rational[]) property.getValue();
            n = rationalArray.length;
        } else if (PropertyType.PROPERTY.equals(propType)) {
            propArray = (Property[]) property.getValue();
            n = propArray.length;
        } else if (PropertyType.NISOIMAGEMETADATA.equals(propType)) {
            nisoArray = (NisoImageMetadata[]) property.getValue();
            n = nisoArray.length;
        } else if (PropertyType.AESAUDIOMETADATA.equals(propType)) {
            aesArray = (AESAudioMetadata[]) property.getValue();
            n = aesArray.length;
        } else if (PropertyType.TEXTMDMETADATA.equals(propType)) {
            textMDArray = (TextMDMetadata[]) property.getValue();
            n = textMDArray.length;
        }

        for (int i = 0; i < n; i++) {
            String elem;
            if (PropertyType.BOOLEAN.equals(propType)) {
                elem = String.valueOf(boolArray[i]);
            } else if (PropertyType.BYTE.equals(propType)) {
                elem = String.valueOf(byteArray[i]);
            } else if (PropertyType.CHARACTER.equals(propType)) {
                elem = String.valueOf(charArray[i]);
            } else if (PropertyType.DATE.equals(propType)) {
                elem = dateArray[i].toString();
            } else if (PropertyType.DOUBLE.equals(propType)) {
                elem = String.valueOf(doubleArray[i]);
            } else if (PropertyType.FLOAT.equals(propType)) {
                elem = String.valueOf(floatArray[i]);
            } else if (PropertyType.INTEGER.equals(propType)) {
                elem = String.valueOf(intArray[i]);
            } else if (PropertyType.LONG.equals(propType)) {
                elem = String.valueOf(longArray[i]);
            } else if (PropertyType.OBJECT.equals(propType)) {
                elem = valueToString(objArray[i]);
            } else if (PropertyType.SHORT.equals(propType)) {
                elem = String.valueOf(shortArray[i]);
            } else if (PropertyType.STRING.equals(propType)) {
                elem = stringArray[i];
            } else if (PropertyType.RATIONAL.equals(propType)) {
                elem = rationalArray[i].toString();
            } else if (PropertyType.PROPERTY.equals(propType)) {
                showProperty(propArray[i]);
                continue;
            } else if (PropertyType.NISOIMAGEMETADATA.equals(propType)) {
                showNisoImageMetadata(nisoArray[i]);
                continue;
            } else if (PropertyType.AESAUDIOMETADATA.equals(propType)) {
                showAESAudioMetadata(aesArray[i]);
                continue;
            } else if (PropertyType.TEXTMDMETADATA.equals(propType)) {
                showTextMDMetadata(textMDArray[i]);
                continue;
            } else
                elem = "<error>";
            _writer.println(margin + element("value", elem));
        }
    }

    /**
     * Display the text metadata formatted according to the textMD schema (see
     * http://www.loc.gov/standards/textMD).
     *
     * @param textMD
     *               textMD text metadata
     */
    protected void showTextMDMetadata(TextMDMetadata textMD) {
        String margin = getIndent(++_level);
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";

        String[][] attrs = {
                { "xmlns:textmd", TextMDMetadata.NAMESPACE },
                { "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" },
                {
                        "xsi:schemaLocation",
                        TextMDMetadata.NAMESPACE + " "
                                + TextMDMetadata.DEFAULT_LOCATION }, };
        _writer.println(margin + elementStart("textmd:textMD", attrs));
        _writer.println(margn2 + elementStart("textmd:character_info"));
        _writer.println(margn3 + element("textmd:charset", textMD.getCharset()));
        _writer.println(margn3
                + element("textmd:byte_order", textMD.getByte_orderString()));
        _writer.println(margn3
                + element("textmd:byte_size", textMD.getByte_size()));
        if ("variable".equals(textMD.getCharacter_size())) {
            String[][] attrs1 = { { "encoding", textMD.getCharset() } };
            _writer.println(margn3
                    + element("textmd:character_size", attrs1, "variable"));
        } else if (textMD.getCharacter_size() != null
                && textMD.getCharacter_size().length() != 0) {
            _writer.println(margn3
                    + element("textmd:character_size",
                            textMD.getCharacter_size()));
        }
        _writer.println(margn3
                + element("textmd:linebreak", textMD.getLinebreakString()));
        _writer.println(margn2 + elementEnd("textmd:character_info"));
        if (textMD.getLanguage() != null && textMD.getLanguage().length() != 0) {
            _writer.println(margn2
                    + element("textmd:language", textMD.getLanguage()));
        }
        if (textMD.getMarkup_basis() != null
                && textMD.getMarkup_basis().length() != 0) {
            if (textMD.getMarkup_basis_version() != null) {
                String[][] attrs1 = { { "version",
                        textMD.getMarkup_basis_version() } };
                _writer.println(margn2
                        + element("textmd:markup_basis", attrs1,
                                textMD.getMarkup_basis()));
            } else {
                _writer.println(margn2
                        + element("textmd:markup_basis",
                                textMD.getMarkup_basis()));
            }
        }
        if (textMD.getMarkup_language() != null
                && textMD.getMarkup_language().length() != 0) {
            if (textMD.getMarkup_language_version() != null) {
                String[][] attrs1 = { { "version",
                        textMD.getMarkup_language_version() } };
                _writer.println(margn2
                        + element("textmd:markup_language", attrs1,
                                textMD.getMarkup_language()));
            } else {
                _writer.println(margn2
                        + element("textmd:markup_language",
                                textMD.getMarkup_language()));
            }
        }
        _writer.println(margin + elementEnd("textmd:textMD"));
        _level--;

    }

    /**
     * Display the NISO image metadata formatted according to the MIX schema.
     * The schema which is used may be 0.2 or 1.0 or 2.0, depending on the
     * module parameters.
     *
     * @param niso
     *             NISO image metadata
     */
    protected void showNisoImageMetadata(NisoImageMetadata niso) {
        if ("0.2".equals(_je.getMixVersion())) {
            showNisoImageMetadata02(niso);
        } else if ("1.0".equals(_je.getMixVersion())) {
            showNisoImageMetadata10(niso);
        } else {
            showNisoImageMetadata20(niso);
        }
    }

    /**
     * Display the NISO image metadata formatted according to the MIX 0.2
     * schema.
     */
    protected void showNisoImageMetadata02(NisoImageMetadata niso) {
        String margin = getIndent(++_level);

        String[][] attrs = {
                { "xmlns:mix", "http://www.loc.gov/mix/" },
                { "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" },
                { "xsi:schemaLocation",
                        "http://www.loc.gov/mix/ http://www.loc.gov/mix/mix02.xsd" } };
        _writer.println(margin + elementStart("mix:mix", attrs));

        showNisoBasicImageParameters02(niso, margin);
        showNisoImageCreation02(niso, margin);
        showNisoImagingPerformanceAssessment02(niso, margin);
        showNisoChangeHistory02(niso, margin);

        _writer.println(margin + elementEnd("mix:mix"));

        _level--;
    }

    /*
     * The NISO Metadata output is split into multiple functions so that they're
     * merely outrageously big rather than disgustingly big
     */
    /* Top level element 1 of 4: BasicImageParameters */
    protected void showNisoBasicImageParameters02(NisoImageMetadata niso,
            String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";
        String margn6 = margn5 + " ";

        _writer.println(margn2 + elementStart("mix:BasicImageParameters"));

        // Here things get rather deeply nested, and we want to be able
        // to back out if it turns out that we really have no content to
        // deliver. So rather than writing directly to the writer, we
        // write to a StringBuffer that may or may not get used.
        StringBuffer fBuf = new StringBuffer(margn3
                + elementStart("mix:Format") + EOL);
        boolean useFBuf = false; // set to true if some data show up
        String s = niso.getMimeType();
        if (s != null) {
            fBuf.append(margn4 + element("mix:MIMEType", s) + EOL);
            useFBuf = true;
        }
        if ((s = niso.getByteOrder()) != null) {
            fBuf.append(margn4 + element("mix:ByteOrder", s) + EOL);
            useFBuf = true;
        }
        int comp = niso.getCompressionScheme();
        int level = niso.getCompressionLevel();
        if (comp != NisoImageMetadata.NULL || level != NisoImageMetadata.NULL) {
            fBuf.append(margn4 + elementStart("mix:Compression") + EOL);
            if (comp != NisoImageMetadata.NULL) {
                fBuf.append(margn5
                        + element("mix:CompressionScheme",
                                Integer.toString(comp))
                        + EOL);
            }
            if (level != NisoImageMetadata.NULL) {
                fBuf.append(margn5
                        + element("mix:CompressionLevel",
                                Integer.toString(level))
                        + EOL);
            }
            fBuf.append(margn4 + elementEnd("mix:Compression") + EOL);
            useFBuf = true;
        }
        // Nest photometric interpretation tentative buffer in fBuf
        StringBuffer piBuf = new StringBuffer(margn4
                + elementStart("mix:PhotometricInterpretation") + EOL);
        boolean usePIBuf = false;
        int n = niso.getColorSpace();
        if (n != NisoImageMetadata.NULL) {
            piBuf.append(margn5
                    + element("mix:ColorSpace", Integer.toString(n)) + EOL);
            usePIBuf = true;
        }
        String s2 = niso.getProfileURL();
        if ((s = niso.getProfileName()) != null || s2 != null) {
            piBuf.append(margn5 + elementStart("mix:ICCProfile") + EOL);
            if (s != null) {
                piBuf.append(margn6 + element("mix:ProfileName", s) + EOL);
            }
            if (s2 != null) {
                piBuf.append(margn6 + element("mix:ProfileURL", s2) + EOL);
            }
            piBuf.append(margn5 + elementEnd("mix:ICCProfile") + EOL);
            usePIBuf = true;
        }
        int[] iarray = niso.getYCbCrSubSampling();
        if (iarray != null) {
            piBuf.append(margn5
                    + element("mix:YCbCrSubSampling", integerArray(iarray))
                    + EOL);
            usePIBuf = true;
        }
        if ((n = niso.getYCbCrPositioning()) != NisoImageMetadata.NULL) {
            piBuf.append(margn5
                    + element("mix:YCbCrPositioning", Integer.toString(n))
                    + EOL);
            usePIBuf = true;
        }
        Rational[] rarray = niso.getYCbCrCoefficients();
        if (rarray != null) {
            piBuf.append(margn5
                    + element("mix:YCbCrCoefficients", rationalArray(rarray))
                    + EOL);
            usePIBuf = true;
        }
        if ((rarray = niso.getReferenceBlackWhite()) != null) {
            piBuf.append(margn5
                    + element("mix:ReferenceBlackWhite", rationalArray(rarray))
                    + EOL);
            usePIBuf = true;
        }
        piBuf.append(margn4 + elementEnd("mix:PhotometricInterpretation") + EOL);
        if (usePIBuf) {
            fBuf.append(piBuf);
            useFBuf = true;
        }

        // Now a tentative buffer for the Segments element
        StringBuffer segBuf = new StringBuffer(margn4
                + elementStart("mix:Segments") + EOL);
        boolean useSegBuf = false;
        if ((n = niso.getSegmentType()) != NisoImageMetadata.NULL) {
            segBuf.append(margn5
                    + element("mix:SegmentType", Integer.toString(n)) + EOL);
            useSegBuf = true;
        }
        long[] larray = niso.getStripOffsets();
        if (larray != null) {
            segBuf.append(margn5
                    + element("mix:StripOffsets", longArray(larray)) + EOL);
            useSegBuf = true;
        }
        long ln = niso.getRowsPerStrip();
        if (ln != NisoImageMetadata.NULL) {
            segBuf.append(margn5
                    + element("mix:RowsPerStrip", Long.toString(ln)) + EOL);
            useSegBuf = true;
        }
        if ((larray = niso.getStripByteCounts()) != null) {
            segBuf.append(margn5
                    + element("mix:StripByteCounts", longArray(larray)) + EOL);
            useSegBuf = true;
        }
        if ((ln = niso.getTileWidth()) != NisoImageMetadata.NULL) {
            segBuf.append(margn5 + element("mix:TileWidth", Long.toString(ln))
                    + EOL);
            useSegBuf = true;
        }
        if ((ln = niso.getTileLength()) != NisoImageMetadata.NULL) {
            segBuf.append(margn5 + element("mix:TileLength", Long.toString(ln))
                    + EOL);
            useSegBuf = true;
        }
        if ((larray = niso.getTileOffsets()) != null) {
            segBuf.append(margn5
                    + element("mix:TileOffsets", longArray(larray)) + EOL);
            useSegBuf = true;
        }
        if ((larray = niso.getTileByteCounts()) != null) {
            segBuf.append(margn5
                    + element("mix:TileByteCounts", longArray(larray)) + EOL);
            useSegBuf = true;
        }
        segBuf.append(margn4 + elementEnd("mix:Segments") + EOL);
        if (useSegBuf) {
            fBuf.append(segBuf);
            useFBuf = true;
        }
        if ((n = niso.getPlanarConfiguration()) != NisoImageMetadata.NULL) {
            fBuf.append(margn4
                    + element("mix:PlanarConfiguration", Integer.toString(n))
                    + EOL);
        }
        fBuf.append(margn3 + elementEnd("mix:Format") + EOL);
        if (useFBuf) {
            _writer.print(fBuf.toString());
        }

        // Now a tentative buffer for the File element. It's actually
        // pretty safe that this will have some content, but I'd rather
        // be consistent. Besides, it's a good test of the pattern.
        StringBuffer fileBuf = new StringBuffer(margn3
                + elementStart("mix:File") + EOL);
        boolean useFileBuf = false;
        s = niso.getImageIdentifier();
        if (s != null) {
            fileBuf.append(margn4 + element("mix:ImageIdentifier", s) + EOL);
            useFileBuf = true;
        }
        ln = niso.getFileSize();
        if (ln != NisoImageMetadata.NULL) {
            fileBuf.append(margn4 + element("mix:FileSize", Long.toString(ln))
                    + EOL);
            useFileBuf = true;
        }
        n = niso.getChecksumMethod();
        s = niso.getChecksumValue();
        if (n != NisoImageMetadata.NULL || s != null) {
            fileBuf.append(margn4 + elementStart("mix:Checksum") + EOL);
            if (n != NisoImageMetadata.NULL) {
                fileBuf.append(margn5
                        + element("mix:ChecksumMethod", Integer.toString(n))
                        + EOL);
            }
            if (s != null) {
                fileBuf.append(margn5 + element("mix:ChecksumValue", s) + EOL);
            }
            fileBuf.append(margn4 + elementEnd("mix:Checksum") + EOL);
            useFileBuf = true;
        }
        n = niso.getOrientation();
        if (n != NisoImageMetadata.NULL) {
            if (n > 9 || n < 1) {
                n = 9; // force "unknown" for reserved value
            }
            fileBuf.append(margn4
                    + element("mix:Orientation", Integer.toString(n)) + EOL);
            useFileBuf = true;
        }
        n = niso.getDisplayOrientation();
        if (n != NisoImageMetadata.NULL) {
            fileBuf.append(margn4
                    + element("mix:DisplayOrientation", Integer.toString(n))
                    + EOL);
            useFileBuf = true;
        }
        ln = niso.getXTargetedDisplayAR();
        long ln2 = niso.getYTargetedDisplayAR();
        if (ln != NisoImageMetadata.NULL || ln2 != NisoImageMetadata.NULL) {
            fileBuf.append(margn4 + elementStart("mix:TargetedDisplayAR") + EOL);
            if (ln != NisoImageMetadata.NULL) {
                fileBuf.append(margn5
                        + element("mix:XTargetedDisplayAR", Long.toString(ln))
                        + EOL);
            }
            if (ln2 != NisoImageMetadata.NULL) {
                fileBuf.append(margn5
                        + element("mix:YTargetedDisplayAR", Long.toString(ln2))
                        + EOL);
            }
            fileBuf.append(margn4 + elementEnd("mix:TargetedDisplayAR") + EOL);
            useFileBuf = true;
        }

        fileBuf.append(margn3 + elementEnd("mix:File") + EOL);
        if (useFileBuf) {
            _writer.print(fileBuf.toString());
        }
        _writer.println(margn2 + elementEnd("mix:BasicImageParameters"));

    }

    /* Top level element 2 of 4: ImageCreation */
    protected void showNisoImageCreation02(NisoImageMetadata niso, String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";
        String margn6 = margn5 + " ";

        _writer.println(margn2 + elementStart("mix:ImageCreation"));
        String s = niso.getSourceType();
        if (s != null) {
            _writer.println(margn3 + element("mix:SourceType", s));
        }
        s = niso.getSourceID();
        if (s != null) {
            _writer.println(margn3 + element("mix:SourceID", s));
        }
        s = niso.getImageProducer();
        if (s != null) {
            _writer.println(margn3 + element("mix:ImageProducer", s));
        }

        StringBuffer hostBuf = new StringBuffer(margn3
                + elementStart("mix:Host") + EOL);
        boolean useHostBuf = false;
        s = niso.getHostComputer();
        if (s != null) {
            hostBuf.append(margn4 + element("mix:HostComputer", s) + EOL);
            useHostBuf = true;
        }
        s = niso.getOS();
        if (s != null) {
            hostBuf.append(margn4 + element("mix:OperatingSystem", s) + EOL);
            useHostBuf = true;
        }
        s = niso.getOSVersion();
        if (s != null) {
            hostBuf.append(margn4 + element("mix:OSVersion", s) + EOL);
            useHostBuf = true;
        }
        hostBuf.append(margn3 + elementEnd("mix:Host") + EOL);
        if (useHostBuf) {
            _writer.print(hostBuf);
        }

        s = niso.getDeviceSource();
        if (s != null) {
            _writer.println(margn3 + element("mix:DeviceSource", s));
        }

        // Here things get rather deeply nested, and we want to be able
        // to back out if it turns out that we really have no content to
        // deliver. So rather than writing directly to the writer, we
        // write to a StringBuffer that may or may not get used.
        StringBuffer sscBuf = new StringBuffer(margn3
                + elementStart("mix:ScanningSystemCapture") + EOL);
        boolean useSSCBuf = false; // set to true if we find interesting data.
        StringBuffer sshBuf = new StringBuffer(margn4
                + elementStart("mix:ScanningSystemHardware") + EOL);
        boolean useSSHBuf = false;
        s = niso.getScannerManufacturer();
        if (s != null) {
            sshBuf.append(margn5 + element("mix:ScannerManufacturer", s) + EOL);
            useSSHBuf = true;
        }
        s = niso.getScannerModelName();
        String s1 = niso.getScannerModelNumber();
        String s2 = niso.getScannerModelSerialNo();
        if (s != null || s1 != null || s2 != null) {
            sshBuf.append(margn5 + elementStart("mix:ScannerModel") + EOL);
            if (s != null) {
                sshBuf.append(margn6 + element("mix:ScannerModelName", s) + EOL);
                useSSHBuf = true;
            }
            if (s1 != null) {
                sshBuf.append(margn6 + element("mix:ScannerModelNumber", s1)
                        + EOL);
                useSSHBuf = true;
            }
            if (s2 != null) {
                sshBuf.append(margn6 + element("mix:ScannerModelSerialNo ", s2)
                        + EOL);
                useSSHBuf = true;
            }
            sshBuf.append(margn5 + elementEnd("mix:ScannerModel") + EOL);
        }
        sshBuf.append(margn4 + elementEnd("mix:ScanningSystemHardware") + EOL);
        if (useSSHBuf) {
            // There's some ScanningSystemHardware content, and therefore some
            // ScanningSystemCapture content; keep the element.
            sscBuf.append(sshBuf);
            useSSCBuf = true;
        }

        // Build a tentative buffer for scanning system software
        StringBuffer sssBuf = new StringBuffer(margn4
                + elementStart("mix:ScanningSystemSoftware") + EOL);
        boolean useSSSBuf = false;
        s = niso.getScanningSoftware();
        if (s != null) {
            sssBuf.append(margn5 + element("mix:ScanningSoftware", s) + EOL);
            useSSSBuf = true;
        }
        s = niso.getScanningSoftwareVersionNo();
        if (s != null) {
            sssBuf.append(margn5 + element("mix:ScanningSoftwareVersionNo", s)
                    + EOL);
            useSSSBuf = true;
        }
        sssBuf.append(margn4 + elementEnd("mix:ScanningSystemSoftware") + EOL);
        if (useSSSBuf) {
            // There's some ScanningSystemSoftware content, and therefore some
            // ScanningSystemCapture content; keep the element.
            sscBuf.append(sssBuf);
            useSSCBuf = true;
        }

        // Build a tentative buffer for scanner capture settings
        StringBuffer scsBuf = new StringBuffer(margn4
                + elementStart("mix:ScannerCaptureSettings") + EOL);
        boolean useSCSBuf = false;
        double d = niso.getPixelSize();
        if (d != NisoImageMetadata.NILL) {
            scsBuf.append(margn5
                    + element("mix:PixelSize", formatters.get().format(d))
                    + EOL);
            useSCSBuf = true;
        }
        d = niso.getXPhysScanResolution();
        double d1 = niso.getYPhysScanResolution();
        if (d != NisoImageMetadata.NILL || d1 != NisoImageMetadata.NILL) {
            scsBuf.append(margn5 + elementStart("mix:PhysScanResolution") + EOL);
            if (d != NisoImageMetadata.NILL) {
                scsBuf.append(margn6
                        + element("mix:XphysScanResolution", formatters.get()
                                .format(d))
                        + EOL);
            }
            if (d1 != NisoImageMetadata.NILL) {
                scsBuf.append(margn6
                        + element("mix:YphysScanResolution", formatters.get()
                                .format(d1))
                        + EOL);
            }
            scsBuf.append(margn5 + elementEnd("mix:PhysScanResolution") + EOL);
            useSCSBuf = true;
        }
        scsBuf.append(margn4 + elementEnd("mix:ScannerCaptureSettings") + EOL);
        if (useSCSBuf) {
            sscBuf.append(scsBuf);
            useSSCBuf = true;
        }
        sscBuf.append(margn3 + elementEnd("mix:ScanningSystemCapture") + EOL);

        // Finally! Do we use any of this stuff we just went through?
        if (useSSCBuf) {
            _writer.print(sscBuf.toString());
        }

        // Same deal for digital camera capture; put the element in a tentative
        // StringBuffer and then decide if it's non-trivial.
        StringBuffer dccBuf = new StringBuffer(margn3
                + elementStart("mix:DigitalCameraCapture") + EOL);
        boolean useDCCBuf = false;
        s = niso.getDigitalCameraManufacturer();
        if (s != null) {
            dccBuf.append(margn4 + element("mix:DigitalCameraManufacturer", s)
                    + EOL);
            useDCCBuf = true;
        }
        s = niso.getDigitalCameraModelName();
        if (s != null) {
            dccBuf.append(margn4 + element("mix:DigitalCameraModel", s) + EOL);
            useDCCBuf = true;
        }

        dccBuf.append(margn3 + elementEnd("mix:DigitalCameraCapture") + EOL);
        if (useDCCBuf) {
            _writer.print(dccBuf.toString());
        }

        // Same tentative buffer deal for camera capture settings.
        StringBuffer ccsBuf = new StringBuffer(margn3
                + elementStart("mix:CameraCaptureSettings") + EOL);
        boolean useCCSBuf = false;
        d = niso.getFNumber();
        if (d != NisoImageMetadata.NILL) {
            ccsBuf.append(margn4
                    + element("mix:FNumber", formatters.get().format(d)) + EOL);
            useCCSBuf = true;
        }
        d = niso.getExposureTime();
        if (d != NisoImageMetadata.NILL) {
            ccsBuf.append(margn4
                    + element("mix:ExposureTime", formatters.get().format(d))
                    + EOL);
            useCCSBuf = true;
        }
        Rational r = niso.getBrightness();
        if (r != null) {
            d = r.toDouble();
            ccsBuf.append(margn4
                    + element("mix:Brightness", formatters.get().format(d))
                    + EOL);
            useCCSBuf = true;
        }
        r = niso.getExposureBias();
        if (r != null) {
            d = r.toDouble();
            ccsBuf.append(margn4
                    + element("mix:ExposureBias", formatters.get().format(d))
                    + EOL);
            useCCSBuf = true;
        }
        double[] darray = niso.getSubjectDistance();
        if (darray != null) {
            ccsBuf.append(margn4
                    + element("mix:SubjectDistance", doubleArray(darray)) + EOL);
            useCCSBuf = true;
        }
        int n = niso.getMeteringMode();
        if (n != NisoImageMetadata.NULL) {
            s = meteringModeToString(n);
            if (s.startsWith("Center weighted")) {
                s = "Center weighted Average";
            }
            ccsBuf.append(margn4 + element("mix:MeteringMode", s) + EOL);
            useCCSBuf = true;
        }
        n = niso.getSceneIlluminant();
        if (n != NisoImageMetadata.NULL) {
            ccsBuf.append(margn4
                    + element("mix:SceneIlluminant", Integer.toString(n)) + EOL);
            useCCSBuf = true;
        }
        d = niso.getColorTemp();
        if (d != NisoImageMetadata.NILL) {
            ccsBuf.append(margn4
                    + element("mix:ColorTemp", formatters.get().format(d))
                    + EOL);
            useCCSBuf = true;
        }
        d = niso.getFocalLength();
        if (d != NisoImageMetadata.NILL) {
            ccsBuf.append(margn4
                    + element("mix:FocalLength", formatters.get().format(d))
                    + EOL);
            useCCSBuf = true;
        }
        n = niso.getFlash();
        if (n != NisoImageMetadata.NULL) {
            // First bit (0 = Flash did not fire, 1 = Flash fired)
            int firstBit = n & 1;
            ccsBuf.append(margn4
                    + element("mix:Flash", NisoImageMetadata.FLASH[firstBit])
                    + EOL);
            useCCSBuf = true;
        }
        r = niso.getFlashEnergy();
        if (r != null) {
            d = r.toDouble();
            ccsBuf.append(margn4
                    + element("mix:FlashEnergy", formatters.get().format(d))
                    + EOL);
            useCCSBuf = true;
        }
        n = niso.getFlashReturn();
        if (n != NisoImageMetadata.NULL) {
            ccsBuf.append(margn4
                    + element("mix:FlashReturn", Integer.toString(n)) + EOL);
            useCCSBuf = true;
        }
        n = niso.getBackLight();
        if (n != NisoImageMetadata.NULL) {
            ccsBuf.append(margn4
                    + element("mix:BackLight", Integer.toString(n)) + EOL);
            useCCSBuf = true;
        }
        d = niso.getExposureIndex();
        if (d != NisoImageMetadata.NILL) {
            ccsBuf.append(margn4
                    + element("mix:ExposureIndex", formatters.get().format(d))
                    + EOL);
            useCCSBuf = true;
        }
        n = niso.getAutoFocus();
        if (n != NisoImageMetadata.NULL) {
            ccsBuf.append(margn4
                    + element("mix:AutoFocus", Integer.toString(n)) + EOL);
            useCCSBuf = true;
        }
        d = niso.getXPrintAspectRatio();
        d1 = niso.getYPrintAspectRatio();
        if (d != NisoImageMetadata.NILL || d1 != NisoImageMetadata.NILL) {
            ccsBuf.append(margn4 + elementStart("mix:PrintAspectRatio") + EOL);
            if (d != NisoImageMetadata.NILL) {
                ccsBuf.append(margn5
                        + element("mix:XPrintAspectRatio", formatters.get()
                                .format(d))
                        + EOL);
            }
            if (d1 != NisoImageMetadata.NILL) {
                ccsBuf.append(margn5
                        + element("mix:YPrintAspectRatio", formatters.get()
                                .format(d1))
                        + EOL);
                ccsBuf.append(margn4 + elementEnd("mix:PrintAspectRatio") + EOL);
                useCCSBuf = true;
            }
        }
        ccsBuf.append(margn3 + elementEnd("mix:CameraCaptureSettings") + EOL);
        if (useCCSBuf) {
            _writer.print(ccsBuf.toString());
        }

        // Finally we get a relative breather without having to check nested
        // elements.
        n = niso.getSensor();
        if (n != NisoImageMetadata.NULL) {
            _writer.println(margn3 + element("mix:Sensor", Integer.toString(n)));
        }
        s = niso.getDateTimeCreated();
        if (s != null) {
            _writer.println(margn3 + element("mix:DateTimeCreated", s));
        }
        s = niso.getMethodology();
        if (s != null) {
            _writer.println(margn3 + element("mix:Methodology", s));
        }

        _writer.println(margn2 + elementEnd("mix:ImageCreation"));
    }

    /* Top level element 3 of 4: ImagingPerformanceAssessment */
    protected void showNisoImagingPerformanceAssessment02(
            NisoImageMetadata niso, String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";
        String margn6 = margn5 + " ";
        String margn7 = margn6 + " ";

        StringBuffer ipaBuf = new StringBuffer(margn3
                + elementStart("mix:ImagingPerformanceAssessment") + EOL);
        boolean useIPABuf = false; // set to true if we find interesting data.
        StringBuffer smBuf = new StringBuffer(margn4
                + elementStart("mix:SpatialMetrics") + EOL);
        boolean useSMBuf = false;

        int n = niso.getSamplingFrequencyPlane();
        if (n != NisoImageMetadata.NULL) {
            smBuf.append(margn5
                    + element("mix:SamplingFrequencyPlane", Integer.toString(n))
                    + EOL);
            useSMBuf = true;
        }
        n = niso.getSamplingFrequencyUnit();
        if (n != NisoImageMetadata.NULL) {
            smBuf.append(margn5
                    + element("mix:SamplingFrequencyUnit", Integer.toString(n))
                    + EOL);
            useSMBuf = true;
        }
        Rational r = niso.getXSamplingFrequency();
        if (r != null) {
            smBuf.append(margn5
                    + element("mix:XSamplingFrequency",
                            Long.toString(r.toLong()))
                    + EOL);
            useSMBuf = true;
        }
        r = niso.getYSamplingFrequency();
        if (r != null) {
            smBuf.append(margn5
                    + element("mix:YSamplingFrequency",
                            Long.toString(r.toLong()))
                    + EOL);
            useSMBuf = true;
        }
        long ln = niso.getImageWidth();
        if (ln != NisoImageMetadata.NULL) {
            smBuf.append(margn5 + element("mix:ImageWidth", Long.toString(ln))
                    + EOL);
            useSMBuf = true;
        }
        ln = niso.getImageLength();
        if (ln != NisoImageMetadata.NULL) {
            smBuf.append(margn5 + element("mix:ImageLength", Long.toString(ln))
                    + EOL);
            useSMBuf = true;
        }

        double d = niso.getSourceXDimension();
        n = niso.getSourceXDimensionUnit();
        if (d != NisoImageMetadata.NILL || n != NisoImageMetadata.NULL) {
            smBuf.append(margn5 + elementStart("mix:Source_X") + EOL);
            if (d != NisoImageMetadata.NILL) {
                smBuf.append(margn6
                        + element("mix:Source_Xdimension", formatters.get()
                                .format(d))
                        + EOL);
            }
            if (n != NisoImageMetadata.NULL) {
                smBuf.append(margn6
                        + element("mix:Source_XdimensionUnit",
                                Integer.toString(n))
                        + EOL);
            }
            smBuf.append(margn5 + elementEnd("mix:Source_X") + EOL);
            useSMBuf = true;
        }

        d = niso.getSourceYDimension();
        n = niso.getSourceYDimensionUnit();
        if (d != NisoImageMetadata.NILL || n != NisoImageMetadata.NULL) {
            smBuf.append(margn4 + elementStart("mix:Source_Y") + EOL);
            if (d != NisoImageMetadata.NILL) {
                smBuf.append(margn5
                        + element("mix:Source_Ydimension", formatters.get()
                                .format(d))
                        + EOL);
            }
            if (n != NisoImageMetadata.NULL) {
                smBuf.append(margn5
                        + element("mix:Source_YdimensionUnit",
                                Integer.toString(n))
                        + EOL);
            }
            smBuf.append(margn4 + elementEnd("mix:Source_Y") + EOL);
            useSMBuf = true;
        }
        smBuf.append(margn3 + elementEnd("mix:SpatialMetrics") + EOL);
        if (useSMBuf) {
            ipaBuf.append(smBuf);
            useIPABuf = true;
        }

        // Now a tentative buffer for the Energetics element
        StringBuffer eBuf = new StringBuffer(margn3
                + elementStart("mix:Energetics") + EOL);
        boolean useEBuf = false;
        int[] iarray = niso.getBitsPerSample();
        if (iarray != null) {
            eBuf.append(margn4
                    + element("mix:BitsPerSample", integerArray(iarray, ','))
                    + EOL);
            useEBuf = true;
        }
        n = niso.getSamplesPerPixel();
        if (n != NisoImageMetadata.NULL) {
            eBuf.append(margn4
                    + element("mix:SamplesPerPixel", Integer.toString(n)) + EOL);
            useEBuf = true;
        }
        iarray = niso.getExtraSamples();
        if (iarray != null) {
            // extraSamples can only be an integer, so the best we can do is
            // snag the first value from the array. It also must be limited to
            // 0, 1, 2, or 3.
            n = iarray[0];
            if (n >= 0 && n <= 3) {
                eBuf.append(margn4
                        + element("mix:ExtraSamples", Integer.toString(n))
                        + EOL);
                useEBuf = true;
            }
            // This is what we'd really like to do, but it violates the schema.
            // Keep this code around in the event the schema is fixed in the
            // future.
            // eBuf.append (margn4 + element ("mix:ExtraSamples",
            // integerArray (iarray)) + EOL);
            // useEBuf = true;
        }
        // Tentative buffer for colormap element within Energetics
        StringBuffer cmBuf = new StringBuffer(margn4
                + elementStart("mix:Colormap") + EOL);
        boolean useCMBuf = false;
        String s = niso.getColormapReference();
        if (s != null) {
            cmBuf.append(margn5 + element("mix:Reference", s) + EOL);
            useCMBuf = true;
        }
        iarray = niso.getColormapRedValue();
        if (iarray != null) {
            cmBuf.append(margn5 + elementStart("mix:Wrap"));
            // If the red array is there, assume the others are too, and are
            // equal in length.
            int[] bcarray = niso.getColormapBitCodeValue();
            int[] garray = niso.getColormapGreenValue();
            int[] barray = niso.getColormapBlueValue();
            try {
                for (int i = 0; i < iarray.length; i++) {
                    cmBuf.append(margn6 + elementStart("mix:Color") + EOL);
                    n = bcarray[i];
                    if (n != NisoImageMetadata.NULL) {
                        cmBuf.append(margn7
                                + element("mix:BitCodeValue",
                                        Integer.toString(n))
                                + EOL);
                    }
                    n = iarray[i];
                    if (n != NisoImageMetadata.NULL) {
                        cmBuf.append(margn7
                                + element("mix:RedValue", Integer.toString(n))
                                + EOL);
                    }
                    n = garray[i];
                    if (n != NisoImageMetadata.NULL) {
                        cmBuf.append(margn7
                                + element("mix:GreenValue", Integer.toString(n))
                                + EOL);
                    }
                    n = barray[i];
                    if (n != NisoImageMetadata.NULL) {
                        cmBuf.append(margn7
                                + element("mix:BlueValue", Integer.toString(n))
                                + EOL);
                    }

                    cmBuf.append(margn6 + elementEnd("mix:Color") + EOL);
                }
            } catch (Exception e) {
                // If the assumption mentioned above is wrong,
                // we'll get broken XML, but at least won't die here.
            }
            cmBuf.append(margn5 + elementEnd("mix:Wrap") + EOL);
            useCMBuf = true;
        }
        cmBuf.append(margn4 + elementEnd("mix:Colormap"));
        if (useCMBuf) {
            eBuf.append(cmBuf);
            useEBuf = true;
        }

        iarray = niso.getGrayResponseCurve();
        n = niso.getGrayResponseUnit();
        if (iarray != null || n != NisoImageMetadata.NULL) {
            eBuf.append(margn4 + elementStart("mix:GrayResponse") + EOL);
            if (iarray != null) {
                eBuf.append(margn5
                        + element("mix:GrayResponseCurve", integerArray(iarray))
                        + EOL);
            }
            if (n != NisoImageMetadata.NULL) {
                eBuf.append(margn5
                        + element("mix:GrayResponseUnit", Integer.toString(n))
                        + EOL);
            }
            eBuf.append(margn4 + elementEnd("mix:GrayResponse") + EOL);
            useEBuf = true;
        }
        r = niso.getWhitePointXValue();
        Rational r1 = niso.getWhitePointYValue();
        if (r != null || r1 != null) {
            // These are specified in the scheme as CIExyType. All the
            // schema tells us about this type is that it's a string.
            eBuf.append(margn4 + elementStart("mix:WhitePoint") + EOL);
            if (r != null) {
                eBuf.append(margn5
                        + element("mix:WhitePoint_Xvalue", r.toString()) + EOL);
            }
            if (r1 != null) {
                eBuf.append(margn5
                        + element("mix:WhitePoint_Yvalue", r1.toString()) + EOL);
            }
            eBuf.append(margn4 + elementEnd("mix:WhitePoint") + EOL);
            useEBuf = true;
        }

        r = niso.getPrimaryChromaticitiesRedX();
        // For simplicity, we check only the red x in deciding whether
        // to incorporate this element. A partial set of chromaticities
        // would be meaningless anyway.
        if (r != null) {
            eBuf.append(margn4 + elementStart("mix:PrimaryChromaticities")
                    + EOL);
            eBuf.append(margn5
                    + element("mix:PrimaryChromaticities_RedX", r.toString())
                    + EOL);
            r = niso.getPrimaryChromaticitiesRedY();
            if (r != null) {
                eBuf.append(margn5
                        + element("mix:PrimaryChromaticities_RedY",
                                r.toString())
                        + EOL);
            }
            r = niso.getPrimaryChromaticitiesGreenX();
            if (r != null) {
                eBuf.append(margn5
                        + element("mix:PrimaryChromaticities_GreenX",
                                r.toString())
                        + EOL);
            }
            r = niso.getPrimaryChromaticitiesGreenY();
            if (r != null) {
                eBuf.append(margn5
                        + element("mix:PrimaryChromaticities_GreenY",
                                r.toString())
                        + EOL);
            }
            r = niso.getPrimaryChromaticitiesBlueX();
            if (r != null) {
                eBuf.append(margn5
                        + element("mix:PrimaryChromaticities_BlueX",
                                r.toString())
                        + EOL);
            }
            r = niso.getPrimaryChromaticitiesBlueY();
            if (r != null) {
                eBuf.append(margn5
                        + element("mix:PrimaryChromaticities_BlueY",
                                r.toString())
                        + EOL);
            }
            eBuf.append(margn4 + elementEnd("mix:PrimaryChromaticities") + EOL);
            useEBuf = true;
        }

        eBuf.append(margn3 + elementEnd("mix:Energetics") + EOL);
        if (useEBuf) {
            ipaBuf.append(eBuf);
            useIPABuf = true;
        }

        // Another tentative buffer for TargetData
        StringBuffer tdBuf = new StringBuffer(margn3
                + elementStart("mix:TargetData") + EOL);
        boolean useTDBuf = false;
        n = niso.getTargetType();
        if (n != NisoImageMetadata.NULL) {
            tdBuf.append(margn4
                    + element("mix:TargetType", Integer.toString(n)) + EOL);
            useTDBuf = true;
        }

        // Nest a TargetID tentative buffer in the TargetData buffer
        StringBuffer tiBuf = new StringBuffer(margn4
                + elementStart("mix:TargetID") + EOL);
        boolean useTIBuf = false;
        s = niso.getTargetIDManufacturer();
        if (s != null) {
            tiBuf.append(margn5 + element("mix:TargetIDManufacturer", s) + EOL);
            useTIBuf = true;
        }
        s = niso.getTargetIDName();
        if (s != null) {
            tiBuf.append(margn5 + element("mix:TargetIDName", s) + EOL);
            useTIBuf = true;
        }
        s = niso.getTargetIDNo();
        if (s != null) {
            tiBuf.append(margn5 + element("mix:TargetIDNo", s) + EOL);
            useTIBuf = true;
        }
        s = niso.getTargetIDMedia();
        if (s != null) {
            tiBuf.append(margn5 + element("mix:TargetIDMedia", s) + EOL);
            useTIBuf = true;
        }
        tiBuf.append(margn4 + elementEnd("mix:TargetID") + EOL);
        if (useTIBuf) {
            tdBuf.append(tiBuf);
            useTDBuf = true;
        }

        s = niso.getImageData();
        if (s != null) {
            tdBuf.append(margn5 + element("mix:ImageData", s) + EOL);
            useTDBuf = true;
        }
        s = niso.getPerformanceData();
        if (s != null) {
            tdBuf.append(margn5 + element("mix:PerformanceData", s) + EOL);
            useTDBuf = true;
        }
        s = niso.getProfiles();
        if (s != null) {
            tdBuf.append(margn5 + element("mix:Profiles", s) + EOL);
            useTDBuf = true;
        }
        tdBuf.append(margn3 + elementEnd("mix:TargetData") + EOL);
        if (useTDBuf) {
            ipaBuf.append(tdBuf);
            useIPABuf = true;
        }

        ipaBuf.append(margn2 + elementEnd("mix:ImagingPerformanceAssessment")
                + EOL);
        if (useIPABuf) {
            _writer.print(ipaBuf.toString());
        }
    }

    /* Top level element 4 of 4: ChangeHistory */
    protected void showNisoChangeHistory02(NisoImageMetadata niso, String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";

        // Yet again, build elements in tentative buffers and throw them
        // away if they prove trivial.
        StringBuffer chBuf = new StringBuffer(margn2
                + elementStart("mix:ChangeHistory") + EOL);
        boolean useCHBuf = false;
        StringBuffer ipBuf = new StringBuffer(margn3
                + elementStart("mix:ImageProcessing") + EOL);
        boolean useIPBuf = false;

        String s = niso.getDateTimeProcessed();
        if (s != null) {
            ipBuf.append(margn4 + element("DateTimeProcessed", s) + EOL);
            useIPBuf = true;
        }
        s = niso.getSourceData();
        if (s != null) {
            ipBuf.append(margn4 + element("SourceData", s) + EOL);
            useIPBuf = true;
        }
        s = niso.getProcessingAgency();
        if (s != null) {
            ipBuf.append(margn4 + element("ProcessingAgency", s) + EOL);
            useIPBuf = true;
        }

        // Third-level nesting of tentative buffer!
        StringBuffer psBuf = new StringBuffer(margn4
                + elementStart("ProcessingSoftware") + EOL);
        boolean usePSBuf = false;
        s = niso.getProcessingSoftwareName();
        if (s != null) {
            psBuf.append(margn5 + element("ProcessingSoftwareName", s) + EOL);
            usePSBuf = true;
        }
        s = niso.getProcessingSoftwareVersion();
        if (s != null) {
            psBuf.append(margn5 + element("ProcessingSoftwareVersion", s) + EOL);
            usePSBuf = true;
        }
        psBuf.append(margn4 + elementEnd("ProcessingSoftware") + EOL);
        if (usePSBuf) {
            ipBuf.append(psBuf);
            useIPBuf = true;
        }

        // Hard to say, but I think the intent is that there be
        // one ProcessingActions element per processing action.
        String[] sarray = niso.getProcessingActions();
        if (sarray != null) {
            for (int i = 0; i < sarray.length; i++) {
                ipBuf.append(margn4 + element("ProcessingActions", sarray[i])
                        + EOL);
            }
            useIPBuf = true;
        }
        ipBuf.append(margn3 + elementEnd("mix:ImageProcessing") + EOL);
        if (useIPBuf) {
            chBuf.append(ipBuf);
            useCHBuf = true;
        }
        chBuf.append(margn2 + elementEnd("mix:ChangeHistory") + EOL);
        if (useCHBuf) {
            _writer.print(chBuf.toString());
        }
    }

    /**
     * Display the NISO image metadata formatted according to the MIX 1.0
     * schema.
     */
    protected void showNisoImageMetadata10(NisoImageMetadata niso) {
        String margin = getIndent(++_level);

        String[][] attrs = {
                { "xmlns:mix", "http://www.loc.gov/mix/v10" },
                { "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" },
                { "xsi:schemaLocation",
                        "http://www.loc.gov/mix/v10 http://www.loc.gov/standards/mix/mix10/mix10.xsd" } };
        _writer.println(margin + elementStart("mix:mix", attrs));

        showNisoBasicDigitalObjectInformation10(niso, margin);
        showNisoBasicImageInformation10(niso, margin);
        showNisoImageCaptureMetadata10(niso, margin);
        showNisoImageAssessmentMetadata10(niso, margin);
        showChangeHistory10(niso, margin);

        _writer.println(margin + elementEnd("mix:mix"));

        _level--;

    }

    /*
     * The NISO Metadata output (1.0 now) is split into multiple functions so
     * that they're merely outrageously big rather than disgustingly big
     */
    /* Top level element 1 of 5: BasicDigitalObjectInformation */
    protected void showNisoBasicDigitalObjectInformation10(
            NisoImageMetadata niso, String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";

        _writer.println(margn2
                + elementStart("mix:BasicDigitalObjectInformation"));

        StringBuffer objIDBuf = new StringBuffer(margn3
                + elementStart("mix:ObjectIdentifier") + EOL);
        boolean useObjIDBuf = false;
        objIDBuf.append(margn4 + element("mix:objectIdentifierType", "JHOVE")
                + EOL);
        String s = niso.getImageIdentifier();
        if (s != null) {
            objIDBuf.append(margn4 + element("mix:objectIdentifierValue", s)
                    + EOL);
            useObjIDBuf = true;
        }
        objIDBuf.append(margn3 + elementEnd("mix:ObjectIdentifier") + EOL);
        if (useObjIDBuf) {
            _writer.print(objIDBuf.toString());
        }
        long ln = niso.getFileSize();
        if (ln != NisoImageMetadata.NULL) {
            _writer.print(margn4 + element("mix:fileSize", Long.toString(ln))
                    + EOL);
        }

        // TODO we really should output a FormatDesignation, but it isn't in the
        // NisoImageMetadata class yet.
        // TODO If we output a FormatDesignation, we should output a
        // FormatRegistry.

        if ((s = niso.getByteOrder()) != null) {
            // Convert strings to MIX 1.0 form
            if (s.startsWith("big")) {
                s = "big_endian";
            } else if (s.startsWith("little")) {
                s = "little_endian";
            }
            _writer.print(margn4 + element("mix:byteOrder", s) + EOL);
        }
        int comp = niso.getCompressionScheme();
        int level = niso.getCompressionLevel();
        if (comp != NisoImageMetadata.NULL || level != NisoImageMetadata.NULL) {
            _writer.print(margn4 + elementStart("mix:Compression") + EOL);
            if (comp != NisoImageMetadata.NULL) {
                if (comp == 34713 || comp == 34714) {
                    _writer.print(margn5
                            + element("mix:compressionScheme",
                                    compressionSchemeToString(comp))
                            + EOL);
                    if (level != NisoImageMetadata.NULL) {
                        _writer.print(margn5
                                + element("mix:compressionRatio",
                                        Integer.toString(level))
                                + EOL);
                    }
                } else {
                    _writer.print(margn5
                            + element("mix:compressionScheme",
                                    Integer.toString(comp))
                            + EOL);
                }
            }
            // TODO it isn't clear how to get from compression level to
            // compression ratio

            _writer.print(margn4 + elementEnd("mix:Compression") + EOL);
        }
        int n = niso.getChecksumMethod();
        s = niso.getChecksumValue();
        if (n != NisoImageMetadata.NULL || s != null) {
            _writer.print(margn4 + elementStart("mix:Fixity") + EOL);
            if (n != NisoImageMetadata.NULL) {
                _writer.print(margn5
                        + element("mix:messageDigestAlgorithm",
                                Integer.toString(n))
                        + EOL);
            }
            if (s != null) {
                _writer.print(margn5 + element("mix:messageDigest", s) + EOL);
            }
            _writer.println(margn4 + elementEnd("mix:Fixity"));
        }
        _writer.println(margn3
                + elementEnd("mix:BasicDigitalObjectInformation"));
    }

    /* 1.0, Top level element 2 of 5: BasicImageInformation */
    protected void showNisoBasicImageInformation10(NisoImageMetadata niso,
            String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";
        String margn6 = margn5 + " ";
        String margn7 = margn6 + " ";
        _writer.println(margn2 + elementStart("mix:BasicImageInformation"));
        StringBuffer basCharBuf = new StringBuffer(margn3
                + elementStart("mix:BasicImageCharacteristics") + EOL);
        boolean useBasCharBuf = false;
        long ln = niso.getImageWidth();
        if (ln != NisoImageMetadata.NULL) {
            basCharBuf.append(margn4
                    + element("mix:imageWidth", Long.toString(ln)) + EOL);
            useBasCharBuf = true;
        }
        ln = niso.getImageLength();
        if (ln != NisoImageMetadata.NULL) {
            basCharBuf.append(margn4
                    + element("mix:imageHeight", Long.toString(ln)) + EOL);
            useBasCharBuf = true;
        }
        // Nest photometric interpretation tentative buffer in basCharBuf
        StringBuffer piBuf = new StringBuffer(margn4
                + elementStart("mix:PhotometricInterpretation") + EOL);
        boolean usePIBuf = false;
        int n = niso.getColorSpace();
        if (n != NisoImageMetadata.NULL) {
            piBuf.append(margn5
                    + element("mix:colorSpace", Integer.toString(n)) + EOL);
            usePIBuf = true;
        }
        String s = niso.getProfileName();
        String s2 = niso.getProfileURL();
        if (s != null || s2 != null) {
            piBuf.append(margn5 + elementStart("mix:ColorProfile") + EOL);
            piBuf.append(margn6 + elementStart("mix:IccProfile") + EOL);
            if (s != null) {
                piBuf.append(margn7 + element("mix:iccProfileName", s) + EOL);
            }
            if (s2 != null) {
                piBuf.append(margn7 + element("mix:iccProfileURL", s2) + EOL);
            }
            piBuf.append(margn6 + elementEnd("mix:IccProfile") + EOL);
            piBuf.append(margn5 + elementEnd("mix:ColorProfile") + EOL);
            usePIBuf = true;
        }
        int[] iarray = niso.getYCbCrSubSampling();
        n = niso.getYCbCrPositioning();
        Rational[] rarray = niso.getYCbCrCoefficients();
        if (iarray != null || n != NisoImageMetadata.NULL || rarray != null) {
            piBuf.append(margn5 + elementStart("mix:YCbCr") + EOL);
            usePIBuf = true;
            if (iarray != null && iarray.length >= 2) {
                piBuf.append(margn6 + elementStart("mix:YCbCrSubSampling")
                        + EOL);
                piBuf.append(margn7
                        + element("mix:yCbCrSubsampleHoriz",
                                Integer.toString(iarray[0]))
                        + EOL);
                piBuf.append(margn7
                        + element("mix:yCbCrSubsampleVert",
                                Integer.toString(iarray[1]))
                        + EOL);
                piBuf.append(margn6 + elementEnd("mix:YCbCrSubSampling") + EOL);
            }
            if (n != NisoImageMetadata.NULL) {
                piBuf.append(margn6
                        + element("mix:yCbCrPositioning", Integer.toString(n))
                        + EOL);
            }
            if (rarray != null) {
                piBuf.append(margn6
                        + element("mix:yCbCrCoefficients",
                                rationalArray10(rarray))
                        + EOL);
            }
            piBuf.append(margn5 + elementEnd("mix:YCbCr") + EOL);
        }
        rarray = niso.getReferenceBlackWhite();
        if (rarray != null) {
            piBuf.append(margn6
                    + element("mix:referenceBlackWhite",
                            rationalArray10(rarray))
                    + EOL);
            usePIBuf = true;
        }
        piBuf.append(margn4 + elementEnd("mix:PhotometricInterpretation") + EOL);
        if (usePIBuf) {
            basCharBuf.append(piBuf);
            useBasCharBuf = true;
        }
        basCharBuf.append(margn3 + elementEnd("mix:BasicImageCharacteristics"));

        if (useBasCharBuf) {
            _writer.println(basCharBuf);
        }
        // SpecialFormatCharacteristics limited to JPEG2000
        StringBuffer speCharBuf = new StringBuffer(margn3
                + elementStart("mix:SpecialFormatCharacteristics") + EOL);
        boolean useSpeCharBuf = false;
        int lay = niso.getJp2Layers();
        int lev = niso.getJp2ResolutionLevels();
        String sizTiles = niso.getJp2Tiles();
        if (sizTiles != null || lay != NisoImageMetadata.NULL
                || lev != NisoImageMetadata.NULL) {

            useSpeCharBuf = true;
            speCharBuf.append(margn4 + elementStart("mix:JPEG2000") + EOL);
            speCharBuf.append(margn5 + elementStart("mix:EncodingOptions")
                    + EOL);
            if (sizTiles != null) {
                speCharBuf
                        .append(margn6 + element("mix:tiles", sizTiles) + EOL);
            }
            if (lay != NisoImageMetadata.NULL) {
                speCharBuf.append(margn6
                        + element("mix:qualityLayers", Integer.toString(lay))
                        + EOL);
            }
            if (sizTiles != null) {
                speCharBuf
                        .append(margn6
                                + element("mix:resolutionLevels",
                                        Integer.toString(lev))
                                + EOL);
            }
            speCharBuf.append(margn5 + elementEnd("mix:EncodingOptions") + EOL);
            speCharBuf.append(margn4 + elementEnd("mix:JPEG2000") + EOL);
        }

        speCharBuf.append(margn3
                + elementEnd("mix:SpecialFormatCharacteristics"));
        if (useSpeCharBuf) {
            _writer.println(speCharBuf);
        }

        _writer.println(margn2 + elementEnd("mix:BasicImageInformation"));
    }

    /* 1.0, Top level element 3 of 5: ImageCaptureMetadata */
    protected void showNisoImageCaptureMetadata10(NisoImageMetadata niso,
            String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";
        String margn6 = margn5 + " ";
        String margn7 = margn6 + " ";

        StringBuffer captureBuffer = new StringBuffer();
        boolean useCaptureBuffer = false;

        String s = niso.getSourceType();
        if (s != null) {
            captureBuffer.append(margn3 + element("mix:sourceType", s));
            useCaptureBuffer = true;
        }
        s = niso.getSourceID();
        if (s != null) {
            captureBuffer.append(margn3 + elementStart("mix:SourceID"));
            captureBuffer.append(margn3 + element("mix:sourceIDValue", s));
            captureBuffer.append(margn3 + elementEnd("mix:sourceID"));
            useCaptureBuffer = true;
        }
        double d = niso.getSourceXDimension();
        int n = niso.getSourceXDimensionUnit();
        if (d != NisoImageMetadata.NILL || n != NisoImageMetadata.NULL) {
            // Assume that both X and Y exist, or neither
            captureBuffer.append(margn3 + elementStart("mix:SourceSize"));
            captureBuffer.append(margn4 + elementStart("mix:SourceXDimension")
                    + EOL);
            if (d != NisoImageMetadata.NILL) {
                captureBuffer.append(margn5
                        + element("mix:sourceXDimensionValue", formatters.get()
                                .format(d))
                        + EOL);
            }
            if (n != NisoImageMetadata.NULL) {
                captureBuffer.append(margn5
                        + element("mix:sourceXDimensionUnit",
                                Integer.toString(n))
                        + EOL);
            }
            captureBuffer.append(margn4 + elementEnd("mix:SourceXDimension")
                    + EOL);

            d = niso.getSourceYDimension();
            n = niso.getSourceYDimensionUnit();
            if (d != NisoImageMetadata.NILL || n != NisoImageMetadata.NULL) {
                captureBuffer.append(margn4
                        + elementStart("mix:SourceYDimension") + EOL);
                if (d != NisoImageMetadata.NILL) {
                    captureBuffer.append(margn5
                            + element("mix:sourceYDimensionValue", formatters
                                    .get().format(d))
                            + EOL);
                }
                if (n != NisoImageMetadata.NULL) {
                    captureBuffer.append(margn5
                            + element("mix:sourceYDimensionUnit",
                                    Integer.toString(n))
                            + EOL);
                }
                captureBuffer.append(margn4
                        + elementEnd("mix:SourceYDimension") + EOL);
            }
            captureBuffer.append(margn3 + elementEnd("mix:SourceSize") + EOL);
            useCaptureBuffer = true;
        }
        StringBuffer genCapBuf = new StringBuffer(margn3
                + elementStart("mix:GeneralCaptureInformation") + EOL);
        boolean useGenCapBuf = false;

        s = niso.getDateTimeCreated();
        if (s != null) {
            genCapBuf.append(margn3 + element("mix:dateTimeCreated", s) + EOL);
            useGenCapBuf = true;
        }
        s = niso.getImageProducer();
        if (s != null) {
            genCapBuf.append(margn3 + element("mix:imageProducer", s) + EOL);
            useGenCapBuf = true;
        }

        s = niso.getDeviceSource();
        if (s != null) {
            genCapBuf.append(margn3 + element("mix:captureDevice", s) + EOL);
            /*
             * This has a restricted set of values. Does the setting code
             * conform?
             */
        }

        genCapBuf.append(margn3 + elementEnd("mix:GeneralCaptureInformation")
                + EOL);
        if (useGenCapBuf) {
            captureBuffer.append(genCapBuf);
            useCaptureBuffer = true;
        }

        // Here's a chunk of XML for scanners.
        StringBuffer scanCapBuf = new StringBuffer(margn3
                + elementStart("mix:ScannerCapture") + EOL);
        boolean useScanCapBuf = false;
        String mfg = niso.getScannerManufacturer();
        if (mfg != null) {
            scanCapBuf.append(margn4 + element("mix:scannerManufacturer", mfg)
                    + EOL);
            useScanCapBuf = true;
        }
        String model = niso.getScannerModelName();
        String modelNum = niso.getScannerModelNumber();
        String serNum = niso.getScannerModelSerialNo();
        if (model != null || modelNum != null || serNum != null) {
            useScanCapBuf = true;
            scanCapBuf.append(margn4 + elementStart("mix:ScannerModel") + EOL);
            if (model != null) {
                scanCapBuf.append(margn5
                        + element("mix:scannerModelName", model) + EOL);
            }
            if (modelNum != null) {
                scanCapBuf.append(margn5
                        + element("mix:scannerModelNumber", modelNum) + EOL);
            }
            if (serNum != null) {
                scanCapBuf.append(margn5
                        + element("mix:scannerModelSerialNo", serNum) + EOL);
            }
            scanCapBuf.append(margn4 + elementEnd("mix:ScannerModel") + EOL);
        }
        double xres = niso.getXPhysScanResolution();
        double yres = niso.getYPhysScanResolution();
        if (xres != NisoImageMetadata.NULL && yres != NisoImageMetadata.NULL) {
            double res = (xres > yres ? xres : yres);
            scanCapBuf.append(margn4
                    + element("mix:maximumOpticalResolution", formatters.get()
                            .format(res))
                    + EOL);
        }
        s = niso.getScanningSoftware();
        if (s != null) {
            useScanCapBuf = true;
            scanCapBuf.append(margn4
                    + elementStart("mix:ScanningSystemSoftware") + EOL);
            scanCapBuf.append(margn5 + element("mix:scanningSoftwareName", s)
                    + EOL);
            s = niso.getScanningSoftwareVersionNo();
            if (s != null) {
                scanCapBuf.append(margn5
                        + element("mix:scanningSoftwareVersionNo", s) + EOL);
            }
            scanCapBuf.append(margn4 + elementEnd("mix:ScanningSystemSoftware")
                    + EOL);
        }
        scanCapBuf.append(margn3 + elementEnd("mix:ScannerCapture") + EOL);
        if (useScanCapBuf) {
            captureBuffer.append(scanCapBuf);
            useCaptureBuffer = true;
        }

        // Now we'll hear from the digital cameras.
        StringBuffer digCamBuf = new StringBuffer(margn3
                + elementStart("mix:DigitalCameraCapture") + EOL);
        boolean useDigCamBuf = false;

        s = niso.getDigitalCameraManufacturer();
        if (s != null) {
            digCamBuf.append(margn4
                    + element("mix:digitalCameraManufacturer", s) + EOL);
            useDigCamBuf = true;
        }
        String dcmodel = niso.getDigitalCameraModelName();
        String dcmodelNum = niso.getDigitalCameraModelNumber();
        String dcserNum = niso.getDigitalCameraModelSerialNo();
        if (dcmodel != null || dcmodelNum != null || dcserNum != null) {
            useDigCamBuf = true;
            digCamBuf.append(margn4 + elementStart("mix:DigitalCameraModel")
                    + EOL);
            if (dcmodel != null) {
                digCamBuf.append(margn5
                        + element("mix:digitalCameraModelName", dcmodel) + EOL);
            }
            if (dcmodelNum != null) {
                digCamBuf.append(margn5
                        + element("mix:digitalCameraModelNumber", dcmodelNum)
                        + EOL);
            }
            if (dcserNum != null) {
                digCamBuf.append(margn5
                        + element("mix:mix:digitalCameraModelSerialNo",
                                dcserNum)
                        + EOL);
            }
            digCamBuf.append(margn4 + elementEnd("mix:DigitalCameraModel")
                    + EOL);
        }

        // Nest a buffer for CameraCaptureSettings
        StringBuffer ccSetBuf = new StringBuffer(margn4
                + elementStart("mix:CameraCaptureSettings") + EOL);
        boolean useCcSetBuf = false;
        // CameraCaptureSettings consists only of an ImageData element, so we
        // don't need another use flag here.
        ccSetBuf.append(margn5 + elementStart("mix:ImageData") + EOL);
        d = niso.getFNumber();
        if (d != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:fNumber", formatters.get().format(d)) + EOL);
            useCcSetBuf = true;
        }
        d = niso.getExposureTime();
        if (d != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:exposureTime", formatters.get().format(d))
                    + EOL);
            useCcSetBuf = true;
        }
        n = niso.getExposureProgram();
        if (n != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:exposureProgram", Integer.toString(n)) + EOL);
            useCcSetBuf = true;
        }
        s = niso.getExifVersion();
        if ("0220".equals(s)) { // Only valid value
            ccSetBuf.append(margn6 + element("mix:exifVersion", s) + EOL);
            useCcSetBuf = true;
        }
        Rational r = niso.getBrightness();
        if (r != null) {
            rationalToString(ccSetBuf, "mix:brightnessValue", margn6, r);
            useCcSetBuf = true;
        }
        r = niso.getExposureBias();
        if (r != null) {
            rationalToString(ccSetBuf, "mix:exposureBiasValue", margn6, r);
            useCcSetBuf = true;
        }
        r = niso.getMaxApertureValue();
        if (r != null) {
            rationalToString(ccSetBuf, "mix:maxApertureValue", margn6, r);
            useCcSetBuf = true;
        }
        double[] darray = niso.getSubjectDistance();
        if (darray != null) {
            // For the old schema, we dumped out the whole array, but the 1.0
            // schema clearly says a non-negative number is expected.
            // So just use darray[0].
            ccSetBuf.append(margn6
                    + element("mix:subjectDistance",
                            formatters.get().format(darray[0]))
                    + EOL);
            useCcSetBuf = true;
        }
        n = niso.getMeteringMode();
        if (n != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:meteringMode", meteringModeToString(n))
                    + EOL);
            useCcSetBuf = true;
        }
        n = niso.getFlash();
        if (n != NisoImageMetadata.NULL) {
            // First bit (0 = Flash did not fire, 1 = Flash fired)
            int firstBit = n & 1;
            ccSetBuf.append(margn6
                    + element("mix:flash", NisoImageMetadata.FLASH_20[firstBit])
                    + EOL);
            useCcSetBuf = true;
        }
        d = niso.getFocalLength();
        if (d != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:focalLength", formatters.get().format(d))
                    + EOL);
            useCcSetBuf = true;
        }
        r = niso.getFlashEnergy();
        if (r != null) {
            rationalToString(ccSetBuf, "mix:flashEnergy", margn6, r);
            useCcSetBuf = true;
        }
        n = niso.getBackLight();
        if (n != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:backLight", Integer.toString(n)) + EOL);
            useCcSetBuf = true;
        }
        d = niso.getExposureIndex();
        if (d != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:exposureIndex", formatters.get().format(d))
                    + EOL);
            useCcSetBuf = true;
        }
        n = niso.getAutoFocus();
        if (n != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:autoFocus", Integer.toString(n)) + EOL);
            useCcSetBuf = true;
        }
        d = niso.getXPrintAspectRatio();
        double d2 = niso.getYPrintAspectRatio();
        if (d != NisoImageMetadata.NULL || d2 != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6 + elementStart("mix:PrintAspectRatio") + EOL);
            if (d != NisoImageMetadata.NULL) {
                ccSetBuf.append(margn7
                        + element("mix:xPrintAspectRatio", formatters.get()
                                .format(d))
                        + EOL);
            }
            if (d2 != NisoImageMetadata.NULL) {
                ccSetBuf.append(margn7
                        + element("mix:yPrintAspectRatio", formatters.get()
                                .format(d))
                        + EOL);
            }

            ccSetBuf.append(margn6 + elementEnd("mix:PrintAspectRatio") + EOL);
        }

        ccSetBuf.append(margn5 + elementEnd("mix:ImageData") + EOL);
        ccSetBuf.append(margn4 + elementEnd("mix:CameraCaptureSettings") + EOL);
        if (useCcSetBuf) {
            digCamBuf.append(ccSetBuf);
            useDigCamBuf = true;
        }
        digCamBuf.append(margn3 + elementEnd("mix:DigitalCameraCapture") + EOL);
        if (useDigCamBuf) {
            captureBuffer.append(digCamBuf);
            useCaptureBuffer = true;
        }

        n = niso.getOrientation();
        if (n != NisoImageMetadata.NULL) {
            if (n > 9 || n < 1) {
                n = 9; // force "unknown" for reserved value
            }
            captureBuffer.append(margn3
                    + element("mix:orientation", Integer.toString(n)) + EOL);
            useCaptureBuffer = true;
        }
        s = niso.getMethodology();
        if (s != null) {
            captureBuffer.append(margn3 + element("mix:methodology", s) + EOL);
        }
        if (useCaptureBuffer) {
            _writer.println(margn2 + elementStart("mix:ImageCaptureMetadata"));
            _writer.print(captureBuffer.toString());
            _writer.println(margn2 + elementEnd("mix:ImageCaptureMetadata"));
        }
    }

    /* 1.0, Top level element 4 of 5: ImageAssessmentMetadata */
    protected void showNisoImageAssessmentMetadata10(NisoImageMetadata niso,
            String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";

        _writer.println(margn2 + elementStart("mix:ImageAssessmentMetadata"));
        StringBuffer metricsBuf = new StringBuffer(margn3
                + elementStart("mix:SpatialMetrics") + EOL);
        boolean useMetricsBuf = false;

        int n = niso.getSamplingFrequencyPlane();
        if (n != NisoImageMetadata.NULL) {
            metricsBuf
                    .append(margn4
                            + element("mix:samplingFrequencyPlane",
                                    Integer.toString(n))
                            + EOL);
            useMetricsBuf = true;
        }
        n = niso.getSamplingFrequencyUnit();
        if (n != NisoImageMetadata.NULL) {
            metricsBuf.append(margn4
                    + element("mix:samplingFrequencyUnit", Integer.toString(n))
                    + EOL);
            useMetricsBuf = true;
        }
        Rational r = niso.getXSamplingFrequency();
        if (r != null) {
            rationalToString(metricsBuf, "mix:xSamplingFrequency", margn4, r);
        }
        r = niso.getYSamplingFrequency();
        if (r != null) {
            rationalToString(metricsBuf, "mix:ySamplingFrequency", margn4, r);
        }
        metricsBuf.append(margn3 + elementEnd("mix:SpatialMetrics"));
        if (useMetricsBuf) {
            _writer.println(metricsBuf);
        }

        StringBuffer colorEncBuf = new StringBuffer(margn3
                + elementStart("mix:ImageColorEncoding") + EOL);
        boolean useColorEncBuf = false;

        int[] iarray = niso.getBitsPerSample();
        if (iarray != null) {
            colorEncBuf
                    .append(margn4 + elementStart("mix:bitsPerSample") + EOL);
            colorEncBuf.append(margn5
                    + element("mix:bitsPerSampleValue",
                            integerArray(iarray, ','))
                    + EOL);
            colorEncBuf.append(margn5
                    + element("mix:bitsPerSampleUnit", "integer") + EOL);
            // bitsPerSampleUnit can also be floating point. Don't ask me why.
            colorEncBuf.append(margn4 + elementEnd("mix:bitsPerSample") + EOL);
            useColorEncBuf = true;
        }
        n = niso.getSamplesPerPixel();
        if (n != NisoImageMetadata.NULL) {
            colorEncBuf
                    .append(margn4
                            + element("mix:samplesPerPixel",
                                    Integer.toString(n))
                            + EOL);
            useColorEncBuf = true;
        }

        iarray = niso.getExtraSamples();
        if (iarray != null) {
            // extraSamples can only be an integer, so the best we can do is
            // snag the first value from the array. It also must be limited to
            // 0, 1, 2, or 3.
            n = iarray[0];
            if (n >= 0 && n <= 3) {
                colorEncBuf.append(margn4
                        + element("mix:extraSamples", Integer.toString(n))
                        + EOL);
                useColorEncBuf = true;
            }
        }

        String s = niso.getColormapReference();
        if (s != null) {
            colorEncBuf.append(margn4 + elementStart("mix:Colormap") + EOL);
            colorEncBuf.append(margn5 + element("mix:colormapReference", s)
                    + EOL);
            colorEncBuf.append(margn4 + elementEnd("mix:Colormap") + EOL);
            useColorEncBuf = true;
        }

        // This is complete nonsense, but it's what the spec says
        iarray = niso.getGrayResponseCurve();
        if (iarray != null) {
            colorEncBuf.append(margn4 + element("mix:grayResponseCurve", "N")
                    + EOL);
            useColorEncBuf = true;
        }

        n = niso.getGrayResponseUnit();
        if (n != NisoImageMetadata.NULL) {
            colorEncBuf.append(margn4
                    + element("mix:grayResponseUnit", Integer.toString(n))
                    + EOL);
            useColorEncBuf = true;
        }

        r = niso.getWhitePointXValue();
        Rational r2 = niso.getWhitePointYValue();
        if (r != null || r2 != null) {
            colorEncBuf.append(margn4 + elementStart("mix:WhitePoint") + EOL);
            if (r != null) {
                colorEncBuf.append(margn5
                        + element("mix:whitePointXValue", r.toString()) + EOL);
            }
            if (r2 != null) {
                colorEncBuf.append(margn5
                        + element("mix:whitePointYValue", r2.toString()) + EOL);
            }
            colorEncBuf.append(margn4 + elementEnd("mix:WhitePoint") + EOL);
            useColorEncBuf = true;
        }

        // A chromaticities buffer to go in the color encoding buffer.
        StringBuffer chromaBuf = new StringBuffer(margn4
                + elementStart("mix:PrimaryChromaticities") + EOL);
        boolean useChromaBuf = false;
        r = niso.getPrimaryChromaticitiesRedX();
        if (r != null) {
            chromaBuf.append(margn5
                    + element("mix:primaryChromaticitiesRedX", r.toString())
                    + EOL);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesRedY();
        if (r != null) {
            chromaBuf.append(margn5
                    + element("mix:primaryChromaticitiesRedY", r.toString())
                    + EOL);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesGreenX();
        if (r != null) {
            chromaBuf.append(margn5
                    + element("mix:primaryChromaticitiesGreenX", r.toString())
                    + EOL);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesGreenY();
        if (r != null) {
            chromaBuf.append(margn5
                    + element("mix:primaryChromaticitiesGreenY", r.toString())
                    + EOL);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesBlueX();
        if (r != null) {
            chromaBuf.append(margn5
                    + element("mix:primaryChromaticitiesBlueX", r.toString())
                    + EOL);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesBlueY();
        if (r != null) {
            chromaBuf.append(margn5
                    + element("mix:primaryChromaticitiesBlueY", r.toString())
                    + EOL);
            useChromaBuf = true;
        }
        chromaBuf
                .append(margn4 + elementEnd("mix:PrimaryChromaticities") + EOL);
        if (useChromaBuf) {
            colorEncBuf.append(chromaBuf);
            useColorEncBuf = true;
        }

        colorEncBuf.append(margn3 + elementEnd("mix:ImageColorEncoding") + EOL);
        if (useColorEncBuf) {
            _writer.print(colorEncBuf);
        }

        StringBuffer targetBuf = new StringBuffer(margn3
                + elementStart("mix:TargetData") + EOL);
        boolean useTargetBuf = false;
        n = niso.getTargetType();
        if (n != NisoImageMetadata.NULL) {
            targetBuf.append(margn4
                    + element("mix:targetType", Integer.toString(n)) + EOL);
            useTargetBuf = true;
        }

        // Now a nested buffer for TargetID.
        StringBuffer targetIDBuf = new StringBuffer(margn4
                + elementStart("mix:TargetID") + EOL);
        boolean useTargetIDBuf = false;

        s = niso.getTargetIDManufacturer();
        if (s != null) {
            targetIDBuf.append(margn5 + element("mix:targetManufacturer", s)
                    + EOL);
            useTargetIDBuf = true;
        }
        s = niso.getTargetIDName();
        if (s != null) {
            targetIDBuf.append(margn5 + element("mix:targetName", s) + EOL);
            useTargetIDBuf = true;
        }
        s = niso.getTargetIDNo();
        if (s != null) {
            targetIDBuf.append(margn5 + element("mix:targetNo", s) + EOL);
            useTargetIDBuf = true;
        }
        s = niso.getTargetIDMedia();
        if (s != null) {
            targetIDBuf.append(margn5 + element("mix:targetMedia", s) + EOL);
            useTargetIDBuf = true;
        }
        targetIDBuf.append(margn4 + elementEnd("mix:TargetID") + EOL);

        if (useTargetIDBuf) {
            targetBuf.append(targetIDBuf);
            useTargetBuf = true;
        }
        s = niso.getImageData();
        if (s != null) {
            targetBuf.append(margn4 + element("mix:externalTarget", s) + EOL);
            useTargetBuf = true;
        }
        s = niso.getPerformanceData();
        if (s != null) {
            targetBuf.append(margn4 + element("mix:performanceData", s) + EOL);
            useTargetBuf = true;
        }

        targetBuf.append(margn3 + elementEnd("mix:TargetData") + EOL);

        if (useTargetBuf) {
            _writer.print(targetBuf);
        }
        _writer.println(margn2 + elementEnd("mix:ImageAssessmentMetadata"));
    }

    /* 1.0, Top level element 5 of 5: ChangeHistory (without time travel) */
    protected void showChangeHistory10(NisoImageMetadata niso, String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";

        // There may be nothing at all to write. Put the whole thing in a
        // buffer.
        StringBuffer chBuf = new StringBuffer(margn2
                + elementStart("mix:ChangeHistory") + EOL);
        boolean useChBuf = false;

        chBuf.append(margn3 + elementStart("mix:ImageProcessing") + EOL);

        String s = niso.getSourceData();
        if (s != null) {
            chBuf.append(margn4 + element("mix:sourceData", s) + EOL);
            useChBuf = true;
        }
        s = niso.getProcessingAgency();
        if (s != null) {
            chBuf.append(margn4 + element("mix:processingAgency", s) + EOL);
            useChBuf = true;
        }
        StringBuffer sftwBuf = new StringBuffer(margn4
                + elementStart("mix:ProcessingSoftware") + EOL);
        boolean useSftwBuf = false;
        s = niso.getProcessingSoftwareName();
        if (s != null) {
            sftwBuf.append(margn5 + element("mix:processingSoftwareName", s)
                    + EOL);
            useSftwBuf = true;
        }
        s = niso.getProcessingSoftwareVersion();
        if (s != null) {
            sftwBuf.append(margn5 + element("mix:processingSoftwareVersion", s)
                    + EOL);
            useSftwBuf = true;
        }
        s = niso.getOS();
        if (s != null) {
            sftwBuf.append(margn5
                    + element("mix:processingOperatingSystemName", s) + EOL);
            useSftwBuf = true;
        }
        s = niso.getOSVersion();
        if (s != null) {
            sftwBuf.append(margn5
                    + element("mix:processingOperatingSystemVersion", s) + EOL);
            useSftwBuf = true;
        }
        sftwBuf.append(margn4 + elementEnd("mix:ProcessingSoftware") + EOL);
        if (useSftwBuf) {
            chBuf.append(sftwBuf);
            useChBuf = true;
        }

        String[] sarray = niso.getProcessingActions();
        if (sarray != null) {
            for (int i = 0; i < sarray.length; i++) {
                chBuf.append(margn4
                        + element("mix:processingActions", sarray[i]) + EOL);
            }
            useChBuf = true;
        }

        chBuf.append(margn3 + elementEnd("mix:ImageProcessing") + EOL);
        chBuf.append(margn2 + elementEnd("mix:ChangeHistory") + EOL);
        if (useChBuf) {
            _writer.println(chBuf);
        }

    }

    /**
     * Display the NISO image metadata formatted according to the MIX 2.0
     * schema.
     */
    protected void showNisoImageMetadata20(NisoImageMetadata niso) {
        String margin = getIndent(++_level);

        String[][] attrs = {
                { "xmlns:mix", "http://www.loc.gov/mix/v20" },
                { "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" },
                { "xsi:schemaLocation",
                        "http://www.loc.gov/mix/v20 http://www.loc.gov/standards/mix/mix20/mix20.xsd" } };
        _writer.println(margin + elementStart("mix:mix", attrs));

        showNisoBasicDigitalObjectInformation20(niso, margin);
        showNisoBasicImageInformation20(niso, margin);
        showNisoImageCaptureMetadata20(niso, margin);
        showNisoImageAssessmentMetadata20(niso, margin);
        showChangeHistory20(niso, margin);

        _writer.println(margin + elementEnd("mix:mix"));

        _level--;

    }

    /*
     * The NISO Metadata output for version 2.0. Top level element 1 of 6:
     * BasicDigitalObjectInformation
     */
    protected void showNisoBasicDigitalObjectInformation20(
            NisoImageMetadata niso, String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";
        String margn6 = margn5 + " ";

        _writer.println(margn2
                + elementStart("mix:BasicDigitalObjectInformation"));

        StringBuffer objIDBuf = new StringBuffer(margn3
                + elementStart("mix:ObjectIdentifier") + EOL);
        objIDBuf.append(margn4 + element("mix:objectIdentifierType", "JHOVE")
                + EOL);
        String s = niso.getImageIdentifier();
        if (s != null) {
            objIDBuf.append(margn4 + element("mix:objectIdentifierValue", s)
                    + EOL);
        }
        objIDBuf.append(margn3 + elementEnd("mix:ObjectIdentifier") + EOL);
        _writer.print(objIDBuf.toString());
        long ln = niso.getFileSize();
        if (ln != NisoImageMetadata.NULL) {
            _writer.print(margn3 + element("mix:fileSize", Long.toString(ln))
                    + EOL);
        }

        String mime = niso.getMimeType();
        if (mime != null) {
            _writer.println(margn3 + elementStart("mix:FormatDesignation"));
            _writer.print(margn4 + element("mix:formatName", mime) + EOL);
            _writer.println(margn3 + elementEnd("mix:FormatDesignation"));
        }

        if ((s = niso.getByteOrder()) != null) {
            // Convert strings to MIX 1.0 form
            if (s.startsWith("big")) {
                s = "big endian";
            } else if (s.startsWith("little")) {
                s = "little endian";
            }
            _writer.print(margn3 + element("mix:byteOrder", s) + EOL);
        }

        int comp = niso.getCompressionScheme();
        int level = niso.getCompressionLevel();
        String compStr;
        switch (comp) {
            case 1:
                compStr = "Uncompressed";
                break;
            case 2:
                compStr = "CCITT 1D";
                break;
            case 3:
                compStr = "Group 3 Fax";
                break;
            case 4:
                compStr = "Group 4 Fax";
                break;
            case 5:
                compStr = "LZW";
                break;
            case 6:
                compStr = "JPEG";
                break;
            case 32773:
                compStr = "PackBits";
                break;
            case 34713:
                compStr = "JPEG2000 Lossy";
                break;
            case 34714:
                compStr = "JPEG2000 Lossless";
                break;
            default:
                compStr = "Unknown";
                break;
        }
        if (comp != NisoImageMetadata.NULL || level != NisoImageMetadata.NULL) {
            _writer.print(margn3 + elementStart("mix:Compression") + EOL);
            if (comp != NisoImageMetadata.NULL) {
                _writer.print(margn4
                        + element("mix:compressionScheme", compStr) + EOL);
            }
            // TODO it isn't clear how to get from compression level to
            // compression ratio
            if (level != NisoImageMetadata.NULL
                    && (comp == 34713 || comp == 34714)) {
                _writer.print(margn5 + elementStart("mix:compressionRatio")
                        + EOL);
                _writer.print(margn6
                        + element("mix:numerator", Integer.toString(level))
                        + EOL);
                _writer.print(margn5 + elementEnd("mix:compressionRatio") + EOL);
            }

            _writer.print(margn3 + elementEnd("mix:Compression") + EOL);
        }

        // NOTE: Checksum method and value are never set currently. If they are,
        // the
        // values set will need to be converted to meaningful MIX values. This
        // code is left
        // here just as a reminder.
        int n = niso.getChecksumMethod();
        s = niso.getChecksumValue();
        if (n != NisoImageMetadata.NULL || s != null) {
            _writer.print(margn4 + elementStart("mix:Fixity") + EOL);
            if (n != NisoImageMetadata.NULL) {
                _writer.print(margn5
                        + element("mix:messageDigestAlgorithm",
                                Integer.toString(n))
                        + EOL);
            }
            if (s != null) {
                _writer.print(margn5 + element("mix:messageDigest", s) + EOL);
            }
            _writer.println(margn4 + elementEnd("mix:Fixity"));
        }

        _writer.print(margn2 + elementEnd("mix:BasicDigitalObjectInformation")
                + EOL);
    }

    /* MIX/NISO 2.0, Top level element 2 of 5: BasicImageInformation */
    protected void showNisoBasicImageInformation20(NisoImageMetadata niso,
            String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";
        String margn6 = margn5 + " ";
        String margn7 = margn6 + " ";
        _writer.println(margn2 + elementStart("mix:BasicImageInformation"));
        StringBuffer basCharBuf = new StringBuffer(margn3
                + elementStart("mix:BasicImageCharacteristics") + EOL);
        boolean useBasCharBuf = false;
        long ln = niso.getImageWidth();
        if (ln != NisoImageMetadata.NULL) {
            basCharBuf.append(margn4
                    + element("mix:imageWidth", Long.toString(ln)) + EOL);
            useBasCharBuf = true;
        }
        ln = niso.getImageLength();
        if (ln != NisoImageMetadata.NULL) {
            basCharBuf.append(margn4
                    + element("mix:imageHeight", Long.toString(ln)) + EOL);
            useBasCharBuf = true;
        }
        // Nest photometric interpretation tentative buffer in basCharBuf
        StringBuffer piBuf = new StringBuffer(margn4
                + elementStart("mix:PhotometricInterpretation") + EOL);
        boolean usePIBuf = false;
        int n = niso.getColorSpace();
        if (n != NisoImageMetadata.NULL) {
            piBuf.append(margn5
                    + element("mix:colorSpace",
                            photometricInterpretationToString(n))
                    + EOL);
            usePIBuf = true;
        }
        String s = niso.getProfileName();
        String s2 = niso.getProfileURL();
        if (s != null || s2 != null) {
            piBuf.append(margn5 + elementStart("mix:ColorProfile") + EOL);
            piBuf.append(margn6 + elementStart("mix:IccProfile") + EOL);
            if (s != null) {
                piBuf.append(margn7 + element("mix:iccProfileName", s) + EOL);
            }
            if (s2 != null) {
                piBuf.append(margn7 + element("mix:iccProfileURI", s2) + EOL);
            }
            piBuf.append(margn6 + elementEnd("mix:IccProfile") + EOL);
            // MIX 2.0 also allows embedded and local profiles. We don't
            // currently support that.
            piBuf.append(margn5 + elementEnd("mix:ColorProfile") + EOL);
            usePIBuf = true;
        }
        int[] iarray = niso.getYCbCrSubSampling();
        n = niso.getYCbCrPositioning();
        Rational[] rarray = niso.getYCbCrCoefficients();
        if (iarray != null || n != NisoImageMetadata.NULL || rarray != null) {
            piBuf.append(margn5 + elementStart("mix:YCbCr") + EOL);
            usePIBuf = true;
            if (iarray != null && iarray.length >= 2) {
                piBuf.append(margn6 + elementStart("mix:YCbCrSubSampling")
                        + EOL);
                piBuf.append(margn7
                        + element("mix:yCbCrSubsampleHoriz",
                                Integer.toString(iarray[0]))
                        + EOL);
                piBuf.append(margn7
                        + element("mix:yCbCrSubsampleVert",
                                Integer.toString(iarray[1]))
                        + EOL);
                piBuf.append(margn6 + elementEnd("mix:YCbCrSubSampling") + EOL);
            }
            if (n != NisoImageMetadata.NULL) {
                piBuf.append(margn6
                        + element("mix:yCbCrPositioning", Integer.toString(n))
                        + EOL);
            }
            if (rarray != null && rarray.length >= 3) {
                piBuf.append(margn6 + elementStart("mix:YCbCrCoefficients")
                        + EOL);
                rationalToString(piBuf, "mix:lumaRed", margn7, rarray[0]);
                rationalToString(piBuf, "mix:lumaGreen", margn7, rarray[1]);
                rationalToString(piBuf, "mix:lumaBlue", margn7, rarray[2]);
                piBuf.append(margn6 + elementEnd("mix:YCbCrCoefficients") + EOL);
            }
            piBuf.append(margn5 + elementEnd("mix:YCbCr") + EOL);
        }

        rarray = niso.getReferenceBlackWhite();
        if (rarray != null) {
            piBuf.append(margn5 + elementStart("mix:ReferenceBlackWhite") + EOL);
            for (int i = 0; i < rarray.length - 1; i += 2) {
                piBuf.append(margn6 + elementStart("mix:Component") + EOL);
                piBuf.append(margn7
                        + elementStart("mix:componentPhotometricInterpretation"));
                // Tricky here. The reference BW might be given as either RGB or
                // yCbCr.
                String pi;
                if (niso.getColorSpace() == 6) { // yCbCr
                    switch (i) {
                        case 0:
                            pi = "Y";
                            break;
                        case 2:
                            pi = "Cb";
                            break;
                        case 4:
                        default:
                            pi = "Cr";
                            break;
                    }
                } else {
                    switch (i) { // otherwise assume RGB
                        case 0:
                            pi = "R";
                            break;
                        case 2:
                            pi = "G";
                            break;
                        case 4:
                        default:
                            pi = "B";
                            break;
                    }

                }
                piBuf.append(pi
                        + elementEnd("mix:componentPhotometricInterpretation")
                        + EOL);
                rationalToString(piBuf, "mix:footroom", margn7, rarray[i]);
                rationalToString(piBuf, "mix:headroom", margn7, rarray[i + 1]);
                piBuf.append(margn7 + elementEnd("mix:Component") + EOL);
            }
            piBuf.append(margn6 + elementEnd("mix:ReferenceBlackWhite") + EOL);
            usePIBuf = true;
        }
        piBuf.append(margn4 + elementEnd("mix:PhotometricInterpretation") + EOL);
        if (usePIBuf) {
            basCharBuf.append(piBuf);
            useBasCharBuf = true;
        }
        basCharBuf.append(margn3 + elementEnd("mix:BasicImageCharacteristics"));

        if (useBasCharBuf) {
            _writer.println(basCharBuf);
        }

        // TODO SpecialFormatCharacteristics would be nice to have here,
        // Limited to JPEG2000
        StringBuffer speCharBuf = new StringBuffer(margn3
                + elementStart("mix:SpecialFormatCharacteristics") + EOL);
        boolean useSpeCharBuf = false;
        int lay = niso.getJp2Layers();
        int lev = niso.getJp2ResolutionLevels();
        String sizTiles = niso.getJp2Tiles();
        if (sizTiles != null || lay != NisoImageMetadata.NULL
                || lev != NisoImageMetadata.NULL) {

            useSpeCharBuf = true;
            speCharBuf.append(margn4 + elementStart("mix:JPEG2000") + EOL);
            speCharBuf.append(margn5 + elementStart("mix:EncodingOptions")
                    + EOL);
            if (sizTiles != null) {
                String[] sizes = sizTiles.split("x");
                speCharBuf.append(margn6 + elementStart("mix:Tiles") + EOL);
                speCharBuf.append(margn7 + element("mix:tileWidth", sizes[0])
                        + EOL);
                speCharBuf.append(margn7 + element("mix:tileHeight", sizes[1])
                        + EOL);
                speCharBuf.append(margn6 + elementEnd("mix:Tiles") + EOL);
            }
            if (lay != NisoImageMetadata.NULL) {
                speCharBuf.append(margn6
                        + element("mix:qualityLayers", Integer.toString(lay))
                        + EOL);
            }
            if (sizTiles != null) {
                speCharBuf
                        .append(margn6
                                + element("mix:resolutionLevels",
                                        Integer.toString(lev))
                                + EOL);
            }
            speCharBuf.append(margn5 + elementEnd("mix:EncodingOptions") + EOL);
            speCharBuf.append(margn4 + elementEnd("mix:JPEG2000") + EOL);
        }

        speCharBuf.append(margn3
                + elementEnd("mix:SpecialFormatCharacteristics"));
        if (useSpeCharBuf) {
            _writer.println(speCharBuf);
        }

        _writer.println(margn2 + elementEnd("mix:BasicImageInformation"));
    }

    /* 2.0, Top level element 3 of 5: ImageCaptureMetadata */
    protected void showNisoImageCaptureMetadata20(NisoImageMetadata niso,
            String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";
        String margn6 = margn5 + " ";
        String margn7 = margn6 + " ";
        String margn8 = margn7 + " ";

        // We don't start with an ImageCaptureMetadata element, because the
        // whole element is conditional on having some content.
        StringBuffer captureBuffer = new StringBuffer();
        boolean useCaptureBuffer = false;
        int n;

        String s = niso.getSourceType();
        String si = niso.getSourceID();
        double d = niso.getSourceXDimension();
        if (s != null || si != null || d != NisoImageMetadata.NILL) {
            captureBuffer.append(margn3 + element("mix:SourceInformation", s));
            useCaptureBuffer = true;
            if (s != null) {
                captureBuffer.append(margn4 + element("mix:sourceType", s));
            }
            if (si != null) {
                captureBuffer.append(margn4 + elementStart("mix:SourceID"));
                captureBuffer.append(margn4 + element("mix:sourceIDValue", si));
                captureBuffer.append(margn4 + elementEnd("mix:sourceID"));
            }
            n = niso.getSourceXDimensionUnit();
            if (d != NisoImageMetadata.NILL || n != NisoImageMetadata.NULL) {
                // Assume that both X and Y exist, or neither
                captureBuffer.append(margn4 + elementStart("mix:SourceSize"));
                captureBuffer.append(margn5
                        + elementStart("mix:SourceXDimension") + EOL);
                if (d != NisoImageMetadata.NILL) {
                    captureBuffer.append(margn6
                            + element("mix:sourceXDimensionValue", formatters
                                    .get().format(d))
                            + EOL);
                }
                if (n != NisoImageMetadata.NULL) {
                    captureBuffer.append(margn6
                            + element("mix:sourceXDimensionUnit",
                                    Integer.toString(n))
                            + EOL);
                }
                captureBuffer.append(margn5
                        + elementEnd("mix:SourceXDimension") + EOL);

                d = niso.getSourceYDimension();
                n = niso.getSourceYDimensionUnit();
                if (d != NisoImageMetadata.NILL || n != NisoImageMetadata.NULL) {
                    captureBuffer.append(margn5
                            + elementStart("mix:SourceYDimension") + EOL);
                    if (d != NisoImageMetadata.NILL) {
                        captureBuffer.append(margn6
                                + element("mix:sourceYDimensionValue",
                                        formatters.get().format(d))
                                + EOL);
                    }
                    if (n != NisoImageMetadata.NULL) {
                        captureBuffer.append(margn6
                                + element("mix:sourceYDimensionUnit",
                                        Integer.toString(n))
                                + EOL);
                    }
                    captureBuffer.append(margn5
                            + elementEnd("mix:SourceYDimension") + EOL);
                }
                captureBuffer.append(margn4 + elementEnd("mix:SourceSize")
                        + EOL);
            }
            captureBuffer.append(margn3 + elementEnd("mix:SourceInformation")
                    + EOL);
        }
        StringBuffer genCapBuf = new StringBuffer(margn3
                + elementStart("mix:GeneralCaptureInformation") + EOL);
        boolean useGenCapBuf = false;

        s = niso.getDateTimeCreated();
        if (s != null) {
            genCapBuf.append(margn3 + element("mix:dateTimeCreated", s) + EOL);
            useGenCapBuf = true;
        }
        s = niso.getImageProducer();
        if (s != null) {
            genCapBuf.append(margn3 + element("mix:imageProducer", s) + EOL);
            useGenCapBuf = true;
        }

        s = niso.getDeviceSource();
        if (s != null) {
            genCapBuf.append(margn3 + element("mix:captureDevice", s) + EOL);
            /*
             * This has a restricted set of values. Does the setting code
             * conform?
             */
        }

        genCapBuf.append(margn3 + elementEnd("mix:GeneralCaptureInformation")
                + EOL);
        if (useGenCapBuf) {
            captureBuffer.append(genCapBuf);
            useCaptureBuffer = true;
        }

        // Here's a chunk of XML for scanners.
        StringBuffer scanCapBuf = new StringBuffer(margn3
                + elementStart("mix:ScannerCapture") + EOL);
        boolean useScanCapBuf = false;
        String mfg = niso.getScannerManufacturer();
        if (mfg != null) {
            scanCapBuf.append(margn4 + element("mix:scannerManufacturer", mfg)
                    + EOL);
            useScanCapBuf = true;
        }
        String model = niso.getScannerModelName();
        String modelNum = niso.getScannerModelNumber();
        String serNum = niso.getScannerModelSerialNo();
        if (model != null || modelNum != null || serNum != null) {
            useScanCapBuf = true;
            scanCapBuf.append(margn4 + elementStart("mix:ScannerModel") + EOL);
            if (model != null) {
                scanCapBuf.append(margn5
                        + element("mix:scannerModelName", model) + EOL);
            }
            if (modelNum != null) {
                scanCapBuf.append(margn5
                        + element("mix:scannerModelNumber", modelNum) + EOL);
            }
            if (serNum != null) {
                scanCapBuf.append(margn5
                        + element("mix:scannerModelSerialNo", serNum) + EOL);
            }
            scanCapBuf.append(margn4 + elementEnd("mix:ScannerModel") + EOL);
        }
        double xres = niso.getXPhysScanResolution();
        double yres = niso.getYPhysScanResolution();
        if (xres != NisoImageMetadata.NULL && yres != NisoImageMetadata.NULL) {
            scanCapBuf.append(margn4
                    + elementStart("mix:MaximumOpticalResolution") + EOL);
            scanCapBuf.append(margn5
                    + element("mix:xOpticalResolution", formatters.get()
                            .format(xres))
                    + EOL);
            scanCapBuf.append(margn5
                    + element("mix:yOpticalResolution", formatters.get()
                            .format(yres))
                    + EOL);
            scanCapBuf.append(margn5 + element("mix:resolutionUnit", "in.")
                    + EOL); // is this a safe assumption?
            scanCapBuf.append(margn4
                    + elementEnd("mix:MaximumOpticalResolution"));
        }
        s = niso.getScanningSoftware();
        if (s != null) {
            useScanCapBuf = true;
            scanCapBuf.append(margn4
                    + elementStart("mix:ScanningSystemSoftware") + EOL);
            scanCapBuf.append(margn5 + element("mix:scanningSoftwareName", s)
                    + EOL);
            s = niso.getScanningSoftwareVersionNo();
            if (s != null) {
                scanCapBuf.append(margn5
                        + element("mix:scanningSoftwareVersionNo", s) + EOL);
            }
            scanCapBuf.append(margn4 + elementEnd("mix:ScanningSystemSoftware")
                    + EOL);
        }
        scanCapBuf.append(margn3 + elementEnd("mix:ScannerCapture") + EOL);
        if (useScanCapBuf) {
            captureBuffer.append(scanCapBuf);
            useCaptureBuffer = true;
        }

        // Now we'll hear from the digital cameras.
        StringBuffer digCamBuf = new StringBuffer(margn3
                + elementStart("mix:DigitalCameraCapture") + EOL);
        boolean useDigCamBuf = false;

        s = niso.getDigitalCameraManufacturer();
        if (s != null) {
            digCamBuf.append(margn4
                    + element("mix:digitalCameraManufacturer", s) + EOL);
            useDigCamBuf = true;
        }
        String dcmodel = niso.getDigitalCameraModelName();
        String dcmodelNum = niso.getDigitalCameraModelNumber();
        String dcserNum = niso.getDigitalCameraModelSerialNo();
        if (dcmodel != null || dcmodelNum != null || dcserNum != null) {
            useDigCamBuf = true;
            digCamBuf.append(margn4 + elementStart("mix:DigitalCameraModel")
                    + EOL);
            if (dcmodel != null) {
                digCamBuf.append(margn5
                        + element("mix:digitalCameraModelName", dcmodel) + EOL);
            }
            if (dcmodelNum != null) {
                digCamBuf.append(margn5
                        + element("mix:digitalCameraModelNumber", dcmodelNum)
                        + EOL);
            }
            if (dcserNum != null) {
                digCamBuf.append(margn5
                        + element("mix:mix:digitalCameraModelSerialNo",
                                dcserNum)
                        + EOL);
            }
            digCamBuf.append(margn4 + elementEnd("mix:DigitalCameraModel")
                    + EOL);
        }

        // Nest a buffer for CameraCaptureSettings
        StringBuffer ccSetBuf = new StringBuffer(margn4
                + elementStart("mix:CameraCaptureSettings") + EOL);
        boolean useCcSetBuf = false;
        // CameraCaptureSettings consists only of an ImageData element, so we
        // don't need another use flag here.
        ccSetBuf.append(margn5 + elementStart("mix:ImageData") + EOL);
        d = niso.getFNumber();
        if (d != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:fNumber", formatters.get().format(d)) + EOL);
            useCcSetBuf = true;
        }
        d = niso.getExposureTime();
        if (d != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:exposureTime", formatters.get().format(d))
                    + EOL);
            useCcSetBuf = true;
        }
        n = niso.getExposureProgram();
        if (n != NisoImageMetadata.NULL) {
            if (n > 8 || n < 0) {
                n = 0; // force "Not defined" for bad value
            }

            ccSetBuf.append(margn6
                    + element("mix:exposureProgram",
                            NisoImageMetadata.EXPOSURE_PROGRAM[n])
                    + EOL);
            useCcSetBuf = true;
        }
        if (niso.getExifVersion() != null) {
            ccSetBuf.append(margn6
                    + element("mix:exifVersion", niso.getExifVersion()) + EOL);
            useCcSetBuf = true;
        }
        Rational r = niso.getBrightness();
        if (r != null) {
            rationalToString(ccSetBuf, "mix:brightnessValue", margn6, r);
            useCcSetBuf = true;
        }
        r = niso.getExposureBias();
        if (r != null) {
            rationalToString(ccSetBuf, "mix:exposureBiasValue", margn6, r);
            useCcSetBuf = true;
        }
        r = niso.getMaxApertureValue();
        if (r != null) {
            rationalToString(ccSetBuf, "mix:maxApertureValue", margn6, r);
            useCcSetBuf = true;
        }
        double[] darray = niso.getSubjectDistance();
        if (darray != null) {
            // darray has two values. If they're equal, set "distance".
            // Otherwise,
            // set the min and max.
            ccSetBuf.append(margn6 + elementStart("mix:SubjectDistance") + EOL);
            useCcSetBuf = true;
            if (darray[0] == darray[1]) {
                ccSetBuf.append(margn7
                        + element("mix:distance",
                                formatters.get().format(darray[0]))
                        + EOL);
            } else {
                ccSetBuf.append(margn7 + elementStart("mix:MinMaxDistance")
                        + EOL);
                ccSetBuf.append(margn8
                        + element("mix:minDistance",
                                formatters.get().format(darray[0]))
                        + EOL);
                ccSetBuf.append(margn8
                        + element("mix:maxDistance",
                                formatters.get().format(darray[1]))
                        + EOL);
                ccSetBuf.append(margn7 + elementEnd("mix:MinMaxDistance") + EOL);
            }
            ccSetBuf.append(margn6 + elementEnd("mix:SubjectDistance") + EOL);

        }
        n = niso.getMeteringMode();
        if (n != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:meteringMode", meteringModeToString(n))
                    + EOL);
            useCcSetBuf = true;
        }
        n = niso.getFlash();
        if (n != NisoImageMetadata.NULL) {
            // First bit (0 = Flash did not fire, 1 = Flash fired)
            int firstBit = n & 1;
            ccSetBuf.append(margn6
                    + element("mix:flash", NisoImageMetadata.FLASH_20[firstBit])
                    + EOL);
            useCcSetBuf = true;
        }
        d = niso.getFocalLength();
        if (d != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:focalLength", formatters.get().format(d))
                    + EOL);
            useCcSetBuf = true;
        }
        r = niso.getFlashEnergy();
        if (r != null) {
            rationalToString(ccSetBuf, "mix:flashEnergy", margn6, r);
            useCcSetBuf = true;
        }
        n = niso.getBackLight();
        if (n != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:backLight", Integer.toString(n)) + EOL);
            useCcSetBuf = true;
        }
        d = niso.getExposureIndex();
        if (d != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:exposureIndex", formatters.get().format(d))
                    + EOL);
            useCcSetBuf = true;
        }
        n = niso.getAutoFocus();
        if (n != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6
                    + element("mix:autoFocus", Integer.toString(n)) + EOL);
            useCcSetBuf = true;
        }
        d = niso.getXPrintAspectRatio();
        double d2 = niso.getYPrintAspectRatio();
        if (d != NisoImageMetadata.NULL || d2 != NisoImageMetadata.NULL) {
            ccSetBuf.append(margn6 + elementStart("mix:PrintAspectRatio") + EOL);
            if (d != NisoImageMetadata.NULL) {
                ccSetBuf.append(margn7
                        + element("mix:xPrintAspectRatio", formatters.get()
                                .format(d))
                        + EOL);
            }
            if (d2 != NisoImageMetadata.NULL) {
                ccSetBuf.append(margn7
                        + element("mix:yPrintAspectRatio", formatters.get()
                                .format(d2))
                        + EOL);
            }

            ccSetBuf.append(margn6 + elementEnd("mix:PrintAspectRatio") + EOL);
        }

        ccSetBuf.append(margn5 + elementEnd("mix:ImageData") + EOL);
        ccSetBuf.append(margn4 + elementEnd("mix:CameraCaptureSettings") + EOL);
        if (useCcSetBuf) {
            digCamBuf.append(ccSetBuf);
            useDigCamBuf = true;
        }
        digCamBuf.append(margn3 + elementEnd("mix:DigitalCameraCapture") + EOL);
        if (useDigCamBuf) {
            captureBuffer.append(digCamBuf);
            useCaptureBuffer = true;
        }

        n = niso.getOrientation();
        if (n != NisoImageMetadata.NULL) {
            // Values defined in the MIX 2.0 schema
            final String[] orient = { "", "normal*",
                    "normal, image flipped", "normal, rotated 180\u00B0",
                    "normal, image flipped, rotated 180\u00B0",
                    "normal, image flipped, rotated cw 90\u00B0",
                    "normal, rotated ccw 90\u00B0",
                    "normal, image flipped, rotated ccw 90\u00B0",
                    "normal, rotated cw 90\u00B0", "unknown" };
            if (n > 9 || n < 1) {
                n = 9; // force "unknown" for reserved value
            }
            captureBuffer.append(margn3 + element("mix:orientation", orient[n])
                    + EOL);
            useCaptureBuffer = true;
        }
        s = niso.getMethodology();
        if (s != null) {
            captureBuffer.append(margn3 + element("mix:methodology", s) + EOL);
        }
        if (useCaptureBuffer) {
            _writer.println(margn2 + elementStart("mix:ImageCaptureMetadata"));
            _writer.print(captureBuffer.toString());
            _writer.println(margn2 + elementEnd("mix:ImageCaptureMetadata"));
        }
    }

    /* 2.0, Top level element 4 of 5: ImageAssessmentMetadata */
    protected void showNisoImageAssessmentMetadata20(NisoImageMetadata niso,
            String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";

        _writer.println(margn2 + elementStart("mix:ImageAssessmentMetadata"));
        StringBuffer metricsBuf = new StringBuffer(margn3
                + elementStart("mix:SpatialMetrics") + EOL);
        boolean useMetricsBuf = false;

        int n = niso.getSamplingFrequencyPlane();
        if (n != NisoImageMetadata.NULL) {
            metricsBuf
                    .append(margn4
                            + element("mix:samplingFrequencyPlane",
                                    Integer.toString(n))
                            + EOL);
            useMetricsBuf = true;
        }
        n = niso.getSamplingFrequencyUnit();
        if (n != NisoImageMetadata.NULL) {
            final String sfu[] = { null, "no absolute unit of measurement",
                    "in.", "cm" };
            if (n < 1 || n > 3) {
                n = 1;
            }
            metricsBuf.append(margn4
                    + element("mix:samplingFrequencyUnit", sfu[n]) + EOL);
            useMetricsBuf = true;
        }
        Rational r = niso.getXSamplingFrequency();
        if (r != null) {
            rationalToString(metricsBuf, "mix:xSamplingFrequency", margn4, r);
        }
        r = niso.getYSamplingFrequency();
        if (r != null) {
            rationalToString(metricsBuf, "mix:ySamplingFrequency", margn4, r);
        }
        metricsBuf.append(margn3 + elementEnd("mix:SpatialMetrics"));
        if (useMetricsBuf) {
            _writer.println(metricsBuf);
        }

        StringBuffer colorEncBuf = new StringBuffer(margn3
                + elementStart("mix:ImageColorEncoding") + EOL);
        boolean useColorEncBuf = false;

        int[] iarray = niso.getBitsPerSample();
        if (iarray != null) {
            colorEncBuf
                    .append(margn4 + elementStart("mix:BitsPerSample") + EOL);
            for (int ii = 0; ii < iarray.length; ii++) {
                colorEncBuf.append(margn5
                        + element("mix:bitsPerSampleValue",
                                Integer.toString(iarray[ii]))
                        + EOL);
            }
            colorEncBuf.append(margn5
                    + element("mix:bitsPerSampleUnit", "integer") + EOL);
            // bitsPerSampleUnit can also be floating point. Don't ask me why.
            colorEncBuf.append(margn4 + elementEnd("mix:BitsPerSample") + EOL);
            useColorEncBuf = true;
        }
        n = niso.getSamplesPerPixel();
        if (n != NisoImageMetadata.NULL) {
            colorEncBuf
                    .append(margn4
                            + element("mix:samplesPerPixel",
                                    Integer.toString(n))
                            + EOL);
            useColorEncBuf = true;
        }

        iarray = niso.getExtraSamples();
        if (iarray != null) {
            for (int ii = 0; ii < iarray.length; ii++) {
                n = iarray[ii];
                if (n >= 0 && n <= 3) {
                    colorEncBuf
                            .append(margn4
                                    + element(
                                            "mix:extraSamples",
                                            NisoImageMetadata.EXTRA_SAMPLE_20[n])
                                    + EOL);
                    useColorEncBuf = true;
                }
            }
        }

        String s = niso.getColormapReference();
        if (s != null) {
            colorEncBuf.append(margn4 + elementStart("mix:Colormap") + EOL);
            colorEncBuf.append(margn5 + element("mix:colormapReference", s)
                    + EOL);
            colorEncBuf.append(margn4 + elementEnd("mix:Colormap") + EOL);
            useColorEncBuf = true;
        }

        iarray = niso.getGrayResponseCurve();
        n = niso.getGrayResponseUnit();
        if (iarray != null || n != NisoImageMetadata.NULL) {
            StringBuffer grayRespBuf = new StringBuffer(margn4
                    + elementStart("mix:GrayResponse") + EOL);
            if (iarray != null) {
                for (int ii = 0; ii < iarray.length; ii++) {
                    grayRespBuf.append(margn5
                            + element("mix:grayResponseCurve",
                                    Integer.toString(iarray[ii]))
                            + EOL);
                }
            }

            if (n != NisoImageMetadata.NULL && n > 0 && n <= 5) {
                // Convert integer to text value; only values 1-5 are legal
                grayRespBuf.append(margn5
                        + element("mix:grayResponseUnit",
                                NisoImageMetadata.GRAY_RESPONSE_UNIT_20[n - 1])
                        + EOL);
            }
            grayRespBuf.append(margn4 + elementEnd("mix:GrayResponse") + EOL);
            colorEncBuf.append(grayRespBuf);
            useColorEncBuf = true;
        }

        r = niso.getWhitePointXValue();
        Rational r2 = niso.getWhitePointYValue();
        if (r != null || r2 != null) {
            colorEncBuf.append(margn4 + elementStart("mix:WhitePoint") + EOL);
            if (r != null) {
                rationalToString(colorEncBuf, "mix:whitePointXValue", margn5, r);
            }
            if (r2 != null) {
                rationalToString(colorEncBuf, "mix:whitePointYValue", margn5,
                        r2);
            }
            colorEncBuf.append(margn4 + elementEnd("mix:WhitePoint") + EOL);
            useColorEncBuf = true;
        }

        // A chromaticities buffer to go in the color encoding buffer.
        StringBuffer chromaBuf = new StringBuffer(margn4
                + elementStart("mix:PrimaryChromaticities") + EOL);
        boolean useChromaBuf = false;
        r = niso.getPrimaryChromaticitiesRedX();
        if (r != null) {
            rationalToString(chromaBuf, "mix:primaryChromaticitiesRedX",
                    margn5, r);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesRedY();
        if (r != null) {
            rationalToString(chromaBuf, "mix:primaryChromaticitiesRedY",
                    margn5, r);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesGreenX();
        if (r != null) {
            rationalToString(chromaBuf, "mix:primaryChromaticitiesGreenX",
                    margn5, r);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesGreenY();
        if (r != null) {
            rationalToString(chromaBuf, "mix:primaryChromaticitiesGreenY",
                    margn5, r);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesBlueX();
        if (r != null) {
            rationalToString(chromaBuf, "mix:primaryChromaticitiesBlueX",
                    margn5, r);
            useChromaBuf = true;
        }
        r = niso.getPrimaryChromaticitiesBlueY();
        if (r != null) {
            rationalToString(chromaBuf, "mix:primaryChromaticitiesBlueY",
                    margn5, r);
            useChromaBuf = true;
        }
        chromaBuf
                .append(margn4 + elementEnd("mix:PrimaryChromaticities") + EOL);
        if (useChromaBuf) {
            colorEncBuf.append(chromaBuf);
            useColorEncBuf = true;
        }

        colorEncBuf.append(margn3 + elementEnd("mix:ImageColorEncoding") + EOL);
        if (useColorEncBuf) {
            _writer.print(colorEncBuf);
        }

        StringBuffer targetBuf = new StringBuffer(margn3
                + elementStart("mix:TargetData") + EOL);
        boolean useTargetBuf = false;
        n = niso.getTargetType();
        if (n != NisoImageMetadata.NULL) {
            targetBuf.append(margn4
                    + element("mix:targetType", Integer.toString(n)) + EOL);
            useTargetBuf = true;
        }

        // Now a nested buffer for TargetID.
        StringBuffer targetIDBuf = new StringBuffer(margn4
                + elementStart("mix:TargetID") + EOL);
        boolean useTargetIDBuf = false;

        s = niso.getTargetIDManufacturer();
        if (s != null) {
            targetIDBuf.append(margn5 + element("mix:targetManufacturer", s)
                    + EOL);
            useTargetIDBuf = true;
        }
        s = niso.getTargetIDName();
        if (s != null) {
            targetIDBuf.append(margn5 + element("mix:targetName", s) + EOL);
            useTargetIDBuf = true;
        }
        s = niso.getTargetIDNo();
        if (s != null) {
            targetIDBuf.append(margn5 + element("mix:targetNo", s) + EOL);
            useTargetIDBuf = true;
        }
        s = niso.getTargetIDMedia();
        if (s != null) {
            targetIDBuf.append(margn5 + element("mix:targetMedia", s) + EOL);
            useTargetIDBuf = true;
        }
        targetIDBuf.append(margn4 + elementEnd("mix:TargetID") + EOL);

        if (useTargetIDBuf) {
            targetBuf.append(targetIDBuf);
            useTargetBuf = true;
        }
        s = niso.getImageData();
        if (s != null) {
            targetBuf.append(margn4 + element("mix:externalTarget", s) + EOL);
            useTargetBuf = true;
        }
        s = niso.getPerformanceData();
        if (s != null) {
            targetBuf.append(margn4 + element("mix:performanceData", s) + EOL);
            useTargetBuf = true;
        }

        targetBuf.append(margn3 + elementEnd("mix:TargetData") + EOL);

        if (useTargetBuf) {
            _writer.print(targetBuf);
        }
        _writer.println(margn2 + elementEnd("mix:ImageAssessmentMetadata"));
    }

    /* 2.0, Top level element 5 of 5: ChangeHistory */
    protected void showChangeHistory20(NisoImageMetadata niso, String margin) {
        String margn2 = margin + " ";
        String margn3 = margn2 + " ";
        String margn4 = margn3 + " ";
        String margn5 = margn4 + " ";
        // String margn6 = margn5 + " ";

        // There may be nothing at all to write. Put the whole thing in a
        // buffer.
        StringBuffer chBuf = new StringBuffer(margn2
                + elementStart("mix:ChangeHistory") + EOL);
        boolean useChBuf = false;

        chBuf.append(margn3 + elementStart("mix:ImageProcessing") + EOL);

        String s = niso.getSourceData();
        if (s != null) {
            chBuf.append(margn4 + element("mix:sourceData", s) + EOL);
            useChBuf = true;
        }
        s = niso.getProcessingAgency();
        if (s != null) {
            chBuf.append(margn4 + element("mix:processingAgency", s) + EOL);
            useChBuf = true;
        }
        StringBuffer sftwBuf = new StringBuffer(margn4
                + elementStart("mix:ProcessingSoftware") + EOL);
        boolean useSftwBuf = false;
        s = niso.getProcessingSoftwareName();
        if (s != null) {
            sftwBuf.append(margn5 + element("mix:processingSoftwareName", s)
                    + EOL);
            useSftwBuf = true;
        }
        s = niso.getProcessingSoftwareVersion();
        if (s != null) {
            sftwBuf.append(margn5 + element("mix:processingSoftwareVersion", s)
                    + EOL);
            useSftwBuf = true;
        }
        s = niso.getOS();
        if (s != null) {
            sftwBuf.append(margn5
                    + element("mix:processingOperatingSystemName", s) + EOL);
            useSftwBuf = true;
        }
        s = niso.getOSVersion();
        if (s != null) {
            sftwBuf.append(margn5
                    + element("mix:processingOperatingSystemVersion", s) + EOL);
            useSftwBuf = true;
        }
        sftwBuf.append(margn4 + elementEnd("mix:ProcessingSoftware") + EOL);
        if (useSftwBuf) {
            chBuf.append(sftwBuf);
            useChBuf = true;
        }

        String[] sarray = niso.getProcessingActions();
        if (sarray != null) {
            for (int i = 0; i < sarray.length; i++) {
                chBuf.append(margn4
                        + element("mix:processingActions", sarray[i]) + EOL);
            }
            useChBuf = true;
        }

        chBuf.append(margn3 + elementEnd("mix:ImageProcessing") + EOL);
        chBuf.append(margn2 + elementEnd("mix:ChangeHistory") + EOL);
        if (useChBuf) {
            _writer.println(chBuf);
        }

    }

    /**
     * Convert the metering mode value to one of the suggested values for MIX
     * 2.0
     */
    private String meteringModeToString(int n) {
        String s = NisoImageMetadata.METERING_MODE[1];
        if (n >= 1 && n <= 6) {
            s = NisoImageMetadata.METERING_MODE[n];
        }
        // Capitalize first letter
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    /**
     * Convert the color space value (which is based on the TIFF
     * PhotometricInterpretation convention) to one of the suggested
     * values for MIX 2.0
     */
    private String photometricInterpretationToString(int n) {
        String s = "Unknown";
        switch (n) {
            case 0:
                s = "WhiteIsZero";
                break;
            case 1:
                s = "BlackIsZero";
                break;
            case 2:
                s = "RGB";
                break;
            case 3:
                s = "PaletteColor";
                break;
            case 4:
                s = "TransparencyMask";
                break;
            case 5:
                s = "CMYK";
                break;
            case 6:
                s = "YCbCr";
                break;
            case 8:
                s = "CIELab";
                break;
            case 9:
                s = "ICCLab";
                break;
            case 10:
                s = "ITULab";
                break;
            case 32803:
                s = "CFA";
                break; // used by DNG
            case 34892:
                s = "LinearRaw";
                break; // used by DNG
            case 65535:
                s = "YCCK";
                break; // used by Adobe JPEG
            default:
                break;
        }
        return s;
    }

    /**
     * Convert compression scheme value (based on the TIFF compression convention)
     * to a label
     */
    private String compressionSchemeToString(int n) {
        for (int i = 0; i < NisoImageMetadata.COMPRESSION_SCHEME_INDEX.length; i++) {
            if (n == NisoImageMetadata.COMPRESSION_SCHEME_INDEX[i])
                return NisoImageMetadata.COMPRESSION_SCHEME[i];
        }
        return Integer.toString(n);
    }

    /**
     * Display the audio metadata formatted according to
     * the AES schema.
     * 
     * @param aes AES audio metadata
     */
    protected void showAESAudioMetadata(AESAudioMetadata aes) {
        _level += 3;
        final String margin = getIndent(_level);
        final String margn2 = margin + " ";
        final String margn3 = margn2 + " ";
        final String margn4 = margn3 + " ";
        final String margn5 = margn4 + " ";
        // final String margn6 = margn5 + " ";

        // ID strings. These are arbitrary, but must be unique
        // within the document.
        final String formatRegionID = "J1";
        final String faceRegionID = "J2";
        final String faceID = "J3";
        final String audioObjectID = "J4";
        final String streamIDBase = "J9";

        _sampleRate = aes.getSampleRate();

        final String[][] attrs = { { "xmlns:aes", "http://www.aes.org/audioObject" },
                { "xmlns:xsi",
                        "http://www.w3.org/2001/XMLSchema-instance" },
                { "ID", audioObjectID },
                { "analogDigitalFlag",
                        aes.getAnalogDigitalFlag() },
                { "disposition",
                        "Validated by JHOVE" },
                { "schemaVersion", aes.getSchemaVersion() } };
        _writer.println(margin + elementStart("aes:audioObject", attrs));
        String s = aes.getFormat();
        if (s != null) {
            String v = aes.getSpecificationVersion();
            String[][] fmattrs = new String[1][2];
            fmattrs[0][0] = "specificationVersion";
            if (v != null) {
                fmattrs[0][1] = v;
            } else {
                // Shouldn't happen
                fmattrs[0][1] = "";
            }
            _writer.println(margn2 + element("aes:format", fmattrs, s));
        }
        s = aes.getAppSpecificData();
        if (s != null) {
            _writer.println(margn2 + element("aes:appSpecificData", s));
        }
        s = aes.getAudioDataEncoding();
        if (s != null) {
            _writer.println(margn2 + element("aes:audioDataEncoding", s));
        }
        int in = aes.getByteOrder();
        if (in != AESAudioMetadata.NULL) {
            _writer.println(margn2 + element("aes:byteOrder",
                    in == AESAudioMetadata.BIG_ENDIAN ? "BIG_ENDIAN" : "LITTLE_ENDIAN"));
        }
        long lin = aes.getFirstSampleOffset();
        if (lin != AESAudioMetadata.NULL) {
            _writer.println(margn2 + element("aes:firstSampleOffset",
                    Long.toString(lin)));
        }
        String[] use = aes.getUse();
        if (use != null) {
            String[][] uattrs = new String[][] { { "useType", use[0] },
                    { "otherType", use[1] } };
            _writer.println(margn2 + element("aes:use", uattrs));
        }
        s = aes.getPrimaryIdentifier();
        if (s != null) {
            String t = aes.getPrimaryIdentifierType();
            String[][] idattrs = new String[1][2];
            idattrs[0][0] = "identifierType";
            if (t != null) {
                idattrs[0][1] = t;
            } else {
                // Shouldn't happen
                idattrs[0][1] = "";
            }
            _writer.println(margn2 + element("aes:primaryIdentifier", idattrs, s));
        }

        // Add the face information, which is mostly filler.
        // In the general case, it can contain multiple Faces;
        // this isn't supported yet.
        List facelist = aes.getFaceList();
        if (!facelist.isEmpty()) {
            final String[][] faceRegionAttrs = {
                    { "ID", faceRegionID },
                    { "formatRef", formatRegionID },
                    { "faceRef", faceID },
                    { "label", "BuiltByJHOVE" }
            };
            final String[][] faceAttrs = {
                    { "direction", null },
                    { "ID", faceID },
                    { "audioObjectRef", audioObjectID },
                    { "label", "Face" }
            };
            AESAudioMetadata.Face f = (AESAudioMetadata.Face) facelist.get(0);
            faceAttrs[0][1] = f.getDirection();
            _writer.println(margn2 + elementStart("aes:face", faceAttrs));
            // Fill in a minimal time range.
            AESAudioMetadata.TimeDesc startTime = f.getStartTime();
            if (startTime != null) {
                _writer.println(margn3 + elementStart("aes:timeline"));
                writeAESTimeRange(margn3, startTime, f.getDuration());
                _writer.println(margn3 + elementEnd("aes:timeline"));
            }

            // For the present, assume just one face region
            AESAudioMetadata.FaceRegion facergn = f.getFaceRegion(0);
            _writer.println(margn3 + elementStart("aes:region", faceRegionAttrs));
            _writer.println(margn4 + elementStart("aes:timeRange"));
            writeAESTimeRange(margn4,
                    facergn.getStartTime(), facergn.getDuration());
            _writer.println(margn4 + elementEnd("aes:timeRange"));
            int nchan = aes.getNumChannels();
            if (nchan != AESAudioMetadata.NULL) {
                _writer.println(margn4 + element("aes:numChannels",
                        Integer.toString(nchan)));
            }
            for (int ch = 0; ch < nchan; ch++) {
                // write a stream element for each channel
                String[][] streamAttrs = {
                        { "ID", streamIDBase + Integer.toString(ch) },
                        { "label", "JHOVE" },
                        { "faceRegionRef", faceRegionID }

                };
                _writer.println(margn4 + elementStart("aes:stream", streamAttrs));
                String[][] chanAttrs = {
                        { "channelNum", Integer.toString(ch) }
                };
                _writer.println(margn5 + element("aes:channelAssignment", chanAttrs));
                _writer.println(margn4 + elementEnd("aes:stream"));
            }
            _writer.println(margn3 + elementEnd("aes:region"));
            _writer.println(margn2 + elementEnd("aes:face"));
        }

        // In the general case, a FormatList can contain multiple
        // FormatRegions. This doesn't happen with any of the current
        // modules; if it's needed in the future, simply set up an
        // iteration loop on formatList.
        List flist = aes.getFormatList();
        if (!flist.isEmpty()) {
            AESAudioMetadata.FormatRegion rgn = (AESAudioMetadata.FormatRegion) flist.get(0);
            int bitDepth = rgn.getBitDepth();
            double sampleRate = rgn.getSampleRate();
            int wordSize = rgn.getWordSize();
            String[] bitRed = rgn.getBitrateReduction();
            // Build a FormatRegion subtree if at least one piece of data
            // that goes into it is present.
            if (bitDepth != AESAudioMetadata.NULL ||
                    sampleRate != AESAudioMetadata.NILL ||
                    wordSize != AESAudioMetadata.NULL) {
                _writer.println(margn2 + elementStart("aes:formatList"));
                String[][] frAttr = { { "ID", formatRegionID },
                        { "xsi:type", "aes:formatRegionType" },
                        { "ownerRef", faceRegionID },
                        { "label", "JHOVE" } };
                _writer.println(margn3 + elementStart("aes:formatRegion", frAttr));
                if (bitDepth != AESAudioMetadata.NULL) {
                    _writer.println(margn4 + element("aes:bitDepth",
                            Integer.toString(bitDepth)));
                }
                if (sampleRate != AESAudioMetadata.NILL) {
                    _writer.println(margn4 + element("aes:sampleRate",
                            formatters.get().format(sampleRate)));
                }
                if (wordSize != AESAudioMetadata.NULL) {
                    _writer.println(margn4 + element("aes:wordSize",
                            Integer.toString(wordSize)));
                }
                if (bitRed != null) {
                    _writer.println(margn4 + elementStart("aes:bitrateReduction"));
                    _writer.println(margn5 + element("aes:codecName", bitRed[0]));
                    _writer.println(margn5 + element("aes:codecNameVersion", bitRed[1]));
                    _writer.println(margn5 + element("aes:codecCreatorApplication", bitRed[2]));
                    _writer.println(margn5 + element("aes:codecCreatorApplicationVersion", bitRed[3]));
                    _writer.println(margn5 + element("aes:codecQuality", bitRed[4]));
                    _writer.println(margn5 + element("aes:dataRate", bitRed[5]));
                    _writer.println(margn5 + element("aes:dataRateMode", bitRed[6]));
                    _writer.println(margn4 + elementEnd("aes:bitrateReduction"));
                }
                _writer.println(margn3 + elementEnd("aes:formatRegion"));
                _writer.println(margn2 + elementEnd("aes:formatList"));
            }
        }
        /* This should go somewhere, but where? */
        // int nchan = aes.getNumChannels ();
        // if (nchan != AESAudioMetadata.NULL) {
        // _writer.println (margn2 + element ("aes:numChannels",
        // Integer.toString (nchan)));
        // }

        _writer.println(margin + elementEnd("aes:audioObject"));

        _level -= 3;
    }

    private void writeAESTimeRange(String baseIndent,
            AESAudioMetadata.TimeDesc start,
            AESAudioMetadata.TimeDesc duration) {
        final String margn1 = baseIndent + " ";

        writeAESTimeRangePart(margn1, "aes:startTime", start);

        if (duration != null) {
            writeAESTimeRangePart(margn1, "aes:duration", duration);
        }
    }

    private void writeAESTimeRangePart(String indent, String elementName, AESAudioMetadata.TimeDesc timeDesc) {
        double sampleRate = timeDesc.getSampleRate();
        if (sampleRate == 1.0) {
            sampleRate = _sampleRate;
        }

        String[][] attributes = {
                { "editRate", formatters.get().format(sampleRate) },
                { "factorNumerator", "1" },
                { "factorDenominator", "1" }
        };

        _writer.println(indent +
                element(elementName, attributes, String.valueOf(timeDesc.getSamples())));
    }

    /**
     * Returns a path normalised URI from the presented string path.@interface
     * Solution based upon the follwing post from Eugene Yokota:
     * https://eed3si9n.com/encoding-file-path-as-URI-reference/
     */
    private static final String cleanURIString(final String path) {
        File input = new File(path);
        final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");
        final String fileScheme = "file";
        try {
            if (isWindows && !path.isEmpty() && path.startsWith(Character.toString(File.separatorChar))) {
                if (path.startsWith("\\")) {
                    return new URI(fileScheme, normaliseToSlash(path), null).toString();
                } else {
                    return new URI(fileScheme, "", normaliseToSlash(path), null).toString();
                }
            } else if (input.isAbsolute()) {
                return new URI(fileScheme, "", normaliseToSlash(ensureHeadSlash(input.getAbsolutePath())), null)
                        .toString();
            }
            return new URI(null, normaliseToSlash(path), null).toString();
        } catch (URISyntaxException e) {
            // If this fails simply return the original path
            return path;
        }
    }

    private static final String ensureHeadSlash(final String name) {
        return (!name.isEmpty() && name.startsWith(Character.toString(File.separatorChar)))
                ? Character.toString(File.separatorChar) + name
                : name;
    }

    private static final String normaliseToSlash(final String name) {
        return (File.separatorChar == '/') ? name : name.replace(File.separatorChar, '/');
    }

    /** Appends a Rational value to a StringBuffer */
    public void rationalToString(StringBuffer buf, String tag, String margin,
            Rational r) {
        String margn2 = margin + " ";

        long numer = r.getNumerator();
        long denom = r.getDenominator();
        buf.append(margin + elementStart(tag) + EOL);
        buf.append(margn2 + element("mix:numerator", Long.toString(numer))
                + EOL);
        if (denom != 1L) {
            buf.append(margn2
                    + element("mix:denominator", Long.toString(denom)) + EOL);
        }
        buf.append(margin + elementEnd(tag) + EOL);
    }
}
