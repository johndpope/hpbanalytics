package com.highpowerbear.hpbanalytics.rest;

import com.highpowerbear.hpbanalytics.dao.IbLoggerDao;
import com.highpowerbear.hpbanalytics.dao.filter.FilterParser;
import com.highpowerbear.hpbanalytics.dao.filter.IbOrderFilter;
import com.highpowerbear.hpbanalytics.entity.IbAccount;
import com.highpowerbear.hpbanalytics.entity.IbOrder;
import com.highpowerbear.hpbanalytics.iblogger.HeartbeatControl;
import com.highpowerbear.hpbanalytics.iblogger.IbController;
import com.highpowerbear.hpbanalytics.iblogger.Position;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by robertk on 11/18/2017.
 */
@RestController
@RequestMapping("/iblogger")
public class IbLoggerRestController {

    @Autowired private IbLoggerDao ibLoggerDao;
    @Autowired private IbController ibController;
    @Autowired FilterParser filterParser;
    @Autowired HeartbeatControl heartbeatControl;

    @RequestMapping("/ibaccounts")
    public RestList<IbAccount> getIbAccount() {
        List<IbAccount> ibAccounts = ibLoggerDao.getIbAccounts();
        ibAccounts.forEach(ibAccount -> ibAccount.setIbConnection(ibController.getIbConnection(ibAccount.getAccountId())));

        return new RestList<>(ibAccounts, (long) ibAccounts.size());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/ibaccounts")
    public ResponseEntity<?> updateIbAccount(
            @RequestBody IbAccount ibAccount) {

        IbAccount ibAccountDb = ibLoggerDao.findIbAccount(ibAccount.getAccountId());
        if (ibAccountDb == null) {
            return ResponseEntity.notFound().build();
        }
        ibAccountDb = ibLoggerDao.updateIbAccount(ibAccount);

        return ResponseEntity.ok(ibAccountDb);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/ibaccounts/{accountId}/connect/{connect}")
    public ResponseEntity<?> connectIbAccount(
            @PathVariable("accountId") String accountId,
            @PathVariable("connect") boolean connect) {

        IbAccount ibAccount = ibLoggerDao.findIbAccount(accountId);
        if (ibAccount == null) {
            return ResponseEntity.notFound().build();
        }

        if (connect) {
            ibController.connect(accountId);
        } else {
            ibController.disconnect(accountId);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            // Ignore
        }
        ibAccount.setIbConnection(ibController.getIbConnection(accountId));

        return ResponseEntity.ok(ibAccount);
    }

    @RequestMapping("/ibaccounts/{accountId}/iborders")
    public ResponseEntity<?> getFilteredIbOrders(
            @PathVariable("accountId") String accountId,
            @RequestParam(required = false, value = "filter") String jsonFilter,
            @RequestParam("start") Integer start,
            @RequestParam("limit") Integer limit) {

        IbAccount ibAccount = ibLoggerDao.findIbAccount(accountId);
        if (ibAccount == null) {
            return ResponseEntity.notFound().build();
        }

        List<IbOrder> ibOrders = new ArrayList<>();

        IbOrderFilter filter = filterParser.parseIbOrderFilter(jsonFilter);
        Map<IbOrder, Integer> hm = heartbeatControl.getOpenOrderHeartbeatMap().get(accountId);

        for (IbOrder ibOrder : ibLoggerDao.getFilteredIbOrders(accountId, filter, start, limit)) {
            ibOrder.setHeartbeatCount(hm.get(ibOrder));
            ibOrders.add(ibOrder);
        }

        return ResponseEntity.ok(new RestList<>(ibOrders, ibLoggerDao.getNumFilteredIbOrders(accountId, filter)));
    }

    @RequestMapping("/ibaccounts/{accountId}/positions")
    public ResponseEntity<?> getPositions(
            @PathVariable("accountId") String accountId) {

        IbAccount ibAccount = ibLoggerDao.findIbAccount(accountId);
        if (ibAccount == null) {
            return ResponseEntity.notFound().build();
        }

        List<Position> positions = ibController.getPositions(accountId);
        if (positions == null) {
            positions = new ArrayList<>();
        }
        return ResponseEntity.ok(new RestList<>(positions, (long) positions.size()));
    }
}