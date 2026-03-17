package j2j.model;

import j2j.annotation.Id;
import j2j.annotation.Persistent;
import j2j.annotation.Reference;

@Persistent
public class User {

    @Id
    private Long id;
    @Reference
    private Fullname name;
    private int age;
    private boolean active;

    public User(Fullname name, int age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }
    public User(){}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Fullname getName() { return name; }
    public void setName(Fullname name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}