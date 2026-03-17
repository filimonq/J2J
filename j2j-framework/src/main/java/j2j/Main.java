package j2j;

import j2j.id.CounterIdStrategy;
import j2j.model.Fullname;
import j2j.model.User;

public class Main {

    public static void main(String[] args) {
        PersistenceManager manager =
                new PersistenceManager("storage.json", new CounterIdStrategy());

        Fullname f1 = new Fullname("Anna", "Ivanova");
        Fullname f2 = new Fullname("Ben", "Johnson");

        manager.save(f1);
        manager.save(f2);

        User u1 = new User(f1, 19, true);
        User u2 = new User(f2, 25, false);

        manager.save(u1);
        manager.save(u2);
        manager.flush();
    }
}
