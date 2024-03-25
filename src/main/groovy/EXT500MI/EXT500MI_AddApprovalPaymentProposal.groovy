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
 import groovy.json.JsonSlurper;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.text.DecimalFormat;


/*
 *Modification area - M3
 *Jira Nbr          Date      User id       Description
 *MGMCAW-1756       20240322  RKROPP        Changes to set default values for input parameters, changes to make all variables 
 *                                          lowerCamelCase, change to remove duplicate validation of existing record in EXTAPP.         
 *MGMCAW-1756       20240311  RKROPP        Developed WKF014- Write EXTAPP Approval Payment Proposal as a basis for 
 *                                          Payments Approval (Payments) Change Request.
 *
 */

/*
* - Write the record to EXTAPP
*/
public class Add extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
 /*
  * Input fields with Default Values
  */
  private String cono = "0";
  private int xxCono = 0;
  private String divi = "0";
  private String prpn = "0";
  private String pyon = "0";
  private String asts = "";
  private String appr = "";
  private String grpi = "";
  private String apam = "0";
    
 /*
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
    logger.debug("main() Start");
    /* Validate input fields */
    cono = mi.inData.get("CONO") == null ? '' : mi.inData.get("CONO").trim();
  	if (cono == "?") {
  	  cono = "";
  	}
    if (!cono.isEmpty()) {
		  if (cono.isInteger()){
			  xxCono = cono.toInteger();
			} else {
				mi.error("Company " + cono + " is invalid");
				return;
		  }
		} else {
			xxCono = program.LDAZD.CONO;
		}
    logger.debug("cono=" + cono + " xxCono=" + xxCono);
  	divi = mi.inData.get("DIVI") == null ? '' : mi.inData.get("DIVI").trim();
  	logger.debug("divi=" + divi);
    if (divi == "?") {
      divi = "";
    }
    if (divi.isEmpty()) {
      divi = program.LDAZD.DIVI;
  	  logger.debug("divi=" + divi);
    } 
  	prpn = mi.inData.get("PRPN") == null ? '' : mi.inData.get("PRPN").trim();
  	if (prpn == "?") {
  	  prpn = "";
  	}
    logger.debug("prpn=" + prpn);
    pyon = mi.inData.get("PYON") == null ? '' : mi.inData.get("PYON").trim();
  	if (pyon == "?") {
  	  pyon = "";
  	}
    logger.debug("pyon=" + pyon);
    asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
  	logger.debug("asts=" + asts);
    if (asts == "?") {
  	  asts = "";
  	  logger.debug("asts=" + asts);
    }
    appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	logger.debug("appr=" + appr);
    if (appr == "?") {
  	  appr = "";
  	  logger.debug("appr=" + appr);
    }
    grpi = mi.inData.get("GRPI") == null ? '' : mi.inData.get("GRPI").trim();
  	logger.debug("grpi=" + grpi);
    if (grpi == "?") {
  	  grpi = "";
  	  logger.debug("grpi=" + grpi);
    }
    apam = mi.inData.get("APAM") == null ? '' : mi.inData.get("APAM").trim();
  	logger.debug("apam=" + apam);
    if (apam == "?") {
  	  apam = "";
  	}
    logger.debug("apam=" + apam);  	
    if (prpn.isEmpty()) {
      mi.error("Payment Proposal Number must be entered");
      return;
    }
    if (pyon.isEmpty()) {
      mi.error("Payment Order Number must be entered");
      return;
    }
    if (asts.isEmpty()) {
      mi.error("Approval Status must be entered");
      return;
    }
    if (grpi.isEmpty()) {
      mi.error("Group ID must be entered");
      return;
    }
    if (apam.isEmpty() == 0) {
      mi.error("Approved Amount must be entered");
      return;
    }
    /* validate asts */
    if (!asts.equals("Sent for Approval") && !asts.equals("Under Approval") && !asts.equals("Under Cancellation") && !asts.equals("Approved") && !asts.equals("Declined") && !asts.equals("Rejected") && !asts.equals("Cancelled")) {
      mi.error("Invalid Approval Status");
      return;
    }
    /* validate approver */
    if (!appr.isEmpty()) {
      DBAction queryCmnUsr = database.table("CMNUSR").index("00").build();
      DBContainer cmnUsr = queryCmnUsr.getContainer();
      cmnUsr.set("JUCONO", 0);
      cmnUsr.set("JUDIVI", "");
      cmnUsr.set("JUUSID", appr);
      if (!queryCmnUsr.read(cmnUsr)) {
        mi.error("Approver is invalid.");
        return;
      }
    }
    logger.debug("Valid approver detected");
    /* validate prpn is in FPSUGH table */
    DBAction queryFpsugh = database.table("FPSUGH").index("00").build();
    DBContainer fpsugh = queryFpsugh.getContainer();
    fpsugh.set("P1CONO", xxCono);
    fpsugh.set("P1DIVI", divi);
    fpsugh.set("P1PRPN", Long.parseLong(prpn));
    fpsugh.set("P1PYON", Integer.parseInt(pyon));
    if (!queryFpsugh.read(fpsugh)) {
      mi.error("Transaction is invalid");
      return;
    }
    logger.debug("prpn exists in FPSUGH table");
    logger.debug("About to call writeEXTAPP() function");
    writeEXTAPP();
  }
  /*
  * Write Approval Payment Proposal record to table EXTAPP
  *
  */
  void writeEXTAPP() {
	  logger.debug("writeEXTAPP() Start");
    /* Current date and time */
  	ZoneId zid = ZoneId.of("Australia/Brisbane"); 
    LocalDateTime currentDateTimeNow = LocalDateTime.now(zid);
    int currentDate = currentDateTimeNow.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    int currentYear = currentDateTimeNow.format(DateTimeFormatter.ofPattern("yyyy")).toInteger();
    int currentTime = Integer.valueOf(currentDateTimeNow.format(DateTimeFormatter.ofPattern("HHmmss")));
  	
	  /* Write record */
    DBAction actionExtApp = database.table("EXTAPP").build();
  	DBContainer extApp = actionExtApp.getContainer();
  	extApp.set("EXCONO", xxCono);
  	extApp.set("EXDIVI", divi);
  	extApp.set("EXPRPN", Integer.parseInt(prpn));
  	extApp.set("EXPYON", Integer.parseInt(pyon));
    extApp.set("EXINYR", currentYear);
    extApp.set("EXASTS", asts);
    extApp.set("EXAPPR", appr);
    extApp.set("EXGRPI", grpi);
    extApp.set("EXDATE", currentDate);
    extApp.set("EXAPAM", Double.parseDouble(apam));
  	extApp.set("EXRGDT", currentDate);
  	extApp.set("EXRGTM", currentTime);
  	extApp.set("EXCHNO", 0);
  	extApp.set("EXCHID", program.getUser());
    logger.debug("writeEXTAPP() About to insert to EXTAPP");
  	actionExtApp.insert(extApp, recordExists);
    logger.debug("writeEXTAPP() After insert to EXTAPP");
	}
  /*
   * recordExists - return record already exists error message to the MI
   *
  */
  Closure recordExists = {
	  mi.error("An approval entry already exists for this transaction");
  }
}