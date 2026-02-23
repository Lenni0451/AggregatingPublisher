package net.lenni0451.aggregatingpublisher.swingui;

import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.AggregatingPublisher;
import net.lenni0451.aggregatingpublisher.utils.ConfigUtils;
import net.lenni0451.commons.gson.elements.GsonObject;

import javax.swing.*;
import java.io.File;

@Slf4j
public class Main {

    private static final File configFile = new File("config.json");
    public static AggregatingPublisher aggregatingPublisher;
    public static FileCollector fileCollector;

    public static void main(String[] args) {
        try {
            GsonObject config = ConfigUtils.loadConfig(configFile);
            if (config == null) {
                Popup.show("Config file created.\nPlease configure the settings and restart the application.", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Tray.showIcon();
            aggregatingPublisher = new AggregatingPublisher(config.getString("bindAddress", "0.0.0.0"), config.getInt("bindPort", 8080));
            fileCollector = new FileCollector();
            aggregatingPublisher.registerDeploymentManagement(fileCollector);
            ConfigUtils.loadAggregators(aggregatingPublisher, config);
            ConfigUtils.loadPublishers(aggregatingPublisher, config);
            aggregatingPublisher.start();
        } catch (Throwable t) {
            log.error("An error occurred while starting the application", t);
            Popup.showException("An error occurred while starting the application", t);
        }
    }

}
