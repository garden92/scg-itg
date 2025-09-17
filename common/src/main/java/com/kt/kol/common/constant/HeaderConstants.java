package com.kt.kol.common.constant;

public final class HeaderConstants {

    // ====== KOL Constants ======
    public static final String KN_CHNL_TYPE = "KN";
    public static final String KN_USER_ID = "91383041";
    public static final String KN_ORG_ID = "SPT8050";

    // ====== CommonHeader Constants ======
    public static final String COMM_TR_FLAG_THROW = "T";
    public static final String COMM_TR_FLAG_RECEIVE = "R";

    // ====== Application-specific headers ======
    public static final String APP_NAME = "X-App-Name";
    public static final String SVC_NAME = "X-Svc-Name";
    public static final String FN_NAME = "X-Fn-Name";
    public static final String GLOBAL_NO = "KOL-Global-No";
    public static final String CHANNEL_TYPE = "KOL-Chnl-Type";
    public static final String TRANSACTION_FLAG = "X-Transaction-Flag";

    // ====== Common transactional and logging headers ======
    public static final String TRANSACTION_DATE = "X-Transaction-Date";
    public static final String TRANSACTION_TIME = "X-Transaction-Time";
    public static final String CLIENT_IP = "X-Client-IP";
    public static final String USER_ID = "KOL-User-Id";
    public static final String REAL_USER_ID = "X-Real-User-ID";
    public static final String ORGANIZATION_ID = "KOL-Org-Id";
    public static final String SOURCE_ID = "KOL-Src-Id";
    public static final String CMPN_CD = "KOL-Cmpn-Cd";
    public static final String LOG_DATETIME = "KOL-Lg-Date-Time";
    public static final String ORI_IP = "KOL-Ori-IP"; // 최초 RemoteAddr 보관

    // ====== Prevent Instantiation ======
    private HeaderConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
