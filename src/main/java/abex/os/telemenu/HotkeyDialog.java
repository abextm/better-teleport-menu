package abex.os.telemenu;

import java.awt.BorderLayout;
import java.awt.MouseInfo;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.config.Keybind;

public class HotkeyDialog extends JDialog
{
	public HotkeyDialog(Window owner, String titleText, Keybind current, Consumer<Keybind> done)
	{
		super(owner, ModalityType.APPLICATION_MODAL);
		setTitle("Set hotkey");

		JPanel pane = new JPanel();
		GroupLayout gl = new GroupLayout(pane);
		pane.setLayout(gl);

		JLabel title = new JLabel(titleText);
		HotkeyButton hotkeyBtn = new HotkeyButton(current, false);

		JButton ok = new JButton("Ok");
		ok.addActionListener(ev ->
		{
			done.accept(hotkeyBtn.getValue());
			this.setVisible(false);
			this.dispose();
		});

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(ev ->
		{
			this.setVisible(false);
			this.dispose();
		});

		gl.setAutoCreateGaps(true);
		gl.setAutoCreateContainerGaps(true);

		gl.setVerticalGroup(gl.createSequentialGroup()
			.addComponent(true, title)
			.addComponent(hotkeyBtn)
			.addGroup(gl.createParallelGroup()
				.addComponent(ok)
				.addComponent(cancel)));
		gl.setHorizontalGroup(gl.createParallelGroup()
			.addComponent(title)
			.addComponent(hotkeyBtn)
			.addGroup(gl.createSequentialGroup()
				.addGap(0)
				.addComponent(ok)
				.addComponent(cancel)));

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(pane, BorderLayout.CENTER);

		pack();
		setLocation(MouseInfo.getPointerInfo().getLocation());
		setVisible(true);
	}
}
