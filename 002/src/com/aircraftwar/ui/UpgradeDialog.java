package com.aircraftwar.ui;

import com.aircraftwar.entity.PlayerAircraft;
import com.aircraftwar.upgrade.UpgradeOption;
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
        setSize(420, 200);
        setResizable(false);
        setLocationRelativeTo(owner);

        JLabel tip = new JLabel("选择一项升级（已升级：" + appliedCount + ")");
        tip.setHorizontalAlignment(SwingConstants.CENTER);
        tip.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        add(tip, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1,3,10,10));
        JButton b1 = new JButton(UpgradeOption.FIRE.getLabel());
        JButton b2 = new JButton(UpgradeOption.SHIELD.getLabel());
        JButton b3 = new JButton(UpgradeOption.SPEED.getLabel());

        // 尝试加载图标并设置到首个按钮
        BufferedImage icon = ImageUtil.loadImage("BulletUpgrade.png");
        if (icon != null) {
            b1.setIcon(new ImageIcon(icon.getScaledInstance(48, 48, Image.SCALE_SMOOTH)));
            b1.setHorizontalTextPosition(SwingConstants.CENTER);
            b1.setVerticalTextPosition(SwingConstants.BOTTOM);
        }

        center.add(b1); center.add(b2); center.add(b3);
        add(center, BorderLayout.CENTER);

        b1.addActionListener(e -> { choice = 0; dispose(); });
        b2.addActionListener(e -> { choice = 1; dispose(); });
        b3.addActionListener(e -> { choice = 2; dispose(); });

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
