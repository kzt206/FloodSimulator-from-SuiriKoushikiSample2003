import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FileGeo2dRead {
	private int IMAX = 0;
	private int JMAX = 0;
	private double DX, DY;
	private char[][] IP;
	private double[][] ZB;
	private double[][] RN;
	
	ArrayList<ArrayList<FloodCell>> arraysFCell = new ArrayList<ArrayList<FloodCell>>();
	
	public FileGeo2dRead() {
		try {
			File file = new File("GEO2D.DAT");
			BufferedReader br = new BufferedReader(new FileReader(file));
			if (file.exists()) {

				// Read file.
				String dataString[];
//			}
				// line 1.
				dataString = br.readLine().split("\\s+");
				IMAX = Integer.parseInt(dataString[1]);
				JMAX = Integer.parseInt(dataString[2]);
				DX = Double.parseDouble(dataString[3]);
				DY = Double.parseDouble(dataString[4]);
				
//				System.out.printf("IMAX:%d, JMAX:%d, DX:%f, DY:%f\n", IMAX, JMAX, DX, DY);
				
				for(int j = 0;j<JMAX;j++) {
					ArrayList<FloodCell> array = new ArrayList<>();
					for(int i = 0;i<IMAX;i++) {
						FloodCell fcell = new FloodCell();
						fcell.setX(i);
						fcell.setY(j);
						array.add(fcell);
					}
					arraysFCell.add(array);
				}
//			
				// line 2. Dummy
				br.readLine();

				// line 3-50 (JMAX 48 lines) IP information
				for (int j = 0; j < JMAX; j++) {
					String[] lines = br.readLine().split("\\s+");
//					System.out.println(br.readLine());
//					System.out.println(lines[1]);  // 0:blank 1:Y(j)
//					System.out.println(lines[2]);  // 2:IP X(i) 36
					for(int i = 0;i<lines[2].length();i++) {
						arraysFCell.get(j).get(i).setIp(lines[2].substring(i,i+1).toCharArray()[0]);
					}
				}

				// line 51. Dummy
				br.readLine();

				// line 52-291 (JMAX 48 * 5 lines) ZB information
				for (int j = 0; j < JMAX; j++) {
					int i = 0;
					for (int k = 0; k < 5; k++) {
						if(k == 0) {
							String[] lines = br.readLine().split("\\s+");
							for(int l =0 ; l< 8;l++) {
								arraysFCell.get(j).get(i).setZb(Double.parseDouble(lines[l+2]));
								i++;
							}
						}else if(k == 4) {
							String[] lines = br.readLine().split("\\s+");
							for(int l =0 ; l< 4;l++) {
								arraysFCell.get(j).get(i).setZb(Double.parseDouble(lines[l+1]));
								i++;
							}
						}else {
							String[] lines = br.readLine().split("\\s+");
							for(int l =0 ; l< 8;l++) {
								arraysFCell.get(j).get(i).setZb(Double.parseDouble(lines[l+1]));
								i++;
							}
						}
					
					}
					
				}

				// line 292. Dummy
				br.readLine();

				// line 293-532 (JMAX 48 * 5 lines) RN information
				for (int j = 0; j < JMAX; j++) {
					int i = 0;
					for (int k = 0; k < 5; k++) {
						if(k == 0) {
							String[] lines = br.readLine().split("\\s+");
							for(int l =0 ; l< 8;l++) {
								arraysFCell.get(j).get(i).setRn(Double.parseDouble(lines[l+2]));
								i++;
							}
						}else if(k == 4) {
							String[] lines = br.readLine().split("\\s+");
							for(int l =0 ; l< 4;l++) {
								arraysFCell.get(j).get(i).setRn(Double.parseDouble(lines[l+1]));
								i++;
							}
						}else {
							String[] lines = br.readLine().split("\\s+");
							for(int l =0 ; l< 8;l++) {
								arraysFCell.get(j).get(i).setRn(Double.parseDouble(lines[l+1]));
								i++;
							}
						}
					
					}
					
				}

				// Close file.
				br.close();
				
				// make IP,ZB,RN arrays
				IP = new char[IMAX][JMAX];
				ZB = new double[IMAX][JMAX];
				RN = new double[IMAX][JMAX];
				
				for(int i = 0; i<IMAX;i++) {
					for(int j= 0 ;j<JMAX;j++) {
						IP[i][j] = arraysFCell.get(j).get(i).getIp();
						ZB[i][j] = arraysFCell.get(j).get(i).getZb();
						RN[i][j] = arraysFCell.get(j).get(i).getRn();
					}
				}
				
			} else {
				System.out.println("File does not exist.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

//		return arraysFCell;
	}

	
	public char[][] getIP(){
		return IP;
	}
	
	public double[][] getZB(){
		return ZB;
	}
	
	public double[][] getRN(){
		return RN;
	}
	public int getIMAX() {
		return this.IMAX;
	}
	
	public int getJMAX() {
		return this.JMAX;
	}
		
	
	
}
