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
 *MGMCAW-1756       20240228  TTATAROGLOU   Developed WKF014- Update EXTAPP Approval Payment Proposal as a basis for 
 *                                          Payments Approval (Payments) Change Request.
 *
 */

 /*
  * Update Purchase Authorisation extension table
 */
  public class Update extends ExtendM3Transaction {
    
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  //Input fields must be all strings then covert to data type required to adding to database
  private String cono;
  private int XXCONO;
  private String divi;
  private String prpn;
  private String pyon;
  private String asts;
  private String appr;
  private String grpi;
  private String apam;
  
 /*
  * Update Approval Payment Proposal extension table row
  */
  public Update(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
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
			} else {
				mi.error("Company " + cono + " is invalid");
				return;
		  }
		} else {
			XXCONO= program.LDAZD.CONO;
		}
    logger.info("cono=" + cono + " XXCONO=" + XXCONO);
  	divi = mi.inData.get("DIVI") == null ? '' : mi.inData.get("DIVI").trim();
  	logger.info("divi=" + divi);
    if (divi == "?") {
      divi = "";
    }
    if (divi.isEmpty()) {
      divi = program.LDAZD.DIVI; // get from M3 current DIVI using, not sure if this will work, if not then force them to send it
  	  logger.info("divi=" + divi);
    } 
  	prpn = mi.inData.get("PRPN") == null ? '' : mi.inData.get("PRPN").trim();
  	if (prpn == "?") {
  	  prpn = "";
  	}
    logger.info("prpn=" + prpn);
    pyon = mi.inData.get("PYON") == null ? '' : mi.inData.get("PYON").trim();
  	if (pyon == "?") {
  	  pyon = "";
  	}
    logger.info("pyon=" + pyon);
    asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
  	logger.info("asts=" + asts);
    if (asts == "?") {
  	  asts = "";
  	  logger.info("asts=" + asts);
    }
    appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	logger.info("appr=" + appr);
    if (appr == "?") {
  	  appr = "";
  	  logger.info("appr=" + appr);
    }
    grpi = mi.inData.get("GRPI") == null ? '' : mi.inData.get("GRPI").trim();
  	logger.info("grpi=" + grpi);
    if (grpi == "?") {
  	  grpi = "";
  	  logger.info("grpi=" + grpi);
    }
    apam = mi.inData.get("APAM") == null ? '' : mi.inData.get("APAM").trim();
  	logger.info("apam=" + apam);
    if (apam == "?") {
  	  apam = "";
  	}
    logger.info("apam=" + apam);  	
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
    // - validate asts
    if (!asts.equals("Sent for Approval") && !asts.equals("Under Approval") && !asts.equals("Under Cancellation") && !asts.equals("Approved") && !asts.equals("Declined") && !asts.equals("Rejected") && !asts.equals("Cancelled")) {
      mi.error("Invalid Approval Status");
      return;
    }
    // - validate approver
    if (!appr.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", appr); //THEOT is a valid user on Madison to test with
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("Approver is invalid");
        return;
      }
    }
    logger.info("Valid approver detected");
    // - Update Payment Proposal Number record if exists
    logger.info("About to Update record in the database but will do it while checking if record exits first");
    DBAction query = database.table("EXTAPP").index("00").build();
    DBContainer container = query.getContainer();
    container.set("EXCONO", XXCONO);
    container.set("EXDIVI", divi);
    container.set("EXPRPN", Long.parseLong(prpn));
    container.set("EXPYON", Integer.parseInt(pyon));
    if (!query.readLock(container, updateCallBack)) {
      mi.error("An approval entry does not exist for this transaction");
      return;
    }
  }
  
  /*
   * updateCallBack - Callback function to update EXTAPP table
   *
   */
  Closure<?> updateCallBack = { LockedResult lockedResult ->
    
    ZoneId zid = ZoneId.of("Australia/Brisbane"); 
    int currentDate = LocalDate.now(zid).format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    
    lockedResult.set("EXAPPR", appr);
    lockedResult.set("EXASTS", asts);
    lockedResult.set("EXGRPI", grpi);
    lockedResult.set("EXAPAM", Double.parseDouble(apam));
    lockedResult.set("EXASTS", asts);
    if (asts == "Approved")
		lockedResult.set("EXDATE", currentDate);
    lockedResult.update();
  }
}