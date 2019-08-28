// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.ad.pc;


import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ru.vachok.networker.configuretests.TestConfigure;
import ru.vachok.networker.configuretests.TestConfigureThreadsLogMaker;
import ru.vachok.networker.net.scanner.NetListsTest;


/**
 @see DBPCHTMLInfo
 @since 18.08.2019 (23:41) */
public class DBPCHTMLInfoTest {
    
    
    private static final TestConfigure TEST_CONFIGURE_THREADS_LOG_MAKER = new TestConfigureThreadsLogMaker(NetListsTest.class.getSimpleName(), System.nanoTime());
    
    private DBPCHTMLInfo dbpchtmlInfo = new DBPCHTMLInfo();
    
    @BeforeClass
    public void setUp() {
        Thread.currentThread().setName(getClass().getSimpleName().substring(0, 5));
        TEST_CONFIGURE_THREADS_LOG_MAKER.before();
        dbpchtmlInfo.setClassOption("do0213");
    }
    
    @AfterClass
    public void tearDown() {
        TEST_CONFIGURE_THREADS_LOG_MAKER.after();
    }
    
    @Test
    public void testTestToString() {
        String do0213 = dbpchtmlInfo.toString();
        Assert.assertTrue(do0213.contains("DBPCInfo{"), do0213);
    }
    
    @Test
    public void testFillWebModel() {
        String fillWebModel = dbpchtmlInfo.fillWebModel();
        Assert.assertEquals(fillWebModel, "<p><p>");
    }
    
    @Test
    public void testFillAttribute() {
        String fillAttributeStr = dbpchtmlInfo.fillAttribute("do0001");
        Assert.assertTrue(fillAttributeStr.contains("Крайнее имя пользователя на ПК do0001.eatmeat.ru - estrelyaeva"), fillAttributeStr);
    }
    
    @Test
    public void testSetClassOption() {
        dbpchtmlInfo.setClassOption("do0213");
        Assert.assertTrue(dbpchtmlInfo.toString().contains("DBPCInfo{pcName='do0213'"), dbpchtmlInfo.toString());
    }
    
    @Test
    public void testLastOnline() {
        String firstOnline = dbpchtmlInfo.firstOnline();
        Assert.assertTrue(firstOnline.contains("2019-06-03"), firstOnline);
    }
    
    @Test
    public void testCountOnOff() {
        String countOnOff = dbpchtmlInfo.countOnOff();
        Assert.assertTrue(countOnOff.contains("ffline times and"), countOnOff);
        Assert.assertTrue(countOnOff.contains("online times"), countOnOff);
    }
}