package abex.os.telemenu;

import java.util.regex.Matcher;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Assert;
import org.junit.Test;

public class BetterTeleportMenuPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BetterTeleportMenuPlugin.class);
		RuneLite.main(args);
	}

	@Test
	public void testKeyMatcher()
	{
		testKeyMatcher("<col=ccccff>1:</col> Foo", "", "1");
		testKeyMatcher("<col=ccccff>1</col>: Foo", "", "1");
		testKeyMatcher("<col=ccccff>1</col> :  Foo", "", "1");
		testKeyMatcher("<col=ccccff>1</col> :  Foo", "(Ignore Me)", "1");
		testKeyMatcher("Foo", "", null);
	}
	void testKeyMatcher(String test, String badSuffix, String key)
	{
		Matcher m = BetterTeleportMenuPlugin.KEY_PREFIX_MATCHER.matcher(test + badSuffix);
		Assert.assertTrue(test, m.find());
		Assert.assertEquals(key, m.group(2));
		Assert.assertEquals("Foo", m.group(4));
		Assert.assertEquals(test, (key != null ? m.group(1) + m.group(2) + m.group(3) : "") + m.group(4));
	}
}