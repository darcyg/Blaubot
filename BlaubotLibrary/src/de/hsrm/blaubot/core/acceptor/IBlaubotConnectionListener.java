package de.hsrm.blaubot.core.acceptor;

import de.hsrm.blaubot.core.IBlaubotConnection;

/**
 * Listener interface to be registered to {@link IBlaubotConnection} instances.
 * @author Henning Gross <mail.to@henning-gross.de>
 *
 */
public interface IBlaubotConnectionListener {
	/**
	 * Called when a connection was closed (for any reason)
	 * @param connection
	 */
	public void onConnectionClosed(IBlaubotConnection connection);
}
