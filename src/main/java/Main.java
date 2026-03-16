import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();

            // 全局 UI 微调
            UIManager.put("defaultFont", new Font("Microsoft YaHei UI", Font.PLAIN, 14));
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("ScrollBar.width", 12);

            // 品牌色：跨境电商/出海风格
            UIManager.put("Button.default.background", new Color(31, 78, 121));   // 深海蓝
            UIManager.put("Button.default.foreground", Color.WHITE);
            UIManager.put("Component.focusColor", new Color(42, 157, 143));        // 蓝绿色
            UIManager.put("TextComponent.selectionBackground", new Color(212, 234, 248));
            UIManager.put("Panel.background", new Color(247, 249, 252));
            UIManager.put("ScrollPane.background", Color.WHITE);

        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}