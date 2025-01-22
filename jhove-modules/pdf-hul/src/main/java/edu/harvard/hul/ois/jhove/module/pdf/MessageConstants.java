package edu.harvard.hul.ois.jhove.module.pdf;

import edu.harvard.hul.ois.jhove.messages.JhoveMessage;
import edu.harvard.hul.ois.jhove.messages.JhoveMessageFactory;
import edu.harvard.hul.ois.jhove.messages.JhoveMessages;

/**
 * Enum used to externalise the PDF modules message Strings. Using an enum
 * INSTANCE as a "trick" to ensure a single instance of the class. String
 * constants should be prefixed according to their use in the module:
 * <ul>
 * <li>LOG_ for messages used exclusively by the logger.</li>
 * <li>INF_ for informational messages.</li>
 * <li>ERR_ for error messages that indicate a file is invalid or not well
 * formed.</li>
 * </ul>
 * When adding new messages try to adopt the following order for the naming
 * elements:
 * <ol>
 * <li>PREFIX: one of the three prefixes from the list above.</li>
 * <li>ENTITY_NAME: the name of the PDF entity causing the prohlem, e.g. FONT,
 * or DOC.</li>
 * <li>Problem: a short indicator of the problem type, e.g. MISSING, ILLEGAL,
 * etc.</li>
 * </ol>
 * The elements should be separated by underscores. The messages currently don't
 * follow a consistent vocabulary, that is terms such as invalid, illegal, or
 * malformed are used without definition.
 *
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1 Created 1 Oct 2016:11:38:18
 */

public enum MessageConstants {
    INSTANCE;

    public static final JhoveMessageFactory messageFactory = JhoveMessages
            .getInstance("edu.harvard.hul.ois.jhove.module.pdf.ErrorMessages");

    public static final JhoveMessage PDF_HUL_1 = messageFactory.getMessage("PDF-HUL-1");
    public static final JhoveMessage PDF_HUL_2 = messageFactory.getMessage("PDF-HUL-2");
    public static final JhoveMessage PDF_HUL_3 = messageFactory.getMessage("PDF-HUL-3");
    public static final JhoveMessage PDF_HUL_4 = messageFactory.getMessage("PDF-HUL-4");
    public static final JhoveMessage PDF_HUL_5 = messageFactory.getMessage("PDF-HUL-5");
    public static final JhoveMessage PDF_HUL_6 = messageFactory.getMessage("PDF-HUL-6");
    public static final JhoveMessage PDF_HUL_7 = messageFactory.getMessage("PDF-HUL-7");
    public static final JhoveMessage PDF_HUL_8 = messageFactory.getMessage("PDF-HUL-8");
    public static final JhoveMessage PDF_HUL_9 = messageFactory.getMessage("PDF-HUL-9");
    public static final JhoveMessage PDF_HUL_10 = messageFactory.getMessage("PDF-HUL-10");
    public static final JhoveMessage PDF_HUL_11 = messageFactory.getMessage("PDF-HUL-11");
    public static final JhoveMessage PDF_HUL_12 = messageFactory.getMessage("PDF-HUL-12");
    public static final JhoveMessage PDF_HUL_13 = messageFactory.getMessage("PDF-HUL-13");
    public static final JhoveMessage PDF_HUL_14 = messageFactory.getMessage("PDF-HUL-14");
    public static final JhoveMessage PDF_HUL_15 = messageFactory.getMessage("PDF-HUL-15");
    public static final JhoveMessage PDF_HUL_16 = messageFactory.getMessage("PDF-HUL-16");
    public static final JhoveMessage PDF_HUL_17 = messageFactory.getMessage("PDF-HUL-17");
    public static final JhoveMessage PDF_HUL_18 = messageFactory.getMessage("PDF-HUL-18");
    public static final JhoveMessage PDF_HUL_19 = messageFactory.getMessage("PDF-HUL-19");
    public static final JhoveMessage PDF_HUL_20 = messageFactory.getMessage("PDF-HUL-20");
    public static final JhoveMessage PDF_HUL_21 = messageFactory.getMessage("PDF-HUL-21");
    public static final JhoveMessage PDF_HUL_22 = messageFactory.getMessage("PDF-HUL-22");
    public static final JhoveMessage PDF_HUL_23 = messageFactory.getMessage("PDF-HUL-23");
    public static final JhoveMessage PDF_HUL_24 = messageFactory.getMessage("PDF-HUL-24");
    public static final JhoveMessage PDF_HUL_25 = messageFactory.getMessage("PDF-HUL-25");
    public static final JhoveMessage PDF_HUL_26 = messageFactory.getMessage("PDF-HUL-26");
    public static final JhoveMessage PDF_HUL_27 = messageFactory.getMessage("PDF-HUL-27");
    public static final JhoveMessage PDF_HUL_28 = messageFactory.getMessage("PDF-HUL-28");
    public static final JhoveMessage PDF_HUL_29 = messageFactory.getMessage("PDF-HUL-29");
    public static final JhoveMessage PDF_HUL_30 = messageFactory.getMessage("PDF-HUL-30");
    public static final JhoveMessage PDF_HUL_31 = messageFactory.getMessage("PDF-HUL-31");
    public static final JhoveMessage PDF_HUL_32 = messageFactory.getMessage("PDF-HUL-32");
    public static final JhoveMessage PDF_HUL_33 = messageFactory.getMessage("PDF-HUL-33");
    public static final JhoveMessage PDF_HUL_34 = messageFactory.getMessage("PDF-HUL-34");
    public static final JhoveMessage PDF_HUL_35 = messageFactory.getMessage("PDF-HUL-35");
    public static final JhoveMessage PDF_HUL_36 = messageFactory.getMessage("PDF-HUL-36");
    public static final JhoveMessage PDF_HUL_37 = messageFactory.getMessage("PDF-HUL-37");
    public static final JhoveMessage PDF_HUL_38 = messageFactory.getMessage("PDF-HUL-38");
    public static final JhoveMessage PDF_HUL_39 = messageFactory.getMessage("PDF-HUL-39");
    public static final JhoveMessage PDF_HUL_40 = messageFactory.getMessage("PDF-HUL-40");
    public static final JhoveMessage PDF_HUL_41 = messageFactory.getMessage("PDF-HUL-41");
    public static final JhoveMessage PDF_HUL_42 = messageFactory.getMessage("PDF-HUL-42");
    public static final JhoveMessage PDF_HUL_43 = messageFactory.getMessage("PDF-HUL-43");
    public static final JhoveMessage PDF_HUL_44 = messageFactory.getMessage("PDF-HUL-44");
    public static final JhoveMessage PDF_HUL_45 = messageFactory.getMessage("PDF-HUL-45");
    public static final JhoveMessage PDF_HUL_46 = messageFactory.getMessage("PDF-HUL-46");
    public static final JhoveMessage PDF_HUL_47 = messageFactory.getMessage("PDF-HUL-47");
    public static final JhoveMessage PDF_HUL_48 = messageFactory.getMessage("PDF-HUL-48");
    public static final JhoveMessage PDF_HUL_49 = messageFactory.getMessage("PDF-HUL-49");
    public static final JhoveMessage PDF_HUL_50 = messageFactory.getMessage("PDF-HUL-50");
    public static final JhoveMessage PDF_HUL_51 = messageFactory.getMessage("PDF-HUL-51");
    public static final JhoveMessage PDF_HUL_52 = messageFactory.getMessage("PDF-HUL-52");
    public static final JhoveMessage PDF_HUL_53 = messageFactory.getMessage("PDF-HUL-53");
    public static final JhoveMessage PDF_HUL_54 = messageFactory.getMessage("PDF-HUL-54");
    public static final JhoveMessage PDF_HUL_55 = messageFactory.getMessage("PDF-HUL-55");
    public static final JhoveMessage PDF_HUL_56 = messageFactory.getMessage("PDF-HUL-56");
    public static final JhoveMessage PDF_HUL_57 = messageFactory.getMessage("PDF-HUL-57");
    public static final JhoveMessage PDF_HUL_58 = messageFactory.getMessage("PDF-HUL-58");
    public static final JhoveMessage PDF_HUL_59 = messageFactory.getMessage("PDF-HUL-59");
    public static final JhoveMessage PDF_HUL_60 = messageFactory.getMessage("PDF-HUL-60");
    public static final JhoveMessage PDF_HUL_61 = messageFactory.getMessage("PDF-HUL-61");
    public static final JhoveMessage PDF_HUL_62 = messageFactory.getMessage("PDF-HUL-62");
    public static final JhoveMessage PDF_HUL_63 = messageFactory.getMessage("PDF-HUL-63");
    public static final JhoveMessage PDF_HUL_64 = messageFactory.getMessage("PDF-HUL-64");
    public static final JhoveMessage PDF_HUL_65 = messageFactory.getMessage("PDF-HUL-65");
    public static final JhoveMessage PDF_HUL_66 = messageFactory.getMessage("PDF-HUL-66");
    public static final JhoveMessage PDF_HUL_67 = messageFactory.getMessage("PDF-HUL-67");
    public static final JhoveMessage PDF_HUL_68 = messageFactory.getMessage("PDF-HUL-68");
    public static final JhoveMessage PDF_HUL_69 = messageFactory.getMessage("PDF-HUL-69");
    public static final JhoveMessage PDF_HUL_70 = messageFactory.getMessage("PDF-HUL-70");
    public static final JhoveMessage PDF_HUL_71 = messageFactory.getMessage("PDF-HUL-71");
    public static final JhoveMessage PDF_HUL_72 = messageFactory.getMessage("PDF-HUL-72");
    public static final JhoveMessage PDF_HUL_73 = messageFactory.getMessage("PDF-HUL-73");
    public static final JhoveMessage PDF_HUL_74 = messageFactory.getMessage("PDF-HUL-74");
    public static final JhoveMessage PDF_HUL_75 = messageFactory.getMessage("PDF-HUL-75");
    public static final JhoveMessage PDF_HUL_76 = messageFactory.getMessage("PDF-HUL-76");
    public static final JhoveMessage PDF_HUL_77 = messageFactory.getMessage("PDF-HUL-77");
    public static final JhoveMessage PDF_HUL_78 = messageFactory.getMessage("PDF-HUL-78");
    public static final JhoveMessage PDF_HUL_79 = messageFactory.getMessage("PDF-HUL-79");
    public static final JhoveMessage PDF_HUL_80 = messageFactory.getMessage("PDF-HUL-80");
    public static final JhoveMessage PDF_HUL_81 = messageFactory.getMessage("PDF-HUL-81");
    public static final JhoveMessage PDF_HUL_82 = messageFactory.getMessage("PDF-HUL-82");
    public static final JhoveMessage PDF_HUL_83 = messageFactory.getMessage("PDF-HUL-83");
    public static final JhoveMessage PDF_HUL_84 = messageFactory.getMessage("PDF-HUL-84");
    public static final JhoveMessage PDF_HUL_85 = messageFactory.getMessage("PDF-HUL-85");
    public static final JhoveMessage PDF_HUL_86 = messageFactory.getMessage("PDF-HUL-86");
    public static final JhoveMessage PDF_HUL_87 = messageFactory.getMessage("PDF-HUL-87");
    public static final JhoveMessage PDF_HUL_88 = messageFactory.getMessage("PDF-HUL-88");
    public static final JhoveMessage PDF_HUL_89 = messageFactory.getMessage("PDF-HUL-89");
    public static final JhoveMessage PDF_HUL_90 = messageFactory.getMessage("PDF-HUL-90");
    public static final JhoveMessage PDF_HUL_91 = messageFactory.getMessage("PDF-HUL-91");
    public static final JhoveMessage PDF_HUL_92 = messageFactory.getMessage("PDF-HUL-92");
    public static final JhoveMessage PDF_HUL_93 = messageFactory.getMessage("PDF-HUL-93");
    public static final JhoveMessage PDF_HUL_94 = messageFactory.getMessage("PDF-HUL-94");
    public static final JhoveMessage PDF_HUL_95 = messageFactory.getMessage("PDF-HUL-95");
    public static final JhoveMessage PDF_HUL_96 = messageFactory.getMessage("PDF-HUL-96");
    public static final JhoveMessage PDF_HUL_97 = messageFactory.getMessage("PDF-HUL-97");
    public static final JhoveMessage PDF_HUL_98 = messageFactory.getMessage("PDF-HUL-98");
    public static final JhoveMessage PDF_HUL_99 = messageFactory.getMessage("PDF-HUL-99");
    public static final JhoveMessage PDF_HUL_100 = messageFactory.getMessage("PDF-HUL-100");
    public static final JhoveMessage PDF_HUL_101 = messageFactory.getMessage("PDF-HUL-101");
    public static final JhoveMessage PDF_HUL_102 = messageFactory.getMessage("PDF-HUL-102");
    public static final JhoveMessage PDF_HUL_103 = messageFactory.getMessage("PDF-HUL-103");
    public static final JhoveMessage PDF_HUL_104 = messageFactory.getMessage("PDF-HUL-104");
    public static final JhoveMessage PDF_HUL_105 = messageFactory.getMessage("PDF-HUL-105");
    public static final JhoveMessage PDF_HUL_106 = messageFactory.getMessage("PDF-HUL-106");
    public static final JhoveMessage PDF_HUL_107 = messageFactory.getMessage("PDF-HUL-107");
    public static final JhoveMessage PDF_HUL_108 = messageFactory.getMessage("PDF-HUL-108");
    public static final JhoveMessage PDF_HUL_109 = messageFactory.getMessage("PDF-HUL-109");
    public static final JhoveMessage PDF_HUL_110 = messageFactory.getMessage("PDF-HUL-110");
    public static final JhoveMessage PDF_HUL_111 = messageFactory.getMessage("PDF-HUL-111");
    public static final JhoveMessage PDF_HUL_112 = messageFactory.getMessage("PDF-HUL-112");
    public static final JhoveMessage PDF_HUL_113 = messageFactory.getMessage("PDF-HUL-113");
    public static final JhoveMessage PDF_HUL_114 = messageFactory.getMessage("PDF-HUL-114");
    public static final JhoveMessage PDF_HUL_115 = messageFactory.getMessage("PDF-HUL-115");
    public static final JhoveMessage PDF_HUL_116 = messageFactory.getMessage("PDF-HUL-116");
    public static final JhoveMessage PDF_HUL_117 = messageFactory.getMessage("PDF-HUL-117");
    public static final JhoveMessage PDF_HUL_118 = messageFactory.getMessage("PDF-HUL-118");
    public static final JhoveMessage PDF_HUL_119 = messageFactory.getMessage("PDF-HUL-119");
    public static final JhoveMessage PDF_HUL_120 = messageFactory.getMessage("PDF-HUL-120");
    public static final JhoveMessage PDF_HUL_121 = messageFactory.getMessage("PDF-HUL-121");
    public static final JhoveMessage PDF_HUL_122 = messageFactory.getMessage("PDF-HUL-122");
    public static final JhoveMessage PDF_HUL_123 = messageFactory.getMessage("PDF-HUL-123");
    public static final JhoveMessage PDF_HUL_124 = messageFactory.getMessage("PDF-HUL-124");
    public static final JhoveMessage PDF_HUL_125 = messageFactory.getMessage("PDF-HUL-125");
    public static final JhoveMessage PDF_HUL_126 = messageFactory.getMessage("PDF-HUL-126");
    public static final JhoveMessage PDF_HUL_127 = messageFactory.getMessage("PDF-HUL-127");
    public static final JhoveMessage PDF_HUL_128 = messageFactory.getMessage("PDF-HUL-128");
    public static final JhoveMessage PDF_HUL_129 = messageFactory.getMessage("PDF-HUL-129");
    public static final JhoveMessage PDF_HUL_130 = messageFactory.getMessage("PDF-HUL-130");
    public static final JhoveMessage PDF_HUL_131 = messageFactory.getMessage("PDF-HUL-131");
    public static final JhoveMessage PDF_HUL_132 = messageFactory.getMessage("PDF-HUL-132");
    public static final JhoveMessage PDF_HUL_133 = messageFactory.getMessage("PDF-HUL-133");
    public static final JhoveMessage PDF_HUL_134 = messageFactory.getMessage("PDF-HUL-134");
    public static final JhoveMessage PDF_HUL_135 = messageFactory.getMessage("PDF-HUL-135");
    public static final JhoveMessage PDF_HUL_136 = messageFactory.getMessage("PDF-HUL-136");
    public static final JhoveMessage PDF_HUL_136_SUB = messageFactory.getMessage("PDF-HUL-136-SUB");
    public static final JhoveMessage PDF_HUL_137 = messageFactory.getMessage("PDF-HUL-137");
    public static final JhoveMessage PDF_HUL_138 = messageFactory.getMessage("PDF-HUL-138");
    public static final JhoveMessage PDF_HUL_139 = messageFactory.getMessage("PDF-HUL-139");
    public static final JhoveMessage PDF_HUL_140 = messageFactory.getMessage("PDF-HUL-140");
    public static final JhoveMessage PDF_HUL_141 = messageFactory.getMessage("PDF-HUL-141");
    public static final JhoveMessage PDF_HUL_142 = messageFactory.getMessage("PDF-HUL-142");
    public static final JhoveMessage PDF_HUL_143 = messageFactory.getMessage("PDF-HUL-143");
    public static final JhoveMessage PDF_HUL_144 = messageFactory.getMessage("PDF-HUL-144");
    public static final JhoveMessage PDF_HUL_145 = messageFactory.getMessage("PDF-HUL-145");
    public static final JhoveMessage PDF_HUL_146 = messageFactory.getMessage("PDF-HUL-146");
    public static final JhoveMessage PDF_HUL_147 = messageFactory.getMessage("PDF-HUL-147");
    public static final JhoveMessage PDF_HUL_148 = messageFactory.getMessage("PDF-HUL-148");
    public static final JhoveMessage PDF_HUL_149 = messageFactory.getMessage("PDF-HUL-149");
    public static final JhoveMessage PDF_HUL_150 = messageFactory.getMessage("PDF-HUL-150");
    public static final JhoveMessage PDF_HUL_151 = messageFactory.getMessage("PDF-HUL-151");
    public static final JhoveMessage PDF_HUL_152 = messageFactory.getMessage("PDF-HUL-152");
    public static final JhoveMessage PDF_HUL_153 = messageFactory.getMessage("PDF-HUL-153");
    public static final JhoveMessage PDF_HUL_154 = messageFactory.getMessage("PDF-HUL-154");
    public static final JhoveMessage PDF_HUL_155 = messageFactory.getMessage("PDF-HUL-155");
    public static final JhoveMessage PDF_HUL_156 = messageFactory.getMessage("PDF-HUL-156");
    public static final JhoveMessage PDF_HUL_157 = messageFactory.getMessage("PDF-HUL-157");
    public static final JhoveMessage PDF_HUL_158 = messageFactory.getMessage("PDF-HUL-158");
    public static final JhoveMessage PDF_HUL_159 = messageFactory.getMessage("PDF-HUL-159");
    public static final JhoveMessage PDF_HUL_160 = messageFactory.getMessage("PDF-HUL-160");
    public static final JhoveMessage PDF_HUL_161 = messageFactory.getMessage("PDF-HUL-161");
    public static final JhoveMessage PDF_HUL_162 = messageFactory.getMessage("PDF-HUL-162");

    /**
     * Logger Messages
     */
    public static final String LOG_HINT_ARRY_CHK = "Checking hint array";
    public static final String LOG_IMAGE_XOBJ = "Image XObject";
    public static final String LOG_IMAGE_GET = "Getting image";
    public static final String LOG_K_ELEM_IS_ARRY = "Type K element is an array";
    public static final String LOG_K_ELEM_IS_DICT = "Type K element is dictionary";
    public static final String LOG_LIN_PROF_CHK = "Checking Linearized Profile";
    public static final String LOG_NAMES_DICT_EXCEP = "Exception on names dictionary: ";
    public static final String LOG_NO_CHILD_STRUCT_ELEM = "No children are structure elements";
    public static final String LOG_NO_CHILD_OBJS = "No child objects, exiting";
    public static final String LOG_REVISION_NUM_RETRIEVAL_EXCEP = "Exception getting revision number: ";
    public static final String LOG_SUBTREE_BUILDING = "Building subtree";
    public static final String LOG_XREF_TABLE_VERIFYING = "Verifying cross-reference table";
}
