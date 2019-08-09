// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker.controller;


import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.vachok.networker.SSHFactory;
import ru.vachok.networker.UsefulUtilities;
import ru.vachok.networker.enums.ConstantsNet;
import ru.vachok.networker.enums.ModelAttributeNames;
import ru.vachok.networker.info.InformationFactory;
import ru.vachok.networker.info.PageFooter;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.concurrent.*;


/**
 @see ru.vachok.networker.controller.OKMakerTest
 @since 06.04.2019 (20:49) */
@Controller
public class OKMaker {
    
    
    private static final String STR_BR = " ||| executing:</i><br>";
    
    private final InformationFactory pageFooter = new PageFooter();
    
    @GetMapping("/makeok")
    public String makeOk(Model model, HttpServletRequest request) {
        UsefulUtilities.getVis(request);
        StringBuilder stringBuilder = new StringBuilder();
        String connectToSrv = "192.168.13.30";
        if (!UsefulUtilities.thisPC().toLowerCase().contains("rups")) {
            connectToSrv = "192.168.13.42";
        }
        try {
            stringBuilder.append(execCommand(connectToSrv, connectToSrv));
        }
        catch (IndexOutOfBoundsException | InterruptedException | ExecutionException | TimeoutException e) {
            stringBuilder.append(e.getMessage());
            Thread.currentThread().checkAccess();
            Thread.currentThread().interrupt();
        }
        model.addAttribute(ModelAttributeNames.ATT_TITLE, connectToSrv + " at " + new Date());
        model.addAttribute("ok", stringBuilder.toString().replace("\n", "<br>"));
        model.addAttribute(ModelAttributeNames.ATT_FOOTER, pageFooter.getInfoAbout(ModelAttributeNames.ATT_FOOTER));
        return "ok";
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OKMaker{");
        sb.append("pageFooter=").append(pageFooter);
        sb.append('}');
        return sb.toString();
    }
    
    private @NotNull String execCommand(String connectToSrv, String commandToExec) throws InterruptedException, ExecutionException, TimeoutException {
        SSHFactory sshFactory = new SSHFactory.Builder(connectToSrv, commandToExec, this.getClass().getSimpleName()).build();
        StringBuilder stringBuilder = new StringBuilder();
    
        sshFactory.setCommandSSH(ConstantsNet.COM_INITPF.replace("initpf", "1915initpf"));
        stringBuilder.append("<p><i>").append(sshFactory.getCommandSSH()).append(STR_BR);
        Future<String> submit = Executors.newSingleThreadExecutor().submit(sshFactory);
        stringBuilder.append(submit.get(30, TimeUnit.SECONDS));
    
        sshFactory.setCommandSSH("sudo squid && exit");
        stringBuilder.append("<p><i>").append(sshFactory.getCommandSSH()).append(STR_BR);
        submit = Executors.newSingleThreadExecutor().submit(sshFactory);
        stringBuilder.append(submit.get(30, TimeUnit.SECONDS));
        
        sshFactory.setCommandSSH("sudo squid -k reconfigure && exit");
        stringBuilder.append("<p><i>").append(sshFactory.getCommandSSH()).append(STR_BR);
        submit = Executors.newSingleThreadExecutor().submit(sshFactory);
        stringBuilder.append(submit.get(30, TimeUnit.SECONDS));
    
        sshFactory.setCommandSSH("sudo pfctl -s nat;sudo pfctl -s rules;sudo ps ax | grep squid && exit");
        stringBuilder.append("<p><i>").append(sshFactory.getCommandSSH()).append(STR_BR);
        submit = Executors.newSingleThreadExecutor().submit(sshFactory);
        stringBuilder.append(submit.get(30, TimeUnit.SECONDS));
        
        return stringBuilder.toString();
    }
}