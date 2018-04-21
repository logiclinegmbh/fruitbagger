package de.logicline.fruitbagger.domain;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.io.Serializable;
import java.util.UUID;

@Entity("users")
public class FruitUser implements Serializable {
  @Id
  private ObjectId id;
  private String email;
  private String apiToken;

  public FruitUser(){}

  public FruitUser(String email) {
    this.email = email;
    resetApiKey();
  }

  public String getApiToken() {
    return apiToken;
  }

  public String getEmail() {
    return email;
  }

  public void resetApiKey() {
    this.apiToken = UUID.randomUUID().toString();
  }
}
