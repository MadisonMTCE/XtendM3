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
 *MGMCAW-1756       20240325  RKROPP        Changes to set default values for input parameters, changes to make all variables 
 *                                          lowerCamelCase, replaced def with typed variable in listExtApp callback function.
 *MGMCAW-1756       20240311  RKROPP        Developed WKF014- GetDailyAmount which is the sum of the APAM total for every 
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

  /* Input fields with Default Values */
  private String cono = "";
  private int xxCono = 0;
  private String grpi = "";
  
  /* For getting data on closure method */
  private String apam;
  private String date;
  
  /* lst used of EXXAPP records used to calculate output field */
  private List lstExtApp;
   
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
  	logger.debug("main() Start");
  	/* Validate input fields */
    cono = mi.inData.get("CONO") == null ? '' : mi.inData.get("CONO").trim();
  	if (cono == "?") {
  	  cono = "";
  	}
	if (!cono.isEmpty()) {
		if (cono.isInteger()){
			xxCono = cono.toInteger();
			/* validate approver */			
			DBAction queryCmnCmp = database.table("CMNCMP").index("00").build();
			DBContainer cmnCmp = queryCmnCmp.getContainer();
			cmnCmp.set("JICONO", xxCono);
			if (!queryCmnCmp.read(cmnCmp)) {
				mi.error("CONO is valid");
				return;
			}
			logger.debug("Valid CONO detected");
		} else {
			mi.error("Company " + cono + " is invalid");
			return;
		}
	} else {
		xxCono = program.LDAZD.CONO;
	}
	logger.debug("cono=" + cono + " xxCono=" + xxCono);
	grpi = mi.inData.get("GRPI") == null ? '' : mi.inData.get("GRPI").trim();
  	logger.debug("grpi=" + grpi);
	if (grpi == "?") {
		grpi = "";
		logger.debug("grpi=" + grpi);
	}
	if (grpi.isEmpty()) {
		mi.error("An Approval Group must be supplied");
		return;
	}
	logger.debug("Valid GRPI detected");
	
	/* Perform Query */
	ZoneId zid = ZoneId.of("Australia/Brisbane");
	int currentDate = LocalDate.now(zid).format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
	logger.debug("Will now perform query xxCono=" + xxCono + " grpi=" + grpi + " currentDate=" + currentDate);
	DBAction queryExtAppRecords = database.table("EXTAPP").index("20").selection("EXCONO", "EXGRPI", "EXDATE", "EXASTS").build();
	DBContainer extAppRecords = queryExtAppRecords.getContainer();
	extAppRecords.set("EXCONO", xxCono);
	extAppRecords.set("EXGRPI", grpi);
	extAppRecords.set("EXASTS", "Approved");
	extAppRecords.set("EXDATE", currentDate);
	lstExtApp = new ArrayList();
	if (queryExtAppRecords.readAll(extAppRecords, 2, 999, listExtApp) == 0) {  
		mi.error("Query is invalid.");
		return;
	}
	logger.debug("lstExtApp.size()=" + lstExtApp.size());
	double totalApam = 0;
	int extAppRecordDate;
	String extAppRecordAsts;
	if (lstExtApp.size() == 1) {
		Map<String, String> record1 = (Map<String, String>) lstExtApp[0];
		extAppRecordDate = Integer.parseInt(record1.EXDATE);
		extAppRecordAsts = record1.EXASTS;
		if ((currentDate == extAppRecordDate) && (extAppRecordAsts == "Approved"))
			totalApam = Double.parseDouble(record1.EXAPAM);
	} else if (lstExtApp.size() > 1) {
		for (int j=0;j<lstExtApp.size();j++) {
			Map<String, String> record2 = (Map<String, String>) lstExtApp[j];
			extAppRecordDate = Integer.parseInt(record2.EXDATE);
			extAppRecordAsts = record2.EXASTS;
			if ((currentDate == extAppRecordDate) && (extAppRecordAsts == "Approved")) {	
				double currentRecordApam = 0;
				currentRecordApam = Double.parseDouble(record2.EXAPAM);
				totalApam = totalApam + currentRecordApam;
			}
		}
	} else if (lstExtApp.size() == 0) {
		mi.error("No Record exist for this search criteria");
		return;
	}
    
	/* Output results */
	mi.outData.put("CONO", xxCono.toString());
	mi.outData.put("GRPI", grpi);
	mi.outData.put("DATE", currentDate.toString());
	mi.outData.put("APAM", totalApam.toString());
	mi.write();
 }
 
 /*
 * listExtApp - Callback function to return EXTAPP
 *
 */
 Closure<?> listExtApp = { DBContainer extAppRecords ->
	String apam = extAppRecords.get("EXAPAM").toString().trim();
	String asts = extAppRecords.get("EXASTS").toString().trim();
	String date = extAppRecords.get("EXDATE").toString().trim();
	Map<String,String> params= ["EXAPAM":"${apam}".toString(), "EXASTS":"${asts}".toString(), "EXDATE":"${date}".toString()] // toString is needed to convert from gstring to string
	lstExtApp.add(params);
 }

}