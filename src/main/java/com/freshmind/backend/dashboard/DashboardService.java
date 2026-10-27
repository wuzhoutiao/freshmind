package com.freshmind.backend.dashboard;

import com.freshmind.backend.inventory.ExpiryStatusCalculator;
import com.freshmind.backend.inventory.InventoryItem;
import com.freshmind.backend.inventory.InventoryMapper;
import com.freshmind.backend.inventory.InventoryReadService;
import com.freshmind.backend.inventory.InventoryStatus;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final InventoryReadService inventory;
    private final InventoryMapper mapper;
    private final ExpiryStatusCalculator statusCalculator;
    private final Clock clock;

    public DashboardService(
            InventoryReadService inventory,
            InventoryMapper mapper,
            ExpiryStatusCalculator statusCalculator,
            Clock clock) {
        this.inventory = inventory;
        this.mapper = mapper;
        this.statusCalculator = statusCalculator;
        this.clock = clock;
    }

    public DashboardResponse get(UUID ownerId) {
        List<InventoryItem> items = inventory.findAllForRecommendation(ownerId);
        long fresh = count(items, InventoryStatus.FRESH);
        long nearExpiry = count(items, InventoryStatus.NEAR_EXPIRY);
        long expired = count(items, InventoryStatus.EXPIRED);
        List<com.freshmind.backend.inventory.InventoryItemResponse> urgent = items.stream()
                .filter(item -> statusCalculator.status(item.getExpiresOn()) != InventoryStatus.FRESH)
                .sorted(Comparator.comparing(InventoryItem::getExpiresOn))
                .limit(5)
                .map(mapper::toResponse)
                .toList();
        return new DashboardResponse(
                items.size(),
                fresh,
                nearExpiry,
                expired,
                urgent,
                clock.instant());
    }

    private long count(List<InventoryItem> items, InventoryStatus status) {
        return items.stream()
                .filter(item -> statusCalculator.status(item.getExpiresOn()) == status)
                .count();
    }
}

