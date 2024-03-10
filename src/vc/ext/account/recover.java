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

public class recover extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		//Retrieve variables from user
        String uName = p.getUtfString("Name");
        String uPass = p.getUtfString("Pass");
        
        //Variables
        Connection connection;
        IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
        
        //Build and execute query
        try {
        	connection = dbManager.getConnection();
        	PreparedStatement stmt = connection.prepareStatement("SELECT ID FROM vc_accounts WHERE BINARY Name=? AND BINARY Pass=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        	stmt.setString(1, uName);
        	stmt.setString(2, uPass);
        	ResultSet res = stmt.executeQuery();
        	if(res.first()) { //Correct U/P, send to user
        		int ID = res.getInt("ID");
        		sendResult(u, ID, uName, uPass);
        	} else { //Incorrect U/P, send error
        		sendError(u, "Incorrect username or password");
        	}
        	connection.close();
        }
        catch(SQLException e) {
        	sendError(u, e.getMessage());
        	trace("SQL Error: ", e);
        }
	}
	private void sendResult(User u, int i, String n, String p) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putInt("ID", i);
		resObj.putUtfString("Name", n);
		resObj.putUtfString("Pass", p);
		send("recover", resObj, u); //Send result to client
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("recover", resObj, u);
	}
}