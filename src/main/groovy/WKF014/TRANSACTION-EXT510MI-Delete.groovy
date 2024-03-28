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
 /*
 *Modification area - M3
 *Jira Nbr    Date      User id   Description
 *WKF014      20240311  KVERCO    Developed WKF014 - Delete EXTAPA auti Trail record 
 *
 */

public class Delete extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  /* Input fields */
  private String cono;
  private int xxCONO;
  private String divi;
  private String prpn;
  private String pyon;
  private String lmts;
  
 /**
  * Delete Approval Payment Proposal extension table row
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
  	divi = mi.inData.get("DIVI") == null ? '' : mi.inData.get("DIVI").trim();
    if (divi == "?") {
      divi = "";
    }
    if (divi.isEmpty()) {
      divi = program.LDAZD.DIVI;
    } 
  	prpn = mi.inData.get("PRPN") == null ? '' : mi.inData.get("PRPN").trim();
    if (prpn.isEmpty()) {
      mi.error("Payment Proposal Number must be entered");
      return;
    }
    pyon = mi.inData.get("PYON") == null ? '' : mi.inData.get("PYON").trim();
    if (pyon.isEmpty()) {
      mi.error("Payment Order Number must be entered");
      return;
    }
  	lmts = mi.inData.get("LMTS") == null ? '' : mi.inData.get("LMTS").trim();
    if (lmts.isEmpty()) {
      mi.error("Time stamp must be entered");
      return;
    }
    /* Delete Payment Proposal Number record if exists */
    logger.debug("About to Delete record in the database but will do it while checking if record exits first");
  	DBAction queryEXTAPA = database.table("EXTAPA").index("00").build();
    DBContainer EXTAPA = queryEXTAPA.getContainer();
    EXTAPA.set("EXCONO", xxCONO);
    EXTAPA.set("EXDIVI", divi);
    EXTAPA.set("EXPRPN", Long.parseLong(prpn));
    EXTAPA.set("EXPYON", Integer.parseInt(pyon));
    EXTAPA.set("EXLMTS", lmts);
    if (!queryEXTAPA.readLock(EXTAPA, deleteCallBack)) {
      mi.error("An approval audit trail entry does not exist for this transaction.");
      return;
    }
  }
  
  /**
   * deleteCallBack - Callback function to delete EXTAPP table
   *
  */
  Closure<?> deleteCallBack = { LockedResult lockedResult ->

    lockedResult.delete(); 
   
  }
  
}
