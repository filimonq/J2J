package j2j.model;

import j2j.annotation.Id;
import j2j.annotation.Persistent;

@Persistent
public class Fullname {
    @Id
    private Long id;
    public String name;
    public String surname;
    public Fullname(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }
    public Fullname() {}
}