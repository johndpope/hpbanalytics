package com.highpowerbear.hpbanalytics.ordtrack;

import com.highpowerbear.hpbanalytics.common.CoreSettings;
import com.highpowerbear.hpbanalytics.dao.OrdTrackDao;
import com.highpowerbear.hpbanalytics.entity.IbOrder;
import com.highpowerbear.hpbanalytics.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by robertk on 4/6/2015.
 */
@Component
public class HeartbeatControl {

    @Autowired private OrdTrackDao ordTrackDao;

    private Map<String, Map<IbOrder, Integer>> openOrderHeartbeatMap = new ConcurrentHashMap<>(); // accountId --> (ibOrder --> number of failed heartbeats left before UNKNOWN)

    @PostConstruct
    public void init() {
        ordTrackDao.getIbAccounts().forEach(ibAccount -> openOrderHeartbeatMap.put(ibAccount.getAccountId(), new ConcurrentHashMap<>()));
        ordTrackDao.getIbAccounts().stream()
                .flatMap(ibAccount -> ordTrackDao.getOpenIbOrders(ibAccount.getAccountId()).stream())
                .forEach(this::initHeartbeat);
    }

    public Map<String, Map<IbOrder, Integer>> getOpenOrderHeartbeatMap() {
        return openOrderHeartbeatMap;
    }

    public void updateHeartbeats(String accountId) {
        Map<IbOrder, Integer> hm = openOrderHeartbeatMap.get(accountId);
        Set<IbOrder> keyset = new HashSet<>(hm.keySet());

        for (IbOrder ibOrder : keyset) {
            Integer failedHeartbeatsLeft = hm.get(ibOrder);

            if (failedHeartbeatsLeft <= 0) {
                if (!OrderStatus.UNKNOWN.equals(ibOrder.getStatus())) {
                    ibOrder.addEvent(OrderStatus.UNKNOWN, null);
                    ordTrackDao.updateIbOrder(ibOrder);
                }
                hm.remove(ibOrder);
            } else {
                hm.put(ibOrder, failedHeartbeatsLeft - 1);
            }
        }
    }

    public void initHeartbeat(IbOrder ibOrder) {
        openOrderHeartbeatMap.get(ibOrder.getIbAccount().getAccountId()).put(ibOrder, CoreSettings.MAX_ORDER_HEARTBEAT_FAILS);
    }

    public void removeHeartbeat(IbOrder ibOrder) {
        openOrderHeartbeatMap.get(ibOrder.getIbAccount().getAccountId()).remove(ibOrder);
    }
}