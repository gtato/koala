package topology;

import koala.KoalaNeighbor;
import koala.KoalaNode;

public class TopologyPathNode{
	private String commonID;
	private String specificID;
	
	public TopologyPathNode(String commonID, String specificID) {
		super();
		this.commonID = commonID;
		this.specificID = specificID;
	}
	
	public TopologyPathNode(String commonID) {
		super();
		this.commonID = commonID;
		this.specificID = commonID;
	}
	
	public TopologyPathNode(TopologyNode tn) {
		super();
		this.commonID = tn.getCID();
		this.specificID = tn.getSID();
	}
	
	
	public String getCID() {
		return commonID;
	}
	
	public void setCID(String commonID) {
		this.commonID = commonID;
	}
	
	public String getSID() {
		return specificID;
	}
	
	public void setSID(String specificID) {
		this.specificID = specificID;
	}
	
	
	public String toString(){
		return specificID;
	}
	
	public TopologyPathNode clone(){
		return new TopologyPathNode(commonID, specificID);
		
	}
	
	public boolean equals(Object n){
		if (KoalaNode.class.isInstance(n))
			return this.equals((KoalaNode)n);
		if (TopologyPathNode.class.isInstance(n))
			return this.equals((TopologyPathNode)n);
		return false;
	}
	
	public boolean equals(TopologyPathNode n){
		return this.getSID().equals(n.getSID());
	}
	
	public boolean equals(KoalaNode n){
		return this.getSID().equals(n.getSID());
	}
}
