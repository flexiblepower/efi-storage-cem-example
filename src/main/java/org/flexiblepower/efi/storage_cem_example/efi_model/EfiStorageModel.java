package org.flexiblepower.efi.storage_cem_example.efi_model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.UUID;
import java.util.stream.Collectors;

import org.flexiblepower.efi.storage_cem_example.websocket.EfiMessageSender;
import org.flexiblepower.efi.storage_cem_example.xml.XmlUtil;
import org.flexiblepower.efi.xml.ActuatorBehaviour;
import org.flexiblepower.efi.xml.ActuatorInstruction;
import org.flexiblepower.efi.xml.ActuatorInstructions;
import org.flexiblepower.efi.xml.ActuatorStatus;
import org.flexiblepower.efi.xml.EfiMessage;
import org.flexiblepower.efi.xml.EfiMessage.Header;
import org.flexiblepower.efi.xml.FlexibilityRevoke;
import org.flexiblepower.efi.xml.Measurement;
import org.flexiblepower.efi.xml.RunningMode;
import org.flexiblepower.efi.xml.StorageContinuousRunningMode;
import org.flexiblepower.efi.xml.StorageDiscreteRunningMode;
import org.flexiblepower.efi.xml.StorageInstruction;
import org.flexiblepower.efi.xml.StorageRegistration;
import org.flexiblepower.efi.xml.StorageStatus;
import org.flexiblepower.efi.xml.StorageSystemDescription;
import org.flexiblepower.efi.xml.Timer;
import org.flexiblepower.efi.xml.TimerReferences.TimerReference;
import org.flexiblepower.efi.xml.TimerUpdate;
import org.flexiblepower.efi.xml.Transition;

public class EfiStorageModel extends Observable {

	private StorageRegistration storageRegistration;
	private StorageSystemDescription lastStorageSystemDescription;
	private StorageStatus lastStorageStatus;
	private Measurement lastMeasurement;
	private EfiMessageSender efiMessageSender;
	private final Map<Integer, Map<Integer, TimerUpdate>> timerUpdates = new HashMap<>();

	public void setEfiMessageSender(EfiMessageSender efiMessageSender) {
		this.efiMessageSender = efiMessageSender;
	}

	public void unsetEfiMessageSender(EfiMessageSender efiMessageSender) {
		this.efiMessageSender = null;
	}

	/**
	 * Handle a new EFI message coming in from the ResourceManager. This method is
	 * typically called by the WebsocketClientEndpoint.
	 *
	 * @param efiMessage
	 *            EFI message for the Storage category
	 */
	public void handleEfiMessage(EfiMessage efiMessage) {
		if (efiMessage instanceof StorageRegistration) {
			this.storageRegistration = (StorageRegistration) efiMessage;
			System.out.println("Received StorageRegistration message");
		} else if (efiMessage instanceof StorageSystemDescription) {
			this.lastStorageSystemDescription = (StorageSystemDescription) efiMessage;
			System.out.println("Received StorageSystemDescription message");
		} else if (efiMessage instanceof StorageStatus) {
			this.lastStorageStatus = (StorageStatus) efiMessage;
			this.updateTimerUpdates(this.lastStorageStatus);
			System.out.println("Received StorageStatus message");
		} else if (efiMessage instanceof Measurement) {
			this.lastMeasurement = (Measurement) efiMessage;
			System.out.println("Received Measurement message");
		} else if (efiMessage instanceof FlexibilityRevoke) {
			this.revokeFlexibility();
			System.out.println("Received FlexibilityRevoke message");
		} else {
			System.err.println("Received EfiMessage of type " + efiMessage.getClass().getSimpleName()
					+ " but handling this message is not yet implemented");
			return;
		}
		this.setChanged();
		this.notifyObservers(efiMessage.getClass());
	}

	private void revokeFlexibility() {
		this.lastStorageSystemDescription = null;
		this.lastStorageStatus = null;
		this.timerUpdates.clear();
	}

	private void updateTimerUpdates(StorageStatus storageStatus) {
		for (final ActuatorStatus as : storageStatus.getActuatorStatuses().getActuatorStatus()) {
			if (!this.timerUpdates.containsKey(as.getActuatorId())) {
				this.timerUpdates.put(as.getActuatorId(), new HashMap<>());
			}
			final Map<Integer, TimerUpdate> actuatorMap = this.timerUpdates.get(as.getActuatorId());
			for (final TimerUpdate t : as.getTimerUpdates().getTimerUpdate()) {
				actuatorMap.put(t.getTimerId(), t);
			}
		}
	}

	/**
	 * Return the EFI Resource ID as specified by the ResourceManager.
	 *
	 * @return the EFI Resource ID, or null if not yet known
	 */
	public String getEfiResourceId() {
		if (this.storageRegistration != null) {
			return this.storageRegistration.getHeader().getEfiResourceId();
		} else {
			return null;
		}
	}

	/**
	 * Return the currently known fill level
	 *
	 * @return The fill level, or null if not yet known
	 */
	public Double getFillLevel() {
		if (this.lastStorageStatus != null) {
			return this.lastStorageStatus.getCurrentFillLevel();
		} else {
			return null;
		}
	}

	/**
	 * Return the last received Measurement message from the ResourceManager.
	 *
	 * @return The last received Measurement, or null if no Measurement received
	 */
	public Measurement getLastMeasurement() {
		return this.lastMeasurement;
	}

	/**
	 * List all the ActuatorBehavior elements from the StorageSystemDescription
	 * message
	 *
	 * @return A list of all AcutatorBehavior elements, or null if no
	 *         StorageSystemDescription received yet
	 */
	public List<ActuatorBehaviour> getActuatorBehaviours() {
		if (this.lastStorageSystemDescription != null) {
			return this.lastStorageSystemDescription.getActuatorBehaviours().getActuatorBehaviour();
		} else {
			return null;
		}
	}

	/**
	 * List the status of all actuators, as described by the last received
	 * StorageStatus message
	 *
	 * @return A list of AcutatorStatus elements, or null if no StorageStatus
	 *         message received yet
	 */
	public List<ActuatorStatus> getActuatorStatuses() {
		if (this.lastStorageStatus != null) {
			return this.lastStorageStatus.getActuatorStatuses().getActuatorStatus();
		} else {
			return null;
		}
	}

	/**
	 * The AcutatorBehavior for a specific Actuator ID
	 *
	 * @param actuatorId
	 *            ID of the desired actuator
	 * @return AcutatorBehavior, or null if actuator ID unknown or no
	 *         StorageSystemDescription received yet
	 */
	public ActuatorBehaviour getActuatorBehaviourById(int actuatorId) {
		final List<ActuatorBehaviour> actuators = this.getActuatorBehaviours();
		if (actuators == null) {
			return null;
		} else {
			return actuators.stream().filter(a -> a.getActuatorId() == actuatorId).findFirst().orElse(null);
		}
	}

	public ActuatorStatus getActuatorStatusById(int actuatorId) {
		final List<ActuatorStatus> actuators = this.getActuatorStatuses();
		if (actuators == null) {
			return null;
		} else {
			return actuators.stream().filter(a -> a.getActuatorId() == actuatorId).findFirst().orElse(null);
		}
	}

	public List<RunningMode> getRunningModes(int actuatorId) {
		final ActuatorBehaviour actuator = this.getActuatorBehaviourById(actuatorId);
		if (actuator == null) {
			return null;
		} else {
			return actuator.getRunningModes().getDiscreteRunningModeOrContinuousRunningMode();
		}
	}

	public RunningMode getRunningMode(int actuatorId, int runningModeId) {
		final List<RunningMode> runningModes = this.getRunningModes(actuatorId);
		if (runningModes == null) {
			return null;
		} else {
			return runningModes.stream().filter(rm -> rm.getId() == runningModeId).findFirst().orElse(null);
		}
	}

	public Integer getActiveRunningMode(int actuatorId) {
		final ActuatorStatus actuatorStatus = this.getActuatorStatusById(actuatorId);
		if (actuatorStatus == null) {
			return null;
		} else {
			return actuatorStatus.getCurrentRunningMode();
		}
	}

	private StorageInstruction createEmptyInstructionMessage() {
		return new StorageInstruction().withEfiVersion("2.0")
				.withHeader(new Header().withEfiResourceId(this.storageRegistration.getHeader().getEfiResourceId())
						.withTimestamp(XmlUtil.date(new Date())))
				.withFlexibilityUpdateId(this.lastStorageStatus.getFlexibilityUpdateId())
				.withInstructionId(UUID.randomUUID().toString()).withIsEmergencyInstruction(false);
	}

	public List<Transition> getOutgoingTransitions(int actuatorId, int runningModeId) {
		final ActuatorBehaviour actuator = this.getActuatorBehaviourById(actuatorId);
		if (actuator == null || actuator.getTransitions() == null) {
			return null;
		}
		return actuator.getTransitions().getTransition().stream().filter(t -> t.getFromRunningModeId() == runningModeId)
				.collect(Collectors.toList());
	}

	public boolean isTransitionBlocked(int actuatorId, Transition transition) {
		for (final TimerReference tr : transition.getBlockingTimers().getTimerReference()) {
			final Timer timer = this.getTimer(actuatorId, tr.getTimerId());
			if (this.isTimerActive(actuatorId, timer)) {
				// The timer is active, the transition is currently blocked
				return true;
			}
		}
		// If we encountered no blocking timers, the transition is not blocked
		return false;
	}

	public Date transitionBlockedUntil(int actuatorId, Transition transition) {
		Date end = new Date();
		for (final TimerReference tr : transition.getBlockingTimers().getTimerReference()) {
			final Timer timer = this.getTimer(actuatorId, tr.getTimerId());
			final Date finishedAt = this.getTimerFinishedAt(actuatorId, timer);
			if (finishedAt.after(end)) {
				end = finishedAt;
			}
		}
		return end;
	}

	public boolean isTimerActive(int actuatorId, Timer timer) {
		final TimerUpdate lastTimerUpdate = this.timerUpdates.get(actuatorId).get(timer.getId());
		return XmlUtil.date(lastTimerUpdate.getFinishedAt()).after(new Date());
	}

	public Date getTimerFinishedAt(int actuatorId, Timer timer) {
		final TimerUpdate lastTimerUpdate = this.timerUpdates.get(actuatorId).get(timer.getId());
		return XmlUtil.date(lastTimerUpdate.getFinishedAt());
	}

	public Timer getTimer(int actuatorId, int timerId) {
		final ActuatorBehaviour actuator = this.getActuatorBehaviourById(actuatorId);
		if (actuator == null || actuator.getTimers() == null) {
			return null;
		}
		return actuator.getTimers().getTimer().stream().filter(t -> t.getId() == timerId).findFirst().orElse(null);
	}

	public void activateDiscreteRunningMode(int actuatorId, int runningModeId) {
		if (this.lastStorageSystemDescription == null) {
			throw new IllegalStateException("No System description message received yet");
		}
		final RunningMode runningMode = this.getRunningMode(actuatorId, runningModeId);
		if (runningMode == null) {
			throw new IllegalArgumentException(
					"Runnigmode " + runningModeId + " for actuator " + actuatorId + " doesn't exist");
		}
		if (runningMode instanceof StorageDiscreteRunningMode) {
			final StorageInstruction instruction = this.createEmptyInstructionMessage();
			final ActuatorInstructions actuatorInstructions = new ActuatorInstructions();
			actuatorInstructions.withActuatorInstruction(new ActuatorInstruction().withActuatorId(actuatorId)
					.withRunningModeId(runningModeId).withStartTime(XmlUtil.date(new Date())));
			instruction.setActuatorInstructions(actuatorInstructions);
			if (this.efiMessageSender == null) {
				System.err.println("Could not send EFI message, no efiMessageSender registered");
			} else {
				this.efiMessageSender.sendEfiMessage(instruction);
			}
		} else {
			throw new IllegalArgumentException(
					"Runnigmode " + runningModeId + " for actuator " + actuatorId + " is not a discrete runningmode");
		}
	}

	public void activateContinuousRunningMode(int actuatorId, int runningModeId, double factor) {
		if (this.lastStorageSystemDescription == null) {
			throw new IllegalStateException("No System description message received yet");
		}
		final RunningMode runningMode = this.getRunningMode(actuatorId, runningModeId);
		if (runningMode == null) {
			throw new IllegalArgumentException(
					"Runnigmode " + runningModeId + " for actuator " + actuatorId + " doesn't exist");
		}
		if (runningMode instanceof StorageContinuousRunningMode) {
			final StorageInstruction instruction = this.createEmptyInstructionMessage();
			final ActuatorInstructions actuatorInstructions = new ActuatorInstructions();
			actuatorInstructions.withActuatorInstruction(
					new ActuatorInstruction().withActuatorId(actuatorId).withRunningModeId(runningModeId)
							.withRunningModeFactor(factor).withStartTime(XmlUtil.date(new Date())));
			instruction.setActuatorInstructions(actuatorInstructions);
			if (this.efiMessageSender == null) {
				System.err.println("Could not send EFI message, no efiMessageSender registered");
			} else {
				this.efiMessageSender.sendEfiMessage(instruction);
			}
		} else {
			throw new IllegalArgumentException(
					"Runnigmode " + runningModeId + " for actuator " + actuatorId + " is not a continuous runningmode");
		}
	}

}
