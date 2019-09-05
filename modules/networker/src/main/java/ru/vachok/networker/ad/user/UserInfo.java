// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.ad.user;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.vachok.networker.AppComponents;
import ru.vachok.networker.ad.pc.PCInfo;
import ru.vachok.networker.componentsrepo.UsefulUtilities;
import ru.vachok.networker.componentsrepo.data.NetKeeper;
import ru.vachok.networker.componentsrepo.data.enums.ConstantsFor;
import ru.vachok.networker.componentsrepo.data.enums.ModelAttributeNames;
import ru.vachok.networker.info.InformationFactory;
import ru.vachok.networker.restapi.DataConnectTo;
import ru.vachok.networker.restapi.message.MessageLocal;
import ru.vachok.networker.restapi.message.MessageToUser;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;


/**
 @see UserInfoTest */
public abstract class UserInfo implements InformationFactory {
    
    
    public static @NotNull UserInfo getInstance(String type) {
        if (type == null) {
            return new UnknownUser(UserInfo.class.getSimpleName());
        }
        else {
            return checkType(type);
        }
    }
    
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Contract("null -> new")
    private static @NotNull UserInfo checkType(String type) {
        PCInfo.checkValidNameWithoutEatmeat(type);
        if (ModelAttributeNames.ADUSER.equals(type)) {
            return new LocalUserResolver();
        }
        else {
            return new ResolveUserInDataBase(type);
        }
        
    }
    
    public static void writeUsersToDBFromSET() {
        new UserInfo.DatabaseWriter().writeAllPrefixToDB();
    }
    
    public static void autoResolvedUsersRecord(String pcName, @NotNull String lastFileUse) {
        if (!lastFileUse.contains("Unknown user") | !lastFileUse.contains("not found")) {
            AppComponents.threadConfig().execByThreadConfig(()->new UserInfo.DatabaseWriter().writeAutoresolvedUserToDB(pcName, lastFileUse));
        }
        else {
            System.err.println(MessageFormat.format("{0}. Unknown user. DB NOT WRITTEN", pcName));
        }
    }
    
    public abstract List<String> getLogins(String pcName, int resultsLimit);
    
    @Override
    public abstract String getInfoAbout(String aboutWhat);
    
    @Override
    public abstract void setClassOption(Object option);
    
    @Override
    public abstract String getInfo();
    
    @Override
    public String toString() {
        return new StringJoiner(",\n", UserInfo.class.getSimpleName() + "[\n", "\n]")
                .toString();
    }
    
    static String resolvePCUserOverDB(String pcOrUser) {
        String result;
        List<String> userLogins = new ArrayList<>(new ResolveUserInDataBase().getLogins(pcOrUser, 1));
        try {
            result = userLogins.get(0);
        }
        catch (IndexOutOfBoundsException e) {
            userLogins.addAll(new LocalUserResolver().getLogins(pcOrUser, 1));
            if (userLogins.size() > 0) {
                result = userLogins.get(0);
            }
            else {
                result = new UnknownUser(pcOrUser).getInfo();
            }
        }
        return result;
    }
    


    private static class DatabaseWriter {
        
        
        private static final Pattern COMPILE = Pattern.compile(ConstantsFor.DBFIELD_PCUSER);
        
        private static final MessageToUser messageToUser = MessageToUser.getInstance(MessageToUser.LOCAL_CONSOLE, UserInfo.DatabaseWriter.class.getSimpleName());
        
        private String pcName;
        
        private String userName;
    
        @Contract(pure = true)
        private DatabaseWriter() {
        
        }
        
        @Override
        public String toString() {
            return new StringJoiner(",\n", UserInfo.DatabaseWriter.class.getSimpleName() + "[\n", "\n]")
                    .toString();
        }
    
        /**
         @param pcName do0001
         @param lastFileUse 1561612688516 \\do0001.eatmeat.ru\c$\Users\estrelyaeva Thu Jun 27 08:18:08 MSK 2019 1561612688516
         */
        private void writeAutoresolvedUserToDB(String pcName, @NotNull String lastFileUse) {
            this.pcName = pcName;
            this.userName = lastFileUse;
            final String sql = "insert into pcuser (pcName, userName, lastmod, stamp) values(?,?,?,?)";
            StringBuilder stringBuilder = new StringBuilder();
            final String sqlReplaced = COMPILE.matcher(sql).replaceAll(ConstantsFor.DBFIELD_PCUSERAUTO);
        
            try (Connection connection = new AppComponents().connection(ConstantsFor.DBBASENAME_U0466446_VELKOM);
                 PreparedStatement preparedStatement = connection.prepareStatement(sqlReplaced)) {
                stringBuilder.append(execAutoResolvedUser(preparedStatement));
            }
            catch (SQLException | ArrayIndexOutOfBoundsException | NullPointerException | InvalidPathException e) {
                stringBuilder.append(MessageFormat.format("{4}: insert into pcuser (pcName, userName, lastmod, stamp) values({0},{1},{2},{3})",
                    pcName, lastFileUse, UsefulUtilities.thisPC(), "split[0]", e.getMessage()));
            }
            System.out.println(stringBuilder.toString());
        }
        
        private @NotNull String execAutoResolvedUser(@NotNull PreparedStatement preparedStatement) throws SQLException, IndexOutOfBoundsException {
            String[] split = userName.split(" ");
            preparedStatement.setString(1, pcName);
            if (userName.toLowerCase().contains(ModelAttributeNames.USERS)) {
                userName = userName.split(" ")[1];
                userName = Paths.get(userName).getFileName().toString();
            }
            preparedStatement.setString(2, userName);
            preparedStatement.setString(3, UsefulUtilities.thisPC());
            preparedStatement.setString(4, split[0]);
            String retStr = MessageFormat.format("{0}: insert into pcuser (pcName, userName, lastmod, stamp) values({1},{2},{3},{4})", preparedStatement
                    .executeUpdate(), pcName, userName, UsefulUtilities.thisPC(), split[0]);
            ((MessageLocal) messageToUser).loggerFine(retStr);
            return retStr;
        }
        
        private @NotNull String manualUsersDatabaseRecord(String pcName, String userName) {
            String sql = "insert into pcuser (pcName, userName) values(?,?)";
            String msg = userName + " on pc " + pcName + " is set.";
            int retIntExec = 0;
            try (Connection connection = new AppComponents().connection(ConstantsFor.DBBASENAME_U0466446_VELKOM);
                 PreparedStatement p = connection.prepareStatement(sql)) {
                p.setString(1, userName);
                p.setString(2, pcName);
                retIntExec = p.executeUpdate();
                NetKeeper.getPcUser().put(pcName, msg);
            }
            catch (SQLException ignore) {
                //nah
            }
            return MessageFormat.format("{0} executeUpdate {1}", userName, retIntExec);
        }
    
        private void writeAllPrefixToDB() {
            int exUpInt = 0;
            try (Connection connection = DataConnectTo.getDefaultI().getDataSource().getConnection();
                 PreparedStatement prepStatement = connection
                     .prepareStatement("insert into  velkompc (NamePP, AddressPP, SegmentPP , OnlineNow, instr) values (?,?,?,?,?)")) {
                List<String> toSort = new ArrayList<>(NetKeeper.getPcNamesForSendToDatabase());
                toSort.sort(null);
                for (String resolvedStrFromSet : toSort) {
                    exUpInt = makeVLANSegmentation(resolvedStrFromSet, prepStatement);
                }
            }
            catch (SQLException e) {
                messageToUser.error(e.getMessage() + " see line: 181 ***");
            }
            System.out.println(MessageFormat.format("Update = {0} . (insert into  velkompc (NamePP, AddressPP, SegmentPP , OnlineNow, instr))", exUpInt));
        }
    
        /**
         @param resolvedStrFromSet строка {@link NetKeeper#getPcNamesForSendToDatabase()} {@code do0009:10.200.213.132 online true}
         @param prStatement {@link PreparedStatement}
         @return {@link PreparedStatement#executeUpdate()}
     
         @throws SQLException insert into  velkompc
         */
        private int makeVLANSegmentation(@NotNull String resolvedStrFromSet, PreparedStatement prStatement) throws SQLException {
            String pcSegment;
            if (resolvedStrFromSet.contains("200.200")) {
                pcSegment = "Торговый дом";
            }
            else if (resolvedStrFromSet.contains("200.201")) {
                pcSegment = "IP телефоны";
            }
            else if (resolvedStrFromSet.contains("200.202")) {
                pcSegment = "Техслужба";
            }
            else if (resolvedStrFromSet.contains("200.203")) {
                pcSegment = "СКУД";
            }
            else if (resolvedStrFromSet.contains("200.204")) {
                pcSegment = "Упаковка";
            }
            else if (resolvedStrFromSet.contains("200.205")) {
                pcSegment = "МХВ";
            }
            else if (resolvedStrFromSet.contains("200.206")) {
                pcSegment = "Здание склада 5";
            }
            else if (resolvedStrFromSet.contains("200.207")) {
                pcSegment = "Сырокопоть";
            }
            else if (resolvedStrFromSet.contains("200.208")) {
                pcSegment = "Участок убоя";
            }
            else if (resolvedStrFromSet.contains("200.209")) {
                pcSegment = "Да ладно?";
            }
            else if (resolvedStrFromSet.contains("200.210")) {
                pcSegment = "Мастера колб";
            }
            else if (resolvedStrFromSet.contains("200.212")) {
                pcSegment = "Мастера деликатесов";
            }
            else if (resolvedStrFromSet.contains("200.213")) {
                pcSegment = "2й этаж. АДМ.";
            }
            else if (resolvedStrFromSet.contains("200.214")) {
                pcSegment = "WiFiCorp";
            }
            else if (resolvedStrFromSet.contains("200.215")) {
                pcSegment = "WiFiFree";
            }
            else if (resolvedStrFromSet.contains("200.217")) {
                pcSegment = "1й этаж АДМ";
            }
            else if (resolvedStrFromSet.contains("200.218")) {
                pcSegment = "ОКК";
            }
            else if (resolvedStrFromSet.contains("192.168")) {
                pcSegment = "Может быть в разных местах...";
            }
            else if (resolvedStrFromSet.contains("172.16.200")) {
                pcSegment = "Open VPN авторизация - сертификат";
            }
            else {
                pcSegment = "Новый?";
            }
            boolean onLine = false;
            if (resolvedStrFromSet.contains("true")) {
                onLine = true;
            }
    
            String namePP = resolvedStrFromSet.split(":")[0];
            prStatement.setString(1, namePP);
            String addressPP = resolvedStrFromSet.split(":")[1];
            prStatement.setString(2, addressPP.split("<")[0]);
            prStatement.setString(3, pcSegment);
            prStatement.setBoolean(4, onLine);
            prStatement.setString(5, UsefulUtilities.thisPC());
            return prStatement.executeUpdate();
        }
    }
    
    public static @NotNull String manualUsersTableRecord(String pcName, String lastFileUse) {
        return new UserInfo.DatabaseWriter().manualUsersDatabaseRecord(pcName, lastFileUse);
    }
}
