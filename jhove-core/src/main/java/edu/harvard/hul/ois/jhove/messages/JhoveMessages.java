package edu.harvard.hul.ois.jhove.messages;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utility class that handles creation of Message type instances, etc.
 * 
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 */

public enum JhoveMessages {
	INSTANCE;

	public static final String NO_ID = "NO-ID";
	public static final String EMPTY_MESSAGE = "";
	public static final JhoveMessage DEFAULT_MESSAGE = getMessageInstance(NO_ID,
			EMPTY_MESSAGE);

	/**
	 * Create a new message instance with a DEFAULT_ID
	 * 
	 * @param message
	 *            the message of the new message
	 * @return the new message instance
	 * @throws IllegalArgumentException
	 *             if the id or message is null or empty
	 */
	public static JhoveMessage getMessageInstance(final String message)
			throws IllegalArgumentException {
		return getMessageInstance(NO_ID, message);
	}

	/**
	 * Create a JhoveMessage instance with the give id and message value
	 * 
	 * @param id
	 *            the id of the new message
	 * @param message
	 *            the message of the new message
	 * @return the new message instance
	 * @throws IllegalArgumentException
	 *             if the id or message is null or empty
	 */
	public static JhoveMessage getMessageInstance(final String id,
			final String message) throws IllegalArgumentException {
		return getMessageInstance(id, message, EMPTY_MESSAGE);
	}

    public static JhoveMessage getMessageInstance(final JhoveMessage message, final String subMessage) {
        return getMessageInstance(message.getId(), message.getMessage(), subMessage);
    }

    /**
	 * Create a new JhoveMessage instance with the given id, message and
	 * sub-message
	 * 
	 * @param id
	 *            the id of the new message
	 * @param message
	 *            the message of the new message
	 * @param subMessage
	 *            the sub-message of the new message
	 * @return the new message instance
	 * @throws IllegalArgumentException
	 *             if the id or message is null or empty
	 */
	public static JhoveMessage getMessageInstance(final String id,
			final String message, final String subMessage)
			throws IllegalArgumentException {
		if (id == null || id.isEmpty())
			throw new IllegalArgumentException(
					"id cannot be null or an empty string.");
		if (message == null)
			throw new IllegalArgumentException(
					"message cannot be null.");
		return JhoveMessageImpl.getInstance(id, message, subMessage);
	}

	/**
	 * Get a JhoveMessageFactory instance with the requested property based
	 * bundle name and the default user locale
	 * 
	 * @param bundleName
	 *            the fully qualified resource path for the message bundle
	 *            property file
	 * @return a new JhoveMessageFactory instance backed by the bundle property
	 *         file.
	 * @throws IllegalArgumentException
	 *             if the bundle name is empty or the message bundle can't be
	 *             located
	 */
	public static JhoveMessageFactory getInstance(final String bundleName)
			throws IllegalArgumentException {
		String moduleLanguage = System.getProperty("module.language");
		Locale local = moduleLanguage == null || moduleLanguage.trim().length() == 0 ? Locale.getDefault() : Locale.forLanguageTag(moduleLanguage);

		return getInstance(bundleName, local);
	}

	/**
	 * Get a JhoveMessageFactory instance with the requested property based
	 * bundle name and a specific locale
	 * 
	 * @param bundleName
	 *            the fully qualified resource path for the message bundle
	 *            property file
	 * @param locale
	 *            the locale for the message bundle
	 * @return a new JhoveMessageFactory instance backed by the bundle property
	 *         file.
	 * @throws IllegalArgumentException
	 *             if the bundle name is empty or the message bundle can't be
	 *             located
	 */
	public static JhoveMessageFactory getInstance(final String bundleName,
			final Locale locale) throws IllegalArgumentException {
		if (bundleName == null || bundleName.isEmpty())
			throw new IllegalArgumentException(
					"bundleName cannot be null or empty");
		if (locale == null)
			throw new IllegalArgumentException("locale cannot be null.");
		ResourceBundle messageBundle = ResourceBundle.getBundle(bundleName,
				locale);
		return JhoveMessageFactImpl.getInstance(messageBundle);
	}
}
