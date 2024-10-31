package edu.harvard.hul.ois.jhove.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import nl.jqno.equalsverifier.EqualsVerifier;

@RunWith(JUnit4.class)
public class JhoveMessagesTest {
	@BeforeClass
	public static void setUp() {
		// Ensure the default locale is English for testing on any platform
		Locale.setDefault(Locale.ENGLISH);
	}
	
    @Test
    public void testMessageInstance() {
        JhoveMessage msg = JhoveMessages.getMessageInstance("MSG");
        assertEquals(JhoveMessages.NO_ID, msg.getId());
        assertEquals("MSG", msg.getMessage());
        assertFalse(msg.hasSubMessage());
        ;
    }

    @Test
    public void testMessageInstanceNullMessage() {
        assertThrows("IllegalArgument exception expected",
                IllegalArgumentException.class,
                () -> {
                    JhoveMessages.getMessageInstance(null);
                });
    }

    @Test
    public void testMessageInstanceId() {
        JhoveMessage msg = JhoveMessages.getMessageInstance("ID", "MSG");
        assertEquals("ID", msg.getId());
        assertEquals("MSG", msg.getMessage());
        assertFalse(msg.hasSubMessage());
        ;
    }

    @Test
    public void testMessageInstanceEmptyId() {
        assertThrows("IllegalArgument exception expected",
                IllegalArgumentException.class,
                () -> {
                    JhoveMessages.getMessageInstance("", "MSG");
                });
    }

    @Test
    public void testMessageInstanceNullId() {
        final String id = null;
        assertThrows("IllegalArgument exception expected",
                IllegalArgumentException.class,
                () -> {
                    JhoveMessages.getMessageInstance(id, "MSG");
                });
    }

    @Test
    public void testMessageInstanceSubMessage() {
        JhoveMessage msg = JhoveMessages.getMessageInstance("ID", "MSG", "SUB");
        assertTrue(msg.hasSubMessage());
        assertEquals("SUB", msg.getSubMessage());
    }

    @Test
    public void testMessageInstanceNullSubMessage() {
        JhoveMessage msg = JhoveMessages.getMessageInstance("ID", "MSG", null);
        assertFalse(msg.hasSubMessage());
    }

    @Test
    public void testMessageFactoryInstanceNullName() {
        assertThrows("IllegalArgument exception expected",
                IllegalArgumentException.class,
                () -> {
                    JhoveMessages.getInstance(null);
                });
    }

    @Test
    public void testMessageFactoryInstanceNullLocale() {
        assertThrows("IllegalArgument exception expected",
                IllegalArgumentException.class,
                () -> {
                    JhoveMessages.getInstance("bundle", null);
                });
    }

    @Test
    public void testMessageFactoryInstanceEmptyName() {
        assertThrows("IllegalArgument exception expected",
                IllegalArgumentException.class,
                () -> {
                    JhoveMessages.getInstance("");
                });
    }

    @Test
    public void testMessageFactoryImplInstanceEmptyName() {
        assertThrows("IllegalArgument exception expected",
                IllegalArgumentException.class,
                () -> {
                    JhoveMessageFactImpl.getInstance(null);
                });
    }

    /**
     * Test the hash and equals contract for the class using EqualsVerifier.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(JhoveMessageImpl.class).verify();
        assertNotEquals(JhoveMessages.DEFAULT_MESSAGE, JhoveMessages.getMessageInstance("ID", "MSG"));
        assertEquals(JhoveMessages.DEFAULT_MESSAGE,
                JhoveMessages.getMessageInstance(JhoveMessages.NO_ID, JhoveMessages.EMPTY_MESSAGE));
    }

    @Test
    public void testDefault() {
        System.getProperties().remove("module.language");
        JhoveMessageFactory messageFactory = JhoveMessages
                .getInstance("edu.harvard.hul.ois.jhove.messages.ErrorMessages");
        JhoveMessage msg = messageFactory.getMessage("MSG");
        assertEquals("English", msg.getMessage());
    }

    @Test
    public void testEnglish() {
        System.setProperty("module.language", "en");
        JhoveMessageFactory messageFactory = JhoveMessages
                .getInstance("edu.harvard.hul.ois.jhove.messages.ErrorMessages");
        JhoveMessage msg = messageFactory.getMessage("MSG");
        assertEquals("English", msg.getMessage());
    }

    @Test
    public void testUnknown() {
        System.setProperty("module.language", "jp");
        JhoveMessageFactory messageFactory = JhoveMessages
                .getInstance("edu.harvard.hul.ois.jhove.messages.ErrorMessages");
        JhoveMessage msg = messageFactory.getMessage("MSG");
        assertEquals("English", msg.getMessage());
    }

    @Test
    public void testCustomized() {
        {
            System.setProperty("module.language", "da");
            JhoveMessageFactory messageFactory = JhoveMessages
                    .getInstance("edu.harvard.hul.ois.jhove.messages.ErrorMessages");
            JhoveMessage msg = messageFactory.getMessage("MSG");
            assertEquals("Danish", msg.getMessage());
        }

        {
            System.setProperty("module.language", "fr");
            JhoveMessageFactory messageFactory = JhoveMessages
                    .getInstance("edu.harvard.hul.ois.jhove.messages.ErrorMessages");
            JhoveMessage msg = messageFactory.getMessage("MSG");
            assertEquals("French", msg.getMessage());
        }

        {
            System.setProperty("module.language", "");
            JhoveMessageFactory messageFactory = JhoveMessages
                    .getInstance("edu.harvard.hul.ois.jhove.messages.ErrorMessages");
            JhoveMessage msg = messageFactory.getMessage("MSG");
            assertEquals("English", msg.getMessage());
        }
    }
}
