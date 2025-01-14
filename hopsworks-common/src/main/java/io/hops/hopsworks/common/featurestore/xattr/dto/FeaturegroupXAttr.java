/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.featurestore.xattr.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class FeaturegroupXAttr {
  /**
   * common fields
   */
  public static abstract class Base {
    @XmlElement(nillable = false, name = FeaturestoreXAttrsConstants.FEATURESTORE_ID)
    private Integer featurestoreId;
  
    public Base() {}
  
    public Base(Integer featurestoreId) {
      this.featurestoreId = featurestoreId;
    }
  
    public Integer getFeaturestoreId() {
      return featurestoreId;
    }
  
    public void setFeaturestoreId(Integer featurestoreId) {
      this.featurestoreId = featurestoreId;
    }
  
    @Override
    public String toString() {
      return "Base{" +
        "featurestoreId=" + featurestoreId +
        '}';
    }
  }

  /**
   * document attached as an xattr to a featuregroup directory
   */
  @XmlRootElement
  public static class FullDTO extends Base {
    @XmlElement(nillable = true, name = FeaturestoreXAttrsConstants.DESCRIPTION)
    private String description;
    @XmlElement(nillable = true, name = FeaturestoreXAttrsConstants.CREATE_DATE)
    private Long createDate;
    @XmlElement(nillable = true, name = FeaturestoreXAttrsConstants.CREATOR)
    private String creator;
    @XmlElement(nillable = true, name = FeaturestoreXAttrsConstants.FG_TYPE)
    private FGType fgType;
    @XmlElement(nillable = false, name = FeaturestoreXAttrsConstants.FG_FEATURES)
    private List<SimpleFeatureDTO> features = new LinkedList<>();
  
    public FullDTO() {
      super();
    }
  
    public FullDTO(Integer featurestoreId, String description, Date createDate, String creator) {
      this(featurestoreId, description, createDate, creator, new LinkedList<>());
    }
  
    public FullDTO(Integer featurestoreId, String description,
      Date createDate, String creator, List<SimpleFeatureDTO> features) {
      super(featurestoreId);
      this.features = features;
      this.description = description;
      this.createDate = createDate.getTime();
      this.creator = creator;
    }
  
    public String getDescription() {
      return description;
    }
  
    public void setDescription(String description) {
      this.description = description;
    }
  
    public Long getCreateDate() {
      return createDate;
    }
  
    public void setCreateDate(Long createDate) {
      this.createDate = createDate;
    }
  
    public String getCreator() {
      return creator;
    }
  
    public void setCreator(String creator) {
      this.creator = creator;
    }

    public FGType getFgType() {
      return fgType;
    }

    public void setFgType(FGType fgType) {
      this.fgType = fgType;
    }
    
    public List<SimpleFeatureDTO> getFeatures() {
      return features;
    }

    public void setFeatures(List<SimpleFeatureDTO> features) {
      this.features = features;
    }

    public void addFeature(SimpleFeatureDTO feature) {
      features.add(feature);
    }

    public void addFeatures(List<SimpleFeatureDTO> features) {
      this.features.addAll(features);
    }

    @Override
    public String toString() {
      return super.toString() + "Extended{" +
        "description='" + description + '\'' +
        ", createDate=" + createDate +
        ", creator='" + creator + '\'' +
        ", fgType='" + fgType + '\'' +
        ", features=" + features +
        '}';
    }
  }
  
  /**
   * simplified version of FullDTO that is part of the
   * @link io.hops.hopsworks.common.featurestore.xattr.dto.TrainingDatasetXAttrDTO
   */
  @XmlRootElement
  public static class SimplifiedDTO extends Base {
    @XmlElement(nillable = false, name = FeaturestoreXAttrsConstants.NAME)
    private String name;
    @XmlElement(nillable = false, name = FeaturestoreXAttrsConstants.VERSION)
    private Integer version;
    @XmlElement(nillable = false, name = FeaturestoreXAttrsConstants.FG_FEATURES)
    private List<String> features = new LinkedList<>();
  
    public SimplifiedDTO() {
      super();
    }
    
    public SimplifiedDTO(Integer featurestoreId, String name, Integer version) {
      super(featurestoreId);
      this.name = name;
      this.version = version;
    }
    public String getName() {
      return name;
    }
  
    public void setName(String name) {
      this.name = name;
    }
  
    public Integer getVersion() {
      return version;
    }
  
    public void setVersion(Integer version) {
      this.version = version;
    }

    public List<String> getFeatures() {
      return features;
    }
  
    public void setFeatures(List<String> features) {
      this.features = features;
    }
  
    public void addFeature(String feature) {
      features.add(feature);
    }
  
    public void addFeatures(List<String> features) {
      this.features.addAll(features);
    }
  }

  @XmlRootElement
  public static class SimpleFeatureDTO {
    private String name;
    private String description;
    
    public SimpleFeatureDTO() {}
    
    public SimpleFeatureDTO(String name, String description) {
      this.name = name;
      this.description = description;
    }
  
    public String getName() {
      return name;
    }
  
    public void setName(String name) {
      this.name = name;
    }
  
    public String getDescription() {
      return description;
    }
  
    public void setDescription(String description) {
      this.description = description;
    }
  
    @Override
    public String toString() {
      return "SimpleFeatureDTO{" +
        "name='" + name + '\'' +
        ", description='" + description + '\'' +
        '}';
    }
  }

  public enum FGType {
    ON_DEMAND,
    CACHED,
    STREAM
  }
}
