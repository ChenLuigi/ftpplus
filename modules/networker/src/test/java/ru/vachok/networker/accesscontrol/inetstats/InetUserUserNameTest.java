// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.accesscontrol.inetstats;


import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ru.vachok.networker.configuretests.TestConfigure;
import ru.vachok.networker.configuretests.TestConfigureThreadsLogMaker;
import ru.vachok.networker.info.InformationFactory;
import ru.vachok.networker.net.scanner.NetListsTest;


/**
 @see InetUserUserName
 @since 17.08.2019 (15:34) */
public class InetUserUserNameTest {
    
    
    private static final TestConfigure TEST_CONFIGURE_THREADS_LOG_MAKER = new TestConfigureThreadsLogMaker(NetListsTest.class.getSimpleName(), System.nanoTime());
    
    private InformationFactory informationFactory = InformationFactory.getInstance(InformationFactory.INET_USAGE);
    
    @BeforeClass
    public void setUp() {
        Thread.currentThread().setName(getClass().getSimpleName().substring(0, 5));
        TEST_CONFIGURE_THREADS_LOG_MAKER.before();
    }
    
    @AfterClass
    public void tearDown() {
        TEST_CONFIGURE_THREADS_LOG_MAKER.after();
    }
    
    @Test
    public void testGetInfoAbout() {
        String infoAbout = informationFactory.getInfoAbout("e.v.vinokur");
        Assert.assertTrue(infoAbout.contains("<p>"), infoAbout);
        Assert.assertTrue(infoAbout.contains("мегабайт трафика"), infoAbout);
    }
    
    @Test
    public void testTestToString() {
        String toStr = informationFactory.toString();
        System.out.println("informationFactory = " + toStr);
    }
}