package edu.harvard.hul.ois.jhove.module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.Diagnosis;
import org.jwat.common.Diagnostics;
import org.jwat.common.InputStreamNoSkip;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipReader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

import edu.harvard.hul.ois.jhove.Agent;
import edu.harvard.hul.ois.jhove.Agent.Builder;
import edu.harvard.hul.ois.jhove.AgentType;
import edu.harvard.hul.ois.jhove.Document;
import edu.harvard.hul.ois.jhove.DocumentType;
import edu.harvard.hul.ois.jhove.ErrorMessage;
import edu.harvard.hul.ois.jhove.ExternalSignature;
import edu.harvard.hul.ois.jhove.Identifier;
import edu.harvard.hul.ois.jhove.IdentifierType;
import edu.harvard.hul.ois.jhove.InfoMessage;
import edu.harvard.hul.ois.jhove.JhoveException;
import edu.harvard.hul.ois.jhove.ModuleBase;
import edu.harvard.hul.ois.jhove.Property;
import edu.harvard.hul.ois.jhove.PropertyArity;
import edu.harvard.hul.ois.jhove.PropertyType;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.Signature;
import edu.harvard.hul.ois.jhove.SignatureType;
import edu.harvard.hul.ois.jhove.SignatureUseType;
import edu.harvard.hul.ois.jhove.module.warc.MessageConstants;
import edu.harvard.hul.ois.jhove.module.warc.WarcRecordProperties;

/**
 * JHOVE module for identifying, validating and characterizing WARC files.
 * Ported from the JHOVE2 WARC module and based on the JWAT-tool, both
 * created by nicl@kb.dk (nclarkekb@git).
 *
 * This is a non-recursive validation. It only validates the WARC file format
 * and WARC headers, not the actual payload of the WARC records.
 *
 * @author jolf@kb.dk
 */
public class WarcModule extends ModuleBase {

    /*------------ MODULE DEFINITIONS ---------------*/
    private static final Agent KB_AGENT = new Builder(
            "Royal Library of Denmark", AgentType.STANDARD)
            .address("Søren Kierkegaards Plads 1, 1219 København K, Denmark")
            .fax("+45 3393 2218")
            .web("http://kb.dk").build();

    private static final String NAME = "WARC-kb";
    private static final String RELEASE = "1.2";
    private static final int[] DATE = { 2022, 03, 16 };
    private static final String[] FORMAT = {
            "WARC", "WARC, Web ARChive file format"
    };
    private static final String COVERAGE = "WARC, 28500:2009";
    private static final String[] MIMETYPE = { "application/warc", "application/warc-fields" };
    private static final String WELLFORMED = "";
    private static final String VALIDITY = "The file is well-formed";
    private static final String REPINFO = "";
    private static final String NOTE = "";
    private static final String RIGHTS = "Copyright 2015 by The Royal Library of Denmark. " +
            "Released under the GNU Lesser General Public License.";

    /* DEFAULT VALUES */
    private static final Boolean DEFAULT_COMPUTE_BLOCK_DIGEST = Boolean.TRUE;
    private static final String DEFAULT_BLOCK_DIGEST_ALGORITHM = "sha1";
    private static final String DEFAULT_BLOCK_DIGEST_ENCODING = "base32";
    private static final Boolean DEFAULT_COMPUTE_PAYLOAD_DIGEST = Boolean.TRUE;
    private static final String DEFAULT_PAYLOAD_DIGEST_ALGORITHM = "sha1";
    private static final String DEFAULT_PAYLOAD_DIGEST_ENCODING = "base32";
    private static final Boolean DEFAULT_STRICT_TARGET_URI_VALIDATION = Boolean.FALSE;
    private static final Boolean DEFAULT_STRICT_URI_VALIDATION = Boolean.FALSE;

    /*-------------- Local variables --------------*/

    private boolean bComputeBlockDigest;
    private String blockDigestAlgorithm;
    private String blockDigestEncoding;

    private boolean bComputePayloadDigest;
    private String payloadDigestAlgorithm;
    private String payloadDigestEncoding;

    private boolean bStrictTargetUriValidation;
    private boolean bStrictUriValidation;

    /**
     * Map of the WARC record versions and their count.
     * Used for reporting the most seen version, as the version for the WARC file.
     */
    private Map<String, Integer> versions;
    /**
     * List of Property elements for the records of the WARC-file.
     * Each Property contains a map of all properties for a given record.
     */
    private List<Property> recordProperties;

    /**
     * Constructor.
     */
    public WarcModule() {
        super(NAME, RELEASE, DATE, FORMAT, COVERAGE, MIMETYPE, WELLFORMED,
                VALIDITY, REPINFO, NOTE, RIGHTS, false);
        setVendorAndSpecification();
        initialiseVariables();
    }

    /**
     * Sets the vendor and specification for this module.
     */
    private void setVendorAndSpecification() {
        _vendor = KB_AGENT;

        Document doc = new Document("WARC (Web ARChive) file format",
                DocumentType.WEB);
        // Should probably have IIPC and others as authors
        doc.setPublisher(Agent.newIsoInstance());
        doc.setDate("2009");
        doc.setIdentifier(new Identifier("28500:2009",
                IdentifierType.ISO));
        _specification.add(doc);

        // Add optional external signatures (.warc or .warc.gz)
        Signature sig = new ExternalSignature(".warc", SignatureType.EXTENSION,
                SignatureUseType.OPTIONAL);
        _signature.add(sig);
        sig = new ExternalSignature(".warc.gz", SignatureType.EXTENSION,
                SignatureUseType.OPTIONAL, "when compressed");
        _signature.add(sig);
    }

    /**
     * Initializes the variables.
     */
    private void initialiseVariables() {
        versions = new HashMap<>();
        recordProperties = new ArrayList<>();

        bComputeBlockDigest = DEFAULT_COMPUTE_BLOCK_DIGEST;
        blockDigestAlgorithm = DEFAULT_BLOCK_DIGEST_ALGORITHM;
        blockDigestEncoding = DEFAULT_BLOCK_DIGEST_ENCODING;

        bComputePayloadDigest = DEFAULT_COMPUTE_PAYLOAD_DIGEST;
        payloadDigestAlgorithm = DEFAULT_PAYLOAD_DIGEST_ALGORITHM;
        payloadDigestEncoding = DEFAULT_PAYLOAD_DIGEST_ENCODING;

        bStrictTargetUriValidation = DEFAULT_STRICT_TARGET_URI_VALIDATION;
        bStrictUriValidation = DEFAULT_STRICT_URI_VALIDATION;
    }

    /**
     * Reset parameter settings.
     * Returns to a default state without any parameters.
     */
    @Override
    public void resetParams() {
        initialiseVariables();
    }

    @Override
    public void checkSignatures(File file,
            InputStream stream,
            RepInfo info)
            throws IOException {
        info.setFormat(_format[0]);
        info.setMimeType(_mimeType[0]);
        info.setModule(this);

        ByteCountingPushBackInputStream pbin = new ByteCountingPushBackInputStream(stream,
                GzipReader.DEFAULT_INPUT_BUFFER_SIZE);
        // First try warc uncompressed
        boolean checkIsWarc = WarcReaderFactory.isWarcFile(pbin);
        if (checkIsWarc) {
            info.setSigMatch(_name);
            return;
        }
        // Then try warc compressed
        boolean checkIsGzip = GzipReader.isGzipped(pbin);
        if (checkIsGzip) {
            info.setSigMatch(_name);
            return;
        }
        // Not a warc or a gzip
        info.setWellFormed(false);
    }

    @Override
    public void checkSignatures(File file,
            RandomAccessFile raf,
            RepInfo info) throws IOException {
        try (InputStream stream = new RandomAccessFileInputStream(raf)) {
            checkSignatures(file, stream, info);
        }
    }

    @Override
    public void parse(RandomAccessFile file, RepInfo info) throws IOException {
        try (InputStream stream = new RandomAccessFileInputStream(file)) {
            parse(stream, info, 0);
        }
    }

    @Override
    public int parse(InputStream stream, RepInfo info, int parseIndex) throws IOException {
        WarcReader reader = WarcReaderFactory.getReader(new InputStreamNoSkip(stream), 8192);
        try {
            info.setFormat(_format[0]);
            info.setMimeType(_mimeType[0]);
            info.setModule(this);

            setReaderOptions(reader);
            parseRecords(reader);

            info.setValid(reader.isCompliant());
            info.setWellFormed(reader.isCompliant());

            reportResults(reader, info);

            if (reader.isCompliant()) {
                info.setSigMatch(_name);
            }
        } catch (JhoveException e) {
            info.setMessage(new ErrorMessage(MessageConstants.WARC_KB_1,
                    String.format("%s: %s", e.getClass().getName(), e.getMessage())));
            info.setValid(false);
            info.setWellFormed(false);
        } finally {
            if (reader != null) {
                reader.close();
                reader = null;
            }
        }
        return 0;
    }

    /**
     * Set digest options for WARC reader.
     * 
     * @param reader WARC reader instance
     */
    protected void setReaderOptions(WarcReader reader) throws JhoveException {
        reader.setBlockDigestEnabled(bComputeBlockDigest);
        reader.setPayloadDigestEnabled(bComputePayloadDigest);
        if (!reader.setBlockDigestAlgorithm(blockDigestAlgorithm)) {
            throw new JhoveException(MessageConstants.ERR_BLOCK_DIGEST_INVALID + blockDigestAlgorithm);
        }
        if (!reader.setPayloadDigestAlgorithm(payloadDigestAlgorithm)) {
            throw new JhoveException(MessageConstants.ERR_PAYLOAD_DIGEST_INVALID + payloadDigestAlgorithm);
        }
        reader.setBlockDigestEncoding(blockDigestEncoding);
        reader.setPayloadDigestEncoding(payloadDigestEncoding);
        if (bStrictTargetUriValidation) {
            reader.setWarcTargetUriProfile(UriProfile.RFC3986);
        } else {
            reader.setWarcTargetUriProfile(UriProfile.RFC3986_ABS_16BIT_LAX);
        }
        if (bStrictUriValidation) {
            reader.setUriProfile(UriProfile.RFC3986);
        } else {
            reader.setUriProfile(UriProfile.RFC3986_ABS_16BIT_LAX);
        }
    }

    /**
     * Parse WARC records. Parsing should be straight forward with all records
     * accessible through the same source.
     * 
     * @param reader WARC reader used to parse records
     * @throws IOException    if an IO error occurs while processing
     * @throws JhoveException if a serious problem needs to be reported
     */
    protected void parseRecords(WarcReader reader) throws IOException, JhoveException {
        if (reader != null) {
            WarcRecord warcRecord;
            while ((warcRecord = reader.getNextRecord()) != null) {
                processRecord(warcRecord);
                reader.diagnostics.addAll(warcRecord.diagnostics);
            }
        } else {
            throw new JhoveException(MessageConstants.ERR_RECORD_NULL);
        }
    }

    /**
     * Process a WARC record.
     * Does not characterize the record payload.
     * 
     * @param record WARC record from WARC reader
     * @throws IOException if an IO error occurs while processing
     */
    protected void processRecord(WarcRecord record) throws IOException {
        if (record.header.bValidVersionFormat) {
            Integer count = versions.get(record.header.versionStr);
            if (count == null) {
                count = 0;
            }
            ++count;
            versions.put(record.header.versionStr, count);
        }

        WarcRecordProperties properties = new WarcRecordProperties(record);
        Property p = new Property("Record", PropertyType.STRING, PropertyArity.MAP, properties.getProperties());

        recordProperties.add(p);

        record.close();
    }

    /**
     * Report the results of the characterization.
     * 
     * @param reader  The WARC reader, which has read the WARC-file.
     * @param repInfo The representation info, where to report the results.
     */
    private void reportResults(WarcReader reader, RepInfo repInfo) {
        JwatJhoveIdMinter minter = JwatJhoveIdMinter.getInstance(NAME);
        Diagnostics diagnostics = reader.diagnostics;
        if (diagnostics.hasErrors()) {
            for (Diagnosis d : diagnostics.getErrors()) {
                repInfo.setMessage(new ErrorMessage(minter.mint(d)));
            }
            repInfo.setConsistent(false);
        }
        if (diagnostics.hasWarnings()) {
            // Report warnings on source object.
            for (Diagnosis d : diagnostics.getWarnings()) {
                repInfo.setMessage(new InfoMessage(minter.mint(d)));
            }
        }

        int maxCount = -1;
        for (Entry<String, Integer> e : versions.entrySet()) {
            if (e.getValue() > maxCount) {
                maxCount = e.getValue();
                repInfo.setVersion(e.getKey());
            }

            _features.add(e.getValue() + " WARC records of version " + e.getKey());
        }

        repInfo.setProperty(new Property("Records", PropertyType.PROPERTY, PropertyArity.LIST, recordProperties));
        repInfo.setSize(reader.getConsumed());
    }
}
