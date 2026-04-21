package com.cube.qa.framework.config;

public class TestConfig {
    private String platform;
    private String buildPath;
    private String udid;
    private boolean fullReset;
    private String env;
    private boolean isSimulator;
    private String deviceName;
    private String platformVersion;
    
    // App-specific configuration fields for performance optimization
    private String androidPackageName;
    private String iosBundleId;
    private String iosXcodeOrgId;
    private String iosXcodeSigningId;
    private String iosWdaBundleId;

    public TestConfig(String platform, String buildPath, String udid, boolean fullReset, String env, 
                     boolean isSimulator, String deviceName, String platformVersion,
                     String androidPackageName, String iosBundleId, String iosXcodeOrgId,
                     String iosXcodeSigningId, String iosWdaBundleId) {
        this.platform = platform;
        this.buildPath = buildPath;
        this.udid = udid;
        this.fullReset = fullReset;
        this.env = env;
        this.isSimulator = isSimulator;
        this.deviceName = deviceName;
        this.platformVersion = platformVersion;
        this.androidPackageName = androidPackageName;
        this.iosBundleId = iosBundleId;
        this.iosXcodeOrgId = iosXcodeOrgId;
        this.iosXcodeSigningId = iosXcodeSigningId;
        this.iosWdaBundleId = iosWdaBundleId;
    }

    public String getPlatform() { return platform; }
    public String getBuildPath() { return buildPath; }
    public String getUdid() { return udid; }
    public boolean isFullReset() { return fullReset; }
    public String getEnv() { return env; }
    public boolean isSimulator() { return isSimulator; }
    public String getDeviceName() { return deviceName; }
    public String getPlatformVersion() { return platformVersion; }
    
    // App-specific configuration getters
    public String getAndroidPackageName() { return androidPackageName; }
    public String getIosBundleId() { return iosBundleId; }
    public String getIosXcodeOrgId() { return iosXcodeOrgId; }
    public String getIosXcodeSigningId() { return iosXcodeSigningId; }
    public String getIosWdaBundleId() { return iosWdaBundleId; }
}
