package edu.harvard.hul.ois.jhove.module;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.Diagnosis;
import org.jwat.common.Diagnostics;
import org.jwat.common.InputStreamNoSkip;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;

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
import edu.harvard.hul.ois.jhove.module.gzip.GzipEntryProperties;
import edu.harvard.hul.ois.jhove.module.gzip.MessageConstants;

/**
 * JHOVE module for identifying, validating and characterizing GZIP files.
 * Ported from the JHOVE2 GZIP module, created by lbihanic, selghissassi, nicl
 * 
 * JHOVE2 GZip module. This module parses and validates GZip files
 * in compliance with
 * <a href="http://www.ietf.org/rfc/rfc1952.txt">RFC 1952</a> (GZIP
 * file format specification version 4.3) and supports multiple member
 * GZIP files.
 * <p>
 * This is a non-recursive validation. It only validates the GZIP file format,
 * not the actual content within the WARC records.
 * 
 * @author jolf@kb.dk
 */
public class GzipModule extends ModuleBase {
    /*------------ MODULE DEFINITIONS ---------------*/
    private static final Agent KB_AGENT = new Builder(
            "Royal Library of Denmark", AgentType.STANDARD)
            .address("Søren Kierkegaards Plads 1, 1219 København K, Denmark")
            .fax("+45 3393 2218")
            .web("http://kb.dk").build();

    private static final String NAME = "GZIP-kb";
    private static final String RELEASE = "0.3";
    private static final int[] DATE = { 2023, 03, 16 };
    private static final String[] FORMAT = { "GZIP" };
    private static final String COVERAGE = "GZIP, https://tools.ietf.org/html/rfc1952";
    private static final String[] MIMETYPE = { "application/gzip", "application/x-gzip" };
    private static final String WELLFORMED = "";
    private static final String VALIDITY = "The file is well-formed";
    private static final String REPINFO = "";
    private static final String NOTE = "";
    private static final String RIGHTS = "Copyright 2015 by The Royal Library of Denmark. " +
            "Released under the GNU Lesser General Public License.";

    private static final String EXTENSION = ".gz";

    /**
     * List of Property elements for the entry of the GZIP-file.
     * Each Property contains a map of all properties for a given entry.
     */
    private List<Property> entryProperties;

    /**
     * Constructor.
     */
    public GzipModule() {
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

        Document doc = new Document(FORMAT[0],
                DocumentType.RFC);
        // Should probably have IIPC and others as authors
        Agent ietfAgent = new Agent.Builder("IETF", AgentType.STANDARD).web(
                "http://www.ietf.org").build();
        doc.setPublisher(ietfAgent);
        doc.setDate("1996");
        doc.setIdentifier(new Identifier("https://www.ietf.org/rfc/rfc1952.txt",
                IdentifierType.RFC));
        _specification.add(doc);

        // Add optional external signature (.gz)
        Signature sig = new ExternalSignature(EXTENSION, SignatureType.EXTENSION,
                SignatureUseType.OPTIONAL);
        _signature.add(sig);

    }

    /**
     * Initializes the variables.
     */
    private void initialiseVariables() {
        entryProperties = new ArrayList<>();
    }

    /**
     * Resets parameter settings.
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

        boolean checkIsGzip = GzipReader
                .isGzipped(new ByteCountingPushBackInputStream(stream, GzipReader.DEFAULT_INPUT_BUFFER_SIZE));
        if (checkIsGzip) {
            info.setSigMatch(_name);
        } else {
            info.setWellFormed(false);
        }

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
    public void parse(RandomAccessFile file, RepInfo info) {
        InputStream stream = new RandomAccessFileInputStream(file);
        parse(stream, info, 0);
    }

    @Override
    public int parse(InputStream stream, RepInfo info, int parseIndex) {

        try (GzipReader reader = new GzipReader(new InputStreamNoSkip(stream), 8192)) {
            info.setFormat(_format[0]);
            info.setVersion("4.3"); // Is it really version 4.3?
            info.setMimeType(_mimeType[0]);
            info.setModule(this);

            parseRecords(reader);

            info.setValid(reader.isCompliant());
            info.setWellFormed(reader.isCompliant());

            reportResults(reader, info);

            if (reader.isCompliant()) {
                info.setSigMatch(_name);
            }
        } catch (Exception e) {
            info.setMessage(new ErrorMessage(MessageConstants.GZIP_KB_1,
                    String.format("%s: %s", e.getClass().getName(), e.getMessage())));
            info.setValid(false);
            info.setWellFormed(false);
        }
        return 0;
    }

    /**
     * Parses GZIP entries. Parsing should be straight forward with all records
     * accessible through the same source.
     * 
     * @param reader GZIP reader used to parse records
     * @throws EOFException   if EOF occurs prematurely
     * @throws IOException    if an IO error occurs while processing
     * @throws JhoveException if a serious problem needs to be reported
     */
    protected void parseRecords(GzipReader reader) throws EOFException, IOException, JhoveException {
        if (reader != null) {
            GzipEntry entry;
            while ((entry = reader.getNextEntry()) != null) {
                processEntry(entry);
                reader.diagnostics.addAll(entry.diagnostics);
            }
        } else {
            throw new JhoveException(MessageConstants.ERR_RECORD_NULL);
        }
    }

    /**
     * Processes a GZIP entry.
     * Extracts all the properties of the entry into a map, and puts this map on the
     * list.
     * 
     * @param entry GZIP entry from GZIP reader
     * @throws EOFException if EOF occurs prematurely
     * @throws IOException  if an IO error occurs while processing
     */
    protected void processEntry(GzipEntry entry) throws EOFException, IOException {
        GzipEntryProperties properties = new GzipEntryProperties(entry);
        Property p = new Property("Record", PropertyType.STRING, PropertyArity.MAP, properties.getProperties());

        entryProperties.add(p);

        entry.close();
    }

    /**
     * Reports the results of the characterization.
     * 
     * @param reader  The GZIP reader, which has read the GZIP-file.
     * @param repInfo The representation info, where to report the results.
     */
    private void reportResults(GzipReader reader, RepInfo repInfo) {
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
        repInfo.setProperty(new Property("Records", PropertyType.PROPERTY, PropertyArity.LIST, entryProperties));
        repInfo.setSize(reader.getConsumed());
    }
}
