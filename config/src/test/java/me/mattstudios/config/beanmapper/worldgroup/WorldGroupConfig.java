package me.mattstudios.config.beanmapper.worldgroup;

import java.util.Map;

/**
 * Main class for the world groups configuration.
 */
public class WorldGroupConfig {

    private Map<String, Group> groups;

    public Map<String, Group> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Group> groups) {
        this.groups = groups;
    }
}