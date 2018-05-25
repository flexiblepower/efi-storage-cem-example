package org.flexiblepower.efi.storage_cem_example.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.flexiblepower.efi.storage_cem_example.efi_model.EfiStorageModel;
import org.flexiblepower.efi.xml.ActuatorBehaviour;
import org.flexiblepower.efi.xml.Measurement;
import org.flexiblepower.efi.xml.RunningMode;
import org.flexiblepower.efi.xml.StorageContinuousRunningMode;
import org.flexiblepower.efi.xml.StorageStatus;
import org.flexiblepower.efi.xml.StorageSystemDescription;
import org.flexiblepower.efi.xml.Transition;

public class UserInterface implements Observer {

	private boolean actuatorPanelCreated = false;
	private JLabel fillLevelLabel;
	private JLabel powerLabel;
	private JPanel actuatorsPanel;
	private JFrame frame;
	private final Map<Integer, Map<Integer, JLabel>> rmStatusLabels = new HashMap<>();
	private final Map<Integer, Map<Integer, JButton>> rmActivateButtons = new HashMap<>();
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final EfiStorageModel model;

	public UserInterface(EfiStorageModel model) {
		this.model = model;
	}

	public void setUp() {
		this.fillLevelLabel = new JLabel("FillLevel: ?");
		this.fillLevelLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.powerLabel = new JLabel("Power: ?");
		this.powerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.actuatorsPanel = new JPanel();
		this.actuatorsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		EventQueue.invokeLater(() -> {
			this.frame = new JFrame("EFI Storage demo client");
			this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.frame.setPreferredSize(new Dimension(500, 600));

			// Set up the content pane.
			final Container pane = this.frame.getContentPane();
			pane.setLayout(new GridLayout(2, 1));

			final JPanel statusPanel = new JPanel();
			statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
			statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			statusPanel.setBorder(BorderFactory.createTitledBorder("Storage status information"));

			statusPanel.add(this.fillLevelLabel);
			statusPanel.add(this.powerLabel);

			pane.add(statusPanel);

			pane.add(this.actuatorsPanel);

			// Display the window.
			this.frame.pack();
			this.frame.setVisible(true);
		});

		this.executor.scheduleAtFixedRate(() -> {
			EventQueue.invokeLater(() -> {
				this.updateRunningModes(this.model);
			});
		}, 5, 5, TimeUnit.SECONDS);
	}

	@Override
	public void update(Observable o, Object msgType) {
		try {
			if (Measurement.class.equals(msgType)) {
				// We received an new Mesaurement, update the Power label
				this.powerLabel.setText("Power: " + (this.model.getLastMeasurement() == null ? "?"
						: this.model.getLastMeasurement().getElectricityMeasurement().getPower() + " watt ("
								+ this.model.getLastMeasurement().getMeasurementTimestamp() + ")"));
			} else if (StorageStatus.class.equals(msgType)) {
				// We received a StorageStatus message, update the fillevel
				this.fillLevelLabel
						.setText("FillLevel: " + (this.model.getFillLevel() == null ? "?" : this.model.getFillLevel()));
				if (!this.actuatorPanelCreated) {
					this.createActuatorPanels(this.model);
				}
				this.updateRunningModes(this.model);
			} else if (StorageSystemDescription.class.equals(msgType)) {
				this.createActuatorPanels(this.model);
			}
		} catch (final Exception e) {
			System.err.println("Error while updating UI");
			e.printStackTrace(System.err);
		}
	}

	private void createActuatorPanels(EfiStorageModel model) {
		// cleanup
		this.rmStatusLabels.clear();
		this.rmActivateButtons.clear();
		this.actuatorsPanel.removeAll();
		if (model.getActuatorBehaviours().isEmpty()) {
			return;
		}

		this.actuatorsPanel.setLayout(new GridLayout(1, model.getActuatorBehaviours().size()));
		for (final ActuatorBehaviour a : model.getActuatorBehaviours()) {
			if (model.getActiveRunningMode(a.getActuatorId()) == null) {
				// No status update message yet, we don't have enough information to create the
				// Acutators panel
				return;
			}
			this.rmActivateButtons.put(a.getActuatorId(), new HashMap<>());
			this.rmStatusLabels.put(a.getActuatorId(), new HashMap<>());
			final JPanel actuatorPanel = new JPanel();
			this.actuatorsPanel.add(actuatorPanel);
			actuatorPanel.setLayout(new BoxLayout(actuatorPanel, BoxLayout.Y_AXIS));
			actuatorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			actuatorPanel.setBorder(BorderFactory.createTitledBorder("Actuator " + a.getActuatorId()));
			for (final RunningMode rm : model.getRunningModes(a.getActuatorId())) {
				actuatorPanel.add(new JLabel("RunningMode " + rm.getId() + " (" + rm.getLabel() + ")"));
				final JLabel statusLabel = new JLabel("Status: "
						+ (rm.getId() == model.getActiveRunningMode(a.getActuatorId()) ? "ACTIVE" : "INACTIVE"));
				actuatorPanel.add(statusLabel);
				this.rmStatusLabels.get(a.getActuatorId()).put(rm.getId(), statusLabel);
				final JButton activateButton;
				if (rm instanceof StorageContinuousRunningMode) {
					actuatorPanel.add(new JLabel("Type: Continuous"));
					actuatorPanel.add(new JLabel("Factor:"));
					final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1));
					actuatorPanel.add(spinner);
					activateButton = new JButton("Activate");
					activateButton.addActionListener(e -> {
						this.executor.submit(() -> {
							model.activateContinuousRunningMode(a.getActuatorId(), rm.getId(),
									(double) spinner.getValue());
						});
					});
					actuatorPanel.add(activateButton);
				} else {
					actuatorPanel.add(new JLabel("Type: Discrete"));
					activateButton = new JButton("Activate");
					activateButton.addActionListener(e -> {
						this.executor.submit(() -> {
							model.activateDiscreteRunningMode(a.getActuatorId(), rm.getId());
						});
					});
					actuatorPanel.add(activateButton);
				}
				this.rmActivateButtons.get(a.getActuatorId()).put(rm.getId(), activateButton);
			}
		}
		this.actuatorsPanel.validate();
		this.actuatorPanelCreated = true;
	}

	private void updateRunningModes(EfiStorageModel model) {
		for (final ActuatorBehaviour a : model.getActuatorBehaviours()) {
			if (this.rmStatusLabels.containsKey(a.getActuatorId())) {
				final int activeRunningModeId = model.getActiveRunningMode(a.getActuatorId());
				final List<Transition> outgoingTransitions = model.getOutgoingTransitions(a.getActuatorId(),
						activeRunningModeId);
				for (final RunningMode rm : model.getRunningModes(a.getActuatorId())) {
					final JLabel label = this.rmStatusLabels.get(a.getActuatorId()).get(rm.getId());
					final JButton button = this.rmActivateButtons.get(a.getActuatorId()).get(rm.getId());
					final Transition transition = this.getTransitionTo(outgoingTransitions, rm.getId());
					if (rm.getId() == activeRunningModeId) {
						label.setText("Status: Active");
						button.setEnabled(false);
					} else if (transition != null) {
						// There is a transition to this runnigmode... is it blocked by a timer?
						if (model.isTransitionBlocked(a.getActuatorId(), transition)) {
							label.setText("Status: Blocked by timer until "
									+ model.transitionBlockedUntil(a.getActuatorId(), transition));
							button.setEnabled(false);
						} else {
							label.setText("Status: Ready to be activated");
							button.setEnabled(true);
						}
					} else {
						// There is no transition to this runningmode
						label.setText("Status: Not reachable");
						button.setEnabled(false);
					}
				}
			}
		}
	}

	private Transition getTransitionTo(List<Transition> outgoingTransitions, int runningModeId) {
		return outgoingTransitions.stream().filter(t -> t.getToRunningModeId() == runningModeId).findAny().orElse(null);
	}

}
