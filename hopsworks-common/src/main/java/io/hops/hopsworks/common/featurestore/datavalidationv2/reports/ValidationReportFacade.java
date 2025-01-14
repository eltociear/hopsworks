/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */


package io.hops.hopsworks.common.featurestore.datavalidationv2.reports;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.ConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.sql.Timestamp;

/**
 * A facade for the expectation_suite table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
public class ValidationReportFacade extends AbstractFacade<ValidationReport> {
  private static final Logger LOGGER = Logger.getLogger(ValidationReportFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;


  public ValidationReportFacade() {
    super(ValidationReport.class);
  }

  /**
   * Gets the entity manager of the facade
   *
   * @return entity manager
   */
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  /**
   * Transaction to create a new featuregroup in the database
   *
   * @param validationReport
   *   the ValidationReport to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(ValidationReport validationReport) {
    try {
      em.persist(validationReport);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new ValidationReport", cve);
    }
  }

  public Optional<ValidationReport> findById(Integer id) {
    try {
      return Optional.of(em.createNamedQuery("ValidationReport.findById", 
        ValidationReport.class).setParameter("id", id).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public CollectionInfo<ValidationReport> findByFeaturegroup(Integer offset, Integer limit,
                                                        Set<? extends SortBy> sorts,
                                                        Set<? extends FilterBy> filters,
                                                        Featuregroup featuregroup) {
    String queryStr = buildQuery("SELECT vr from ValidationReport vr ", filters,
        sorts, "vr.featuregroup = :featuregroup");
    String queryCountStr = buildQuery("SELECT COUNT(vr.id) from ValidationReport vr ", filters,
        sorts, "vr.featuregroup = :featuregroup");
    Query query = em.createQuery(queryStr, ValidationReport.class)
        .setParameter("featuregroup", featuregroup);
    Query queryCount = em.createQuery(queryCountStr, ValidationReport.class)
        .setParameter("featuregroup", featuregroup);
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo<ValidationReport>((Long) queryCount.getSingleResult(), query.getResultList());
  }

  public Optional<ValidationReport> findFeaturegroupLatestValidationReport(Featuregroup featuregroup) {
    List<ValidationReport> validationReports = em.createNamedQuery("ValidationReport" +
        ".findByFeaturegroupOrderedByDescDate",
      ValidationReport.class).setParameter("featuregroup", featuregroup).getResultList();
    Optional<ValidationReport> latestReport;

    if (validationReports.isEmpty()) {
      latestReport = Optional.empty();
    } else {
      latestReport = Optional.of(validationReports.get(0));
    }

    return latestReport;
  }

  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      q.setParameter(aFilter.getField(), new Timestamp(Long.parseLong(aFilter.getParam())));
    }
  }

  /* The validation time could be parsed from the json of the report 
  but for now the created field populated on report upload serves as 
  a proxy */
  public enum Sorts {
    VALIDATION_TIME("VALIDATION_TIME", "vr.validationTime ", "DESC");

    private final String value;
    private final String sql;
    private final String defaultParam;

    Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }

    public String getValue() {
      return value;
    }

    public String getDefaultParam() {
      return defaultParam;
    }

    public String getSql() {
      return sql;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  /* The validation time could be parsed from the json of the report 
  but for now the created field populated on report upload serves as 
  a proxy */
  public enum Filters {
    VALIDATION_TIME_GT("VALIDATION_TIME_GT", "vr.validationTime > :validationTime ","validationTime",""),
    VALIDATION_TIME_LT("VALIDATION_TIME_LT", "vr.validationTime < :validationTime ","validationTime",""),
    VALIDATION_TIME_EQ("VALIDATION_TIME_EQ", "vr.validationTime = :validationTime ","validationTime","");

    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;

    private Filters(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }

    public String getValue() {
      return value;
    }

    public String getDefaultParam() {
      return defaultParam;
    }

    public String getSql() {
      return sql;
    }

    public String getField() {
      return field;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
