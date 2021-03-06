package de.hsrm.blaubot.core.statemachine.states;

import de.hsrm.blaubot.core.ConnectionStateMachineConfig;
import de.hsrm.blaubot.core.IBlaubotConnection;
import de.hsrm.blaubot.core.IBlaubotDevice;
import de.hsrm.blaubot.core.statemachine.BlaubotAdapterHelper;
import de.hsrm.blaubot.core.statemachine.StateMachineSession;
import de.hsrm.blaubot.core.statemachine.events.AbstractBlaubotDeviceDiscoveryEvent;
import de.hsrm.blaubot.core.statemachine.events.AbstractTimeoutStateMachineEvent;
import de.hsrm.blaubot.core.statemachine.events.DiscoveredFreeEvent;
import de.hsrm.blaubot.core.statemachine.events.DiscoveredKingEvent;
import de.hsrm.blaubot.core.statemachine.states.PeasantState.ConnectionAccomplishmentType;
import de.hsrm.blaubot.message.admin.AbstractAdminMessage;
import de.hsrm.blaubot.util.Log;

public class FreeState implements IBlaubotState {
	private static final String LOG_TAG = "FreeState";
	private StateMachineSession session;
	
	@Override
	public void handleState(StateMachineSession session) {
		this.session = session;
		BlaubotAdapterHelper.startBeacons(session.getBeaconService());
		BlaubotAdapterHelper.stopAcceptors(session.getConnectionStateMachine().getConnectionAcceptors());
		BlaubotAdapterHelper.setDiscoveryActivated(session.getConnectionStateMachine().getBeaconService(), true);
		if(Log.logDebugMessages()) {
			Log.d(LOG_TAG, "Waiting for discovery events.");
		}
	}

	@Override
	public IBlaubotState onConnectionEstablished(IBlaubotConnection connection) {
		// connections should be rejected in free state!
		if(Log.logWarningMessages()) {
			Log.w(LOG_TAG, "Got a connection in FreeState - disconnecting.");
		}
		connection.disconnect();
		return this;
	}

	@Override
	public IBlaubotState onConnectionClosed(IBlaubotConnection connection) {
		return this;
	}

	@Override
	public IBlaubotState onDeviceDiscoveryEvent(AbstractBlaubotDeviceDiscoveryEvent discoveryEvent) {
		IBlaubotDevice device = discoveryEvent.getRemoteDevice();
		if(device.getAdapter().getOwnDevice().compareTo(device) == 0) {
			if(Log.logErrorMessages()) {
				Log.e(LOG_TAG, "We discovered ourselves");
			}
			throw new RuntimeException();
		};
		if (discoveryEvent instanceof DiscoveredKingEvent) {
			if(Log.logDebugMessages()) {
				Log.d(LOG_TAG, "Found a king. Trying to connect to the king " + device.getUniqueDeviceID() + " (" + device.getReadableName() + ")");
			}
			ConnectionStateMachineConfig conf = discoveryEvent.getRemoteDevice().getAdapter().getConnectionStateMachineConfig();
			final int crowningTimeout = conf.getCrowningPreparationTimeout();
			// -> found a king
			// connect to the king; first let the king time to crown himself
			try {
				Thread.sleep(crowningTimeout);
			} catch (InterruptedException e) {
			}
			
			int maxRetries = device.getAdapter().getBlaubotAdapterConfig().getMaxConnectionRetries();
			IBlaubotConnection conn = session.getConnectionManager().connectToBlaubotDevice(device, maxRetries);
			boolean connect = conn != null;
			if (connect) {
				if(Log.logDebugMessages()) {
					Log.d(LOG_TAG, "Successfully connected to King.");
				}
				// change to peasant state
				return new PeasantState(conn, ConnectionAccomplishmentType.VOLUNTARILY);
				
			} else {
				if(Log.logDebugMessages()) {
					Log.d(LOG_TAG, "Failed to connect to King - remaining in FreeState.");
				}
				// do nothing ... we wait for another king
				// TODO: maybe retry every x ms?
			}
		} else if (discoveryEvent instanceof DiscoveredFreeEvent) {
			if(Log.logDebugMessages()) {
				Log.d(LOG_TAG, "Discovered another free blaubot instance");
			}
			// -> we found another free blaubot instance
			// check mac address (or whatever)
			if (device.isGreaterThanOurDevice()) {
				if(Log.logDebugMessages()) {
					Log.d(LOG_TAG, "Other Free is greater than we are - trying to connect.");
				}
				// the other device should get crowned soon
				// connect to the greater device
				int maxRetries = device.getAdapter().getBlaubotAdapterConfig().getMaxConnectionRetries();
				IBlaubotConnection conn = session.getConnectionManager().connectToBlaubotDevice(device, maxRetries);
				boolean connect = conn != null;
				if (connect) {
					if(Log.logDebugMessages()) {
						Log.d(LOG_TAG, "Connection to free succeeded.");
					}
					// change to peasant state
					return new PeasantState(conn, ConnectionAccomplishmentType.VOLUNTARILY);
					
				} else {
					if(Log.logDebugMessages()) {
						Log.d(LOG_TAG, "Connection to free failed, remaining in FreeState.");
					}
					// TODO: maybe retry some times and then decide to be king?
				}
			} else {
				if(Log.logDebugMessages()) {
					Log.d(LOG_TAG, "We are greater or equal than the other Free - we claim the throne ... (" + device.getAdapter().getOwnDevice() + " >= " + device + ")");
				}
				// -- we are the greater device
				// we crown ourselves
				return new KingState();
			}
		} else {
			// yes, no, election ...
			// wee keep our state
		}
		return this;
	}

	@Override
	public IBlaubotState onTimeoutEvent(AbstractTimeoutStateMachineEvent timeoutEvent) {
		return this; // no timeout events
		
	}


	@Override
	public IBlaubotState onAdminMessage(AbstractAdminMessage adminMessage) {
		// not relevant
		return this;
	}

	@Override
	public String toString() {
		return "FreeState";
	}

}
