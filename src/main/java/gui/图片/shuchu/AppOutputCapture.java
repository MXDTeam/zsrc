package gui.ͼƬ.shuchu;


import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;

public class AppOutputCapture {

    private static Process process;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("�÷���java AppOutputCapture "
                    + "<��������> {����1 ����2 ...}");
            System.exit(0);
        }

        try {
            // ����������ָ��������½���
            process = Runtime.getRuntime().exec(args);
        } catch (IOException e) {
            System.err.println("��������ʱ����...\n" + e);
            System.exit(1);
        }

        // ����½�����д�����
        InputStream[] inStreams
                = new InputStream[]{
                    process.getInputStream(), process.getErrorStream()};
        ConsoleTextArea cta = new ConsoleTextArea(inStreams);
        cta.setFont(java.awt.Font.decode("monospaced"));

        JFrame frame = new JFrame(args[0]
                + "����̨���");

        frame.getContentPane().add(new JScrollPane(cta),
                BorderLayout.CENTER);
        frame.setBounds(50, 50, 400, 400);
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                process.destroy();
                try {
                    process.waitFor(); // ��Win98�¿��ܱ�����
                } catch (InterruptedException e) {
                }
                System.exit(0);
            }
        });
    } // main()
} // AppOutputCapture
