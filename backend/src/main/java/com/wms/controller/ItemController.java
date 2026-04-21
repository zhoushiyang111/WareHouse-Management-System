package com.wms.controller;

import com.wms.domain.Item;
import com.wms.dto.AdjustStockRequest;
import com.wms.service.WarehouseService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class ItemController {
  private final WarehouseService service;

  public ItemController(WarehouseService service) {
    this.service = service;
  }

  @GetMapping("/items")
  public List<Item> list(@RequestParam(value = "q", required = false) String q) {
    return service.listItems(q);
  }

  @GetMapping("/items/{id}")
  public Item get(@PathVariable("id") Long id) {
    return service.getItem(id);
  }

  @PostMapping("/items")
  public Item create(@Valid @RequestBody Item input) {
    return service.createItem(input);
  }

  @PutMapping("/items/{id}")
  public Item update(@PathVariable("id") Long id, @Valid @RequestBody Item input) {
    return service.updateItem(id, input);
  }

  @DeleteMapping("/items/{id}")
  public void delete(@PathVariable("id") Long id) {
    service.deleteItem(id);
  }

  @PostMapping("/items/{id}/inbound")
  public Item inbound(@PathVariable("id") Long id, @Valid @RequestBody AdjustStockRequest req) {
    return service.inbound(id, req.getQuantity(), req.getNote());
  }

  @PostMapping("/items/{id}/outbound")
  public Item outbound(@PathVariable("id") Long id, @Valid @RequestBody AdjustStockRequest req) {
    return service.outbound(id, req.getQuantity(), req.getNote());
  }
}

