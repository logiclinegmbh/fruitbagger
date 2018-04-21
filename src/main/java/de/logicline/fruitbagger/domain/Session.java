package de.logicline.fruitbagger.domain;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

import java.time.Instant;
import java.util.Date;

@Entity("sessions")
public class Session {
  @Id
  private ObjectId id;
  private Integer number;
  @Reference
  private FruitUser user;
  private Date startDate;
  private Date finishDate;
  private Integer fruitIndex;

  private Integer lookAhead;

  public static Session create(FruitUser user, Integer number){
    Session newSession = new Session();
    newSession.id = new ObjectId();
    newSession.number = number;
    newSession.user = user;
    newSession.startDate = Date.from(Instant.now());
    newSession.fruitIndex = 0;
    newSession.lookAhead = 4;
    return newSession;
  }

  public ObjectId getId() {
    return id;
  }

  public FruitUser getUser() {
    return user;
  }

  public Date getStartDate() {
    return startDate;
  }

  public Date getFinishDate() {
    return finishDate;
  }

  public Integer getFruitIndex() {
    return fruitIndex;
  }

  public Integer getLookAhead() {
    return lookAhead;
  }

  public void incrementIndex(){
    this.fruitIndex++;
  }

  public Integer getNumber() {
    return number;
  }

  public void closeNow(){
    this.finishDate = Date.from(Instant.now());
  }
}
