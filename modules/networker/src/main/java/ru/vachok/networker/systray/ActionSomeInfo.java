package ru.vachok.networker.systray;


import ru.vachok.messenger.MessageCons;
import ru.vachok.messenger.MessageToUser;
import ru.vachok.networker.IntoApplication;
import ru.vachok.networker.net.DiapazonedScan;
import ru.vachok.networker.services.MessageLocal;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 Action Some Info
 <p>

 @see SystemTrayHelper
 @since 25.01.2019 (9:33) */
class ActionSomeInfo extends AbstractAction {

    private transient MessageToUser messageToUser = new MessageLocal();

    ActionSomeInfo() {
        new MessageCons().errorAlert("ActionSomeInfo.ActionSomeInfo");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Date newScan = new Date(DiapazonedScan.getInstance().getStopClassStampLong() + TimeUnit.MINUTES.toMillis(111));
        new Thread(IntoApplication.infoMsgRunnable).start();
        messageToUser.info("ActionSomeInfo.actionPerformed", "newScan = ", newScan.toString());
        SystemTrayHelper.delOldActions();
    }
}
