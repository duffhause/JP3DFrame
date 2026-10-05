package stonerteam.JP3DFrame;

import javax.swing.JFrame;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.awt.event.ActionEvent;

public class FrameTest {
	public static void main(String[] args) {
		int dx = 900;
		int dy = 600;
		JFrame frame = new JFrame("JP3D Test");
		frame.addWindowListener(new WindowListener() {
			@Override
			public void windowActivated(WindowEvent e) {}
			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowClosing(WindowEvent e) {
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}

			@Override
			public void windowDeactivated(WindowEvent e) {}
			@Override
			public void windowDeiconified(WindowEvent e) {}
			@Override
			public void windowIconified(WindowEvent e) {}
			@Override
			public void windowOpened(WindowEvent e) {}
		});
		
		P3DFrame p3dFrame = new P3DFrame (dx, dy);
		frame.setSize(dx, dy);
		
		JToolBar toolBar = new JToolBar();
		frame.getContentPane().add(toolBar, BorderLayout.NORTH);
		JButton btnLoadPd = new JButton("Load P3D");
		toolBar.add(btnLoadPd);
		
		
		btnLoadPd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser jfc = new JFileChooser("D:\\Java");
				int UserChoice = jfc.showOpenDialog(frame);
				if (UserChoice == JFileChooser.APPROVE_OPTION)
				{
					File SelectedFile = jfc.getSelectedFile();
					p3dFrame.requestP3DLoad(SelectedFile.getAbsolutePath());
				}
			}
		});
		
		JButton btnLoadTerra = new JButton("Load TERRA");
		toolBar.add(btnLoadTerra);
		btnLoadTerra.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser jfc = new JFileChooser("D:\\Java");
				int UserChoice = jfc.showOpenDialog(frame);
				if (UserChoice == JFileChooser.APPROVE_OPTION)
				{
					File SelectedFile = jfc.getSelectedFile();
					p3dFrame.requestP3DLoad(SelectedFile.getAbsolutePath());
				}
			}
		});
		
		frame.add(p3dFrame.getCanvas());
		
		frame.setVisible(true);
		
		return;
	}
}
