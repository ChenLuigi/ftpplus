// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.net.libswork;


import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.testng.Assert;
import org.testng.annotations.Test;
import ru.vachok.networker.ConstantsFor;

import java.awt.*;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.AccessDeniedException;


public class RegRuFTPLibsUploaderTest extends RegRuFTPLibsUploader {
    
    
    @Test()
    public void ftpTest() {
        LibsHelp libsHelp = new RegRuFTPLibsUploader();
        try {
            libsHelp.uploadLibs();
        }
        catch (AccessDeniedException | ConnectException e) {
            System.err.println(e.getMessage() + " " + getClass().getSimpleName() + ".ftpTest");
        }
    }
    
    @Test
    public void continueConnect() {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(getHost(), ConstantsFor.FTP_PORT);
            FTPClientConfig config = new FTPClientConfig();
            config.setServerTimeZoneId("Europe/Moscow");
            ftpClient.configure(config);
        }
        catch (IOException e) {
            Assert.assertNull(e, e.getMessage());
        }
    }
    
    @Test
    public void testRun1() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testUploadLibs1() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testGetContentsQueue1() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testSetUploadDirectoryStr1() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testGetUploadDirectoryStr1() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testGetLibFiles1() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testUploadToServer1() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testUploadToServer2() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testGetHost1() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
}