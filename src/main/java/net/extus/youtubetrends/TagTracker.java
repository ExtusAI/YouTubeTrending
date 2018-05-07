package net.extus.youtubetrends;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TagTracker implements Comparable<TagTracker> {
    private final String tagName;
    private final AtomicInteger views;
    private final List<String> alreadyTrackedIds;

    public TagTracker(final String tagName) {
        this.tagName = tagName;
        this.views = new AtomicInteger();
        this.alreadyTrackedIds = Lists.newArrayList();
    }

    public String getTagName() {
        return tagName;
    }

    public AtomicInteger getViews() {
        return views;
    }

    public List<String> getAlreadyTrackedIds() {
        return alreadyTrackedIds;
    }

    public boolean isTrackingVideo(final String videoId) {
        return this.alreadyTrackedIds.contains(videoId);
    }

    @Override
    public int compareTo(TagTracker o) {
        int compareViews = o.getViews().get();
        return this.getViews().get() - compareViews;
    }
}
