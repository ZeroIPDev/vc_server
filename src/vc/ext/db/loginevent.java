package vc.ext.db;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.exceptions.SFSErrorCode;
import com.smartfoxserver.v2.exceptions.SFSErrorData;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSLoginException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class loginevent extends BaseServerEventHandler {
	@Override
	public void handleServerEvent(ISFSEvent e) throws SFSException {
		//Get login data from client
		String uName = (String) e.getParameter(SFSEventParam.LOGIN_NAME);
		String uPass = (String) e.getParameter(SFSEventParam.LOGIN_PASSWORD);
        ISession session = (ISession) e.getParameter(SFSEventParam.SESSION);
        
        //Variables
        Connection connection;
        IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
        
        //Build and execute query
        try {
        	connection = dbManager.getConnection();
            PreparedStatement stmt = connection.prepareStatement("SELECT Pass FROM vc_accounts WHERE BINARY Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, uName);
            ResultSet res = stmt.executeQuery();
            if(res.first()) {
            	if(!getApi().checkSecurePassword(session, res.getString("Pass"), uPass)) {
            		SFSErrorData errPsw = new SFSErrorData(SFSErrorCode.LOGIN_BAD_PASSWORD);
            		errPsw.addParameter(uName);
            		throw new SFSLoginException("Error: Password mismatch", errPsw);
            	}
            } else {
            	SFSErrorData errUser = new SFSErrorData(SFSErrorCode.LOGIN_BAD_USERNAME);
            	errUser.addParameter(uName);
            	throw new SFSLoginException("Error: Cannot match user info for " + uName, errUser);
            }
            connection.close();
        }
        catch(SQLException err) {
        	throw new SFSLoginException("Error: " + err);
        }
	}
	
}
