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
 *WKF010         	20231220  WYLLIEL     Write/Update EXTOOH records as a basis for CO authorization process
 *
*/

 /**
  * Get CO Authorisation extension table row
 */
 public class Get extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  //Input fields
  private String orno;

  private int XXCONO;
   
 /*
  * Get CO Authorisation extension table row
 */
  public Get(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
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
    if (orno.isEmpty()) {
      mi.error("CO number must be entered");
      return;
    }
  	
    XXCONO = (Integer)program.LDAZD.CONO;
    
    DBAction query = database.table("EXTOOH").index("00").selection("EXCONO", "EXORNO", "EXPTWF", "EXOVWF", "EXADWF", "EXOBLC", "EXUSID", "EXORDE", "EXAPPR", "EXASTS", "EXRGDT", "EXRGTM", "EXLMDT", "EXCHNO", "EXCHID" ).build();
    DBContainer container = query.getContainer();
    container.set("EXCONO", XXCONO);
    container.set("EXORNO", orno);
    if (query.read(container)) {
      mi.outData.put("CONO", XXCONO.toString());
      mi.outData.put("ORNO", container.get("EXORNO").toString());
      mi.outData.put("PTWF", container.get("EXPTWF").toString());
      mi.outData.put("OVWF", container.get("EXOVWF").toString());
      mi.outData.put("ADWF", container.get("EXADWF").toString());
      mi.outData.put("OBLC", container.get("EXOBLC").toString());
      mi.outData.put("USID", container.get("EXUSID").toString());
      mi.outData.put("ORDE", container.get("EXORDE").toString());
      mi.outData.put("APPR", container.get("EXAPPR").toString());
      mi.outData.put("ASTS", container.get("EXASTS").toString());
      mi.outData.put("RGDT", container.get("EXRGDT").toString());
      mi.outData.put("RGTM", container.get("EXRGTM").toString());
      mi.outData.put("LMDT", container.get("EXLMDT").toString());
      mi.outData.put("CHNO", container.get("EXCHNO").toString());
      mi.outData.put("CHID", container.get("EXCHID").toString());
      mi.write();
    } else {
      mi.error("Record does not exist in EXTOOH.");
      return;
    }
  }
  
}
