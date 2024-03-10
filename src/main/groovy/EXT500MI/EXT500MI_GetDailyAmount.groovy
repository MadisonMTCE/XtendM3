/*
 ***************************************************************
 *                                                             *
 *                           NOTICE                            *
 *                                                             *
 *   THIS SOFTWARE IS THE PROPERTY OF AND CONTAINS             *
 *   CONFIDENTIAL INFORMATION OF INFOR AND/OR ITS AFFILIATES   *
 *   OR SUBSIDIARIES AND SHALL NOT BE DISCLOSED WITHOUT PRIOR  *
 *   WRITTEN PERMISSION. LICENSED CUSTOMERS MAY COPY AND       *
 *   ADAPT THIS SOFTWARE FOR THEIR OWN USE IN ACCORDANCE WITH  *
 *   THE TERMS OF THEIR SOFTWARE LICENSE AGREEMENT.            *
 *   ALL OTHER RIGHTS RESERVED.                                *
 *                                                             *
 *   (c) COPYRIGHT 2020 INFOR.  ALL RIGHTS RESERVED.           *
 *   THE WORD AND DESIGN MARKS SET FORTH HEREIN ARE            *
 *   TRADEMARKS AND/OR REGISTERED TRADEMARKS OF INFOR          *
 *   AND/OR ITS AFFILIATES AND SUBSIDIARIES. ALL RIGHTS        *
 *   RESERVED.  ALL OTHER TRADEMARKS LISTED HEREIN ARE         *
 *   THE PROPERTY OF THEIR RESPECTIVE OWNERS.                  *
 *                                                             *
 ***************************************************************
 */

 import groovy.lang.Closure
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.ZoneId;
 import groovy.json.JsonSlurper;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.text.DecimalFormat;

/*
 *Modification area - M3
 *Jira Nbr          Date      User id       Description
 *MGMCAW-1756       20240301  TTATAROGLOU   Developed WKF014- GetDailyAmount which is the sum of the APAM total for every 
 *                                          row in EXTAPP Approval Payment Proposal where row contains a CONO, GRPI and 
 *                                          DATE as a basis for Payments Approval (Payments) Change Request.
 *
 */

public class GetDailyAmount extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;

  //Input fields
  private String cono;
  private int XXCONO;
  private String grpi;
  
  // For getting data on closure method
  private String apam;
  private String date;
  
  // List used of EXXAPP records used to calculate output field
  private List lstEXTAPP;
   
 /*
  * GetDailyAmount which is the sum of the APAM total for every row in EXTAPP Approval Payment Proposal where row
  * contains CONO, GRPI and DATE
 */
  public GetDailyAmount(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
  	this.miCaller = miCaller;
  	this.logger = logger;
  	this.program = program;
	this.ion = ion; 
  }
  
  public void main() {
  	logger.info("main() Start");
  	//Validate input fields
    cono = mi.inData.get("CONO") == null ? '' : mi.inData.get("CONO").trim();
  	if (cono == "?") {
  	  cono = "";
  	}
	if (!cono.isEmpty()) {
		if (cono.isInteger()){
			XXCONO= cono.toInteger();
			// - validate approver			
			DBAction queryCMNCMP = database.table("CMNCMP").index("00").build();
			DBContainer CMNCMP = queryCMNCMP.getContainer();
			CMNCMP.set("JICONO", XXCONO);
			if (!queryCMNCMP.read(CMNCMP)) {
				mi.error("CONO is valid");
				return;
			}
			logger.info("Valid CONO detected");
		} else {
			mi.error("Company " + cono + " is invalid");
			return;
		}
	} else {
		XXCONO= program.LDAZD.CONO;
	}
	logger.info("cono=" + cono + " XXCONO=" + XXCONO);
	grpi = mi.inData.get("GRPI") == null ? '' : mi.inData.get("GRPI").trim();
  	logger.info("grpi=" + grpi);
	if (grpi == "?") {
		grpi = "";
		logger.info("grpi=" + grpi);
	}
	if (grpi.isEmpty()) {
		mi.error("An Approval Group must be supplied");
		return;
	}
	logger.info("Valid GRPI detected");
	
	//Perform Query
	ZoneId zid = ZoneId.of("Australia/Brisbane");
	int currentDate = LocalDate.now(zid).format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
	logger.info("Will now perform query XXCONO=" + XXCONO + " grpi=" + grpi + " currentDate=" + currentDate);
	DBAction queryEXTAPPRECORDS = database.table("EXTAPP").index("20").selection("EXCONO", "EXGRPI", "EXDATE", "EXASTS").build();
	DBContainer EXTAPPRECORDS = queryEXTAPPRECORDS.getContainer();
	EXTAPPRECORDS.set("EXCONO", XXCONO);
	EXTAPPRECORDS.set("EXGRPI", grpi);
	EXTAPPRECORDS.set("EXASTS", "Approved");
	EXTAPPRECORDS.set("EXDATE", currentDate);
	lstEXTAPP = new ArrayList();
	if (queryEXTAPPRECORDS.readAll(EXTAPPRECORDS, 2, 999, listEXTAPP) == 0) {  
		mi.error("Query is invalid.");
		return;
	}
	logger.info("lstEXTAPP.size()=" + lstEXTAPP.size());
	double totalApam = 0;
	int extAppRecordDate;
	String extAppRecordAsts;
	if (lstEXTAPP.size() == 1) {
		Map<String, String> record1 = (Map<String, String>) lstEXTAPP[0];
		extAppRecordDate = Integer.parseInt(record1.EXDATE);
		extAppRecordAsts = record1.EXASTS;
		if ((currentDate == extAppRecordDate) && (extAppRecordAsts == "Approved"))
			totalApam = Double.parseDouble(record1.EXAPAM);
	} else if (lstEXTAPP.size() > 1) {
		for (int j=0;j<lstEXTAPP.size();j++) {
			Map<String, String> record2 = (Map<String, String>) lstEXTAPP[j];
			extAppRecordDate = Integer.parseInt(record2.EXDATE);
			extAppRecordAsts = record2.EXASTS;
			if ((currentDate == extAppRecordDate) && (extAppRecordAsts == "Approved")) {	
				double currentRecordApam = 0;
				currentRecordApam = Double.parseDouble(record2.EXAPAM);
				totalApam = totalApam + currentRecordApam;
			}
		}
	} else if (lstEXTAPP.size() == 0) {
		mi.error("No Record exist for this search criteria");
		return;
	}
    
	//Output results
	mi.outData.put("CONO", XXCONO.toString());
	mi.outData.put("GRPI", grpi);
	mi.outData.put("DATE", currentDate.toString());
	mi.outData.put("APAM", totalApam.toString());
	mi.write();
 }
 
 /*
 * listEXTAPP - Callback function to return EXTAPP
 *
 */
 Closure<?> listEXTAPP = { DBContainer EXTAPPRECORDS ->
	String apam = EXTAPPRECORDS.get("EXAPAM").toString().trim();
	String asts = EXTAPPRECORDS.get("EXASTS").toString().trim();
	String date = EXTAPPRECORDS.get("EXDATE").toString().trim();
	def map = [EXAPAM: apam, EXASTS: asts, EXDATE: date];
	lstEXTAPP.add(map);
 }

}