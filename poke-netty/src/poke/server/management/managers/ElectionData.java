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

import io.netty.channel.Channel;

import java.net.SocketAddress;

/**
 * This class contains a node's connection information and status. The
 * connection information is used to establish a connect to the specified
 * host/port to send and receive heart beat messages.
 * 
 * The information contained in this class is used by both nodes of an edge.
 * 
 * This may appear to be inverted logic as the edges logically read: given nodes
 * A and B, node A has a interest to node B. Node B will send a heartbeatMgr to A
 * rather than A ping's node B. We implement a push model to allow A to
 * passively monitor B (its nodes of interest) though A must establish the
 * connection due to possible firewall issues.
 * 
 * As a result, the heartbeatMgr will keep the connection between the two nodes active.
 * On connection loss, the emitting node (e.g., node B) will wait for the
 * connection to be re-established.
 * 
 * 
 * @author gash
 * 
 */
public class ElectionData {

	 enum VoteAction {
	      ELECTION,
	      NOMINATE ,
	      ABSTAIN ,
	      DECLAREWINNER ,
	      DECLAREVOID 
	   }
	private String nodeId;
	private String host;
	private Integer port;
	private Integer mgmtport;
	private String ballot_id;
	private VoteAction vote;
	private String desc;
	private int expires;
	// the connection
	public SocketAddress sa;
	public Channel channel;
	

	public ElectionData(String nodeId) {
		super();
		this.nodeId = nodeId;
	}

	public ElectionData(String nodeId, String host, Integer port, Integer mgmtport) {
		this.nodeId = nodeId;
		this.host = host;
		this.port = port;
		this.mgmtport = mgmtport;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public Integer getMgmtport() {
		return mgmtport;
	}

	public void setMgmtport(Integer mgmtport) {
		this.mgmtport = mgmtport;
	}

	/**
	 * a heartbeatMgr may not be active (established) on initialization so, we need to
	 * provide a way to initialize the metadata from the connection data
	 * 
	 * @param channel
	 * @param sa
	 */
	public void setConnection(Channel channel, SocketAddress sa) {
		this.channel = channel;
		this.sa = sa;
	}

	/**
	 * clear/reset internal tracking information and connection. The
	 * host/port/ID are retained.
	 */
	public void clearAll() {
		clearElectionData();
	}

	

	public void clearElectionData() {
		// TODO if we attempt o reconnect this should be removed
		if (channel != null)
			channel.close();

		channel = null;
		sa = null;
	}

	 

	/**
	 * An assigned unique key (node ID) to the remote connection
	 * 
	 * @return
	 */
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * The host to connect to
	 * 
	 * @return
	 */
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * The port the remote connection is listening to
	 * 
	 * @return
	 */
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	
	public enum BeatStatus {
		Unknown, Init, Active, Weak, Failed
	}
}
