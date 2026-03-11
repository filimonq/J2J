package j2j;

import j2j.id.CounterIdStrategy;
import j2j.model.User;

public class Main {

    public static void main(String[] args) {
        PersistenceManager manager =
                new PersistenceManager("storage.json", new CounterIdStrategy());

        User u1 = new User("Anna", 19, true);
        User u2 = new User("Bob", 25, false);

        manager.save(u1);
        manager.save(u2);
        manager.flush();
    }
}
