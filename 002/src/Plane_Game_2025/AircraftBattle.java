package Plane_Game_2025;

import javax.swing.JFrame;
import javax.swing.UIManager;

public class AircraftBattle {
    public static void main(String[] args) {
        try {
            // 适配系统外观，避免字体和样式异常
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("经典飞机大战");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null); // 居中显示
        frame.setResizable(false); // 禁止缩放

        GamePanel gamePanel = new GamePanel();
        frame.add(gamePanel);

        frame.setVisible(true);
        gamePanel.startGame(); // 启动游戏
    }
}