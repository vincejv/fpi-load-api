package com.abavilla.vbl.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class MongoItem extends AbsItem {
  public ObjectId Id;
}
