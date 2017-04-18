package koala;



import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


import messaging.KoalaMessage;
import peersim.core.CommonState;
import peersim.core.Node;
import topology.TopologyNode;
import utilities.NodeUtilities;


public class KoalaNode extends TopologyNode{

	/**
	 * 
	 */

//	private int dcID;
	
	private String bootstrapID;
	
//	these two are just for statistics purposes, not necessary  
	public int nrMsgRouted=0;
	public int nrMsgRoutedByLatency=0;
	
	
	

	private boolean isJoining;
	private KoalaRoutingTable routingTable;
	
	private HashMap<Integer, Double> latencyPerDC;
	
	public KoalaNode(String prefix) {
		super(prefix);
	}
	
	
	public KoalaNode(String prefix, String cid, String sid) {
		super(prefix, cid, sid);
	}
	
	public Object clone() {
		KoalaNode inp = null;
        inp = (KoalaNode) super.clone();
        inp.reset();
        return inp;
    }

	public void reset(){
		super.reset();
		resetRoutingTable();
		resetLatencyPerDC();
	}
	
	public void resetLatencyPerDC(){
		this.latencyPerDC = new HashMap<Integer, Double>();
	}
	
//
//    public int getNodeID() {
//		return nodeID;
//	}
//
//	public void setNodeID(int nodeID) {
//		this.nodeID = nodeID;
//	}
//
//	public void setID(int dcID, int nodeID) {
//		this.dcID = dcID;
//		this.nodeID = nodeID;
//	}
//	
//	public void setID(String id) {
//		this.dcID = Integer.parseInt(id.split("-")[0]);
//		this.nodeID = Integer.parseInt(id.split("-")[1]);
//	}
//	
//	public String getSID() {
//		return this.dcID + "-" + this.nodeID;
//	}
	
	public String getBootstrapID() {
		return bootstrapID;
	}

	public void setBootstrapID(String bootstrapID) {
		this.bootstrapID = bootstrapID;
	}
	
	public KoalaRoutingTable getRoutingTable() {
		return routingTable;
	}
	
	public void resetRoutingTable() {
		routingTable = new KoalaRoutingTable();
	}
	

	public boolean getJoining(){
		return this.isJoining; 
	}
	
	public void setJoining(boolean isJoining) {
		this.isJoining = isJoining;
	}

	public String toString(){
		return getSID();
	}

	
	/* The relevant methods start here */
	
	
	public int tryAddNeighbour(KoalaNeighbor n){
//      Discard down neighbors (like Lamport would do)
		Node nn = NodeUtilities.Nodes.get(n.getCID());
		if(nn==null || !nn.isUp())
        	return -1;
			
		ArrayList<KoalaNeighbor> oldNeighbors = new ArrayList<KoalaNeighbor>();
        int addedS, addedP;
        addedS = addedP = -1;
        KoalaNeighbor oldS, oldP;
        oldS = oldP = null;
        
        
        
        boolean local = this.isLocal(n.getSID());
		if(local){
			addedS = getRoutingTable().addLocal(n) ? 1 : 0;
		}else{
	        for(int i = 0; i < NodeUtilities.NEIGHBORS; i++){
			    if(this.isSuccessor(n.getSID(), i)){
			    	if(i==0) addedS = 0;
			        oldS = getRoutingTable().setGlobalSucessor(n,i);
			    }
			    if (this.isPredecessor(n.getSID(), i)){
			    	if(i==0) addedP = 0;
			        oldP = getRoutingTable().setGlobalPredecessor(n,i);
			    }
			    if(i==0){
			    	addedS = oldS != null ? 1 : addedS;
			    	addedP = oldP != null ? 1 : addedP;
			    	if (!NodeUtilities.isDefault(oldS)) oldNeighbors.add(oldS);
			    	if (!NodeUtilities.isDefault(oldP)) oldNeighbors.add(oldP);
			    }
	        }
		}
//        updateNeighbors(n);
        
        int ret = Math.max(addedS, addedP);
         
        if( ret == 1)
            ret++;

        if( ret ==-1 && canBeNeighbour(n.getSID(),0))
            ret = 1;

        getRoutingTable().setOldNeighborsContainer(oldNeighbors);
        // 2: added, 1: potential neighbor, 0: updated , -1:not neighbor
        return ret; // should return oldNeighbors as well
	}
	
	
	public int getLatencyQuality(boolean isSender, String sourceID, KoalaNeighbor kn){
		if(isSender)
			return 3;
		if(isLocal(sourceID) && kn.getLatencyQuality() > 1)
			return 2;
		return 1;
	}
	
	public boolean isJoining(){
		if(this.getBootstrapID() == null)
			return false;
		ArrayList<KoalaNeighbor> neighbors = getRoutingTable().getNeighbors();
		for(KoalaNeighbor kn : neighbors)
			if(!NodeUtilities.isDefault(kn) && kn.getLatency() != -1)
				return false;
		return true;
	}
	
	
	
	
	private boolean isSuccessor(String nodeID, int index){
        if (this.isLocal(nodeID)) return false;
        
        KoalaNeighbor successor = getRoutingTable().getGlobalSucessor(index);
        KoalaNeighbor predecessor = getRoutingTable().getGlobalPredecessor(index);
        String current = index == 0 ? this.getSID() : getRoutingTable().getGlobalSucessor(index-1).getSID();
        if(index > 0)
        	predecessor = getRoutingTable().getGlobalSucessor(index-1);
        if (canBeSuccessor(nodeID, index)){
        	if( NodeUtilities.isDefault(successor) ||  NodeUtilities.compareIDs(nodeID, successor.getSID(), false) != 0)
        		return true;
        	if(successor.equals(predecessor))
        		return true;
        	if(!nodeID.equals(predecessor.getSID())  &&  NodeUtilities.distanceGlobal(current, nodeID) < NodeUtilities.distanceGlobal(current, successor.getSID()))
        		return true;
        	if(NodeUtilities.distanceGlobal(current, nodeID) == NodeUtilities.distanceGlobal(current, successor.getSID()) &&
               NodeUtilities.distanceLocal(current, nodeID) <= NodeUtilities.distanceLocal(current, successor.getSID()))
             		return true;
        }
        return false;
	}
	 
	private boolean isPredecessor(String nodeID, int index){
		if (this.isLocal(nodeID)) return false;
		
        KoalaNeighbor successor = getRoutingTable().getGlobalSucessor(index);
        KoalaNeighbor predecessor =getRoutingTable().getGlobalPredecessor(index);
        String current = index == 0 ? this.getSID() : getRoutingTable().getGlobalSucessor(index-1).getSID();
        if(index > 0)
        	successor = getRoutingTable().getGlobalPredecessor(index-1);
        if (canBePredecessor(nodeID, index)){
        	if(NodeUtilities.isDefault(predecessor) || NodeUtilities.compareIDs(nodeID, predecessor.getSID(), false) != 0)
        		return true;
        	if(successor.getSID().equals(predecessor.getSID()))
        		return true;
        	if(!nodeID.equals(successor.getSID())  &&  NodeUtilities.distanceGlobal(current, nodeID) < NodeUtilities.distanceGlobal(current, predecessor.getSID()))
        		return true;
        	if(NodeUtilities.distanceGlobal(current, nodeID) == NodeUtilities.distanceGlobal(current, predecessor.getSID()) &&
        	   NodeUtilities.distanceLocal(current, nodeID) <= NodeUtilities.distanceLocal(current, predecessor.getSID()))
        		return true;
        }
        return false;
	}
	
	
//	can be but is not
	private boolean canBeNeighbour(String nodeID, int index){
		if(routingTable.getNeighboursIDs(2).contains(nodeID))
			return false;
        if (canBeSuccessor(nodeID, index))
            return true;
        if (canBePredecessor(nodeID, index))
            return true;
        return false;
	}
	
	private boolean canBeSuccessor(String nodeID, int index){
        if( this.isLocal(nodeID)) return false;
        KoalaNeighbor successor = getRoutingTable().getGlobalSucessor(index);
        String current;
        if(index == 0)
        	current = this.getSID();
        else
        	current = getRoutingTable().getGlobalSucessor(index-1).getSID(); 
        
        if (NodeUtilities.isDefault(successor) || successor.getSID().equals(current))
            return true;
        else{
            if((NodeUtilities.compareIDs(nodeID, successor.getSID(), false) <= 0 && NodeUtilities.compareIDs(successor.getSID(), current, false) < 0) || 
               (NodeUtilities.compareIDs(nodeID, successor.getSID(), false) >= 0 && NodeUtilities.compareIDs(successor.getSID(), current, false) < 0 && NodeUtilities.compareIDs(nodeID, current, false) > 0) || 
               (NodeUtilities.compareIDs(nodeID, successor.getSID(), false) <= 0 && NodeUtilities.compareIDs(nodeID, current, false) > 0) )
                return true;
        }
        return false;
	}
	
	 
	private boolean canBePredecessor(String nodeID, int index){
		if( this.isLocal(nodeID)) return false;
        KoalaNeighbor predecessor = getRoutingTable().getGlobalPredecessor(index);
        String current = index == 0 ? this.getSID() : getRoutingTable().getGlobalPredecessor(index-1).getSID();
         
        if (NodeUtilities.isDefault(predecessor) || predecessor.getSID().equals(current) )
            return true;
        else{
            if((NodeUtilities.compareIDs(nodeID, predecessor.getSID(), false) >= 0 && NodeUtilities.compareIDs(predecessor.getSID(), current, false) > 0) || 
               (NodeUtilities.compareIDs(nodeID, predecessor.getSID(), false) <= 0 && NodeUtilities.compareIDs(predecessor.getSID(), current, false) > 0 && NodeUtilities.compareIDs(nodeID, current, false) < 0) || 
               (NodeUtilities.compareIDs(nodeID, predecessor.getSID(), false) >= 0 && NodeUtilities.compareIDs(nodeID, current, false) < 0) )
                return true;
        }
        return false;
	}
    
    	
	public boolean isLocal(String id){
		return NodeUtilities.sameDC(getSID(), id); 
	}

	public boolean isLocal(KoalaNeighbor n){
		return NodeUtilities.sameDC(getSID(), n.getSID()); 
	}
	
	
    public void updateLatencyPerDC(String id, double l, int lq){    	
    	if(this.getSID().equals(id))
    		return;
        if (lq > 1)
            latencyPerDC.put(NodeUtilities.getDCID(id), l);

        ArrayList<KoalaNeighbor> links = routingTable.getNeighbors();
        for(KoalaNeighbor ln : links){
            if(ln.getSID().equals(id) && lq >= ln.getLatencyQuality()){
                ln.setLatency(l);
                ln.setLatencyQuality(lq);
            }
        }
    }
    
    public void updateLatencies(){
    	ArrayList<KoalaNeighbor> neigs = routingTable.getNeighbors();
    	for(KoalaNeighbor n : neigs){
    		if(isLocal(n.getSID()))
    			continue;
    		int ndc = NodeUtilities.getDCID(n.getSID());
    		if(n.getLatencyQuality() < 2 &&  latencyPerDC.keySet().contains(ndc)){
    			n.setLatency(2);
    			n.setLatency(latencyPerDC.get(ndc));
    		}
    			
    	}
    }
    	
    
    public KoalaNeighbor getRoute(KoalaNode dest,  KoalaMessage msg) {
    	KoalaNeighbor normal = getRouteForAlpha(dest, msg, NodeUtilities.B);
    	KoalaNeighbor no_latency = getRouteForAlpha(dest, msg, 1);
    	if(normal!=null && no_latency!=null && !normal.equals(no_latency))
    		this.nrMsgRoutedByLatency++;
    	return normal;
    }
    
    public KoalaNeighbor getRouteForAlpha(KoalaNode dest,  KoalaMessage msg, double alpha) {
		ArrayList<String> destNeigs = dest.getRoutingTable().getNeighborsContainerIDs();
    	AbstractMap.SimpleEntry<Double, KoalaNeighbor> mre;
		double v=0;
		ArrayList<KoalaNeighbor> rt = getRoutingTable().getNeighbors();
		ArrayList<AbstractMap.SimpleEntry<Double, KoalaNeighbor>> potentialDests = new ArrayList<AbstractMap.SimpleEntry<Double, KoalaNeighbor>>(); 
	
		for(KoalaNeighbor re : rt){
			v = getRouteValue(dest.getSID(), re, alpha);
			mre = new AbstractMap.SimpleEntry<Double, KoalaNeighbor>(v, re);
			if(v>0) // add only those better than myself
				potentialDests.add(mre);
	
		}
		Collections.sort(potentialDests, Collections.reverseOrder(new Comparator<AbstractMap.SimpleEntry<Double, KoalaNeighbor>>() {
			@Override
			public int compare(AbstractMap.SimpleEntry<Double, KoalaNeighbor> o1, AbstractMap.SimpleEntry<Double, KoalaNeighbor> o2) {
				return o1.getKey().compareTo(o2.getKey());
				
			}
			}));
		
		KoalaNeighbor ret = null; 
		ArrayList<KoalaNeighbor> downEntries = new ArrayList<KoalaNeighbor>();
		for(AbstractMap.SimpleEntry<Double, KoalaNeighbor> entry : potentialDests){
			KoalaNeighbor rentry = entry.getValue();
			boolean isDown = !NodeUtilities.isUp(rentry.getCID()); 
			if(isDown)
				downEntries.add(rentry);
			if(rentry.getSID().equals(dest) && isDown)
				break;
			if(msg.getPath().contains(rentry) || 
					destNeigs.contains(rentry.getSID()) ||
					isDown)
				continue;
			
			ret = rentry;
			break;
		}
		
		for(KoalaNeighbor down : downEntries){
			down.reset();
		}
		
		
		
		return ret;
    }

   


	private double getRouteValue(String dest, KoalaNeighbor re, double alpha) {
 		double res = 0;
 		double max = Double.MAX_VALUE;
 		max = 100000;
 
 		if(NodeUtilities.distanceGlobal(this.getSID(), dest) < NodeUtilities.distanceGlobal(re.getSID(), dest))
             res = -1;
 		
 		else if( NodeUtilities.getDCID(dest) == NodeUtilities.getDCID(re.getSID()))
             res = max - (double)NodeUtilities.A * (double)NodeUtilities.distance(re.getSID(), dest);
         
 		else if( NodeUtilities.getDCID(getSID()) == NodeUtilities.getDCID(re.getSID()))
             res = NodeUtilities.A * NodeUtilities.distance(this.getSID(), re.getSID());
         
 		else if( NodeUtilities.distance(this.getSID(), dest) > NodeUtilities.distance(re.getSID(), dest)){
             int tot_distance = NodeUtilities.distance(this.getSID(), dest);
             int rem_distance = NodeUtilities.distance(dest, re.getSID());
             
             double norm_dist =  1 - (double)rem_distance/tot_distance;
             double norm_latency = NodeUtilities.normalizeLatency(tot_distance, re.getLatency());
             res = 1 + alpha * norm_dist + (1-alpha) * norm_latency;
             
             if(alpha == -1)
             	res = 1+CommonState.r.nextInt(100);
         }
         
 		
         
 		return res;
 	}
	
	public KoalaNeighbor getClosestEntryAfter(String dest){
		int sdistfromme = NodeUtilities.signDistance(this.getSID(), dest);
		int minDist = Integer.MAX_VALUE;
		KoalaNeighbor closestEntry = null;
		for( KoalaNeighbor re: getRoutingTable().getNeighbors()){
			int sdistfromre = NodeUtilities.signDistance(this.getSID(), re.getSID());
			if(sdistfromme*sdistfromre < 0) continue;
			int dist = NodeUtilities.distance(re.getSID(), dest);
			if(!NodeUtilities.isUp(re.getSID())){re.reset(); continue;}
			if(dist < minDist){
				minDist = dist;
				closestEntry = re;
			}
		}
		
		return closestEntry;
	}
	
	public KoalaNeighbor getClosestEntryBefore(String dest){
		int sdistfromme = NodeUtilities.signDistance(this.getSID(), dest);
		int distfromme = NodeUtilities.distance(this.getSID(), dest);
		int minDist = Integer.MAX_VALUE;
		KoalaNeighbor closestEntry = null;
		for( KoalaNeighbor re: getRoutingTable().getNeighbors()){
			
			int sdistfromre = NodeUtilities.signDistance(this.getSID(), re.getSID());
			int distfromre = NodeUtilities.distance(this.getSID(), re.getSID());
			if(sdistfromme*sdistfromre < 0) continue;
			if(distfromre > distfromme ) continue;
			int dist = NodeUtilities.distance(re.getSID(), dest);
			if(dist==0) continue;
			if(!NodeUtilities.isUp(re.getSID())){ re.reset(); continue;}
			
			if(dist < minDist){
				minDist = dist;
				closestEntry = re;
			}
		}
		
		return closestEntry;
	}
	
	
//	public Set<String> createRandomIDs(int nr){
//		Set<String> rids = new HashSet<String>();
//		
//		if (getRoutingTable().hasAllDefaultLocals())
//			nr = 0;
//		else if(getRoutingTable().getLocalSucessor(0).equals(getRoutingTable().getLocalPredecessor(0)))
//			nr = 1;
//		
//        while( rids.size() < nr){
//            String rand_id = this.dcID + "-" + CommonState.r.nextInt(NodeUtilities.NR_NODE_PER_DC);
//            if( !this.isResponsible(rand_id))
//                rids.add(rand_id);
//            
//        }
//        return rids;
//		return null;
//	}



//	public boolean isResponsible(String id) {
//		if(id.equals(getSID()))
//			return true;
//		if (getRoutingTable().hasAllDefaultLocals())
//            return false;
//        return NodeUtilities.distance(getSID(), id) < NodeUtilities.distance(getRoutingTable().getLocalSucessor(0).getNodeID(), id) 
//                && NodeUtilities.distance(getSID(), id) < NodeUtilities.distance(getRoutingTable().getLocalPredecessor(0).getNodeID(), id);
//		
//	}

	public KoalaNeighbor getResponsibleLocalNeighbor(String globalN){
		int min = Integer.MAX_VALUE;
		KoalaNeighbor minLocalNeighbor = KoalaNeighbor.getDefaultNeighbor();
		for(KoalaNeighbor ln : this.routingTable.getLocals()){
			int dist = NodeUtilities.distanceLocal(ln.getSID(), globalN);
			if(dist < min)
				minLocalNeighbor = ln;
		}
		return minLocalNeighbor;
	}
	
	public boolean inNeighborsList(String id){
		Set<String> ids = isLocal(id) ?  getRoutingTable().getNeighboursIDs(1):
										 getRoutingTable().getNeighboursIDs(2); 
		return ids.contains(id);
	}
	
//	public static class KoalaNodeSerializer implements JsonSerializer<KoalaNode> {
//
//		@Override
//		public JsonElement serialize(KoalaNode src, Type typeOfSrc, JsonSerializationContext context) {
//			JsonArray neighbors = new JsonArray();
//			ArrayList<KoalaNeighbor> neigs = src.getRoutingTable().getNeighbors();
//			for(KoalaNeighbor neig : neigs){
//				neighbors.add(KoalaJsonParser.toJsonTree(neig));
//			}
//			
//			JsonArray oldNeighbors = new JsonArray();
//			ArrayList<KoalaNeighbor> oldNeigs = src.getRoutingTable().getOldNeighborsContainer();
//			for(KoalaNeighbor oldNeig : oldNeigs){
//				oldNeighbors.add(KoalaJsonParser.toJsonTree(oldNeig));
//			}
//			
//			JsonObject obj = new JsonObject();
//			obj.addProperty("id", src.getSID());
//			obj.addProperty("actid", src.getActualID());
//			obj.addProperty("joining", src.isJoining());
//			
//			obj.add("neighbors", (JsonElement)neighbors);
//			obj.add("oldNeighbors", (JsonElement)oldNeighbors);
//			return obj;
//		}
//		
//	}
//	
//	public static class KoalaNodeDeserializer implements JsonDeserializer<KoalaNode> {
//		private KoalaNode sample;
//		public KoalaNodeDeserializer(KoalaNode sample){
//			this.sample = sample;
//		}
//		
//		@Override
//		public KoalaNode deserialize(JsonElement src, Type typeOfSrc, JsonDeserializationContext context) throws JsonParseException {
//			JsonObject srcJO = src.getAsJsonObject();
//			KoalaNode kn = (KoalaNode) sample.clone();
//			kn.setID(srcJO.get("id").getAsString());
//			//TODO: check whats happening with actid here 
//			kn.setJoining(srcJO.get("joining").getAsBoolean());
//		
//			JsonArray neigs = srcJO.getAsJsonArray("neighbors");
//			ArrayList<KoalaNeighbor> neighbors = new ArrayList<KoalaNeighbor>();
//			for(JsonElement neig : neigs)
//				neighbors.add(KoalaJsonParser.jsonTreeToObject(neig, KoalaNeighbor.class));
//			
//			JsonArray oldNeigs = srcJO.getAsJsonArray("oldNeighbors");
//			ArrayList<KoalaNeighbor> oldNeighbors = new ArrayList<KoalaNeighbor>();
//			for(JsonElement neig : oldNeigs)
//				oldNeighbors.add(KoalaJsonParser.jsonTreeToObject(neig, KoalaNeighbor.class));
//			
//			kn.getRoutingTable().setNeighborsContainer(neighbors);
//			kn.getRoutingTable().setOldNeighborsContainer(oldNeighbors); 
//			return kn;
//		}
//		
//	}

}
