// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.controller;


import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.TForms;
import ru.vachok.networker.accesscontrol.MatrixSRV;
import ru.vachok.networker.configuretests.TestConfigureThreadsLogMaker;
import ru.vachok.networker.enums.ModelAttributeNames;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.testng.Assert.*;


/**
 @since 14.06.2019 (14:10) */
public class MatrixCtrTest {
    
    
    private final TestConfigureThreadsLogMaker testConfigureThreadsLogMaker = new TestConfigureThreadsLogMaker(getClass().getSimpleName(), System.nanoTime());
    
    @BeforeClass
    public void setUp() {
        Thread.currentThread().setName(getClass().getSimpleName().substring(0, 6));
        testConfigureThreadsLogMaker.before();
    }
    
    @AfterClass
    public void tearDown() {
        testConfigureThreadsLogMaker.after();
    }
    
    
    @Test
    public void testSetCurrentProvider() {
        MatrixSRV matrixSRV = new MatrixSRV();
        MatrixCtr matrixCtr = new MatrixCtr(matrixSRV);
        MatrixCtr.setCurrentProvider();
        String currentProvider = matrixCtr.getCurrentProvider();
        assertFalse(currentProvider.isEmpty());
        assertNotNull(matrixCtr.toString());
    }
    
    @Test
    public void testGetFirst() {
        MatrixSRV matrixSRV = new MatrixSRV();
        MatrixCtr matrixCtr = new MatrixCtr(matrixSRV);
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();
        HttpServletResponse response = new MockHttpServletResponse();
        
        String matrixCtrFirst = matrixCtr.getFirst(httpServletRequest, model, response);
        assertTrue(matrixCtrFirst.equals("starting"), matrixCtrFirst + " is wrong!");
        assertTrue(response.getHeader("Refresh").equals("120"), new TForms().fromArray(response.getHeaders("Refresh"), false));
    }
    
    @Test
    public void testGetWorkPosition() {
        MatrixSRV matrixSRV = new MatrixSRV();
        MatrixCtr matrixCtr = new MatrixCtr(matrixSRV);
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();
        HttpServletResponse response = new MockHttpServletResponse();
        
        matrixSRV.setWorkPos("адми");
        String matrixCtrWorkPosition = matrixCtr.getWorkPosition(matrixSRV, model);
        assertEquals(matrixCtrWorkPosition, "ok");
        assertTrue(model.asMap().size() >= 1);
        assertTrue(model.asMap().get("ok").toString().contains("адми"));
    }
    
    @Test
    public void testShowResults() {
        MatrixSRV matrixSRV = new MatrixSRV();
        MatrixCtr matrixCtr = new MatrixCtr(matrixSRV);
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();
        HttpServletResponse response = new MockHttpServletResponse();
        
        try {
            String showResultsStr = matrixCtr.showResults(httpServletRequest, response, model);
            assertTrue(showResultsStr.equals(ConstantsFor.BEANNAME_MATRIX));
            assertTrue(response.getStatus() == 200);
            assertTrue(showResultsStr.equals("matrix"));
            assertTrue(model.asMap().get(ModelAttributeNames.WORKPOS).toString().equals("whois: ya.ru"));
        }
        catch (IOException e) {
            assertNull(e, e.getMessage());
        }
    }
}