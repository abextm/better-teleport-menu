package abex.os.telemenu;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BetterTeleportMenuConfig.GROUP)
public interface BetterTeleportMenuConfig extends Config
{
	String GROUP = "betterteleportmenu";

	@ConfigItem(
		keyName = "aliasNumpad",
		name = "Alias Numpad keys",
		description = "Treat numpad keys as their number row variants"
	)
	default boolean aliasNumpad()
	{
		return true;
	}
}
