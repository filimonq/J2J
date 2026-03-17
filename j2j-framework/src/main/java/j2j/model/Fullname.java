package j2j.model;

import j2j.annotation.Id;
import j2j.annotation.Persistent;

@Persistent
public class Fullname {
    @Id
    private Long id;
    private String name;
    private String surname;
    public Fullname(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }
}