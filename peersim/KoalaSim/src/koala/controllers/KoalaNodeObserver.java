package koala.controllers;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.ListSelectionEvent;

import koala.KoalaNode;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import topology.controllers.NodeObserver;
import utilities.NodeUtilities;


public class KoalaNodeObserver extends NodeObserver {

    
    ArrayList<String> rtReport = new ArrayList<String>();
    boolean ended = false;
	public KoalaNodeObserver(String prefix) {
		super(prefix);
	}

	

	
	@Override
	public boolean execute() {
		updateGraph();
		rtReport();
		if(CommonState.getTime() == CommonState.getEndTime()-1 && !ended){
			graphToFile();
			countRoutedMsgs();
			ended = true;
		}
		
		return false;
	}

	
	private void countRoutedMsgs() {
		int totalMsgRouted = 0;
		int totalMsgRoutedbyLatency = 0;
		for (int i = 0; i < g.size(); i++) 
		{	
			KoalaNode current = (KoalaNode) ((Node)g.getNode(i)).getProtocol(pid);
			totalMsgRouted += current.nrMsgRouted;
			totalMsgRoutedbyLatency += current.nrMsgRoutedByLatency;
		}
		System.out.println("total routed: " + totalMsgRouted + ", based on latency: "+ totalMsgRoutedbyLatency+ " ratio: " + (double)totalMsgRoutedbyLatency/totalMsgRouted );
	}




	private void rtReport() {
		
		int size = 0;
		ArrayList<Integer> sizes = new ArrayList<Integer>();

		for (int i = 0; i < g.size(); i++) 
		{	
			KoalaNode current = (KoalaNode) ((Node)g.getNode(i)).getProtocol(pid);
			size += current.getRoutingTable().getSize();
			sizes.add(current.getRoutingTable().getSize());
		}
		
		Collections.sort(sizes);
		
		double mean = (double) size/g.size();
		double sum=0;
		for (int i = 0; i < g.size(); i++) 
		{	
			KoalaNode current = (KoalaNode) ((Node)g.getNode(i)).getProtocol(pid);
			sum += Math.pow(current.getRoutingTable().getSize() - mean, 2);
		}
		
		double std = Math.sqrt(sum/g.size());
		int q1 = sizes.get(sizes.size()/4);
		int q3 = sizes.get(sizes.size()/2 + sizes.size()/4);
		int median = sizes.get(sizes.size()/2);
		int min = sizes.get(0);
		int max = sizes.get(sizes.size()-1);
		
		rtReport.add(mean +" " + std + " " + min + " "  + q1 + " " + median + " " + q3 + " " + max  );
		
	}

//	private void simpleReport() {
//		ArrayList<String> withoutLocalNeighs = new ArrayList<String>();
//		ArrayList<String> withOneLocalNeigh = new ArrayList<String>();
//		ArrayList<String> withTwoLocalNeigh = new ArrayList<String>();
//		ArrayList<String> unknown = new ArrayList<String>();
//		for (int i = 0; i < g.size(); i++) 
//		{
//			boolean neighborToSomeone=false;
//			KoalaNode current = (KoalaNode) ((Node)g.getNode(i)).getProtocol(pid);
//			if(!current.hasJoined()) continue;
//			String log = "ID: " + current.getID() + ", bootstrap: " + current.getBootstrapID();
//			log += ", neighbours: ";
//			
//			Set<String> neigs = current.getRoutingTable().getNeighboursIDs(); 
//			
//			int ln=0;
//			for(String n : neigs ){
//				if(NodeUtilities.sameDC(current.getID(), n))
//					ln++;
//				log += "\t" + n;
//			}
//			if(ln == 0)
//				withoutLocalNeighs.add(current.getID());
//			else if (ln == 1)
//				withOneLocalNeigh.add(current.getID());
//			else if (ln == 2)
//				withTwoLocalNeigh.add(current.getID());
//			
//			System.out.println(log);
//			
//			
//			for (int j = 0; j < g.size(); j++) 
//			{
//				KoalaNode kn = (KoalaNode) ((Node)g.getNode(j)).getProtocol(pid);
//				if (i == j || !kn.hasJoined()) continue;
//				if (kn.getRoutingTable().getNeighboursIDs().contains(current.getID()))
//					neighborToSomeone = true;
//			}
//			
//			if(!neighborToSomeone)
//				unknown.add(current.getID());
//			
//		}
//		
//		System.out.println("Without local neighbors: ("+ withoutLocalNeighs.size()+ ") "  +withoutLocalNeighs +
//			           "\nWith one local neighbor: ("+ withOneLocalNeigh.size()+ ") " +withOneLocalNeigh +
//			           "\nWith two local neighbors: ("+ withTwoLocalNeigh.size()+ ") " +withTwoLocalNeigh +
//			           "\nUnknown: (" +unknown.size() +  ") "+unknown           
//			);
//	}
//	
	


	@Override
	protected void printGraph(PrintStream ps, int psIndex) {
		if (psIndex != 0)
			return;
		for(String line : rtReport)
			ps.println(line);
            
    }
	
	@Override
	protected String getOutputFileBase() {
		return super.getOutputFileBase() +  "koala/";
	}

	@Override
	protected String[] getOutputFileNames() {
		return new String[]{"rt"};
	}
	
		
	
}
