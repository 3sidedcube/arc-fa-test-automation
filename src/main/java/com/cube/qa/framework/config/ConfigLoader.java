package com.cube.qa.framework.config;

public class ConfigLoader {
    public static TestConfig load(String platformFromXml, String buildFromXml, String udidFromXml,
                                  String fullResetFromXml, String envFromXml, String isSimulatorFromXml, 
                                  String deviceNameFromXml, String platformVersionFromXml,
                                  String androidPackageNameFromXml, String iosBundleIdFromXml,
                                  String iosXcodeOrgIdFromXml, String iosXcodeSigningIdFromXml,
                                  String iosWdaBundleIdFromXml) {

        String platform = System.getProperty("platform", platformFromXml);
        String buildPath = System.getProperty("build", buildFromXml);
        String udid = System.getProperty("udid", udidFromXml);
        boolean fullReset = Boolean.parseBoolean(System.getProperty("fullReset", fullResetFromXml));
        String env = System.getProperty("env", envFromXml);
        boolean isSimulator = Boolean.parseBoolean(System.getProperty("isSimulator", isSimulatorFromXml));
        String deviceName = System.getProperty("deviceName", deviceNameFromXml);
        String platformVersion = System.getProperty("platformVersion", platformVersionFromXml);
        
        // App-specific parameters for performance optimization
        String androidPackageName = System.getProperty("androidPackageName", androidPackageNameFromXml);
        String iosBundleId = System.getProperty("iosBundleId", iosBundleIdFromXml);
        String iosXcodeOrgId = System.getProperty("iosXcodeOrgId", iosXcodeOrgIdFromXml);
        String iosXcodeSigningId = System.getProperty("iosXcodeSigningId", iosXcodeSigningIdFromXml);
        String iosWdaBundleId = System.getProperty("iosWdaBundleId", iosWdaBundleIdFromXml);

        return new TestConfig(platform, buildPath, udid, fullReset, env, isSimulator, deviceName, platformVersion,
                            androidPackageName, iosBundleId, iosXcodeOrgId, iosXcodeSigningId, iosWdaBundleId);
    }

}