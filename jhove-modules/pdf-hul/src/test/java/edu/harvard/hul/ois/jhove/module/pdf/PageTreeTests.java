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
 *
 * @version 0.1
 * 
 *          Created 19 Mar 2018:02:03:50
 */
public class PageTreeTests {

    private static final String pdfResourcePath = "/edu/harvard/hul/ois/jhove/module/pdf/";
    private static final String pageTreeResourcePath = pdfResourcePath + "page-tree/";

    private static final String minimalPdfPath = pdfResourcePath
            + "T00_000_minimal-valid.pdf";

    private static final String noPageTreeNodePath = pageTreeResourcePath
            + "T02-02_001_no-page-tree-node.pdf";
    private static final String rcrsPageTreeKidsPath = pageTreeResourcePath
            + "T02-02_002_page-tree-kids-links-recursive.pdf";
    private static final String diffPageTreeKidsPath = pageTreeResourcePath
            + "T02-02_003_page-tree-different-kids.pdf";
    private static final String ntExstPageTreeChldPath = pageTreeResourcePath
            + "T02-02_004_page-tree-non-existing-object-as-kid.pdf";
    private static final String noPageTreeKidsPath = pageTreeResourcePath
            + "T02-02_005_page-tree-no-kids.pdf";
    private static final String noTypePageTreePath = pageTreeResourcePath
            + "T02-02_006_page-tree-no-type.pdf";
    private static final String wrngPageTreeCountPath = pageTreeResourcePath
            + "T02-02_007_page-tree-wrong-count.pdf";
    private static final String noPageTreeCountPath = pageTreeResourcePath
            + "T02-02_008_page-tree-node-no-count.pdf";
    private static final String wrngPageTreeTypePath = pageTreeResourcePath
            + "T02-02_009_page-tree-wrong-type.pdf";
    private PdfModule module;

    @Before
    public void setUp() throws Exception {
        this.module = new PdfModule();
        JhoveBase je = new JhoveBase();
        this.module.setBase(je);
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testValidCatType() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, minimalPdfPath,
                RepInfo.TRUE, RepInfo.TRUE, null);
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testNoPageTreeNode() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, noPageTreeNodePath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_38.getId());
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testRcrsPageTreeKids() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, rcrsPageTreeKidsPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_32.getId());
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testDiffPageTreeKids() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, diffPageTreeKidsPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_33.getId());
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testNtExstPageTreeChldPath() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, ntExstPageTreeChldPath,
                RepInfo.TRUE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_147.getId());
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testNoPageTreeKids() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, noPageTreeKidsPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_38.getId());
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testNoTypePageTree() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, noTypePageTreePath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_38.getId());
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testWrngPageTreeCount() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, wrngPageTreeCountPath,
                RepInfo.TRUE, RepInfo.TRUE, null);
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testNoPageTreeCount() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, noPageTreeCountPath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_38.getId());
    }

    /**
     * Test method for {@link PdfModule#getCatalogDict()}.
     */
    @Test
    public final void testWrngPageTreeType() throws URISyntaxException {
        TestUtils.testValidateResource(this.module, wrngPageTreeTypePath,
                RepInfo.FALSE, RepInfo.FALSE,
                MessageConstants.PDF_HUL_146.getId());
    }
}
