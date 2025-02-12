/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2003-2005 by JSTOR and the President and Fellows of Harvard College
 **********************************************************************/

package edu.harvard.hul.ois.jhove.viewer;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.harvard.hul.ois.jhove.AESAudioMetadata;
import edu.harvard.hul.ois.jhove.Checksum;
import edu.harvard.hul.ois.jhove.ErrorMessage;
import edu.harvard.hul.ois.jhove.InfoMessage;
import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.Message;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.NisoImageMetadata;
import edu.harvard.hul.ois.jhove.Property;
import edu.harvard.hul.ois.jhove.PropertyArity;
import edu.harvard.hul.ois.jhove.PropertyType;
import edu.harvard.hul.ois.jhove.Rational;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.TextMDMetadata;
import edu.harvard.hul.ois.jhove.handler.Handlers;

/**
 * This subclass of DefaultMutableTreeNode simply adds a method for constructing
 * the tree. All nodes in the tree except for the root will be plain
 * DefaultMutablereeNodes.
 */
public class RepTreeRoot extends DefaultMutableTreeNode {

    /**
     * Serialisation identifier
     */
    private static final long serialVersionUID = -4409152022584715925L;
    private RepInfo _info;
    private JhoveBase _base;
    private boolean _rawOutput;
    private DateFormat _dateFmt;

    /* Sample rate. */
    private double _sampleRate;
    private String reportingModule = "";

    /**
     * Constructor.
     *
     * @param info
     *             The RepInfo object whose contents are to be displayed.
     * @param base
     *             The JHOVE base on which we're operating.
     */
    public RepTreeRoot(RepInfo info, JhoveBase base) {
        super(info.getUri());
        _info = info;
        _base = base;
        _rawOutput = _base.getShowRawFlag();

        // Set the DateFormat for displaying the module date.
        _dateFmt = DateFormat.getDateInstance();

        // Snarf everything up into the tree.

        snarfRepInfo();
    }

    private static final String TEXT_MD_METADTA = "TextMDMetadata";
    private static final String FORMAT = "Format: ";
    private static final String VERSION = "Version: ";
    private static final String BYTE_ORDER = "ByteOrder: ";

    /**
     * Constructs a DefaultMutableTreeNode representing a property
     */
    private DefaultMutableTreeNode propToNode(Property pProp) {
        PropertyArity arity = pProp.getArity();
        PropertyType typ = pProp.getType();
        Object pValue = pProp.getValue();
        if (arity == PropertyArity.SCALAR) {
            if (null == typ) {
                // Simple types: just use name plus string value.
                return new DefaultMutableTreeNode(
                        pProp.getName() + ": " + pValue.toString());
            } else {
                TextMDMetadata tData;
                DefaultMutableTreeNode val;
                switch (typ) {
                    case NISOIMAGEMETADATA:
                        // NISO Image metadata is a world of its own.
                        NisoImageMetadata nData = (NisoImageMetadata) pValue;
                        return nisoToNode(nData);
                    case AESAUDIOMETADATA:
                        // AES audio metadata is another world.
                        AESAudioMetadata aData = (AESAudioMetadata) pValue;
                        return aesToNode(aData);
                    case TEXTMDMETADATA:
                        // textMD metadata is another world.
                        tData = (TextMDMetadata) pValue;
                        return textMDToNode(tData);
                    case PROPERTY:

                        if (TEXT_MD_METADTA.equals(pProp.getName())) {
                            tData = (TextMDMetadata) pValue;
                            return textMDToNode(tData);
                        }
                        // A scalar property of type Property -- seems
                        // pointless, but we handle it.
                        val = new DefaultMutableTreeNode(pProp.getName());
                        val.add(propToNode((Property) pValue));
                        return val;

                    default:

                        // Simple types: just use name plus string value.
                        val = new DefaultMutableTreeNode(pProp.getName() + ": "
                                + pValue.toString());
                        return val;

                }
            }
        }
        // Compound properties. The text of the node is the
        // property name.
        DefaultMutableTreeNode val = new DefaultMutableTreeNode(pProp.getName());
        if (null != arity)
            switch (arity) {
                case ARRAY:
                    addArrayMembers(val, pProp);
                    break;
                case LIST:
                    addListMembers(val, pProp);
                    break;
                case MAP:
                    addMapMembers(val, pProp);
                    break;
                case SET:
                    addSetMembers(val, pProp);
                    break;
                default:
                    break;
            }
        return val;
    }

    /**
     * Find the index of an object in its parent. Understands the Jhove property
     * structure.
     */
    public int getIndexOfChild(Object parent, Object child) {
        Property pProp = (Property) parent;
        PropertyArity arity = pProp.getArity();
        // For Lists, Maps, and Sets we construct an Iterator.
        Iterator<?> iter = null;
        if (arity == PropertyArity.SET || arity == PropertyArity.LIST
                || arity == PropertyArity.MAP) {
            if (null == arity) {
                List<?> list = (List<?>) pProp.getValue();
                iter = list.iterator();
            } else
                switch (arity) {
                    case SET:
                        Set<?> set = (Set<?>) pProp.getValue();
                        iter = set.iterator();
                        break;
                    case MAP:
                        Map<?, ?> map = (Map<?, ?>) pProp.getValue();
                        iter = map.values().iterator();
                        break;
                    default:
                        List<?> list = (List<?>) pProp.getValue();
                        iter = list.iterator();
                        break;
                }
            for (int i = 0;; i++) {
                if (!iter.hasNext()) {
                    return 0; // Should never happen
                } else if (iter.next() == child) {
                    return i;
                }
            }
        }
        // OK, that was the easy one. Now for that damn array arity.
        // In the case of non-object types, we can't actually tell which
        // position matches the object, so we return 0 and hope it doesn't
        // mess things up too much.
        PropertyType propType = pProp.getType();
        java.util.Date[] dateArray = null;
        Property[] propArray = null;
        Rational[] rationalArray = null;
        Object[] objArray = null;
        int n = 0;

        if (null == propType) {
            return 0; // non-object array type
        } else
            switch (propType) {
                case DATE:
                    dateArray = (java.util.Date[]) pProp.getValue();
                    n = dateArray.length;
                    break;
                case OBJECT:
                    objArray = (Object[]) pProp.getValue();
                    n = objArray.length;
                    break;
                case RATIONAL:
                    rationalArray = (Rational[]) pProp.getValue();
                    n = rationalArray.length;
                    break;
                case PROPERTY:
                    propArray = (Property[]) pProp.getValue();
                    n = propArray.length;
                    break;
                default:
                    return 0; // non-object array type
            }

        for (int i = 0; i < n; i++) {
            Object elem = null;
            switch (propType) {
                case DATE:
                    elem = dateArray[i];
                    break;
                case OBJECT:
                    elem = objArray[i];
                    break;
                case RATIONAL:
                    elem = rationalArray[i];
                    break;
                case PROPERTY:
                    elem = propArray[i];
                    break;
                default:
                    break;
            }
            if (elem == child) {
                return i;
            }
        }
        return 0; // somehow fell through
    }

    private void snarfRepInfo() {
        // This node has two children, for the module and the RepInfo

        Module module = _info.getModule();
        if (module != null) {
            // Create a subnode for the module, which has three
            // leaf children.
            DefaultMutableTreeNode moduleNode = new DefaultMutableTreeNode(
                    "Module");
            this.reportingModule = module.getName();
            moduleNode.add(new DefaultMutableTreeNode(this.reportingModule, false));
            moduleNode.add(new DefaultMutableTreeNode("Release: "
                    + module.getRelease(), false));
            moduleNode.add(new DefaultMutableTreeNode("Date: "
                    + _dateFmt.format(module.getDate()), false));
            add(moduleNode);
        }

        DefaultMutableTreeNode infoNode = new DefaultMutableTreeNode("RepInfo");
        infoNode.add(new DefaultMutableTreeNode("URI: " + _info.getUri(), false));
        Date dt = _info.getCreated();
        if (dt != null) {
            infoNode.add(new DefaultMutableTreeNode(
                    "Created: " + dt.toString(), false));
        }
        dt = _info.getLastModified();
        if (dt != null) {
            infoNode.add(new DefaultMutableTreeNode("LastModified: "
                    + dt.toString(), false));
        }
        long sz = _info.getSize();
        if (sz != -1) {
            infoNode.add(new DefaultMutableTreeNode("Size: "
                    + Long.toString(sz), false));
        }
        String s = _info.getFormat();
        if (s != null) {
            infoNode.add(new DefaultMutableTreeNode(FORMAT + s, false));
        }
        s = _info.getVersion();
        if (s != null) {
            infoNode.add(new DefaultMutableTreeNode(VERSION + s, false));
        }
        String wfStr;
        switch (_info.getWellFormed()) {
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
        if (_info.getWellFormed() == RepInfo.TRUE) {
            switch (_info.getValid()) {
                case RepInfo.TRUE:
                    wfStr += " and valid";
                    break;

                case RepInfo.FALSE:
                    wfStr += ", but not valid";
                    break;

                // case UNDETERMINED: add nothing
            }
        }
        infoNode.add(new DefaultMutableTreeNode("Status: " + wfStr, false));

        // Report modules that said their signatures match
        List<String> sigList = _info.getSigMatch();
        if (sigList != null && !sigList.isEmpty()) {
            DefaultMutableTreeNode sigNode = new DefaultMutableTreeNode(
                    "SignatureMatches");
            infoNode.add(sigNode);
            for (int i = 0; i < sigList.size(); i++) {
                DefaultMutableTreeNode sNode = new DefaultMutableTreeNode(
                        sigList.get(i));
                sigNode.add(sNode);
            }
        }
        // Compile a list of messages and offsets into a subtree
        List<Message> messageList = _info.getMessage();
        if (messageList != null && !messageList.isEmpty()) {
            DefaultMutableTreeNode msgNode = new DefaultMutableTreeNode(
                    "Messages");
            infoNode.add(msgNode);
            int i;
            for (i = 0; i < messageList.size(); i++) {
                Message msg = messageList.get(i);
                String prefix;
                if (msg instanceof InfoMessage) {
                    prefix = "InfoMessage: ";
                } else if (msg instanceof ErrorMessage) {
                    prefix = "ErrorMessage: ";
                } else {
                    prefix = "Message: ";
                }
                DefaultMutableTreeNode mNode = new DefaultMutableTreeNode(
                        prefix + msg.getMessage());

                if (msg.getId() != null && !msg.getId().isEmpty()) {
                    mNode.add(new DefaultMutableTreeNode("ID: "
                            + msg.getId()));
                    mNode.add(new DefaultMutableTreeNode("InfoLink: "
                            + Handlers.makeInfoLink(this.reportingModule, msg.getId())));
                }

                String subMessage = msg.getSubMessage();
                if (subMessage != null) {
                    mNode.add(new DefaultMutableTreeNode("SubMessage: "
                            + subMessage));
                }
                long offset = -1;
                if (msg instanceof ErrorMessage) {
                    offset = ((ErrorMessage) msg).getOffset();
                }
                //
                // If the offset is positive, we give the message node
                // a child with the offset value.
                if (offset >= 0) {
                    mNode.add(new DefaultMutableTreeNode("Offset: "
                            + Long.toString(offset)));
                }
                msgNode.add(mNode);
            }
        }

        s = _info.getMimeType();
        if (s != null) {
            infoNode.add(new DefaultMutableTreeNode("MimeType: " + s, false));
        }

        // Compile a list of profile strings into a string list
        List<String> profileList = _info.getProfile();
        if (profileList != null && !profileList.isEmpty()) {
            DefaultMutableTreeNode profNode = new DefaultMutableTreeNode(
                    "Profiles");
            infoNode.add(profNode);
            int i;
            for (i = 0; i < profileList.size(); i++) {
                profNode.add(new DefaultMutableTreeNode(profileList.get(i),
                        false));
            }
        }

        // Here we come to the property map. We have to walk
        // through all the properties recursively, turning
        // each into a leaf or subtree.
        Map<String, Property> map = _info.getProperty();
        if (map != null) {
            Iterator<String> iter = map.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                Property property = _info.getProperty(key);
                infoNode.add(propToNode(property));
            }
        }

        List<Checksum> cksumList = _info.getChecksum();
        if (cksumList != null && !cksumList.isEmpty()) {
            DefaultMutableTreeNode ckNode = new DefaultMutableTreeNode(
                    "Checksums");
            infoNode.add(ckNode);
            for (Checksum cksum : cksumList) {
                DefaultMutableTreeNode csNode = new DefaultMutableTreeNode(
                        "Checksum");
                ckNode.add(csNode);
                csNode.add(new DefaultMutableTreeNode("Type:"
                        + cksum.getType().toString(), false));
                csNode.add(new DefaultMutableTreeNode("Checksum: "
                        + cksum.getValue(), false));
            }
        }

        s = _info.getNote();
        if (s != null) {
            infoNode.add(new DefaultMutableTreeNode("Note: " + s, false));
        }
        add(infoNode);
    }

    /*
     * Add the members of an array property to a node. The property must be of
     * arity ARRAY.
     */
    private void addArrayMembers(DefaultMutableTreeNode node, Property p) {
        Object pVal = p.getValue();
        PropertyType typ = p.getType();
        if (null != typ)
            switch (typ) {
                case INTEGER: {
                    if (pVal instanceof int[]) {
                        Integer[] vals = Arrays.stream((int[]) pVal) // IntStream
                                .boxed() // Stream<Integer>
                                .toArray(Integer[]::new);
                        addToNode(node, vals);
                    } else {
                        addToNode(node, (Integer[]) pVal);
                    }
                    break;
                }
                case LONG: {
                    if (pVal instanceof long[]) {
                        Long[] vals = Arrays.stream((long[]) pVal) // IntStream
                                .boxed() // Stream<Integer>
                                .toArray(Long[]::new);
                        addToNode(node, vals);
                    } else {
                        addToNode(node, (Long[]) pVal);
                    }
                    break;
                }
                case BOOLEAN: {
                    addToNode(node, (Boolean[]) pVal);
                    break;
                }
                case CHARACTER: {
                    addToNode(node, (Character[]) pVal);
                    break;
                }
                case DOUBLE: {
                    if (pVal instanceof double[]) {
                        Double[] vals = Arrays.stream((double[]) pVal) // IntStream
                                .boxed() // Stream<Double>
                                .toArray(Double[]::new);
                        addToNode(node, vals);
                    } else {
                        addToNode(node, (Double[]) pVal);
                    }
                    break;
                }
                case FLOAT: {
                    addToNode(node, (Float[]) pVal);
                    break;
                }
                case SHORT: {
                    addToNode(node, (Short[]) pVal);
                    break;
                }
                case BYTE: {
                    addToNode(node, (Byte[]) pVal);
                    break;
                }
                case STRING: {
                    addToNode(node, (String[]) pVal);
                    break;
                }
                case RATIONAL: {
                    addToNode(node, (Rational[]) pVal);
                    break;
                }
                case PROPERTY: {
                    addToNode(node, (Property[]) pVal);
                    break;
                }
                case NISOIMAGEMETADATA: {
                    addToNode(node, (NisoImageMetadata[]) pVal);
                    break;
                }
                case OBJECT: {
                    addToNode(node, (Object[]) pVal);
                    break;
                }
                default:
                    break;
            }
    }

    /*
     * Add the members of a list property to a node. The property must be of
     * arity LIST.
     */
    private void addListMembers(DefaultMutableTreeNode node, Property p) {
        List<?> l = (List<?>) p.getValue();
        PropertyType ptyp = p.getType();
        boolean canHaveChildren = Boolean.FALSE;
        l.forEach(item -> node.add(getDefaultMutableTreeNode(ptyp, item,
                canHaveChildren)));
    }

    /*
     * Add the members of a set property to a node. The property must be of
     * arity SET.
     */
    private void addSetMembers(DefaultMutableTreeNode node, Property p) {
        Set<?> s = (Set<?>) p.getValue();
        PropertyType ptyp = p.getType();
        boolean canHaveChildren = Boolean.FALSE;
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            node.add(getDefaultMutableTreeNode(ptyp, item, canHaveChildren));
        }
    }

    /*
     * Add the members of a map property to a node. The property must be of
     * arity MAP.
     */
    private void addMapMembers(DefaultMutableTreeNode node, Property p) {
        Map<?, ?> m = (Map<?, ?>) p.getValue();
        PropertyType ptyp = p.getType();
        Boolean canHaveChildren = Boolean.TRUE;
        Iterator<?> iter = m.keySet().iterator();
        while (iter.hasNext()) {
            DefaultMutableTreeNode itemNode;
            String key = (String) iter.next();
            Object item = m.get(key);
            itemNode = getDefaultMutableTreeNode(ptyp, item, canHaveChildren);
            node.add(itemNode);

            // Add a subnode for the key
            itemNode.setAllowsChildren(true);
            itemNode.add(new DefaultMutableTreeNode("Key: " + key, false));
        }
    }

    /* Function for turning the AES metadata into a subtree. */
    private DefaultMutableTreeNode aesToNode(AESAudioMetadata aes) {
        _sampleRate = aes.getSampleRate();

        DefaultMutableTreeNode val = new DefaultMutableTreeNode(
                "AESAudioMetadata", true);
        String s = aes.getAnalogDigitalFlag();
        if (s != null) {
            val.add(new DefaultMutableTreeNode("AnalogDigitalFlag: " + s, false));
            // The "false" argument signifies this will have no subnodes
        }
        s = aes.getSchemaVersion();
        if (s != null) {
            val.add(new DefaultMutableTreeNode("SchemaVersion: " + s, false));
        }
        s = aes.getFormat();
        if (s != null) {
            DefaultMutableTreeNode fmt = new DefaultMutableTreeNode(FORMAT
                    + s, true);
            val.add(fmt);
            String v = aes.getSpecificationVersion();
            if (v != null) {
                fmt.add(new DefaultMutableTreeNode(
                        "SpecificationVersion: " + v, false));
            }
        }
        s = aes.getAppSpecificData();
        if (s != null) {
            val.add(new DefaultMutableTreeNode("AppSpecificData: " + s, false));
        }
        s = aes.getAudioDataEncoding();
        if (s != null) {
            val.add(new DefaultMutableTreeNode("AudioDataEncoding: " + s, false));
        }
        int in = aes.getByteOrder();
        if (in != AESAudioMetadata.NULL) {
            val.add(new DefaultMutableTreeNode(BYTE_ORDER
                    + (in == AESAudioMetadata.BIG_ENDIAN ? "BIG_ENDIAN"
                            : "LITTLE_ENDIAN")));
        }
        long lin = aes.getFirstSampleOffset();
        if (lin != AESAudioMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("FirstSampleOffset: "
                    + Long.toString(lin)));
        }
        String[] use = aes.getUse();
        if (use != null) {
            DefaultMutableTreeNode u = new DefaultMutableTreeNode("Use", true);
            val.add(u);
            u.add(new DefaultMutableTreeNode("UseType: " + use[0], false));
            u.add(new DefaultMutableTreeNode("OtherType: " + use[1], false));
        }
        s = aes.getPrimaryIdentifier();
        if (s != null) {
            String t = aes.getPrimaryIdentifierType();
            DefaultMutableTreeNode pi = new DefaultMutableTreeNode(
                    "PrimaryIdentifier: " + s, true);
            val.add(pi);
            if (t != null) {
                pi.add(new DefaultMutableTreeNode("IdentifierType: " + t));
            }
        }
        // Add the face information, which is mostly filler.
        // In the general case, it can contain multiple Faces;
        // this isn't supported yet.
        List<AESAudioMetadata.Face> facelist = aes.getFaceList();
        if (!facelist.isEmpty()) {
            AESAudioMetadata.Face f = facelist.get(0);

            DefaultMutableTreeNode face = new DefaultMutableTreeNode("Face",
                    true);
            DefaultMutableTreeNode timeline = new DefaultMutableTreeNode(
                    "TimeLine", true);
            AESAudioMetadata.TimeDesc startTime = f.getStartTime();
            if (startTime != null) {
                addAESTimeRange(timeline, startTime, f.getDuration());
            }
            face.add(timeline);

            // For the present, assume just one face region
            AESAudioMetadata.FaceRegion facergn = f.getFaceRegion(0);
            DefaultMutableTreeNode region = new DefaultMutableTreeNode(
                    "Region", true);
            timeline = new DefaultMutableTreeNode("TimeRange", true);
            addAESTimeRange(timeline, facergn.getStartTime(),
                    facergn.getDuration());
            region.add(timeline);
            int nchan = aes.getNumChannels();
            if (nchan != AESAudioMetadata.NULL) {
                region.add(new DefaultMutableTreeNode("NumChannels: "
                        + Integer.toString(nchan), false));
                for (int ch = 0; ch < nchan; ch++) {
                    // write a stream element for each channel
                    DefaultMutableTreeNode stream = new DefaultMutableTreeNode(
                            "Stream", true);
                    region.add(stream);
                    stream.add(new DefaultMutableTreeNode("ChannelNum: " + Integer.toString(ch), false));
                }
            }
            face.add(region);
            val.add(face);
        }
        // In the general case, a FormatList can contain multiple
        // FormatRegions. This doesn't happen with any of the current
        // modules; if it's needed in the future, simply set up an
        // iteration loop on formatList.
        List<AESAudioMetadata.FormatRegion> flist = aes.getFormatList();
        if (!flist.isEmpty()) {
            AESAudioMetadata.FormatRegion rgn = flist.get(0);
            int bitDepth = rgn.getBitDepth();
            double sampleRate = rgn.getSampleRate();
            int wordSize = rgn.getWordSize();
            String[] bitRed = rgn.getBitrateReduction();
            // Build a FormatRegion subtree if at least one piece of data
            // that goes into it is present.
            if (bitDepth != AESAudioMetadata.NULL
                    || sampleRate != AESAudioMetadata.NILL
                    || wordSize != AESAudioMetadata.NULL) {
                DefaultMutableTreeNode formatList = new DefaultMutableTreeNode(
                        "FormatList", true);
                DefaultMutableTreeNode formatRegion = new DefaultMutableTreeNode(
                        "FormatRegion", true);
                if (bitDepth != AESAudioMetadata.NULL) {
                    formatRegion.add(new DefaultMutableTreeNode("BitDepth: "
                            + Integer.toString(bitDepth), false));
                }
                if (sampleRate != AESAudioMetadata.NILL) {
                    formatRegion.add(new DefaultMutableTreeNode("SampleRate: "
                            + Double.toString(sampleRate), false));
                }
                if (wordSize != AESAudioMetadata.NULL) {
                    formatRegion.add(new DefaultMutableTreeNode("WordSize: "
                            + Integer.toString(bitDepth), false));
                }
                if (bitRed != null) {
                    DefaultMutableTreeNode br = new DefaultMutableTreeNode(
                            "BitrateReduction", true);
                    br.add(new DefaultMutableTreeNode(
                            "codecName: " + bitRed[0], false));
                    br.add(new DefaultMutableTreeNode("codecNameVersion: "
                            + bitRed[1], false));
                    br.add(new DefaultMutableTreeNode(
                            "codecCreatorApplication: " + bitRed[2], false));
                    br.add(new DefaultMutableTreeNode(
                            "codecCreatorApplicationVersion: " + bitRed[3],
                            false));
                    br.add(new DefaultMutableTreeNode("codecQuality: "
                            + bitRed[4], false));
                    br.add(new DefaultMutableTreeNode("dataRate: " + bitRed[5],
                            false));
                    br.add(new DefaultMutableTreeNode("dataRateMode: "
                            + bitRed[6], false));
                    formatRegion.add(br);
                }
                formatList.add(formatRegion);
                val.add(formatList);
            }
        }

        return val;
    }

    private void addAESTimeRange(DefaultMutableTreeNode parent,
            AESAudioMetadata.TimeDesc start, AESAudioMetadata.TimeDesc duration) {
        writeAESTimeRangePart(parent, "StartTime", start);
        // Duration is optional.
        if (duration != null) {
            writeAESTimeRangePart(parent, "Duration", duration);
        }
    }

    private void writeAESTimeRangePart(DefaultMutableTreeNode parent,
            String name, AESAudioMetadata.TimeDesc timeDesc) {
        double sampleRate = timeDesc.getSampleRate();
        if (sampleRate == 1.0) {
            sampleRate = _sampleRate;
        }

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name, true);
        node.add(new DefaultMutableTreeNode(
                "Value: " + String.valueOf(timeDesc.getSamples()), false));
        node.add(new DefaultMutableTreeNode(
                "EditRate: " + sampleRate, false));
        node.add(new DefaultMutableTreeNode(
                "FactorNumerator: 1", false));
        node.add(new DefaultMutableTreeNode(
                "FactorDenominator: 1", false));

        parent.add(node);
    }

    /* Function for turning the textMD metadata into a subtree. */
    private DefaultMutableTreeNode textMDToNode(TextMDMetadata textMD) {
        DefaultMutableTreeNode val = new DefaultMutableTreeNode(
                TEXT_MD_METADTA, true);

        DefaultMutableTreeNode u = new DefaultMutableTreeNode("Character_info",
                true);
        val.add(u);

        String s = textMD.getCharset();
        if (s != null) {
            u.add(new DefaultMutableTreeNode("Charset: " + s, false));
        }
        s = textMD.getByte_orderString();
        if (s != null) {
            u.add(new DefaultMutableTreeNode("Byte_order: " + s, false));
        }
        s = textMD.getByte_size();
        if (s != null) {
            u.add(new DefaultMutableTreeNode("Byte_size: " + s, false));
        }
        s = textMD.getCharacter_size();
        if (s != null) {
            u.add(new DefaultMutableTreeNode("Character_size: " + s, false));
        }
        s = textMD.getLinebreakString();
        if (s != null) {
            u.add(new DefaultMutableTreeNode("Linebreak: " + s, false));
        }
        s = textMD.getLanguage();
        if (s != null) {
            val.add(new DefaultMutableTreeNode("Language: " + s, false));
        }
        s = textMD.getMarkup_basis();
        if (s != null) {
            DefaultMutableTreeNode basis = new DefaultMutableTreeNode(
                    "Markup_basis: " + s, true);
            val.add(basis);
            s = textMD.getMarkup_basis_version();
            if (s != null) {
                basis.add(new DefaultMutableTreeNode(VERSION + s, false));
            }
        }
        s = textMD.getMarkup_language();
        if (s != null) {
            DefaultMutableTreeNode language = new DefaultMutableTreeNode(
                    "Markup_language: " + s, true);
            val.add(language);
            s = textMD.getMarkup_language_version();
            if (s != null) {
                language.add(new DefaultMutableTreeNode(VERSION + s, false));
            }
        }
        return val;
    }

    /* Function for turning the Niso metadata into a subtree. */
    private DefaultMutableTreeNode nisoToNode(NisoImageMetadata niso) {
        DefaultMutableTreeNode val = new DefaultMutableTreeNode(
                "NisoImageMetadata", true);
        String s = niso.getMimeType();
        if (s != null) {
            val.add(new DefaultMutableTreeNode("FormatName: " + s, false));
        }
        s = niso.getByteOrder();
        if (s != null) {
            val.add(new DefaultMutableTreeNode(BYTE_ORDER + s, false));
        }

        int n = niso.getCompressionScheme();
        if (n != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("CompressionScheme: "
                    + integerRepresentation(n,
                            NisoImageMetadata.COMPRESSION_SCHEME,
                            NisoImageMetadata.COMPRESSION_SCHEME_INDEX),
                    false));
        }
        if ((n = niso.getCompressionLevel()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("CompressionLevel: "
                    + Integer.toString(n), false));
        }
        if ((n = niso.getColorSpace()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("ColorSpace: "
                    + integerRepresentation(n, NisoImageMetadata.COLORSPACE,
                            NisoImageMetadata.COLORSPACE_INDEX),
                    false));
        }
        if ((s = niso.getProfileName()) != null) {
            val.add(new DefaultMutableTreeNode("ProfileName: " + s, false));
        }
        if ((s = niso.getProfileURL()) != null) {
            val.add(new DefaultMutableTreeNode("ProfileURL: " + s, false));
        }
        int[] iarray = niso.getYCbCrSubSampling();
        if (iarray != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "YCbCrSubSampling"));
            val.add(nod);
            for (int i = 0; i < iarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Integer.toString(iarray[i]),
                        false));
            }
        }
        if ((n = niso.getYCbCrPositioning()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("YCbCrPositioning: "
                    + integerRepresentation(n,
                            NisoImageMetadata.YCBCR_POSITIONING),
                    false));
        }
        Rational[] rarray = niso.getYCbCrCoefficients();
        if (rarray != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "YCbCrCoefficients", true));
            val.add(nod);
            for (int i = 0; i < rarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(rarray[i].toString(), false));
            }
        }
        rarray = niso.getReferenceBlackWhite();
        if (rarray != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "ReferenceBlackWhite", true));
            val.add(nod);
            for (int i = 0; i < rarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(rarray[i].toString(), false));
            }
        }
        if ((n = niso.getSegmentType()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("YSegmentType: "
                    + integerRepresentation(n, NisoImageMetadata.SEGMENT_TYPE),
                    false));
        }
        long[] larray = niso.getStripOffsets();
        if (larray != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "StripOffsets", true));
            val.add(nod);
            for (int i = 0; i < larray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Long.toString(larray[i]),
                        false));
            }
        }
        long ln = niso.getRowsPerStrip();
        if (ln != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("RowsPerStrip: "
                    + Long.toString(ln), false));
        }
        if ((larray = niso.getStripByteCounts()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "StripByteCounts", true));
            val.add(nod);
            for (int i = 0; i < larray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Long.toString(larray[i]),
                        false));
            }
        }
        if ((ln = niso.getTileWidth()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("TileWidth: "
                    + Long.toString(ln)));
        }
        if ((ln = niso.getTileLength()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("TileLength: "
                    + Long.toString(ln)));
        }
        if ((larray = niso.getTileOffsets()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "TileOffsets", true));
            val.add(nod);
            for (int i = 0; i < larray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Long.toString(larray[i]),
                        false));
            }
        }
        if ((larray = niso.getTileByteCounts()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "TileByteCounts", true));
            val.add(nod);
            for (int i = 0; i < larray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Long.toString(larray[i]),
                        false));
            }
        }
        if ((n = niso.getPlanarConfiguration()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("PlanarConfiguration: "
                    + integerRepresentation(n,
                            NisoImageMetadata.PLANAR_CONFIGURATION),
                    false));
        }
        if ((s = niso.getImageIdentifier()) != null) {
            val.add(new DefaultMutableTreeNode("ImageIdentifier: " + s, false));
        }
        if ((s = niso.getImageIdentifierLocation()) != null) {
            val.add(new DefaultMutableTreeNode("ImageIdentifierLocation: " + s,
                    false));
        }
        if ((ln = niso.getFileSize()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode(
                    "FileSize: " + Long.toString(ln), false));
        }
        if ((n = niso.getChecksumMethod()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("ChecksumMethod: "
                    + integerRepresentation(n,
                            NisoImageMetadata.CHECKSUM_METHOD),
                    false));
        }
        if ((s = niso.getChecksumValue()) != null) {
            val.add(new DefaultMutableTreeNode("ChecksumValue: " + s, false));
        }
        if ((n = niso.getOrientation()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("Orientation: "
                    + integerRepresentation(n, NisoImageMetadata.ORIENTATION),
                    false));
        }
        if ((n = niso.getDisplayOrientation()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("DisplayOrientation: "
                    + integerRepresentation(n,
                            NisoImageMetadata.DISPLAY_ORIENTATION),
                    false));
        }
        if ((ln = niso.getXTargetedDisplayAR()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("XTargetedDisplayAR: "
                    + Long.toString(ln), false));
        }
        if ((ln = niso.getYTargetedDisplayAR()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("YTargetedDisplayAR: "
                    + Long.toString(ln), false));
        }
        if ((s = niso.getPreferredPresentation()) != null) {
            val.add(new DefaultMutableTreeNode("PreferredPresentation: " + s,
                    false));
        }
        if ((s = niso.getSourceType()) != null) {
            val.add(new DefaultMutableTreeNode("SourceType: " + s, false));
        }
        if ((s = niso.getImageProducer()) != null) {
            val.add(new DefaultMutableTreeNode("ImageProducer: " + s, false));
        }
        if ((s = niso.getHostComputer()) != null) {
            val.add(new DefaultMutableTreeNode("HostComputer: " + s, false));
        }
        if ((s = niso.getOS()) != null) {
            val.add(new DefaultMutableTreeNode("OperatingSystem: " + s, false));
        }
        if ((s = niso.getOSVersion()) != null) {
            val.add(new DefaultMutableTreeNode("OSVersion: " + s, false));
        }
        if ((s = niso.getDeviceSource()) != null) {
            val.add(new DefaultMutableTreeNode("DeviceSource: " + s, false));
        }
        if ((s = niso.getScannerManufacturer()) != null) {
            val.add(new DefaultMutableTreeNode("ScannerManufacturer: " + s,
                    false));
        }
        if ((s = niso.getScannerModelName()) != null) {
            val.add(new DefaultMutableTreeNode("ScannerModelName: " + s, false));
        }
        if ((s = niso.getScannerModelNumber()) != null) {
            val.add(new DefaultMutableTreeNode("ScannerModelNumber: " + s,
                    false));
        }
        if ((s = niso.getScannerModelSerialNo()) != null) {
            val.add(new DefaultMutableTreeNode("ScannerModelSerialNo: " + s,
                    false));
        }
        if ((s = niso.getScanningSoftware()) != null) {
            val.add(new DefaultMutableTreeNode("ScanningSoftware: " + s, false));
        }
        if ((s = niso.getScanningSoftwareVersionNo()) != null) {
            val.add(new DefaultMutableTreeNode("ScanningSoftwareVersionNo: "
                    + s, false));
        }
        double d = niso.getPixelSize();
        if (d != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("PixelSize: "
                    + Double.toString(d), false));
        }
        if ((d = niso.getXPhysScanResolution()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("XPhysScanResolution: "
                    + Double.toString(d), false));
        }
        if ((d = niso.getYPhysScanResolution()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("YPhysScanResolution: "
                    + Double.toString(d), false));
        }

        if ((s = niso.getDigitalCameraManufacturer()) != null) {
            val.add(new DefaultMutableTreeNode("DigitalCameraManufacturer: "
                    + s, false));
        }
        if ((s = niso.getDigitalCameraModelName()) != null) {
            val.add(new DefaultMutableTreeNode("DigitalCameraModelName: " + s,
                    false));
        }
        if ((s = niso.getDigitalCameraModelNumber()) != null) {
            val.add(new DefaultMutableTreeNode(
                    "DigitalCameraModelNumber: " + s, false));
        }
        if ((s = niso.getDigitalCameraModelSerialNo()) != null) {
            val.add(new DefaultMutableTreeNode("DigitalCameraModelSerialNo: "
                    + s, false));
        }
        if ((d = niso.getFNumber()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode(
                    "FNumber: " + Double.toString(d), false));
        }
        if ((d = niso.getExposureTime()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("ExposureTime: "
                    + Double.toString(d), false));
        }
        if ((n = niso.getExposureProgram()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("ExposureProgram: "
                    + Integer.toString(n), false));
        }
        if ((s = niso.getExifVersion()) != null) {
            val.add(new DefaultMutableTreeNode("ExifVersion: " + s, false));
        }
        Rational r;
        if ((r = niso.getBrightness()) != null) {
            val.add(new DefaultMutableTreeNode("Brightness: " + r.toString(),
                    false));
        }
        if ((r = niso.getExposureBias()) != null) {
            val.add(new DefaultMutableTreeNode("ExposureBias: " + r.toString(),
                    false));
        }

        double[] darray = niso.getSubjectDistance();
        if (darray != null) {
            DefaultMutableTreeNode nod = new DefaultMutableTreeNode(
                    "SubjectDistance", true);
            val.add(nod);
            for (int i = 0; i < darray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Double.toString(darray[i]),
                        false));
            }
        }
        if ((n = niso.getMeteringMode()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("MeteringMode: "
                    + Integer.toString(n), false));
        }
        if ((n = niso.getSceneIlluminant()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("SceneIlluminant: "
                    + Integer.toString(n), false));
        }
        if ((d = niso.getColorTemp()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("ColorTemp: "
                    + Double.toString(d), false));
        }
        if ((d = niso.getFocalLength()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("FocalLength: "
                    + Double.toString(d), false));
        }
        if ((n = niso.getFlash()) != NisoImageMetadata.NULL) {
            // First bit (0 = Flash did not fire, 1 = Flash fired)
            val.add(new DefaultMutableTreeNode("Flash: "
                    + integerRepresentation(n & 1, NisoImageMetadata.FLASH_20),
                    false));
        }
        if ((r = niso.getFlashEnergy()) != null) {
            val.add(new DefaultMutableTreeNode("FlashEnergy: " + r.toString(),
                    false));
        }
        if ((n = niso.getFlashReturn()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("FlashReturn: "
                    + integerRepresentation(n, NisoImageMetadata.FLASH_RETURN),
                    false));
        }
        if ((n = niso.getBackLight()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("BackLight: "
                    + integerRepresentation(n, NisoImageMetadata.BACKLIGHT),
                    false));
        }
        if ((d = niso.getExposureIndex()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("ExposureIndex: "
                    + Double.toString(d), false));
        }
        if ((n = niso.getAutoFocus()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("AutoFocus: "
                    + Integer.toString(n), false));
            // NisoImageMetadata.AUTOFOCUS
        }
        if ((d = niso.getXPrintAspectRatio()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("XPrintAspectRatio: "
                    + Double.toString(d), false));
        }
        if ((d = niso.getYPrintAspectRatio()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("YPrintAspectRatio: "
                    + Double.toString(d), false));
        }

        if ((n = niso.getSensor()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("Sensor: "
                    + integerRepresentation(n, NisoImageMetadata.SENSOR), false));
        }
        if ((s = niso.getDateTimeCreated()) != null) {
            val.add(new DefaultMutableTreeNode("DateTimeCreated: " + s, false));
        }
        if ((s = niso.getMethodology()) != null) {
            val.add(new DefaultMutableTreeNode("Methodology: " + s, false));
        }
        if ((n = niso.getSamplingFrequencyPlane()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("SamplingFrequencyPlane: "
                    + integerRepresentation(n,
                            NisoImageMetadata.SAMPLING_FREQUENCY_PLANE),
                    false));
        }
        if ((n = niso.getSamplingFrequencyUnit()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("SamplingFrequencyUnit: "
                    + integerRepresentation(n,
                            NisoImageMetadata.SAMPLING_FREQUENCY_UNIT),
                    false));
        }
        Rational rat = niso.getXSamplingFrequency();
        if (rat != null) {
            val.add(new DefaultMutableTreeNode("XSamplingFrequency: "
                    + rat.toString(), false));
        }
        rat = niso.getYSamplingFrequency();
        if (rat != null) {
            val.add(new DefaultMutableTreeNode("YSamplingFrequency: "
                    + rat.toString(), false));
        }
        if ((ln = niso.getImageWidth()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("ImageWidth: "
                    + Long.toString(ln), false));
        }
        if ((ln = niso.getImageLength()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("ImageLength: "
                    + Long.toString(ln), false));
        }
        if ((d = niso.getSourceXDimension()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("SourceXDimension: "
                    + Double.toString(d), false));
        }
        if ((n = niso.getSourceXDimensionUnit()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("SourceXDimensionUnit: "
                    + integerRepresentation(n,
                            NisoImageMetadata.SOURCE_DIMENSION_UNIT),
                    false));
        }
        if ((d = niso.getSourceYDimension()) != NisoImageMetadata.NILL) {
            val.add(new DefaultMutableTreeNode("SourceYDimension: "
                    + Double.toString(d), false));
        }
        if ((n = niso.getSourceYDimensionUnit()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("SourceYDimensionUnit: "
                    + integerRepresentation(n,
                            NisoImageMetadata.SOURCE_DIMENSION_UNIT),
                    false));
        }
        if ((iarray = niso.getBitsPerSample()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "BitsPerSample"));
            val.add(nod);
            for (int i = 0; i < iarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Integer.toString(iarray[i]),
                        false));
                // BitsPerSampleUnit Integer is assumed, because of BitsPerSample
                // According to the specification, it can also be float.
                // This is currently not supported by jHove
                nod.add(new DefaultMutableTreeNode("Integer"));
            }
        }
        if ((n = niso.getSamplesPerPixel()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("SamplesPerPixel: "
                    + Integer.toString(n), false));
        }
        if ((iarray = niso.getExtraSamples()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "ExtraSamples"));
            val.add(nod);
            for (int i = 0; i < iarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(integerRepresentation(
                        iarray[i], NisoImageMetadata.EXTRA_SAMPLES), false));
            }
        }
        if ((s = niso.getColormapReference()) != null) {
            val.add(new DefaultMutableTreeNode("ColormapReference: " + s));
        }
        if ((iarray = niso.getColormapBitCodeValue()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "ColormapBitCodeValue"));
            val.add(nod);
            for (int i = 0; i < iarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Integer.toString(iarray[i]),
                        false));
            }
        }
        if ((iarray = niso.getColormapRedValue()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "ColormapRedValue"));
            val.add(nod);
            for (int i = 0; i < iarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Integer.toString(iarray[i]),
                        false));
            }
        }
        if ((iarray = niso.getColormapGreenValue()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "ColormapGreenValue"));
            val.add(nod);
            for (int i = 0; i < iarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Integer.toString(iarray[i]),
                        false));
            }
        }
        if ((iarray = niso.getColormapBlueValue()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "ColormapBlueValue"));
            val.add(nod);
            for (int i = 0; i < iarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Integer.toString(iarray[i]),
                        false));
            }
        }
        if ((iarray = niso.getGrayResponseCurve()) != null) {
            DefaultMutableTreeNode nod = (new DefaultMutableTreeNode(
                    "GrayResponseCurve"));
            val.add(nod);
            for (int i = 0; i < iarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(Integer.toString(iarray[i]),
                        false));
            }
        }
        if ((n = niso.getGrayResponseUnit()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("GrayResponseUnit: "
                    + Integer.toString(n), false));
        }
        r = niso.getWhitePointXValue();
        if (r != null) {
            val.add(new DefaultMutableTreeNode("WhitePointXValue: "
                    + r.toString(), false));
        }
        if ((r = niso.getWhitePointYValue()) != null) {
            val.add(new DefaultMutableTreeNode("WhitePointYValue: "
                    + r.toString(), false));
        }
        if ((r = niso.getPrimaryChromaticitiesRedX()) != null) {
            val.add(new DefaultMutableTreeNode("PrimaryChromaticitiesRedX: "
                    + r.toString(), false));
        }
        if ((r = niso.getPrimaryChromaticitiesRedY()) != null) {
            val.add(new DefaultMutableTreeNode("PrimaryChromaticitiesRedY: "
                    + r.toString(), false));
        }
        if ((r = niso.getPrimaryChromaticitiesGreenX()) != null) {
            val.add(new DefaultMutableTreeNode("PrimaryChromaticitiesGreenX: "
                    + r.toString(), false));
        }
        if ((r = niso.getPrimaryChromaticitiesGreenY()) != null) {
            val.add(new DefaultMutableTreeNode("PrimaryChromaticitiesGreenY: "
                    + r.toString(), false));
        }
        if ((r = niso.getPrimaryChromaticitiesBlueX()) != null) {
            val.add(new DefaultMutableTreeNode("PrimaryChromaticitiesBlueX: "
                    + r.toString()));
        }
        if ((r = niso.getPrimaryChromaticitiesBlueY()) != null) {
            val.add(new DefaultMutableTreeNode("PrimaryChromaticitiesBlueY: "
                    + r.toString()));
        }
        if ((n = niso.getTargetType()) != NisoImageMetadata.NULL) {
            val.add(new DefaultMutableTreeNode("TargetType: "
                    + integerRepresentation(n, NisoImageMetadata.TARGET_TYPE),
                    false));
        }
        if ((s = niso.getTargetIDManufacturer()) != null) {
            val.add(new DefaultMutableTreeNode("TargetIDManufacturer: " + s,
                    false));
        }
        if ((s = niso.getTargetIDName()) != null) {
            val.add(new DefaultMutableTreeNode("TargetIDName: " + s, false));
        }
        if ((s = niso.getTargetIDNo()) != null) {
            val.add(new DefaultMutableTreeNode("TargetIDNo: " + s, false));
        }
        if ((s = niso.getTargetIDMedia()) != null) {
            val.add(new DefaultMutableTreeNode("TargetIDMedia: " + s, false));
        }
        if ((s = niso.getImageData()) != null) {
            val.add(new DefaultMutableTreeNode("ImageData: " + s, false));
        }
        if ((s = niso.getPerformanceData()) != null) {
            val.add(new DefaultMutableTreeNode("PerformanceData: " + s, false));
        }
        if ((s = niso.getProfiles()) != null) {
            val.add(new DefaultMutableTreeNode("Profiles: " + s, false));
        }
        if ((s = niso.getDateTimeProcessed()) != null) {
            val.add(new DefaultMutableTreeNode("DateTimeProcessed: " + s, false));
        }
        if ((s = niso.getSourceData()) != null) {
            val.add(new DefaultMutableTreeNode("SourceData: " + s, false));
        }
        if ((s = niso.getProcessingAgency()) != null) {
            val.add(new DefaultMutableTreeNode("ProcessingAgency: " + s, false));
        }
        if ((s = niso.getProcessingSoftwareName()) != null) {
            val.add(new DefaultMutableTreeNode("ProcessingSoftwareName: " + s,
                    false));
        }
        if ((s = niso.getProcessingSoftwareVersion()) != null) {
            val.add(new DefaultMutableTreeNode("ProcessingSoftwareVersion: "
                    + s, false));
        }
        String[] sarray = niso.getProcessingActions();
        if (sarray != null) {
            DefaultMutableTreeNode nod = new DefaultMutableTreeNode(
                    "ProcessingActions", true);
            val.add(nod);
            for (int i = 1; i < sarray.length; i++) {
                nod.add(new DefaultMutableTreeNode(sarray[i], false));
            }
        }
        return val;
    }

    /*
     * Return the string equivalent of an integer if raw output isn't selected,
     * or its literal representation if it is.
     */
    private String integerRepresentation(int n, String[] labels) {
        if (_rawOutput) {
            return Integer.toString(n);
        }
        try {
            return labels[n];
        } catch (Exception e) {
            return Integer.toString(n);
        }
    }

    private String integerRepresentation(int n, String[] labels, int[] index) {
        if (_rawOutput) {
            return Integer.toString(n);
        }
        try {
            int idx = -1;
            for (int i = 0; i < index.length; i++) {
                if (n == index[i]) {
                    idx = i;
                    break;
                }
            }
            if (idx > -1) {
                return labels[idx];
            }
        } catch (Exception e) {
        }
        // If we don't get a match, or do get an exception
        return Integer.toString(n);
    }

    /**
     * This method returns a member of a property list based on it's
     * PropertyType
     *
     * @param ptyp
     *                       the propertytype
     * @param item
     *                       the item of the list
     * @param allowsChildren
     * @return
     */
    private DefaultMutableTreeNode getDefaultMutableTreeNode(PropertyType ptyp,
            Object item, boolean allowsChildren) {

        DefaultMutableTreeNode itemNode;

        if (null == ptyp) {
            // Simple objects just need a leaf.
            itemNode = (new DefaultMutableTreeNode(item, allowsChildren));
        } else
            switch (ptyp) {
                case PROPERTY:
                    itemNode = (propToNode((Property) item));
                    break;
                case NISOIMAGEMETADATA:
                    itemNode = (nisoToNode((NisoImageMetadata) item));
                    break;
                default:
                    // Simple objects just need a leaf.
                    itemNode = (new DefaultMutableTreeNode(item, allowsChildren));
                    break;
            }

        return itemNode;
    }

    /**
     * Method to add an array to a node
     *
     * @param <E>
     *              generic method, can be used for arrays of different types
     * @param node
     *              the node to add the elements of the array
     * @param array
     *              the array to be added to the node
     */
    private <E> void addToNode(DefaultMutableTreeNode node, E[] array) {
        for (E element : array) {
            if (element instanceof Property) {
                node.add(propToNode((Property) element));
            } else {
                node.add(new DefaultMutableTreeNode(element));
            }
        }
    }
}
