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
 *Nbr             Date      User id     Description
 *WKF010         	20231220  WYLLIEL     Write/Update EXTOOL records as a basis for CO authorization process
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
  private String ponr;
  private String posx;
  private String sapr;
  private String orqa;
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
  	ponr = mi.inData.get("PONR") == null ? '' : mi.inData.get("PONR").trim();
  	if (ponr == "?") {
  	  ponr = "0";
  	} 
  	posx = mi.inData.get("POSX") == null ? '' : mi.inData.get("POSX").trim();
  	if (posx == "?") {
  	  posx = "0";
  	} 
  	sapr = mi.inData.get("SAPR") == null ? '' : mi.inData.get("SAPR").trim();
  	if (sapr == "?") {
  	  sapr = "0";
  	} 
  	orqa = mi.inData.get("ORQA") == null ? '' : mi.inData.get("ORQA").trim();
  	if (orqa == "?") {
  	  orqa = "0";
  	} 
  	
  	if (ponr.isEmpty()) { ponr = "0";  }
  	if (posx.isEmpty()) { posx = "0";  }
  	
		XXCONO = (Integer)program.LDAZD.CONO;
	
  	if (orno.isEmpty()) {
      mi.error("CO number must be entered");
      return;
    }
    // - validate orno
    if (!orno.isEmpty()) {
      DBAction queryOOLINE = database.table("OOLINE").index("00").selection("OBORNO", "OBPONR", "OBPOSX").build();
      DBContainer OOLINE = queryOOLINE.getContainer();
      OOLINE.set("OBCONO", XXCONO);
      OOLINE.set("OBORNO", orno);
      OOLINE.set("OBPONR", ponr.toInteger());
      OOLINE.set("OBPOSX", posx.toInteger());
      if (!queryOOLINE.read(OOLINE)) {        
  	     found = false;
         mi.error("CO Line number invalid");
         return;
      }
    }
    
    
    DBAction query = database.table("EXTOOL").index("00").build();
    DBContainer container = query.getContainer();
    container.set("EXCONO", XXCONO);
    container.set("EXORNO", orno);
    container.set("EXPONR", ponr.toInteger());
    container.set("EXPOSX", posx.toInteger());
    if (!query.readLock(container, updateCallBack)) {
      mi.error("Record does not exists");
      return;
    }
  }
  
  /**
   * updateCallBack - Callback function to update EXTOOL table
   *
   */   
  Closure<?> updateCallBack = { LockedResult lockedResult ->
    
    ZoneId zid = ZoneId.of("Australia/Sydney"); 
    int currentDate = LocalDate.now(zid).format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    
    if (!sapr.isEmpty()) {
      lockedResult.set("EXSAPR", sapr.toDouble());
    }
    if (!orqa.isEmpty()) {
      lockedResult.set("EXORQA", orqa.toDouble());
    }
    lockedResult.set("EXCHNO", lockedResult.get("EXCHNO").toString().toInteger() +1);
    lockedResult.set("EXCHID", program.getUser());
    lockedResult.set("EXLMDT", currentDate);
    lockedResult.update();
  
  }
}
