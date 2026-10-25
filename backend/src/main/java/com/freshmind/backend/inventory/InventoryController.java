package com.freshmind.backend.inventory;

import com.freshmind.backend.catalog.IngredientCategory;
import com.freshmind.backend.common.error.InvalidRequestException;
import com.freshmind.backend.common.web.PageResponse;
import com.freshmind.backend.session.SessionAuthenticationInterceptor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryWriteService writer;
    private final InventoryReadService reader;

    public InventoryController(InventoryWriteService writer, InventoryReadService reader) {
        this.writer = writer;
        this.reader = reader;
    }

    @PostMapping
    public ResponseEntity<InventoryItemResponse> create(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId,
            @Valid @RequestBody CreateInventoryRequest request) {
        InventoryItemResponse item = writer.create(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/inventory/" + item.id())).body(item);
    }

    @GetMapping
    public PageResponse<InventoryItemResponse> list(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) IngredientCategory category,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "expiry") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Sort.Direction parsedDirection = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new InvalidRequestException("direction must be asc or desc."));
        return reader.list(userId, query, category, status, page, size, sortBy, parsedDirection);
    }

    @GetMapping("/{itemId}")
    public InventoryItemResponse get(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId,
            @PathVariable UUID itemId) {
        return reader.get(userId, itemId);
    }

    @PatchMapping("/{itemId}")
    public InventoryItemResponse update(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        return writer.update(userId, itemId, request);
    }

    @PostMapping("/{itemId}/consume")
    public ConsumptionResponse consume(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId,
            @PathVariable UUID itemId,
            @Valid @RequestBody ConsumeInventoryRequest request) {
        return writer.consume(userId, itemId, request);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId,
            @PathVariable UUID itemId) {
        writer.delete(userId, itemId);
        return ResponseEntity.noContent().build();
    }
}
