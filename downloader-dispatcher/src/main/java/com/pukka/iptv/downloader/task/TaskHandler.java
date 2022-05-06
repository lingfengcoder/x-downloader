package com.pukka.iptv.downloader.task;

@FunctionalInterface
public interface TaskHandler {
    boolean handler();
}
