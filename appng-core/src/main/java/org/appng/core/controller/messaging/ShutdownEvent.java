package org.appng.core.controller.messaging;

import java.util.Map;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.model.Site;
import org.appng.core.controller.messaging.NodeEvent.NodeState;
import org.slf4j.LoggerFactory;

public class ShutdownEvent extends Event {

	private static final long serialVersionUID = 1L;

	@Override
	public void perform(Environment environment, Site site) throws InvalidConfigurationException, BusinessException {
		String nodeId = getNodeId();
		LoggerFactory.getLogger(ShutdownEvent.class.getName()).info("Received shutdown for node '{}'", nodeId);
		Map<String, NodeState> clusterState = environment.getAttribute(Scope.PLATFORM, NodeEvent.NODE_STATE);
		clusterState.remove(nodeId);
		environment.setAttribute(Scope.PLATFORM, NodeEvent.NODE_STATE, clusterState);
	}

}
