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

 import groovy.lang.Closure;
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.ZoneId;

/*
 *Modification area - M3
 *Jira Nbr     Date      User id Description
 *WKF014       20240311  KVERCO  Developed WKF014- Write EXTAPA Audit Trail as a basis for 
 *                               Payments Approval (Payments).
 *
 */

/**
* - Write the record to EXTAPA
*/
public class Add extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
 /*
  * Input fields
  */
  private String cono;
  private int xxCONO;
  private String divi;
  private String prpn;
  private String pyon;
  private String wfac;
  private String usid;
  private String grpi;
  private String cuam;
  private String cucd;
  private String apam;

 /**
  * Add Approval Payment Proposal extension table row
  */
  public Add(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
  	this.miCaller = miCaller;
  	this.logger = logger;
  	this.program = program;
    this.ion = ion;
  }
  
  public void main() {
  	/* Validate input fields */
    cono = mi.inData.get("CONO") == null ? '' : mi.inData.get("CONO").trim();
  	if (cono == "?") {
  	  cono = "";
  	}
    if (!cono.isEmpty()) {
		  if (cono.isInteger()){
			  xxCONO = cono.toInteger();
			} else {
				mi.error("Company " + cono + " is invalid");
				return;
		  }
		} else {
			xxCONO = program.LDAZD.CONO;
		}
  	divi = mi.inData.get("DIVI") == null ? '' : mi.inData.get("DIVI").trim();
    if (divi == "?") {
      divi = "";
    }
    if (divi.isEmpty()) {
      divi = program.LDAZD.DIVI;
    } 
  	prpn = mi.inData.get("PRPN") == null ? '' : mi.inData.get("PRPN").trim();
    if (prpn.isEmpty()) {
      mi.error("Payment Proposal Number must be entered");
      return;
    }
    pyon = mi.inData.get("PYON") == null ? '' : mi.inData.get("PYON").trim();
    if (pyon.isEmpty()) {
      mi.error("Payment Order Number must be entered");
      return;
    }
    wfac = mi.inData.get("WFAC") == null ? '' : mi.inData.get("WFAC").trim();
    if (wfac.isEmpty()) {
      mi.error("Workflow action must be entered");
      return;
    }
    usid = mi.inData.get("USID") == null ? '' : mi.inData.get("USID").trim();
    if (usid.isEmpty()) {
      mi.error("User ID must be entered");
      return;
    }
    grpi = mi.inData.get("GRPI") == null ? '' : mi.inData.get("GRPI").trim();
    if (grpi.isEmpty()) {
      mi.error("Group ID must be entered");
      return;
    }
    cuam = mi.inData.get("CUAM") == null ? '' : mi.inData.get("CUAM").trim();
    if (cuam.isEmpty() == 0) {
      mi.error("Currency Amount must be entered");
      return;
    }
    cucd = mi.inData.get("CUCD") == null ? '' : mi.inData.get("CUCD").trim();
    if (cucd.isEmpty()) {
      mi.error("Currency must be entered");
      return;
    }
    apam = mi.inData.get("APAM") == null ? '' : mi.inData.get("APAM").trim();
    if (apam.isEmpty() == 0) {
      mi.error("Approved Amount must be entered");
      return;
    }
    
    /* validate prpn is in FPSUGH table */
    DBAction queryFPSUGH = database.table("FPSUGH").index("00").build();
    DBContainer FPSUGH = queryFPSUGH.getContainer();
    FPSUGH.set("P1CONO", xxCONO);
    FPSUGH.set("P1DIVI", divi);
    FPSUGH.set("P1PRPN", Long.parseLong(prpn));
    FPSUGH.set("P1PYON", Integer.parseInt(pyon));
    if (!queryFPSUGH.read(FPSUGH)) {
      mi.error("Payment Proposal is invalid");
      return;
    }
    /* validate currency */
    if (!cucd.isEmpty()) {
      DBAction queryCSYTAB = database.table("CSYTAB").index("00").build();
      DBContainer CSYTAB = queryCSYTAB.getContainer();
      CSYTAB.set("CTCONO", xxCONO);
      CSYTAB.set("CTDIVI", "");
      CSYTAB.set("CTSTCO", "CUCD");
      CSYTAB.set("CTSTKY", cucd);
      if (!queryCSYTAB.read(CSYTAB)) {
        mi.error("Currency is invalid.");
        return;
      }
    }
    /* validate approver */
    if (!usid.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", usid);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("User ID is invalid.");
        return;
      }
    }
    logger.debug("About to call writeEXTAPA() function");
    writeEXTAPA();
  }
  /**
  * Write Approval Payment Proposal audit trail record to table EXTAPA
  *
  */
  void writeEXTAPA() {
	  logger.debug("writeEXTAPA() Start");
    /* Current date and time */
  	ZoneId zid = ZoneId.of("Australia/Brisbane"); 
    LocalDateTime currentDateTimeNow = LocalDateTime.now(zid);
    int currentDate = currentDateTimeNow.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    int currentTime = Integer.valueOf(currentDateTimeNow.format(DateTimeFormatter.ofPattern("HHmmss")));
    String timestamp = currentDateTimeNow.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

    /* Write record */
    DBAction actionEXTAPA = database.table("EXTAPA").build();
  	DBContainer EXTAPA = actionEXTAPA.getContainer();
  	EXTAPA.set("EXCONO", xxCONO);
  	EXTAPA.set("EXDIVI", divi);
  	EXTAPA.set("EXPRPN", Integer.parseInt(prpn));
  	EXTAPA.set("EXPYON", Integer.parseInt(pyon));
    EXTAPA.set("EXWFAC", wfac);
    EXTAPA.set("EXUSID", usid);
    EXTAPA.set("EXGRPI", grpi);
    EXTAPA.set("EXCUAM", Double.parseDouble(cuam));
    EXTAPA.set("EXCUCD", cucd);
    EXTAPA.set("EXAPAM", Double.parseDouble(apam));
    EXTAPA.set("EXLMTS", timestamp);
  	EXTAPA.set("EXRGDT", currentDate);
  	EXTAPA.set("EXRGTM", currentTime);
  	EXTAPA.set("EXCHNO", 0);
  	EXTAPA.set("EXCHID", program.getUser());
  	actionEXTAPA.insert(EXTAPA, recordExists);
	}
  /**
   * recordExists - return record already exists error message to the MI
   *
  */
  Closure recordExists = {
	  mi.error("Record already exists");
  }
}
