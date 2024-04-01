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
 *Nbr             Date      User id     Description
 *WKF010          20231220  KVERCO      Write/Update EXTOOH records as a basis for CO authorization process
 *
 */

 /**
  * Delete CO Authorisation extension table row
 */
 public class Delete extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;

  //Input fields
  private String orno;
  private int XXCONO;
  
 /*
  * Delete CO Authorisation extension table row
 */
  public Delete(MIAPI mi, DatabaseAPI database, ProgramAPI program) {
    this.mi = mi;
    this.database = database;
  	this.program = program;
  }
  
  public void main() {
  	orno = mi.inData.get("ORNO") == null ? '' : mi.inData.get("ORNO").trim();
  	if (orno == "?") {
  	  orno = "";
  	}
  	// Validate input fields
  	if (orno.isEmpty()) {
      mi.error("CO number must be entered");
      return;
    }
    XXCONO = (Integer)program.getLDAZD().CONO;
    DBAction queryEXTOOH = database.table("EXTOOH").index("00").build();
    DBContainer EXTOOH = queryEXTOOH.getContainer();
    EXTOOH.set("EXCONO", XXCONO);
    EXTOOH.set("EXORNO", orno);
    if (!queryEXTOOH.readLock(EXTOOH, deleteCallBack)) {
      mi.error("Record does not exist");
      return;
    }
  }
  
  /**
   * deleteCallBack - Callback function to delete EXTOOH table
   *
  */
  Closure<?> deleteCallBack = { LockedResult lockedResult ->
    lockedResult.delete();
  }
  
}
