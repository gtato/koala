package topology.controllers;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import koala.KoalaNode;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.reports.GraphObserver;
import utilities.NodeUtilities;

public abstract class NodeObserver extends GraphObserver {
//    private static final String PAR_FILENAME_BASE = "file_base";
//    private static final String PAR_NR_FILES= "nr_files";

//    private final String graph_filename;
//    private final FileNameGenerator fng;
//    private final int nrFiles;
//    protected String plotScript;
    boolean dumpToStd = false;
    String fileCounter = "";
    
	protected NodeObserver(String prefix) {
		super(prefix);
//		pid = Configuration.getPid(prefix + "." + PAR_PROT);
//		nrFiles = Configuration.getInt(prefix + "."+ PAR_NR_FILES, 1);
//		graph_filename = Configuration.getString(prefix + "."+ PAR_FILENAME_BASE, "graph_dump");
//		if(graph_filename.equals("graph_dump"))
//        	dumpToStd = true;
//		fng = new FileNameGenerator(graph_filename, ".dat");
		int exp = Configuration.getInt("EXP", -1);
		double alpha = Configuration.getDouble("ALPHA", -2.0);
		if(exp >= 0 && alpha >= -1)
			fileCounter = "E"+exp+"A"+alpha;
		else if(exp >= 0)
			fileCounter = "E"+exp;
		else if(alpha >= -1)
			fileCounter = "A"+alpha;
		
//		fileCounter += System.currentTimeMillis() ;
	}

	protected void graphToFile() {
		
		try {
			File file;
			String[] filenames = getOutputFileNames();		
			for(int i= 0; i < filenames.length; i++){
				//file = new File(getOutputFileBase()+ filenames[i]);
				file = new File(getOutputFileBase()+ filenames[i]+ fileCounter+".dat");
				
				file.getParentFile().mkdirs();
				file.createNewFile();
				FileOutputStream fos = new FileOutputStream(file);
	            PrintStream ps = dumpToStd ? System.out : new PrintStream(fos);
	
	            printGraph(ps, i);
	            
	            fos.close();
	            ps.close();
			}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
	protected PrintStream[] openFiles(){
		PrintStream[] pss = null;
		try {
			File file;
			String[] filenames = getOutputFileNames();
			pss = new PrintStream[filenames.length]; 
			
			for(int i= 0; i < filenames.length; i++){
				file = new File(getOutputFileBase()+ filenames[i]+ fileCounter+".dat");
				file.getParentFile().mkdirs();
				file.createNewFile();
				FileOutputStream fos = new FileOutputStream(file, true);
	            pss[i] = new PrintStream(fos);
	            
	            
			}
			
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
		
		return pss;
	} 
	
	protected void deleteFiles(){
		File file;
		String[] filenames = getOutputFileNames();
		 
		for(int i= 0; i < filenames.length; i++){
			file = new File(getOutputFileBase()+ filenames[i]+ fileCounter+".dat");
			if (file.exists())
				file.delete();
		}
		
	} 
	
	
	protected void closeFiles(PrintStream[] pss){
		for(PrintStream ps : pss)
			ps.close();
	} 
	
//	protected void plotIt(){
//		try {
////			new ProcessBuilder("gnuplot", "-persistent", plotScript).start();
//			new ProcessBuilder("gnuplot", plotScript).start();
////			Process p = new ProcessBuilder("gnuplot", "-persistent", plotScript).start();
////			BufferedReader reader = new BufferedReader (new InputStreamReader(p.getErrorStream()));
////			String line;
////			while ((line = reader.readLine ()) != null) {
////				System.out.println ("Stdout: " + line);
////			}
//
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
//	protected KoalaNode getNodeFromID(String id)
//	{
//		for (int i = 0; i < g.size(); i++) 
//		{
//			KoalaNode current = (KoalaNode) ((Node)g.getNode(i)).getProtocol(NodeUtilities.KID);
//			if(current.getID().equals(id))
//				return current;
//		}
//		return null;
//	}

	protected abstract void printGraph(PrintStream ps, int psIndex);
	
	protected String getOutputFileBase(){return "out/";}
	protected String[] getOutputFileNames(){return new String[0];}
	
	
}
