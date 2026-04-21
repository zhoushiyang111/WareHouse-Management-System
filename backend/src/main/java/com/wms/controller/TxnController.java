package com.wms.controller;

import com.wms.domain.StockTxn;
import com.wms.service.WarehouseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TxnController {
  private final WarehouseService service;

  public TxnController(WarehouseService service) {
    this.service = service;
  }

  @GetMapping("/txns")
  public List<StockTxn> list(@RequestParam(value = "itemId", required = false) Long itemId) {
    return service.listTxns(itemId);
  }
}

