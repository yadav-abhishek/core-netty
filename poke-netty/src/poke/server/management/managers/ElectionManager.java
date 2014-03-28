/*
 * copyright 2012, gash
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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.ServerConf.NearestConf;
import poke.server.management.ManagementQueue;
import poke.server.management.managers.HeartbeatData.BeatStatus;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.Heartbeat;
import eye.Comm.LeaderElection;
import eye.Comm.Management;
import eye.Comm.LeaderElection.VoteAction;

/**
 * A server can contain multiple, separate interfaces that represent different
 * connections within an overlay network. Therefore, we will need to manage
 * these edges (heart beats) separately.
 * 
 * Essentially, there should be one HeartbeatManager per node (assuming a node
 * does not support more than one overlay network - which it could...).
 * 
 * @author gash
 * 
 */
public class ElectionManager extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<ElectionManager> instance = new AtomicReference<ElectionManager>();
    
	public enum Status{P,NP};
	Status electionstatus=Status.NP;
	
	ElectionData leader;
	
	
	// frequency that heartbeats are checked
	static final int sHeartRate = 5000; // msec
	
	String nodeId;
	ManagementQueue mqueue;
	boolean forever = true;

	ConcurrentHashMap<Channel, ElectionData> outgoingED = new ConcurrentHashMap<Channel, ElectionData>();
	ConcurrentHashMap<String, ElectionData> incomingED = new ConcurrentHashMap<String, ElectionData>();

	public static ElectionManager getInstance(String id) {
		instance.compareAndSet(null, new ElectionManager(id));
		return instance.get();
	}

	public static ElectionManager getInstance() {
		return instance.get();
	}

	/**
	 * initialize the electiontMgr for this server
	 * 
	 * @param nodeId
	 *            The server's (this) ID
	 */
	protected ElectionManager(String nodeId) {
		this.nodeId = nodeId;
		leader = new ElectionData(nodeId);
		// outgoingHB = new DefaultChannelGroup();
	}

	/**
	 * create/register expected connections that this node will make. These
	 * edges are connections this node is responsible for monitoring.
	 * 
	 * @param edges
	 */
	
	public void initNetwork(NearestConf edges) {
	}

	/**
	 * update information on a node we monitor
	 * 
	 * @param nodeId
	 */
	
	
	public int compareID ( String id ){  // 0 for equal | 1 if id > nodeId  |  else -1

		HashMap<String, Integer > idMap = new HashMap<String , Integer>();

		idMap.put("zero", 0);
		idMap.put("one", 1);
		idMap.put("two", 2);
		idMap.put("three", 3);
		
		if ( idMap.get( id ) > idMap.get( nodeId ) )
			return 1;
		else if( idMap.get( nodeId ) > idMap.get( id ) )
			return -1;
		else
			return 0; 		
	}
	
	public void processRequest(LeaderElection req) {
		
		logger.info(" Election manager procedd request " );
		
		if (req == null)
			return;

		if (req.hasExpires()) {
			long ct = System.currentTimeMillis();
			if (ct > req.getExpires()) {
				// election is over
				return;
			}
		}

		if (req.getVote().getNumber() == VoteAction.ELECTION_VALUE) {
			
			logger.info("******REcEIVED THE DECLARE ELECTION MESSAGE********");
			//logger.info(req.toString());

			if ( compareID( req.getNodeId() ) == 1 ) { // id > self 
				logger.info("* Election forewarding packet " + req.getNodeId() );
				
				sendElectionMessage( VoteAction.ELECTION , req.getNodeId() );
				
			} else if ( compareID( req.getNodeId() ) == -1 ) { // id < self 
				
				if ( electionstatus == Status.NP ){
					sendElectionMessage( VoteAction.ELECTION , nodeId );
					logger.info("* Election forewarding packet " + nodeId );
				}else{

					logger.info("* Election Discarding packet " + req.getNodeId() );
					// discard message
				}
				
			} else { // I am winner
				
				logger.info("* Election I am the winner !!!! announcing myself " + req.getNodeId() );
				
				sendElectionMessage( VoteAction.DECLAREWINNER , nodeId );
				leader.setNodeId(nodeId);
				

				electionstatus = Status.NP;
				forever = false;
				
			}
			
		}else if ( req.getVote().getNumber() == VoteAction.DECLAREWINNER_VALUE ){
			
			if ( 0 != compareID( req.getNodeId() ) )
				sendElectionMessage( VoteAction.DECLAREWINNER , req.getNodeId() );
			
			logger.info("* Election Leader is announced now stop and leader is " + req.getNodeId() );
			
			
			leader.setNodeId( req.getNodeId() );
			
			electionstatus = Status.NP;
			forever = false;
	
		}else {
		
		}
			
		/*	
			
		} else if (req.getVote().getNumber() == VoteAction.DECLAREVOID_VALUE) {
			// no one was elected, I am dropping into standby mode`
		} else if (req.getVote().getNumber() == VoteAction.DECLAREWINNER_VALUE) {
			// some node declared them self as the leader
		} else if (req.getVote().getNumber() == VoteAction.ABSTAIN_VALUE) {
			// for some reason, I decline to vote
		} else if (req.getVote().getNumber() == VoteAction.NOMINATE_VALUE) {
			int comparedToMe = req.getNodeId().compareTo(nodeId);
			if (comparedToMe == -1) {
				// Someone else has a higher priority, forward nomination
				// TODO forward
			} else if (comparedToMe == 1) {
				// I have a higher priority, nominate myself
				// TODO nominate myself
			}
		}*/
	}

	/**
	 * send a heartbeatMgr to a node. This is called when a client/server makes
	 * a request to receive heartbeats.
	 * 
	 * @param nodeId
	 * @param ch
	 * @param sa
	 */
	
	public void addOutgoingChannel(String nodeId, String host, int mgmtport, Channel ch, SocketAddress sa) {
		if (!outgoingED.containsKey(ch)) {
			ElectionData election = new ElectionData(nodeId, host, null, mgmtport);
			election.setConnection(ch, sa);
			outgoingED.put(ch, election);

			// when the channel closes, remove it from the outgoingHB
			ch.closeFuture().addListener( new CloseElectionListener( election ) );

		} else {
			logger.error("Received a Election connection unknown to the server, node ID = ", nodeId);
			// TODO actions?
		}
	}

	/**
	 * This is called by the god knows whome when this node requests a
	 * connection to a node, this is called to register interest in creating a
	 * connection/edge.
	 * 
	 * @param node
	 */
	protected void addAdjacentNode(ElectionData node) {
		if (node == null || node.getHost() == null || node.getMgmtport() == null) {
			logger.error("HeartbeatManager registration of edge failed, missing data");
			return;
		}

		if (!incomingED.containsKey(node.getNodeId())) {
			logger.info("Expects to connect to node " + node.getNodeId() + " (" + node.getHost() + ", "
					+ node.getMgmtport() + ")");

			// ensure if we reuse node instances that it is not dirty.
			node.clearAll();
			incomingED.put(node.getNodeId(), node);
		}
	}

	/**
	 * add an incoming end point (receive HB from). This is called when this node
	 * actually establishes the connection to the node. Prior to this call, the
	 * system will register an inactive/pending node through addIncomingNode().
	 * 
	 * @param nodeId
	 * @param ch
	 * @param sa
	 */
	
	public void addAdjacentNodeChannel(String nodeId, Channel ch, SocketAddress sa) {
		ElectionData ed = incomingED.get(nodeId);
		if (ed != null) {
			ed.setConnection(ch, sa);
			 ////////ed.setStatus(BeatStatus.Active);

			// when the channel closes, remove it from the incomingHB list
			ch.closeFuture().addListener(new CloseElectionListener(ed));
		} else {
			logger.error("Received a Election ack from an unknown node, node ID = ", nodeId);
			// TODO actions?
		}
	}

	public void release() {
		forever = true;
	}

/*	private Management generateHB() {
		Heartbeat.Builder h = Heartbeat.newBuilder();
		h.setTimeRef(System.currentTimeMillis());
		h.setNodeId(nodeId);

		Management.Builder b = Management.newBuilder();
		b.setBeat(h.build());

		return b.build();
	}*/
	
	
	public GeneratedMessage generateDeclareElectionMessage( VoteAction va , String electionID )
	{
		LeaderElection.Builder e = LeaderElection.newBuilder();
		e.setBallotId("0");
		e.setNodeId( electionID );
		e.setVote(va);
		
		e.setDesc(nodeId);
		//h.setTimeRef(System.currentTimeMillis());
		//h.setNodeId(nodeId);
		Management.Builder b = Management.newBuilder();
		b.setElection(e.build());
		return b.build();
		
	}
	
	
	public void sendElectionMessage( VoteAction va , String electionID ){
		
		try {
	
			if (outgoingED.size() > 0) {
				
				GeneratedMessage msg = null;
	
				for (ElectionData ed : outgoingED.values()) {
				
					if (msg == null)
						msg = generateDeclareElectionMessage( va , electionID );
	
					try {
					
						logger.info("sending declare election message to "+ed.getNodeId());
						ed.channel.writeAndFlush(msg);
	
						electionstatus=Status.P;
						
						if (logger.isDebugEnabled())
							logger.debug("Election msg ( " + nodeId + ") sent to " + ed.getNodeId() + " at " + ed.getHost());
		
					} catch (Exception e) {
	
						logger.error("Failed " +/* hd.getFailures() +*/ "some times to send HB for " + ed.getNodeId()
								+ " at " + ed.getHost(), e);
					}
				}
				
			} else{
				logger.info("No nodes to send Election messages");
			}
			
		} catch (Exception e) {
		
			logger.error("Unexpected management communcation failure", e);
		
		}
}
	
	

	
	

	@Override
	public void run() {
		logger.info("starting Election manager");

		while (forever) {
			try {
				
				Thread.sleep(sHeartRate);
				sendElectionMessage( VoteAction.ELECTION , nodeId );
				
			} catch (InterruptedException ie) {
				
				break;
				
			} catch (Exception e) {
				
				logger.error("Unexpected management communcation failure", e);
				break;
			
			}
		}

		if (!forever)
			logger.info("Election management outbound queue closing leader is chosen !!!!!!! ");
		else
			logger.info("unexpected closing of Election manager");

	}

	public class CloseElectionListener implements ChannelFutureListener {
		private ElectionData election;

		public CloseElectionListener(ElectionData election) {
			this.election = election;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (outgoingED.containsValue(election)) {
				logger.warn("HB outgoing channel closing for node '" + election.getNodeId() + "' at " + election.getHost());
				outgoingED.remove(future.channel());
			} else if (incomingED.containsValue(election)) {
				logger.warn("HB incoming channel closing for node '" + election.getNodeId() + "' at " + election.getHost());
				incomingED.remove(future.channel());
			}
		}
	}
}
