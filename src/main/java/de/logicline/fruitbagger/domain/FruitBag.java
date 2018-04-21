package de.logicline.fruitbagger.domain;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity("fruitbags")
public class FruitBag {
  @Id
  private ObjectId id;
  private Integer number;
  @Reference
  private Session session;
  private Set<Integer> fruits;
  private Date startDate;
  private Date finishDate;

  public Session getSession() {
    return session;
  }

  public Date getStartDate() {
    return startDate;
  }

  public Date getFinishDate() {
    return finishDate;
  }

  public Set<Integer> getFruits() {
    if(this.fruits == null)
      this.fruits = new HashSet<>();
    return fruits;
  }

  public Integer getNumber() {
    return number;
  }

  public static FruitBag create(Session session, int number) {
    FruitBag newBag = new FruitBag();
    newBag.id = new ObjectId();
    newBag.number = number;
    newBag.session = session;
    newBag.startDate = Date.from(Instant.now());
    newBag.fruits = new HashSet<>();
    return newBag;
  }

  public void closeNow() {
    this.finishDate = Date.from(Instant.now());
  }
}
