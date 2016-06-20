package koala;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import messaging.KoalaNGNMsgContent;
import messaging.KoalaMessage;
import messaging.KoalaRTMsgConent;
import messaging.KoalaRouteMsgContent;
import koala.utility.ErrorDetection;
import koala.utility.KoalaJsonParser;
import koala.utility.KoalaNodeUtilities;
import peersim.core.CommonState;
import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;


public class KoalaProtocol extends TopologyProtocol implements CDProtocol{

	KoalaNode myNode;
	public KoalaProtocol(String prefix) {
		super(prefix);
	}
		
	public void join()
	{
		if(hasJoined())
			return;
		
		Node bootstrap = getBootstrap();
		
		if (bootstrap == null)
			setJoined(true);
		else{
			KoalaNode bootstrapRn = (KoalaNode)bootstrap.getProtocol(linkPid);
			String bootstrapID = bootstrapRn.getID();
			myNode.setBootstrapID( bootstrapID );
			KoalaNeighbor first = new KoalaNeighbor(bootstrapID);
			myNode.tryAddNeighbour(first);
			
			setJoined(true);
			KoalaMessage km = new KoalaMessage(myNode.getID(), new KoalaRTMsgConent(myNode));
			send(bootstrapID, km);
		}
	}
	
	private Node getBootstrap()
	{
		KoalaProtocol each;
		ArrayList<Node> joined = new ArrayList<Node>();
		for (int i = 0; i < Network.size(); i++) {
            each = (KoalaProtocol) Network.get(i).getProtocol(myPid);
            if(each.hasJoined())
            	joined.add(Network.get(i));   	
		}
		if(joined.size() == 0)
			return null;
		
		return joined.get(CommonState.r.nextInt(joined.size()));
	}
	
	
	public void send(String destinationID, KoalaMessage msg)
	{
		Node each = null;
		for (int i = 0; i < Network.size(); i++) {
            each =  Network.get(i);
            if(((KoalaNode)each.getProtocol(linkPid)).getID().equals(destinationID))
            	break;
		}
		if(each != null){
			if(ErrorDetection.hasLoopCommunication(myNode.getID(),destinationID))
				System.out.println("problems in horizont");
				
//			System.out.println(me.getID() +"->"+ destinationID);
			msg.setRandomLatency(myNode.getID(), destinationID);
			msg.addToPath(destinationID);
			((KoalaProtocol)each.getProtocol(myPid)).registerMsg(msg);
			/*TODO: uncomment this later*/
//			((KoalaProtocol)each.getProtocol(koalaPid)).receive();
		}
	}
	
	


	protected void onNewGlobalNeighbours(KoalaMessage msg) {
		KoalaNGNMsgContent content = (KoalaNGNMsgContent )msg.getContent();
		
        ArrayList<String> respees = new ArrayList<String>();
        for(String  c : content.getCandidates()){
            if(myNode.isResponsible(c))
                if(respees.size() == 0)
                {
                	KoalaMessage km = new KoalaMessage(myNode.getID(), new KoalaRTMsgConent(myNode), true);
                	send(content.getNeighbor().getNodeID(), km);
                }
 
                respees.add(c);
        }
        content.getNeighbor().setLatencyQuality(2);
        int rnes = myNode.tryAddNeighbour(content.getNeighbor());
        if(rnes != 2)
            return;
        
        List<String> cands = new ArrayList<String>();
        for(String cand : content.getCandidates())
        	if (!respees.contains(cand))
        		cands.add(cand);
        
        Set<String> add_cands = myNode.createRandomIDs(respees.size() - 1);
        cands.addAll(add_cands);
        Set<String> new_cands = new HashSet<String>(cands);
        content.setCandidates(new_cands.toArray(new String[new_cands.size()]));
        msg.setContent(content);
        msg.setSource(myNode.getID());
        
        KoalaNeighbor target = myNode.getRoutingTable().getLocalPredecessor();
        if (msg.getSource().equals(target))
            target = myNode.getRoutingTable().getLocalSucessor();
        if(!KoalaNodeUtilities.isDefault(target))
        	send(target.getNodeID(), msg);
	}

	protected void onRoutingTable(KoalaMessage msg) {
		KoalaNode sender = ((KoalaRTMsgConent)msg.getContent()).getNode();
		ArrayList<KoalaNeighbor> senderOldNeighbors = sender.getRoutingTable().getOldNeighborsContainer();
		ArrayList<KoalaNeighbor> newNeighbors = new ArrayList<KoalaNeighbor>();
		ArrayList<KoalaNeighbor> receivedNeighbors = sender.getRoutingTable().getNeighborsContainer();
		receivedNeighbors.addAll(senderOldNeighbors);
		
		receivedNeighbors.add(new KoalaNeighbor(sender.getID()));
		
		
		
		Set<String> neighborsBefore = myNode.getRoutingTable().getNeighboursIDs();
		Set<Integer> dcsBefore = new HashSet<Integer>(); 
		for(String neighID : neighborsBefore)
			dcsBefore.add(KoalaNodeUtilities.getDCID(neighID));
		dcsBefore.add(myNode.getDCID());
		
		boolean sourceJoining = sender.getJoining();
		boolean selfJoining = myNode.isJoining();
		ArrayList<KoalaNeighbor> myOldNeighbors = new ArrayList<KoalaNeighbor>();
		for(KoalaNeighbor recNeighbor: receivedNeighbors){
			boolean isSource = recNeighbor.getNodeID().equals(sender.getID());
			if(recNeighbor.getNodeID().equals(myNode.getID()))
				continue;
			if(selfJoining && myNode.isLocal(sender.getID()))
				dcsBefore.add(KoalaNodeUtilities.getDCID(recNeighbor.getNodeID()));

			int l = isSource ? msg.getLatency() : recNeighbor.getLatency();
			int lq = myNode.getLatencyQuality(isSource, sender.getID(), recNeighbor);
			
            int res  = myNode.tryAddNeighbour(new KoalaNeighbor(recNeighbor.getNodeID(), l, lq));
			ArrayList<KoalaNeighbor> oldies = myNode.getRoutingTable().getOldNeighborsContainer();
			myOldNeighbors.addAll(oldies);

			myNode.updateLatencyPerDC(recNeighbor.getNodeID(), l, lq);
			
			if( res == 2 || (res == 1 && isSource && sourceJoining))
				newNeighbors.add(new KoalaNeighbor(recNeighbor.getNodeID(), l));
			else if (res < 0 && recNeighbor.getNodeID().equals(sender.getID())){				
				String dest = myNode.getRoute(sender.getID());
				msg.setConfidential(false);
				send(dest, msg);
			}


		}
		myNode.updateLatencies();

		Set<String> neighborsAfter = myNode.getRoutingTable().getNeighboursIDs();
		for(KoalaNeighbor newNeig : newNeighbors){
			if(neighborsAfter.contains(newNeig.getNodeID()) && !neighborsBefore.contains(newNeig.getNodeID()) || newNeig.getNodeID().equals(sender.getID()))
			{
				KoalaMessage newMsg = new KoalaMessage(myNode.getID(), new KoalaRTMsgConent(myNode));
				if(myNode.isLocal(newNeig.getNodeID())){
					send(newNeig.getNodeID(), newMsg);
				}else
				{
					boolean newDC = !dcsBefore.contains(KoalaNodeUtilities.getDCID(newNeig.getNodeID()));
					if(newDC && !selfJoining)
						broadcastGlobalNeighbor(newNeig);
					if(!myNode.isLocal(sender.getID())  && (!msg.isConfidential() || newDC) )
					{
						newMsg.setConfidential(true); 
						send(newNeig.getNodeID(), newMsg);
					}
				}
			}
		}
		
	}

	private void broadcastGlobalNeighbor(KoalaNeighbor newNeig) {
        Set<String> candidates = myNode.createRandomIDs(KoalaNodeUtilities.MAGIC);
        KoalaNeighbor[] localNeigs = {myNode.getRoutingTable().getLocalSucessor(), myNode.getRoutingTable().getLocalPredecessor()};
        
        KoalaNGNMsgContent msgContent = new KoalaNGNMsgContent(candidates.toArray(new String[candidates.size()]), newNeig); 
        KoalaMessage newMsg = new KoalaMessage(myNode.getID(), msgContent);        
        for( KoalaNeighbor n : localNeigs)
            if(!KoalaNodeUtilities.isDefault(n))
                send(n.getNodeID(), newMsg);
		
	}
	
	
	protected void onRoute(KoalaMessage msg){
        String nid = ((KoalaRouteMsgContent)msg.getContent()).getId();
        myNode.updateLatencyPerDC(msg.getSource(), msg.getLatency(), 3);
        myNode.updateLatencies();
        if(nid != myNode.getID())
            send(myNode.getRoute(nid), msg);
        
	}
	
	public boolean hasJoined() {
		return joined;
	}

	@Override
	protected void intializeMyNode(Node node) {
		myNode = (KoalaNode) (Linkable) node.getProtocol(linkPid);
		
	}
}
