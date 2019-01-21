package ru.vachok.networker.ad;


import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import ru.vachok.mysqlandprops.RegRuMysql;
import ru.vachok.mysqlandprops.props.DBRegProperties;
import ru.vachok.mysqlandprops.props.FileProps;
import ru.vachok.mysqlandprops.props.InitProperties;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.componentsrepo.AppComponents;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;


/**
 <h1>Создаёт команды для MS Power Shell, чтобы добавить фото пользователей</h1>

 @since 21.08.2018 (15:57) */
@Service (ConstantsFor.ATT_PHOTO_CONVERTER)
public class PhotoConverterSRV {

    /**
     {@link Logger}
     */
    private static final Logger LOGGER = AppComponents.getLogger();

    private static InitProperties initProperties;

    static {
        try {
            initProperties = new DBRegProperties(ConstantsFor.APP_NAME + ConstantsFor.ATT_PHOTO_CONVERTER);
        } catch (Exception e) {
            initProperties = new FileProps(ConstantsFor.APP_NAME + ConstantsFor.ATT_PHOTO_CONVERTER);
        }
    }

    private Properties properties = getProps();

    private static Properties getProps() {
        try{
            return initProperties.getProps();
        }
        catch(Exception e){
            PhotoConverterSRV.initProperties = new DBRegProperties(ConstantsFor.APP_NAME + PhotoConverterSRV.class.getSimpleName());
            return initProperties.getProps();
        }
    }

    /**
     Путь до папки с фото.
     */
    private String adPhotosPath = properties.getProperty("adphotopath");

    /**
     Файл-фото
     */
    private File adFotoFile;

    private List<String> psCommands = new ArrayList<>();

    /**
     <b>Преобразование в JPG</b>
     Подготавливает фотографии для импорта в ActiveDirectory. Преобразует любой понимаемый {@link BufferedImage} формат в jpg.
     */
    private BiConsumer<String, BufferedImage> imageBiConsumer = (x, y) -> {
        String pathName = properties.getOrDefault("pathName", "\\\\srv-mail3.eatmeat.ru\\c$\\newmailboxes\\foto\\").toString();
        File outFile = new File(pathName + x + ".jpg");
        String fName = "jpg";
        Set<String> samAccountNames = samAccFromDB();
        for (String sam : samAccountNames) {
            if (sam.toLowerCase().contains(x)) x = sam;
        }
        try {
            BufferedImage bufferedImage = new BufferedImage(y.getWidth(), y.getHeight(), BufferedImage.TYPE_INT_RGB);
            bufferedImage.createGraphics().drawImage(y, 0, 0, Color.WHITE, null);
            ImageIO.write(bufferedImage, fName, outFile);
            String msg = outFile.getAbsolutePath() + " written";
            LOGGER.info(msg);
            msg = "Import-RecipientDataProperty -Identity " +
                x + " -Picture -FileData ([Byte[]] $(Get-Content -Path “C:\\newmailboxes\\foto\\" +
                outFile.getName() +
                "\" -Encoding Byte -ReadCount 0))";
            LOGGER.warn(msg);
            psCommands.add(msg);
        } catch (Exception e) {
            AppComponents.getLogger().error(e.getMessage(), e);
            psCommands.add(e.getMessage());
        }
    };

    public File getAdFotoFile() {
        return adFotoFile;
    }

    public void setAdFotoFile(File adFotoFile) {
        this.adFotoFile = adFotoFile;
    }

    public String getAdPhotosPath() {
        return adPhotosPath;
    }

    public void setAdPhotosPath(String adPhotosPath) {
        this.adPhotosPath = adPhotosPath;
    }

    /**
     Создание списка PoShe комманд для добавления фото
     <p>
     Usages: {@link ActDirectoryCTRL#adFoto(PhotoConverterSRV, Model)} , {@link ADSrv#psComm()} <br> Uses: {@link #convertFoto()}

     @return Комманды Exchange PowerShell
     @throws NullPointerException если нет фото
     */
    String psCommands() throws NullPointerException {
        try {
            convertFoto();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : psCommands) {
            stringBuilder.append(s);
            stringBuilder.append("<br>");
        }
        return stringBuilder.toString();
    }

    private void convertFoto() throws NullPointerException, IOException {
        File[] fotoFiles = new File(this.adPhotosPath).listFiles();
        Map<String, BufferedImage> filesList = new HashMap<>();
        if (fotoFiles != null) {
            for (File f : fotoFiles) {
                for (String format : ImageIO.getWriterFormatNames()) {
                    String key = f.getName();
                    if (key.contains(format)) filesList.put(key.replaceFirst("\\Q.\\E" + format, ""), ImageIO.read(f));
                }
            }
        } else filesList.put("No files", null);
        try {
            filesList.forEach(imageBiConsumer);
        } catch (NullPointerException e) {
            filesList.put("ERROR", null);
        }
        initProperties.delProps();
        initProperties.setProps(properties);
    }

    private Set<String> samAccFromDB() {
        Set<String> samAccounts = new HashSet<>();
        Connection c = new RegRuMysql().getDefaultConnection(ConstantsFor.DB_PREFIX + ConstantsFor.STR_VELKOM);
        try (PreparedStatement p = c.prepareStatement("select * from u0466446_velkom.adusers");
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                samAccounts.add(r.getString("samAccountName"));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return samAccounts;
    }
}