package abex.os.telemenu;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BetterTeleportMenuConfig.GROUP)
public interface BetterTeleportMenuConfig extends Config
{
	String GROUP = "betterteleportmenu";

	@ConfigItem(
		keyName = "ignore",
		name = "",
		description = "ignore me"
	)
	default String unused()
	{
		return null;
	}
}
