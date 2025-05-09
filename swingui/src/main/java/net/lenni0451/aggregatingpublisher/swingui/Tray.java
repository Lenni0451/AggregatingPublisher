package net.lenni0451.aggregatingpublisher.swingui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Tray {

    public static void showIcon() throws AWTException, IOException {
        PopupMenu popupMenu = new PopupMenu();
        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(e -> Window.open());
        popupMenu.add(showItem);
        MenuItem exitItem = new MenuItem("Exit");
        popupMenu.add(exitItem);
        exitItem.addActionListener(e -> System.exit(0));
        TrayIcon trayIcon = new TrayIcon(resize(ImageIO.read(Tray.class.getResourceAsStream("/icon.png"))), "Aggregating Publisher", popupMenu);
        SystemTray.getSystemTray().add(trayIcon);
    }

    private static BufferedImage resize(final BufferedImage icon) {
        Dimension requiredSize = SystemTray.getSystemTray().getTrayIconSize();
        BufferedImage resized = new BufferedImage(requiredSize.width, requiredSize.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(icon, 0, 0, requiredSize.width, requiredSize.height, null);
        g2d.dispose();
        return resized;
    }

}
