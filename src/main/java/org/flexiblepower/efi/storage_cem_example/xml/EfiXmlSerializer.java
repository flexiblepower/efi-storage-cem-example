package org.flexiblepower.efi.storage_cem_example.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.flexiblepower.efi.xml.EfiMessage;
import org.flexiblepower.efi.xml.ObjectFactory;

public class EfiXmlSerializer {

	public static String serialize(final EfiMessage baseMessage) throws JAXBException {
		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		final JAXBContext jaxbContext = JAXBContext.newInstance(baseMessage.getClass());
		final Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.marshal(baseMessage, byteStream);

		return byteStream.toString();
	}

	public static EfiMessage deserialize(final String xml) throws JAXBException {
		String trimmedXml = xml.trim().replaceFirst("^([\\W]+)<", "<");

		final ByteArrayInputStream byteStream = new ByteArrayInputStream(trimmedXml.getBytes(Charset.forName("UTF-8")));

		final ClassLoader loader = ObjectFactory.class.getClassLoader();
		final JAXBContext jaxbContext = JAXBContext.newInstance(
				"org.flexiblepower.efi.xml", loader);
		final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

		return (EfiMessage) unmarshaller.unmarshal(byteStream);
	}

}
