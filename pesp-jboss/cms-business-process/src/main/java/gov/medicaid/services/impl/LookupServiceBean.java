/*
 * Copyright (C) 2012 TopCoder Inc., All Rights Reserved.
 */
package gov.medicaid.services.impl;

import gov.medicaid.domain.model.ApplicantType;
import gov.medicaid.entities.AgreementDocument;
import gov.medicaid.entities.BeneficialOwnerType;
import gov.medicaid.entities.EntityStructureType;
import gov.medicaid.entities.LookupEntity;
import gov.medicaid.entities.ProviderType;
import gov.medicaid.entities.ServiceAssuranceExtType;
import gov.medicaid.entities.ServiceAssuranceType;
import gov.medicaid.entities.dto.ViewStatics;
import gov.medicaid.services.LookupService;
import gov.medicaid.services.util.Util;

import java.util.Collections;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Defines the UI related services.
 * 
 * @author TCSASSEMBLER
 * @version 1.0
 */
@Stateless
@Local(LookupService.class)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class LookupServiceBean implements LookupService {

    /**
     * The entity manager.
     */
    @PersistenceContext(unitName = "cms")
    private EntityManager em;

    /**
     * Empty constructor.
     */
    public LookupServiceBean() {
    }

    /**
     * Retrieves the provider types filtered by applicant type.
     * 
     * @param applicantType
     *            individual or organizations
     * @return the filtered provider types
     */
    @SuppressWarnings("unchecked")
    public List<ProviderType> getProviderTypes(ApplicantType applicantType) {
        Query q = null;
        switch (applicantType) {
        case INDIVIDUAL:
            q = em.createQuery("FROM ProviderType p WHERE applicantType = 0");
            break;
        case ORGANIZATION:
            q = em.createQuery("FROM ProviderType p WHERE applicantType = 1");
            break;
        default:
            q = em.createQuery("FROM ProviderType p");
            break;
        }
        return q.getResultList();
    }

    /**
     * Retrieves the lookup with the given description.
     * 
     * @param cls
     *            the class to lookup
     * @param description
     *            the filter
     * @param <T>
     *            the return type
     * @return the matched lookup
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends LookupEntity> T findLookupByDescription(Class<T> cls, String description) {
        if (Util.isBlank(description)) {
            return null;
        }
        Query query = em.createQuery("FROM " + cls.getName() + " WHERE description = :description");
        query.setParameter("description", description);
        List rs = query.getResultList();
        if (rs.isEmpty()) {
            return null;
        }
        if (rs.size() > 1) {
            throw new IllegalStateException("Lookup table contains non unique element.");
        }
        return (T) rs.get(0);
    }

    /**
     * Retrieves the lookup with the given code.
     * 
     * @param cls
     *            the class to lookup
     * @param description
     *            the filter
     * @param <T>
     *            the return type
     * @return the matched lookup
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends LookupEntity> T findLookupByCode(Class<T> cls, String code) {
        if (Util.isBlank(code)) {
            return null;
        }
        Query query = em.createQuery("FROM " + cls.getName() + " WHERE code = :code");
        query.setParameter("code", code);
        List rs = query.getResultList();
        if (rs.isEmpty()) {
            return null;
        }
        if (rs.size() > 1) {
            throw new IllegalStateException("Lookup table contains non unique element.");
        }
        return (T) rs.get(0);
    }

    /**
     * Sets the value of the field <code>em</code>.
     * 
     * @param em
     *            the em to set
     */
    public void setEm(EntityManager em) {
        this.em = em;
    }

    /**
     * Find the related lookups to the given provider.
     * 
     * @param cls
     *            the class to search for
     * @param providerType
     *            the provider type to filter on
     * @param relType
     *            the relationship mapping
     * @param <T>
     *            the return type
     * @return the related types
     */
    @SuppressWarnings("unchecked")
    public <T extends LookupEntity> List<T> findRelatedLookup(Class<T> cls, String providerType, String relType) {
        Query query = em.createQuery("Select l FROM " + cls.getName() + " l, ProviderTypeSetting s WHERE "
                + "s.providerTypeCode = :providerType AND l.code = s.relatedEntityCode AND "
                + "s.relationshipType = :relationshipType  AND s.relatedEntityType = :entityType");
        query.setParameter("providerType", providerType);
        query.setParameter("relationshipType", relType);
        query.setParameter("entityType", cls.getSimpleName());
        return query.getResultList();
    }

    /**
     * Finds all the required agreements for the given provider type.
     * 
     * @param providerType
     *            the provider type
     * @return the required documents
     */
    @SuppressWarnings("unchecked")
    public List<AgreementDocument> findRequiredDocuments(String providerType) {
        Query query = em.createQuery("Select d FROM AgreementDocument d, ProviderTypeSetting s WHERE "
                + "s.providerTypeCode = :providerType AND d.type = s.relatedEntityCode "
                + "AND s.relatedEntityType = 'AgreementDocument'");
        query.setParameter("providerType", providerType);
        return query.getResultList();
    }

    /**
     * Retrieves all the lookups of the given class.
     * 
     * @param cls
     *            the class to search for
     * @param <T>
     *            the return type
     * @return the matched lookups
     */
    @SuppressWarnings("unchecked")
    public <T extends LookupEntity> List<T> findAllLookups(Class<T> cls) {
        return em.createQuery("FROM " + cls.getName()).getResultList();
    }

    /**
     * Retrieves all the owner types allowed for the given structure.
     * 
     * @param entityType
     *            the corporate structure type
     * @return the matched lookups
     */
    @SuppressWarnings("unchecked")
    public List<BeneficialOwnerType> findBeneficialOwnerTypes(String entityType) {
        EntityStructureType entity = findLookupByDescription(EntityStructureType.class, entityType);
        List<BeneficialOwnerType> results = Collections.EMPTY_LIST;
        if (entity != null) {
            results = findRelatedLookup(BeneficialOwnerType.class, entity.getCode(),
                    ViewStatics.REL_BENEFICIAL_OWNER_TYPE);
        }

        if (results.isEmpty()) {
            results = findAllLookups(gov.medicaid.entities.BeneficialOwnerType.class);
        }
        return results;
    }

    /**
     * Retrieves all the service types based on indicator.
     * 
     * @param indicator
     *            in/out patient indicator
     * @return the matched lookups
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ServiceAssuranceType> findAssuredServiceTypes(String indicator) {
        return em.createQuery("from ServiceAssuranceType t where t.patientInd = :ind").setParameter("ind", indicator)
                .getResultList();
    }

    /**
     * Retrieves all the service types based on code.
     * 
     * @param code
     *            the parent service code
     * @return the matched lookups
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<ServiceAssuranceExtType> findAssuredServiceExtTypes(String code) {
        return em.createQuery("from ServiceAssuranceExtType t where t.serviceAssuranceCode = :code")
                .setParameter("code", code).getResultList();
    }
}