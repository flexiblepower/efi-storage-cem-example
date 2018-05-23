package org.flexiblepower.efi.storage_cem_example.websocket;

import java.net.URI;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.xml.bind.JAXBException;

import org.flexiblepower.efi.storage_cem_example.efi_model.EfiStorageModel;
import org.flexiblepower.efi.storage_cem_example.xml.EfiXmlSerializer;
import org.flexiblepower.efi.xml.EfiMessage;

@ClientEndpoint
public class WebsocketClientEndpoint implements EfiMessageSender {

	Session userSession = null;
	private EfiStorageModel storageModel;

	public WebsocketClientEndpoint(URI endpointURI, EfiStorageModel storageModel) {
		this.storageModel = storageModel;
		this.storageModel.setEfiMessageSender(this);
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			container.connectToServer(this, endpointURI);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Callback hook for Connection open events.
	 *
	 * @param userSession
	 *            the userSession which is opened.
	 */
	@OnOpen
	public void onOpen(Session userSession) {
		System.out.println("opening websocket");
		this.userSession = userSession;
	}

	/**
	 * Callback hook for Connection close events.
	 *
	 * @param userSession
	 *            the userSession which is getting closed.
	 * @param reason
	 *            the reason for connection close
	 */
	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
		System.out.println("closing websocket");
		storageModel.unsetEfiMessageSender(this);
		this.userSession = null;
	}

	/**
	 * Callback hook for Message Events. This method will be invoked when a client
	 * send a message.
	 *
	 * @param message
	 *            The text message
	 */
	@OnMessage
	public void onMessage(String message) {
		try {
			System.out.println("Received message: " + message);
			EfiMessage efiMessage = EfiXmlSerializer.deserialize(message);
			this.storageModel.handleEfiMessage(efiMessage);
		} catch (JAXBException e) {
			System.err.println("Error while deserializing XML message " + message);
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Send a message.
	 *
	 * @param message
	 */
	public void sendMessage(String message) {
		this.userSession.getAsyncRemote().sendText(message);
	}

	@Override
	public void sendEfiMessage(EfiMessage efiMessage) {
		try {
			String serialized = EfiXmlSerializer.serialize(efiMessage);
			System.out.println("Sending message: " + serialized);
			sendMessage(serialized);
		} catch (JAXBException e) {
			System.err.println("Error while serializing EFI message");
			e.printStackTrace(System.err);
		}
	}

}