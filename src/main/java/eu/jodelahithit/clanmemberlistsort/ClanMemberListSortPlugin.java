package eu.jodelahithit.clanmemberlistsort;

import com.google.inject.Provides;

import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.util.*;
import java.util.List;

@PluginDescriptor(
        name = "Clan Member List Sorting",
        description = "Adds a sort button to the clan member list",
        tags = {"clan", "members", "list", "sorting", "alphabetically", "world", "rank", "role"}
)
public class ClanMemberListSortPlugin extends Plugin {
    static final String CONFIG_GROUP = "clanmemberlistsorting";

    final int WIDGET_HEIGHT = 15;
    private Widget clanMemberListHeaderWidget;
    private Widget clanMemberListsWidget;
    private Widget sortButton;

    @Inject
    Client client;
    @Inject
    ClientThread clientThread;
    @Inject
    ClanMemberListSortConfig config;

    private long lastSortTime = 0;
    private static final long SORT_INTERVAL = 1000;

    @Provides
    ClanMemberListSortConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(ClanMemberListSortConfig.class);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (configChanged.getGroup().equals(CONFIG_GROUP)) {
            if (configChanged.getKey().equals("reverseSort")) {
                updateSortButtonSprite();
            }
        }
    }

    @Override
    public void startUp() {
        clientThread.invokeLater(this::initWidgets);
    }

    @Override
    public void shutDown() {
        if (clanMemberListHeaderWidget != null) clanMemberListHeaderWidget.deleteAllChildren();
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == ComponentID.CLAN_MEMBERS >> 16) {
            initWidgets();
        }
    }

    private Widget GetOnOpListenerWidgetFromName(Widget[] widgets, String name) {
        for (int i = 0; i < widgets.length; i++) {
            if (widgets[i].getOnOpListener() != null) {
                if (Text.removeTags(widgets[i].getName()).compareTo(name) == 0) return widgets[i];
            }
        }
        return null;
    }

    private final Map<String, Long> lastChatTimestamps = new HashMap<>();
    private List<ClanMemberListEntry> entries = new ArrayList<>();

    @Subscribe
    public void onGameTick(GameTick e) {
        if (clanMemberListsWidget == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSortTime < SORT_INTERVAL) {
            return;
        }
        lastSortTime = currentTime;


        entries = new ArrayList<>();

        //Widgets are always in the same order for other players: name, world, icon. OpListener widget location does not seem to have a reliable position
        //Local player doesn't have an opListener so we have to skip it

        Widget[] widgets = clanMemberListsWidget.getChildren();
        if (widgets == null) return;

        for (int i = 0; i < widgets.length - 3; i++) {
            int firstType = widgets[i].getType();
            if (firstType == 3 || firstType == 5) {
                if (widgets[i + 1].getType() == 4 && widgets[i + 2].getType() == 4 && widgets[i + 3].getType() == 5) {
                    entries.add(new ClanMemberListEntry(this, GetOnOpListenerWidgetFromName(widgets, widgets[i + 1].getText()), widgets[i + 1], widgets[i + 2], widgets[i + 3]));
                }
            }
        }

        sort();
    }

    private void sort(){
        Comparator<ClanMemberListEntry> comparator = null;
        switch (config.activeSortType()) {
            case SORT_BY_WORLD:
                comparator = Comparator.comparing(ClanMemberListEntry::getWorld);
                break;
            case SORT_BY_NAME:
                comparator = Comparator.comparing(ClanMemberListEntry::getPlayerName);
                break;
            case SORT_BY_RANK:
                entries.forEach(entry -> entry.updateClanRank(client));
                comparator = Comparator.comparing(ClanMemberListEntry::getClanRankAsInt);
                break;
            case SORT_BY_RECENT_CHAT:
                comparator = Comparator.comparing((ClanMemberListEntry entry) -> lastChatTimestamps.getOrDefault(entry.getPlayerName(), 0L)).reversed();
                break;
        }
        entries.sort(config.reverseSort() ? comparator.reversed() : comparator);

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setOriginalYAndRevalidate(WIDGET_HEIGHT * i);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if(event.getType() != ChatMessageType.CLAN_CHAT && event.getType() != ChatMessageType.CLAN_GUEST_CHAT) return;
        String playerName =  Text.removeTags(event.getName()); //Fix for wise old man plugin icons
        lastChatTimestamps.put(Text.toJagexName(playerName), System.currentTimeMillis());
    }

    private void initWidgets() {
        clanMemberListsWidget = client.getWidget(ComponentID.CLAN_MEMBERS);
        clanMemberListHeaderWidget = client.getWidget(ComponentID.CLAN_MEMBERS >> 16, 0);

        if (clanMemberListHeaderWidget == null) return;

        clanMemberListHeaderWidget.deleteAllChildren();

        sortButton = clanMemberListHeaderWidget.createChild(-1, WidgetType.GRAPHIC);
        reorderSortButton(config.activeSortType());
        sortButton.setOriginalY(2);
        sortButton.setOriginalX(2);
        sortButton.setOriginalHeight(16);
        sortButton.setOriginalWidth(16);
        sortButton.setOnClickListener((JavaScriptCallback) this::handleSortButtonClick);
        sortButton.setOnOpListener((JavaScriptCallback) this::handleSortButtonOp);
        sortButton.setHasListener(true);
        updateSortButtonSprite();
        sortButton.revalidate();
    }

    private void updateSortButtonSprite() {
        sortButton.setSpriteId(config.reverseSort() ? SpriteID.SCROLLBAR_ARROW_UP : SpriteID.SCROLLBAR_ARROW_DOWN);
    }

    private void handleSortButtonClick(ScriptEvent event) {
        config.reverseSort(!config.reverseSort());
        updateSortButtonSprite();
        sort();
    }

    private void handleSortButtonOp(ScriptEvent event) {
        for (SortType type : SortType.values()) {
            if (type.actionIndex == event.getOp()) {
                config.activeSortType(type);
                reorderSortButton(type);
                return;
            }
        }
    }

    private void reorderSortButton(SortType firstType) {
        int index = 0;
        sortButton.setAction(index, firstType.name);
        firstType.actionIndex = 1;
        for (SortType type : SortType.values()) {
            if (type == firstType) continue;
            sortButton.setAction(++index, type.name);
            type.actionIndex = index + 1;
        }
        sort();
    }
}
