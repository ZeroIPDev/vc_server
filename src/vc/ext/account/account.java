package vc.ext.account;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class account extends SFSExtension {
	public static boolean enable_steam_ids = false;
    @Override
    public void init() {
        addRequestHandler("search", search.class);
        addRequestHandler("recover", recover.class);
        addRequestHandler("creation", creation.class);
    }
    @Override
    public void destroy() {
    	super.destroy();
    }
}