package edu.harvard.hul.ois.jhove.module.pdf;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.module.PdfModule;
import edu.harvard.hul.ois.jhove.module.TestUtils;

/**
 * Tests for the {@link PdfHeader} class and for JHOVE's PDF Header validation
 * capabilities.
 *
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1 Created 13 Mar 2018:11:28:10
 */
public class HeaderTests {

    private static final String pdfResourcePath = "/edu/harvard/hul/ois/jhove/module/pdf/";
    private static final String headerResourcePath = pdfResourcePath + "header/";
    private static final String minimalPdfPath = pdfResourcePath
            + "T00_000_minimal-valid.pdf";

    private static final String invalidMajorPath = headerResourcePath
            + "T01_001_header-invalid-major-version.pdf";
    private static final String invalidMinorPath = headerResourcePath
            + "T01_002_header-invalid-minor-version.pdf";
    private static final String noMinorPath = headerResourcePath
            + "T01_003_header-no-minor-version.pdf";
    private static final String noHeaderDashPath = headerResourcePath
            + "T01_004_header_invalid-syntax-no-dash.pdf";
    private static final String invalidSyntaxRepPath = headerResourcePath
            + "T01_005_header-invalid-syntax-replace-char.pdf";
    private static final String invalidSyntaxNoPdfPath = headerResourcePath
            + "T01_006_header-invalid-syntax-no-pdf.pdf";
    private static final String noVersionInfoPath = headerResourcePath
            + "T01_007_header-no-version-info.pdf";

    private PdfModule module;

    @Before
    public void setUp() throws Exception {
        this.module = new PdfModule();
        JhoveBase je = new JhoveBase();
        this.module.setBase(je);
    }

    /**
     * Test method for {@link PdfHeader}.
     * Ensures that a valid PDF passes.
     */
    @Test
    public final void testMinorVersion() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, minimalPdfPath,
                RepInfo.TRUE, RepInfo.TRUE);
    }

    /**
     * Test method for {@link PdfModule}.
     * Ensures that a file with a major version &gt; 1 (%PDF-2.4) is not
     * well-formed.
     */
    @Test
    public final void testInvalidMajorVersion() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, invalidMajorPath,
                RepInfo.TRUE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_148.getId());
    }

    /**
     * Test method for {@link PdfHeader}.
     * Ensures that a file with a minor version &gt; 7 (%PDF-1.9) is invalid.
     */
    @Test
    public final void testInvalidMinorVersion() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, invalidMinorPath,
                RepInfo.TRUE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_148.getId());
    }

    /**
     * Test method for {@link PdfHeader#getVersionString()}.
     * Ensures that a file missing a minor version (%PDF-14) is not well-formed.
     */
    @Test
    public final void testNoMinor() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, noMinorPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_155.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     * Ensures that a file with no dash in header (%PDF1.4) is invalid.
     */
    @Test
    public final void testNoheaderDash() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, noHeaderDashPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_137.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     * Ensures that a file with no dash replaced by period in
     * header (%PDF.1.4) is not well-formed.
     */
    @Test
    public final void testInvalidSyntaxRep() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, invalidSyntaxRepPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_137.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     * Ensures that a file with no PDF in header (%1.4) is not well-formed.
     */
    @Test
    public final void testInvalidSyntaxNoPdf() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, invalidSyntaxNoPdfPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_137.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     * Ensures that a file missing a header is not well-formed.
     */
    @Test
    public final void testNoVersionInfo() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, noVersionInfoPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_137.getId());
    }
}
