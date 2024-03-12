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
* - Add CO Line Authorisation extension table row
*/
public class Add extends ExtendM3Transaction {
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
 
 /*
  * Add CO Authorisation extension table row
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
  	if (sapr.isEmpty()) { sapr = "0";  }
  	if (orqa.isEmpty()) { orqa = "0";  }

	XXCONO = (Integer)program.LDAZD.CONO;

    // Validate input fields  	
	if (orno.isEmpty()) {
      mi.error("Customer Order Number must be entered");
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
          mi.error("CO line number invalid");
          return;
        }
    }
  
    writeEXTOOL(orno);
  }
  /**
  * writeEXTOOL - Write CO Authorisation extension table EXTOOL
  *
  */
  def writeEXTOOL(String orno) {
	//Current date and time
  	int currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
  	int currentTime = Integer.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
  	
	DBAction actionEXTOOL = database.table("EXTOOL").build();
  	DBContainer EXTOOL = actionEXTOOL.getContainer();
  	EXTOOL.set("EXCONO", XXCONO);
  	EXTOOL.set("EXORNO", orno);
  	EXTOOL.set("EXPONR", ponr.toInteger());
  	EXTOOL.set("EXPOSX", posx.toInteger());
  	EXTOOL.set("EXORQA", orqa.toDouble());
  	EXTOOL.set("EXSAPR", sapr.toDouble());
  	EXTOOL.set("EXRGDT", currentDate);
  	EXTOOL.set("EXRGTM", currentTime);
  	EXTOOL.set("EXLMDT", currentDate);
  	EXTOOL.set("EXCHNO", 0);
  	EXTOOL.set("EXCHID", program.getUser());
  	actionEXTOOL.insert(EXTOOL, recordExists);
	}
  /**
   * recordExists - return record already exists error message to the MI
   *
  */
  Closure recordExists = {
	  mi.error("Record already exists");
  }
  
}
