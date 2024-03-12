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
  * Delete CO Authorisation extension table row
 */
 public class Delete extends ExtendM3Transaction {
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
  
  private int XXCONO;
  
 /*
  * Delete CO Authorisation extension table row
 */
  public Delete(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
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
  	
  	if (ponr.isEmpty()) { ponr = "0";  }
  	if (posx.isEmpty()) { posx = "0";  }

  	// Validate input fields
  	if (orno.isEmpty()) {
      mi.error("CO number must be entered");
      return;
    }
    XXCONO = (Integer)program.getLDAZD().CONO;
    
  	DBAction queryEXTOOL = database.table("EXTOOL").index("00").selection("EXORNO", "EXPONR", "EXPOSX").build();
    DBContainer EXTOOL = queryEXTOOL.getContainer();
    EXTOOL.set("EXCONO", XXCONO);
    EXTOOL.set("EXORNO", orno);
    EXTOOL.set("EXPONR", ponr.toInteger());
    EXTOOL.set("EXPOSX", posx.toInteger());
    if (!queryEXTOOL.readLock(EXTOOL, deleteCallBack)) {
      mi.error("Record does not exist");
      return;
    }
  }
  
  /**
   * deleteCallBack - Callback function to delete EXTOOL table
   *
  */
  Closure<?> deleteCallBack = { LockedResult lockedResult ->

    lockedResult.delete();
  
   
  }
  
}
