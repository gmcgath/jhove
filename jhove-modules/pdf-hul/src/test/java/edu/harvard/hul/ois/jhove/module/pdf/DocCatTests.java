package edu.harvard.hul.ois.jhove.module.pdf;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.module.PdfModule;
import edu.harvard.hul.ois.jhove.module.TestUtils;

/**
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1 Created 13 Mar 2018:18:17:45
 */
public class DocCatTests {

    private static final String pdfResourcePath = "/edu/harvard/hul/ois/jhove/module/pdf/";
    private static final String docCatResourcePath = pdfResourcePath + "doc-cat/";

    private static final String minimalPdfPath = pdfResourcePath
            + "T00_000_minimal-valid.pdf";
    private static final String oneByteMissingPath = pdfResourcePath
            + "corruptionOneByteMissing.pdf";

    private static final String catNoCat = docCatResourcePath
            + "T02-01_001_document-catalog-No-document-catalog.pdf";
    private static final String catWrongObjNumberPath = docCatResourcePath
            + "T02-01_002_document-catalog-wrong-object-number.pdf";
    private static final String catPagRefMissPath = docCatResourcePath
            + "T02-01_003_document-catalog-indirecte-pages-reference-missing.pdf";
    private static final String catPageRefIncorrectPath = docCatResourcePath
            + "T02-01_004_document-catalog-incorrect-pages-reference.pdf";
    private static final String catTypeKyMissPath = docCatResourcePath
            + "T02-01_005_document-catalog-type-key-missing.pdf";
    private static final String catTypeValNotCatalogPath = docCatResourcePath
            + "T02-01_006_document-catalog-wrong-type-key.pdf";
    private static final String catTypeKyValPairMissPath = docCatResourcePath
            + "T02-01_007_document-catalog-type-key-value-pair-missing.pdf";

    private PdfModule module;

    @Before
    public void setUp() throws Exception {
        this.module = new PdfModule();
        JhoveBase je = new JhoveBase();
        this.module.setBase(je);
    }

    /**
     * Test method for {@link PdfModule}.
     */
    @Test
    public final void testValidCatType() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, minimalPdfPath,
                RepInfo.TRUE, RepInfo.TRUE, null);
    }

    /**
     * Test method for {@link PdfModule}.
     */
    @Test
    public final void testNoCat() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, catNoCat, RepInfo.FALSE,
                RepInfo.FALSE, MessageConstants.PDF_HUL_86.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     */
    @Test
    public final void testCatWrongObjNum() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, catWrongObjNumberPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_140.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     */
    @Test
    public final void testPagRefMiss() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, catPagRefMissPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_38.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     */
    @Test
    public final void testPageRefIncorrect() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, catPageRefIncorrectPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_96.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     */
    @Test
    public final void testCatTypeKeyMiss() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, catTypeKyMissPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_85.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     */
    @Test
    public final void testCatTypeVal() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, catTypeValNotCatalogPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_141.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     */
    @Test
    public final void testCatTypeKeyValMiss() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, catTypeKyValPairMissPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_38.getId());
    }

    /**
     * Test method for {@link PdfModule}.
     */
    @Test
    public final void testOneByteMiss() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, oneByteMissingPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_140.getId());
    }
}
