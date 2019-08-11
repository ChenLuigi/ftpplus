// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.fileworks;


import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.TForms;
import ru.vachok.networker.componentsrepo.exceptions.TODOException;
import ru.vachok.networker.configuretests.TestConfigure;
import ru.vachok.networker.configuretests.TestConfigureThreadsLogMaker;
import ru.vachok.networker.restapi.MessageToUser;
import ru.vachok.networker.restapi.fsworks.UpakFiles;
import ru.vachok.networker.restapi.message.MessageLocal;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 @see FileSystemWorker
 @since 23.06.2019 (9:44) */
@SuppressWarnings("ALL")
public class FileSystemWorkerTest extends SimpleFileVisitor<Path> {
    
    
    private final TestConfigure testConfigureThreadsLogMaker = new TestConfigureThreadsLogMaker(getClass().getSimpleName(), System.nanoTime());
    
    private String testRootPath = Paths.get(ConstantsFor.ROOT_PATH_WITH_SEPARATOR + "tmp").toAbsolutePath().normalize()
        .toString() + ConstantsFor.FILESYSTEM_SEPARATOR;
    
    private MessageToUser messageToUser = new MessageLocal(this.getClass().getSimpleName());
    
    @BeforeClass
    public void setUp() {
        Thread.currentThread().setName(getClass().getSimpleName().substring(0, 6));
        testConfigureThreadsLogMaker.before();
        if (Paths.get(testRootPath).toAbsolutePath().normalize().toFile().exists() || Paths.get(testRootPath).toAbsolutePath().normalize().toFile().isDirectory()) {
            System.out.println("testRootPath = " + testRootPath);
        
        }
        else {
            try {
                Files.createDirectories(Paths.get(testRootPath));
            }
            catch (IOException e) {
                messageToUser.error(MessageFormat.format("FileSystemWorkerTest.setUp says: {0}. Parameters: \n[]: {1}", e.getMessage(), new TForms().fromArray(e)));
            }
        }
    }
    
    @AfterClass
    public void tearDown() {
        testConfigureThreadsLogMaker.after();
    }
    /**
     @see FileSystemWorker#countStringsInFile(Path)
     */
    @Test
    public void testCountStringsInFile() {
        String fileSeparator = System.getProperty("file.separator");
        Path fileToCount = Paths.get(ConstantsFor.ROOT_PATH_WITH_SEPARATOR + "inetstats\\192.168.13.220.csv").toAbsolutePath().normalize();
        final long startNano = System.nanoTime();
        int stringsInCommonOwn = FileSystemWorker.countStringsInFile(fileToCount);
        final long endNano = System.nanoTime();
        Assert.assertTrue(stringsInCommonOwn > 50, MessageFormat.format("{0} strings in {1}", stringsInCommonOwn, fileToCount.toFile().getName()));
        long nanoElapsed = endNano - startNano;
        Assert.assertTrue((nanoElapsed < 26_927_200_499L), String.valueOf(nanoElapsed));
        try {
            testConfigureThreadsLogMaker.getPrintStream().println(MessageFormat.format("Standart = {0} nanos", nanoElapsed));
        }
        catch (IOException e) {
            Assert.assertNull(e, e.getMessage() + "\n" + new TForms().fromArray(e));
        }
        System.out.println("stringsInCommonOwn = " + stringsInCommonOwn);
    }
    
    @Test(enabled = false)
    public void countStringsInFileAsStream() {
        Path fileToCount = Paths.get(ConstantsFor.ROOT_PATH_WITH_SEPARATOR + "tmp\\common.own");
        final long startNano = System.nanoTime();
        try (InputStream inputStream = new FileInputStream(fileToCount.toAbsolutePath().normalize().toString());
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            long count = bufferedReader.lines().count();
        }
        catch (IOException e) {
            messageToUser.error(MessageFormat.format("FileSystemWorker.countStringsInFileAsStream: {0}, ({1})", e.getMessage(), e.getClass().getName()));
        }
        final long endNano = System.nanoTime();
        
        long nanoElapsed = endNano - startNano;
        System.out.println(MessageFormat.format("AsStream = {0} nanos", nanoElapsed));
        try {
            testConfigureThreadsLogMaker.getPrintStream().println(MessageFormat.format("Standart = {0} nanos", nanoElapsed));
        }
        catch (IOException e) {
            Assert.assertNull(e, e.getMessage() + "\n" + new TForms().fromArray(e));
        }
    }
    
    @Test
    public void testWriteFile() {
        FileSystemWorker.writeFile(getClass().getSimpleName() + ".test", "test");
        Assert.assertTrue(new File(getClass().getSimpleName() + ".test").lastModified() > (System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10)));
    }
    
    @Test
    public void testDelTemp() {
        FileSystemWorker.delTemp();
        Assert.assertTrue(new File("DeleterTemp.txt").lastModified() > (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1)));
    }
    
    @Test(enabled = false)
    public void testCopyOrDelFileWithPath() {
        Path pathForTestOriginal = Paths.get(testRootPath + "testCopyOrDelFileWithPath.test");
        Path pathForCopy = Paths.get(pathForTestOriginal.toString().replace("test", "log"));
        try (OutputStream outputStream = new FileOutputStream(pathForTestOriginal.normalize().toString());
             PrintStream printStream = new PrintStream(outputStream, true)) {
            printStream.println(new TForms().fromArray(Thread.currentThread().getStackTrace()));
        }
        catch (IOException e) {
            Assert.assertNull(e, e.getMessage() + "\n" + new TForms().fromArray(e));
        }
        FileSystemWorker.copyOrDelFileWithPath(pathForTestOriginal.toFile(), pathForCopy, false);
        Assert.assertTrue(pathForTestOriginal.toFile().exists());
        Assert.assertTrue(pathForCopy.toFile().exists());
        FileSystemWorker.copyOrDelFileWithPath(pathForTestOriginal.toFile(), pathForCopy, true);
        Assert.assertFalse(pathForTestOriginal.toFile().exists());
        Assert.assertTrue(pathForCopy.toFile().exists());
        Assert.assertTrue(pathForCopy.toFile().lastModified() > System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10));
    }
    
    @Test
    public void testError() {
        String errorMsg = FileSystemWorker.error(getClass().getSimpleName() + ".testError", new TODOException("22.07.2019 (16:28)"));
        System.out.println("errorMsg = " + errorMsg);
        Assert.assertTrue(new File(errorMsg.split(" | ")[0]).exists(), errorMsg);
    }
    
    @Test
    public void testReadFile() {
        String readFile = FileSystemWorker.readFile("build.gradle");
        Assert.assertTrue(readFile.contains("rsion"));
    }
    
    @Test
    public void testReadFileToQueue() {
        Queue<String> stringQueue = FileSystemWorker.readFileToQueue(Paths.get("build.gradle"));
        Assert.assertFalse(stringQueue.isEmpty());
    }
    
    @Test
    public void testCopyOrDelFile() {
        File origin = new File("build.gradle");
        Path toCopy = Paths.get("build.gradle.bak");
        FileSystemWorker.copyOrDelFile(origin, toCopy, false);
        Assert.assertTrue(origin.exists());
        Assert.assertTrue(toCopy.toFile().exists());
    
        Path buildBak = Paths.get("build.bak");
        FileSystemWorker.copyOrDelFile(toCopy.toFile(), buildBak, true);
        Assert.assertFalse(toCopy.toFile().exists());
        Assert.assertTrue(buildBak.toFile().exists());
        Assert.assertTrue(buildBak.toFile().lastModified() > (System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10)));
    }
    
    @Test
    public void testReadFileToList() {
        List<String> stringSet = FileSystemWorker.readFileToList(String.valueOf(Paths.get("build.gradle").toAbsolutePath().normalize()));
        Assert.assertTrue(stringSet.size() > 0);
    }
    
    @Test
    public void testReadFileToSet() {
        Set<String> stringSet = FileSystemWorker.readFileToSet(Paths.get("build.gradle").toAbsolutePath().normalize());
        Assert.assertTrue(stringSet.size() > 0);
    }
    
    @Test
    public void testAppendObjectToFile() {
        File forAppend = new File(this.getClass().getSimpleName());
        FileSystemWorker.appendObjectToFile(forAppend, "test");
        FileSystemWorker.appendObjectToFile(forAppend, "test1");
        Assert.assertTrue(FileSystemWorker.readFile(forAppend.getAbsolutePath()).contains("test1"));
        Assert.assertTrue(forAppend.delete());
    }
    
    @Test
    public void testPackFiles() {
        UpakFiles upakFiles = new UpakFiles();
        List<File> filesToUpak = new ArrayList<>();
        filesToUpak.add(new File("build.gradle"));
        filesToUpak.add(new File("settings.gradle"));
        upakFiles.createZip(filesToUpak, "gradle.zip", 5);
        File gradleZip = new File("gradle.zip");
        Assert.assertTrue(gradleZip.exists());
        long bytesOrig = 0;
        for (File file : filesToUpak) {
            bytesOrig = bytesOrig + file.length();
        }
        Assert.assertTrue(bytesOrig > gradleZip.length());
    }
}