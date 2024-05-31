package eu.jodelahithit.clanmemberlistsort;

public enum SortType {
    SORT_BY_NAME("Sort by name"),
    SORT_BY_RANK("Sort by rank"),
    SORT_BY_WORLD("Sort by world");
    String name;
    int actionIndex = -1;

    SortType(String name) {
        this.name = name;
    }
}
