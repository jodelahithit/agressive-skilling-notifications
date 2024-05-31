package eu.jodelahithit.clanmemberlistsort;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(ClanMemberListSortPlugin.CONFIG_GROUP)
public interface ClanMemberListSortConfig extends Config {
    @ConfigItem(
            keyName = "activeSortType",
            name = "Active sort method",
            description = "The active sort method that's used for sorting the clan member list"
    )
    default SortType activeSortType() {
        return SortType.SORT_BY_NAME;
    }

    @ConfigItem(
            keyName = "activeSortType",
            name = "",
            description = ""
    )
    void activeSortType(SortType sortType);

    @ConfigItem(
            keyName = "reverseSort",
            name = "Reverse sort method",
            description = "Reverse the sort method"
    )
    default boolean reverseSort() {
        return false;
    }

    @ConfigItem(
            keyName = "reverseSort",
            name = "",
            description = ""
    )
    void reverseSort(boolean value);
}
