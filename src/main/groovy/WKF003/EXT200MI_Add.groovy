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
 *Nbr               Date      User id     Description
 *ABF_R_200         20220405  RDRIESSEN   Mods BF0200- Write/Update EXTAPR records as a basis for PO authorization process
 *ABF_R_200         20220511  RDRIESSEN   Update for XtendM3 review feedback
 *WKF013            20240318  KRISTINAV   Add PURC field - as at time of approval workflow
 *
 */

/*
* - Write the record to EXTAPR
*/
public class Add extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  //Input fields
  private String puno;
  private String appr;
  private String asts;
  private String purc;

  private int XXCONO;
 
 /*
  * Add Purchase Authorisation extension table row
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
    
  	puno = mi.inData.get("PUNO") == null ? '' : mi.inData.get("PUNO").trim();
  	if (puno == "?") {
  	  puno = "";
  	} 
  	appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	if (appr == "?") {
  	  appr = "";
  	} 
  	asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
  	if (asts == "?") {
  	  asts = "";
  	} 
  	purc = mi.inData.get("PURC") == null ? '' : mi.inData.get("PURC").trim();
  	if (purc == "?") {
  	  purc = "";
  	} 

		XXCONO = (Integer)program.LDAZD.CONO;

    // Validate input fields  	
		if (puno.isEmpty()) {
      mi.error("Purchase Order must be entered");
      return;
    }
    if (asts.isEmpty()) {
      mi.error("Authorisation status must be entered");
      return;
    }
    // - validate puno
    DBAction queryMPHEAD = database.table("MPHEAD").index("00").build();
    DBContainer MPHEAD = queryMPHEAD.getContainer();
    MPHEAD.set("IACONO", XXCONO);
    MPHEAD.set("IAPUNO", puno);
    if (!queryMPHEAD.read(MPHEAD)) {
      mi.error("Purchase order number is invalid.");
      return;
    }

    if (!asts.equals("Authorised") && !asts.equals("Cancelled") && !asts.equals("Declined") && !asts.equals("Sent for approval") && !asts.equals("Cancelling workflow") && !asts.equals("Under authorisation")) {
      mi.error("Invalid authorisation status");
      return;
    }
    // - validate approver
    if (!appr.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", appr);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("Approver is invalid.");
        return;
      }
    }
    // - validate PO creator
    if (!purc.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", purc);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("PO Creator is invalid.");
        return;
      }
    }
    writeEXTAPR(puno, appr, asts, purc);
  }
  /*
  * Write Purchase Authorisation extension table EXTAPR
  *
  */
  void writeEXTAPR(String puno, String appr, String asts, purc) {
	  //Current date and time
  	int currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
  	int currentTime = Integer.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
  	
	  DBAction actionEXTAPR = database.table("EXTAPR").build();
  	DBContainer EXTAPR = actionEXTAPR.getContainer();
  	EXTAPR.set("EXCONO", XXCONO);
  	EXTAPR.set("EXPUNO", puno);
  	EXTAPR.set("EXAPPR", appr);
  	EXTAPR.set("EXASTS", asts);
  	EXTAPR.set("EXPURC", purc);
  	EXTAPR.set("EXRGDT", currentDate);
  	EXTAPR.set("EXRGTM", currentTime);
  	EXTAPR.set("EXLMDT", currentDate);
  	EXTAPR.set("EXCHNO", 0);
  	EXTAPR.set("EXCHID", program.getUser());
  	actionEXTAPR.insert(EXTAPR, recordExists);
	}
  /*
   * recordExists - return record already exists error message to the MI
   *
  */
  Closure recordExists = {
	  mi.error("Record already exists");
  }
}
