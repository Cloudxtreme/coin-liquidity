package com.coinliquidity.web;

import com.coinliquidity.core.OrderBookDownloader;
import com.coinliquidity.core.model.DownloadStatus;
import com.coinliquidity.core.model.DownloadStatusKey;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatusCache {

    private Map<DownloadStatusKey, DownloadStatus> downloadStatuses = new ConcurrentSkipListMap<>();

    void update(final List<OrderBookDownloader> downloaders) {
        final Stream<DownloadStatus> statuses = downloaders.stream()
                .map(OrderBookDownloader::getDownloadStatuses)
                .flatMap(Collection::stream);

        process(statuses);
    }

    void process(final Stream<DownloadStatus> statuses) {
        final Stopwatch now = Stopwatch.createStarted();

        final Set<DownloadStatusKey> currentKeys = Sets.newHashSet();

        statuses.forEach(status -> {
            final DownloadStatusKey key = status.toKey();
            currentKeys.add(key);
            final DownloadStatus current = downloadStatuses.getOrDefault(key, status);

            current.setSizeBytes(status.getSizeBytes());
            current.setStatus(status.getStatus());
            current.setTimeElapsed(status.getTimeElapsed());
            current.setTotalAsks(status.getTotalAsks());
            current.setTotalBids(status.getTotalBids());
            current.setUpdateTime(status.getUpdateTime());

            if (DownloadStatus.OK.equals(status.getStatus())) {
                current.setLastOk(now);
                // clear old errors
                if (current.getLastError() != null && current.getLastError().elapsed(TimeUnit.HOURS) > 1) {
                    current.setLastError(null);
                    current.setLastErrorMessage(null);
                }
            } else if (DownloadStatus.ERROR.equals(status.getStatus())) {
                current.setLastError(now);
                current.setLastErrorMessage(status.getLastErrorMessage());
                current.setErrorCount(current.getErrorCount() + 1);
            }

            downloadStatuses.put(key, current);
        });

        downloadStatuses.keySet().removeIf(key -> !currentKeys.contains(key));
    }

    public Collection<DownloadStatus> getDownloadStatuses() {
        return downloadStatuses.values();
    }
}
