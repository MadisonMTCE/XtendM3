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
 *Nbr             Date      User id     Description
 *WKF010         	20231220  KVERCO     Write/Update EXTOH records as a basis for CO authorization process
 *
 */

 /**
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
  private String orno;
  private String ptwf;
  private String ovwf;
  private String adwf;
  private String oblc;
  private String usid;
  private String orde;
  private String appr;
  private String asts;
  private boolean found;
  
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
    
  	orno = mi.inData.get("ORNO") == null ? '' : mi.inData.get("ORNO").trim();
  	if (orno == "?") {
  	  orno = "";
  	} 
  	ptwf = mi.inData.get("PTWF") == null ? '' : mi.inData.get("PTWF").trim();
  	if (ptwf == "?") {
  	  ptwf = "";
  	} 
  	ovwf = mi.inData.get("OVWF") == null ? '' : mi.inData.get("OVWF").trim();
  	if (ovwf == "?") {
  	  ovwf = "";
  	} 
  	adwf = mi.inData.get("ADWF") == null ? '' : mi.inData.get("ADWF").trim();
  	if (adwf == "?") {
  	  adwf = "";
  	} 
  	oblc = mi.inData.get("OBLC") == null ? '' : mi.inData.get("OBLC").trim();
  	if (oblc == "?") {
  	  oblc = "";
  	} 
  	usid = mi.inData.get("USID") == null ? '' : mi.inData.get("USID").trim();
  	if (usid == "?") {
  	  usid = "";
  	} 
  	orde = mi.inData.get("ORDE") == null ? '' : mi.inData.get("ORDE").trim();
  	if (orde == "?") {
  	  orde = "";
  	} 
  	appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	if (appr == "?") {
  	  appr = "";
  	} 
  	asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
  	if (asts == "?") {
  	  asts = "";
  	} 
  	
		XXCONO = (Integer)program.LDAZD.CONO;
	
  	if (orno.isEmpty()) {
      mi.error("CO number must be entered");
      return;
    }
    // - validate orno
    if (!orno.isEmpty()) {
      DBAction queryOOHEAD = database.table("OOHEAD").index("00").selection("OAORNO").build();
      DBContainer OOHEAD = queryOOHEAD.getContainer();
      OOHEAD.set("OACONO", XXCONO);
      OOHEAD.set("OAORNO", orno);
      if (!queryOOHEAD.read(OOHEAD)) {        
  	     found = false;
         mi.error("CO number is invalid");
         return;
      }
    }
    
    // - validate user id
    if (!usid.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", usid);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("User id is invalid.");
        return;
      }
    }  
    
    // - validate approver user id
    if (!appr.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", appr);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("Approver is invalid.");
        return;
      }
    }  

    // - validate the status
    if (!asts.isEmpty()) {
      if (!asts.equals("Approved") && !asts.equals("Rejected")) {
        mi.error("Invalid authorisation status");
        return;
      }
    }    
    
    DBAction query = database.table("EXTOOH").index("00").build();
    DBContainer container = query.getContainer();
    container.set("EXCONO", XXCONO);
    container.set("EXORNO", orno);
    if (!query.readLock(container, updateCallBack)) {
      mi.error("Record does not exists");
      return;
    }
  }
  
  /**
   * updateCallBack - Callback function to update EXTOH table
   *
   */   
  Closure<?> updateCallBack = { LockedResult lockedResult ->
    
    ZoneId zid = ZoneId.of("Australia/Sydney"); 
    int currentDate = LocalDate.now(zid).format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    
    if (!ptwf.isEmpty()) {
      lockedResult.set("EXPTWF", ptwf);
    }
    if (!ovwf.isEmpty()) {
      lockedResult.set("EXOVWF", ovwf);
    }
    if (!adwf.isEmpty()) {
      lockedResult.set("EXADWF", adwf);
    }
    if (!oblc.isEmpty()) {
      lockedResult.set("EXOBLC", oblc.toDouble());
    }
    if (!usid.isEmpty()) {
      lockedResult.set("EXUSID", usid);
    }
    if (!orde.isEmpty()) {
      lockedResult.set("EXORDE", orde);
    }
    if (!appr.isEmpty()) {
      lockedResult.set("EXAPPR", appr);
    }
    if (!asts.isEmpty()) {
      lockedResult.set("EXASTS", asts);
    }
    lockedResult.set("EXCHNO", lockedResult.get("EXCHNO").toString().toInteger() +1);
    lockedResult.set("EXCHID", program.getUser());
    lockedResult.set("EXLMDT", currentDate);
    lockedResult.update();
  
  }
}
