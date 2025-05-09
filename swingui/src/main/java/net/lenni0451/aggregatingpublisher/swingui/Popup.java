package net.lenni0451.aggregatingpublisher.swingui;

import javax.swing.*;

public class Popup {

    public static void show(final String message, final int type) {
        Runnable runnable = () -> {
            JFrame alwaysOnTop = new JFrame();
            alwaysOnTop.setAlwaysOnTop(true);
            JOptionPane.showMessageDialog(alwaysOnTop, message, "Aggregating Publisher", type);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static void showException(final String message, final Throwable t) {
        show(message + ":\n" + t.getClass().getSimpleName() + (t.getMessage() == null ? "" : (" " + t.getMessage())), JOptionPane.ERROR_MESSAGE);
    }

}
