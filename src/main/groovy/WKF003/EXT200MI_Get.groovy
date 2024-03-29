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
 *Nbr               Date      User id     Description
 *ABF_R_200         20220405  RDRIESSEN   Mods BF0200- Write/Update EXTAPR records as a basis for PO authorization process
 *ABF_R_200         20220511  KVERCO      Update for XtendM3 review feedback
 *ABF_R_200         20220603  KVERCO      Bug fixing - populate XXCONO
 *WKF013            20240318  KVERCO      Add PURC field - as at time of approval workflow
 *
*/

public class Get extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  //Input fields
  private String puno;
  
  private int XXCONO;
   
 /*
  * Get Purchase Authorisation extension table row
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
  	puno = mi.inData.get("PUNO") == null ? '' : mi.inData.get("PUNO").trim();
  	if (puno == "?") {
  	  puno = "";
  	}
    if (puno.isEmpty()) {
      mi.error("PO number must be entered");
      return;
    }
    XXCONO = (Integer)program.LDAZD.CONO;
    
    DBAction query = database.table("EXTAPR").index("00").selection("EXCONO", "EXPUNO", "EXAPPR", "EXASTS", "EXPURC").build();
    DBContainer container = query.getContainer();
    container.set("EXCONO", XXCONO);
    container.set("EXPUNO", puno);
    if (query.read(container)) {
      mi.outData.put("CONO", XXCONO.toString());
      mi.outData.put("PUNO", container.get("EXPUNO").toString());
      mi.outData.put("APPR", container.get("EXAPPR").toString());
      mi.outData.put("ASTS", container.get("EXASTS").toString());
      mi.outData.put("PURC", container.get("EXPURC").toString());
      mi.write();
    } else {
      mi.error("Record does not exist in EXTAPR.");
      return;
    }
  }
  
}
