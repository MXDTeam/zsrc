/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import gui.控制台.控制台2号;
import org.jvnet.substance.skin.SubstanceBusinessBlackSteelLookAndFeel;

import javax.swing.*;

import static gui.ZEVMS.*;
import static gui.ZEVMS2.脚本编辑器2;

/**
 *
 * @author Administrator
 */
public class 更多应用 extends javax.swing.JFrame {

    /**
     * Creates new form 更多应用
     */
    public 更多应用() {
        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("gui/图片/pp/2.png"));
        setIconImage(icon.getImage());
        setTitle("更多应用");
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        一键还原 = new javax.swing.JButton();
        锻造控制台 = new javax.swing.JButton();
        角色转移工具 = new javax.swing.JButton();
        脚本编辑器 = new javax.swing.JButton();

        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        一键还原.setFont(new java.awt.Font("幼圆", 0, 18)); // NOI18N
        一键还原.setText("一键还原");
        一键还原.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                一键还原ActionPerformed(evt);
            }
        });
        jPanel1.add(一键还原, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 70, 160, 30));

        锻造控制台.setFont(new java.awt.Font("幼圆", 0, 18)); // NOI18N
        锻造控制台.setText("锻造控制台");
        锻造控制台.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                锻造控制台ActionPerformed(evt);
            }
        });
        jPanel1.add(锻造控制台, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 30, 160, 30));

        角色转移工具.setFont(new java.awt.Font("幼圆", 0, 18)); // NOI18N
        角色转移工具.setText("角色转移工具");
        角色转移工具.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                角色转移工具ActionPerformed(evt);
            }
        });
        jPanel1.add(角色转移工具, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 110, 160, 30));

        脚本编辑器.setFont(new java.awt.Font("幼圆", 0, 18)); // NOI18N
        脚本编辑器.setText("脚本编辑器");
        脚本编辑器.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                脚本编辑器ActionPerformed(evt);
            }
        });
        jPanel1.add(脚本编辑器, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 150, 160, 30));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 290, 470));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void 一键还原ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_一键还原ActionPerformed
        一键还原();
        dispose();
    }//GEN-LAST:event_一键还原ActionPerformed

    private void 锻造控制台ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_锻造控制台ActionPerformed
        锻造控制台();
        dispose();
    }//GEN-LAST:event_锻造控制台ActionPerformed

    private void 角色转移工具ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_角色转移工具ActionPerformed
        角色转移工具();
        dispose();
    }//GEN-LAST:event_角色转移工具ActionPerformed

    private void 脚本编辑器ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_脚本编辑器ActionPerformed
       脚本编辑器2();
    }//GEN-LAST:event_脚本编辑器ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(控制台2号.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.setLookAndFeel(new SubstanceBusinessBlackSteelLookAndFeel());
            // UIManager.setLookAndFeel(new SubstanceBusinessLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new 更多应用().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton 一键还原;
    private javax.swing.JButton 脚本编辑器;
    private javax.swing.JButton 角色转移工具;
    private javax.swing.JButton 锻造控制台;
    // End of variables declaration//GEN-END:variables
}
