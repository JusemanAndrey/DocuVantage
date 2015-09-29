package com.docuvantage.andhelper.util;

import com.dv.edm.api.constants.Constants;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class EmbedUtil {
    
    static String ACTION_CREATE_RECORD = "createRecord";
    static String ACTION_MAIN = "main";

    public static String makeCreateRecordUrl(String appUrl, String webSessionId, long archiveId, long objectId, String objectName, char objectType) throws Exception {
        Map <String, String> parms = new HashMap<String, String>();
        parms.put("objectId", "" + objectId);
        parms.put("objectName", objectName);
        parms.put("objectType", "" + objectType);
        parms.put("archiveId", "" + archiveId);
        return makeEmbedUrl(appUrl, webSessionId, ACTION_CREATE_RECORD, parms);
    }
    
    public static String makeAppMainUrl(String appUrl, String webSessionId) throws Exception { 
        Map <String, String> parms = new HashMap<String, String>();
        return makeEmbedUrl(appUrl, webSessionId, ACTION_MAIN, parms);
    }

    private static String makeEmbedUrl(String appUrl, String webSessionId, String action, Map<String, String> parms) throws Exception {

        StringBuilder str = new StringBuilder();

        str.append(appUrl);
        str.append("Embed.htm?a=");
        str.append(action);

        // make a local copy so we can add our own values
        Map<String, String> myParms = new HashMap<String, String>();
        for (Map.Entry<String, String> p : parms.entrySet()) {
            myParms.put(p.getKey(), p.getValue());
        }

        // add webSessionId to Map
        myParms.put("webSessionId", webSessionId);

        // append parms to query String
        if (myParms.size() > 0) {
            for (Map.Entry<String, String> parm : myParms.entrySet()) {
                str.append('&');
                str.append(parm.getKey());
                str.append('=');
                str.append(URLEncoder.encode(parm.getValue(), "UTF-8"));
            }
        }
        return str.toString();
    }    
    
}
