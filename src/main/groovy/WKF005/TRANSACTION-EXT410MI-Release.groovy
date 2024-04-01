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
 *Nbr               Date      User id     Description
 *WKF005            20231013  KVERCO      Invoke the workflow for Physical Inventory PO authorization process
 *
 */

/**
* - Start Physical Inventory Approval workflow
*/

public class Release extends ExtendM3Transaction {

  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  //Input fields
  private String dlix;
  private String XXCONO;
  private String resp;
 

  public Release(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
  	this.miCaller = miCaller;
  	this.logger = logger;
  	this.program = program;
	  this.ion = ion;
	  
  }

  public void main() {
  	dlix = mi.inData.get("DLIX") == null ? '' : mi.inData.get("DLIX").trim();
  	if (dlix == "?") {
  	  dlix = "";
  	}     
		XXCONO = (String)program.LDAZD.CONO;
		resp = program.getUser();
		
    // Validate input fields  	
		if (dlix.isEmpty()) {
      mi.error("Delivery Index must be entered");
      return;
    }
    
    // - validate dlix
    DBAction queryMHDISH = database.table("MHDISH").index("00").selection("OQDLIX").build();
    DBContainer MHDISH = queryMHDISH.getContainer();
    MHDISH.set("OQCONO", XXCONO.toInteger());
    MHDISH.set("OQINOU", 1);
    MHDISH.set("OQDLIX", dlix.toInteger());
    if (!queryMHDISH.read(MHDISH)) {
      mi.error("Delivery Index is invalid." + XXCONO + " DLIX= " + dlix);
      return;
    }        
    
  	releasePicking(dlix);
  }

  /**
  * Release for picking via Physical Inventory Approval workflow
  */
  void releasePicking(String dlix) {
  
    Map<String,String> headers = ["Accept":"application/json","Content-Type":"application/json"];
    Map<String,String> params = null;
    String url = "IONSERVICES/process/application/v1/workflow/start?logicalId=lid%3A%2F%2Finfor.m3.m3";
    String body =  '{"workflowName": "Physical_Inventory_Approval", "instanceName": "Physical_Inventory_Approval", "inputVariables": [{ "name": "CONO", "dataType": "STRING",  "value": "' + XXCONO + '" }, { "name": "DLIX", "dataType": "STRING",  "value": "' + dlix + '" },  { "name": "RESP",  "dataType": "STRING",   "value": "' + resp + '"  }]}' 

    logger.debug("body:" + body);
    IonResponse response = ion.post(url, headers, params, body);

 	  if (response.getError()) {
      return
    }
    
    if (response.getStatusCode() != 200) {
      return
    }

    String content = response.getContent()
    if (content != null) {
      return
    }
  }
  	
}
