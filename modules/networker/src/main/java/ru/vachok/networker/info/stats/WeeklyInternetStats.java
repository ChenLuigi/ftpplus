// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.info.stats;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.vachok.networker.AbstractForms;
import ru.vachok.networker.AppComponents;
import ru.vachok.networker.componentsrepo.UsefulUtilities;
import ru.vachok.networker.componentsrepo.exceptions.InvokeIllegalException;
import ru.vachok.networker.componentsrepo.fileworks.FileSystemWorker;
import ru.vachok.networker.componentsrepo.services.FilesZipPacker;
import ru.vachok.networker.componentsrepo.services.MyCalen;
import ru.vachok.networker.data.enums.*;
import ru.vachok.networker.data.synchronizer.SyncData;
import ru.vachok.networker.info.InformationFactory;
import ru.vachok.networker.restapi.database.DataConnectTo;
import ru.vachok.networker.restapi.message.MessageToTray;
import ru.vachok.networker.restapi.message.MessageToUser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.MessageFormat;
import java.time.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.*;


/**
 @see ru.vachok.networker.info.stats.WeeklyInternetStatsTest
 @since 20.05.2019 (9:36) */
class WeeklyInternetStats implements Runnable, Stats {
    
    
    private static final MessageToUser messageToUser = MessageToUser.getInstance(MessageToUser.LOCAL_CONSOLE, WeeklyInternetStats.class.getSimpleName());
    
    private long totalBytes = 0;
    
    private String fileName;
    
    private String sql;
    
    private InformationFactory informationFactory = InformationFactory.getInstance(INET_USAGE);
    
    protected String getFileName() {
        return fileName;
    }
    
    @Override
    public String getInfoAbout(String aboutWhat) {
        return MessageFormat.format("I will NOT start before: {0}. {1}", MyCalen.getNextDayofWeek(0, 0, DayOfWeek.SUNDAY), this.getClass().getSimpleName());
    }
    
    @Override
    public String getInfo() {
        if (Stats.isSunday()) {
            run();
        }
        return toString();
    }
    
    @Override
    public void setClassOption(@NotNull Object option) {
        if (option instanceof InformationFactory) {
            this.informationFactory = (InformationFactory) option;
        }
        else {
            throw new InvokeIllegalException(WeeklyInternetStats.class.getSimpleName());
        }
    }
    
    @Override
    public void run() {
        FileSystemWorker
                .writeFile(this.getClass().getSimpleName() + "." + LocalTime.now().toSecondOfDay(), AbstractForms.networkerTrace(Thread.currentThread().getStackTrace()));
        InformationFactory informationFactory = InformationFactory.getInstance(InformationFactory.REGULAR_LOGS_SAVER);
        long iPsWithInet = 0;
        try {
            iPsWithInet = readIPsWithInet();
        }
        catch (RuntimeException e) {
            messageToUser.error("WeeklyInternetStats.run", e.getMessage(), AbstractForms.networkerTrace(e.getStackTrace()));
        }
        String headerMsg = MessageFormat.format("{0} in kb. ", getClass().getSimpleName());
        String titleMsg = new File(FileNames.INETSTATSIP_CSV).getAbsolutePath();
        String bodyMsg = " = " + iPsWithInet + " size in kb";
        messageToUser.info(headerMsg, titleMsg, bodyMsg);
        
        if (Stats.isSunday()) {
            readStatsToCSVAndDeleteFromDB();
            AppComponents.threadConfig().getTaskExecutor().execute(new WeeklyInternetStats.InetStatSorter(), 10);
        }
        else {
            throw new InvokeIllegalException(LocalDate.now().getDayOfWeek().name() + " not best day for stats...");
        }
        
    }
    
    long readIPsWithInet() {
        try (Connection connection = DataConnectTo.getInstance(DataConnectTo.DEFAULT_I).getDefaultConnection(ConstantsFor.DB_VELKOMINETSTATS)) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(ConstantsFor.SQL_SELECTINETSTATS)) {
                try (ResultSet r = preparedStatement.executeQuery()) {
                    makeIPFile(r);
                }
            }
        }
        catch (SQLException e) {
            messageToUser.error(e.getMessage() + " see line: 112 ***");
        }
        return new File(FileNames.INETSTATSIP_CSV).length() / ConstantsFor.KBYTE;
    }
    
    private void readStatsToCSVAndDeleteFromDB() {
        List<String> chkIps = FileSystemWorker.readFileToList(new File(FileNames.INETSTATSIP_CSV).getPath());
        for (String ip : chkIps) {
            messageToUser.info(writeObj(ip, "300000"));
        }
        new MessageToTray(this.getClass().getSimpleName()).info("ALL STATS SAVED\n", totalBytes / ConstantsFor.KBYTE + " Kb", fileName);
    }
    
    @Override
    public int hashCode() {
        int result = messageToUser.hashCode();
        result = 31 * result + (int) (totalBytes ^ (totalBytes >>> 32));
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + (sql != null ? sql.hashCode() : 0);
        result = 31 * result + informationFactory.hashCode();
        return result;
    }
    
    @Contract(value = ConstantsFor.NULL_FALSE, pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        WeeklyInternetStats stats = (WeeklyInternetStats) o;
        
        if (totalBytes != stats.totalBytes) {
            return false;
        }
        if (!messageToUser.equals(stats.messageToUser)) {
            return false;
        }
        if (fileName != null ? !fileName.equals(stats.fileName) : stats.fileName != null) {
            return false;
        }
        if (sql != null ? !sql.equals(stats.sql) : stats.sql != null) {
            return false;
        }
        return informationFactory.equals(stats.informationFactory);
    }
    
    @Override
    public String writeObj(String ip, Object rowsLimit) {
        this.fileName = ip + "_" + LocalTime.now().toSecondOfDay() + ".csv";
        this.sql = new StringBuilder().append("SELECT * FROM `inetstats` WHERE `ip` LIKE '").append(ip).append(ConstantsFor.LIMIT).append(rowsLimit).toString();
        String retStr = downloadConcreteIPStatistics();
        File file = new File(fileName);
        this.totalBytes += file.length();
        
        retStr = MessageFormat.format("{0} file is {1}. Total kb: {2}", retStr, file.length() / ConstantsFor.KBYTE, totalBytes / ConstantsFor.KBYTE);
        
        if (Stats.isSunday() & file.length() > 10) {
            retStr = MessageFormat.format("{0} ||| {1} rows deleted.", retStr, deleteFrom(ip, (String) rowsLimit));
        }
        return retStr;
    }
    
    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(",\n", WeeklyInternetStats.class.getSimpleName() + "[\n", "\n]");
        stringJoiner.add("totalBytes = " + totalBytes);
        if (!Stats.isSunday()) {
            stringJoiner.add(LocalDate.now().getDayOfWeek().name());
            stringJoiner.add(daySunCounter());
        }
        return stringJoiner.toString();
    }
    
    private @NotNull String daySunCounter() {
        Date daySun = MyCalen.getNextDayofWeek(0, 0, DayOfWeek.SUNDAY);
        long sundayDiff = daySun.getTime() - System.currentTimeMillis();
        return MessageFormat.format("{0} ({1} hours left)", daySun.toString(), TimeUnit.MILLISECONDS.toHours(sundayDiff));
    }
    
    private void printConcreteIPToFile(@NotNull ResultSet r, PrintStream printStream) throws SQLException {
        while (r.next()) {
            printStream.print(new java.util.Date(Long.parseLong(r.getString("Date"))));
            printStream.print(",");
            printStream.print(r.getString(ConstantsFor.DBCOL_RESPONSE));
            printStream.print(",");
            printStream.print(r.getString(ConstantsFor.DBCOL_BYTES));
            printStream.print(",");
            printStream.print(r.getString(ConstantsFor.DBFIELD_METHOD));
            printStream.print(",");
            printStream.print(r.getString("site"));
            printStream.println();
        }
    }
    
    private void makeIPFile(@NotNull ResultSet r) throws SQLException {
        try (OutputStream outputStream = new FileOutputStream(FileNames.INETSTATSIP_CSV)) {
            try (PrintStream printStream = new PrintStream(outputStream, true)) {
                while (r.next()) {
                    String ip = r.getString("ip");
                    printStream.println(ip);
                }
            }
        }
        catch (IOException e) {
            messageToUser.error(MessageFormat.format("WeeklyInternetStats.makeIPFile", e.getMessage(), AbstractForms.networkerTrace(e.getStackTrace())));
        }
    }
    
    private String downloadConcreteIPStatistics() {
        try (Connection connection = DataConnectTo.getInstance(DataConnectTo.DEFAULT_I)
                .getDefaultConnection(ConstantsFor.STR_VELKOM + "." + FileNames.DIR_INETSTATS)) {
            try (PreparedStatement p = connection.prepareStatement(sql)) {
                try (ResultSet r = p.executeQuery()) {
                    try (OutputStream outputStream = new FileOutputStream(fileName)) {
                        try (PrintStream printStream = new PrintStream(outputStream, true)) {
                            printConcreteIPToFile(r, printStream);
                        }
                    }
                }
            }
            return fileName;
        }
        catch (SQLException | IOException | OutOfMemoryError e) {
            Thread.currentThread().checkAccess();
            Thread.currentThread().interrupt();
            Executors.unconfigurableExecutorService(Executors.newSingleThreadExecutor()).execute(new WeeklyInternetStats());
            messageToUser.error(e.getMessage() + " see line: 210 ***");
            return e.getMessage();
        }
    }
    
    private static class InetStatSorter implements Runnable {
        
        
        private static final ru.vachok.messenger.MessageToUser messageToUser = MessageToUser
                .getInstance(MessageToUser.LOCAL_CONSOLE, WeeklyInternetStats.InetStatSorter.class.getSimpleName());
        
        @Override
        public void run() {
            sortFiles();
            Future<String> submit = Executors.unconfigurableExecutorService(Executors.newSingleThreadExecutor()).submit(new FilesZipPacker());
            try {
                messageToUser.info(this.getClass().getSimpleName(), "running", submit.get());
                SyncData syncData = SyncData.getInstance("10.200.202.55");
                AppComponents.threadConfig().getTaskExecutor().getThreadPoolExecutor().execute(syncData::superRun);
                trunkDB();
            }
            catch (InterruptedException | ExecutionException e) {
                messageToUser.error(FileSystemWorker.error(getClass().getSimpleName() + ".run", e));
            }
        }
        
        private void sortFiles() {
            File[] rootFiles = new File(".").listFiles();
            Map<File, String> mapFileStringIP = new TreeMap<>();
            
            for (File fileFromRoot : Objects.requireNonNull(rootFiles)) {
                if (fileFromRoot.getName().toLowerCase().contains(".csv")) {
                    try {
                        String[] nameSplit = fileFromRoot.getName().split("_");
                        mapFileStringIP.put(fileFromRoot, nameSplit[0].replace(".csv", ""));
                    }
                    catch (ArrayIndexOutOfBoundsException ignore) {
                        //
                    }
                }
            }
            if (mapFileStringIP.size() == 0) {
                FileSystemWorker.writeFile("no.csv", new Date().toString());
            }
            else {
                Set<String> ipsSet = new TreeSet<>(mapFileStringIP.values());
                ipsSet.forEach(ip->makeFile(mapFileStringIP, ip));
                FileSystemWorker.writeFile(FileNames.INETIPS_SET, ipsSet.stream());
            }
        }
        
        private void trunkDB() {
            DataConnectTo dataConnectTo = DataConnectTo.getInstance(DataConnectTo.DEFAULT_I);
            try (Connection connection = dataConnectTo.getDefaultConnection(ConstantsFor.DB_VELKOMINETSTATS);
                 PreparedStatement preparedStatement = connection.prepareStatement("truncate table inetstats")) {
                preparedStatement.executeUpdate();
            }
            catch (SQLException e) {
                messageToUser.error(FileSystemWorker.error(getClass().getSimpleName() + ".trunkDB", e));
            }
        }
        
        private void makeFile(@NotNull Map<File, String> mapFileStringIP, String ip) {
            Collection<File> csvTMPFilesQueue = new LinkedList<>();
            for (File file : mapFileStringIP.keySet()) {
                if (file.getName().contains("_") & file.getName().contains(ip)) {
                    csvTMPFilesQueue.add(file);
                }
            }
            makeCSV(ip, csvTMPFilesQueue);
            
        }
        
        private void makeCSV(String ip, @NotNull Collection<File> queueCSVFilesFromRoot) {
            String fileSeparator = System.getProperty(PropertiesNames.SYS_SEPARATOR);
            String pathInetStats = Paths.get(".").toAbsolutePath().normalize() + fileSeparator + FileNames.DIR_INETSTATS + fileSeparator;
            File finalFile = new File(pathInetStats + ip + ".csv");
            checkDirExists(pathInetStats);
            Set<String> toWriteStatsSet = new TreeSet<>();
            if (finalFile.exists() & queueCSVFilesFromRoot.size() > 0) {
                toWriteStatsSet.addAll(FileSystemWorker.readFileToSet(finalFile.toPath()));
            }
            if (queueCSVFilesFromRoot.size() > 0) {
                messageToUser.info(this.getClass().getSimpleName(), "Adding statistics to: ", finalFile.getAbsolutePath());
                boolean isDelete = false;
                for (File nextFile : queueCSVFilesFromRoot) {
                    toWriteStatsSet.addAll(FileSystemWorker.readFileToSet(nextFile.toPath()));
                    isDelete = nextFile.delete();
                    if (!isDelete) {
                        nextFile.deleteOnExit();
                    }
                }
                boolean isWrite = FileSystemWorker.writeFile(finalFile.getAbsolutePath(), toWriteStatsSet.stream().sorted());
                messageToUser.info(String.valueOf(isWrite), " write: ", finalFile.getAbsolutePath());
                messageToUser.info(this.getClass().getSimpleName(), String.valueOf(isDelete), " deleted temp csv.");
            }
            else {
                messageToUser.warn(this.getClass().getSimpleName(), finalFile.getAbsolutePath(), " is NOT modified.");
            }
            if (!UsefulUtilities.thisPC().toLowerCase().contains("rups")) {
                FileSystemWorker.copyOrDelFile(finalFile, Paths.get("\\\\rups00.eatmeat.ru\\c$\\Users\\ikudryashov\\Documents\\inetstats\\"), true);
            }
        }
        
        private void checkDirExists(String directoryName) {
            File inetStatsDirectory = new File(directoryName);
            if (!inetStatsDirectory.exists() || !inetStatsDirectory.isDirectory()) {
                try {
                    Files.createDirectories(inetStatsDirectory.toPath());
                }
                catch (IOException e) {
                    messageToUser.error(e.getMessage() + " see line: 363 ***");
                }
            }
        }
        
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("InetStatSorter{");
            sb.append(LocalDate.now().getDayOfWeek());
            sb.append('}');
            return sb.toString();
        }
    }
    
    protected long deleteFrom(String ip, String rowsLimit) {
        this.sql = new StringBuilder().append("DELETE FROM `inetstats` WHERE `ip` LIKE '").append(ip).append(ConstantsFor.LIMIT).append(rowsLimit).toString();
        try (Connection connection = DataConnectTo.getDefaultI().getDefaultConnection(ConstantsFor.STR_VELKOM + "." + FileNames.DIR_INETSTATS)) {
            try (PreparedStatement p = connection.prepareStatement(sql)) {
                return p.executeLargeUpdate();
            }
        }
        catch (SQLException e) {
            messageToUser.error(e.getMessage() + " see line: 223 ***");
            Thread.currentThread().checkAccess();
            Thread.currentThread().interrupt();
            Executors.unconfigurableExecutorService(Executors.newSingleThreadExecutor()).execute(new WeeklyInternetStats());
        }
        return -1;
    }
    
    protected void setSql() {
        this.sql = ConstantsFor.SQL_SELECTINETSTATS;
    }
}