package org.flexiblepower.efi.storage_cem_example;

import java.net.URI;
import java.net.URISyntaxException;

import org.flexiblepower.efi.storage_cem_example.efi_model.EfiStorageModel;
import org.flexiblepower.efi.storage_cem_example.gui.UserInterface;
import org.flexiblepower.efi.storage_cem_example.websocket.WebsocketClientEndpoint;

public class App {

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: java App.class <websocket url>");
			System.err.println("For example: java App.class ws://localhost:9090/storage");
			System.exit(1);
		}

		try {
			// create StorageModel
			final EfiStorageModel storageModel = new EfiStorageModel();

			// create UI
			final UserInterface ui = new UserInterface(storageModel);
			ui.setUp();

			// Register UI with StorageModel
			storageModel.addObserver(ui);

			// open websocket
			final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(args[0]), storageModel);

			// Wait until the application is killed
			while (true) {
				Thread.sleep(10000);
			}

		} catch (final InterruptedException ex) {
			System.err.println("InterruptedException exception: " + ex.getMessage());
		} catch (final URISyntaxException ex) {
			System.err.println("URISyntaxException exception: " + ex.getMessage());
		}
	}
}
