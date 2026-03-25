package j2j.model;

import j2j.annotation.Id;
import j2j.annotation.Persistent;
import j2j.annotation.Reference;

@Persistent
public class UserTest {

    @Id
    private Long id;

    @Reference
    private Fullname name;

    private int age;
    private boolean active;

    public UserTest() {}

    public UserTest(Fullname name, int age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }

    public Fullname getName() { return name; }
    public int getAge() { return age; }
}