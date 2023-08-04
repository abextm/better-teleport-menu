package abex.os.telemenu;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.inject.Provides;
import java.applet.Applet;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostStructComposition;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Better Teleport Menu",
	description = "Customize hotkeys for the Spirit Tree/Jewelery box/Portal nexus layout/Diary/Construction cape interfaces",
	tags = "poh,jewelery,cape,diary,tele,port,nexus,hotkey,keybind"
)
public class BetterTeleportMenuPlugin extends Plugin implements KeyListener
{
	private static final int PARAMID_TELENEXUS_DESTINATION_NAME = 660;

	@VisibleForTesting
	static final Pattern KEY_PREFIX_MATCHER = Pattern.compile("^(<[^>]+>)([A-Za-z0-9])(:</[^>]+> |</[^>]+> *: +)(.*?)((?:\\([^)]+\\))?)$");

	private static final Map<Integer, String> ALTERNATE_NEXUS_NAMES = ImmutableMap.<Integer, String>builder()
		.put(459, "Digsite")
		.put(460, "Ape Atoll")
		.put(461, "Canifis")
		.put(466, "Demonic Ruins")
		.put(469, "Frozen Waste Plateau")
		.put(470, "Graveyard of Shadows")
		.build();

	// cleanify(display) -> canon
	private static Map<String, String> canonicalNames = new HashMap<>();
	// canon -> old
	private static Multimap<String, String> migrateNames = HashMultimap.create();

	static
	{
		migrateNames.put("telenexus-carralanger", "telenexus-carralangar");
	}

	@Inject
	private Client client;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BetterTeleportMenuConfig config;

	@Inject
	ConfigManager configManager;

	private List<KeyEvent> recentKeypresses = new ArrayList<>();

	private List<TeleMenu> teleMenus = new ArrayList<>();

	private int timeout = 0;
	private boolean menuJustOpened = false;

	@Subscribe
	private void onGameTick(GameTick t)
	{
		List<TeleMenu> change = null;
		for (TeleMenu menu : teleMenus)
		{
			if (client.getWidget(menu.textWidget.getId() >> 16, 0) == null)
			{
				if (change == null)
				{
					change = new ArrayList<>(teleMenus);
				}
				change.remove(menu);
			}
		}
		if (change != null)
		{
			teleMenus = change;
		}
	}

	private String activeMenu = null;

	@Subscribe
	private void onScriptPreFired(ScriptPreFired ev)
	{
		switch (ev.getScriptId())
		{
			case ScriptID.MENU_SETUP:
			{
				String title = client.getStringStack()[client.getStringStackSize() - 1];
				activeMenu = cleanify(title);
				teleMenus = new ArrayList<>();
				break;
			}
		}
	}

	@Subscribe
	private void onPostStructComposition(PostStructComposition ev)
	{
		String newName = ALTERNATE_NEXUS_NAMES.get(ev.getStructComposition().getId());
		if (newName != null)
		{
			if (config.alternateNames())
			{
				String oldName = ev.getStructComposition().getStringValue(PARAMID_TELENEXUS_DESTINATION_NAME);
				// the old name in parentheses is stripped before being passed to cleanify
				String cleanOld = "telenexus-" + cleanify(oldName);
				String cleanNew = "telenexus-" + cleanify(newName);
				canonicalNames.put(cleanNew + "-", cleanOld);
				migrateNames.put(cleanOld, cleanNew); // versions which wrote this did not include the -
				ev.getStructComposition().setValue(PARAMID_TELENEXUS_DESTINATION_NAME, newName + " (" + oldName + ")");
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged ev)
	{
		if (!BetterTeleportMenuConfig.GROUP.equals(ev.getGroup()))
		{
			return;
		}

		if ("alternateNames".equals(ev.getKey()))
		{
			clientThread.invoke(() ->
				client.getStructCompositionCache().reset());
		}
	}

	@Subscribe
	private void onScriptPostFired(ScriptPostFired ev)
	{
		switch (ev.getScriptId())
		{
			case ScriptID.MENU_CREATEENTRY:
				if (activeMenu != null)
				{
					new TeleMenu()
						.textWidget(client.getScriptActiveWidget())
						.resumeWidget(client.getScriptActiveWidget())
						.opWidget(client.getScriptActiveWidget())
						.keyListenerWidget(client.getScriptDotWidget())
						.identifier(activeMenu)
						.build();
				}
				break;
			case ScriptID.TELENEXUS_CREATE_TELELINE:
				new TeleMenu()
					.textWidget(client.getScriptActiveWidget())
					.resumeWidget(client.getScriptDotWidget())
					.opWidget(client.getWidget(client.getScriptActiveWidget().getId() + 1)
						.getChild(client.getScriptActiveWidget().getIndex()))
					.keyListenerWidget(client.getScriptDotWidget())
					.identifier("telenexus")
					.build();
				break;
			case ScriptID.POH_JEWELLERY_BOX_ADDBUTTON:
				new TeleMenu()
					.textWidget(client.getScriptActiveWidget())
					.resumeWidget(client.getScriptDotWidget())
					.opWidget(client.getScriptActiveWidget())
					.keyListenerWidget(client.getScriptDotWidget())
					.identifier("jewelbox")
					.build();
				break;
		}
	}

	@NoArgsConstructor
	@Accessors(fluent = true, chain = true)
	class TeleMenu
	{
		@Setter
		Widget textWidget;
		@Setter
		Widget resumeWidget;
		@Setter
		Widget opWidget;
		@Setter
		Widget keyListenerWidget;

		@Setter
		String identifier = "";

		@Setter
		String highlightTag = "<shad=ffffff>";

		String displayText;
		char defaultBind;
		String preText;
		String postText;

		boolean highlight;

		Multikeybind bind;

		public void build()
		{
			if (!this.identifier.isEmpty())
			{
				this.identifier += "-";
			}

			Matcher m = KEY_PREFIX_MATCHER.matcher(textWidget.getText());
			if (!m.find())
			{
				log.warn("bad msg \"{}\"", textWidget.getText());
				return;
			}
			preText = m.group(1);
			defaultBind = Character.toUpperCase(m.group(2).charAt(0));
			postText = m.group(3);
			displayText = m.group(4) + m.group(5);

			this.identifier += cleanify(m.group(4));
			this.identifier = canonicalNames.getOrDefault(this.identifier, this.identifier);
			String strConfigValue = null;
			for (String ident : migrateNames.get(this.identifier))
			{
				if (ident.equals(identifier))
				{
					continue;
				}

				String v = configManager.getConfiguration(BetterTeleportMenuConfig.GROUP, BetterTeleportMenuConfig.KEYBIND_PREFIX + ident);
				if (v != null)
				{
					strConfigValue = v;
					configManager.unsetConfiguration(BetterTeleportMenuConfig.GROUP, BetterTeleportMenuConfig.KEYBIND_PREFIX + ident);
				}
			}
			{
				String v = configManager.getConfiguration(BetterTeleportMenuConfig.GROUP, BetterTeleportMenuConfig.KEYBIND_PREFIX + identifier);
				if (v != null)
				{
					strConfigValue = v;
				}
				else if (strConfigValue != null)
				{
					configManager.setConfiguration(BetterTeleportMenuConfig.GROUP, BetterTeleportMenuConfig.KEYBIND_PREFIX + identifier, strConfigValue);
				}
			}
			this.bind = Multikeybind.fromConfig(strConfigValue);
			if (this.bind == null)
			{
				this.bind = new Multikeybind(new Keybind(defaultBind, 0));
			}

			clearKeyListener();
			hotkeyChanged();

			if (opWidget.getOnOpListener() == null)
			{
				// otherwise the actions don't get shown
				opWidget.setOnOpListener(net.runelite.api.ScriptID.NULL);
			}

			List<TeleMenu> change = new ArrayList<>(teleMenus);
			change.add(this);
			change.sort(Comparator.comparing((TeleMenu tm) -> tm.bind.getKeybinds().size()).reversed());
			teleMenus = change;

			menuJustOpened = true;
		}

		void hotkeyChanged()
		{
			opWidget.setAction(8, "Set Hotkey (" + this.bind + ")");
			if (this.bind.isUnset())
			{
				textWidget.setText(displayText);
			}
			else
			{
				String preHighlight = this.highlight ? this.highlightTag : "";
				textWidget.setText(this.preText + this.bind + this.postText + preHighlight + displayText);
			}
		}

		void openSetDialog()
		{
			SwingUtilities.invokeLater(() ->
			{
				Window window = null;
				for (Component c = (Applet) client; c != null; c = c.getParent())
				{
					if (c instanceof Window)
					{
						window = (Window) c;
						break;
					}
				}
				new HotkeyDialog(window, this.displayText, new Multikeybind(new Keybind(defaultBind, 0)), bind, bind ->
				{
					this.bind = bind;
					configManager.setConfiguration(BetterTeleportMenuConfig.GROUP, BetterTeleportMenuConfig.KEYBIND_PREFIX + identifier, bind.toConfig());
					clientThread.invokeLater(() -> hotkeyChanged());
				});
			});
		}

		void clearKeyListener()
		{
			keyListenerWidget.setOnKeyListener((Object[]) null);
		}

		void onTrigger()
		{
			if (timeout >= client.getGameCycle())
			{
				return;
			}

			resume(resumeWidget);
			textWidget.setText("Please wait...");
			timeout = client.getGameCycle() + 20;
		}

		Multikeybind.MatchState matches(List<KeyEvent> keyEvent)
		{
			if (bind == null)
			{
				return Multikeybind.MatchState.NO;
			}

			return bind.matches(keyEvent, config.aliasNumpad());
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked ev)
	{
		if (ev.getMenuAction() == MenuAction.CC_OP_LOW_PRIORITY && ev.getId() == 9)
		{
			for (TeleMenu menu : teleMenus)
			{
				if (menu.opWidget.getId() == ev.getParam1() && menu.opWidget.getIndex() == ev.getParam0())
				{
					menu.openSetDialog();
					ev.consume();
					return;
				}
			}
		}
	}

	@Override
	public void resetConfiguration()
	{
		for (String key : configManager.getConfigurationKeys(BetterTeleportMenuConfig.GROUP + ".keybind."))
		{
			int firstDot = key.indexOf('.');
			if (firstDot != -1)
			{
				configManager.unsetConfiguration(BetterTeleportMenuConfig.GROUP, key.substring(firstDot + 1));
			}
		}
	}

	private void resume(Widget w)
	{
		assert w.getId() == w.getParentId();
		// we are abusing this cs2 to just do a cc_find + cc_resume_pausebutton for us
		client.runScript(ScriptID.SOMETHING_THAT_CC_RESUME_PAUSEBUTTON, w.getId(), w.getIndex());
	}

	private static String cleanify(String in)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < in.length(); i++)
		{
			char c = in.charAt(i);
			c = Character.toLowerCase(c);
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
			{
				sb.append(c);
			}
			else if (c == '-' || c == ' ')
			{
				sb.append('-');
			}
		}
		return sb.toString();
	}

	@Override
	protected void startUp() throws Exception
	{
		keyManager.registerKeyListener(this);
		// bleh idc to make this work
	}

	@Override
	protected void shutDown() throws Exception
	{
		keyManager.unregisterKeyListener(this);
		clientThread.invokeLater(() ->
		{
			client.getStructCompositionCache().reset();
		});
		// less bleh to make work but idc still
	}

	@Override
	public void keyTyped(KeyEvent keyEvent)
	{

	}

	@Override
	public void keyPressed(KeyEvent keyEvent)
	{
		if (teleMenus.isEmpty())
		{
			return;
		}

		if (menuJustOpened)
		{
			menuJustOpened = false;
			recentKeypresses.clear();
		}

		recentKeypresses.add(keyEvent);
		for (; recentKeypresses.size() > 8; recentKeypresses.remove(0)) ;
		for (TeleMenu menu : teleMenus)
		{
			Multikeybind.MatchState match = menu.matches(recentKeypresses);
			if (match != Multikeybind.MatchState.NO)
			{
				keyEvent.consume();

				if (match == Multikeybind.MatchState.YES)
				{
					clientThread.invokeLater(() -> menu.onTrigger());
					return;
				}
			}
			if (menu.highlight != (match == Multikeybind.MatchState.PARTIAL))
			{
				menu.highlight = match == Multikeybind.MatchState.PARTIAL;
				menu.hotkeyChanged();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent keyEvent)
	{

	}

	@Provides
	BetterTeleportMenuConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterTeleportMenuConfig.class);
	}
}
