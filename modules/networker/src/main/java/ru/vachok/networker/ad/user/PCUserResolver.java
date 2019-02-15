package ru.vachok.networker.ad.user;


import ru.vachok.mysqlandprops.RegRuMysql;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.ad.ADSrv;
import ru.vachok.networker.fileworks.FileSystemWorker;
import ru.vachok.networker.net.enums.ConstantsNet;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;


/**
 <b>Ищет имя пользователя</b>

 @since 02.10.2018 (17:32) */
public class PCUserResolver extends ADSrv {

    /**
     New instance
     */
    private static final PCUserResolver PC_USER_RESOLVER = new PCUserResolver();

    /**
     * Последний измененный файл.
     * @see #getLastTimeUse(String)
     */
    private String lastFileUse = null;

    /**
     @return {@link #PC_USER_RESOLVER}
     */
    public static PCUserResolver getPcUserResolver() {
        return PC_USER_RESOLVER;
    }

    static {
        connection = new RegRuMysql().getDefaultConnection(ConstantsNet.DB_NAME);
    }

    /**
     Default-конструктор
     */
    private PCUserResolver() {
    }

    /**
     Реконнект к БД.

     @return {@link Connection}
     @see #recAutoDB(String, String)
     */
    private static Connection reconnectToDB() {
        connection = new RegRuMysql().getDefaultConnection(ConstantsNet.DB_NAME);
        return connection;
    }

    /**
     Записывает содержимое c-users в файл с именем ПК
     <p>
     Создаёт {@link String} пути к пользовательской папке, через
     {@link StringBuilder#append(java.lang.String)}: "\\\\" + name + "\\c$\\Users\\"
     <p>
     1 {@link #getLastTimeUse(java.lang.String)}. Получение строки, с последним модифицированным файлом.
     <p>
     <b>{@link IOException}</b>:<br>
     {@link FileSystemWorker#error(java.lang.String, java.lang.Exception)} <br>
     {@link ArrayIndexOutOfBoundsException}:<br>
     IGNORED
     <p>
     Если {@link #lastFileUse} != 0: {@link #recAutoDB(String, String)}

     @param pcName имя компьютера
     @see ru.vachok.networker.net.ConditionChecker#onLinesCheck(String, String)
     */
    public synchronized void namesToFile(String pcName) {
        Thread.currentThread().setName(pcName);
        Thread.currentThread().setPriority(3);
        File[] files;
        try (OutputStream outputStream = new FileOutputStream(pcName);
             PrintWriter writer = new PrintWriter(outputStream, true)) {
            String pathAsStr = new StringBuilder().append("\\\\").append(pcName).append("\\c$\\Users\\").toString();
            lastFileUse = getLastTimeUse(pathAsStr).split("Users")[1];
            files = new File(pathAsStr).listFiles();
            writer
                .append(Arrays.toString(files).replace(", ", "\n"))
                .append("\n\n\n")
                .append(lastFileUse);

        } catch (IOException e) {
            FileSystemWorker.error("PCUserResolver.namesToFile", e);
        } catch (ArrayIndexOutOfBoundsException ignore) {
            //
        }
        if (lastFileUse != null) {
            recAutoDB(pcName, lastFileUse);
        }
    }

    /**
     Записывает инфо о пльзователе в <b>pcuserauto</b>
     <p>
     Записи добавляются к уже имеющимся.
     <p>
     <b>{@link SQLException}: </b>
     1. {@link FileSystemWorker#error(java.lang.String, java.lang.Exception)} <br>
     2. {@link #reconnectToDB()}
     <p>
     <b>{@link ArrayIndexOutOfBoundsException}, {@link NullPointerException}:</b><br>
     {@link FileSystemWorker#error(java.lang.String, java.lang.Exception)}
     @param pcName      имя ПК
     @param lastFileUse строка - имя последнего измененного файла в папке пользователя.
     */
    private synchronized void recAutoDB(String pcName, String lastFileUse) {
        String sql = "insert into pcuser (pcName, userName, lastmod, stamp) values(?,?,?,?)";
        String classMeth = "PCUserResolver.recAutoDB";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql
            .replaceAll(ConstantsFor.STR_PCUSER, ConstantsFor.STR_PCUSERAUTO))) {
            String[] split = lastFileUse.split(" ");
            preparedStatement.setString(1, pcName);
            preparedStatement.setString(2, split[0]);
            preparedStatement.setString(3, IntStream.of(2, 3, 4).mapToObj(i -> split[i]).collect(Collectors.joining()));
            preparedStatement.setString(4, split[7]);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            FileSystemWorker.error(classMeth, e);
            connection = reconnectToDB();
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            FileSystemWorker.error(classMeth, e);
        }
    }

    /**
     Ищет в подпапках папки Users, файлы.
     <p>
     Работа в new {@link WalkerToUserFolder}. <br> В {@link Files#walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)}, отправляем параметры: <br> 1. Путь<br>
     2. {@link FileVisitOption#FOLLOW_LINKS} <br> 3. Макс. глубина 2 <br> 4. {@link WalkerToUserFolder}
     <p>
     Сортируем {@link WalkerToUserFolder#getTimePath()} по Timestamp. От меньшего к большему.

     @param pathAsStr путь, который нужно пролистать.
     @return {@link WalkerToUserFolder#getTimePath()} последняя запись из списка.
     */
    private synchronized String getLastTimeUse(String pathAsStr) {
        WalkerToUserFolder walkerToUserFolder = new WalkerToUserFolder();
        try {
            Files.walkFileTree(Paths.get(pathAsStr), Collections.singleton(FOLLOW_LINKS), 2, walkerToUserFolder);
            List<String> timePath = walkerToUserFolder.getTimePath();
            Collections.sort(timePath);
            return timePath.get(timePath.size() - 1);
        } catch (IOException | IndexOutOfBoundsException e) {
            return e.getMessage();
        }
    }

    /**
     Поиск файлов в папках {@code c-users}.

     @see #getLastTimeUse(String)
     @since 22.11.2018 (14:46)
     */
    static class WalkerToUserFolder extends SimpleFileVisitor<Path> {

        /**
         * new {@link ArrayList}, список файлов, с отметками {@link File#lastModified()}
         * @see #visitFile(Path, BasicFileAttributes)
         */
        private final List<String> timePath = new ArrayList<>();

        /**
         @return {@link #timePath}
         */
        List<String> getTimePath() {
            return timePath;
        }

        /**
         Предпросмотр директории.
         <p>
         До листинга файлов.

         @param dir  {@link Path}
         @param attrs {@link BasicFileAttributes}
         @return {@link FileVisitResult#CONTINUE}
         */
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        /**
         Просмотр файла.
         <p>
         Добавляет в {@link #timePath}: <br>
         Время модификации файла {@link File#lastModified()} + файл {@link Path#toString()} + new {@link Date}(java.io.File#lastModified()) + {@link File#lastModified()}.
         @param file {@link Path}
         @param attrs {@link BasicFileAttributes}
         @return {@link FileVisitResult#CONTINUE}
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            timePath.add(file.toFile().lastModified() + " " + file + " " + new Date(file.toFile().lastModified()) + " " + file.toFile().lastModified());
            return FileVisitResult.CONTINUE;
        }

        /**
         Просмотр файла не удался.
         <p>
         @param file {@link Path}
         @param  exc {@link IOException}
         @return {@link FileVisitResult#CONTINUE}
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        /**
         Постпросмотр директории.
         <p>
         После листинга файлов.

         @param dir {@link Path}
         @param exc {@link IOException}
         @return {@link FileVisitResult#CONTINUE}
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}