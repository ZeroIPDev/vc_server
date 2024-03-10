package vc.ext.account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class search extends BaseClientRequestHandler {
    	@Override
    	public void handleClientRequest(User u, ISFSObject p) {
    		//Retrieve variables from user
            String uID = p.getUtfString("ID");
            String uPlatform = p.getUtfString("Platform");
            
            //Verify Steam ID
            boolean doSearch = false;
            if(uPlatform.contentEquals("Steam")) {
            	doSearch = true;
            }
            
            if(doSearch) {
            	//Variables
                Connection connection;
                IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
                
                //Build and execute query
                try {
                	connection = dbManager.getConnection();
                	PreparedStatement stmt = connection.prepareStatement("SELECT ID,Name,Pass FROM vc_accounts WHERE "+ uPlatform +"=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                	stmt.setString(1, uID);
                	ResultSet res = stmt.executeQuery();
                	if(res.first()) { //Account found
                		int ID = res.getInt("ID");
                		String uName = res.getString("Name");
                		String uPass = res.getString("Pass");
                		sendResult(u, ID, uName, uPass);
                	} else { //No account
                		sendError(u, "No account found");
                	}
                	connection.close();
                }
                catch(SQLException e) {
                	sendError(u, e.getMessage());
                	trace("SQL Error: ", e);
                }
            } else {
            	sendError(u, "No account found");
            }
    	}
    	public int getIDFromLogin(String n, String p, Connection connection) throws SQLException {
    		int ID = -1;
    		PreparedStatement stmt = connection.prepareStatement("SELECT ID FROM vc_accounts WHERE Name=? AND Pass=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
    		stmt.setString(1, n);
    		stmt.setString(2, p);
    		ResultSet res = stmt.executeQuery();
    		if(res.first()) {
    			ID = res.getInt("ID");
    		}
    		connection.close();
    		return ID;
    	}
    	private void sendResult(User u, int i, String n, String p) {
    		ISFSObject resObj = new SFSObject();
    		resObj.putBool("Success", true);
    		resObj.putInt("ID", i);
    		resObj.putUtfString("Name", n);
    		resObj.putUtfString("Pass", p);
    		send("search", resObj, u); //Send result to client
    	}
    	private void sendError(User u, String e) {
			ISFSObject resObj = new SFSObject();
			resObj.putBool("Success", false);
			resObj.putUtfString("Error", e);
			send("search", resObj, u); //Send result to client
		}
}