package com.mmiholdings.member.money.service;

import com.mmiholdings.member.money.api.domain.AccountCreation;
import com.mmiholdings.member.money.api.domain.AccountType;
import com.mmiholdings.service.money.util.LoggingInterceptor;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Created by pieter on 2017/03/13.
 */
@Stateless
@Interceptors(LoggingInterceptor.class)
public class MemberApplicationDaoImpl {

    @PersistenceContext(name = "money")
    private EntityManager em;


    public AccountCreation getLastApplicationByType(AccountType type) {
        AccountCreation application = null;
        try {
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

            CriteriaQuery<AccountCreation> cQuery = criteriaBuilder.createQuery(AccountCreation.class);

            Root<AccountCreation> criteria = cQuery.from(AccountCreation.class);

            cQuery.select(criteria).where(criteriaBuilder.equal(criteria.get(AccountCreation.ACCOUNT_TYPE), type))
                    .orderBy(criteriaBuilder.desc(criteria.get(AccountCreation.ACCOUNT_CREATION_ID)));

            List<AccountCreation> applications = em.createQuery(cQuery).getResultList();

            if (applications != null && !applications.isEmpty()) {
                return applications.get(0);
            }
        } catch (NoResultException nre) {
            return application;
        }

        return application;
    }

    public void saveApplication(AccountCreation memberApplication) {
        em.merge(memberApplication);
    }
}
