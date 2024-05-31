package eu.jodelahithit.clanmemberlistsort;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.clan.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

import java.util.Arrays;
import java.util.Random;

@Slf4j
public class ClanMemberListEntry {
    ClanMemberListSortPlugin plugin;
    Widget opListener;
    Widget icon;
    Widget name;
    Widget world;
    ClanRank clanRank = randomRank();

    public ClanMemberListEntry(ClanMemberListSortPlugin plugin, Widget opListener, Widget name, Widget world, Widget icon) {
        this.plugin = plugin;
        this.opListener = opListener;
        this.name = name;
        this.world = world;
        this.icon = icon;
    }

    private ClanRank randomRank() {
        int pick = new Random().nextInt(ClanRank.values().length);
        return ClanRank.values()[pick];
    }

    public void setOriginalYAndRevalidate(int y) {
        if (opListener != null) {
            opListener.setOriginalY(y);
            opListener.revalidate();
        }
        name.setOriginalY(y);
        name.revalidate();
        world.setOriginalY(y);
        world.revalidate();
        icon.setOriginalY(y);
        icon.revalidate();
    }

    public void updateClanRank(Client client) {
        ClanChannel clanChannel = client.getClanChannel();
        if (clanChannel == null) {
            return;
        }

        ClanSettings clanSettings = client.getClanSettings();
        if (clanSettings == null) {
            return;
        }
        ClanChannelMember member = null;
        try {
            String cleanName =  Text.removeTags(name.getText()); //Fix for wise old man plugin icons
            member = clanChannel.findMember(cleanName);
        } catch (Exception ignored) {
        }
        if (member == null) {
            return;
        }

        clanRank = member.getRank();
    }

    public String getPlayerName() {
        return name.getText();
    }

    public String getWorld() {
        return world.getText();
    }

    public ClanRank getClanRank() {
        return clanRank;
    }
}