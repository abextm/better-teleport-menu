package abex.os.telemenu;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import javax.swing.JPanel;
import junit.framework.TestCase;
import net.runelite.client.config.Keybind;
import org.junit.Assert;
import org.junit.Test;

public class MultikeybindTest extends TestCase
{
	@Test
	public void testAliasNumpad()
	{
		Assert.assertEquals(KeyEvent.VK_NUMPAD0, Multikeybind.swapNumpadKey(KeyEvent.VK_0));
		Assert.assertEquals(KeyEvent.VK_0, Multikeybind.swapNumpadKey(KeyEvent.VK_NUMPAD0));
		Assert.assertEquals(KeyEvent.VK_NUMPAD2, Multikeybind.swapNumpadKey(KeyEvent.VK_2));
		Assert.assertEquals(KeyEvent.VK_2, Multikeybind.swapNumpadKey(KeyEvent.VK_NUMPAD2));
	}

	public void testMatches()
	{
		Multikeybind ab = new Multikeybind(new Keybind(KeyEvent.VK_A, 0), new Keybind(KeyEvent.VK_B, 0));
		Assert.assertEquals(Multikeybind.MatchState.NO, ab.matches(Arrays.asList(ke(KeyEvent.VK_B, 0))));
		Assert.assertEquals(Multikeybind.MatchState.NO, ab.matches(Arrays.asList()));
		Assert.assertEquals(Multikeybind.MatchState.PARTIAL, ab.matches(Arrays.asList(ke(KeyEvent.VK_A, 0))));
		Assert.assertEquals(Multikeybind.MatchState.PARTIAL, ab.matches(Arrays.asList(ke(KeyEvent.VK_A, 0), ke(KeyEvent.VK_SHIFT, 0))));
		Assert.assertEquals(Multikeybind.MatchState.YES, ab.matches(Arrays.asList(ke(KeyEvent.VK_A, 0), ke(KeyEvent.VK_B, 0))));
		Assert.assertEquals(Multikeybind.MatchState.YES, ab.matches(Arrays.asList(ke(KeyEvent.VK_A, 0), ke(KeyEvent.VK_SHIFT, 0), ke(KeyEvent.VK_B, 0))));
		Assert.assertEquals(Multikeybind.MatchState.NO, ab.matches(Arrays.asList(ke(KeyEvent.VK_SHIFT, 0))));

		Multikeybind aB = new Multikeybind(new Keybind(KeyEvent.VK_A, 0), new Keybind(KeyEvent.VK_B, KeyEvent.SHIFT_DOWN_MASK));
		Assert.assertEquals(Multikeybind.MatchState.YES, aB.matches(Arrays.asList(ke(KeyEvent.VK_A, 0), ke(KeyEvent.VK_SHIFT, 0), ke(KeyEvent.VK_B, KeyEvent.SHIFT_DOWN_MASK))));

		Multikeybind shift = new Multikeybind(new Keybind(KeyEvent.VK_UNDEFINED, KeyEvent.SHIFT_DOWN_MASK));
		Assert.assertEquals(Multikeybind.MatchState.YES, shift.matches(Arrays.asList(ke(KeyEvent.VK_A, 0), ke(KeyEvent.VK_SHIFT, 0))));
	}

	private KeyEvent ke(int keyCode, int modifiers)
	{
		KeyEvent ke = new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, 'x');
		try
		{
			Field f = KeyEvent.class.getDeclaredField("extendedKeyCode");
			f.setAccessible(true);
			f.set(ke, keyCode);
		}
		catch (ReflectiveOperationException e)
		{
			throw new RuntimeException(e);
		}
		return ke;
	}
}