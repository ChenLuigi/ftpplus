package ru.vachok.networker.services;


import org.slf4j.Logger;
import ru.vachok.messenger.MessageToUser;
import ru.vachok.messenger.email.ESender;
import ru.vachok.mysqlandprops.DataConnectTo;
import ru.vachok.mysqlandprops.EMailAndDB.MailMessages;
import ru.vachok.mysqlandprops.RegRuMysql;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.TForms;
import ru.vachok.networker.componentsrepo.AppComponents;
import ru.vachok.networker.fileworks.FileSystemWorker;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


/**
 @since 22.08.2018 (9:36) */
public class SpeedChecker implements Callable<Long> {

    /**
     {@link RegRuMysql}
     */
    private static final DataConnectTo DATA_CONNECT_TO = new RegRuMysql();

    /**
     {@link AppComponents#getLogger()}
     */
    private static final Logger LOGGER = AppComponents.getLogger();

    /**
     When an object implementing interface <code>Runnable</code> is used
     to create a thread, MatrixCtr the thread causes the object's
     <code>dnldRSA</code> method to be called in that separately executing
     thread.
     <p>
     The general contract of the method <code>dnldRSA</code> is that it may
     take any action whatsoever.

     @see Thread#run()
     */
    @Override
    public Long call() {
        Long chkForLastLong = chkForLast();
        String msg = new java.util.Date(chkForLastLong) + " from " + SpeedChecker.class.getSimpleName();
        LOGGER.info(msg);
        return chkForLastLong;
    }

    private static Long chkForLast() {
        String sql = ConstantsFor.SELECT_FROM_SPEED;
        Long rtLong = Calendar.getInstance().getTimeInMillis() - ConstantsFor.getAtomicTime();
        try(Connection c = DATA_CONNECT_TO.getDefaultConnection(ConstantsFor.DB_PREFIX + "liferpg");
            PreparedStatement p = c.prepareStatement(sql);
            ResultSet r = p.executeQuery()){
            while(r.last()){
                double timeSpend = r.getDouble(ConstantsFor.TIME_SPEND);
                long timeStamp = r.getTimestamp(ConstantsFor.COL_SQL_NAME_TIMESTAMP).getTime();
                String msg = timeSpend + " time spend;\n" + timeStamp;
                rtLong = timeStamp + TimeUnit.MINUTES.toMillis(3);
                LOGGER.info(msg);
                return rtLong;
            }
        }
        catch(SQLException e){
            FileSystemWorker.recFile(SpeedChecker.class.getSimpleName() + ".log", Collections.singletonList(new TForms().fromArray(e, false)));
        }
        return rtLong;
    }

    /*END FOR CLASS*/
    public static final class SpFromMail implements Runnable {

        @Override
        public void run() {
            chechMail();
        }

        private String chechMail() {
            MessageToUser eSender = new ESender(ConstantsFor.GMAIL_COM);
            Message[] messagesBot = new MailMessages().call();
            String chDB = new TForms().fromArray(checkDB());
            FileSystemWorker.recFile(this.getClass().getSimpleName() + ConstantsFor.LOG, Collections.singletonList(chDB));
            for(Message m : messagesBot){
                try(InputStream inputStream = m.getInputStream()){
                    String subjMail = m.getSubject();
                    if(subjMail.toLowerCase().contains("speed:")){
                        if(chDB.contains("okok")){
                            eSender.info(SpFromMail.class.getSimpleName(), true + " sending to base", chDB);
                        }
                        else{
                            eSender.errorAlert(
                                this.getClass().getSimpleName(),
                                ".chechMail " + false,
                                LocalDateTime.now().toString());
                        }
                    }
                }
                catch(MessagingException | IOException e){
                    eSender.errorAlert(
                        this.getClass().getSimpleName(),
                        LocalDateTime.now() + " " + e.getMessage(),
                        new TForms().fromArray(e, false));
                }
            }
            return chDB;
        }

        private Map<String, String> checkDB() {
            Map<String, String> retMap = new HashMap<>();
            DataConnectTo dataConnectTo = new RegRuMysql();
            DataSource source = dataConnectTo.getDataSource();
            String sql = ConstantsFor.SELECT_FROM_SPEED;
            try(Connection c = dataConnectTo.getDefaultConnection(ConstantsFor.U_0466446_LIFERPG);
                PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()){
                while(r.next()){
                    String valueS = r.getInt("Road") +
                        " road, " +
                        r.getString("Speed") +
                        " speed, " + r.getString(ConstantsFor.TIME_SPEND) + " time in min, " +
                        DayOfWeek.of(r.getInt("WeekDay"));
                    retMap.put(r.getTimestamp(ConstantsFor.COL_SQL_NAME_TIMESTAMP).toString(), valueS);
                }
                retMap.put(LocalDateTime.now().toString(), "okok");
            }
            catch(SQLException e){
                retMap.put(e.getMessage(), new TForms().fromArray(e, false));
            }
            return retMap;
        }
    }
}