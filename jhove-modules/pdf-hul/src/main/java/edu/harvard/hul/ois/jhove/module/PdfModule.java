/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2003-2007 by JSTOR and the President and Fellows of Harvard College
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 **********************************************************************/

package edu.harvard.hul.ois.jhove.module;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.harvard.hul.ois.jhove.Agent;
import edu.harvard.hul.ois.jhove.Document;
import edu.harvard.hul.ois.jhove.DocumentType;
import edu.harvard.hul.ois.jhove.ErrorMessage;
import edu.harvard.hul.ois.jhove.ExternalSignature;
import edu.harvard.hul.ois.jhove.Identifier;
import edu.harvard.hul.ois.jhove.IdentifierType;
import edu.harvard.hul.ois.jhove.InfoMessage;
import edu.harvard.hul.ois.jhove.InternalSignature;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.ModuleBase;
import edu.harvard.hul.ois.jhove.NisoImageMetadata;
import edu.harvard.hul.ois.jhove.Property;
import edu.harvard.hul.ois.jhove.PropertyArity;
import edu.harvard.hul.ois.jhove.PropertyType;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.SignatureType;
import edu.harvard.hul.ois.jhove.SignatureUseType;
import edu.harvard.hul.ois.jhove.XMPHandler;
import edu.harvard.hul.ois.jhove.messages.JhoveMessage;
import edu.harvard.hul.ois.jhove.messages.JhoveMessages;
import edu.harvard.hul.ois.jhove.module.pdf.Comment;
import edu.harvard.hul.ois.jhove.module.pdf.CrossRefStream;
import edu.harvard.hul.ois.jhove.module.pdf.Destination;
import edu.harvard.hul.ois.jhove.module.pdf.DictionaryStart;
import edu.harvard.hul.ois.jhove.module.pdf.DocNode;
import edu.harvard.hul.ois.jhove.module.pdf.FileTokenizer;
import edu.harvard.hul.ois.jhove.module.pdf.Filter;
import edu.harvard.hul.ois.jhove.module.pdf.Keyword;
import edu.harvard.hul.ois.jhove.module.pdf.LinearizedProfile;
import edu.harvard.hul.ois.jhove.module.pdf.Literal;
import edu.harvard.hul.ois.jhove.module.pdf.MessageConstants;
import edu.harvard.hul.ois.jhove.module.pdf.Name;
import edu.harvard.hul.ois.jhove.module.pdf.NameTreeNode;
import edu.harvard.hul.ois.jhove.module.pdf.Numeric;
import edu.harvard.hul.ois.jhove.module.pdf.ObjectStream;
import edu.harvard.hul.ois.jhove.module.pdf.PageLabelNode;
import edu.harvard.hul.ois.jhove.module.pdf.PageObject;
import edu.harvard.hul.ois.jhove.module.pdf.PageTreeNode;
import edu.harvard.hul.ois.jhove.module.pdf.Parser;
import edu.harvard.hul.ois.jhove.module.pdf.PdfArray;
import edu.harvard.hul.ois.jhove.module.pdf.PdfDictionary;
import edu.harvard.hul.ois.jhove.module.pdf.PdfException;
import edu.harvard.hul.ois.jhove.module.pdf.PdfHeader;
import edu.harvard.hul.ois.jhove.module.pdf.PdfIndirectObj;
import edu.harvard.hul.ois.jhove.module.pdf.PdfInvalidException;
import edu.harvard.hul.ois.jhove.module.pdf.PdfMalformedException;
import edu.harvard.hul.ois.jhove.module.pdf.PdfObject;
import edu.harvard.hul.ois.jhove.module.pdf.PdfProfile;
import edu.harvard.hul.ois.jhove.module.pdf.PdfSimpleObject;
import edu.harvard.hul.ois.jhove.module.pdf.PdfStream;
import edu.harvard.hul.ois.jhove.module.pdf.PdfStrings;
import edu.harvard.hul.ois.jhove.module.pdf.PdfXMPSource;
import edu.harvard.hul.ois.jhove.module.pdf.StringValuedToken;
import edu.harvard.hul.ois.jhove.module.pdf.TaggedProfile;
import edu.harvard.hul.ois.jhove.module.pdf.Token;
import edu.harvard.hul.ois.jhove.module.pdf.Tokenizer;
import edu.harvard.hul.ois.jhove.module.pdf.X1Profile;
import edu.harvard.hul.ois.jhove.module.pdf.X1aProfile;
import edu.harvard.hul.ois.jhove.module.pdf.X2Profile;
import edu.harvard.hul.ois.jhove.module.pdf.X3Profile;

/**
 * Module for identification and validation of PDF files.
 */
public class PdfModule extends ModuleBase {

    public static final String MIME_TYPE = "application/pdf";
    public static final String EXT = ".pdf";
    public static final int MAX_PAGE_TREE_DEPTH = 100;
    public static final int MAX_OBJ_STREAM_DEPTH = 30;

    private static final String ENCODING_PREFIX = "ENC=";

    private static final String DEFAULT_PAGE_LAYOUT = "SinglePage";
    private static final String DEFAULT_MODE = "UseNone";

    private static final String FILTER_NAME_CCITT = "CCITTFaxDecode";
    private static final String FILTER_NAME_CRYPT = "Crypt";
    private static final String FILTER_NAME_DCT = "DCTDecode";
    private static final String FILTER_NAME_FLATE = "FlateDecode";
    private static final String FILTER_NAME_JPX = "JPXDecode";
    private static final String FILTER_NAME_LZW = "LZWDecode";
    private static final String FILTER_NAME_RUN_LENGTH = "RunLengthDecode";

    private static final String FILTER_VAL_STANDARD = "Standard";

    private static final String RESOURCE_NAME_XOBJECT = "XObject";

    private static final String FONT_TYPE0 = "Type0";
    private static final String FONT_TYPE1 = "Type1";
    private static final String FONT_TYPE3 = "Type3";
    private static final String FONT_MM_TYPE1 = "MMType1";
    private static final String FONT_TRUE_TYPE = "TrueType";
    private static final String FONT_CID_TYPE0 = "CIDFontType0";
    private static final String FONT_CID_TYPE2 = "CIDFontType2";

    private static final String ACTION_VAL_GOTO = "GoTo";

    private static final String DICT_KEY_DIRECTION = "Direction";

    private static final String DICT_KEY_CENTER_WINDOW = "CenterWindow";
    private static final String DICT_KEY_DISP_DOC_TITLE = "DisplayDocTitle";
    private static final String DICT_KEY_FIT_WINDOW = "FitWindow";
    private static final String DICT_KEY_HIDE_MENUBAR = "HideMenubar";
    private static final String DICT_KEY_HIDE_TOOLBAR = "HideToolbar";
    private static final String DICT_KEY_HIDE_WINDOW_UI = "HideWindowUI";
    private static final String DICT_KEY_NO_FULL_PAGE = "NonFullScreenPageMode";
    private static final String DICT_KEY_PAGE_CLIP = "PageClip";
    private static final String DICT_KEY_PRINT_AREA = "PrintArea";
    private static final String DICT_KEY_VIEW_AREA = "ViewArea";
    private static final String DICT_KEY_VIEW_CLIP = "ViewClip";

    private static final String PROP_NAME_CENTER_WINDOW = DICT_KEY_CENTER_WINDOW;
    private static final String PROP_NAME_DISP_DOC_TITLE = DICT_KEY_DISP_DOC_TITLE;
    private static final String PROP_NAME_FIT_WINDOW = DICT_KEY_FIT_WINDOW;
    private static final String PROP_NAME_HIDE_MENUBAR = DICT_KEY_HIDE_MENUBAR;
    private static final String PROP_NAME_HIDE_TOOLBAR = DICT_KEY_HIDE_TOOLBAR;
    private static final String PROP_NAME_HIDE_WINDOW_UI = DICT_KEY_HIDE_WINDOW_UI;
    private static final String PROP_NAME_NO_FULL_PAGE = DICT_KEY_NO_FULL_PAGE;
    private static final String PROP_NAME_PAGE_CLIP = DICT_KEY_PAGE_CLIP;
    private static final String PROP_NAME_PRINT_AREA = DICT_KEY_PRINT_AREA;
    private static final String PROP_NAME_VIEW_AREA = DICT_KEY_VIEW_AREA;
    private static final String PROP_NAME_VIEW_CLIP = DICT_KEY_VIEW_CLIP;
    private static final String PROP_NAME_DIRECTION = DICT_KEY_DIRECTION;

    private static final String DICT_KEY_FONT_DESCRIPTOR = "FontDescriptor";
    private static final String DICT_KEY_STARTXREF = "startxref";
    private static final String DICT_KEY_BASE_FONT = "BaseFont";
    private static final String DICT_KEY_CONTENTS = "Contents";
    private static final String DICT_KEY_CID_INFO = "CIDSystemInfo";
    private static final String DICT_KEY_DIFFERENCES = "Differences";
    private static final String DICT_KEY_RESOURCES = "Resources";
    private static final String DICT_KEY_TO_UNICODE = "ToUnicode";
    private static final String DICT_KEY_ROOT = "Root";
    private static final String DICT_KEY_RECT = "Rect";
    private static final String DICT_KEY_DEST = "Dest";
    private static final String DICT_KEY_FIRST_CHAR = "FirstChar";
    private static final String DICT_KEY_LAST_CHAR = "LastChar";
    private static final String DICT_KEY_TRAILER = "trailer";
    private static final String DICT_KEY_SIZE = "Size";
    private static final String DICT_KEY_ENCRYPT = "Encrypt";
    private static final String DICT_KEY_STMF = "StmF";
    private static final String DICT_KEY_INFO = "Info";
    private static final String DICT_KEY_ID = "ID";
    private static final String DICT_KEY_FONT_NAME = "FontName";
    private static final String DICT_KEY_FONT_FILE = "FontFile";
    private static final String DICT_KEY_FONT_FILE_2 = "FontFile2";
    private static final String DICT_KEY_FONT_FILE_3 = "FontFile3";
    private static final String DICT_KEY_BBOX = "BBox";
    private static final String DICT_KEY_FONT_BBOX = "FontBBox";
    private static final String DICT_KEY_XREF_STREAM = "XRefStm";
    private static final String DICT_KEY_VIEWER_PREFS = "ViewerPreferences";
    private static final String DICT_KEY_PAGE_LAYOUT = "PageLayout";
    private static final String DICT_KEY_PAGE_MODE = "PageMode";
    private static final String DICT_KEY_OUTLINES = "Outlines";
    private static final String DICT_KEY_ORDERING = "Ordering";
    private static final String DICT_KEY_REGISTRY = "Registry";
    private static final String DICT_KEY_SUPPLEMENT = "Supplement";
    private static final String DICT_KEY_LANG = "Lang";
    private static final String DICT_KEY_PAGES = "Pages";
    private static final String DICT_KEY_PAGE_LABELS = "PageLabels";
    private static final String DICT_KEY_TYPE = "Type";
    private static final String DICT_KEY_VERSION = "Version";
    private static final String DICT_KEY_EXTENSIONS = "Extensions";
    private static final String DICT_KEY_EXTENSIONLEVEL = "ExtensionLevel";
    private static final String DICT_KEY_BASEVERSION = "BaseVersion";
    private static final String PROP_NAME_BASEVERSION = DICT_KEY_BASEVERSION;
    private static final String PROP_NAME_EXTENSIONLEVEL = DICT_KEY_EXTENSIONLEVEL;
    private static final String PROP_NAME_DEVELOPERPREFIX = "DeveloperPrefix";
    private static final String DICT_KEY_NAME = "Name";
    private static final String DICT_KEY_NAMES = "Names";
    private static final String DICT_KEY_EMBEDDED_FILES = "EmbeddedFiles";
    private static final String DICT_KEY_DESTS = "Dests";
    private static final String DICT_KEY_FILTER = "Filter";
    private static final String DICT_KEY_K = "K";
    private static final String DICT_KEY_P = "P";
    private static final String DICT_KEY_R = "R";
    private static final String DICT_KEY_V = "V";
    private static final String DICT_KEY_ENCODING = "Encoding";
    private static final String DICT_KEY_BASE_ENCODING = "BaseEncoding";
    private static final String DICT_KEY_LENGTH = "Length";
    private static final String DICT_KEY_WIDTH = "Width";
    private static final String DICT_KEY_HEIGHT = "Height";
    private static final String DICT_KEY_KEY_LENGTH = "KeyLength";
    private static final String DICT_KEY_TITLE = "Title";
    private static final String DICT_KEY_AUTHOR = "Author";
    private static final String DICT_KEY_SUBJECT = "Subject";
    private static final String DICT_KEY_KEYWORDS = "Keywords";
    private static final String DICT_KEY_CREATOR = "Creator";
    private static final String DICT_KEY_PRODUCER = "Producer";
    private static final String DICT_KEY_CREATION_DATE = "CreationDate";
    private static final String DICT_KEY_MODIFIED_DATE = "ModDate";
    private static final String DICT_KEY_TRAPPED = "Trapped";
    private static final String DICT_KEY_XOBJ_SUBTYPE = "Subtype";
    private static final String DICT_KEY_FONT_SUBTYPE = DICT_KEY_XOBJ_SUBTYPE;
    private static final String DICT_KEY_DECODE_PARAMS = "DecodeParms";
    private static final String DICT_KEY_COLOR_SPACE = "ColorSpace";
    private static final String DICT_KEY_METADATA = "Metadata";
    private static final String DICT_KEY_BITS_PER_COMPONENT = "BitsPerComponent";
    private static final String DICT_KEY_INTENT = "Intent";
    private static final String DICT_KEY_IMAGE_MASK = "ImageMask";
    private static final String DICT_KEY_DECODE = "Decode";
    private static final String DICT_KEY_INTERPOLATE = "Interpolate";
    private static final String DICT_KEY_DESCENDANT_FONTS = "DescendantFonts";
    private static final String DICT_KEY_ROTATE = "Rotate";
    private static final String DICT_KEY_USER_UNIT = "UserUnit";
    private static final String DICT_KEY_VIEWPORT = "VP";
    private static final String DICT_KEY_THUMB = "Thumb";
    private static final String DICT_KEY_MEASURE = "Measure";
    private static final String DICT_KEY_COUNT = "Count";
    private static final String DICT_KEY_PARENT = "Parent";
    private static final String DICT_KEY_PREV = "Prev";
    private static final String DICT_KEY_NEXT = "Next";
    private static final String DICT_KEY_FIRST = "First";
    private static final String DICT_KEY_LAST = "Last";
    private static final String DICT_KEY_FLAGS = "Flags";

    private static final String KEY_VAL_CATALOG = "Catalog";
    private static final String KEY_VAL_PAGES = "Pages";

    private static final String PROP_NAME_BASE_FONT = DICT_KEY_BASE_FONT;
    private static final String PROP_NAME_CALLOUT_LINE = "CalloutLine";
    private static final String PROP_NAME_CMAP_DICT = "CMapDictionary";
    private static final String PROP_NAME_CID_INFO = DICT_KEY_CID_INFO;
    private static final String PROP_NAME_CID_INFOS = "CIDSystemInfos";
    private static final String PROP_NAME_CONTENTS = DICT_KEY_CONTENTS;
    private static final String PROP_NAME_DISTANCE = "Distance";
    private static final String PROP_NAME_DIFFERENCES = DICT_KEY_DIFFERENCES;
    private static final String PROP_NAME_ENCODING = DICT_KEY_ENCODING;
    private static final String PROP_NAME_ENCODING_DICTIONARY = "EncodingDictionary";
    private static final String PROP_NAME_BASE_ENCODING = DICT_KEY_BASE_ENCODING;
    private static final String PROP_NAME_EXTERNAL_STREAMS = "ExternalStreams";
    private static final String PROP_NAME_FILTER = DICT_KEY_FILTER;
    private static final String PROP_NAME_FILTERS = "Filters";
    private static final String PROP_NAME_FILE = "File";
    private static final String PROP_NAME_FIRST_CHAR = DICT_KEY_FIRST_CHAR;
    private static final String PROP_NAME_FLAGS = DICT_KEY_FLAGS;
    private static final String PROP_NAME_AREA = "Area";
    private static final String PROP_NAME_IMAGE = "Image";
    private static final String PROP_NAME_IMAGES = "Images";
    private static final String PROP_NAME_OBJECTS = "Objects";
    private static final String PROP_NAME_RESOURCES = DICT_KEY_RESOURCES;
    private static final String PROP_NAME_SUBTYPE = DICT_KEY_XOBJ_SUBTYPE;
    private static final String PROP_NAME_FREE_OBJECTS = "FreeObjects";
    private static final String PROP_NAME_INC_UPDATES = "IncrementalUpdates";
    private static final String PROP_NAME_DOC_CATALOG = "DocumentCatalog";
    private static final String PROP_NAME_ENCRYPTION = "Encryption";
    private static final String PROP_NAME_KEY_LENGTH = DICT_KEY_KEY_LENGTH;
    private static final String PROP_NAME_INFO = DICT_KEY_INFO;
    private static final String PROP_NAME_DESTINATION = "Destination";
    private static final String PROP_NAME_CHILDREN = "Children";
    private static final String PROP_NAME_PAGE_LAYOUT = DICT_KEY_PAGE_LAYOUT;
    private static final String PROP_NAME_LANG = "Language";
    private static final String PROP_NAME_LAST_CHAR = DICT_KEY_LAST_CHAR;
    private static final String PROP_NAME_MEASURE = DICT_KEY_MEASURE;
    private static final String PROP_NAME_SECURITY_HANDLER = "SecurityHandler";
    private static final String PROP_NAME_EFF = "EFF";
    private static final String PROP_NAME_ALGORITHM = "Algorithm";
    private static final String PROP_NAME_RECT = DICT_KEY_RECT;
    private static final String PROP_NAME_REVISION = "Revision";
    private static final String PROP_NAME_OWNER_STRING = "OwnerString";
    private static final String PROP_NAME_USER_STRING = "UserString";
    private static final String PROP_NAME_OWNERKEY_STRING = "OwnerEncryptionKey";
    private static final String PROP_NAME_USERKEY_STRING = "UserEncryptionKey";
    private static final String PROP_NAME_USER_UNIT = DICT_KEY_USER_UNIT;
    private static final String PROP_NAME_STANDARD_SECURITY_HANDLER = "StandardSecurityHandler";
    private static final String PROP_NAME_TITLE = DICT_KEY_TITLE;
    private static final String PROP_NAME_AUTHOR = DICT_KEY_AUTHOR;
    private static final String PROP_NAME_SUBJECT = DICT_KEY_SUBJECT;
    private static final String PROP_NAME_KEYWORDS = DICT_KEY_KEYWORDS;
    private static final String PROP_NAME_CREATOR = DICT_KEY_CREATOR;
    private static final String PROP_NAME_PRODUCER = DICT_KEY_PRODUCER;
    private static final String PROP_NAME_CREATION_DATE = DICT_KEY_CREATION_DATE;
    private static final String PROP_NAME_MODIFIED_DATE = DICT_KEY_MODIFIED_DATE;
    private static final String PROP_NAME_TRAPPED = DICT_KEY_TRAPPED;
    private static final String PROP_NAME_FILTER_PIPELINE = "FilterPipeline";
    private static final String PROP_NAME_NISO_IMAGE_MD = "NisoImageMetadata";
    private static final String PROP_NAME_COLOR_SPACE = DICT_KEY_COLOR_SPACE;
    private static final String PROP_NAME_ACTION_DEST = "ActionDest";
    private static final String PROP_NAME_ANNOTATION = "Annotation";
    private static final String PROP_NAME_APP_DICT = "AppearanceDictionary";
    private static final String PROP_NAME_INTENT = DICT_KEY_INTENT;
    private static final String PROP_NAME_IMAGE_MASK = DICT_KEY_IMAGE_MASK;
    private static final String PROP_NAME_DECODE = DICT_KEY_DECODE;
    private static final String PROP_NAME_NAME = DICT_KEY_NAME;
    private static final String PROP_NAME_ID = DICT_KEY_ID;
    private static final String PROP_NAME_ITEM = "Item";
    private static final String PROP_NAME_INTERPOLATE = DICT_KEY_INTERPOLATE;
    private static final String PROP_NAME_FONT_TYPE0 = FONT_TYPE0;
    private static final String PROP_NAME_FONT_TYPE1 = FONT_TYPE1;
    private static final String PROP_NAME_FONT_TYPE3 = FONT_TYPE3;
    private static final String PROP_NAME_FONT_MM_TYPE1 = FONT_MM_TYPE1;
    private static final String PROP_NAME_FONT_TRUE_TYPE = FONT_TRUE_TYPE;
    private static final String PROP_NAME_FONT_CID_TYPE0 = FONT_CID_TYPE0;
    private static final String PROP_NAME_FONT_CID_TYPE2 = FONT_CID_TYPE2;
    private static final String PROP_NAME_FONT = "Font";
    private static final String PROP_NAME_FONTS = "Fonts";
    private static final String PROP_NAME_FONT_SUBSET = "FontSubset";
    private static final String PROP_NAME_FONT_BBOX = DICT_KEY_FONT_BBOX;
    private static final String PROP_NAME_FONT_DESC = DICT_KEY_FONT_DESCRIPTOR;
    private static final String PROP_NAME_FONT_FILE = DICT_KEY_FONT_FILE;
    private static final String PROP_NAME_FONT_FILE_2 = DICT_KEY_FONT_FILE_2;
    private static final String PROP_NAME_FONT_FILE_3 = DICT_KEY_FONT_FILE_3;
    private static final String PROP_NAME_FONT_NAME = DICT_KEY_FONT_NAME;
    private static final String PROP_NAME_PDF_METADATA = "PDFMetadata";
    private static final String PROP_NAME_LAST_MOD = "LastModified";
    private static final String PROP_NAME_OUTLINES = DICT_KEY_OUTLINES;
    private static final String PROP_NAME_REGISTRY = DICT_KEY_REGISTRY;
    private static final String PROP_NAME_SUPPLEMENT = DICT_KEY_SUPPLEMENT;
    private static final String PROP_NAME_PAGES = DICT_KEY_PAGES;
    private static final String PROP_NAME_SEQUENCE = "Sequence";
    private static final String PROP_NAME_ANNOTATIONS = "Annotations";
    private static final String PROP_NAME_ROTATE = DICT_KEY_ROTATE;
    private static final String PROP_NAME_REPLY_TYPE = "ReplyType";
    private static final String PROP_NAME_VIEWPORT = "Viewport";
    private static final String PROP_NAME_VIEWPORTS = "Viewports";
    private static final String PROP_NAME_THUMB = DICT_KEY_THUMB;
    private static final String PROP_NAME_TO_UNICODE = DICT_KEY_TO_UNICODE;
    private static final String PROP_NAME_PAGE = "Page";
    private static final String PROP_NAME_LABEL = "Label";
    private static final String PROP_NAME_RATIO = "Ratio";

    private static final String PROP_VAL_CROP_BOX = "CropBox";
    private static final String PROP_VAL_FONT_BBOX = DICT_KEY_FONT_BBOX;
    private static final String PROP_VAL_NULL = "null";
    private static final String PROP_VAL_EXTERNAL = "External";
    private static final String PROP_VAL_NO_FLAGS_SET = "No flags set";
    private static final String XOBJ_SUBTYPE_IMAGE = PROP_NAME_IMAGE;
    private static final String EMPTY_LABEL_PROPERTY = "[empty]";

    /******************************************************************
     * PRIVATE CLASS FIELDS.
     ******************************************************************/

    private static final String NAME = "PDF-hul";
    private static final String RELEASE = "1.12.7";
    private static final int[] DATE = { 2024, 8, 22 };
    private static final String[] FORMAT = { "PDF",
            "Portable Document Format" };
    private static final String COVERAGE = "PDF 1.0-1.6; "
            + "PDF/X-1 (ISO 15930-1:2001), X-1a (ISO 15930-4:2003), "
            + "X-2 (ISO 15930-5:2003), and X-3 (ISO 15930-6:2003); "
            + "Tagged PDF; Linearized PDF";
    private static final String[] MIMETYPE = { MIME_TYPE };
    private static final String WELLFORMED = "A PDF file is "
            + "well-formed if it meets the criteria defined in Chapter "
            + "3 of the PDF Reference 1.6 (5th edition, 2004)";
    private static final String VALIDITY = null;
    private static final String REPINFO = null;
    private static final String NOTE = "This module does *not* validate data "
            + "within content streams (including operators) or encrypted data";
    private static final String RIGHTS = "Copyright 2003-2007 by JSTOR and "
            + "the President and Fellows of Harvard College. "
            + "Released under the GNU Lesser General Public License.";
    private static final String ENCRYPTED = "<May be encrypted>";
    private static final String SPEC_DOC_TITLE = "PDF Reference: Adobe Portable Document Format, Version ";
    /** Logger for this class. */
    protected Logger _logger;

    /** Font type selectors. */
    public final static int F_TYPE0 = 1, F_TYPE1 = 2, F_TT = 3, F_TYPE3 = 4,
            F_MM1 = 5, F_CID0 = 6, F_CID2 = 7;

    /******************************************************************
     * PRIVATE INSTANCE FIELDS.
     ******************************************************************/

    /**
     * The maximum number of fonts that will be reported before we just
     * give up and report a stub to avoid running out of memory.
     */
    protected int DEFAULT_MAX_FONTS = 1000;

    /* Constants for trailer parsing */
    private static final int EOFSCANSIZE = 1024;
    private static final int XREFSCANSIZE = 128; // generous...

    protected RandomAccessFile _raf;
    protected Parser _parser;
    protected String _version;
    protected Property _metadata;
    protected Property _xmpProp;
    protected long _eof;
    protected long _startxref;
    protected long _prevxref;
    protected int _numFreeObjects;
    protected Property _idProperty;
    protected int _objCount; // Count of objects in the cross-reference
                             // table
    protected int _numObjects; // Value of the "Size" entry in the trailer
                               // dictionary
    protected int _numTrailers; // Count of the number of trailers (updates)
    protected Map _objects; // Map of the objects in the file
    protected long[] _xref; // Array of object offsets from XRef table
    protected int[][] _xref2; // Array of int[2], giving object stream and
                              // offset when _xref[i] < 0
    protected boolean _xrefIsStream; // True if XRef streams rather than tables
                                     // are used
    protected boolean _encrypted; // Equivalent to _encryptDictRef != null
    protected boolean _streamsEncrypted; // streams are encrypted and can't be parsed.
    protected List<Property> _docCatalogList; // Info extracted from doc cat dict
    protected List<Property> _encryptList; // Info from encryption dict
    protected List<Property> _docInfoList; // Info from doc info dict
    protected List<Property> _extStreamsList; // List of external streams
    protected List<Property> _imagesList; // List of image streams
    protected List<Property> _filtersList; // List of filters
    protected List<Property> _pagesList; // List of PageObjects

    /** Map of Type 0 font dictionaries. */
    protected Map<Integer, PdfObject> _type0FontsMap;
    /** Map of Type 1 font dictionaries. */
    protected Map<Integer, PdfObject> _type1FontsMap;
    /** Map of Multiple Master font dictionaries. */
    protected Map<Integer, PdfObject> _mmFontsMap;
    /** Map of Type 3 font dictionaries. */
    protected Map<Integer, PdfObject> _type3FontsMap;
    /** Map of TrueType font dictionaries. */
    protected Map<Integer, PdfObject> _trueTypeFontsMap;
    /** Map of CIDFont/Type 1 dictionaries. */
    protected Map<Integer, PdfObject> _cid0FontsMap;
    /** Map of CIDFont/TrueType dictionaries. */
    protected Map<Integer, PdfObject> _cid2FontsMap;

    /** Map associating page object dictionaries with sequence numbers. */
    protected Map<Integer, Integer> _pageSeqMap;

    protected PdfIndirectObj _docCatDictRef;
    protected PdfIndirectObj _encryptDictRef;
    protected PdfIndirectObj _docInfoDictRef;
    protected PdfIndirectObj _pagesDictRef;

    protected PdfDictionary _docCatDict;
    protected PdfDictionary _docInfoDict;
    protected PageTreeNode _docTreeRoot;
    protected PdfDictionary _pageLabelDict;
    protected PageLabelNode _pageLabelRoot;
    protected NameTreeNode _embeddedFiles;
    protected NameTreeNode _destNames;
    protected PdfDictionary _encryptDict;
    protected PdfDictionary _trailerDict;
    protected PdfDictionary _viewPrefDict;
    protected PdfDictionary _outlineDict;
    protected PdfDictionary _destsDict;

    protected boolean _showFonts;
    protected boolean _showOutlines;
    protected boolean _showAnnotations;
    protected boolean _showPages;

    protected boolean _actionsExist;
    protected boolean _pdfACompliant; // flag checking PDF/A compliance

    /** True if warning has been issued on recursive outlines. */
    protected boolean _recursionWarned;

    /*
     * These three variables track whether certain messages have been posted
     * notifying the user of omitted information.
     */
    protected boolean _skippedFontsReported;
    protected boolean _skippedOutlinesReported;
    protected boolean _skippedAnnotationsReported;
    protected boolean _skippedPagesReported;

    /** List of profile checkers. */
    protected List<PdfProfile> _profile;

    /** Cached object stream. */
    protected ObjectStream _cachedObjectStream;

    /** Object number of cached object stream. */
    protected int _cachedStreamIndex;

    /** Map of visited nodes when walking through an outline. */
    protected Set<Integer> _visitedOutlineNodes;

    /** Maximum number of fonts to report full information on. */
    protected int maxFonts;

    /** Number of fonts reported so far. */
    protected int _nFonts;

    /* Name-to-value array pairs for NISO metadata */
    private final static String[] compressionStrings = { FILTER_NAME_LZW,
            /* "FlateDecode", */ FILTER_NAME_RUN_LENGTH, FILTER_NAME_DCT,
            FILTER_NAME_CCITT };
    private final static int[] compressionValues = { 5, /* 8, */ 32773, 6, 2 };
    /*
     * The value of 2 (CCITTFaxDecode) is a placeholder; additional
     * checking of the K parameter is needed to determine the real
     * value if that's returned.
     */

    private final static String[] colorSpaceStrings = { "Lab", "DeviceRGB",
            "DeviceCMYK", "DeviceGray", "Indexed" };
    private final static int[] colorSpaceValues = { 8, 2, 5, 1, 3 };

    /******************************************************************
     * CLASS CONSTRUCTOR.
     ******************************************************************/

    /**
     * Creates an instance of the module and initializes identifying
     * information.
     */
    public PdfModule() {

        super(NAME, RELEASE, DATE, FORMAT, COVERAGE, MIMETYPE, WELLFORMED,
                VALIDITY, REPINFO, NOTE, RIGHTS, true);

        _logger = Logger.getLogger("edu.harvard.hul.ois.jhove.module");

        _vendor = Agent.harvardInstance();

        Document doc = new Document(SPEC_DOC_TITLE + "1.4", DocumentType.BOOK);
        Agent agent = Agent.newAdobeInstance();
        doc.setPublisher(agent);
        doc.setDate("2001-12");
        doc.setEdition("3rd edition");
        doc.setIdentifier(new Identifier("0-201-75839-3", IdentifierType.ISBN));
        doc.setIdentifier(new Identifier(
                "http://partners.adobe.com/asn/" + "acrobat/docs/File_Format_"
                        + "Specifications/PDFReference.pdf",
                IdentifierType.URL));
        _specification.add(doc);

        doc = new Document(SPEC_DOC_TITLE + "1.5", DocumentType.BOOK);
        doc.setPublisher(agent);
        doc.setDate("2003");
        doc.setEdition("4th edition");
        doc.setIdentifier(new Identifier(
                "http://partners.adobe.com/public/developer/en/pdf/PDFReference15_v6.pdf",
                IdentifierType.URL));
        _specification.add(doc);

        doc = new Document(SPEC_DOC_TITLE + "1.6", DocumentType.BOOK);
        doc.setPublisher(agent);
        doc.setDate("2004-11");
        doc.setEdition("5th edition");
        doc.setIdentifier(new Identifier(
                "http://partners.adobe.com/public/developer/en/pdf/PDFReference16.pdf",
                IdentifierType.URL));
        _specification.add(doc);

        doc = new Document("Graphic technology -- Prepress "
                + "digital data exchange -- Use of PDF -- "
                + "Part 1: Complete exchange using CMYK data "
                + "(PDF/X-1 and PDF/X-1a)", DocumentType.STANDARD);
        Agent isoAgent = Agent.newIsoInstance();
        doc.setPublisher(isoAgent);
        doc.setDate("2001-12-06");
        doc.setIdentifier(
                new Identifier("ISO 15930-1:2001", IdentifierType.ISO));
        _specification.add(doc);

        doc = new Document("Graphic technology -- Prepress "
                + "digital data exchange -- Use of PDF -- "
                + "Part 4: Complete exchange using CMYK and "
                + "spot colour printing data using " + "PDF 1.4 (PDF/X-1a)",
                DocumentType.STANDARD);
        doc.setPublisher(isoAgent);
        doc.setDate("2003-08-04");
        doc.setIdentifier(
                new Identifier("ISO 15930-4:2003", IdentifierType.ISO));
        _specification.add(doc);

        doc = new Document("Graphic technology -- Prepress "
                + "digital data exchange -- Use of PDF -- "
                + "Part 5: Partial exchange of printing data "
                + "using PDF 1.4 (PDF/X-2)", DocumentType.STANDARD);
        doc.setPublisher(isoAgent);
        doc.setDate("2003-08-05");
        doc.setIdentifier(
                new Identifier("ISO 15930-5:2003", IdentifierType.ISO));
        _specification.add(doc);

        doc = new Document("Graphic technology -- Prepress "
                + "digital data exchange -- Use of PDF -- "
                + "Part 6: Complete exchange suitable for "
                + "colour-managed workflows using " + "PDF 1.4 (PDF/X-3)",
                DocumentType.STANDARD);
        doc.setPublisher(isoAgent);
        doc.setDate("2003-08-06");
        doc.setIdentifier(
                new Identifier("ISO 15930-6:2003", IdentifierType.ISO));
        _specification.add(doc);

        _signature.add(new ExternalSignature(EXT, SignatureType.EXTENSION,
                SignatureUseType.OPTIONAL));
        _signature.add(new InternalSignature(PdfHeader.PDF_1_SIG_HEADER,
                SignatureType.MAGIC, SignatureUseType.MANDATORY, 0));
        _signature.add(new InternalSignature(PdfHeader.PDF_2_SIG_HEADER,
                SignatureType.MAGIC, SignatureUseType.MANDATORY, 0));

        doc = new Document(
                "Document management -- Electronic "
                        + "document file format for long-term "
                        + "preservation -- Part 1: Use of PDF (PDF/A)",
                DocumentType.RFC);
        doc.setPublisher(isoAgent);
        doc.setDate("2003-11-30");
        doc.setIdentifier(new Identifier("ISO/CD 19005-1", IdentifierType.ISO));
        doc.setIdentifier(new Identifier(
                "http://www.aiim.org/documents/standards/ISO_19005-1_(E).doc",
                IdentifierType.URL));
        _specification.add(doc);

        _profile = new ArrayList<PdfProfile>(6);
        _profile.add(new LinearizedProfile(this));
        TaggedProfile tpr = new TaggedProfile(this);
        _profile.add(tpr);

        /*
         * CURRENT PDF/A PROFILING UNFIT FOR PURPOSE; SEE GITHUB ISSUE #101.
         *
         * AProfile apr = new AProfile(this);
         * _profile.add(apr);
         * // Link AProfile to TaggedProfile to save checking
         * // the former twice.
         * apr.setTaggedProfile(tpr);
         * 
         * AProfileLevelA apra = new AProfileLevelA(this);
         * _profile.add(apra);
         * // AProfileLevelA depends on AProfile
         * apra.setAProfile(apr);
         */

        X1Profile x1 = new X1Profile(this);
        _profile.add(x1);
        X1aProfile x1a = new X1aProfile(this);
        _profile.add(x1a);
        // Linking the X1 profile to the X1a profile saves checking the former
        // twice.
        x1a.setX1Profile(x1);
        _profile.add(new X2Profile(this));
        _profile.add(new X3Profile(this));

        _showAnnotations = false;
        _showFonts = false;
        _showOutlines = false;
        _showPages = false;
        maxFonts = DEFAULT_MAX_FONTS;
    }

    /******************************************************************
     * PUBLIC INSTANCE METHODS.
     *
     * Parsing methods.
     ******************************************************************/

    /**
     * Reset parameter settings.
     * Returns to a default state without any parameters.
     */
    @Override
    public void resetParams() {
        _showAnnotations = true;
        _showFonts = true;
        _showOutlines = true;
        _showPages = true;
        maxFonts = DEFAULT_MAX_FONTS;
    }

    /**
     * Per-action initialization. May be called multiple times.
     *
     * @param param
     *              The module parameter; under command-line Jhove, the -p
     *              parameter.
     *              If the parameter contains the indicated characters, then the
     *              specified information is omitted; otherwise, it is included.
     *              (This is the reverse of the behavior prior to beta 3.)
     *              These characters may be provided as separate parameters,
     *              or all in a single parameter.
     *              <ul>
     *              <li>a: annotations</li>
     *              <li>f: fonts</li>
     *              <li>o: outlines</li>
     *              <li>p: pages</li>
     *              </ul>
     *              <br>
     *              The parameter is case-independent. A null parameter is
     *              equivalent to the empty string.
     */
    @Override
    public void param(String param) {
        if (param != null) {
            param = param.toLowerCase();
            if (param.indexOf('a') >= 0) {
                _showAnnotations = false;
            }
            if (param.indexOf('f') >= 0) {
                _showFonts = false;
            }
            if (param.indexOf('o') >= 0) {
                _showOutlines = false;
            }
            if (param.indexOf('p') >= 0) {
                _showPages = false;
            }
            if (param.indexOf('n') >= 0) {
                // Parse out the number after the n, and use that to set
                // the maximum number of fonts reported. Default is
                // DEFAULT_MAX_FONTS.
                int n = param.indexOf('n');
                StringBuffer b = new StringBuffer();
                for (int i = n + 1; i < param.length(); i++) {
                    char ch = param.charAt(i);
                    if (Character.isDigit(ch)) {
                        b.append(ch);
                    } else {
                        break;
                    }
                }
                try {
                    int mx = Integer.parseInt(b.toString());
                    if (mx > 0) {
                        maxFonts = mx;
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Parses a file and stores descriptive information. A RandomAccessFile
     * must be used to represent the object.
     *
     * @param raf
     *             A PDF file
     * @param info
     *             A clean RepInfo object, which will be modified to hold
     *             the descriptive information
     */
    @Override
    public final void parse(RandomAccessFile raf, RepInfo info)
            throws IOException {
        initParse();
        initInfo(info);
        _objects = new HashMap<>();
        _raf = raf;

        Tokenizer tok = new FileTokenizer(_raf);
        _parser = new Parser(tok);
        _parser.setObjectMap(_objects);

        List<Property> metadataList = new ArrayList<Property>(11);
        /*
         * We construct a big whopping property,
         * which contains up to 11 subproperties
         */
        _metadata = new Property(PROP_NAME_PDF_METADATA, PropertyType.PROPERTY,
                PropertyArity.LIST, metadataList);

        if (_raf.length() > 10000000000L) { // that's 10^10
            _pdfACompliant = false; // doesn't meet size limit in Appendix C
                                    // of PDF spec
        }
        if (!parseHeader(info)) {
            return;
        }
        if (!findLastTrailer(info)) {
            return;
        }

        /*
         * Walk through the linked trailer and cross reference
         * sections.
         */
        _prevxref = -1;
        boolean lastTrailer = true;
        while (_startxref > 0) {
            // After the first (last) trailer, parse only for next "Prev" link
            if (!parseTrailer(info, !lastTrailer)) {
                return;
            }
            if (!readXRefInfo(info)) {
                return;
            }
            ++_numTrailers;
            if (_xrefIsStream) {
                /*
                 * If we have an xref stream, readXRefInfo dealt with all
                 * the streams in a single call.
                 */
                break;
            }
            // Beware infinite loop on badly broken file
            if (_startxref == _prevxref) {
                info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_134, // PDF-HUL-134
                        _parser.getOffset()));
                info.setWellFormed(false);
                return;
            }
            _startxref = _prevxref;
            lastTrailer = false;
        }
        if (!readDocCatalogDict(info)) {
            return;
        }
        if (!readEncryptDict(info)) {
            return;
        }
        if (!readDocInfoDict(info)) {
            return;
        }
        if (!readDocumentTree(info)) {
            return;
        }
        if (!readPageLabelTree(info)) {
            return;
        }
        if (!readXMPData(info)) {
            return;
        }
        findExternalStreams(info);
        if (!findFilters(info) && !_streamsEncrypted) {
            return;
        }
        findImages(info);
        findFonts(info);

        /* Object is well-formed PDF. */

        // Calculate checksums if not already present
        checksumIfRafNotCopied(info, raf);

        info.setVersion(_version);
        metadataList.add(new Property(PROP_NAME_OBJECTS, PropertyType.INTEGER,
                new Integer(_numObjects)));
        metadataList.add(new Property(PROP_NAME_FREE_OBJECTS,
                PropertyType.INTEGER, new Integer(_numFreeObjects)));
        metadataList.add(new Property(PROP_NAME_INC_UPDATES,
                PropertyType.INTEGER, new Integer(_numTrailers)));
        if (_docCatalogList != null) {
            metadataList.add(
                    new Property(PROP_NAME_DOC_CATALOG, PropertyType.PROPERTY,
                            PropertyArity.LIST, _docCatalogList));
        }
        if (_encryptList != null) {
            metadataList.add(new Property(PROP_NAME_ENCRYPTION,
                    PropertyType.PROPERTY, PropertyArity.LIST, _encryptList));
        }
        if (_docInfoList != null) {
            metadataList.add(new Property(PROP_NAME_INFO, PropertyType.PROPERTY,
                    PropertyArity.LIST, _docInfoList));
        }
        if (_idProperty != null) {
            metadataList.add(_idProperty);
        }
        if (_extStreamsList != null && !_extStreamsList.isEmpty()) {
            metadataList.add(new Property(PROP_NAME_EXTERNAL_STREAMS,
                    PropertyType.PROPERTY, PropertyArity.LIST,
                    _extStreamsList));
        }
        if (_filtersList != null && !_filtersList.isEmpty()) {
            metadataList.add(new Property(PROP_NAME_FILTERS,
                    PropertyType.PROPERTY, PropertyArity.LIST, _filtersList));
        }
        if (_imagesList != null && !_imagesList.isEmpty()) {
            metadataList.add(new Property(PROP_NAME_IMAGES,
                    PropertyType.PROPERTY, PropertyArity.LIST, _imagesList));
        }
        if (_showFonts || _verbosity == Module.MAXIMUM_VERBOSITY) {
            try {
                addFontsProperty(metadataList);
            } catch (NullPointerException e) {
                info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_135,
                        e.toString())); // PDF-HUL-135
            }
        }
        if (_nFonts > maxFonts) {
            info.setMessage(new InfoMessage(MessageConstants.PDF_HUL_136, // PDF-HUL-136
                    MessageConstants.PDF_HUL_136_SUB.getMessage() + _nFonts));
        }
        if (_xmpProp != null) {
            metadataList.add(_xmpProp);
        }
        addPagesProperty(metadataList, info);

        if (!doOutlineStuff(info)) {
            return;
        }

        info.setProperty(_metadata);

        /* Check for profile conformance. */

        if (!_parser.getPDFACompliant()) {
            _pdfACompliant = false;
        }
        if (info.getWellFormed() == RepInfo.TRUE) {
            // Well-formedness is necessary to satisfy any profile.
            ListIterator<PdfProfile> pter = _profile.listIterator();
            while (pter.hasNext()) {
                PdfProfile prof = pter.next();
                if (prof.satisfiesProfile(_raf, _parser)) {
                    info.setProfile(prof.getText());
                }
            }
        }
    }

    /**
     * Returns true if the module hasn't detected any violations
     * of PDF/A compliance. This must return true, but is not
     * sufficient by itself, to establish compliance. The
     * <code>AProfile</code> profiler makes the final determination.
     */
    public boolean mayBePDFACompliant() {
        return _pdfACompliant;
    }

    /**
     * Returns the document tree root.
     */
    public PageTreeNode getDocumentTree() {
        return _docTreeRoot;
    }

    /**
     * Returns the document information dictionary.
     */
    public PdfDictionary getDocInfo() {
        return _docInfoDict;
    }

    /**
     * Returns the encryption dictionary.
     */
    public PdfDictionary getEncryptionDict() {
        return _encryptDict;
    }

    /**
     * Return true if Actions have been detected in the file.
     */
    public boolean getActionsExist() {
        return _actionsExist;
    }

    /**
     * Initialize the module. This is called at the start
     * of parse restore the module to its initial state.
     */
    @Override
    protected final void initParse() {
        super.initParse();
        _xref = null;
        _xref2 = null;
        _version = "";
        _objects = null;
        _numFreeObjects = 0;
        _objCount = 0;
        _docInfoList = null;
        _extStreamsList = null;
        _docCatalogList = null;
        _encryptList = null;
        _imagesList = null;
        _filtersList = null;
        _pagesList = null;
        _type0FontsMap = null;
        _type1FontsMap = null;
        _mmFontsMap = null;
        _type3FontsMap = null;
        _trueTypeFontsMap = null;
        _cid0FontsMap = null;
        _cid2FontsMap = null;
        _docCatDictRef = null;
        _encryptDictRef = null;
        _docInfoDictRef = null;
        _pagesDictRef = null;
        _docCatDict = null;
        _docInfoDict = null;
        _docTreeRoot = null;
        _pageLabelDict = null;
        _encryptDict = null;
        _trailerDict = null;
        _viewPrefDict = null;
        _outlineDict = null;
        _destsDict = null;
        _pageSeqMap = null;
        _pageLabelRoot = null;
        _embeddedFiles = null;
        _destNames = null;
        _skippedFontsReported = false;
        _skippedOutlinesReported = false;
        _skippedAnnotationsReported = false;
        _skippedPagesReported = false;
        _idProperty = null;
        _actionsExist = false;
        _numObjects = 0;
        _numTrailers = -1;
        _pdfACompliant = true; // assume compliance till disproven
        _xmpProp = null;
        _cachedStreamIndex = -1;
        _nFonts = 0;
    }

    protected boolean parseHeader(RepInfo info) {
        PdfHeader header = null;
        try {
            header = PdfHeader.parseHeader(_parser);
        } catch (PdfException e) {
            if (e instanceof PdfInvalidException) {
                info.setValid(false);
            } else {
                info.setWellFormed(false);
            }
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), 0L)); // PDF-HUL-155
            return false;
        }
        _version = header.getVersionString();
        _pdfACompliant = header.isPdfACompliant();
        info.setSigMatch(_name);
        return true;
    }

    private long lastEOFOffset(RandomAccessFile raf) throws IOException {

        long offset = 0;
        long flen = 0;
        byte[] buf = null;

        // overkill to restore fileposition, but make this
        // as side-effect free as possible
        long savepos = raf.getFilePointer();
        flen = raf.length();
        buf = new byte[(int) Math.min(EOFSCANSIZE, flen)];
        offset = flen - buf.length;
        raf.seek(offset);
        raf.read(buf);
        raf.seek(savepos);

        // OK:
        // flen is the total length of the file
        // offset is 1024 bytes from the end of file or 0 if file is shorter
        // than 1024
        // buf contains all bytes from offset to end of file

        long eofpos = -1;
        // Note the limits, selected so the index never is out of bounds
        for (int i = buf.length - 4; i >= 1; i--) {
            if (buf[i] == '%') {
                if ((buf[i - 1] == '%') && (buf[i + 1] == 'E')
                        && (buf[i + 2] == 'O') && (buf[i + 3] == 'F')) {
                    eofpos = offset + i - 1;
                    break;
                }
            }
        }

        // if (Tracing.T_MODULE) System.out.println(flen - eofpos);
        return eofpos;

    }

    private long lastStartXrefOffset(RandomAccessFile raf, long eofOffset)
            throws IOException {

        long offset = 0;
        long flen = 0;
        byte[] buf = null;

        // overkill to restore fileposition, but make this
        // as side-effect free as possible
        long savepos = raf.getFilePointer();
        flen = raf.length();
        if (eofOffset <= 0) {
            eofOffset = flen;
        }
        if (eofOffset >= flen) {
            eofOffset = flen;
        }
        buf = new byte[(int) Math.min(XREFSCANSIZE, eofOffset)];
        offset = eofOffset - buf.length;
        raf.seek(offset);
        raf.read(buf);
        raf.seek(savepos);

        // OK:
        // flen is the total length of the file
        // offset is 128 bytes from the end of file or 0 if file is shorter than
        // 128
        // buf contains all bytes from offset to end of file

        long xrefpos = -1;
        // Note the limits, selected so the index never is out of bounds
        for (int i = buf.length - 9; i >= 0; i--) {
            if (buf[i] == 's') {
                if ((buf[i + 1] == 't') && (buf[i + 2] == 'a')
                        && (buf[i + 3] == 'r') && (buf[i + 4] == 't')
                        && (buf[i + 5] == 'x') && (buf[i + 6] == 'r')
                        && (buf[i + 7] == 'e') && (buf[i + 8] == 'f')) {
                    xrefpos = offset + i;
                    break;
                }
            }
        }

        // if (Tracing.T_MODULE) System.out.println(flen - xrefpos);
        return xrefpos;

    }

    /** Locate the last trailer of the file */
    protected boolean findLastTrailer(RepInfo info) throws IOException {
        /*
         * Parse file trailer. Technically, this should be the last thing in
         * the file, but we follow the Acrobat convention of looking in the
         * last 1024 bytes. Since incremental updates may add multiple
         * EOF comments, make sure that we use the last one in the file.
         */

        Token token = null;
        String value = null;

        _eof = lastEOFOffset(_raf);

        if (_eof < 0L) {
            info.setWellFormed(false);
            info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_138,
                    _raf.length())); // PDF-HUL-138
            return false;
        }

        // For PDF-A compliance, this must be at the very end.
        /*
         * Fix contributed by FCLA, 2007-05-30, to test for trailing data
         * properly.
         *
         * if (_raf.length () - _eof > 6) {
         */
        if (_raf.length() - _eof > 7) {
            _pdfACompliant = false;
        }

        /* Retrieve the "startxref" keyword. */

        long startxrefoffset = lastStartXrefOffset(_raf, _eof);
        _startxref = -1L;

        if (startxrefoffset >= 0) {
            try {
                _parser.seek(startxrefoffset); // points to the 'startxref' kw
                // _parser.seek(_eof - 23); // should we allow more slop?
            } catch (PdfException e) {
            }
            while (true) {
                try {
                    token = _parser.getNext();
                } catch (Exception e) {
                    // we're starting at an arbitrary point, so there
                    // can be parsing errors. Ignore them till we get
                    // back in sync.
                    continue;
                }
                if (token == null) {
                    break;
                }
                if (token instanceof Keyword) {
                    value = ((Keyword) token).getValue();
                    if (DICT_KEY_STARTXREF.equals(value)) {
                        try {
                            token = _parser.getNext();
                        } catch (Exception e) {
                            break; // no excuses here
                        }
                        if (token != null && token instanceof Numeric) {
                            _startxref = ((Numeric) token).getLongValue();
                        }
                    }
                }
            }
        }
        if (_startxref < 0L) {
            info.setWellFormed(false);
            info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_139, // PDF-HUL-139
                    _parser.getOffset()));
            return false;
        }
        return true;
    }

    /*
     * Parse a "trailer" (which is not necessarily the last
     * thing in the file, as trailers can be linked.)
     */
    protected boolean parseTrailer(RepInfo info, boolean prevOnly)
            throws IOException {
        Token token = null;
        String value = null;
        /* Parse the trailer dictionary. */

        try {
            _parser.seek(_startxref);
            /*
             * The next object may be either the keyword "xref", signifying
             * a classic cross-reference table, or a stream object,
             * signifying the new-style cross-reference stream.
             */
            Token xref = _parser.getNext();
            if (xref instanceof Keyword) {
                _xrefIsStream = false;
                _parser.getNext(Numeric.class, // PDF-HUL-68
                        MessageConstants.PDF_HUL_68); // first obj number

                _objCount = ((Numeric) _parser.getNext(Numeric.class, // PDF-HUL-69
                        MessageConstants.PDF_HUL_69)).getIntegerValue();
                _parser.seek(_parser.getOffset() + _objCount * 20);
            } else if (xref instanceof Numeric) {
                /* No cross-ref tables to backtrack. */
                _xrefIsStream = true;
                _prevxref = -1;
                /*
                 * But I do need to read the dictionary at this point, to get
                 * essential stuff out of it.
                 */
                PdfObject pdfStreamObj = _parser.readObjectDef((Numeric) xref);
                // the retrieved object should be stream
                if (!(pdfStreamObj instanceof PdfStream)) {
                    throw new PdfInvalidException(MessageConstants.PDF_HUL_150,
                            _parser.getOffset());
                }
                PdfDictionary dict = ((PdfStream) pdfStreamObj).getDict();
                _docCatDictRef = (PdfIndirectObj) dict.get(DICT_KEY_ROOT);
                if (_docCatDictRef == null) {
                    throw new PdfInvalidException(MessageConstants.PDF_HUL_70, // PDF-HUL-70
                            _parser.getOffset());
                }
                // readEncryptDict is not enough to check encryption when exists.
                _encryptDictRef = (PdfIndirectObj) dict.get(DICT_KEY_ENCRYPT);
                if (_encryptDictRef != null) {
                    _encrypted = true;
                }
                /*
                 * We don't need to see a trailer dictionary.
                 * Move along, move along.
                 */
                return true;
            }

            /* Now find the "trailer" keyword. */
            long trailer = -1L;
            while ((token = _parser.getNext()) != null) {
                if (token instanceof Keyword) {
                    value = ((Keyword) token).getValue();
                    if (DICT_KEY_TRAILER.equals(value)) {
                        token = _parser.getNext();
                        if (token instanceof DictionaryStart) {
                            trailer = _parser.getOffset() - 7L;
                            break;
                        }
                    }
                }
            }
            if (trailer < 0L) {
                info.setWellFormed(false);
                info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_71, // PDF-HUL-71
                        _parser.getOffset()));
                return false;
            }

            _trailerDict = _parser.readDictionary();
            PdfObject obj;

            // Extract contents of the trailer dictionary

            _prevxref = -1;
            obj = _trailerDict.get(DICT_KEY_PREV);
            if (obj != null) {
                if (obj instanceof PdfSimpleObject) {
                    token = ((PdfSimpleObject) obj).getToken();
                    if (token instanceof Numeric)
                        _prevxref = ((Numeric) token).getLongValue();
                }
                if (_prevxref < 0) {
                    throw new PdfInvalidException(MessageConstants.PDF_HUL_72, // PDF-HUL-72
                            _parser.getOffset());
                }
            }
            // If this isn't the last (first read) trailer, then we
            // ignore all the other dictionary entries.
            if (prevOnly) {
                return true;
            }

            obj = _trailerDict.get(DICT_KEY_SIZE);
            _docCatDictRef = (PdfIndirectObj) _trailerDict.get(DICT_KEY_ROOT);
            if (obj != null) {
                _numObjects = -1;
                if (obj instanceof PdfSimpleObject) {
                    token = ((PdfSimpleObject) obj).getToken();
                    if (token instanceof Numeric) {
                        _numObjects = ((Numeric) token).getIntegerValue();
                        _xref = new long[_numObjects];
                    } else {
                        throw new PdfInvalidException(MessageConstants.PDF_HUL_73, // PDF-HUL-73
                                _parser.getOffset());
                    }
                }
                if (_numObjects < 0) {
                    throw new PdfInvalidException(MessageConstants.PDF_HUL_73, // PDF-HUL-73
                            _parser.getOffset());
                }
                if (_numObjects > 8388607) {
                    // Appendix C implementation limit is enforced by PDF/A
                    _pdfACompliant = false;
                }
            } else
                throw new PdfInvalidException(MessageConstants.PDF_HUL_74, // PDF-HUL-74
                        _parser.getOffset());

            if (_docCatDictRef == null) {
                throw new PdfInvalidException(MessageConstants.PDF_HUL_75, // PDF-HUL-75
                        _parser.getOffset());
            }
            PdfObject encryptObj = _trailerDict.get(DICT_KEY_ENCRYPT);
            if (encryptObj instanceof PdfIndirectObj) {
                _encryptDictRef = (PdfIndirectObj) _trailerDict
                        .get(DICT_KEY_ENCRYPT);
            } else if (encryptObj instanceof PdfDictionary) {
                _encryptDict = (PdfDictionary) _trailerDict
                        .get(DICT_KEY_ENCRYPT);
            }
            _encrypted = (_encryptDictRef != null) || (_encryptDict != null);

            PdfObject infoObj = _trailerDict.get(DICT_KEY_INFO);
            if (infoObj != null && !(infoObj instanceof PdfIndirectObj)) {
                throw new PdfInvalidException(MessageConstants.PDF_HUL_76, // PDF-HUL-76
                        _parser.getOffset());
            }
            _docInfoDictRef = (PdfIndirectObj) infoObj;

            obj = _trailerDict.get(DICT_KEY_ID); // This is at least v. 1.1
            if (obj != null) {
                if (obj instanceof PdfArray) {
                    String[] id = new String[2];
                    try {
                        PdfArray idArray = (PdfArray) obj;
                        Vector<PdfObject> idVec = idArray.getContent();
                        if (idVec.size() != 2) {
                            throw new PdfInvalidException(
                                    MessageConstants.PDF_HUL_77); // PDF-HUL-77
                        }
                        PdfSimpleObject idobj = (PdfSimpleObject) idVec.get(0);
                        id[0] = toHex(((StringValuedToken) idobj.getToken())
                                .getRawBytes());
                        idobj = (PdfSimpleObject) idVec.get(1);
                        id[1] = toHex(((StringValuedToken) idobj.getToken())
                                .getRawBytes());
                        _idProperty = new Property(DICT_KEY_ID,
                                PropertyType.STRING, PropertyArity.ARRAY, id);
                    } catch (Exception e) {
                        throw new PdfInvalidException(
                                MessageConstants.PDF_HUL_78); // PDF-HUL-78
                    }
                } else {
                    throw new PdfInvalidException(MessageConstants.PDF_HUL_79,
                            _parser.getOffset()); // PDF-HUL-79
                }
            }
            obj = _trailerDict.get(DICT_KEY_XREF_STREAM);
            if (obj != null) {
                /*
                 * We have a "hybrid" cross-reference scheme. This means we have
                 * to go through the cross-reference stream and have its entries
                 * supplement the cross-reference section.
                 */
                _logger.warning("Hybrid cross-reference not yet implemented");
            }
        } catch (PdfException e) {

            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            // If it's merely invalid rather than ill-formed, keep going
            return (e instanceof PdfInvalidException);
        }
        return true;
    }

    /* Parses the cross-reference table or stream. */
    protected boolean readXRefInfo(RepInfo info) throws IOException {
        if (_xrefIsStream) {
            return readXRefStreams(info);
        }
        return readXRefTables(info);
    }

    /*
     * Parses the cross-reference streams. This is called from
     * readXRefInfo if there is no cross-reference table.
     * I still need to deal with hybrid cases. All linked cross-reference
     * streams are handled here.
     */
    protected boolean readXRefStreams(RepInfo info) throws IOException {
        _pdfACompliant = false; // current version of PDF/A doesn't recognize
                                // XREF streams
        while (_startxref > 0) {
            try {
                _parser.seek(_startxref);
                PdfObject pdfStreamObj = _parser.readObjectDef();
                // the retrieved object should be stream
                if (!(pdfStreamObj instanceof PdfStream)) {
                    throw new PdfInvalidException(MessageConstants.PDF_HUL_150,
                            _parser.getOffset());
                }
                PdfStream pstream = (PdfStream) pdfStreamObj;
                int sObjNum = pstream.getObjNumber();
                CrossRefStream xstream = new CrossRefStream(pstream);
                if (!xstream.isValid()) {
                    return false;
                }
                xstream.initRead(_raf);
                int xrefSize = xstream.getCrossRefTableSize();
                if (_xref == null) {
                    _xref = new long[xrefSize];
                    _xref2 = new int[xrefSize][];
                }
                if (sObjNum < 0 || sObjNum >= xrefSize) {
                    throw new PdfMalformedException(MessageConstants.PDF_HUL_80, // PDF-HUL-80
                            _parser.getOffset());
                }
                _xref[sObjNum] = _startxref; // insert the index of the xref
                                             // stream itself
                _startxref = xstream.getPrevXref();
                try {
                    while (xstream.readNextObject()) {
                        int objNum = xstream.getObjNum();
                        if (xstream.isObjCompressed()) {
                            // Hold off on this branch
                            _xref[objNum] = -1; // defers to _xref2
                            _xref2[objNum] = new int[] {
                                    xstream.getContentStreamObjNum(),
                                    xstream.getContentStreamIndex() };
                        } else {
                            if (_xref[objNum] == 0) {
                                _xref[objNum] = xstream.getOffset();
                            }
                        }
                    }
                    _numFreeObjects += xstream.getFreeCount();
                } catch (IOException e) {
                    info.setWellFormed(false);
                    info.setMessage(
                            new ErrorMessage(MessageConstants.PDF_HUL_81, // PDF-HUL-81
                                    _parser.getOffset()));
                    return false;
                }
            } catch (PdfException e) {

                e.disparage(info);
                info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
                // If it's merely invalid rather than ill-formed, keep going
                return (e instanceof PdfInvalidException);
            }
        }
        return true; // incomplete, but let it through
    }

    /*
     * Parses the cross-reference table. This is called from
     * readXRefInfo if there is a cross-reference table.
     */
    protected boolean readXRefTables(RepInfo info) throws IOException {
        Token token = null;
        try {
            _parser.seek(_startxref);
            token = _parser.getNext(); // "xref" keyword or numeric
            if (token instanceof Keyword) {
                while ((token = _parser.getNext()) != null) {
                    int firstObj = 0;
                    // Look for the start of a cross-ref subsection, which
                    // begins with a base object number and a count.
                    if (token instanceof Numeric) {
                        firstObj = ((Numeric) token).getIntegerValue();
                    } else {
                        // On anything else, assume we're done with this
                        // section.
                        // (Most likely we've hit the keyword "trailer".
                        break;
                    }
                    token = _parser.getNext();
                    if (token instanceof Numeric) {
                        _objCount = ((Numeric) token).getIntegerValue();
                    }
                    if (_xref == null) {
                        _xref = new long[_objCount];
                    }
                    for (int i = 0; i < _objCount; i++) {
                        // In reading the cross-reference table, also check
                        // the extra syntactic requirements of PDF/A.
                        long offset = ((Numeric) _parser.getNext(Numeric.class,
                                MessageConstants.PDF_HUL_82)).getLongValue(); // PDF-HUL-82
                        _parser.getNext(); // Generation number
                        if (_parser.getWSString().length() > 1) {
                            _pdfACompliant = false;
                        }
                        token = _parser.getNext(Keyword.class,
                                MessageConstants.PDF_HUL_83); // PDF-HUL-83
                        if (_parser.getWSString().length() > 1) {
                            _pdfACompliant = false;
                        }
                        // A keyword of "n" signifies an object in use,
                        // "f" signifies a free object. If we already
                        // have an entry for this object, don't replace it.
                        String keyval = ((Keyword) token).getValue();
                        if ("n".equals(keyval)) {
                            if (_xref[firstObj + i] == 0) {
                                _xref[firstObj + i] = offset;
                            }
                        } else if ("f".equals(keyval)) {
                            _numFreeObjects++;
                        } else {
                            throw new PdfMalformedException(
                                    MessageConstants.PDF_HUL_84, // PDF-HUL-84
                                    _parser.getOffset());
                        }
                    }
                }
            }
        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            return false;
        } catch (Exception e) {
            info.setValid(false);
            String mess = MessageFormat.format(
                    MessageConstants.PDF_HUL_157.getMessage(),
                    e.getClass().getName());
            JhoveMessage message = JhoveMessages.getMessageInstance(
                    MessageConstants.PDF_HUL_157.getId(), mess);
            info.setMessage(
                    new ErrorMessage(message, e.getMessage(), _parser.getOffset()));
            return false;
        }
        return true;
    }

    private boolean readDocCatalogDict(RepInfo info) throws IOException {
        Property p = null;
        _docCatDict = null;
        _docCatalogList = new ArrayList<Property>(2);
        // Get the Root reference which we had before, and
        // resolve it to the dictionary object.
        if (_docCatDictRef == null) {
            info.setWellFormed(false);
            info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_85, 0)); // PDF-HUL-85
            return false;
        }
        try {
            _docCatDict = (PdfDictionary) resolveIndirectObject(_docCatDictRef);
        } catch (Exception e) {
            _logger.warning("Tried to cast non-dictionary to PdfDictionary");
            e.printStackTrace();
        }
        if (_docCatDict == null) {
            // If no object was returned, the PDF's not well-formed
            info.setWellFormed(false);
            info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_86, 0)); // PDF-HUL-86
            return false;
        } else if (_docCatDict.getObjNumber() != _docCatDictRef
                .getObjNumber()) {
            // If the returned object nmumber is not the same as that requested
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.warning("Inconsistent Document Catalog Object Number");
                _logger.warning(String.format(
                        " - /Root indirect reference number: %d, returned object ID: %d.",
                        _docCatDictRef.getObjNumber(), _docCatDict.getObjNumber()));
            }
            info.setWellFormed(false);
            info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_140, 0)); // PDF-HUL-140
            return false;
        }
        try {
            // Check that the catalog has a key type and the types value is
            // "Catalog"
            if (!checkTypeKey(_docCatDict, info, KEY_VAL_CATALOG,
                    MessageConstants.PDF_HUL_141, // PDF-HUL-141
                    MessageConstants.PDF_HUL_142, // PDF-HUL-142
                    MessageConstants.PDF_HUL_143)) { // PDF-HUL-143
                return false;
            }

            PdfObject viewPref = _docCatDict.get(DICT_KEY_VIEWER_PREFS);
            viewPref = resolveIndirectObject(viewPref);
            if (viewPref instanceof PdfDictionary) {
                _viewPrefDict = (PdfDictionary) viewPref;
                p = buildViewPrefProperty(_viewPrefDict);
                _docCatalogList.add(p);
            }
            String pLayoutText = DEFAULT_PAGE_LAYOUT; // default
            PdfObject pLayout = resolveIndirectObject(
                    _docCatDict.get(DICT_KEY_PAGE_LAYOUT));
            if (pLayout instanceof PdfSimpleObject) {
                pLayoutText = ((PdfSimpleObject) pLayout).getStringValue();
            }
            p = new Property(PROP_NAME_PAGE_LAYOUT, PropertyType.STRING,
                    pLayoutText);
            _docCatalogList.add(p);

            String pModeText = DEFAULT_MODE; // default
            PdfObject pMode = resolveIndirectObject(
                    _docCatDict.get(DICT_KEY_PAGE_MODE));
            if (pMode instanceof PdfSimpleObject) {
                pModeText = ((PdfSimpleObject) pMode).getStringValue();
            }
            p = new Property(DICT_KEY_PAGE_MODE, PropertyType.STRING,
                    pModeText);
            _docCatalogList.add(p);

            if (!_encrypted) {
                PdfObject outlines = resolveIndirectObject(
                        _docCatDict.get(DICT_KEY_OUTLINES));
                if (outlines instanceof PdfDictionary) {
                    _outlineDict = (PdfDictionary) outlines;
                }
            }

            PdfObject lang = resolveIndirectObject(
                    _docCatDict.get(DICT_KEY_LANG));
            if (lang != null && lang instanceof PdfSimpleObject) {
                String langText = ((PdfSimpleObject) lang).getStringValue();
                p = new Property(PROP_NAME_LANG, PropertyType.STRING,
                        _encrypted ? ENCRYPTED : langText);
                _docCatalogList.add(p);
            }

            // The Pages dictionary doesn't go into the property,
            // but this is a convenient time to grab it and the page label
            // dictionary.
            _pagesDictRef = (PdfIndirectObj) _docCatDict.get(DICT_KEY_PAGES);
            if (!_encrypted) {
                _pageLabelDict = (PdfDictionary) resolveIndirectObject(
                        _docCatDict.get(DICT_KEY_PAGE_LABELS));
            }

            // Grab the Version entry, and use it to override the
            // file header IF it's later.
            PdfObject vers = resolveIndirectObject(
                    _docCatDict.get(DICT_KEY_VERSION));
            if (vers instanceof PdfSimpleObject) {
                String versString = ((PdfSimpleObject) vers).getStringValue();
                String infoVersString = _version;
                try {
                    double ver = Double.parseDouble(versString);
                    double infoVer = Double.parseDouble(infoVersString);
                    /* Set a message if this doesn't agree with RepInfo */
                    if (ver != infoVer) {
                        String mess = MessageFormat.format(
                                MessageConstants.PDF_HUL_87.getMessage(),
                                infoVersString, versString);
                        JhoveMessage message = JhoveMessages.getMessageInstance(
                                MessageConstants.PDF_HUL_87.getId(), mess);
                        info.setMessage(new InfoMessage(message));
                    }
                    /* Replace the version in RepInfo if this is larger */
                    if (ver > infoVer) {
                        _version = versString;
                    }
                } catch (NumberFormatException e) {
                    throw new PdfInvalidException(MessageConstants.PDF_HUL_88); // PDF-HUL-88
                }
            }

            // If extensions are defined get the extensionlevel information and the
            // baseVersion from the extensions
            PdfObject extensions = _docCatDict.get(DICT_KEY_EXTENSIONS);
            if (extensions != null) {
                if (extensions instanceof PdfDictionary) {
                    Iterator<PdfObject> extensionsIter = ((PdfDictionary) extensions).iterator();
                    while (extensionsIter.hasNext()) {

                        PdfObject extensionObj = extensionsIter.next();
                        // Arlington PDF Model defines extension as a direct object
                        // https://github.com/pdf-association/arlington-pdf-model/blob/master/tsv/latest/Extensions.tsv
                        if (extensionObj instanceof PdfIndirectObj) {
                            info.setWellFormed(false);
                            JhoveMessage message = JhoveMessages.getMessageInstance(
                                    MessageConstants.PDF_HUL_156.getId(),
                                    MessageConstants.PDF_HUL_156.getMessage());
                            info.setMessage(new ErrorMessage(message)); // PDF-HUL-156
                        } else {
                            PdfDictionary extension = (PdfDictionary) extensionObj;
                            Set<String> developerPrefixKeys = ((PdfDictionary) extensions).getKeys();
                            for (String developerPrefixKey : developerPrefixKeys) {
                                if (PdfStrings.PREFIXNAMESREGISTY.contains(developerPrefixKey.toString())) {
                                    p = new Property(PROP_NAME_DEVELOPERPREFIX, PropertyType.STRING,
                                            developerPrefixKey.toString());
                                    _docCatalogList.add(p);
                                    PdfSimpleObject BaseVersion = (PdfSimpleObject) extension.get(DICT_KEY_BASEVERSION);
                                    String infoVersString = _version;
                                    String versString = BaseVersion.getStringValue();
                                    double ver = Double.parseDouble(versString);
                                    double infoVer = Double.parseDouble(infoVersString);
                                    try {
                                        if (infoVer != ver) {
                                            String mess = MessageFormat.format(
                                                    MessageConstants.PDF_HUL_87.getMessage(),
                                                    infoVersString, ver);
                                            JhoveMessage message = JhoveMessages.getMessageInstance(
                                                    MessageConstants.PDF_HUL_87.getId(), mess);
                                            info.setMessage(new InfoMessage(message));
                                        } else {
                                            p = new Property(PROP_NAME_BASEVERSION, PropertyType.STRING, ver);
                                            _docCatalogList.add(p);
                                        }
                                    } catch (NumberFormatException e) {
                                        throw new PdfInvalidException(MessageConstants.PDF_HUL_88); // PDF-HUL-88
                                    }
                                    PdfSimpleObject extensionLevel = (PdfSimpleObject) extension
                                            .get(DICT_KEY_EXTENSIONLEVEL);
                                    if (extensionLevel != null) {
                                        p = new Property(PROP_NAME_EXTENSIONLEVEL, PropertyType.INTEGER,
                                                extensionLevel.getIntValue());
                                        _docCatalogList.add(p);
                                    }
                                } else {
                                    // There is an unknown developer prefix
                                    info.setMessage(new InfoMessage(MessageConstants.PDF_HUL_154,
                                            developerPrefixKey.toString())); // PDF-HUL-154
                                }
                            }
                        }
                    }
                }
            }

            // Get the Names dictionary in order to grab the
            // EmbeddedFiles and Dests entries.
            try {
                PdfDictionary namesDict = null;
                if (!_encrypted) {
                    namesDict = (PdfDictionary) resolveIndirectObject(
                            _docCatDict.get(DICT_KEY_NAMES));
                }
                if (namesDict != null) {
                    PdfDictionary embeddedDict = (PdfDictionary) resolveIndirectObject(
                            namesDict.get(DICT_KEY_EMBEDDED_FILES));
                    if (embeddedDict != null) {
                        _embeddedFiles = new NameTreeNode(this, null,
                                embeddedDict);
                    }

                    PdfDictionary dDict = (PdfDictionary) resolveIndirectObject(
                            namesDict.get(DICT_KEY_DESTS));
                    if (dDict != null) {
                        _destNames = new NameTreeNode(this, null, dDict);
                    }
                }
            } catch (ClassCastException ce) {
                _logger.info("ClassCastException on names dictionary");
                throw new PdfInvalidException(MessageConstants.PDF_HUL_89); // PDF-HUL-89
            } catch (Exception e) {
                _logger.info("Exception on names dictionary: "
                        + e.getClass().getName());
                throw new PdfMalformedException(MessageConstants.PDF_HUL_90); // PDF-HUL-90
            }

            // Get the optional Dests dictionary. Note that destinations
            // may be specified in either of two completely different
            // ways: a dictionary here, or a name tree from the Names
            // dictionary.

            try {
                _destsDict = (PdfDictionary) resolveIndirectObject(
                        _docCatDict.get(DICT_KEY_DESTS));
            } catch (ClassCastException ce) {
                _logger.info("ClassCastException on dests dictionary");
                throw new PdfInvalidException(MessageConstants.PDF_HUL_91); // PDF-HUL-91
            } catch (Exception e) {
                _logger.info("Exception on dests dictionary: "
                        + e.getClass().getName());
                throw new PdfMalformedException(MessageConstants.PDF_HUL_92); // PDF-HUL-92
            }
        }

        catch (PdfException e) {
            e.disparage(info); // clears Valid or WellFormed as appropriate
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            // Keep going if it's only invalid
            return (e instanceof PdfInvalidException);
        } catch (Exception e) {
            // Unexpected exception -- declare not well-formed
            info.setWellFormed(false);
            info.setValid(false);
            String mess = MessageFormat.format(
                    MessageConstants.PDF_HUL_158.getMessage(),
                    e.getClass().getName());
            JhoveMessage message = JhoveMessages.getMessageInstance(
                    MessageConstants.PDF_HUL_158.getId(), mess);
            info.setMessage(
                    new ErrorMessage(message, e.getMessage(), _parser.getOffset()));
            return false;
        }
        return true;
    }

    protected boolean readEncryptDict(RepInfo info) throws IOException {
        String filterText = "";
        String effText = null;
        // Get the reference which we had before, and
        // resolve it to the dictionary object.
        if (_encryptDictRef == null && _encryptDict == null) {
            return true; // encryption entry is optional
        }
        try {
            _encryptList = new ArrayList<Property>(6);
            if (_encryptDict == null) {
                _encryptDict = (PdfDictionary) resolveIndirectObject(_encryptDictRef);
            }

            PdfObject filter = _encryptDict.get(DICT_KEY_FILTER);
            if (filter instanceof PdfSimpleObject) {
                Token tok = ((PdfSimpleObject) filter).getToken();
                if (tok instanceof Name) {
                    filterText = ((Name) tok).getValue();
                }
            }
            Property p = new Property(PROP_NAME_SECURITY_HANDLER,
                    PropertyType.STRING, filterText);
            _encryptList.add(p);
            // PdfObject eff = dict.get("EFF");
            if (filter instanceof PdfSimpleObject) {
                Token tok = ((PdfSimpleObject) filter).getToken();
                if (tok instanceof Name) {
                    effText = ((Name) tok).getValue();
                }
            }
            if (effText != null) {
                p = new Property(PROP_NAME_EFF, PropertyType.STRING, effText);
                _encryptList.add(p);
            }

            int algValue = 0;
            PdfObject algorithm = _encryptDict.get(DICT_KEY_V);
            if (algorithm instanceof PdfSimpleObject) {
                Token tok = ((PdfSimpleObject) algorithm).getToken();
                if (tok instanceof Numeric) {
                    algValue = ((Numeric) tok).getIntegerValue();
                    if (_je != null && _je.getShowRawFlag()) {
                        p = new Property(PROP_NAME_ALGORITHM,
                                PropertyType.INTEGER, new Integer(algValue));
                    } else {
                        try {
                            p = new Property(PROP_NAME_ALGORITHM,
                                    PropertyType.STRING,
                                    PdfStrings.ALGORITHM[algValue]);
                        } catch (ArrayIndexOutOfBoundsException aioobe) {
                            throw new PdfInvalidException // PDF-HUL-93
                            (MessageConstants.PDF_HUL_93, _parser.getOffset());
                        }
                    }
                    if (p != null) {
                        _encryptList.add(p);
                    }
                }
            }

            int keyLen = 40;
            PdfObject length = _encryptDict.get(DICT_KEY_LENGTH);
            if (length instanceof PdfSimpleObject) {
                Token tok = ((PdfSimpleObject) length).getToken();
                if (tok instanceof Numeric) {
                    keyLen = ((Numeric) tok).getIntegerValue();
                }
                if (_je != null) {
                    p = new Property(PROP_NAME_KEY_LENGTH, PropertyType.INTEGER,
                            new Integer(keyLen));
                    _encryptList.add(p);
                }
            }

            if (FILTER_VAL_STANDARD.equals(filterText)) {
                List<Property> stdList = new ArrayList<Property>(4);
                // Flags have a known meaning only if Standard
                // security handler was specified
                PdfObject flagObj = _encryptDict.get(DICT_KEY_P);
                PdfObject revObj = _encryptDict.get(DICT_KEY_R);
                int rev = 2; // assume old rev if not present
                if (revObj instanceof PdfSimpleObject) {
                    rev = ((PdfSimpleObject) revObj).getIntValue();
                }
                if (flagObj instanceof PdfSimpleObject) {
                    int flags = ((PdfSimpleObject) flagObj).getIntValue();
                    String[] flagStrs;
                    if (rev == 2) {
                        flagStrs = PdfStrings.USERPERMFLAGS2;
                    } else {
                        flagStrs = PdfStrings.USERPERMFLAGS3;
                    }
                    p = buildUserPermProperty(flags, flagStrs);
                    stdList.add(p);

                    stdList.add(new Property(PROP_NAME_REVISION,
                            PropertyType.INTEGER, new Integer(rev)));
                }
                PdfObject oObj = _encryptDict.get("O");
                if (oObj != null) {
                    if (oObj instanceof PdfSimpleObject) {
                        stdList.add(new Property(PROP_NAME_OWNER_STRING,
                                PropertyType.STRING,
                                toHex(((PdfSimpleObject) oObj).getRawBytes())));
                    }
                }
                PdfObject uObj = _encryptDict.get("U");
                if (uObj != null) {
                    if (uObj instanceof PdfSimpleObject) {
                        stdList.add(new Property(PROP_NAME_USER_STRING,
                                PropertyType.STRING,
                                toHex(((PdfSimpleObject) uObj).getRawBytes())));
                    }
                }
                // Required if ExtensionLevel 3 and Encryption Algorithm (V) is 5
                // Defined in Adobe® Supplement to the ISO 32000
                if (algValue == 5) {
                    PdfObject oeObj = _encryptDict.get("OE");
                    if (oeObj != null) {
                        if (oeObj instanceof PdfSimpleObject) {
                            stdList.add(new Property(PROP_NAME_OWNERKEY_STRING,
                                    PropertyType.STRING,
                                    toHex(((PdfSimpleObject) oeObj).getRawBytes())));
                        }
                    } else {
                        // if algValue is 5; OE is mandatory
                        throw new PdfInvalidException(MessageConstants.PDF_HUL_152, _parser.getOffset());
                    }
                    PdfObject ueObj = _encryptDict.get("UE");
                    if (ueObj != null) {
                        if (ueObj instanceof PdfSimpleObject) {
                            stdList.add(new Property(PROP_NAME_USERKEY_STRING,
                                    PropertyType.STRING,
                                    toHex(((PdfSimpleObject) ueObj).getRawBytes())));
                        }
                    } else {
                        // if algValue is 5; UE is mandatory
                        throw new PdfInvalidException(MessageConstants.PDF_HUL_153, _parser.getOffset());
                    }
                }
                _encryptList.add(new Property(
                        PROP_NAME_STANDARD_SECURITY_HANDLER,
                        PropertyType.PROPERTY, PropertyArity.LIST, stdList));
            }
            PdfObject streamEncrypted = _encryptDict.get(DICT_KEY_STMF);
            if (streamEncrypted instanceof PdfSimpleObject) {
                _streamsEncrypted = true;
            }

        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            return (e instanceof PdfInvalidException);
        }
        return true;
    }

    protected boolean readDocInfoDict(RepInfo info) {
        // Get the Info reference which we had before, and
        // resolve it to the dictionary object.
        if (_docInfoDictRef == null) {
            return true; // Info is optional
        }
        _docInfoList = new ArrayList<Property>(9);
        try {
            _docInfoDict = (PdfDictionary) resolveIndirectObject(
                    _docInfoDictRef);
            addStringProperty(_docInfoDict, _docInfoList, DICT_KEY_TITLE,
                    PROP_NAME_TITLE);
            addStringProperty(_docInfoDict, _docInfoList, DICT_KEY_AUTHOR,
                    PROP_NAME_AUTHOR);
            addStringProperty(_docInfoDict, _docInfoList, DICT_KEY_SUBJECT,
                    PROP_NAME_SUBJECT);
            addStringProperty(_docInfoDict, _docInfoList, DICT_KEY_KEYWORDS,
                    PROP_NAME_KEYWORDS);
            addStringProperty(_docInfoDict, _docInfoList, DICT_KEY_CREATOR,
                    PROP_NAME_CREATOR);
            addStringProperty(_docInfoDict, _docInfoList, DICT_KEY_PRODUCER,
                    PROP_NAME_PRODUCER);

            // CreationDate requires string-to-date conversion
            // ModDate does too
            addDateProperty(_docInfoDict, _docInfoList, DICT_KEY_CREATION_DATE,
                    PROP_NAME_CREATION_DATE);
            addDateProperty(_docInfoDict, _docInfoList, DICT_KEY_MODIFIED_DATE,
                    PROP_NAME_MODIFIED_DATE);
            addStringProperty(_docInfoDict, _docInfoList, DICT_KEY_TRAPPED,
                    PROP_NAME_TRAPPED);
        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            // Keep parsing if it's only invalid
            return (e instanceof PdfInvalidException);
        } catch (Exception e) {
            info.setWellFormed(false);
            String mess = MessageFormat.format(
                    MessageConstants.PDF_HUL_94.getMessage(),
                    e.getClass().getName());
            JhoveMessage message = JhoveMessages.getMessageInstance(
                    MessageConstants.PDF_HUL_94.getId(), mess);
            info.setMessage(new ErrorMessage(message)); // PDF-HUL-94
        }
        return true;
    }

    protected boolean readDocumentTree(RepInfo info) {
        try {
            if (_pagesDictRef == null) {
                throw new PdfInvalidException(MessageConstants.PDF_HUL_95); // PDF-HUL-95
            }

            PdfObject pagesObj = resolveIndirectObject(_pagesDictRef);
            if (pagesObj != null && !(pagesObj instanceof PdfDictionary)) {
                throw new PdfMalformedException(MessageConstants.PDF_HUL_97); // PDF-HUL-97
            } else if (pagesObj != null) {

                PdfDictionary pagesDict = (PdfDictionary) pagesObj;

                // Check that the pages dict has a key type and the types value is
                // Pages
                if (!checkTypeKey(pagesDict, info, KEY_VAL_PAGES,
                        MessageConstants.PDF_HUL_146, // PDF-HUL-146
                        MessageConstants.PDF_HUL_144, // PDF-HUL-144
                        MessageConstants.PDF_HUL_145)) { // PDF-HUL-145
                    return false;
                }

                _docTreeRoot = new PageTreeNode(this, null, pagesDict);
                _docTreeRoot.buildSubtree(true, MAX_PAGE_TREE_DEPTH);
            }
        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            // Continue parsing if it's only invalid
            return (e instanceof PdfInvalidException);
        } catch (ArrayIndexOutOfBoundsException excep) {
            info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_96,
                    _parser.getOffset())); // PDF-HUL-96
            info.setWellFormed(false);
            return false;
        } catch (Exception e) {
            // Catch any odd exceptions
            String mess = MessageFormat.format(
                    MessageConstants.PDF_HUL_98.getMessage(),
                    e.getClass().getName());
            JhoveMessage message = JhoveMessages.getMessageInstance(
                    MessageConstants.PDF_HUL_98.getId(), mess);
            info.setMessage(new ErrorMessage(message, _parser.getOffset())); // PDF-HUL-98
            info.setWellFormed(false);
            return false;
        }
        return true;
    }

    protected boolean readPageLabelTree(RepInfo info) {
        // the page labels number tree is optional.
        try {
            if (_pageLabelDict != null) {
                _pageLabelRoot = new PageLabelNode(this, null, _pageLabelDict);
                _pageLabelRoot.buildSubtree();
            }
        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            // Continue parsing if it's only invalid
            return (e instanceof PdfInvalidException);
        } catch (Exception e) {
            info.setWellFormed(false);
            String mess = MessageFormat.format(
                    MessageConstants.PDF_HUL_99.getMessage(),
                    e.getClass().getName());
            JhoveMessage message = JhoveMessages.getMessageInstance(
                    MessageConstants.PDF_HUL_99.getId(), mess);
            info.setMessage(new ErrorMessage(message)); // PDF-HUL-99
            return false;
        }
        return true; // always succeeds
    }

    protected boolean readXMPData(RepInfo info) {
        try {
            PdfStream metadata = (PdfStream) resolveIndirectObject(
                    _docCatDict.get(DICT_KEY_METADATA));
            if (metadata == null) {
                return true; // Not required
            }
            // PdfDictionary metaDict = metadata.getDict ();

            // Create an InputSource to feed the parser.
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XMLReader parser = factory.newSAXParser().getXMLReader();
            PdfXMPSource src = new PdfXMPSource(metadata, getFile());
            XMPHandler handler = new XMPHandler();
            parser.setContentHandler(handler);
            parser.setErrorHandler(handler);

            // We have to parse twice. The first time, we may get
            // an encoding change as part of an exception thrown. If this
            // happens, we create a new InputSource with the encoding, and
            // continue.
            try {
                parser.parse(src);
                _xmpProp = src.makeProperty();
            } catch (SAXException se) {
                String msg = se.getMessage();
                if (msg != null && msg.startsWith(ENCODING_PREFIX)) {
                    String encoding = msg.substring(5);
                    try {
                        src = new PdfXMPSource(metadata, getFile(), encoding);
                        parser.parse(src);
                        _xmpProp = src.makeProperty();
                    } catch (UnsupportedEncodingException uee) {
                        _logger.log(Level.INFO,
                                "Attempt to use explicit encoding to parse XMP metadata failed.",
                                uee);
                        throw new PdfInvalidException(
                                MessageConstants.PDF_HUL_100); // PDF-HUL-100
                    }
                }
            }

        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            // Continue parsing if it's only invalid
            return (e instanceof PdfInvalidException);
        } catch (Exception e) {
            info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_101, // PDF-HUL-101
                    _parser.getOffset()));
            info.setValid(false);
            return false;
        }
        return true;
    }

    protected void findExternalStreams(RepInfo info) throws IOException {
        _extStreamsList = new LinkedList<Property>();
        // stop processing if there is no root for the document tree
        if (_docTreeRoot == null)
            return;
        _docTreeRoot.startWalk();
        try {
            for (;;) {
                // Get all the page objects in the document sequentially
                PageObject page = _docTreeRoot.nextPageObject();
                if (page == null) {
                    break;
                }
                // Get the streams for the page and walk through them
                List<PdfStream> streams = page.getContentStreams();
                if (streams != null) {
                    ListIterator<PdfStream> streamIter = streams.listIterator();
                    while (streamIter.hasNext()) {
                        PdfStream stream = streamIter.next();
                        String specStr = stream.getFileSpecification();
                        if (specStr != null) {
                            Property prop = new Property(PROP_NAME_FILE,
                                    PropertyType.STRING, specStr);
                            _extStreamsList.add(prop);
                        }
                    }
                }
            }
        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage()));
        } catch (Exception e) {
            info.setWellFormed(false);
            String mess = MessageFormat.format(
                    MessageConstants.PDF_HUL_102.getMessage(),
                    e.getClass().getName());
            JhoveMessage message = JhoveMessages.getMessageInstance(
                    MessageConstants.PDF_HUL_102.getId(), mess);
            info.setMessage(new ErrorMessage(message)); // PDF-HUL-102
        }
    }

    /**
     * Locates the filters in the content stream dictionaries
     * and generate a list of unique pipelines.
     *
     * @return <code>false</code> if the filter structure is
     *         defective.
     */
    protected boolean findFilters(RepInfo info) throws IOException {
        _filtersList = new LinkedList<Property>();
        // stop processing if there is no root for the document tree
        if (_docTreeRoot == null)
            return false;
        _docTreeRoot.startWalk();
        try {
            for (;;) {
                // Get all the page objects in the document sequentially
                PageObject page = _docTreeRoot.nextPageObject();
                if (page == null) {
                    break;
                }
                // Get the streams for the page and walk through them
                List<PdfStream> streams = page.getContentStreams();
                if (streams != null) {
                    ListIterator<PdfStream> streamIter = streams.listIterator();
                    while (streamIter.hasNext()) {
                        PdfStream stream = streamIter.next();
                        Filter[] filters = stream.getFilters();
                        extractFilters(filters, stream);
                    }
                }
            }
        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            // Continue parsing if it's only invalid
            return (e instanceof PdfInvalidException);
        }
        return true;
    }

    /**
     * Finds the filters in a stream or array object which is the value
     * of a stream's Filter key, and put them in _filtersList
     * if a duplicate isn't there already. If the name is
     * "Crypt", appends a colon and the name if available.
     * Returns the filter string whether it's added or not,
     * or null if there are no filters.
     */
    protected String extractFilters(Filter[] filters, PdfStream stream) {
        /*
         * Concatenate the names into a string of names separated
         * by spaces.
         */
        int len = filters.length;
        if (len == 0) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < len; i++) {
            Filter filt = filters[i];
            String fname = filt.getFilterName();
            buf.append(fname);
            /* If it's a Crypt filter, add the crypt name. */
            if (FILTER_NAME_CRYPT.equals(fname)) {
                String cname = filt.getNameParam();
                if (cname != null) {
                    buf.append(":" + cname);
                }
            }
            if (i < len - 1) {
                buf.append(' ');
            }
        }
        String filterStr = buf.toString();
        boolean unique = true;
        // Check for uniqueness.
        Iterator<Property> iter = _filtersList.iterator();
        while (iter.hasNext()) {
            Property p = iter.next();
            String s = (String) p.getValue();
            if (s.equals(filterStr)) {
                unique = false;
                break;
            }
        }
        if (filterStr != null && unique) {
            Property prop = new Property(PROP_NAME_FILTER_PIPELINE,
                    PropertyType.STRING, filterStr);
            _filtersList.add(prop);
        }
        return filterStr;
    }

    protected void findImages(RepInfo info) throws IOException {
        _imagesList = new LinkedList<Property>();
        // needed if object streams are encrypted
        if (_docTreeRoot == null) {
            return;
        }
        _docTreeRoot.startWalk();
        try {
            for (;;) {
                // Get all the page objects in the document sequentially
                PageObject page = _docTreeRoot.nextPageObject();
                if (page == null) {
                    break;
                }
                // Get the resources for the page and look for image XObjects
                PdfDictionary rsrc = page.getResources();
                if (rsrc != null) {
                    PdfDictionary xo = (PdfDictionary) resolveIndirectObject(
                            rsrc.get(RESOURCE_NAME_XOBJECT));
                    if (xo != null) {
                        Iterator<PdfObject> iter = xo.iterator();
                        while (iter.hasNext()) {
                            // Get an XObject and check if it's an image.
                            _logger.info("Getting image");
                            PdfDictionary xobdict = null;
                            PdfObject xob = resolveIndirectObject(iter.next());
                            if (xob instanceof PdfStream) {
                                xobdict = ((PdfStream) xob).getDict();
                            }
                            if (xobdict != null) {
                                PdfSimpleObject subtype = (PdfSimpleObject) xobdict
                                        .get(DICT_KEY_XOBJ_SUBTYPE);
                                if (XOBJ_SUBTYPE_IMAGE
                                        .equals(subtype.getStringValue())) {
                                    // It's an image XObject. Report stuff.
                                    _logger.info("Image XObject");
                                    List<Property> imgList = new ArrayList<Property>(
                                            10);
                                    Property prop = new Property(
                                            PROP_NAME_IMAGE,
                                            PropertyType.PROPERTY,
                                            PropertyArity.LIST, imgList);
                                    NisoImageMetadata niso = new NisoImageMetadata();
                                    imgList.add(new Property(
                                            PROP_NAME_NISO_IMAGE_MD,
                                            PropertyType.NISOIMAGEMETADATA,
                                            niso));
                                    PdfObject widthBase = xobdict
                                            .get(DICT_KEY_WIDTH);
                                    PdfSimpleObject widObj = (PdfSimpleObject) resolveIndirectObject(
                                            widthBase);
                                    PdfObject heightBase = xobdict
                                            .get(DICT_KEY_HEIGHT);
                                    PdfSimpleObject htObj = (PdfSimpleObject) resolveIndirectObject(
                                            heightBase);
                                    if (widObj != null || htObj != null) {
                                        niso.setImageWidth(widObj.getIntValue());
                                        niso.setImageLength(htObj.getIntValue());
                                    } else {
                                        info.setWellFormed(false);
                                        JhoveMessage message = JhoveMessages.getMessageInstance(
                                                MessageConstants.PDF_HUL_159.getId(),
                                                MessageConstants.PDF_HUL_159.getMessage());
                                        info.setMessage(new ErrorMessage(message)); // PDF-HUL-159
                                    }
                                    // Check for filters to add to the filter
                                    // list
                                    Filter[] filters = ((PdfStream) xob)
                                            .getFilters();
                                    // Try to derive the image MIME type from
                                    // filter names
                                    String mimeType = imageMimeFromFilters(
                                            filters);
                                    niso.setMimeType(mimeType);
                                    String filt = extractFilters(filters,
                                            (PdfStream) xob);
                                    if (filt != null) {
                                        // If the filter is one which the NISO
                                        // schema
                                        // knows about, put it in the NISO
                                        // metadata,
                                        // otherwise put it in a Filter
                                        // property.
                                        int nisoFilt = nameToNiso(filt,
                                                compressionStrings,
                                                compressionValues);
                                        if (nisoFilt >= 0) {
                                            /*
                                             * If it's 2, it's a CCITTFaxDecode
                                             * filter. There may be an optional
                                             * K entry that can change the
                                             * value.
                                             */
                                            PdfObject parms = xobdict.get(
                                                    DICT_KEY_DECODE_PARAMS);
                                            if (parms != null) {
                                                PdfSimpleObject kobj = null;
                                                if (parms instanceof PdfDictionary) {
                                                    PdfDictionary pdict = (PdfDictionary) parms;
                                                    kobj = (PdfSimpleObject) resolveIndirectObject(
                                                            pdict.get(DICT_KEY_K));
                                                }
                                                /*
                                                 * Note that the DecodeParms
                                                 * value may also be an array
                                                 * of dictionaries. We are not
                                                 * handling that contingency.
                                                 */
                                                if (kobj != null) {
                                                    int k = kobj.getIntValue();
                                                    if (k < 0) {
                                                        nisoFilt = 4;
                                                    } else if (k > 0) {
                                                        nisoFilt = 3;
                                                    }
                                                }
                                            }
                                            niso.setCompressionScheme(nisoFilt);
                                        } else {
                                            imgList.add(new Property(
                                                    PROP_NAME_FILTER,
                                                    PropertyType.STRING, filt));
                                        }
                                    } else {
                                        niso.setCompressionScheme(1); // no
                                                                      // filter
                                    }

                                    // Check for color space info
                                    PdfObject colorSpc = xobdict
                                            .get(DICT_KEY_COLOR_SPACE);
                                    if (colorSpc != null) {
                                        String colorName = null;
                                        if (colorSpc instanceof PdfSimpleObject) {
                                            colorName = ((PdfSimpleObject) colorSpc)
                                                    .getStringValue();
                                        } else if (colorSpc instanceof PdfArray) {
                                            Vector<PdfObject> vec = ((PdfArray) colorSpc)
                                                    .getContent();
                                            // Use the first element, which is
                                            // the color space family
                                            PdfSimpleObject fam = (PdfSimpleObject) vec
                                                    .elementAt(0);
                                            colorName = fam.getStringValue();
                                        }
                                        if (colorName != null) {
                                            int nisoSpace = nameToNiso(
                                                    colorName,
                                                    colorSpaceStrings,
                                                    colorSpaceValues);
                                            if (nisoSpace >= 0) {
                                                niso.setColorSpace(nisoSpace);
                                            } else {
                                                imgList.add(new Property(
                                                        PROP_NAME_COLOR_SPACE,
                                                        PropertyType.STRING,
                                                        colorName));
                                            }
                                        }
                                    }

                                    PdfSimpleObject bpc = (PdfSimpleObject) xobdict
                                            .get(DICT_KEY_BITS_PER_COMPONENT);
                                    if (bpc != null) {
                                        // imgList.add(new
                                        // Property(DICT_KEY_BITS_PER_COMPONENT,
                                        // PropertyType.INTEGER,
                                        // new Integer (bpc.getIntValue())));
                                        niso.setBitsPerSample(new int[] {
                                                bpc.getIntValue() });
                                    }

                                    PdfSimpleObject intent = (PdfSimpleObject) xobdict
                                            .get(DICT_KEY_INTENT);
                                    if (intent != null) {
                                        imgList.add(new Property(
                                                PROP_NAME_INTENT,
                                                PropertyType.STRING,
                                                intent.getStringValue()));
                                    }

                                    PdfSimpleObject imgmsk = (PdfSimpleObject) xobdict
                                            .get(DICT_KEY_IMAGE_MASK);
                                    if (imgmsk != null) {
                                        boolean b = imgmsk.isTrue();
                                        imgList.add(new Property(
                                                PROP_NAME_IMAGE_MASK,
                                                PropertyType.BOOLEAN,
                                                Boolean.valueOf(b)));
                                    }

                                    PdfArray dcd = (PdfArray) xobdict
                                            .get(DICT_KEY_DECODE);
                                    if (dcd != null) {
                                        Vector<PdfObject> dcdvec = dcd
                                                .getContent();
                                        List<Integer> dcdlst = new ArrayList<Integer>(
                                                dcdvec.size());
                                        Iterator<PdfObject> diter = dcdvec
                                                .iterator();
                                        while (diter.hasNext()) {
                                            PdfSimpleObject d = (PdfSimpleObject) diter
                                                    .next();
                                            dcdlst.add(new Integer(
                                                    d.getIntValue()));
                                        }
                                        imgList.add(new Property(
                                                PROP_NAME_DECODE,
                                                PropertyType.INTEGER,
                                                PropertyArity.LIST, dcdlst));
                                    }

                                    PdfSimpleObject intrp = (PdfSimpleObject) xobdict
                                            .get(DICT_KEY_INTERPOLATE);
                                    if (intrp != null) {
                                        boolean b = intrp.isTrue();
                                        imgList.add(new Property(
                                                PROP_NAME_INTERPOLATE,
                                                PropertyType.BOOLEAN,
                                                Boolean.valueOf(b)));
                                    }

                                    PdfSimpleObject nam = (PdfSimpleObject) xobdict
                                            .get(DICT_KEY_NAME);
                                    if (nam != null) {
                                        imgList.add(new Property(PROP_NAME_NAME,
                                                PropertyType.STRING,
                                                nam.getStringValue()));
                                    }

                                    PdfSimpleObject id = (PdfSimpleObject) resolveIndirectObject(
                                            xobdict.get(DICT_KEY_ID));
                                    if (id != null) {
                                        String idstr = toHex(
                                                id.getStringValue());
                                        imgList.add(new Property(PROP_NAME_ID,
                                                PropertyType.STRING, idstr));
                                    }

                                    _imagesList.add(prop);
                                }

                            }
                        }
                    }
                }
            }
        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
        } catch (Exception e) {
            info.setWellFormed(false);
            String mess = MessageFormat.format(
                    MessageConstants.PDF_HUL_103.getMessage(),
                    e.getClass().getName());
            JhoveMessage message = JhoveMessages.getMessageInstance(
                    MessageConstants.PDF_HUL_103.getId(), mess);
            info.setMessage(new ErrorMessage(message)); // PDF-HUL-103
        }
    }

    /*
     * Convert a Filter name to a NISO compression scheme value.
     * If the name is unknown to NISO, return -1.
     */
    protected int nameToNiso(String name, String[] nameArray, int[] valArray) {
        for (int i = 0; i < nameArray.length; i++) {
            if (nameArray[i].equals(name)) {
                return valArray[i];
            }
        }
        return -1; // no match
    }

    protected void findFonts(RepInfo info) throws IOException {
        _type0FontsMap = new HashMap<Integer, PdfObject>();
        _type1FontsMap = new HashMap<Integer, PdfObject>();
        _trueTypeFontsMap = new HashMap<Integer, PdfObject>();
        _mmFontsMap = new HashMap<Integer, PdfObject>();
        _type3FontsMap = new HashMap<Integer, PdfObject>();
        _cid0FontsMap = new HashMap<Integer, PdfObject>();
        _cid2FontsMap = new HashMap<Integer, PdfObject>();
        // needed if object streams are encrypted
        if (_docTreeRoot == null) {
            return;
        }
        try {
            _docTreeRoot.startWalk();
            for (;;) {
                // This time we need all the page objects and page tree
                // nodes, because resources can be inherited from
                // page tree nodes.
                DocNode node = _docTreeRoot.nextDocNode();
                if (node == null) {
                    break;
                }
                // Get the fonts for the node
                PdfDictionary fonts = null;
                fonts = node.getFontResources();
                if (fonts != null) {
                    // In order to make sure we have a collection of
                    // unique fonts, we store them in a map keyed by
                    // object number.
                    Iterator<PdfObject> fontIter = fonts.iterator();
                    while (fontIter.hasNext()) {
                        PdfObject fontRef = fontIter.next();
                        PdfObject font = resolveIndirectObject(fontRef);
                        if (font instanceof PdfDictionary) {
                            addFontToMap((PdfDictionary) font);
                        } else {
                            // Expected a dictionary
                            info.setWellFormed(false);
                            info.setMessage(new ErrorMessage(
                                    MessageConstants.PDF_HUL_104, // PDF-HUL-104
                                    _parser.getOffset()));
                            return;
                        }
                        // If we've been directed appropriately,
                        // we accumulate the information, but don't
                        // report it. In that case, we post a message
                        // just once to that effect.
                        if (!_skippedFontsReported && !_showFonts
                                && _verbosity != Module.MAXIMUM_VERBOSITY) {
                            info.setMessage(new InfoMessage(
                                    MessageConstants.PDF_HUL_105)); // PDF-HUL-105
                            _skippedFontsReported = true;
                        }
                    }
                }
            }
        } catch (PdfException e) {
            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            return;
        } catch (Exception e) {
            // Unexpected exception.
            _logger.log(Level.WARNING,
                    MessageConstants.PDF_HUL_106.getMessage(), e);
            info.setWellFormed(false);
            info.setMessage(new ErrorMessage(MessageConstants.PDF_HUL_106, // PDF-HUL-106
                    e.toString(), _parser.getOffset()));
            return;
        }
    }

    /**
     * Add the font to the appropriate map, and return its subtype.
     * If we've exceeded the maximum number of fonts, then ignore it.
     */
    protected String addFontToMap(PdfDictionary font) {
        if (++_nFonts > maxFonts) {
            return null;
        }
        String subtypeStr = null;
        try {
            PdfSimpleObject subtype = (PdfSimpleObject) font
                    .get(DICT_KEY_FONT_SUBTYPE);
            subtypeStr = subtype.getStringValue();
            if (FONT_TYPE0.equals(subtypeStr)) {
                _type0FontsMap.put(new Integer(font.getObjNumber()), font);
                // If the font is Type 0, we must go
                // through its descendant fonts
                PdfObject desc0 = font.get(DICT_KEY_DESCENDANT_FONTS);
                PdfArray descendants = (PdfArray) resolveIndirectObject(desc0);
                Vector<PdfObject> subfonts = descendants.getContent();
                Iterator<PdfObject> subfontIter = subfonts.iterator();
                while (subfontIter.hasNext()) {
                    PdfObject subfont = subfontIter.next();
                    subfont = resolveIndirectObject(subfont);
                    addFontToMap((PdfDictionary) subfont);
                }
            } else if (FONT_TYPE1.equals(subtypeStr)) {
                _type1FontsMap.put(new Integer(font.getObjNumber()), font);
            } else if (FONT_MM_TYPE1.equals(subtypeStr)) {
                _mmFontsMap.put(new Integer(font.getObjNumber()), font);
            } else if (FONT_TYPE3.equals(subtypeStr)) {
                _type3FontsMap.put(new Integer(font.getObjNumber()), font);
            } else if (FONT_TRUE_TYPE.equals(subtypeStr)) {
                _trueTypeFontsMap.put(new Integer(font.getObjNumber()), font);
            } else if (FONT_CID_TYPE0.equals(subtypeStr)) {
                _cid0FontsMap.put(new Integer(font.getObjNumber()), font);
            } else if (FONT_CID_TYPE2.equals(subtypeStr)) {
                _cid2FontsMap.put(new Integer(font.getObjNumber()), font);
            }
            return subtypeStr;
        } catch (Exception e) {
            return null;
        }
    }

    /******************************************************************
     * PRIVATE CLASS METHODS.
     ******************************************************************/

    protected static String toHex(String s) {
        StringBuffer buffer = new StringBuffer("0x");

        int len = s.length();
        for (int i = 0; i < len; i++) {
            String h = Integer.toHexString(s.charAt(i));
            if (h.length() < 2) {
                buffer.append("0");
            }
            buffer.append(h);
        }

        return buffer.toString();
    }

    protected static String toHex(Vector<Integer> v) {
        StringBuffer buffer = new StringBuffer("0x");

        int len = v.size();
        for (int i = 0; i < len; i++) {
            int hdigit = v.elementAt(i).intValue();
            String h = Integer.toHexString(hdigit);
            if (h.length() < 2) {
                buffer.append("0");
            }
            buffer.append(h);
        }

        return buffer.toString();
    }

    /**
     * If the argument is an indirect object reference,
     * returns the object it resolves to, otherwise returns
     * the object itself. In particular, calling with null will
     * return null.
     */
    public PdfObject resolveIndirectObject(PdfObject obj)
            throws PdfException, IOException {
        if (obj instanceof PdfIndirectObj) {
            int objIndex = ((PdfIndirectObj) obj).getObjNumber();
            /*
             * Here we need to allow for the possibility that the
             * object is compressed in an object stream. That means
             * creating a new structure (call it _xref2) that contains
             * the stream object number and offset whenever _xref[objIndex]
             * is negative. _xref2 will have to contain the content
             * stream object number (which will itself have to be
             * resolved) and the offset into the object stream.
             */
            return getObject(objIndex, MAX_OBJ_STREAM_DEPTH);
        }
        return obj;
    }

    /**
     * Returns an object of a given number. This may involve
     * recursion into object streams, in which case it calls itself.
     *
     * @param objIndex
     *                 The object number to look up
     * @param recGuard
     *                 The maximum permitted number of recursion levels;
     *                 no particular value is required, but 30 or more
     *                 should avoid false exceptions.
     */
    protected PdfObject getObject(int objIndex, int recGuard)
            throws PdfException, IOException {
        /* Guard against infinite recursion */
        if (recGuard <= 0) {
            throw new PdfMalformedException(MessageConstants.PDF_HUL_107);
        }
        long offset = _xref[objIndex];
        if (offset == 0) {
            return null; // This is considered legitimate by the spec
        }
        if (offset < 0) {
            return getObjectFromStream(objIndex, recGuard);
        }
        _parser.seek(offset);
        PdfObject obj = _parser.readObjectDef(this);
        //
        // Experimental carl@openpreservation.org 2018-03-14
        //
        // Previously all object numbers (ids) were overwritten even if they'd
        // previously been assigned.
        //
        // This is caused by a little confusion where the object ID and the
        // index of the _xref array are used interchangeably when they're not
        // the same thing. There's an assumption when for the _xref array
        // that the objects will have continuous numeric object numbers. This
        // means that the object number and array position will always be the
        // same. The setting of the object number meant that the wrong object
        // could
        // be returned with the id changed to match the id requested.
        //
        // My guess is that the assignment was put in to ensure that an
        // object that escaped initialisation had an object number. If that's
        // the case then the code below will still allow that to happen but
        // will prevent assigned numbers from been overwritten by the xref array
        // position.
        if (obj.getObjNumber() == -1) {
            obj.setObjNumber(objIndex);
        }
        return obj;
    }

    /**
     * Return the RandomAccessFile being read.
     */
    public RandomAccessFile getFile() {
        return _raf;
    }

    /**
     * Returns the catalog dictionary object.
     */
    public PdfDictionary getCatalogDict() {
        return _docCatDict;
    }

    /**
     * Returns the trailer dictionary object.
     */
    public PdfDictionary getTrailerDict() {
        return _trailerDict;
    }

    /**
     * Returns the viewer preferences dictionary object.
     */
    public PdfDictionary getViewPrefDict() {
        return _viewPrefDict;
    }

    /**
     * Returns the outlines dictionary object.
     */
    public PdfDictionary getOutlineDict() {
        return _outlineDict;
    }

    /**
     * Get a font map. The map returned is determined by the selector.
     * Any other value returns null.
     */
    public Map<Integer, PdfObject> getFontMap(int selector) {
        switch (selector) {
            case F_TYPE0:
                return _type0FontsMap;
            case F_TYPE1:
                return _type1FontsMap;
            case F_TT:
                return _mmFontsMap;
            case F_TYPE3:
                return _type3FontsMap;
            case F_MM1:
                return _mmFontsMap;
            case F_CID0:
                return _cid0FontsMap;
            case F_CID2:
                return _cid2FontsMap;
            default:
                return null;
        }
    }

    /**
     * Return a List of all the font maps. Together, these contain
     * all the fonts and subfonts in the document. Some of the maps
     * may be null.
     */
    public List<Map<Integer, PdfObject>> getFontMaps() {
        List<Map<Integer, PdfObject>> lst = new ArrayList<Map<Integer, PdfObject>>(
                7);
        lst.add(_type0FontsMap);
        lst.add(_type1FontsMap);
        lst.add(_mmFontsMap);
        lst.add(_type3FontsMap);
        lst.add(_trueTypeFontsMap);
        lst.add(_cid0FontsMap);
        lst.add(_cid2FontsMap);
        return lst;
    }

    /**
     * Returns a NameTreeNode for the EmbeddedFiles entry of the
     * Names dictionary. Returns null if there isn't one.
     */
    public NameTreeNode getEmbeddedFiles() {
        return _embeddedFiles;
    }

    /**
     * Add the various font lists as a fonts property. Note: only add
     * the "Fonts" property if there are, in fact, fonts defined.
     */
    protected void addFontsProperty(List<Property> metadataList) {
        List<Property> fontTypesList = new LinkedList<Property>();
        Property fontp = null;
        if (_type0FontsMap != null && !_type0FontsMap.isEmpty()) {
            try {
                fontp = buildFontProperty(PROP_NAME_FONT_TYPE0, _type0FontsMap,
                        F_TYPE0);
                fontTypesList.add(fontp);
            } catch (ClassCastException e) {
                // Report an error here?
            }
        }
        if (_type1FontsMap != null && !_type1FontsMap.isEmpty()) {
            try {
                fontp = buildFontProperty(PROP_NAME_FONT_TYPE1, _type1FontsMap,
                        F_TYPE1);
                fontTypesList.add(fontp);
            } catch (ClassCastException e) {
                // Report an error here?
            }
        }
        if (_trueTypeFontsMap != null && !_trueTypeFontsMap.isEmpty()) {
            try {
                fontp = buildFontProperty(PROP_NAME_FONT_TRUE_TYPE,
                        _trueTypeFontsMap, F_TT);
                fontTypesList.add(fontp);
            } catch (ClassCastException e) {
                // Report an error here?
            }
        }
        if (_type3FontsMap != null && !_type3FontsMap.isEmpty()) {
            try {
                fontp = buildFontProperty(PROP_NAME_FONT_TYPE3, _type3FontsMap,
                        F_TYPE3);
                fontTypesList.add(fontp);
            } catch (ClassCastException e) {
            }
        }
        if (_mmFontsMap != null && !_mmFontsMap.isEmpty()) {
            try {
                fontp = buildFontProperty(PROP_NAME_FONT_MM_TYPE1, _mmFontsMap,
                        F_MM1);
                fontTypesList.add(fontp);
            } catch (ClassCastException e) {
            }
        }
        if (_cid0FontsMap != null && !_cid0FontsMap.isEmpty()) {
            try {
                fontp = buildFontProperty(PROP_NAME_FONT_CID_TYPE0,
                        _cid0FontsMap, F_CID0);
                fontTypesList.add(fontp);
            } catch (ClassCastException e) {
            }
        }
        if (_cid2FontsMap != null && !_cid2FontsMap.isEmpty()) {
            try {
                fontp = buildFontProperty(PROP_NAME_FONT_CID_TYPE2,
                        _cid2FontsMap, F_CID2);
                fontTypesList.add(fontp);
            } catch (ClassCastException e) {
            }
        }
        if (fontTypesList.size() > 0) {
            metadataList.add(new Property(PROP_NAME_FONTS,
                    PropertyType.PROPERTY, PropertyArity.LIST, fontTypesList));
        }
    }

    /* Build Pages property, with associated subproperties. */
    protected void addPagesProperty(List<Property> metadataList, RepInfo info) {
        _pagesList = new LinkedList<Property>();
        _pageSeqMap = new HashMap<Integer, Integer>(500);
        // needed if object streams are encrypted
        if (_docTreeRoot == null) {
            return;
        }
        try {
            _docTreeRoot.startWalk();
            int pageIndex = 0;
            // Start the pipe with two entries.
            // We always need to have the current and the next
            // entry from the page label tree in order to determine
            // the lower and upper bounds of the applicable range.
            // If the first entry has a bound greater than zero,
            // that appears to be an undefined situation, so we
            // always treat the first entry as starting at zero.
            if (_pageLabelRoot != null) {
                if (!_pageLabelRoot.findNextKeyValue()) {
                    throw new PdfMalformedException(
                            MessageConstants.PDF_HUL_111); // PDF-HUL-111
                }

                _pageLabelRoot.findNextKeyValue();
            }
            for (;;) {
                // Get all the page objects in the document sequentially
                // Have to do this in two passes so that link
                // destinations can be properly reported.
                PageObject page = _docTreeRoot.nextPageObject();
                if (page == null) {
                    break;
                }
                _pageSeqMap.put(new Integer(page.getDict().getObjNumber()),
                        new Integer(pageIndex + 1));
            }
            _docTreeRoot.startWalk();
            for (;;) {
                PageObject page = _docTreeRoot.nextPageObject();
                if (page == null) {
                    break;
                }
                Property p = buildPageProperty(page, pageIndex++, info);
                _pagesList.add(p);
            }
            if (_showPages || _verbosity == Module.MAXIMUM_VERBOSITY) {
                Property prop = new Property(PROP_NAME_PAGES,
                        PropertyType.PROPERTY, PropertyArity.LIST, _pagesList);
                metadataList.add(prop);
            } else {
                if (!_skippedPagesReported) {
                    info.setMessage(
                            new InfoMessage(MessageConstants.PDF_HUL_112)); // PDF-HUL-112
                    _skippedPagesReported = true;
                }
            }
        } catch (PdfException e) {

            e.disparage(info);
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            return;
        }
    }

    /* Build a subproperty for one PageObject. */
    protected Property buildPageProperty(PageObject page, int idx, RepInfo info)
            throws PdfException {
        List<Property> pagePropList = new ArrayList<Property>(4);
        try {
            // Foo on Java's inability to return values through
            // parameters. Passing an array is a crock to achieve
            // that effect.
            int[] nominalNum = new int[1];
            Property plProp = buildPageLabelProperty(page, idx, nominalNum);
            if (plProp != null) {
                pagePropList.add(plProp);
            }
            if (plProp == null || nominalNum[0] != idx + 1) {
                // Page sequence is different from label, or
                // there is no label. Make it 1-based.
                pagePropList.add(new Property(PROP_NAME_SEQUENCE,
                        PropertyType.INTEGER, new Integer(idx + 1)));

            }
        } catch (PdfException e) {
            throw e;
        } catch (Exception f) {
            throw new PdfMalformedException(MessageConstants.PDF_HUL_113); // PDF-HUL-113
        }

        try {
            List<Property> annotsList = new LinkedList<Property>();
            PdfArray annots = page.getAnnotations();
            if (annots != null) {
                Vector<PdfObject> contents = annots.getContent();
                for (int i = 0; i < contents.size(); i++) {
                    PdfObject annot = resolveIndirectObject(
                            contents.elementAt(i));
                    if (annot instanceof PdfDictionary) {
                        annotsList.add(buildAnnotProperty((PdfDictionary) annot,
                                info));
                    } else if (annot instanceof PdfSimpleObject
                            && ((PdfSimpleObject) annot).getToken() instanceof Comment) {
                        // ignore Comments
                        continue;

                    } else {
                        // There are annotations which aren't dictionaries. I've
                        // run into this,
                        // but it violates the spec as far as I can tell.
                        throw new PdfInvalidException(
                                MessageConstants.PDF_HUL_114); // PDF-HUL-114
                    }
                }
                if (!annotsList.isEmpty()) {
                    if (_showAnnotations
                            || _verbosity == Module.MAXIMUM_VERBOSITY) {
                        Property annotProp = new Property(PROP_NAME_ANNOTATIONS,
                                PropertyType.PROPERTY, PropertyArity.LIST,
                                annotsList);
                        pagePropList.add(annotProp);
                    } else {
                        // We don't report annotations if we got here,
                        // but we do report that we don't report them.
                        if (!_skippedAnnotationsReported) {
                            info.setMessage(new InfoMessage(
                                    MessageConstants.PDF_HUL_115)); // PDF-HUL-115
                            _skippedAnnotationsReported = true;
                        }
                    }
                }
            }
        } catch (PdfException e) {
            throw e;
        } catch (Exception f) {
            throw new PdfMalformedException(MessageConstants.PDF_HUL_116); // PDF-HUL-116
        }

        try {
            // Rotation property is inheritable
            PdfObject tempObj = page.get(DICT_KEY_ROTATE,
                    true);
            PdfSimpleObject rot = null;
            if (tempObj != null && tempObj instanceof PdfSimpleObject) {
                rot = (PdfSimpleObject) tempObj;
            } else if (tempObj != null && tempObj instanceof PdfIndirectObj) {
                rot = (PdfSimpleObject) ((PdfIndirectObj) tempObj)
                        .getObject();
            }
            if (rot != null && rot.getIntValue() != 0) {
                pagePropList.add(new Property(PROP_NAME_ROTATE,
                        PropertyType.INTEGER, new Integer(rot.getIntValue())));
            }

            // UserUnit property (1.6), not inheritable
            PdfSimpleObject uu = (PdfSimpleObject) page.get(DICT_KEY_USER_UNIT,
                    false);
            if (uu != null) {
                pagePropList.add(new Property(PROP_NAME_USER_UNIT,
                        PropertyType.DOUBLE, new Double(rot.getDoubleValue())));
            }
            // Viewport dictionaries (1.6), not inheritable
            PdfArray vp = (PdfArray) page.get(DICT_KEY_VIEWPORT, false);
            if (vp != null) {
                Vector<PdfObject> vpv = vp.getContent();
                Iterator<PdfObject> iter = vpv.iterator();
                List<Property> vplist = new ArrayList<Property>(vpv.size());
                while (iter.hasNext()) {
                    PdfDictionary vpd = (PdfDictionary) resolveIndirectObject(
                            iter.next());
                    PdfObject vpdbb = vpd.get(DICT_KEY_BBOX);
                    List<Property> vpPropList = new ArrayList<Property>();
                    vpPropList.add(makeRectProperty(
                            (PdfArray) resolveIndirectObject(vpdbb),
                            DICT_KEY_BBOX));
                    PdfObject meas = vpd.get(DICT_KEY_MEASURE);
                    if (meas instanceof PdfDictionary) {
                        vpPropList.add(
                                buildMeasureProperty((PdfDictionary) meas));
                        // No, that's wrong -- the Viewport property itself
                        // needs to be a list with a bounding box.
                    }
                    vplist.add(new Property(PROP_NAME_VIEWPORT,
                            PropertyType.PROPERTY, PropertyArity.LIST,
                            vpPropList));
                }
                pagePropList.add(new Property(PROP_NAME_VIEWPORTS,
                        PropertyType.PROPERTY, PropertyArity.LIST, vplist));
            }
            // Thumbnail -- we just report if it's there. It's a
            // non-inheritable property
            PdfObject thumb = page.get(DICT_KEY_THUMB, false);
            if (thumb != null) {
                pagePropList.add(new Property(PROP_NAME_THUMB,
                        PropertyType.BOOLEAN, Boolean.TRUE));
            }
            return new Property(PROP_NAME_PAGE, PropertyType.PROPERTY,
                    PropertyArity.LIST, pagePropList);
        } catch (PdfException e) {
            throw e;
        } catch (Exception f) {
            throw new PdfMalformedException(MessageConstants.PDF_HUL_117); // PDF-HUL-117
        }
    }

    /*
     * Build a subproperty of a subproperty for page labels.
     * The nomNumRef argument is a crock for returning the
     * nominal number; element 0 of the array is replaced
     * by the nominal number of the page.
     */
    protected Property buildPageLabelProperty(PageObject page, int pageIndex,
            int[] nomNumRef) throws PdfException {
        if (_pageLabelRoot == null) {
            return null; // no page label info
        }

        // Note that our "current" page is the page label tree's
        // "previous" key. Sorry about that...
        int curFirstPage = _pageLabelRoot.getPrevKey();
        int nextFirstPage = _pageLabelRoot.getCurrentKey();
        try {
            // If we're onto the next page range, advance our pointers.
            if (pageIndex >= nextFirstPage) {
                _pageLabelRoot.findNextKeyValue();
                curFirstPage = nextFirstPage;
            }
            PdfDictionary pageLabelDict = (PdfDictionary) resolveIndirectObject(
                    _pageLabelRoot.getPrevValue());
            StringBuffer labelText = new StringBuffer();
            PdfSimpleObject prefixObj = (PdfSimpleObject) pageLabelDict
                    .get(DICT_KEY_P);
            if (prefixObj != null) {
                labelText.append(prefixObj.getStringValue());
            }
            PdfSimpleObject firstPageObj = (PdfSimpleObject) pageLabelDict
                    .get("St");
            // Sequence start value defaults to 1 if there's no start value
            int firstPageVal = ((firstPageObj != null)
                    ? firstPageObj.getIntValue()
                    : 1);
            int nominalPage = pageIndex - curFirstPage + firstPageVal;
            if (nominalPage <= 0) {
                throw new PdfInvalidException(MessageConstants.PDF_HUL_118); // pDF-HUL-118
            }
            nomNumRef[0] = nominalPage;

            // Get the numbering style. If there is no numbering
            // style entry, the label consists only of the prefix.
            PdfSimpleObject numStyleObj = (PdfSimpleObject) pageLabelDict
                    .get("S");
            String numStyle;
            if (numStyleObj == null) {
                numStyle = null;
            } else {
                numStyle = numStyleObj.getStringValue();
            }
            if ("D".equals(numStyle)) {
                // Nice, simple decimal numbers
                labelText.append(nominalPage);
            } else if ("R".equals(numStyle)) {
                // Upper case roman numerals
                labelText.append(PageLabelNode.intToRoman(nominalPage, true));
            } else if ("r".equals(numStyle)) {
                // Lower case roman numerals
                labelText.append(PageLabelNode.intToRoman(nominalPage, false));
            } else if ("A".equals(numStyle)) {
                // Uppercase letters (A-Z, AA-ZZ, ...)
                labelText.append(PageLabelNode.intToBase26(nominalPage, true));
            } else if ("a".equals(numStyle)) {
                // Lowercase letters (a-z, aa-zz, ...)
                labelText.append(PageLabelNode.intToBase26(nominalPage, false));
            }
            // It screws up the PDF output if we have a blank Label property.
            if (labelText.length() == 0) {
                labelText.append(EMPTY_LABEL_PROPERTY);
            }
            return new Property(PROP_NAME_LABEL, PropertyType.STRING,
                    labelText.toString());
        } catch (Exception e) {
            throw new PdfMalformedException(MessageConstants.PDF_HUL_119); // PDF-HUL-119
        }
    }

    /* Build a subproperty for a measure dictionary. */
    protected Property buildMeasureProperty(PdfDictionary meas) {
        List<Property> plist = new ArrayList<Property>();
        PdfObject itemObj = meas.get(DICT_KEY_XOBJ_SUBTYPE);
        if (itemObj instanceof PdfSimpleObject) {
            plist.add(new Property(PROP_NAME_SUBTYPE, PropertyType.STRING,
                    ((PdfSimpleObject) itemObj).getStringValue()));
        }
        itemObj = meas.get(DICT_KEY_R);
        if (itemObj instanceof PdfSimpleObject) {
            plist.add(new Property(PROP_NAME_RATIO, PropertyType.STRING,
                    ((PdfSimpleObject) itemObj).getStringValue()));
        }
        // All kinds of stuff I could add -- limit it to the required
        // X, Y, D and A arrays.
        itemObj = meas.get("X");
        if (itemObj instanceof PdfArray) {
            plist.add(buildNumberFormatArrayProperty((PdfArray) itemObj, "X"));
        }
        itemObj = meas.get("Y");
        if (itemObj instanceof PdfArray) {
            plist.add(buildNumberFormatArrayProperty((PdfArray) itemObj, "Y"));
        }
        itemObj = meas.get("D");
        if (itemObj instanceof PdfArray) {
            plist.add(buildNumberFormatArrayProperty((PdfArray) itemObj, PROP_NAME_DISTANCE));
        }
        itemObj = meas.get("A");
        if (itemObj instanceof PdfArray) {
            plist.add(buildNumberFormatArrayProperty((PdfArray) itemObj, PROP_NAME_AREA));
        }
        return new Property(PROP_NAME_MEASURE, PropertyType.PROPERTY,
                PropertyArity.LIST, plist);
    }

    /* Build a subproperty for a number format array. */
    private Property buildNumberFormatArrayProperty(PdfArray arr, String propertyName) {
        Vector<PdfObject> v = arr.getContent();
        List<Property> alist = new ArrayList<>();
        for (int i = 0; i < v.size(); i++) {
            PdfObject xobj = v.elementAt(i);
            if (xobj instanceof PdfDictionary) {
                PdfObject obj = ((PdfDictionary) xobj).get("U");
                if (obj instanceof PdfSimpleObject) {
                    alist.add(new Property("Name", PropertyType.DOUBLE, ((PdfSimpleObject) obj).getDoubleValue()));
                }
                obj = ((PdfDictionary) xobj).get("C");
                if (obj instanceof PdfSimpleObject) {
                    alist.add(
                            new Property("Coefficient", PropertyType.STRING, ((PdfSimpleObject) obj).getStringValue()));
                }
            }
        }
        return new Property(propertyName, PropertyType.PROPERTY, PropertyArity.LIST, alist);
    }

    /* Build a subproperty of a subproperty for an annotation. */
    protected Property buildAnnotProperty(PdfDictionary annot, RepInfo info)
            throws PdfException {
        List<Property> propList = new ArrayList<Property>(7);
        PdfObject itemObj;
        try {
            // Subtype is required
            itemObj = annot.get(DICT_KEY_XOBJ_SUBTYPE);
            propList.add(new Property(PROP_NAME_SUBTYPE, PropertyType.STRING,
                    ((PdfSimpleObject) itemObj).getStringValue()));

            // Contents is optional for some subtypes, required for
            // others. We consider it optional here.
            itemObj = annot.get(DICT_KEY_CONTENTS);
            if (itemObj != null) {
                propList.add(
                        new Property(PROP_NAME_CONTENTS, PropertyType.STRING,
                                _encrypted ? ENCRYPTED
                                        : ((PdfSimpleObject) itemObj)
                                                .getStringValue()));
            }

            // Rectangle is required, and must be in the rectangle format
            itemObj = annot.get(DICT_KEY_RECT);
            propList.add(makeRectProperty(
                    (PdfArray) resolveIndirectObject(itemObj), PROP_NAME_RECT));

            // Name comes from the NM entry and is optional
            itemObj = annot.get("NM");
            if (itemObj != null) {
                propList.add(new Property(DICT_KEY_NAME, PropertyType.STRING,
                        _encrypted ? ENCRYPTED
                                : ((PdfSimpleObject) itemObj).getStringValue()));
            }

            // LastModified is optional. The documentation says that
            // a PDF date is preferred but not guaranteed. We just
            // put it out as a string.
            itemObj = annot.get("M");
            if (itemObj != null) {
                Literal lastModLit = (Literal) ((PdfSimpleObject) itemObj)
                        .getToken();
                Property dateProp;
                dateProp = new Property(PROP_NAME_LAST_MOD, PropertyType.STRING,
                        _encrypted ? ENCRYPTED
                                : lastModLit.getValue());

                propList.add(dateProp);
            }

            // Flags.
            itemObj = annot.get("F");
            if (itemObj != null) {
                int flagValue = ((PdfSimpleObject) itemObj).getIntValue();
                Property flagProp = (buildBitmaskProperty(flagValue,
                        PROP_NAME_FLAGS, PdfStrings.ANNOTATIONFLAGS,
                        PROP_VAL_NO_FLAGS_SET));
                if (flagProp != null) {
                    propList.add(flagProp);
                }
            }

            // Appearance dictionary -- just check if it's there.
            itemObj = annot.get("AP");
            if (itemObj != null) {
                propList.add(new Property(PROP_NAME_APP_DICT,
                        PropertyType.BOOLEAN, Boolean.TRUE));
            }

            // Action dictionary -- if it's there, set actionsExist
            itemObj = annot.get("A");
            if (itemObj != null) {
                _actionsExist = true;
                itemObj = resolveIndirectObject(itemObj);
                // Actions are as common as Destinations for
                // connecting to destination pages. If the Action
                // is of type GoTo, note its destination.
                PdfSimpleObject actionSubtype = (PdfSimpleObject) ((PdfDictionary) itemObj)
                        .get("S");
                if (actionSubtype == null) {
                    throw new PdfMalformedException(
                            MessageConstants.PDF_HUL_120); // PDF-HUL-120
                }
                if (ACTION_VAL_GOTO.equals(actionSubtype.getStringValue())) {
                    PdfObject destObj = ((PdfDictionary) itemObj).get("D");
                    if (destObj != null) {
                        addDestination(destObj, PROP_NAME_ACTION_DEST, propList,
                                info);
                    }
                }
            }

            // Destination object.
            itemObj = annot.get(DICT_KEY_DEST);
            if (itemObj != null) {
                addDestination(itemObj, PROP_NAME_DESTINATION, propList, info);
            }

            // Reply Type (RT) (1.6)
            itemObj = annot.get("RT");
            if (itemObj instanceof PdfSimpleObject) {
                String type = ((PdfSimpleObject) itemObj).getStringValue();
                propList.add(new Property(PROP_NAME_REPLY_TYPE,
                        PropertyType.STRING, type));
            }

            // Intent (IT) (1.6)
            itemObj = annot.get("IT");
            if (itemObj instanceof PdfSimpleObject) {
                String type = ((PdfSimpleObject) itemObj).getStringValue();
                propList.add(new Property(PROP_NAME_INTENT, PropertyType.STRING,
                        type));
            }

            // Callout Line (CL) (1.6)
            itemObj = annot.get("CL");
            if (itemObj instanceof PdfArray) {
                Vector<PdfObject> clData = ((PdfArray) itemObj).getContent();
                // This should be an array of numbers.
                Iterator<PdfObject> iter = clData.iterator();
                List<Double> clList = new ArrayList<Double>(6);
                while (iter.hasNext()) {
                    PdfSimpleObject clItem = (PdfSimpleObject) iter.next();
                    clList.add(new Double(clItem.getDoubleValue()));
                }
                propList.add(new Property(PROP_NAME_CALLOUT_LINE,
                        PropertyType.DOUBLE, PropertyArity.LIST, clList));
            }

            return new Property(PROP_NAME_ANNOTATION, PropertyType.PROPERTY,
                    PropertyArity.LIST, propList);
        } catch (PdfException ee) {
            // Just rethrow these
            throw ee;
        } catch (Exception e) {
            throw new PdfMalformedException(MessageConstants.PDF_HUL_121); // PDF-HUL-121
        }
    }

    /*
     * Given a PdfObject that stands for a Destination, add
     * a representative property to the property list.
     */
    protected void addDestination(PdfObject itemObj, String propName,
            List<Property> propList, RepInfo info) {
        try {
            Destination dest = new Destination(itemObj, this, false);
            if (dest.isIndirect()) {
                // Encryption messes up name trees
                if (!_encrypted) {
                    int pageObjNum = resolveIndirectDest(
                            dest.getIndirectDest(), info);
                    if (pageObjNum == -1) {
                        // The scope of the reference is outside this
                        // file, so we just report it as such.
                        propList.add(new Property(propName, PropertyType.STRING,
                                PROP_VAL_EXTERNAL));
                    } else {
                        propList.add(new Property(propName,
                                PropertyType.INTEGER, new Integer(pageObjNum)));
                    }
                }
            } else {
                if (dest.getPageDest() == null) {
                    return; // can't get the page object number
                }
                int pageObjNum = dest.getPageDestObjNumber();
                Integer destPg = _pageSeqMap.get(new Integer(pageObjNum));
                if (destPg != null) {
                    propList.add(new Property(propName, PropertyType.INTEGER,
                            destPg));
                }
            }
        } catch (PdfException e) {
            propList.add(new Property(propName, PropertyType.STRING, PROP_VAL_NULL));
            info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
            info.setValid(false);
        } catch (Exception e) {

            String msg = e.getClass().getName();
            String msg1 = e.getMessage();
            if (msg1 != null) {
                msg = msg + ": " + msg1;
            }
            JhoveMessage message = JhoveMessages.getMessageInstance(
                    MessageConstants.PDF_HUL_122.getId(), msg);
            propList.add(
                    new Property(propName, PropertyType.STRING, PROP_VAL_NULL));
            info.setMessage(new ErrorMessage(message, // PDF-HUL-122
                    _parser.getOffset()));
            info.setValid(false);
        }
    }

    /*
     * Build up a property for one of the kinds of fonts
     * in the file.
     */
    protected Property buildFontProperty(String name, Map map, int fontType) {
        List<Property> fontList = new LinkedList<Property>(); // list of fonts
        Iterator<PdfObject> fontIter = map.values().iterator();
        while (fontIter.hasNext()) {
            // For each font in the map, build a property for it,
            // which consists of a list of scalar properties. Each kind
            // of font is spec'ed to have a slightly different set of
            // properties, grumble...
            PdfDictionary dict = (PdfDictionary) fontIter.next();
            List<Property> fontPropList = oneFontPropList(dict, fontType);
            Property fProp = new Property(PROP_NAME_FONT, PropertyType.PROPERTY,
                    PropertyArity.LIST, fontPropList);
            fontList.add(fProp);
        }
        return new Property(name, PropertyType.PROPERTY, PropertyArity.LIST,
                fontList);
    }

    /* Build the Property list for a given font */
    protected List<Property> oneFontPropList(PdfDictionary dict, int fontType) {
        List<Property> fontPropList = new LinkedList<Property>();
        Property prop;
        if (fontType == F_TYPE1 || fontType == F_TYPE3 || fontType == F_MM1
                || fontType == F_TT) {
            PdfObject tempObj = dict.get(DICT_KEY_NAME);
            PdfSimpleObject nameObj = null;
            if (tempObj instanceof PdfSimpleObject) {
                nameObj = (PdfSimpleObject) tempObj;
            } else if (tempObj instanceof PdfIndirectObj) {
                nameObj = (PdfSimpleObject) ((PdfIndirectObj) tempObj)
                        .getObject();
            }

            if (nameObj != null) {
                String nameStr = nameObj.getStringValue();
                prop = new Property(DICT_KEY_NAME, PropertyType.STRING,
                        nameStr);
                fontPropList.add(prop);
            }
        }

        String baseStr = null;
        if (fontType != F_TYPE3) {
            PdfObject tempObj = dict.get(DICT_KEY_BASE_FONT);
            PdfSimpleObject baseFontObj = null;
            if (tempObj instanceof PdfSimpleObject) {
                baseFontObj = (PdfSimpleObject) tempObj;
            } else if (tempObj instanceof PdfIndirectObj) {
                baseFontObj = (PdfSimpleObject) ((PdfIndirectObj) tempObj)
                        .getObject();
            }

            if (baseFontObj != null) {
                baseStr = baseFontObj.getStringValue();
                prop = new Property(PROP_NAME_BASE_FONT, PropertyType.STRING,
                        baseStr);
                fontPropList.add(prop);
            }
        }

        if (fontType == F_CID0 || fontType == F_CID2) {
            PdfObject elCid = dict.get(DICT_KEY_CID_INFO);
            try {
                elCid = resolveIndirectObject(elCid);
            } catch (Exception e) {
            }
            if (elCid instanceof PdfDictionary) {
                prop = buildCIDInfoProperty((PdfDictionary) elCid);
                fontPropList.add(prop);
            }
        }

        if (fontType == F_TYPE1 || fontType == F_TT || fontType == F_MM1) {
            if (isFontSubset(baseStr)) {
                prop = new Property(PROP_NAME_FONT_SUBSET, PropertyType.BOOLEAN,
                        Boolean.TRUE);
                fontPropList.add(prop);
            }
        }

        if (fontType == F_TYPE1 || fontType == F_TT || fontType == F_MM1
                || fontType == F_TYPE3) {
            PdfObject firstCharObj = dict.get(DICT_KEY_FIRST_CHAR);
            if (firstCharObj instanceof PdfIndirectObj) {
                firstCharObj = ((PdfIndirectObj) firstCharObj).getObject();
            }
            try {
                int firstChar = ((PdfSimpleObject) firstCharObj).getIntValue();
                prop = new Property(PROP_NAME_FIRST_CHAR, PropertyType.INTEGER,
                        new Integer(firstChar));
                fontPropList.add(prop);
            } catch (Exception e) {
            }

            PdfObject lastCharObj = dict.get(DICT_KEY_LAST_CHAR);
            if (lastCharObj instanceof PdfIndirectObj) {
                lastCharObj = ((PdfIndirectObj) lastCharObj).getObject();
            }
            try {
                int lastChar = ((PdfSimpleObject) lastCharObj).getIntValue();
                prop = new Property(PROP_NAME_LAST_CHAR, PropertyType.INTEGER,
                        new Integer(lastChar));
                fontPropList.add(prop);
            } catch (Exception e) {
            }
        }

        if (fontType == F_TYPE3) {
            // Put FontBBox and CharProcs into properties
            PdfObject bboxObj = dict.get(DICT_KEY_FONT_BBOX);
            try {
                if (bboxObj instanceof PdfArray) {
                    fontPropList.add(makeRectProperty((PdfArray) bboxObj,
                            PROP_VAL_FONT_BBOX));
                }
            } catch (Exception e) {
            }

            // For CharProcs, we're just checking if it's there.
            // (It's required for a Type 3 font.)
            // PdfObject charProcs = dict.get("CharProcs");
            // prop = new Property("CharProcs",
            // PropertyType.BOOLEAN,
            // Boolean.valueOf(charProcs != null));
            // fontPropList.add(prop);
        }

        if (fontType == F_TYPE1 || fontType == F_TT || fontType == F_MM1
                || fontType == F_CID0 || fontType == F_CID2) {
            PdfObject descriptorObj = dict.get(DICT_KEY_FONT_DESCRIPTOR);
            try {
                descriptorObj = resolveIndirectObject(descriptorObj);
            } catch (Exception e) {
            }
            if (descriptorObj instanceof PdfDictionary) {
                prop = buildFontDescriptorProperty(
                        (PdfDictionary) descriptorObj);
                fontPropList.add(prop);
            }
        }

        PdfObject encodingObj = dict.get(DICT_KEY_ENCODING);
        try {
            encodingObj = resolveIndirectObject(encodingObj);
        } catch (Exception e) {
        }

        if (fontType == F_TYPE0 || fontType == F_TYPE1 || fontType == F_TT
                || fontType == F_MM1 || fontType == F_TYPE3) {
            // Encoding property -- but only if Encoding is a name
            if (encodingObj instanceof PdfSimpleObject) {
                prop = new Property(PROP_NAME_ENCODING, PropertyType.STRING,
                        ((PdfSimpleObject) encodingObj).getStringValue());
                fontPropList.add(prop);
            }
        }

        if (fontType == F_TYPE1 || fontType == F_TT || fontType == F_MM1
                || fontType == F_TYPE3) {
            if (encodingObj != null && encodingObj instanceof PdfDictionary) {
                prop = buildEncodingDictProperty((PdfDictionary) encodingObj);
                fontPropList.add(prop);
            }
        }

        if (fontType == F_TYPE0) {
            // Encoding is reported as a CMapDictionary property for type 0
            if (encodingObj != null && encodingObj instanceof PdfStream) {
                prop = buildCMapDictProperty((PdfStream) encodingObj);
                fontPropList.add(prop);
            }
        }

        if (fontType == F_TYPE3) {
            // All we're interested in for Resources is whether
            // the dictionary exists
            PdfObject rsrc = dict.get(DICT_KEY_RESOURCES);
            if (rsrc != null) {
                prop = new Property(PROP_NAME_RESOURCES, PropertyType.BOOLEAN,
                        Boolean.TRUE);
                fontPropList.add(prop);
            }
        }

        if (fontType == F_TYPE0 || fontType == F_TYPE1 || fontType == F_TT
                || fontType == F_MM1 || fontType == F_TYPE3) {
            PdfObject toUniObj = dict.get(DICT_KEY_TO_UNICODE);
            if (toUniObj != null) {
                prop = new Property(PROP_NAME_TO_UNICODE, PropertyType.BOOLEAN,
                        Boolean.TRUE);
                fontPropList.add(prop);
            }
        }

        return fontPropList;
    }

    /*
     * Code for CMapProperty for Type 0 fonts, based on the Encoding
     * entry, broken out of buildFontProperty.
     */
    protected Property buildCMapDictProperty(PdfStream encoding) {
        PdfDictionary dict = encoding.getDict();
        List<Property> propList = new ArrayList<Property>(4);
        Property prop = new Property(PROP_NAME_CMAP_DICT, PropertyType.PROPERTY,
                PropertyArity.LIST, propList);
        Property subprop;

        // PdfObject mapName = dict.get ("CMapName");

        PdfObject cidSysInfo = dict.get(DICT_KEY_CID_INFO);
        // We can use buildCIDInfoProperty here to build the subproperty
        PdfDictionary cidDict;
        List<Property> cidList = new LinkedList<Property>();
        try {
            if (cidSysInfo instanceof PdfDictionary) {
                // One CIDInfo dictionary
                cidDict = (PdfDictionary) cidSysInfo;
                subprop = buildCIDInfoProperty(cidDict);
                cidList.add(subprop);
            } else if (cidSysInfo instanceof PdfArray) {
                // Many CIDInfo dictionaries
                Vector<PdfObject> v = ((PdfArray) cidSysInfo).getContent();
                for (int i = 0; i < v.size(); i++) {
                    cidDict = (PdfDictionary) v.elementAt(i);
                    Property subsubprop = buildCIDInfoProperty(cidDict);
                    cidList.add(subsubprop);
                }
            }
        } catch (Exception e) {
        }

        if (!cidList.isEmpty()) {
            subprop = new Property(PROP_NAME_CID_INFOS, PropertyType.PROPERTY,
                    PropertyArity.LIST, cidList);
            propList.add(subprop);
        }

        // PdfObject wMod = dict.get("WMode");
        // PdfObject useCMap = dict.get("UseCMap");

        return prop;
    }

    /*
     * Code for CIDInfoProperty for CIDFontType0 and CIDFontType2
     * conts.
     */
    protected Property buildCIDInfoProperty(PdfDictionary dict) {
        List<Property> propList = new ArrayList<Property>(3);
        Property prop = new Property(PROP_NAME_CID_INFO, PropertyType.PROPERTY,
                PropertyArity.LIST, propList);
        Property subprop;

        // Add the registry identifier
        PdfObject reg = dict.get(DICT_KEY_REGISTRY);
        if (reg instanceof PdfSimpleObject) {
            try {
                String regText = ((PdfSimpleObject) reg).getStringValue();
                subprop = new Property(PROP_NAME_REGISTRY, PropertyType.STRING,
                        _encrypted ? ENCRYPTED : regText);
                propList.add(subprop);
            } catch (Exception e) {
            }
        }

        // Add the name of the char collection within the registry
        PdfObject order = dict.get(DICT_KEY_ORDERING);
        if (reg instanceof PdfSimpleObject) {
            try {
                String ordText = ((PdfSimpleObject) order).getStringValue();
                subprop = new Property(PROP_NAME_REGISTRY, PropertyType.STRING,
                        ordText);
                propList.add(subprop);
            } catch (Exception e) {
            }
        }

        PdfObject supp = dict.get(DICT_KEY_SUPPLEMENT);
        if (supp instanceof PdfSimpleObject) {
            try {
                int suppvalue = ((PdfSimpleObject) supp).getIntValue();
                subprop = new Property(PROP_NAME_SUPPLEMENT,
                        PropertyType.INTEGER, new Integer(suppvalue));
                propList.add(subprop);
            } catch (Exception e) {
            }
        }
        return prop;
    }

    /*
     * Code for EncodingDictionary Property for type 1, 3, TrueType, and
     * MM fonts. This is based on a dictionary entry with the same name
     * as the one for buildCMapDictProperty, but different information.
     * Included properties are BaseEncoding and Differences.
     */
    protected Property buildEncodingDictProperty(PdfDictionary encodingDict) {
        List<Property> propList = new ArrayList<Property>(2);
        Property prop = new Property(PROP_NAME_ENCODING_DICTIONARY,
                PropertyType.PROPERTY, PropertyArity.LIST, propList);
        PdfObject baseEnc = encodingDict.get(DICT_KEY_BASE_ENCODING);
        if (baseEnc instanceof PdfSimpleObject) {
            String baseEncString = ((PdfSimpleObject) baseEnc).getStringValue();
            if (baseEncString != null) {
                Property baseEncProp = new Property(PROP_NAME_BASE_ENCODING,
                        PropertyType.STRING, baseEncString);
                propList.add(baseEncProp);
            }
        }

        PdfObject diffs = encodingDict.get(DICT_KEY_DIFFERENCES);
        Property diffsProp = new Property(PROP_NAME_DIFFERENCES,
                PropertyType.BOOLEAN, Boolean.valueOf(diffs != null));
        propList.add(diffsProp);

        return prop;
    }

    /*
     * Separated-out code for FontDescriptor property. This
     * is a list of six Properies: FontName, Flags,
     * FontBBox, FontFile, FontFile2, and FontFile3.
     */
    protected Property buildFontDescriptorProperty(PdfDictionary encodingDict) {
        List<Property> propList = new ArrayList<Property>(6);
        Property prop = new Property(PROP_NAME_FONT_DESC, PropertyType.PROPERTY,
                PropertyArity.LIST, propList);
        Property subprop;
        try {
            PdfSimpleObject fName = (PdfSimpleObject) encodingDict
                    .get(DICT_KEY_FONT_NAME);
            String fNameStr = fName.getStringValue();
            subprop = new Property(PROP_NAME_FONT_NAME, PropertyType.STRING,
                    fNameStr);
            propList.add(subprop);
        } catch (Exception e) {
        }

        try {
            PdfSimpleObject flags = (PdfSimpleObject) encodingDict
                    .get(DICT_KEY_FLAGS);
            int flagValue = flags.getIntValue();
            subprop = buildBitmaskProperty(flagValue, PROP_NAME_FLAGS,
                    PdfStrings.FONTDESCFLAGS, PROP_VAL_NO_FLAGS_SET);
            if (subprop != null) {
                propList.add(subprop);
            }
        } catch (Exception e) {
        }

        try {
            PdfArray bboxObj = (PdfArray) encodingDict.get(DICT_KEY_FONT_BBOX);
            double[] bbox = bboxObj.toRectangle();
            // toRectangle is written to return an array of double,
            // which is what the bounding box is in the most general
            // case; but the spec requires an array of integer, so
            // we convert is. This may seem like an excess of work,
            // but I'd rather have toRectangle do the right thing
            // rather than losing generality.
            if (bbox != null) {
                int[] ibbox = new int[4];
                for (int i = 0; i < 4; i++) {
                    ibbox[i] = (int) bbox[i];
                }
                subprop = new Property(PROP_NAME_FONT_BBOX,
                        PropertyType.INTEGER, PropertyArity.ARRAY, ibbox);
                propList.add(subprop);
            }
        } catch (Exception e) {
        }

        PdfObject fontFile = encodingDict.get(DICT_KEY_FONT_FILE);
        if (fontFile != null) {
            // All we care about is whether it exists or not
            subprop = new Property(PROP_NAME_FONT_FILE, PropertyType.BOOLEAN,
                    Boolean.TRUE);
            propList.add(subprop);
        }
        fontFile = encodingDict.get(DICT_KEY_FONT_FILE_2);
        if (fontFile != null) {
            subprop = new Property(PROP_NAME_FONT_FILE_2, PropertyType.BOOLEAN,
                    Boolean.TRUE);
            propList.add(subprop);
        }
        fontFile = encodingDict.get(DICT_KEY_FONT_FILE_3);
        if (fontFile != null) {
            subprop = new Property(PROP_NAME_FONT_FILE_3, PropertyType.BOOLEAN,
                    Boolean.TRUE);
            propList.add(subprop);
        }
        return prop;
    }

    protected Property buildViewPrefProperty(PdfDictionary prefDict) {
        Property p;
        PdfObject ob;
        boolean b;
        String s;
        List<Property> propList = new ArrayList<Property>(12);
        Property prop = new Property(DICT_KEY_VIEWER_PREFS,
                PropertyType.PROPERTY, PropertyArity.LIST, propList);

        ob = prefDict.get(DICT_KEY_HIDE_TOOLBAR);
        if (ob instanceof PdfSimpleObject) {
            b = ((PdfSimpleObject) ob).isTrue();
        } else {
            b = false;
        }
        p = new Property(PROP_NAME_HIDE_TOOLBAR, PropertyType.BOOLEAN,
                Boolean.valueOf(b));
        propList.add(p);

        ob = prefDict.get(DICT_KEY_HIDE_MENUBAR);
        if (ob instanceof PdfSimpleObject) {
            b = ((PdfSimpleObject) ob).isTrue();
        } else {
            b = false;
        }
        p = new Property(PROP_NAME_HIDE_MENUBAR, PropertyType.BOOLEAN,
                Boolean.valueOf(b));
        propList.add(p);

        ob = prefDict.get(DICT_KEY_HIDE_WINDOW_UI);
        if (ob instanceof PdfSimpleObject) {
            b = ((PdfSimpleObject) ob).isTrue();
        } else {
            b = false;
        }
        p = new Property(PROP_NAME_HIDE_WINDOW_UI, PropertyType.BOOLEAN,
                Boolean.valueOf(b));
        propList.add(p);

        ob = prefDict.get(DICT_KEY_FIT_WINDOW);
        if (ob instanceof PdfSimpleObject) {
            b = ((PdfSimpleObject) ob).isTrue();
        } else {
            b = false;
        }
        p = new Property(PROP_NAME_FIT_WINDOW, PropertyType.BOOLEAN,
                Boolean.valueOf(b));
        propList.add(p);

        ob = prefDict.get(DICT_KEY_CENTER_WINDOW);
        if (ob instanceof PdfSimpleObject) {
            b = ((PdfSimpleObject) ob).isTrue();
        } else {
            b = false;
        }
        p = new Property(PROP_NAME_CENTER_WINDOW, PropertyType.BOOLEAN,
                Boolean.valueOf(b));
        propList.add(p);

        ob = prefDict.get(DICT_KEY_DISP_DOC_TITLE);
        if (ob instanceof PdfSimpleObject) {
            b = ((PdfSimpleObject) ob).isTrue();
        } else {
            b = false;
        }
        p = new Property(PROP_NAME_DISP_DOC_TITLE, PropertyType.BOOLEAN,
                Boolean.valueOf(b));
        propList.add(p);

        ob = prefDict.get(DICT_KEY_NO_FULL_PAGE);
        if (ob instanceof PdfSimpleObject) {
            s = ((PdfSimpleObject) ob).getStringValue();
        } else
            s = DEFAULT_MODE;
        p = new Property(PROP_NAME_NO_FULL_PAGE, PropertyType.STRING, s);
        propList.add(p);

        ob = prefDict.get(DICT_KEY_DIRECTION);
        if (ob instanceof PdfSimpleObject) {
            s = ((PdfSimpleObject) ob).getStringValue();
        } else
            s = "L2R";
        p = new Property(PROP_NAME_DIRECTION, PropertyType.STRING, s);
        propList.add(p);

        ob = prefDict.get(DICT_KEY_VIEW_AREA);
        if (ob instanceof PdfSimpleObject) {
            s = ((PdfSimpleObject) ob).getStringValue();
        } else
            s = PROP_VAL_CROP_BOX;
        p = new Property(PROP_NAME_VIEW_AREA, PropertyType.STRING, s);
        propList.add(p);

        ob = prefDict.get(DICT_KEY_VIEW_CLIP);
        if (ob instanceof PdfSimpleObject) {
            s = ((PdfSimpleObject) ob).getStringValue();
        } else
            s = PROP_VAL_CROP_BOX;
        p = new Property(PROP_NAME_VIEW_CLIP, PropertyType.STRING, s);
        propList.add(p);

        ob = prefDict.get(DICT_KEY_PRINT_AREA);
        if (ob instanceof PdfSimpleObject) {
            s = ((PdfSimpleObject) ob).getStringValue();
        } else
            s = PROP_VAL_CROP_BOX;
        p = new Property(PROP_NAME_PRINT_AREA, PropertyType.STRING, s);
        propList.add(p);

        ob = prefDict.get(DICT_KEY_PAGE_CLIP);
        if (ob instanceof PdfSimpleObject) {
            s = ((PdfSimpleObject) ob).getStringValue();
        } else
            s = PROP_VAL_CROP_BOX;
        p = new Property(PROP_NAME_PAGE_CLIP, PropertyType.STRING, s);
        propList.add(p);
        return prop;
    }

    /*
     * Return TRUE if the string is a font subset string, which begins
     * with six uppercase letters and then a plus sign
     */
    protected boolean isFontSubset(String baseStr) {
        if (baseStr == null || baseStr.length() < 7) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            char ch = baseStr.charAt(i);
            if (!Character.isUpperCase(ch)) {
                return false;
            }
        }
        return (baseStr.charAt(6) == '+');
    }

    /*
     * Create the "Outlines" property from the Outlines item in the
     * catalog dictionary. As a side effect, we set the actionsExist
     * flag if any Actions are found. Because we check destinations,
     * this can't be called till the page tree is built.
     *
     * Outlines can be recursive, according to Adobe people, so we have
     * to track visited nodes.
     */
    protected Property buildOutlinesProperty(PdfDictionary dict, RepInfo info)
            throws PdfException {
        _recursionWarned = false;
        _visitedOutlineNodes = new HashSet<Integer>();
        List<Property> itemList = new LinkedList<Property>();
        Property prop = new Property(PROP_NAME_OUTLINES, PropertyType.PROPERTY,
                PropertyArity.LIST, itemList);
        try {
            PdfObject item = resolveIndirectObject(dict.get(DICT_KEY_FIRST));
            // In PDF 1.4, "First" and "Last" are unconditionally required.
            // However,
            // in 1.6, they can be omitted if there are no open or closed
            // outline items.
            // Strictly speaking, we should do several additional checks, but
            // letting the
            // outline go as empty seems sufficient.
            // if (item == null || !(item instanceof PdfDictionary)) {
            // throw new PdfInvalidException("Outline dictionary missing
            // required entry");
            // }
            int listCount = 0; // Guard against looping
            while (item != null) {
                Integer onum = new Integer(item.getObjNumber());
                Property p = buildOutlineItemProperty((PdfDictionary) item,
                        info);
                itemList.add(p);
                item = resolveIndirectObject(
                        ((PdfDictionary) item).get(DICT_KEY_NEXT));
                if (item == null) {
                    break;
                }
                // Check if this object is its own sibling. (It really does
                // happen!)
                if (item.getObjNumber() == onum.intValue()) {
                    if (!_recursionWarned) {
                        info.setMessage(
                                new InfoMessage(MessageConstants.PDF_HUL_123)); // PDF-HUL-123
                        _recursionWarned = true;
                    }
                    break;
                }
                if (++listCount > 2000) {
                    break;
                }
            }
        } catch (PdfException e1) {
            throw e1;
        } catch (Exception e) {
            throw new PdfMalformedException(MessageConstants.PDF_HUL_124); // PDF-HUL-124
        }
        if (itemList.isEmpty()) {
            return null;
        }
        return prop;
    }

    /*
     * Create an item property within the outlines hierarchy. If an
     * Outline item property has children, then there is a list
     * property called "Children" with elements called "Item".
     * It calls itself recursively to walk down the outline.
     */
    protected Property buildOutlineItemProperty(PdfDictionary dict,
            RepInfo info) throws PdfException {
        List<Property> itemList = new ArrayList<Property>(3);
        try {
            Property prop = new Property(PROP_NAME_ITEM, PropertyType.PROPERTY,
                    PropertyArity.LIST, itemList);
            PdfSimpleObject title = (PdfSimpleObject) resolveIndirectObject(
                    dict.get(DICT_KEY_TITLE));
            if (title == null) {
                throw new PdfInvalidException(MessageConstants.PDF_HUL_125); // PDF-HUL-125
            }
            itemList.add(new Property(PROP_NAME_TITLE, PropertyType.STRING,
                    _encrypted ? ENCRYPTED : title.getStringValue()));

            // Check other required stuff
            if (dict.get(DICT_KEY_PARENT) == null) {
                throw new PdfInvalidException(MessageConstants.PDF_HUL_126); // PDF-HUL-126
            }
            PdfObject cnt = dict.get(DICT_KEY_COUNT);
            if (cnt != null && (!(cnt instanceof PdfSimpleObject)
                    || !(((PdfSimpleObject) cnt)
                            .getToken() instanceof Numeric))) {
                throw new PdfInvalidException(MessageConstants.PDF_HUL_127); // PDF-HUL-127
            }
            // The entries for Prev, Next, First, and Last must
            // all be indirect references or absent. Just cast them to
            // throw an exception if they're something else
            @SuppressWarnings("unused")
            PdfIndirectObj ob = (PdfIndirectObj) dict.get(DICT_KEY_PREV);
            ob = (PdfIndirectObj) dict.get(DICT_KEY_NEXT);
            ob = (PdfIndirectObj) dict.get(DICT_KEY_FIRST);
            ob = (PdfIndirectObj) dict.get(DICT_KEY_LAST);

            // Check if there are Actions in the outline. This saves going
            // through the outlines all over again if a Profile checker
            // needs to know this. We flag only the existence of one or more
            // Actions
            // in the document.
            if (dict.get("A") != null) {
                _actionsExist = true;
            }

            PdfObject destObj = dict.get(DICT_KEY_DEST);
            if (destObj != null) {
                destObj = resolveIndirectObject(destObj);
                Destination dest = new Destination(destObj, this, false);
                if (dest.isIndirect()) {
                    itemList.add(new Property(PROP_NAME_DESTINATION,
                            PropertyType.STRING, dest.getIndirectDest().getStringValue()));
                } else {
                    int pageObjNum = dest.getPageDestObjNumber();
                    Integer destPg = _pageSeqMap.get(new Integer(pageObjNum));
                    if (destPg != null) {
                        itemList.add(new Property(PROP_NAME_DESTINATION,
                                PropertyType.INTEGER, destPg));
                    }
                }
            }

            PdfDictionary child = (PdfDictionary) resolveIndirectObject(
                    dict.get(DICT_KEY_FIRST));
            if (child != null) {
                List<Property> childList = new LinkedList<Property>();
                Property childProp = new Property(PROP_NAME_CHILDREN,
                        PropertyType.PROPERTY, PropertyArity.LIST, childList);
                // We aren't catching all possible combinations of looping. Put
                // a maximum
                // on the list just to be safe.
                int listCount = 0;
                while (child != null) {
                    Integer onum = new Integer(child.getObjNumber());
                    if (_visitedOutlineNodes.contains(onum)) {
                        /* We have recursion! */
                        if (!_recursionWarned) {
                            // Warn of recursion
                            info.setMessage(new InfoMessage(
                                    MessageConstants.PDF_HUL_128)); // PDF-HUL-128
                            _recursionWarned = true;
                        }
                    } else {
                        _visitedOutlineNodes.add(onum);
                        Property p = buildOutlineItemProperty(child, info);
                        childList.add(p);
                    }
                    child = (PdfDictionary) resolveIndirectObject(
                            child.get(DICT_KEY_NEXT));
                    if (child == null) {
                        break;
                    }
                    // Check if this object is its own sibling. (It really does
                    // happen!)
                    if (child.getObjNumber() == onum.intValue()) {
                        if (!_recursionWarned) {
                            info.setMessage(new InfoMessage(
                                    MessageConstants.PDF_HUL_129)); // PDF-HUL-129
                            _recursionWarned = true;
                        }
                        break;
                    }
                    if (++listCount > 2000)
                        break; // safety check
                }
                itemList.add(childProp);
            }
            return prop;
        } catch (PdfException pe) {
            throw pe;
        } catch (ClassCastException ce) {
            throw new PdfInvalidException(MessageConstants.PDF_HUL_130); // PDF-HUL-130
        } catch (Exception e) {
            throw new PdfInvalidException(MessageConstants.PDF_HUL_131); // PDF-HUL-131
        }
    }

    /*
     * This is separated out from readDocCatalogDict, where it
     * would otherwise make sense, because we can't build
     * the outlines property till we have a page tree to
     * locate destinations.
     */
    protected boolean doOutlineStuff(RepInfo info) {
        if (_outlineDict != null) {
            try {
                Property oprop = buildOutlinesProperty(_outlineDict, info);
                if (_showOutlines || _verbosity == Module.MAXIMUM_VERBOSITY) {
                    if (oprop != null) {
                        _docCatalogList.add(oprop);
                    }
                } else if (!_skippedOutlinesReported) {
                    // We report that we aren't reporting skipped outlines
                    info.setMessage(
                            new InfoMessage(MessageConstants.PDF_HUL_132)); // PDF-HUL-132
                    _skippedOutlinesReported = true;
                }
            } catch (PdfException e) {
                info.setMessage(new ErrorMessage(e.getJhoveMessage(), _parser.getOffset()));
                e.disparage(info);
                // If it's just invalid, we can keep going
                return (e instanceof PdfInvalidException);
            }
        }
        return true;
    }

    /*
     * Given a PdfSimpleObject representing a key,
     * look up the Destination which it references.
     * There are two completely different ways this can be done,
     * though any given PDF file is supposed to implement only one.
     * If _destsDict is non-null, we look the string up there, and
     * may find either a dictionary or an array. Otherwise
     * if _destNames is non-null, it's a NameTreeNode which contains
     * the mapping. In either case, the destination could be
     * external, in which case we just return a string saying so.
     * (The implementation of Destinations in PDF is a prime example
     * of design by stone soup.)
     * We return the page sequence number for the referenced page.
     * If we can't find a match for the reference, we return -1.
     */
    protected int resolveIndirectDest(PdfSimpleObject key, RepInfo info) throws PdfException, IOException {
        if (key == null) {
            throw new IllegalArgumentException("Argument key can not be null");
        }
        _logger.finest("Looking for indirectly referenced Dest: "
                + key.getStringValue());
        if (_destNames == null)
            return -1;
        PdfObject destObj = _destNames.get(key.getRawBytes());
        // Was the Dest this annotation refers to found in the document?
        if (destObj == null) {
            // Treat this condition as invalid:
            String mess = MessageFormat.format(
                    MessageConstants.PDF_HUL_149.getMessage(),
                    key.getStringValue());
            JhoveMessage message = JhoveMessages.getMessageInstance(
                    MessageConstants.PDF_HUL_149.getId(), mess);
            throw new PdfInvalidException(message); // PDF-HUL-149
        }
        Destination dest = new Destination(destObj, this, true);
        return dest.getPageDestObjNumber();
    }

    /* Build the user permission property., */
    protected Property buildUserPermProperty(int flags, String[] flagStrs) {
        return buildBitmaskProperty(flags, "UserAccess", flagStrs,
                "No permissions");
    }

    /**
     * Add a string property, based on a dictionary entry
     * with a string value, to a specified List.
     */
    protected void addStringProperty(PdfDictionary dict,
            List<Property> propList, String key, String propName) {
        String propText = null;
        PdfObject propObject = dict.get(key);
        if (propObject instanceof PdfSimpleObject) {
            Token tok = ((PdfSimpleObject) propObject).getToken();
            if (tok instanceof Literal) {
                if (_encrypted) {
                    propText = ENCRYPTED;
                } else {
                    propText = ((Literal) tok).getValue();
                }
                propList.add(
                        new Property(propName, PropertyType.STRING, (propText == null) ? "" : propText));
            }
        }
    }

    /**
     * Add a date property, based on a dictionary entry
     * with a string value, to a specified List.
     */
    protected void addDateProperty(PdfDictionary dict, List<Property> propList,
            String key, String propName) throws PdfInvalidException {
        if (_encrypted) {
            String propText = ENCRYPTED;
            propList.add(new Property(propName, PropertyType.STRING, propText));
        } else {
            PdfObject propObject = dict.get(key);
            if (propObject instanceof PdfSimpleObject) {
                Token tok = ((PdfSimpleObject) propObject).getToken();
                if (tok instanceof Literal) {
                    Literal lit = (Literal) tok;
                    if (!lit.getValue().isEmpty()) {
                        Date propDate = lit.parseDate();
                        if (propDate != null) {
                            propList.add(new Property(propName, PropertyType.DATE, propDate));
                        }
                    }
                }
            }
        }
    }

    /*
     * General function for adding a property with a 32-bit
     * value, with an array of Strings to interpret
     * the value as a bitmask.
     */
    protected Property buildBitmaskProperty(int val, String name,
            String[] valueNames, String defaultStr) {
        if (_je != null && _je.getShowRawFlag()) {
            return new Property(name, PropertyType.INTEGER, new Integer(val));
        }
        List<String> slist = new LinkedList<String>();
        try {
            for (int i = 0; i < valueNames.length; i++) {
                if ((val & (1 << i)) != 0 && valueNames[i].length() > 0) {
                    slist.add(valueNames[i]);
                }
            }
            // Provision for a default string if the property
            // would otherwise have an empty list
            if (slist.isEmpty() && defaultStr != null) {
                slist.add(defaultStr);
            }
        } catch (Exception e) {
            return null;
        }
        return new Property(name, PropertyType.STRING, PropertyArity.LIST,
                slist);
    }

    /*
     * Take a PdfArray which is supposed to conform to the rectangle
     * description (i.e., it's an array of 4 numbers) and create
     * a Property which is an array of 4 integers.
     */
    protected Property makeRectProperty(PdfArray arrObj, String name) {
        int[] iarr = new int[4];
        double[] arr = arrObj.toRectangle();
        // toRectangle is written to return an array of double,
        // which is what the bounding box is in the most general
        // case; but the spec requires an array of integer, so
        // we convert it. This may seem like an excess of work,
        // but I'd rather have toRectangle do the right thing
        // rather than losing generality.
        for (int i = 0; i < 4; i++) {
            iarr[i] = (int) arr[i];
        }
        return new Property(name, PropertyType.INTEGER, PropertyArity.ARRAY,
                iarr);
    }

    private static boolean checkTypeKey(final PdfDictionary dict,
            final RepInfo info, final String expctVal,
            final JhoveMessage typeInvalMess,
            final JhoveMessage typeNotFoundMess,
            final JhoveMessage typeNotSimpleMess) {
        // Get the type key from the dictionary
        PdfObject typeObj = dict.get(DICT_KEY_TYPE);
        if (typeObj != null && typeObj instanceof PdfSimpleObject) {
            // If the type key is not null and is a simple object
            String typeValue = ((PdfSimpleObject) typeObj).getStringValue();
            if (!expctVal.equals(typeValue)) {
                // If the type key value is not of the expected value
                info.setWellFormed(false);
                info.setMessage(new ErrorMessage(typeInvalMess, 0));
                return false;
            }
        } else {
            // There's no type key or it's not a simple object
            // Choose message depending on whether the value is null or of
            // the wrong type
            JhoveMessage message = (typeObj == null) ? typeNotFoundMess
                    : typeNotSimpleMess;
            info.setMessage(new ErrorMessage(message, 0));
            info.setWellFormed(false);
            return false;
        }
        return true;
    }

    private static String imageMimeFromFilters(Filter[] filters) {
        // If there's no filters it's a PNG
        if (filters == null || filters.length == 0) {
            return "image/png";
        }
        // Iterate the filter list
        for (Filter filt : filters) {
            // Get the Filter name
            String filterName = filt.getFilterName();
            // And the MIME type from htat
            String mime = imageMimeFromFilterName(filterName);
            if (mime != null) {
                // If it's not null then return
                return mime;
            }
            // Next filter
        }
        // No MIME type match made for filter list
        return null;
    }

    // Stolen from an Apache PDF Box method:
    // https://github.com/apache/pdfbox/blob/2.0/pdfbox/src/main/java/org/apache/pdfbox/pdmodel/graphics/image/PDImageXObject.java#L767
    private static String imageMimeFromFilterName(final String filterName) {
        if (FILTER_NAME_DCT.equals(filterName)) {
            // DCTDecode is JPEG
            return "image/jpg";
        } else if (FILTER_NAME_JPX.equals(filterName)) {
            // JPX Decode for JPX (JP2K)
            return "image/jpx";
        } else if (FILTER_NAME_CCITT.equals(filterName)) {
            // CCITT is a TIFF image
            return "image/tiff";
        } else if (FILTER_NAME_FLATE.equals(filterName)
                || FILTER_NAME_LZW.equals(filterName)
                || FILTER_NAME_RUN_LENGTH.equals(filterName)) {
            // There's a bunch of PNG possibilities
            return "image/png";
        }
        // No match made
        return null;
    }

    private PdfObject getObjectFromStream(final int objIndex,
            final int recGuard) throws PdfMalformedException {
        /*
         * The object is located in an object stream. Need to get the
         * object stream first.
         * Be cautious dealing with _cachedStreamIndex and _cachedObjectStream;
         * these can be modified by a recursive call to getObject.
         */
        try {
            int objStreamIndex = _xref2[objIndex][0];
            PdfObject streamObj;
            ObjectStream ostrm = null;
            if (!_streamsEncrypted) {
                if (objStreamIndex == _cachedStreamIndex) {
                    ostrm = _cachedObjectStream;
                    // Reset it
                    if (ostrm.isValid()) {
                        ostrm.readIndex();
                    }
                } else {
                    streamObj = resolveIndirectObject(
                            getObject(objStreamIndex, recGuard - 1));
                    if (streamObj instanceof PdfStream) {
                        ostrm = new ObjectStream((PdfStream) streamObj, _raf);
                        if (ostrm.isValid()) {
                            ostrm.readIndex();
                            _cachedObjectStream = ostrm;
                            _cachedStreamIndex = objStreamIndex;
                        } else {
                            throw new PdfMalformedException(
                                    MessageConstants.PDF_HUL_108); // PDF-HUL-108
                        }
                    }
                }
                /* And finally extract the object from the object stream. */
                return ostrm.getObject(objIndex);
            } else {
                return null;
            }

        } catch (ZipException excep) {
            _logger.info(excep.getMessage());
            throw new PdfMalformedException(MessageConstants.PDF_HUL_109); // PDF-HUL-109
        } catch (Exception e) {
            _logger.info(e.getMessage());
            /* Fall through with error */
        }
        throw new PdfMalformedException(MessageConstants.PDF_HUL_110); // PDF-HUL-110
    }
}
