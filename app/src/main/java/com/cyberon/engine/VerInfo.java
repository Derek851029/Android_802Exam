package com.cyberon.engine;

public class VerInfo {
    public static final int Type_SDKName = 0;
    public static final int Type_SDKVersion = 1;
    public static final int Type_SDKType = 2;
    public static final int Type_ReleaseDate = 3;
    public static final int Type_LicenseType = 4;
    private String strSDKName = "";
    private String strSDKVersion = "";
    private String strSDKType = "";
    private String strReleaseDate = "";
    private String strLicenseType = "";
    private boolean bTrialVersion = true;

    /**
     * Get SDK version information.
     *
     * @param nType [in] There are five types: Type_SDKName, Type_SDKVersion, Type_SDKType, Type_ReleaseDate, Type_LicenseType.
     */
    public String getVerInfo(int nType) {
        switch (nType) {
            case Type_SDKName:
                return strSDKName;
            case Type_SDKVersion:
                return strSDKVersion;
            case Type_SDKType:
                return strSDKType;
            case Type_ReleaseDate:
                return strReleaseDate;
            case Type_LicenseType:
                return strLicenseType;
            default:
                return null;
        }
    }

    public boolean isTrialVersion() {
        return bTrialVersion;
    }
}
