import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoublePredicate;

public class FileFloodRead {

	private int NHT;
	private double TRLX;
	private List<Double> floodQuantity = new ArrayList<>(); 
	
	public int getNHT() {
		return this.NHT;
	}
	
	public double getTRLX() {
		return this.TRLX;
	}
	
	public double[] getFloodQ() {
		int length = floodQuantity.size();
		double[] floodQ = new double[length];
		for(int i = 0 ;i < length ; i++) {
			floodQ[i] = floodQuantity.get(i);
		}
		return floodQ;
	}
	
	public FileFloodRead() {
		
		
		try {
			File file = new File("FLOOD.DAT");
			BufferedReader br = new BufferedReader(new FileReader(file));
			if (file.exists()) {
				String[] line;
				// Read file.
//				while ((line = br.readLine()) != null) {
//
//					System.out.println(line);
//				}
				// line 1 : Dummy
				br.readLine();
				
				// line 2 : NHT,TRLX
				line = br.readLine().split("\\s+");
				NHT = Integer.parseInt(line[1]);
				TRLX = Double.parseDouble(line[2]);
				
				// line 3 : data 1
				line = br.readLine().split("\\s+");
				for(int i = 1 ; i<line.length ; i++) {

					floodQuantity.add(Double.parseDouble(line[i]));
				}
				
				// line 4 : data 2
				line = br.readLine().split("\\s+");
				for(int i = 1 ; i<line.length ; i++) {
					floodQuantity.add(Double.parseDouble(line[i]));
				}
			} else {
				System.out.println("File does not exist.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
