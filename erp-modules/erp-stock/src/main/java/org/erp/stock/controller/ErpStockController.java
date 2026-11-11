package org.erp.stock.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.erp.stock.domain.vo.ErpStockAlertVo;
import org.erp.stock.domain.vo.ErpStockLogVo;
import org.erp.stock.domain.vo.ErpStockVo;
import org.erp.stock.mapper.ErpStockAlertMapper;
import org.erp.stock.service.IInventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存查询
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/stock")
public class ErpStockController extends BaseController {

    private final IInventoryService inventoryService;
    private final ErpStockAlertMapper alertMapper;

    @SaCheckPermission("erp:stock:list")
    @GetMapping("/balances")
    public R<List<ErpStockVo>> balances(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String type) {
        return R.ok(inventoryService.listStock(keyword, type));
    }

    /**
     * 库存上下限预警明细（DOC/25 §3.2）：type=LOW/HIGH 可空=全部，不分页。
     */
    @SaCheckPermission("erp:stock:list")
    @GetMapping("/alerts")
    public R<List<ErpStockAlertVo>> alerts(@RequestParam(required = false) String type,
                                           @RequestParam(required = false) Long productId) {
        return R.ok(alertMapper.selectAlerts(type, productId));
    }

    @SaCheckPermission("erp:stock:query")
    @GetMapping("/logs")
    public R<List<ErpStockLogVo>> logs(@RequestParam Long productId) {
        return R.ok(inventoryService.listLogs(productId));
    }

    /**
     * 单产品库存快照（避免前端全量拉取余额列表后本地过滤）。
     */
    @SaCheckPermission("erp:stock:query")
    @GetMapping("/balances/{productId}")
    public R<ErpStockVo> balance(@PathVariable Long productId) {
        return R.ok(inventoryService.snapshot(productId));
    }
}
