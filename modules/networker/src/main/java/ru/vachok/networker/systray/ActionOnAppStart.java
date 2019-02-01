package ru.vachok.networker.systray;


import ru.vachok.networker.AppInfoOnLoad;
import ru.vachok.networker.config.ThreadConfig;
import ru.vachok.networker.services.SpeedChecker;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 Actions on application start
 <p>

 @see ru.vachok.networker.AppInfoOnLoad
 @since 25.01.2019 (11:58) */
public class ActionOnAppStart extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        Future<Long> submit = new ThreadConfig().threadPoolTaskExecutor().submit(new SpeedChecker());
        String messageSW = null;
        try {
            DateFormat dateFormat = new SimpleDateFormat();
            messageSW = "When arrive: " + dateFormat.format(new Date(submit.get()));
        } catch (InterruptedException | ExecutionException ignore) {
            Thread.currentThread().interrupt();
        }
        new MessageToTray(new ActionCloseMsg(SystemTrayHelper.getTrayIcon())).info(getClass().getSimpleName(), AppInfoOnLoad.iisLogSize(), messageSW);
    }
}
