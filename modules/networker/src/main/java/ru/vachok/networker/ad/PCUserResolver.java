package ru.vachok.networker.ad;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.vachok.mysqlandprops.RegRuMysql;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.TForms;
import ru.vachok.networker.componentsrepo.AppComponents;
import ru.vachok.networker.fileworks.FileSystemWorker;
import ru.vachok.networker.net.NetScannerSvc;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;


/**
 <b>Ищет имя пользователя</b>

 @since 02.10.2018 (17:32) */
@Service
public class PCUserResolver implements Thread.UncaughtExceptionHandler {

    /**
     {@link Logger}
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PCUserResolver.class.getSimpleName());

    private static final String REC_AUTO_DB = ".recAutoDB";

    /**
     <i>private cons</i>
     */
    public static PCUserResolver pcUserResolver = new PCUserResolver();

    /**
     {@link AppComponents#lastNetScan()}.getNetWork()
     */
    private static Map<String, Boolean> lastScanMap = AppComponents.lastNetScan().getNetWork();

    /**
     {@link RegRuMysql#getDefaultConnection(String)} - u0466446_velkom
     */
    private static Connection connection = null;

    private String lastFileUse;

    private PCUserResolver() {
    }

    /**
     Записывает содержимое c-users в файл с именем ПК <br> 1 {@link #recAutoDB(String, String)}

     @param pcName имя компьютера
     @see NetScannerSvc
     */
    public synchronized void namesToFile(String pcName) {
        Thread.currentThread().setName(pcName);
        Thread.currentThread().setPriority(3);
        File[] files;
        try(OutputStream outputStream = new FileOutputStream(pcName);
            PrintWriter writer = new PrintWriter(outputStream, true)){
            String pathAsStr = "\\\\" + pcName + "\\c$\\Users\\";
            lastFileUse = getLastTimeUse(pathAsStr).split("Users")[1];
            files = new File(pathAsStr).listFiles();
            writer
                .append(Arrays.toString(files).replace(", ", "\n"))
                .append("\n\n\n")
                .append(lastFileUse);
            Thread.currentThread().checkAccess();
        }
        catch(IOException | ArrayIndexOutOfBoundsException e){
            Thread.currentThread().checkAccess();
            Thread.currentThread().interrupt();
        }
        if(lastFileUse!=null){
            recAutoDB(pcName, lastFileUse);
            Thread.currentThread().interrupt();
        }
    }

    private synchronized String getLastTimeUse(String pathAsStr) {
        WalkerToUserFolder walkerToUserFolder = new WalkerToUserFolder();
        try{
            Files.walkFileTree(Paths.get(pathAsStr), Collections.singleton(FOLLOW_LINKS), 2, walkerToUserFolder);
            List<String> timePath = walkerToUserFolder.getTimePath();
            Collections.sort(timePath);
            return timePath.get(timePath.size() - 1);
        }
        catch(IOException | IndexOutOfBoundsException e){
            return e.getMessage();
        }
    }

    /**
     Записывает инфо о пльзователе в <b>pcuserauto</b> <br> Записи добавляются к уже имеющимся.
     <p>
     Usages: {@link PCUserResolver#namesToFile(String)} <br>
     Uses: -

     @param pcName      имя ПК
     @param lastFileUse строка - имя последнего измененного файла в папке пользователя.
     */
    private synchronized void recAutoDB(String pcName, String lastFileUse) {

        String sql = "insert into pcuser (pcName, userName, lastmod, stamp) values(?,?,?,?)";
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql
            .replaceAll(ConstantsFor.STR_PCUSER, ConstantsFor.STR_PCUSERAUTO))){
            String[] split = lastFileUse.split(" ");
            preparedStatement.setString(1, pcName);
            preparedStatement.setString(2, split[0]);
            preparedStatement.setString(3, IntStream.of(2, 3, 4).mapToObj(i -> split[i]).collect(Collectors.joining()));
            preparedStatement.setString(4, split[7]);
            preparedStatement.executeUpdate();
        }
        catch(SQLException e){
            FileSystemWorker.recFile(
                this.getClass().getSimpleName() + REC_AUTO_DB + ConstantsFor.LOG,
                Collections.singletonList(new TForms().fromArray(e, false)));
            NetScannerSvc.getI().reconnectToDB();
        }
        catch(ArrayIndexOutOfBoundsException | NullPointerException e){
            LOGGER.error(e.getMessage(), e);
            Thread.currentThread().checkAccess();
            Thread.currentThread().getThreadGroup().destroy();
        }
    }

    /**
     @return {@link #pcUserResolver}
     */
    public static PCUserResolver getPcUserResolver(Connection c) {
        Thread.currentThread().checkAccess();
        Thread.currentThread().getThreadGroup().checkAccess();
        connection = c;
        return pcUserResolver;
    }

    /**
     Запрос на установку пользователя
     <p>
     Usages:  {@link AppComponents#pcUserResolver()} <br>
     Uses: {@link AppComponents#adSrv()} <br>

     @return {@link ADSrv#getAdUser()}
     @see ActDirectoryCTRL
     */
    ADUser adUsersSetter() {
        ADSrv adSrv = AppComponents.adSrv();
        ADUser adUser = adSrv.getAdUser();
        try{
            String resolvedName = getResolvedName();
            LOGGER.info(resolvedName);
            adUser.setUserName(resolvedName);
        }
        catch(NullPointerException e){
            LOGGER.warn("I cant set User for");
            Thread.currentThread().interrupt();
        }
        Thread.currentThread().interrupt();
        return adUser;
    }

    /**
     <b>Рабочий метод</b>
     Делает запрос в {@code \\c$\Users}, ищет там папки, записывает в массив. <br> Сортирует по дате изменения.

     @return {@link String}, имя последнего измененного объекта.
     @see #adUsersSetter()
     */
    private synchronized String getResolvedName() {
        List<String> onlineNow = new ArrayList<>();
        List<String> offNow = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        if(!lastScanMap.isEmpty()){
            lastScanMap.forEach((x, y) -> {
                if(y){
                    onlineNow.add(x);
                }
                else{
                    offNow.add(x);
                }
            });
        }
        else{
            NetScannerSvc.getI().getPCsAsync();
        }
        onlineNow.forEach(x -> {
            x = x.replaceAll("<br><b>", "").split("</b><br>")[0];
            File filesAsFile = new File("\\\\" + x + "\\c$\\Users\\");
            File[] files = filesAsFile.listFiles();
            ConstantsFor.COMPNAME_USERS_MAP.put(x, filesAsFile);
            SortedMap<Long, String> lastMod = new TreeMap<>();
            if(files!=null){
                for(File file : files){
                    lastMod.put(file.lastModified(), file.getName() + " user " + x + " comp\n");

                }
            }
            else{
                stringBuilder
                    .append(System.currentTimeMillis())
                    .append(" millis. Can't set user for: ").append(x).append("\n");
            }
            Optional<Long> max = lastMod.keySet().stream().max(Long::compareTo);
            boolean aLongPresent = max.isPresent();
            if(aLongPresent){
                Long aLong = max.get();

                stringBuilder
                    .append(lastMod.get(aLong));
            }
        });
        offNow.forEach(x -> {
            stringBuilder.append(offNowGetU(x));
        });
        String msg = ConstantsFor.COMPNAME_USERS_MAP.size() + ConstantsFor.COMPNAME_USERS_MAP_SIZE;
        LOGGER.warn(msg);
        return stringBuilder.toString();
    }

    /**
     Читает БД на предмет наличия юзера для <b>offline</b> компьютера.<br> {@link #getResolvedName()}

     @param pcName имя ПК
     @return имя юзера, время записи.
     @see ADSrv#getDetails(String)
     */
    synchronized String offNowGetU(CharSequence pcName) {
        StringBuilder v = new StringBuilder();
        try(Connection c = new RegRuMysql().getDefaultConnection(ConstantsFor.U_0466446_VELKOM)){

            try(PreparedStatement p = c.prepareStatement("select * from pcuser");
                PreparedStatement pAuto = c.prepareStatement("select * from pcuserauto where pcName in (select pcName from pcuser) order by pcName asc limit 203");
                ResultSet resultSet = p.executeQuery();
                ResultSet resultSetA = pAuto.executeQuery()){
                while(resultSet.next()){
                    if(resultSet.getString(ConstantsFor.DB_FIELD_PCNAME).toLowerCase().contains(pcName)){
                        v
                            .append("<b>")
                            .append(resultSet.getString(ConstantsFor.DB_FIELD_USER))
                            .append("</b> <br>At ")
                            .append(resultSet.getString(NetScannerSvc.DB_FIELD_WHENQUERIED));
                    }
                }
                while(resultSetA.next()){
                    if(resultSetA.getString(ConstantsFor.DB_FIELD_PCNAME).toLowerCase().contains(pcName)){
                        v
                            .append("<p>")
                            .append(resultSet.getString(ConstantsFor.DB_FIELD_USER))
                            .append(" auto QUERY at: ")
                            .append(resultSet.getString(NetScannerSvc.DB_FIELD_WHENQUERIED));
                    }
                }
            }
        }
        catch(SQLException e){
            FileSystemWorker.recFile(
                this.getClass().getSimpleName() + REC_AUTO_DB + ConstantsFor.LOG,
                Collections.singletonList(new TForms().fromArray(e, false)));
            NetScannerSvc.getI();
            NetScannerSvc.reconnectToDB();
        }
        Thread.currentThread().checkAccess();
        Thread.currentThread().getThreadGroup().interrupt();
        return v.toString();
    }

    /**
     Запись в БД <b>pcuser</b><br> Запись по-запросу от браузера. <br> pcName - уникальный (таблица не переписывается или не дополняется, при наличии записи по-компу)

     @param userName имя юзера
     @param pcName   имя ПК
     @see ADSrv#getDetails(String)
     */
    synchronized void recToDB(String userName, String pcName) {
        String sql = "insert into pcuser (pcName, userName) values(?,?)";
        ConcurrentMap<String, String> pcUMap = ConstantsFor.PC_U_MAP;
        String msg = userName + " on pc " + pcName + " is set.";
        try(PreparedStatement p = connection.prepareStatement(sql)){
            p.setString(1, userName);
            p.setString(2, pcName);
            p.executeUpdate();
            LOGGER.info(msg);
            pcUMap.put(pcName, msg);
            Thread.currentThread().interrupt();
        }
        catch(SQLException e){
            FileSystemWorker.recFile(
                this.getClass().getSimpleName() + REC_AUTO_DB + ConstantsFor.LOG,
                Collections.singletonList(new TForms().fromArray(e, false)));
            NetScannerSvc.getI().reconnectToDB();
        }
    }

    /**
     Method invoked when the given thread terminates due to the
     given uncaught exception.
     <p>Any exception thrown by this method will be ignored by the
     Java Virtual Machine.  @param t the thread

     @param e the exception
     */
    @Override
    public synchronized void uncaughtException(Thread t, Throwable e) {
        t.checkAccess();
        t.interrupt();
        String msg = t.toString() + "\n" + e.getMessage();
        LOGGER.info(msg);
    }

    /*END FOR CLASS*/

    /**
     Поиск файлов в папках {@code c-users}.

     @since 22.11.2018 (14:46)
     */
    static class WalkerToUserFolder extends SimpleFileVisitor<Path> {

        private List<String> timePath = new ArrayList<>();

        public List<String> getTimePath() {
            return timePath;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            timePath.add(file.toFile().lastModified() + " " + file + " " + new Date(file.toFile().lastModified()) + " " + file.toFile().lastModified());
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }
}