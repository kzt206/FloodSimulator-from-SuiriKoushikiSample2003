import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EventListenerProxy;
import java.util.concurrent.Callable;
import java.util.zip.Inflater;

import javax.swing.border.MatteBorder;
import javax.xml.parsers.DocumentBuilder;

public class Main {

	// Global variables for qbreak()
	static int NHT; //計算の対象時間数　１３時間
	static double TRLX;  //流量の急増を抑える緩和時間　３００秒
	static double[] QHYD; //氾濫流量１時間毎の値

	// Global variables for indflw()
	static int IMAX, JMAX; //計算の対象範囲 x方向 IMAX=36, y方向 JMAX=48
	static double DT, DX, DY, G, EPS, DT2DX, DT2DY, DTGDX, DTGDY, DT2, DXDY;
	static int IBR, JBR, IBRD, JBRD; // Break point, flood direction
	static double QBR; //氾濫流量 qbreake()より求める

	static char[][] IP;  //差分格子の座標
	static double[][] ZB; // 地盤高
	static double[][] RN; // マニングの粗度係数

	static double[][] SMO; // x direction flux at i,j+1/2 in t=n*DT
	static double[][] SNO; // y direction flux at i+1/2,j in t=n*DT
	static double[][] HO; // water depth
	static double[][] ZS; // water height
	static double[][] SMN; // x direction flux at i,j+1/2 in t=(n+2)*DT
	static double[][] SNN; // y direction flux at i+1/2,j in t=(n+2)*DT
	static double[][] HN;  // 座標i+1/2,j+1/2の時間t=(n+3)*DTの水深
	static double[][] SMXCV; // x direction flux at i,j+1/2 for convection term calculation
	static double[][] SNYCV; // y direction flux at i+1/2,j for convection term calculation
	static double[][] HCV;          // water depth at i+1/2,j+1/2 for convection term calclulation
	static double[][] CUM;  // 移流項 convx(x)
	static double[][] CVM;  // 移流項 convx(y)
	static double[][] CUN;  // 移流項 convy(x)
	static double[][] CVN;  // 移流項 convy(y)
	static char[][] IFROF;  // 段落ち計算をしたかどうか Y or N
	static char[][] JFROF;  // 段落ち計算をしたかどうか Y or N
	static double[][] RNGX;  // 抵抗項に関する定数
	static double[][] RNGY;  // 抵抗項に関する定数

	public static void main(String... args) {

		// NPRINT : INTERVAL for PRINT OUTPUT
		// NFILE : INTERVAL for FILE OUTPUT
		// NFILE must be integer times of NPRINT
		int NPRINT = 180;
		int NFILE = 360;

		start(); //入力ファイルの読み込み、変数の初期化
		
		PrintWriter pHOWriter=null,pSMOWriter=null,pSNOWriter=null;

		double NFINAL = (NHT - 1) * 3600. / DT2; // original
		// double NFINAL = (NHT)*3600./DT2;

		// int NSTEP = 0;
		// double TIME = NSTEP*DT2;

		// OUTPUTFILE
		//
		// coding later

		// Check of water volume
		double VIN = 0.0;
		double S0 = 0.0;
		// GOTO 400
		// int N1 = NSTEP/NPRINT;
		for (int NSTEP = 0; NSTEP < NFINAL + 1; NSTEP++) {
			double TIME = NSTEP * DT2;
			QBR = qbreak(TIME, QBR); //氾濫流量の計算
			System.out.println("NSTEP: " + NSTEP);
			System.out.println("QBR: " + QBR);

			indflw(NSTEP, TIME); //氾濫流の計算

			VIN = VIN + QBR * DT2;

			// variables for convective term computation
			for (int i = 0; i < IMAX; i++) {
				for (int j = 0; j < JMAX; j++) {
					HCV[i][j] = (HO[i][j] + HN[i][j]) * 0.5;
					SMXCV[i][j] = (SMO[i][j] + SMN[i][j]) * 0.5;
					SNYCV[i][j] = (SNO[i][j] + SNN[i][j]) * 0.5;
				}
			}
			if (NSTEP % 720 == 0) { // 720 Steps = 3600sec = 1hour
				try {
					String fileNameHO = "HO/HO_" + String.format("%04d", NSTEP) + ".txt";
					File fileHO = new File(fileNameHO);
					pHOWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileHO)));

					String fileNameSMO = "SMO/SMO_" + String.format("%04d", NSTEP) + ".txt";
					File fileSMO = new File(fileNameSMO);
					pSMOWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileSMO)));

					String fileNameSNO = "SNO/SNO_" + String.format("%04d", NSTEP) + ".txt";
					File fileSNO = new File(fileNameSNO);
					pSNOWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileSNO)));

					// replacement of variables for next step
					for (int i = 0; i < IMAX; i++) {
						for (int j = 0; j < JMAX; j++) {
							SMO[i][j] = SMN[i][j];
							SNO[i][j] = SNN[i][j];
							HO[i][j] = HN[i][j];
							ZS[i][j] = ZB[i][j] + HO[i][j];

							pHOWriter.printf("%8.4f  ", HO[i][j]);
							pSMOWriter.printf("%8.4f  ", SMO[i][j]);
							pSNOWriter.printf("%8.4f  ", SNO[i][j]);
							// System.out.println("i:" + i + " j:" + j + " HO:" + HO[i][j]);
						}
						pHOWriter.println();
						pSMOWriter.println();
						pSNOWriter.println();
					}

//					pHOWriter.close();
//					pSMOWriter.close();
//					pSNOWriter.close();

				} catch (IOException e) {
					e.printStackTrace();
				}finally {
					if(pHOWriter != null) {
						try {
							pHOWriter.close();
						}catch (Exception e2) {}	
					}
					if(pSMOWriter != null) {
						try {
							pSMOWriter.close();
						}catch (Exception e2) {}			
					}
					if(pSNOWriter != null) {
						try {
							pSNOWriter.close();
						}catch (Exception e2) {}			
					}
				}

			}

		}

	}

	//入力ファイルの読み込み、変数の初期化//入力ファイルの読み込み、変数の初期化
	static void start() {
		// BEGIN of Subroutine START
		// Read GEO2D.DAT
		FileGeo2dRead fGeo2dRead = new FileGeo2dRead();
		IMAX = fGeo2dRead.getIMAX();
		JMAX = fGeo2dRead.getJMAX();

		IP = fGeo2dRead.getIP();
		ZB = fGeo2dRead.getZB();
		RN = fGeo2dRead.getRN();

		// System.out.println("Main java2:"+IP[18][10]+" "+ZB[18][10]+" "+RN[18][10]);
		// System.out.printf("Main.java :IMAX:%d, JMAX:%d\n", IMAX, JMAX);

		// Data block
		DX = 285.44; //x方向の格子幅
		DY = 231.0;  //y方向の格子幅
		G = 9.8;    // 重力値
		EPS = 0.001;  //各格子の水深限度
		DT = 2.5;  //計算のステップ 2.5秒
		IBR = 29 - 1; //破堤箇所
		JBR = 42 - 1; //破堤箇所
		IBRD = -1;    //流入流量の方向 x方向
		JBRD = 0;     //流入流量の方向 y方向
		QBR = 0.0;

		// Set Break Point
		// Change of IP for LEVEE-Break Point
		IP[IBR][JBR] = 'B'; // 破堤箇所の設定

		// Read FLOOD.DAT
		FileFloodRead ffRead = new FileFloodRead();  //FLOOD.DATの読み込み
		NHT = ffRead.getNHT(); //１時間ごとの氾濫流量
		// System.out.println("NHT: " + NHT);
		TRLX = ffRead.getTRLX(); //急激な氾濫流量の増加の緩和時間
		QHYD = ffRead.getFloodQ(); //氾濫流量の１時間毎の値

		// Initialization of Variables
		SMO = new double[IMAX][JMAX];
		SNO = new double[IMAX][JMAX];
		HO = new double[IMAX][JMAX];
		ZS = new double[IMAX][JMAX];
		SMN = new double[IMAX][JMAX];
		SNN = new double[IMAX][JMAX];
		HN = new double[IMAX][JMAX];
		SMXCV = new double[IMAX][JMAX];
		SNYCV = new double[IMAX][JMAX];
		HCV = new double[IMAX][JMAX];
		CUM = new double[IMAX][JMAX];
		CVM = new double[IMAX][JMAX];
		CUN = new double[IMAX][JMAX];
		CVN = new double[IMAX][JMAX];
		IFROF = new char[IMAX][JMAX];
		JFROF = new char[IMAX][JMAX];
		RNGX = new double[IMAX][JMAX];
		RNGY = new double[IMAX][JMAX];
		for (int i = 0; i < IMAX; i++) {
			for (int j = 0; j < JMAX; j++) {
				SMO[i][j] = 0.0;
				SNO[i][j] = 0.0;
				HO[i][j] = 0.0;
				ZS[i][j] = 0.0;
				SMN[i][j] = 0.0;
				SNN[i][j] = 0.0;
				HN[i][j] = 0.0;
				SMXCV[i][j] = 0.0;
				SNYCV[i][j] = 0.0;
				HCV[i][j] = 0.0;
				CUM[i][j] = 0.0;
				CVM[i][j] = 0.0;
				CUN[i][j] = 0.0;
				CVN[i][j] = 0.0;
				IFROF[i][j] = 'N';
				JFROF[i][j] = 'N';
				if (i != 0) {
					if (IP[i - 1][j] == 'I') {
						RNGX[i][j] = Math.pow(((RN[i][j] + RN[i - 1][j]) / 2.0), 2.) * G * DT;
					} else {
						RNGX[i][j] = Math.pow((RN[i][j]), 2.) * G * DT;
					}
				} else {
					RNGX[i][j] = Math.pow((RN[i][j]), 2.) * G * DT;
				}
				if (j != 0) {
					if (IP[i][j - 1] == 'I') {
						RNGY[i][j] = Math.pow((RN[i][j] + RN[i][j - 1] / 2.0), 2.) * G * DT;
					} else {
						RNGY[i][j] = Math.pow(RN[i][j], 2.) * G * DT;
					}
				} else {
					RNGY[i][j] = Math.pow(RN[i][j], 2.) * G * DT;
				}
			}
		}
		// Constants
		DT2 = DT * 2.0;
		DT2DX = DT * 2.0 / DX;
		DT2DY = DT * 2.0 / DY;
		DTGDX = DT * G / DX * 2.0;
		DTGDY = DT * G / DY * 2.0;
		DXDY = DX * DY;
		// END of Subroutine START
	}

	//氾濫流の計算
	static double qbreak(double TIME, double QBR) {
		// System.out.println("Start qbreak().");
		// for(double d : QHYD) {
		// System.out.println(d);
		// }
		// System.out.println("End qbreak()");
		if (TIME < TRLX) {
			QBR = QHYD[0] / TRLX * TIME;
		} else {
			double THR = TIME / 3600.;
			double IT = THR + 1.0;
			if (IT >= NHT) {
				IT = NHT - 1.;
			}
			double THR0 = IT - 1.;
			int it = (int) IT;
			QBR = (QHYD[it] - QHYD[it - 1]) * (THR - THR0) + QHYD[it - 1];
		}
		// System.out.println("TIME: " + TIME + " QBR: "+ QBR);
		// if(TIME<4000) System.out.println("TIME: " + TIME + " QBR: "+ QBR);
		return QBR;
	}

	//氾濫流の計算
	static void indflw(int NSTEP, double TIME) {
		frovf(); //段落ちのある格子での流量フラックスの計算
		convx(); //x方向の移流項の計算
		convy(); //y方向の移流項の計算

		double HH, SFM, SFN, UX, VY;

		char tmpIP, tmpbIP, tmpIFROF;

		// Discharge flux
		for (int i = 0; i < IMAX; i++) {
			for (int j = 0; j < JMAX; j++) {
				if (i != 0 && j != 0) {  // goto 10
					tmpIP = IP[i][j];
					tmpbIP = IP[i - 1][j];
					tmpIFROF = IFROF[i][j];
					if (IP[i][j] != 'M' && IP[i][j] != 'B') {  //goto 30
						// X-DIRECTION
						if (IP[i - 1][j] != 'M') {
							if (IFROF[i][j] != 'Y') {
								if ((ZS[i][j] <= ZS[i - 1][j] || HO[i][j] > EPS)
										&& (ZS[i][j] >= ZS[i - 1][j] || HO[i - 1][j] > EPS)) { //goto 15
									HH = (HO[i][j] + HO[i - 1][j]) * .5;
									if (HH > EPS) {
										UX = SMO[i][j] / HH;
										VY = (SNO[i - 1][j] + SNO[i][j] + SNO[i][j + 1] + SNO[i - 1][j + 1]) * 0.25
												/ HH;
										SFM = RNGX[i][j] * Math.sqrt(Math.pow(UX, 2.) + Math.pow(VY, 2.))
												/ Math.pow(HH, 1.333333);
									} else {
										// goto 12
										SFM = 0.;
									}
									// goto 13
									SMN[i][j] = (SMO[i][j] * (1. - SFM) - (CUM[i][j] - CUM[i - 1][j]) * DT2DX
											- (CVM[i][j + 1] - CVM[i][j]) * DT2DY
											- (ZS[i][j] - ZS[i - 1][j]) * HH * DTGDX) / (1. + SFM);
//									System.out.println("i:" +i + " j:" + j);
//									System.out.println("SMN: " +SMN[i][j]);
									if ((HO[i][j] > EPS || SMN[i][j] >= 0.0)
											&& (HO[i - 1][j] > EPS || SMN[i][j] <= 0.0)) {
										if (Math.abs(SMN[i][j]) < 5.0e-5) {
											SMN[i][j] = 0.;
										}
									} else {
										// goto 15
										SMN[i][j] = 0.;
									}
								} else {
									// goto 15
									SMN[i][j] = 0.;
								}

							} else {
								// goto 11
								if (Math.abs(SMN[i][j]) < 5.e-5) {
									SMN[i][j] = 0.;
								}
							}
						} else {
							// goto 15
							SMN[i][j] = 0.;
						}

						// Y-DIRECTION
						if (IP[i][j - 1] != 'M') {
							if (JFROF[i][j] != 'Y') {
								if ((ZS[i][j] <= ZS[i][j - 1] || HO[i][j] > EPS)
										&& (ZS[i][j] >= ZS[i][j - 1] || HO[i][j - 1] > EPS)) {
									HH = (HO[i][j] + HO[i][j - 1]) * .5;
									if (HH > EPS) {
										UX = (SMO[i][j] + SMO[i][j - 1] + SMO[i + 1][j - 1] + SMO[i + 1][j]) * 0.25
												/ HH;
										VY = SNO[i][j] / HH;
										SFN = RNGY[i][j] * Math.sqrt(Math.pow(UX, 2.) + Math.pow(VY, 2.))
												/ Math.pow(HH, 1.333333);
									} else {
										// goto 22
										SFN = 0.;
									}
									// goto 23
									SNN[i][j] = (SNO[i][j] * (1. - SFN) - (CUN[i+1][j] - CUN[i][j]) * DT2DX
											- (CVN[i][j] - CVN[i][j - 1]) * DT2DY
											- (ZS[i][j] - ZS[i][j - 1]) * HH * DTGDX) / (1. + SFN);
//									System.out.println("SNN: " +SNN[i][j]);
									if ((HO[i][j] > EPS || SNN[i][j] >= 0.0)
											&& (HO[i][j-1] > EPS || SNN[i][j] <= 0.0)) {
										if (Math.abs(SNN[i][j]) < 5.0e-5) {
											SNN[i][j] = 0.;
										}
									} else {
										// goto 25
										SNN[i][j] = 0.;
									}
								} else {
									// goto 25
									SNN[i][j] = 0.;
								}

							} else {
								// goto 11
								if (Math.abs(SNN[i][j]) < 5.e-5) {
									SNN[i][j] = 0.;
								}
							}
						} else {
							// goto 25
							SNN[i][j] = 0.;
						}
					} else {
						// goto 30
						SMN[i][j] = 0.;
						SNN[i][j] = 0.;
					}
//					if(SMN[i][j]!=0.) {
//					System.out.println("last SMN:" + SMN[i][j]);
//					System.out.println("last SNN:" + SNN[i][j]);
//					}
				} else {
					// goto 10
					continue;
				}
			}
		}
		// Point of LEVEE BREAK
		double QQ;
		if (IBRD == 0 || JBRD == 0) {
			if (IBRD != 0) {
				if (JBRD != 0) {
					SMN[IBR][JBR] = 0.;
					SNN[IBR][JBR] = 0.;
				} else {
					QQ = QBR / DY;
					SMN[IBR][JBR] = IBRD * QQ;
					SNN[IBR][JBR] = 0.0;
					System.out.println("SMN[IBR][JBR]: " + SMN[IBR][JBR] + " ,SNN[IBR][JBR]" + SNN[IBR][JBR]);
				}
			} else {
				QQ = QBR / DX;
				SMN[IBR][JBR] = 0.;
				SNN[IBR][JBR] = JBRD * QQ;
			}
		} else {
			QQ = QBR / (DX + DY);
			SMN[IBR][JBR] = IBRD * QQ;
			SNN[IBR][JBR] = JBRD * QQ;

		}

		// WATER DEPTH
		for (int i = 0; i < IMAX; i++) {
			for (int j = 0; j < JMAX; j++) {
				if (IP[i][j] != 'M' && IP[i][j] != 'B') {
					HN[i][j] = HO[i][j] - (SMN[i + 1][j] - SMN[i][j]) * DT2DX - (SNN[i][j + 1] - SNN[i][j]) * DT2DY;
					if(HN[i][j] < 0.) HN[i][j] = 0.0;
				} else {
					HN[i][j] = 0.;
				}
			}
		}

	}

	// i¿ªÌ¬ÊvZ
	static void frovf() {
		double CQ = 0.544;
		for (int i = 0; i < IMAX; i++) {
			for (int j = 0; j < JMAX; j++) {
				if (i != 0 && j != 0) {
					if (IP[i][j] != 'M' && IP[i][j] != 'B') {
						// System.out.println("i:" + i +"j:"+ j +"IP:"+ IP[i][j]);

						// X-Direction
						if (IP[i - 1][j] != 'M') {  // go to 20
							if (ZB[i - 1][j] < ZS[i][j]) { // goto 11	
								if (ZB[i][j] < ZS[i - 1][j]) {  // goto 12
									IFROF[i][j] = 'N';
									// goto 20
								} else {  // 12
									double ID = -1.;
									double HH = HO[i][j];
									IFROF[i][j] = 'Y';
//									System.out.println(HH);
									if (HH > EPS) { //goto 14
										SMN[i][j] = ID * CQ * HH * Math.sqrt(G * HH);
									} else {  // 14
										SMN[i][j] = 0.0;
									}
								}
							} else { // 11
								double ID = 1.;
								double HH = HO[i-1][j];
								IFROF[i][j] = 'Y';
								if (HH > EPS) {
									SMN[i][j] = ID * CQ * HH * Math.sqrt(G * HH);
								} else {
									SMN[i][j] = 0.0;
								}
							}
						}

						// go to 20
						// Y-Direction
						if (IP[i][j - 1] != 'M') {  // goto 10
							if (ZB[i][j - 1] < ZS[i][j]) { // goto 21
								if (ZB[i][j] < ZS[i][j - 1]) {  // goto 22
									JFROF[i][j] = 'N';
								} else {
									double ID = -1.;
									double HH = HO[i][j];
									JFROF[i][j] = 'Y';
									if (HH > EPS) { // goto 24
										SNN[i][j] = ID * CQ * HH * Math.sqrt(G * HH);
									} else {
										SNN[i][j] = 0.0;
									}
								}

							} else { //21
								double ID = 1.;
								double HH = HO[i][j-1];
								JFROF[i][j] = 'Y';
								if (HH > EPS) { // goto 24
									SNN[i][j] = ID * CQ * HH * Math.sqrt(G * HH);
								} else { // 24
									SNN[i][j] = 0.0;  
								}
							}

						} // 10
					}
				}
			}
		}

	}

	static void convx() {
		double ESPCV = 0.1;
		double HHE, HH, UEE, UU, UE;
		double HHSW, HHSE, VSW, VSE, VS;

		for (int i = 0; i < IMAX; i++) {
			for (int j = 0; j < JMAX; j++) {
				if (i != 0 && j != 0) {
					// convection term : D(UM)/DX on (I+1/2,J)-(I+1/2,j+1)
					if (IP[i][j] != 'M') {  // goto 610
						HHE = (HCV[i + 1][j] + HCV[i][j]) * 0.5;
						HH = (HCV[i][j] + HCV[i - 1][j]) * 0.5;
						if (HHE > ESPCV) { // goto 601
							UEE = SMXCV[i + 1][j] / HHE;
						} else { // 601
							UEE = 0.0;
						}
						if (HH > ESPCV) {  //goto 603
							UU = SMXCV[i][j] / HH;
							// goto 604
						} else {  // 603
							UU = 0.0;
						}
						UE = (UEE + UU) * 0.5;  // 604
						if (UE < 0.0) {   // 605
							CUM[i][j] = UE * SMXCV[i + 1][j];
							// goto 615
						} else {  //605
							CUM[i][j] = UE * SMXCV[i][j];
						} // 615

					} else {  // 610
						CUM[i][j] = 0.0;
					}

					// convection term : D(VM)/DX on (I-1/2,j)-(I+1/2,J)
					HHSW = (HCV[i - 1][j] + HCV[i - 1][j - 1]) * 0.5;
					HHSE = (HCV[i][j - 1] + HCV[i][j]) * 0.5;
					if (HHSW > ESPCV) {
						VSW = SNYCV[i - 1][j] / HHSW;
					} else {
						VSW = 0.0;
					}
					if (HHSE > ESPCV) {
						VSE = SNYCV[i][j] / HHSE;
					} else {
						VSE = 0.0;
					}
					VS = (VSE + VSW) * 0.5;
					if (VS < 0.0) {
						CVM[i][j] = VS * SMXCV[i][j];
					} else {
						CVM[i][j] = VS * SMXCV[i][j - 1];
					}

				}
			}
		}

	}

	static void convy() {
		double ESPCV = 0.1;
		double HHN, HH, VNN, VV, VN;
		double HHNW, HHSW, UNW, USW, UW;

		for (int i = 0; i < IMAX; i++) {
			for (int j = 0; j < JMAX; j++) {
				if (i != 0 && j != 0) {
					// convection term : D(VN)/DY on (I,J+1/2)-(I+1,j+1/2)
					if (IP[i][j] != 'M') {
						HHN = (HCV[i][j + 1] + HCV[i][j]) * 0.5;
						HH = (HCV[i][j] + HCV[i][j - 1]) * 0.5;
						if (HHN > ESPCV) {
							VNN = SNYCV[i][j + 1] / HHN;
						} else {
							VNN = 0.0;
						}
						if (HH > ESPCV) {
							VV = SNYCV[i][j] / HH;
						} else {
							VV = 0.0;
						}
						VN = (VNN + VV) * 0.5;
						if (VN < 0.0) {
							CVN[i][j] = VN * SNYCV[i][j + 1];
						} else {
							CVN[i][j] = VN * SNYCV[i][j];
						}

					} else {
						CVN[i][j] = 0.0;
					}

					// convection term : D(UN)/DX on (I,J-1/2)-(I,J+1/2)
					HHNW = (HCV[i - 1][j] + HCV[i][j]) * 0.5;
					HHSW = (HCV[i][j - 1] + HCV[i - 1][j - 1]) * 0.5;
					if (HHNW > ESPCV) {
						UNW = SMXCV[i][j] / HHNW;
					} else {
						UNW = 0.0;
					}
					if (HHSW > ESPCV) {
						USW = SMXCV[i][j - 1] / HHSW;
					} else {
						USW = 0.0;
					}
					UW = (UNW + USW) * 0.5;
					if (UW < 0.0) {
						CUN[i][j] = UW * SNYCV[i][j];
					} else {
						CUN[i][j] = UW * SNYCV[i - 1][j];
					}

				}
			}
		}

	}

}
