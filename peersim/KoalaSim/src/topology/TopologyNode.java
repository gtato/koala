package topology;

import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;
import utilities.NodeUtilities;

import java.util.ArrayList;

import example.hot.InetCoordinates;

public class TopologyNode extends InetCoordinates implements Protocol, Linkable{

	private String commonID;
	private String specificID;
//	private int pid;
	private long birthday;
	protected boolean joined;
	private Node node;
	boolean visited;
	private ArrayList<Double> vivaldiCoordinates;
	private double vivaldiUncertainty;
	
	
	public int getDCID(){
		return NodeUtilities.getDCID(specificID);
	}
	
	public int getNodeID(){
		return NodeUtilities.getNodeID(specificID);
	}
	
	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public long getBirthday() {
		return birthday;
	}

	public void setBirthday(long birthday) {
		this.birthday = birthday;
	}

	public boolean hasJoined() {
		return joined;
	}

	public void setJoined(boolean joined) {
		this.joined = joined;
		if(joined)
			this.setBirthday(CommonState.getTime());
	}
	
	public ArrayList<Double> getVivaldiCoordinates() {
		return vivaldiCoordinates;
	}

	public void setVivaldiCoordinates(ArrayList<Double> vivaldi_coordinates) {
		this.vivaldiCoordinates = vivaldi_coordinates;
	}

	public double getVivaldiUncertainty() {
		return vivaldiUncertainty;
	}

	public void setVivaldiUncertainty(double vivaldi_uncertainty) {
		this.vivaldiUncertainty = vivaldi_uncertainty;
	}
	
	public void resetVivaldiCoords() {
		vivaldiUncertainty = 1000;
		vivaldiCoordinates = new ArrayList<Double>();
		for(int i =0;i < NodeUtilities.VIV_DIMENSIONS;i++)
			vivaldiCoordinates.add(0.0);
	}
	
	public void reset(){
		setCID(null);
		setSID(null);
		setJoined(false);
		resetVivaldiCoords();
	}
	
	public long getAge(){
		if(!joined)
			return -1;
		return CommonState.getTime() - getBirthday();
	}

	
	public TopologyNode(String prefix) {
		super(prefix);
		reset();
	}
	
	public TopologyNode(String prefix, String cid, String sid) {
		super(prefix);
		reset();
		commonID = cid;
		specificID = sid;
	}
	
	public TopologyNode(String prefix, String cid, String sid, ArrayList<Double> coords, double uncertainty) {
		super(prefix);
		reset();
		commonID = cid;
		specificID = sid;
		vivaldiCoordinates = coords;
		vivaldiUncertainty = uncertainty;
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
	
	@Override
	public void onKill() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean addNeighbor(Node neighbour) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Node neighbor) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int degree() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Node getNeighbor(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void pack() {
		// TODO Auto-generated method stub
		
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}
	
	public boolean isUp(){
		return this.node.isUp();
	}
	
	public long getSimNodeID(){
		return this.node.getID();
	}

//	public int getPid() {
//		return pid;
//	}
//
//	public void setPid(int pid) {
//		this.pid = pid;
//	}
}
