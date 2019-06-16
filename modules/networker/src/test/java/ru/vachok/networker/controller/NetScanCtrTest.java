// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.controller;


import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.testng.Assert;
import org.testng.annotations.Test;
import ru.vachok.networker.AppComponents;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.net.NetPinger;
import ru.vachok.networker.net.enums.ConstantsNet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import static org.testng.Assert.assertTrue;


public class NetScanCtrTest {
    
    
    @Test
    public void testNetScan() {
        NetScanCtr netScanCtr = null;
        try {
            netScanCtr = new NetScanCtr(AppComponents.netScannerSvc(), new NetPinger(), new AppComponents().scanOnline());
        }
        catch (RejectedExecutionException e) {
            Assert.assertNotNull(e, e.getMessage());
        }
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        Model model = new ExtendedModelMap();
        
        String netScanStr = netScanCtr.netScan(request, response, model);
        Assert.assertNotNull(netScanStr);
        assertTrue(netScanStr.equals(ConstantsNet.ATT_NETSCAN));
        assertTrue(model.asMap().size() >= 7, showModel(model.asMap()));
        assertTrue(model.asMap().get(ConstantsFor.ATT_FOOTER).toString().contains("Only Allow Domains"), showModel(model.asMap()));
    }
    
    private String showModel(Map<String, Object> map) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            stringBuilder.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        return stringBuilder.toString();
    }
    
    @Test
    public void testPingAddr() {
        Model model = new ExtendedModelMap();
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        try {
            String pingAddrString = new NetScanCtr(AppComponents.netScannerSvc(), new NetPinger(), new AppComponents().scanOnline()).pingAddr(model, request, response);
            Assert.assertTrue(pingAddrString.contains("ping"));
            Assert.assertTrue(model.asMap().get("pingResult").toString().contains("Pinger is start at"));
        }
        catch (RejectedExecutionException e) {
            Assert.assertNotNull(e, e.getMessage());
        }
    }
    
    @Test
    public void testPingPost() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testPcNameForInfo() {
        Model model = new ExtendedModelMap();
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        try {
            String pcNameInfoStr = NetScanCtr.pcNameForInfo(AppComponents.netScannerSvc(), model);
            Assert.assertTrue(pcNameInfoStr.equals("redirect:/ad?PC"));
        
        }
        catch (RejectedExecutionException e) {
            Assert.assertNotNull(e, e.getMessage());
        }
    }
    
    @Test
    public void testAllDevices() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
    
    @Test
    public void testScanIt() {
        throw new IllegalComponentStateException("15.06.2019 (17:36)");
    }
}