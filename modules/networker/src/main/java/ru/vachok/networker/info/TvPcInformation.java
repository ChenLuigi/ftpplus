// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.info;


import org.jetbrains.annotations.NotNull;
import ru.vachok.messenger.MessageToUser;
import ru.vachok.networker.AppComponents;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.TForms;
import ru.vachok.networker.accesscontrol.inetstats.InetUserPCName;
import ru.vachok.networker.enums.ConstantsNet;
import ru.vachok.networker.enums.FileNames;
import ru.vachok.networker.enums.OtherKnownDevices;
import ru.vachok.networker.fileworks.FileSystemWorker;
import ru.vachok.networker.restapi.internetuse.InternetUse;
import ru.vachok.networker.restapi.message.MessageLocal;
import ru.vachok.networker.restapi.message.MessageToTray;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 Получение более детальной информации о ПК
 <p>
 
 @since 25.01.2019 (11:06) */
public class TvPcInformation implements InformationFactory {
    
    
    private static final Properties PROPERTIES = AppComponents.getProps();
    
    private static final String TV = "tv";
    
    private static final Pattern COMPILE = Pattern.compile(": ");
    
    private String aboutWhat = TV;
    
    private boolean isOnline;
    
    public boolean getOnline() {
        return this.isOnline;
    }
    
    public static @NotNull String getUserFromDB(String userInputRaw) {
        StringBuilder retBuilder = new StringBuilder();
        final String sql = "select * from pcuserauto where userName like ? ORDER BY whenQueried DESC LIMIT 0, 20";
        List<String> userPCName = new ArrayList<>();
        String mostFreqName = "No Name";
    
        try {
            userInputRaw = COMPILE.split(userInputRaw)[1].trim();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            userInputRaw = userInputRaw.split(":")[1].trim();
        }
    
        try (Connection c = new AppComponents().connection(ConstantsFor.DBPREFIX + ConstantsFor.STR_VELKOM);
             PreparedStatement p = c.prepareStatement(sql)
        ) {
            p.setString(1, "%" + userInputRaw + "%");
            try (ResultSet r = p.executeQuery()) {
                StringBuilder stringBuilder = new StringBuilder();
                String headER = "<h3><center>LAST 20 USER (" + userInputRaw + ") PCs</center></h3>";
                stringBuilder.append(headER);
    
                while (r.next()) {
                    rNext(r, userPCName, stringBuilder);
                }
                
                List<String> collectedNames = userPCName.stream().distinct().collect(Collectors.toList());
                Map<Integer, String> freqName = new HashMap<>();
    
                for (String x : collectedNames) {
                    collectFreq(userPCName, x, stringBuilder, freqName);
                }
                if (r.last()) {
                    rLast(r);
                }
                countCollection(collectedNames, stringBuilder, freqName);
                return stringBuilder.toString();
            }
        }
        catch (SQLException | NoSuchElementException e) {
            retBuilder.append(e.getMessage()).append("\n").append(new TForms().fromArray(e, false));
        }
        return retBuilder.toString();
    }
    
    @Override
    public String toString() {
        return new StringJoiner(",\n", TvPcInformation.class.getSimpleName() + "[\n", "\n]")
            .add("aboutWhat = '" + aboutWhat + "'")
            .add("isOnline = " + isOnline)
            .toString();
    }
    
    @Override
    public String getInfoAbout(@NotNull String aboutWhat) {
        this.aboutWhat = aboutWhat;
        if (aboutWhat.equalsIgnoreCase(TV)) {
            return getTVNetInfo();
        }
        else {
            return getSomeMore();
        }
    }
    
    @Override
    public void setInfo(Object info) {
        this.isOnline = (boolean) info;
    }
    
    private static void countCollection(List<String> collectedNames, @NotNull StringBuilder stringBuilder, @NotNull Map<Integer, String> freqName) {
        Collections.sort(collectedNames);
        Set<Integer> integers = freqName.keySet();
        String mostFreqName = freqName.get(Collections.max(integers));
        InternetUse internetUse = new InetUserPCName();
        stringBuilder.append("<br>");
        stringBuilder.append(internetUse.getUsage(mostFreqName));
    }
    
    private static void collectFreq(List<String> userPCName, String x, @NotNull StringBuilder stringBuilder, @NotNull Map<Integer, String> freqName) {
        int frequency = Collections.frequency(userPCName, x);
        stringBuilder.append(frequency).append(") ").append(x).append("<br>");
        freqName.putIfAbsent(frequency, x);
    }
    
    private static void rLast(@NotNull ResultSet r) throws SQLException {
        try {
            MessageToUser messageToUser = new MessageToTray(TvPcInformation.class.getSimpleName());
            messageToUser.info(r.getString(ConstantsFor.DBFIELD_PCNAME), r.getString(ConstantsNet.DB_FIELD_WHENQUERIED), r.getString(ConstantsFor.DB_FIELD_USER));
        }
        catch (HeadlessException e) {
            new MessageLocal(TvPcInformation.class.getSimpleName())
                .info(r.getString(ConstantsFor.DBFIELD_PCNAME), r.getString(ConstantsNet.DB_FIELD_WHENQUERIED), r.getString(ConstantsFor.DB_FIELD_USER));
        }
    }
    
    private static void rNext(@NotNull ResultSet r, @NotNull List<String> userPCName, @NotNull StringBuilder stringBuilder) throws SQLException {
        String pcName = r.getString(ConstantsFor.DBFIELD_PCNAME);
        userPCName.add(pcName);
        String returnER = "<br><center><a href=\"/ad?" + pcName.split("\\Q.\\E")[0] + "\">" + pcName + "</a> set: " + r
            .getString(ConstantsNet.DB_FIELD_WHENQUERIED) + ConstantsFor.HTML_CENTER_CLOSE;
        stringBuilder.append(returnER);
    }
    
    private static @NotNull String getTVNetInfo() {
        File ptvFile = new File(FileNames.FILENAME_PTV);
        
        List<String> readFileToList = FileSystemWorker.readFileToList(ptvFile.getAbsolutePath());
        List<String> onList = new ArrayList<>();
        List<String> offList = new ArrayList<>();
        readFileToList.stream().flatMap(x->Arrays.stream(x.split(", "))).forEach(s->{
            if (s.contains("true")) {
                onList.add(s.split("/")[0]);
            }
            else {
                offList.add(s.split("/")[0]);
            }
        });
        
        String ptv1Str = OtherKnownDevices.PTV1_EATMEAT_RU;
        String ptv2Str = OtherKnownDevices.PTV2_EATMEAT_RU;

//        String ptv3Str = OtherKnownDevices.PTV3_EATMEAT_RU;
        
        int frequencyOffPTV1 = Collections.frequency(offList, ptv1Str);
        int frequencyOnPTV1 = Collections.frequency(onList, ptv1Str);
        int frequencyOnPTV2 = Collections.frequency(onList, ptv2Str);
        int frequencyOffPTV2 = Collections.frequency(offList, ptv2Str);
//        int frequencyOnPTV3 = Collections.frequency(onList, ptv3Str);
//        int frequencyOffPTV3 = Collections.frequency(offList, ptv3Str);
        
        String ptv1Stats = "<br><font color=\"#00ff69\">" + frequencyOnPTV1 + " on " + ptv1Str + "</font> | <font color=\"red\">" + frequencyOffPTV1 + " off " + ptv1Str + "</font>";
        String ptv2Stats = "<font color=\"#00ff69\">" + frequencyOnPTV2 + " on " + ptv2Str + "</font> | <font color=\"red\">" + frequencyOffPTV2 + " off " + ptv2Str + "</font>";
//        String ptv3Stats = "<font color=\"#00ff69\">" + frequencyOnPTV3 + " on " + ptv3Str + "</font> | <font color=\"red\">" + frequencyOffPTV3 + " off " + ptv3Str + "</font>";
        
        return String.join("<br>\n", ptv1Stats, ptv2Stats);
    }
    
    private @NotNull String getSomeMore() throws NoClassDefFoundError {
        StringBuilder buildEr = new StringBuilder();
        if (isOnline) {
            buildEr.append("<font color=\"yellow\">last name is ");
            InformationFactory informationFactory = new ConditionChecker("select * from velkompc where NamePP like ?");
            buildEr.append(informationFactory.getInfoAbout(aboutWhat + ":true"));
            buildEr.append("</font> ");
        }
        else {
            InformationFactory informationFactory = new ConditionChecker("select * from pcuser where pcName like ?");
            buildEr.append(informationFactory.getInfoAbout(aboutWhat + ":false"));
        }
        return buildEr.toString();
    }
    
}
