package org.flexiblepower.efi.storage_cem_example.websocket;

import org.flexiblepower.efi.xml.EfiMessage;

/**
 * This interface is used to send an EFI message to the ResourceMananger.
 */
public interface EfiMessageSender {

	/**
	 * Send an EFI message to the ResourceManager.
	 *
	 * @param efiMessage
	 *            The message to send
	 */
	void sendEfiMessage(EfiMessage efiMessage);

}