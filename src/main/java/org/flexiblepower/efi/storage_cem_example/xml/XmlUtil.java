package org.flexiblepower.efi.storage_cem_example.xml;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Utility class to help with converting dates and durations from and to XML
 * types.
 */
public class XmlUtil {

	private static DatatypeFactory datatypeFactory;

	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (final DatatypeConfigurationException e) {
			System.err.println("Could not get datatypeFactory");
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Create an XML Duration based on a number of milliseconds.
	 *
	 * @param durationMs
	 *            The number of milliseconds
	 * @return and XML Duration
	 */
	public static javax.xml.datatype.Duration duration(long durationMs) {
		return datatypeFactory.newDuration(durationMs);
	}

	/**
	 * Get duration in milliseconds from an XML Duration. The start of the duration
	 * period should also be provided.
	 *
	 * @param duration
	 *            The XML Duration object
	 * @param start
	 *            The start of the Duration period
	 * @return The amount of milliseconds in the Duration
	 */
	public static long duration(javax.xml.datatype.Duration duration, Date start) {
		return duration.getTimeInMillis(start);
	}

	public static XMLGregorianCalendar date(Date date) {
		final GregorianCalendar c = new GregorianCalendar();
		c.setTime(date);
		return datatypeFactory.newXMLGregorianCalendar(c);
	}

	public static Date date(XMLGregorianCalendar xmlCalendar) {
		return xmlCalendar.toGregorianCalendar().getTime();
	}

	public static LocalDateTime localDateTimeFrom(XMLGregorianCalendar xmlCalendar) {
		return LocalDateTime.ofInstant(xmlCalendar.toGregorianCalendar().toInstant(), ZoneOffset.UTC);
	}

	/**
	 * Get duration in milliseconds
	 *
	 * @param duration
	 * @param timestamp
	 * @return
	 */
	public static long duration(javax.xml.datatype.Duration duration, XMLGregorianCalendar timestamp) {
		return duration(duration, date(timestamp));
	}

}
