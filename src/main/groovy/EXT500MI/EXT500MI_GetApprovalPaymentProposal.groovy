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
 *Jira Nbr          Date      User id       Description
 *MGMCAW-1756       20240311  TTATAROGLOU   Developed WKF014- Get to get table row from table EXTAPP the Approval Payment
 *                                          Proposal record as a basis for Payments Approval (Payments) Change Request.
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
  private String cono;
  private int xxCONO;
  private String divi;
  private String prpn;
  private String pyon;
   
 /*
  * Get Approval Payment Proposal table row
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
  	logger.debug("main() Start");
  	/* Validate input fields */
    cono = mi.inData.get("CONO") == null ? '' : mi.inData.get("CONO").trim();
  	if (cono == "?") {
  	  cono = "";
  	}
    if (!cono.isEmpty()) {
		  if (cono.isInteger()){
			  xxCONO = cono.toInteger();
			} else {
				mi.error("Company " + cono + " is invalid");
				return;
		  }
		} else {
			xxCONO = program.LDAZD.CONO;
		}
    logger.debug("cono=" + cono + " xxCONO=" + xxCONO);
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
    if (prpn.isEmpty()) {
      mi.error("Payment Proposal Number must be entered.");
      return;
    }
    if (pyon.isEmpty()) {
      mi.error("Payment Order Number must be entered.");
      return;
    }
    
    /* Search for record */
    logger.debug("Validation passed will now query EXTAPP table");
    DBAction query = database.table("EXTAPP").index("00").selection("EXASTS", "EXAPPR", "EXGRPI", "EXAPAM", "EXDATE").build();
    DBContainer container = query.getContainer();
    container.set("EXCONO", xxCONO);
    container.set("EXDIVI", divi);
    container.set("EXPRPN", Integer.parseInt(prpn));
    container.set("EXPYON", Integer.parseInt(pyon));
    if (query.read(container)) {
      mi.outData.put("CONO", xxCONO.toString());
      mi.outData.put("DIVI", divi);
      mi.outData.put("PRPN", prpn);
      mi.outData.put("PYON", pyon);
      logger.debug("container.get(EXASTS).toString()=" + container.get("EXASTS").toString());
      logger.debug("container.get(EXAPPR).toString()=" + container.get("EXAPPR").toString());
      logger.debug("container.get(EXGRPI).toString()=" + container.get("EXGRPI").toString());
      mi.outData.put("ASTS", container.get("EXASTS").toString());
      mi.outData.put("APPR", container.get("EXAPPR").toString());
      mi.outData.put("GRPI", container.get("EXGRPI").toString());
      mi.outData.put("APAM", container.get("EXAPAM").toString());
      mi.outData.put("DATE", container.get("EXDATE").toString());
      mi.write();
    } else {
      mi.error("An approval entry does not exist for this transaction.");
      return;
    }
  }
  
}