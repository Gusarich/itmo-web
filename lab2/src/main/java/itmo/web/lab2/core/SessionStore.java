package itmo.web.lab2.core;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class SessionStore {
    private final String attributeName;
    private final int capacity;

    public SessionStore(String attributeName, int capacity) {
        this.attributeName = attributeName;
        this.capacity = capacity;
    }

    @SuppressWarnings("unchecked")
    private Deque<Result> getDeque(HttpSession session) {
        Object attr = session.getAttribute(attributeName);
        if (attr instanceof Deque<?>) {
            return (Deque<Result>) attr;
        }
        Deque<Result> deque = new ArrayDeque<>();
        session.setAttribute(attributeName, deque);
        return deque;
    }

    public List<Result> snapshot(HttpSession session) {
        Deque<Result> deque = getDeque(session);
        synchronized (deque) {
            return List.copyOf(deque);
        }
    }

    public List<Result> addAll(HttpSession session, List<Result> results) {
        if (results.isEmpty()) {
            return snapshot(session);
        }
        Deque<Result> deque = getDeque(session);
        synchronized (deque) {
            for (Result r : results) {
                deque.addFirst(r);
                while (deque.size() > capacity) {
                    deque.removeLast();
                }
            }
            return List.copyOf(deque);
        }
    }
}
