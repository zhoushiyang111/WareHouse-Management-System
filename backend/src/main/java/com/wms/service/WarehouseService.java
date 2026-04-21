package com.wms.service;

import com.wms.domain.Item;
import com.wms.domain.StockTxn;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class WarehouseService {
  private final AtomicLong itemIdSeq = new AtomicLong(0);
  private final AtomicLong txnIdSeq = new AtomicLong(0);
  private final Map<Long, Item> items = new ConcurrentHashMap<>();
  private final Map<Long, StockTxn> txns = new ConcurrentHashMap<>();

  public WarehouseService() {
    // seed some demo data
    createItem(make("SKU-001", "纸箱", "A-01", "阿里巴巴"));
    createItem(make("SKU-002", "胶带", "A-02", "腾讯科技"));
    createItem(make("SKU-003", "泡沫板", "B-01", "字节跳动"));
    inbound(1L, 120, "初始化");
    inbound(2L, 80, "初始化");
    inbound(3L, 40, "初始化");
  }

  private static Item make(String sku, String name, String location, String customerName) {
    Item i = new Item();
    i.setSku(sku);
    i.setName(name);
    i.setLocation(location);
    i.setCustomerName(customerName);
    i.setStock(0);
    return i;
  }

  public List<Item> listItems(String q) {
    return items.values().stream()
        .filter(i -> q == null || q.trim().isEmpty()
            || containsIgnoreCase(i.getSku(), q)
            || containsIgnoreCase(i.getName(), q)
            || containsIgnoreCase(i.getLocation(), q))
        .sorted(Comparator.comparing(Item::getId))
        .collect(Collectors.toList());
  }

  private static boolean containsIgnoreCase(String value, String q) {
    if (value == null) return false;
    return value.toLowerCase().contains(q.toLowerCase());
  }

  public Item getItem(Long id) {
    Item item = items.get(id);
    if (item == null) throw new NotFoundException("Item not found: " + id);
    return item;
  }

  public synchronized Item createItem(Item input) {
    // uniqueness on sku (in-memory)
    String sku = input.getSku();
    if (sku != null) {
      boolean exists = items.values().stream().anyMatch(i -> sku.equalsIgnoreCase(i.getSku()));
      if (exists) throw new BadRequestException("SKU already exists: " + sku);
    }

    long id = itemIdSeq.incrementAndGet();
    Instant now = Instant.now();

    Item item = new Item();
    item.setId(id);
    item.setSku(input.getSku());
    item.setName(input.getName());
    item.setLocation(input.getLocation());
    item.setCustomerName(input.getCustomerName());
    item.setStock(Math.max(0, input.getStock()));
    item.setCreatedAt(now);
    item.setUpdatedAt(now);

    items.put(id, item);
    return item;
  }

  public synchronized Item updateItem(Long id, Item input) {
    Item item = getItem(id);

    String sku = input.getSku();
    if (sku != null && !sku.equalsIgnoreCase(item.getSku())) {
      boolean exists = items.values().stream().anyMatch(i -> sku.equalsIgnoreCase(i.getSku()) && !i.getId().equals(id));
      if (exists) throw new BadRequestException("SKU already exists: " + sku);
      item.setSku(sku);
    }

    if (input.getName() != null) item.setName(input.getName());
    item.setLocation(input.getLocation());
    item.setCustomerName(input.getCustomerName());
    item.setUpdatedAt(Instant.now());
    return item;
  }

  public synchronized void deleteItem(Long id) {
    Item removed = items.remove(id);
    if (removed == null) throw new NotFoundException("Item not found: " + id);
  }

  public synchronized Item inbound(Long itemId, int quantity, String note) {
    if (quantity <= 0) throw new BadRequestException("quantity must be > 0");
    Item item = getItem(itemId);
    item.setStock(item.getStock() + quantity);
    item.setUpdatedAt(Instant.now());
    recordTxn(itemId, StockTxn.Type.INBOUND, quantity, note);
    return item;
  }

  public synchronized Item outbound(Long itemId, int quantity, String note) {
    if (quantity <= 0) throw new BadRequestException("quantity must be > 0");
    Item item = getItem(itemId);
    if (item.getStock() < quantity) throw new BadRequestException("insufficient stock");
    item.setStock(item.getStock() - quantity);
    item.setUpdatedAt(Instant.now());
    recordTxn(itemId, StockTxn.Type.OUTBOUND, quantity, note);
    return item;
  }

  private void recordTxn(Long itemId, StockTxn.Type type, int quantity, String note) {
    long id = txnIdSeq.incrementAndGet();
    StockTxn t = new StockTxn();
    t.setId(id);
    t.setItemId(itemId);
    t.setType(type);
    t.setQuantity(quantity);
    t.setNote(note);
    t.setCreatedAt(Instant.now());
    txns.put(id, t);
  }

  public List<StockTxn> listTxns(Long itemId) {
    List<StockTxn> all = new ArrayList<>(txns.values());
    return all.stream()
        .filter(t -> itemId == null || itemId.equals(t.getItemId()))
        .sorted(Comparator.comparing(StockTxn::getId).reversed())
        .collect(Collectors.toList());
  }
}

