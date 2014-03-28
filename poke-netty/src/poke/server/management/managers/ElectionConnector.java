/*
 * copyright 2014, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.server.management.managers;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.ElectionMonitor;
import poke.monitor.HeartMonitor;
import poke.monitor.MonitorHandler;
import poke.server.management.managers.HeartbeatData.BeatStatus;

/**
 * The connector collects connection monitors (e.g., listeners implement the
 * circuit breaker) that maintain HB communication between nodes (to
 * client/requester).
 * 
 * @author gash
 * 
 */
public class ElectionConnector extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<ElectionConnector> instance = new AtomicReference<ElectionConnector>();
	private ConcurrentLinkedQueue<ElectionMonitor> monitors = new ConcurrentLinkedQueue<ElectionMonitor>();
	private int sConnectRate = 2000; // m seconds
	private boolean forever = true;

	public static ElectionConnector getInstance() {
		instance.compareAndSet(null, new ElectionConnector());
		return instance.get();
	}

	/**
	 * The connector will only add nodes for connections that this node wants to
	 * establish. Out bound (we send HB messages to) requests do not come through
	 * this class.
	 * 
	 * @param node
	 */
	public void addConnectToThisNode(ElectionData node) {
		// null data is not allowed
		if (node == null || node.getNodeId() == null)
			throw new RuntimeException("Null nodes or IDs are not allowed");

		// register the node to the manager that is used to determine if a
		// connection is usable by the public messaging
		ElectionManager.getInstance().addAdjacentNode(node);
		
		// this class will monitor this channel/connection and together with the
		// manager, we create the circuit breaker pattern to separate
		// health-status from usage.
		ElectionMonitor em = new ElectionMonitor(node.getNodeId(), node.getHost(), node.getMgmtport());
		em.addListener(new ElectionListener(node));

		// add monitor to the list of adjacent nodes that we track
		monitors.add(em);
	}

	@Override
	public void run() {
		if (monitors.size() == 0) {
			logger.info("Election connection monitor not started, no connections to establish");
			return;
		} else
			logger.info("Election connection monitor starting, node has " + monitors.size() + " connections");

		while (forever) {
			try {
				Thread.sleep(sConnectRate);

				// try to establish connections to our nearest nodes
				for (ElectionMonitor ed : monitors) {
					if (!ed.isConnected()) {
						try {
							logger.info("attempting to connect to node: " + ed.getNodeInfo());
							ed.startElection();
						} catch (Exception ie) {
							// do nothing
						}
					}
				}
			} catch (InterruptedException e) {
				logger.error("Unexpected Election connector failure", e);
				break;
			}
		}
		logger.info("ending Election connection monitoring thread");
	}

	private void validateConnection() {
		// validate connections this node wants to create
		for ( ElectionData ed : ElectionManager.getInstance().incomingED.values()) {
			
			logger.info("Election Connector validate connection incoming values ");
			
			// receive HB - need to check if the channel is readable
		/*	if (ed.channel == null) {
				if (ed.getStatus() == BeatStatus.Active || ed.getStatus() == BeatStatus.Weak) {
					ed.setStatus(BeatStatus.Failed);
					ed.setLastFailed(System.currentTimeMillis());
					ed.incrementFailures();
				}
			} else if (hb.channel.isOpen()) {
				if (hb.channel.isWritable()) {
					if (System.currentTimeMillis() - hb.getLastBeat() >= hb.getBeatInterval()) {
						hb.incrementFailures();
						hb.setStatus(BeatStatus.Weak);
					} else {
						hb.setStatus(BeatStatus.Active);
						hb.setFailures(0);
					}
				} else
					hb.setStatus(BeatStatus.Weak);
			} else {
				if (hb.getStatus() != BeatStatus.Init) {
					hb.setStatus(BeatStatus.Failed);
					hb.setLastFailed(System.currentTimeMillis());
					hb.incrementFailures();
				}
			}*/
		}

		// validate connections this node wants to create
		for (ElectionData ed : ElectionManager.getInstance().outgoingED.values()) {
			logger.info("Election Connector validate connection outgoing values ");
			// emit HB - need to check if the channel is writable
		/*	if (hb.channel == null) {
				if (hb.getStatus() == BeatStatus.Active || hb.getStatus() == BeatStatus.Weak) {
					hb.setStatus(BeatStatus.Failed);
					hb.setLastFailed(System.currentTimeMillis());
					hb.incrementFailures();
				}
			} else if (hb.channel.isOpen()) {
				if (hb.channel.isWritable()) {
					if (System.currentTimeMillis() - hb.getLastBeat() >= hb.getBeatInterval()) {
						hb.incrementFailures();
						hb.setStatus(BeatStatus.Weak);
					} else {
						hb.setStatus(BeatStatus.Active);
						hb.setFailures(0);
					}
				} else
					hb.setStatus(BeatStatus.Weak);
			} else {
				if (hb.getStatus() != BeatStatus.Init) {
					hb.setStatus(BeatStatus.Failed);
					hb.setLastFailed(System.currentTimeMillis());
					hb.incrementFailures();
				}
			}*/
		}
	}
}
