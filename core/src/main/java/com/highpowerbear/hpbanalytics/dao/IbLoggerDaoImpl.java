package com.highpowerbear.hpbanalytics.dao;

import com.highpowerbear.hpbanalytics.common.CoreSettings;
import com.highpowerbear.hpbanalytics.dao.filter.IbOrderFilter;
import com.highpowerbear.hpbanalytics.entity.IbAccount;
import com.highpowerbear.hpbanalytics.entity.IbOrder;
import com.highpowerbear.hpbanalytics.enums.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by robertk on 11/18/2017.
 */
@Repository
@Transactional(propagation = Propagation.SUPPORTS)
public class IbLoggerDaoImpl implements IbLoggerDao {

    private static final Logger log = LoggerFactory.getLogger(IbLoggerDaoImpl.class);

    @PersistenceContext
    private EntityManager em;

    @Autowired private QueryBuilder queryBuilder;

    private final String B = "BEGIN " + this.getClass().getSimpleName() + ".";
    private final String E = "END " + this.getClass().getSimpleName() + ".";

    @Override
    public IbAccount findIbAccount(String accountId) {
        return em.find(IbAccount.class, accountId);
    }

    @Override
    public List<IbAccount> getIbAccounts() {
        TypedQuery<IbAccount> q = em.createQuery("SELECT ia FROM IbAccount ia ORDER BY ia.port", IbAccount.class);
        return q.getResultList();
    }

    @Transactional
    @Override
    public IbAccount updateIbAccount(IbAccount ibAccount) {
        return em.merge(ibAccount);
    }

    @Override
    public List<IbOrder> getFilteredIbOrders(IbAccount ibAccount, IbOrderFilter filter, Integer start, Integer limit) {
        TypedQuery<IbOrder> q = queryBuilder.buildFilteredIbOrdersQuery(em, ibAccount, filter);

        q.setFirstResult(start != null ? start : 0);
        q.setMaxResults(limit != null ? limit : CoreSettings.JPA_MAX_RESULTS);

        return q.getResultList();
    }

    @Override
    public Long getNumFilteredIbOrders(IbAccount ibAccount, IbOrderFilter filter) {
        TypedQuery<Long> q = queryBuilder.buildFilteredIbOrdersCountQuery(em, ibAccount, filter);
        return q.getSingleResult();
    }

    @Override
    public List<IbOrder> getOpenIbOrders(IbAccount ibAccount) {
        TypedQuery<IbOrder> q = em.createQuery("SELECT io FROM IbOrder io WHERE io.ibAccount = :ibAccount AND io.status IN :statuses", IbOrder.class);

        q.setParameter("ibAccount", ibAccount);
        Set<OrderStatus> statuses = new HashSet<>();
        statuses.add(OrderStatus.SUBMITTED);
        statuses.add(OrderStatus.UPDATED);
        q.setParameter("statuses", statuses);

        return q.getResultList();
    }

    @Transactional
    @Override
    public void newIbOrder(IbOrder ibOrder) {
        log.info(B + "newIbOrder, dbId=" + ibOrder.getId() + ", account=" + ibOrder.getIbAccount().getAccountId() + ", permId=" + ibOrder.getPermId());
        em.persist(ibOrder);
        log.info(E + "newIbOrder, dbId=" + ibOrder.getId() + ", account=" + ibOrder.getIbAccount().getAccountId() + ", permId=" + ibOrder.getPermId());
    }

    @Transactional
    @Override
    public void updateIbOrder(IbOrder ibOrder) {
        log.info(B + "updateIbOrder, account=" + ibOrder.getIbAccount().getAccountId() + ", permId=" + ibOrder.getPermId());
        em.merge(ibOrder);
        log.info(E + "updateIbOrder, account=" + ibOrder.getIbAccount().getAccountId() + ", permId=" + ibOrder.getPermId());
    }

    @Override
    public IbOrder getIbOrderByPermId(IbAccount ibAccount, Long permId) {
        TypedQuery<IbOrder> q = em.createQuery("SELECT io FROM IbOrder io WHERE io.ibAccount = :ibAccount AND io.permId = :permId", IbOrder.class);

        q.setParameter("ibAccount", ibAccount);
        q.setParameter("permId", permId);
        List<IbOrder> ibOrders = q.getResultList();

        return !ibOrders.isEmpty() ? ibOrders.get(0) : null;
    }
}