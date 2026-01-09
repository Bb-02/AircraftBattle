package com.aircraftwar.ui;

import com.aircraftwar.entity.PlayerAircraft;
import com.aircraftwar.upgrade.UpgradeOption;
import com.aircraftwar.upgrade.UpgradeManager;
import com.aircraftwar.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 简单的升级对话框：三选一升级，阻塞调用线程（使用 invokeAndWait 时安全）
 */
public class UpgradeDialog extends JDialog {
    private int choice = -1;

    public UpgradeDialog(Window owner, PlayerAircraft player, int appliedCount) {
        super(owner, "选择升级", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(12,12));
        setSize(460, 220);
        setResizable(false);
        setLocationRelativeTo(owner);

        JLabel tip = new JLabel("选择一项升级（已升级：" + appliedCount + ")");
        tip.setHorizontalAlignment(SwingConstants.CENTER);
        tip.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        add(tip, BorderLayout.NORTH);

        UpgradeManager um = UpgradeManager.getInstance();

        JPanel center = new JPanel(new GridLayout(1,2,10,10));

        // FIRE
        String fireText = UpgradeOption.FIRE.getLabel() + "  lv" + player.getFireLevel() + "/" + player.getMaxFireLevel();
        JButton b1 = new JButton(fireText);
        if (um.isMaxed(player, UpgradeOption.FIRE)) {
            b1.setText(UpgradeOption.FIRE.getLabel() + "  MAX");
            b1.setEnabled(false);
        }

        // SPEED
        String speedText = UpgradeOption.SPEED.getLabel() + "  lv" + player.getSpeedLevel() + "/" + player.getMaxSpeedLevel();
        JButton b2 = new JButton(speedText);
        if (um.isMaxed(player, UpgradeOption.SPEED)) {
            b2.setText(UpgradeOption.SPEED.getLabel() + "  MAX");
            b2.setEnabled(false);
        }

        // 图标
        BufferedImage iconFire = ImageUtil.loadImage("BulletUpgrade.png");
        if (iconFire != null) {
            b1.setIcon(new ImageIcon(iconFire.getScaledInstance(48, 48, Image.SCALE_SMOOTH)));
            b1.setHorizontalTextPosition(SwingConstants.CENTER);
            b1.setVerticalTextPosition(SwingConstants.BOTTOM);
        }

        BufferedImage iconSpeed = ImageUtil.loadImage("SpeedUpgrade.png");
        if (iconSpeed != null) {
            b2.setIcon(new ImageIcon(iconSpeed.getScaledInstance(48, 48, Image.SCALE_SMOOTH)));
            b2.setHorizontalTextPosition(SwingConstants.CENTER);
            b2.setVerticalTextPosition(SwingConstants.BOTTOM);
        }

        center.add(b1);
        center.add(b2);
        add(center, BorderLayout.CENTER);

        // choice 与 UpgradeOption.values() 的顺序保持一致：0=FIRE, 1=SPEED
        b1.addActionListener(e -> { choice = 0; dispose(); });
        b2.addActionListener(e -> { choice = 1; dispose(); });

        JPanel foot = new JPanel();
        JButton cancel = new JButton("取消");
        cancel.addActionListener(e -> { choice = -1; dispose(); });
        foot.add(cancel);
        add(foot, BorderLayout.SOUTH);
    }

    public int showDialog() {
        setVisible(true);
        return choice;
    }
}
