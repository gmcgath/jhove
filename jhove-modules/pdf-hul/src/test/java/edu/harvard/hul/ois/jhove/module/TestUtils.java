package edu.harvard.hul.ois.jhove.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;

import edu.harvard.hul.ois.jhove.Message;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.RepInfo;

/**
 * Convenience methods to test that the result of JHOVE validation are as
 * expected. The tests are:
 * <ul>
 * <li>The well formed result is equal to a pre-defined value.</li>
 * <li>The is valid result is equal to a pre-defined value.</li>
 * <li>The message list contains an expected message (optionally).</li>
 * </ul>
 *
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1 Created 14 Mar 2018:20:14:19
 */

public final class TestUtils {
	public static final String MODULE_RESOURCE_BASE = "/edu/harvard/hul/ois/jhove/module/";
	public static final String EMPTY_FILE_PATH = MODULE_RESOURCE_BASE + "empty";

	private TestUtils() {
		// Keep out
		throw new AssertionError("Should never be in constructor.");
	}

	/**
	 * Convenience method that takes the path to a test resource and tests that
	 * the results of JHOVE are as expected.
	 *
	 * @param module
	 *            a {@link edu.harvard.hul.ois.jhove.Module} instance
	 *            to use to validate the resource.
	 * @param resToTest
	 *            the String path of the resource to validate and test.
	 * @param expctWllFrmd
	 *            the expected well formed value
	 * @param expctVld
	 *            the expected is valid value
	 * @throws URISyntaxException
	 *             when there's an issue converting the resource name to a path
	 */
	public static RepInfo testValidateResource(final Module module,
			final String resToTest, final int expctWllFrmd, final int expctVld) throws URISyntaxException {
		return testValidateResource(module, resToTest, expctWllFrmd, expctVld, null);
	}

	/**
	 * 
	 * @param module
	 *            a {@link edu.harvard.hul.ois.jhove.Module} instance
	 *            to use to validate the resource.
	 * @param resToTest
	 *            the String path of the resource to validate and test.
	 * @param expctWllFrmd
	 *            the expected well formed value
	 * @param expctVld
	 *            the expected is valid value
	 * @param messageId
	 *            a JHOVE validation string message expected to be found in the
	 *            list of validation messages. If this parameter is null the
	 *            test isn't performed.
	 * @throws URISyntaxException
	 *             when there's an issue converting the resource name to a path
	 */
	public static RepInfo testValidateResource(final Module module,
			final String resToTest, final int expctWllFrmd, final int expctVld,
			final String messageId) throws URISyntaxException {
		return testValidateResource(module, resToTest, expctWllFrmd, expctVld, messageId, true);
	}

	/**
	 * 
	 * @param module
	 *            a {@link edu.harvard.hul.ois.jhove.Module} instance
	 *            to use to validate the resource.
	 * @param resToTest
	 *            the String path of the resource to validate and test.
	 * @param expctWllFrmd
	 *            the expected well formed value
	 * @param expctVld
	 *            the expected is valid value
	 * @param message
	 *            a JHOVE validation string message which MUST be found in the
	 *            list of validation messages if messMustBePresent is true.
	 *            When messMustBePresent is false the message MUST NOT be
	 *            found in the list of validation messages. 
	 *            If this parameter is null the test isn't performed.
	 * @param messMustBePresent
	 *            if message is not null this param dictates whether the
	 *            test expects the validation message to be in the report or not.
	 * @throws URISyntaxException
	 *             when there's an issue converting the resource name to a path
	 */
	public static RepInfo testValidateResource(final Module module,
			final String resToTest, final int expctWllFrmd, final int expctVld,
			final String expctMessageId, boolean messMustBePresent ) throws URISyntaxException {
		File toTest = new File(TestUtils.class.getResource(resToTest).toURI());

		return testValidateFile(module, toTest, expctWllFrmd, expctVld,
				expctMessageId, messMustBePresent);
	}

	/**
	 * Method that takes a file and tests that the results of JHOVE are as
	 * expected.
	 *
	 * @param pdfModule
	 *            a {@link edu.harvard.hul.ois.jhove.module.PdfModule} instance
	 *            to use to validate the resource.
	 * @param fileToTest
	 *            a Java File instance to validate
	 * @param expctWllFrmd
	 *            the expected well formed value
	 * @param expctVld
	 *            the expected is valid value
	 * @param expctMessage
	 *            a JHOVE validation string message expected to be found in the
	 *            list of validation messages. If this parameter is null the
	 *            test isn't performed.
	 */
	public static RepInfo testValidateFile(final Module module,
			final File fileToTest, final int expctWllFrmd, final int expctVld) {
		return testValidateFile(module, fileToTest, expctWllFrmd, expctVld, null);
	}

	public static RepInfo testValidateFile(final Module module,
			final File fileToTest, final int expctWllFrmd, final int expctVld,
			final String messageId) {
		return testValidateFile(module, fileToTest, expctWllFrmd, expctVld, messageId, true);
	}

	public static RepInfo testValidateFile(final Module module,
			final File fileToTest, final int expctWllFrmd, final int expctVld,
			final String messageId, boolean messMustBePresent) {
		RepInfo info = parseTestFile(module, fileToTest);
		testResult(info, expctWllFrmd, expctVld, messageId, messMustBePresent);
		return info;
	}

	private static void testResult(final RepInfo info, final int expctWllFrmd,
			final int expctVld, final String messageId, boolean messMustBePresent) {
		testWellFormed(info, expctWllFrmd);
		testIsValid(info, expctVld);
		if (messageId == null) {
			return;
		}
		Message jhoveMessage = getMessageIfPresent(info, messageId);
		if (messMustBePresent) {
			if (jhoveMessage == null) {
				System.out.println(String.format(
						"Expected message ID: %s, not found.", messageId));
				outputMessages(info);
			}
			assertTrue("Expected message ID: " + messageId, jhoveMessage != null);
		} else {
			if (jhoveMessage != null) {
				System.out.println(String.format(
						"Unexpected message ID: %s, found.", jhoveMessage.getMessage()));
				outputMessages(info);
			}
			assertTrue("Unexpected message ID: " + messageId, jhoveMessage == null);
		}
	}

	private static void outputMessages(final RepInfo info) {
		System.out.println("Messages in Report Info:");
		for (Message mess : info.getMessage()) {
			System.out.println(String.format(" - %s", mess.getMessage()));
		}
	}

	private static RepInfo parseTestFile(final Module module,
			final File toTest) {
		if (module.isRandomAccess()) {
			return rafModuleTest(module, toTest);
		}
		return streamModuleTest(module, toTest);
	}


	private static RepInfo streamModuleTest(final Module fisModule, final File toTest) {
		RepInfo info = new RepInfo(toTest.getName());
		try (InputStream fis = new FileInputStream(toTest)) {
			int index = fisModule.parse(fis, info, 0);
			while (index > 0) {
				index = fisModule.parse(fis, info, 0);
			}
		} catch (FileNotFoundException excep) {
			excep.printStackTrace();
			fail("Couldn't find file to test: " + toTest.getName());
		} catch (IOException excep) {
			excep.printStackTrace();
			fail("IOException Reading: " + toTest.getName());
		}
		return info;
	}

	private static RepInfo rafModuleTest(final Module rafModule, final File toTest) {
		RepInfo info = new RepInfo(toTest.getName());
		try (RandomAccessFile raf = new RandomAccessFile(toTest, "r")) {
			rafModule.parse(raf, info);
		} catch (FileNotFoundException excep) {
			excep.printStackTrace();
			fail("Couldn't find file to test: " + toTest.getName());
		} catch (IOException excep) {
			excep.printStackTrace();
			fail("IOException Reading: " + toTest.getName());
		}
		return info;
	}

	private static void testWellFormed(final RepInfo info, final int expctWllFrmd) {
		String message = (expctWllFrmd == RepInfo.TRUE)
				? "Should be well formed."
				: "Should NOT be well formed.";
		assertEquals(message, expctWllFrmd, info.getWellFormed());
	}

	private static void testIsValid(final RepInfo info, final int expctVld) {
		String message = (expctVld == RepInfo.TRUE) ? "Should be valid."
				: "Should NOT be valid.";
		assertEquals(message, expctVld, info.getValid());
	}
	
	private static Message getMessageIfPresent(final RepInfo info, final String expectedId) {
		for (Message message : info.getMessage()) {
			if (message.getId().equals(expectedId)) {
				return message;
			}
		}
		return null;
	}
}
