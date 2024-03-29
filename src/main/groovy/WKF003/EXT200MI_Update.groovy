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
 *ABF_R_200         20220405  RDRIESSEN   Mods BF0200- Update EXTAPR records as a basis for PO authorization process
 *ABF_R_200         20220511  RDRIESSEN   Update for XtendM3 review feedback
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
  
  //Input fields
  private String puno;
  private String appr;
  private String asts;
  
  private int XXCONO;
  
  public Update(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
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
  	asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
  	if (asts == "?") {
  	  asts = "";
  	} 
  	appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	if (appr == "?") {
  	  appr = "";
  	} 
		XXCONO = (Integer)program.LDAZD.CONO;
	
  	if (puno.isEmpty()) {
      mi.error("PO number must be entered");
      return;
    }
    DBAction queryMPHEAD = database.table("MPHEAD").index("00").build();
    DBContainer MPHEAD = queryMPHEAD.getContainer();
    MPHEAD.set("IACONO", XXCONO);
    MPHEAD.set("IAPUNO", puno);
    if (!queryMPHEAD.read(MPHEAD)) {
      mi.error("PO number is invalid.");
      return;
    }
    
    if (!asts.isEmpty()) {
      if (!asts.equals("Authorised") && !asts.equals("Cancelled") && !asts.equals("Declined") && !asts.equals("Sent for approval") && !asts.equals("Cancelling workflow") && !asts.equals("Under authorisation")) {
        mi.error("Invalid authorisation status");
        return;
      }
    }
    // - validate approver
    if (!appr.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").build()
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", appr);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("Approver is invalid.");
        return;
      }
    }
    
    DBAction query = database.table("EXTAPR").index("00").build();
    DBContainer container = query.getContainer();
    container.set("EXCONO", XXCONO);
    container.set("EXPUNO", puno);
    if (!query.readLock(container, updateCallBack)) {
      mi.error("Record does not exists");
      return;
    }
  }
  
  /*
   * updateCallBack - Callback function to update EXTAPR table
   *
   */
  Closure<?> updateCallBack = { LockedResult lockedResult ->
    
    ZoneId zid = ZoneId.of("Australia/Sydney"); 
    int currentDate = LocalDate.now(zid).format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    
    lockedResult.set("EXASTS", asts);
    lockedResult.set("EXAPPR", appr);
    lockedResult.set("EXCHNO", lockedResult.get("EXCHNO").toString().toInteger() +1);
    lockedResult.set("EXCHID", program.getUser());
    lockedResult.set("EXLMDT", currentDate);
    lockedResult.update();
  
  
  }
}