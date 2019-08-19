// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.info;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.vachok.messenger.MessageToUser;
import ru.vachok.networker.AppComponents;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.UsefulUtilities;
import ru.vachok.networker.enums.ConstantsNet;
import ru.vachok.networker.enums.PropertiesNames;
import ru.vachok.networker.exe.ThreadConfig;
import ru.vachok.networker.net.NetKeeper;
import ru.vachok.networker.net.NetScanService;
import ru.vachok.networker.restapi.message.MessageLocal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 Проверки из классов.
 <p>
 Пинги, и тп
 
 @since 31.01.2019 (0:20) */
class PCOn extends LocalPCInfo {
    
    
    private static final MessageToUser messageToUser = new MessageLocal(PCOn.class.getSimpleName());
    
    private static Connection connection;
    
    private boolean isOnline;
    
    private @NotNull String sql;
    
    private String pcName;
    
    public PCOn(String pcName) {
        this.pcName = pcName;
        PCInfo.setAboutWhat(pcName);
        initMe();
    }
    
    PCOn() {
        this.pcName = PCInfo.getAboutWhat();
    }
    
    static {
        try {
            connection = new AppComponents().connection(ConstantsFor.DBBASENAME_U0466446_VELKOM);
        }
        catch (SQLException e) {
            messageToUser.error(MessageFormat.format("ConditionChecker.static initializer: {0}, ({1})", e.getMessage(), e.getClass().getName()));
        }
    }
    
    
    private void initMe() {
        if (NetScanService.isReach(pcName)) {
            this.isOnline = true;
            this.sql = "select * from velkompc where NamePP like ?";
        }
        else {
            this.isOnline = false;
            this.sql = "select * from pcuser where pcName like ?";
        }
    }
    
    @Override
    public String getInfoAbout(String aboutWhat) {
        this.pcName = checkString(aboutWhat);
        ThreadConfig.thrNameSet(pcName.substring(0, 4));
        initMe();
        StringBuilder stringBuilder = new StringBuilder();
        if (isOnline) {
            stringBuilder.append(getUserResolved());
            stringBuilder.append(countOnOff());
        }
        else {
            stringBuilder.append(userNameFromDBWhenPCIsOff());
        }
        return stringBuilder.toString();
    }
    
    @Override
    protected String getUserByPCNameFromDB(String pcName) {
        return getInfoAbout(pcName);
    }
    
    @Contract("_ -> param1")
    private String checkString(@NotNull String aboutWhat) {
        if (aboutWhat.contains(":")) {
            this.pcName = aboutWhat.split(":")[0];
            this.isOnline = aboutWhat.split(":")[1].contains("true");
            return pcName;
        }
        else {
            return aboutWhat;
        }
    }
    
    private @NotNull String getUserResolved() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<b><font color=\"white\">");
        final String sqlLoc = "SELECT * FROM `pcuser` WHERE `pcName` LIKE ?";
        try (PreparedStatement p = connection.prepareStatement(sqlLoc)) {
            p.setString(1, new StringBuilder().append("%").append(pcName).append("%").toString());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    stringBuilder.append(r.getString(ConstantsFor.DB_FIELD_USER));
                }
            }
        }
        catch (SQLException e) {
            stringBuilder.append(e.getMessage());
        }
        stringBuilder.append("</b></font> ");
        return stringBuilder.toString();
    }
    
    @Override
    public @NotNull String pcNameWithHTMLLink(String someMore, @NotNull String pcName) {
        StringBuilder builder = new StringBuilder();
        builder.append("<br><b><a href=\"/ad?");
        builder.append(pcName.split(".eatm")[0]);
        builder.append("\" >");
        builder.append(pcName);
        builder.append("</b></a>     ");
        builder.append(someMore);
        builder.append(". ");
        
        String printStr = builder.toString();
        String pcOnline = "online is true<br>";
        
        NetKeeper.getNetworkPCs().put(printStr, true);
        NetKeeper.getPcNamesSet().add(pcName + ":" + pcName + pcOnline);
        messageToUser.info(pcName, pcOnline, someMore);
        int onlinePC = Integer.parseInt((getLocalProps().getProperty(PropertiesNames.PR_ONLINEPC, "0")));
        onlinePC += 1;
        getLocalProps().setProperty(PropertiesNames.PR_ONLINEPC, String.valueOf(onlinePC));
        return builder.toString();
    }
    
    @Override
    public void setClassOption(Object classOption) {
        this.pcName = (String) classOption;
        PCInfo.setAboutWhat((String) classOption);
        initMe();
    }
    
    private @NotNull String userNameFromDBWhenPCIsOff() {
        if (!pcName.contains(ConstantsFor.EATMEAT)) {
            this.pcName = pcName + ConstantsFor.DOMAIN_EATMEATRU;
        }
        StringBuilder stringBuilder = new StringBuilder();
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setString(1, pcName);
            try (PreparedStatement p1 = connection.prepareStatement(sql.replaceAll(ConstantsFor.DBFIELD_PCUSER, ConstantsFor.DBFIELD_PCUSERAUTO))) {
                p1.setString(1, "%" + pcName + "%");
                try (ResultSet resultSet = p.executeQuery()) {
                    stringBuilder.append(parseResults(resultSet, p1));
                }
            }
    
            final String sql2 = "SELECT * FROM `velkompc` WHERE `NamePP` LIKE '" + pcName + "' ORDER BY `TimeNow` DESC LIMIT 2750";
            try (PreparedStatement p2 = connection.prepareStatement(sql2);
                 ResultSet resultSet = p2.executeQuery()) {
                stringBuilder.append(findLastPCOnlineTime(resultSet));
            }
        }
        catch (SQLException | NullPointerException e) {
            stringBuilder.append("<font color=\"red\">EXCEPTION in SQL dropped. <b>");
            stringBuilder.append(e.getMessage());
            stringBuilder.append("</b></font>");
        }
    
        if (stringBuilder.toString().isEmpty()) {
            stringBuilder.append(getClass().getSimpleName()).append(" <font color=\"red\">").append(pcName).append(" null</font>");
        }
        return stringBuilder.toString();
    }
    
    private @NotNull String parseResults(@NotNull ResultSet resultSet, PreparedStatement p1) throws SQLException {
        StringBuilder stringBuilder = new StringBuilder();
        while (resultSet.next()) {
            stringBuilder.append("<b>")
                .append(resultSet.getString(ConstantsFor.DB_FIELD_USER).trim()).append("</b> (time from: <i>")
                .append(resultSet.getString(ConstantsNet.DB_FIELD_WHENQUERIED)).append("</i> to ");
        }
        if (resultSet.wasNull()) {
            stringBuilder.append("<font color=\"red\">user name is null </font>");
        }
        try (ResultSet resultSet1 = p1.executeQuery()) {
            while (resultSet1.next()) {
                if (resultSet.first()) {
                    stringBuilder.append("<i>").append(resultSet1.getString(ConstantsNet.DB_FIELD_WHENQUERIED)).append("</i>)");
                }
                if (resultSet1.last()) {
                    stringBuilder
                        .append("    (AutoResolved name: ")
                        .append(resultSet1.getString(ConstantsFor.DB_FIELD_USER).trim()).append(")").toString();
                }
                if (resultSet1.wasNull()) {
                    stringBuilder.append("<font color=\"orange\">auto resolve is null </font>");
                }
            }
        }
        return stringBuilder.toString();
    }
    
    private @NotNull String findLastPCOnlineTime(@NotNull ResultSet resultSet) throws SQLException {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> onList = new ArrayList<>();
        while (resultSet.next()) {
            if (resultSet.getString("AddressPP").toLowerCase().contains("true")) {
                onList.add(resultSet.getString(ConstantsFor.DBFIELD_TIMENOW));
            }
        }
        Collections.sort(onList);
        Collections.reverse(onList);
        if (onList.size() > 0) {
            searchLastOnlineDate(onList, stringBuilder);
        }
        return stringBuilder.toString();
    }
    
    private void searchLastOnlineDate(@NotNull List<String> onList, StringBuilder stringBuilder) {
        String strDate = onList.get(0);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern("yyyy-MM-dd");
        Date dateFormat = new Date(Long.parseLong(AppComponents.getProps().getProperty(PropertiesNames.PR_LASTSCAN, String.valueOf(System.currentTimeMillis()))));
        try {
            dateFormat = simpleDateFormat.parse(strDate.split(" ")[0]);
        }
        catch (ParseException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
            messageToUser.error(e.getMessage());
        }
        
        if ((dateFormat.getTime() + TimeUnit.DAYS.toMillis(5) < System.currentTimeMillis())) {
            strDate = "<font color=\"yellow\">" + strDate + "</font>";
        }
        if ((dateFormat.getTime() + TimeUnit.DAYS.toMillis(UsefulUtilities.ONE_DAY_HOURS / 2) < System.currentTimeMillis())) {
            strDate = "<font color=\"red\">" + strDate + "</font>";
            
        }
        else {
            strDate = "<font color=\"green\">" + strDate + "</font>";
        }
        stringBuilder.append("    Last online PC: ");
        stringBuilder.append(strDate);
    }
    
    private @NotNull String countOnOff() {
        InformationFactory userResolver = InformationFactory.getInstance(InformationFactory.LOCAL);
        Runnable rPCResolver = ()->userResolver.getInfoAbout(pcName);
        
        Collection<Integer> onLine = new ArrayList<>();
        Collection<Integer> offLine = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        
        Executors.unconfigurableExecutorService(Executors.newSingleThreadExecutor()).execute(rPCResolver);
        
        try (
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, pcName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int onlineNow = resultSet.getInt(ConstantsNet.ONLINE_NOW);
                    if (onlineNow == 1) {
                        onLine.add(onlineNow);
                    }
                    if (onlineNow == 0) {
                        offLine.add(onlineNow);
                    }
                }
            }
        }
        catch (SQLException e) {
            messageToUser.errorAlert(this.getClass().getSimpleName(), "countOnOff", e.getMessage());
            stringBuilder.append(e.getMessage());
        }
        catch (NullPointerException e) {
            stringBuilder.append(e.getMessage());
        }
        return stringBuilder
            .append(offLine.size())
            .append(" offline times and ")
            .append(onLine.size())
            .append(" online times.").toString();
    }
    
    @Override
    public String toString() {
        return new StringJoiner(",\n", PCOn.class.getSimpleName() + "[\n", "\n]")
            .add("isOnline = " + isOnline)
            .add("sql = '" + sql + "'")
            .add("pcName = '" + pcName + "'")
            .toString();
    }
}