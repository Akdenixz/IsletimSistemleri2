import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Flight {
    String flightId;
    boolean[] seats;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public Flight(String flightId, int numSeats) {
        this.flightId = flightId;
        this.seats = new boolean[numSeats];
    }

    public boolean querySeat(int seatNumber) {
        lock.readLock().lock();
        try {
            return seats[seatNumber];
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean makeReservation(int seatNumber) {
        lock.writeLock().lock();
        try {
            if (!seats[seatNumber]) {
                seats[seatNumber] = true;
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean cancelReservation(int seatNumber) {
        lock.writeLock().lock();
        try {
            if (seats[seatNumber]) {
                seats[seatNumber] = false;
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
}

class Database {
    List<Flight> flights = new ArrayList<>();

    public void addFlight(Flight flight) {
        flights.add(flight);
    }

    public Flight getFlight(String flightId) {
        for (Flight flight : flights) {
            if (flight.flightId.equals(flightId)) {
                return flight;
            }
        }
        return null;
    }
}

class Reader extends Thread {
    Database db;
    String flightId;

    public Reader(Database db, String flightId, String name) {
        super(name);
        this.db = db;
        this.flightId = flightId;
    }

    public void run() {
        Flight flight = db.getFlight(flightId);
        if (flight != null) {
            StringBuilder seatsStatus = new StringBuilder();
            for (int i = 0; i < flight.seats.length; i++) {
                seatsStatus.append("Seat No ").append(i).append(" : ").append(flight.querySeat(i) ? "1" : "0").append(" ");
            }
            System.out.println(Thread.currentThread().getName() + " looks for available seats. State of the seats are : ");
            System.out.println(seatsStatus.toString());
        }
    }
}

class Writer extends Thread {
    Database db;
    String flightId;
    int seatNumber;

    public Writer(Database db, String flightId, int seatNumber, String name) {
        super(name);
        this.db = db;
        this.flightId = flightId;
        this.seatNumber = seatNumber;
    }

    public void run() {
        Flight flight = db.getFlight(flightId);
        if (flight != null) {
            if (flight.makeReservation(seatNumber)) {
                System.out.println(Thread.currentThread().getName() + " tries to book the seat " + seatNumber);
                System.out.println(Thread.currentThread().getName() + " booked seat number " + seatNumber + " successfully.");
            } else {
                System.out.println(Thread.currentThread().getName() + " tries to book the seat " + seatNumber);
                System.out.println(Thread.currentThread().getName() + " could not book seat number " + seatNumber + " since it has been already booked.");
            }
        }
    }
}

public class senkron {
    public static void main(String[] args) {
        Database db = new Database();
        Flight flight1 = new Flight("Flight1", 5); // 5 koltuk
        db.addFlight(flight1);

        // Önce reader thread'lerini başlatıyoruz
        new Reader(db, "Flight1", "reader-1").start();
        new Reader(db, "Flight1", "reader-2").start();
        new Reader(db, "Flight1", "reader-3").start();

        // Biraz bekleyip sonra writer thread'lerini başlatıyoruz
        try {
            Thread.sleep(100); // reader'ların başlaması için kısa bir bekleme süresi
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Aynı koltuğu rezerve etmeye çalışan writer thread'leri başlatıyoruz
        new Writer(db, "Flight1", 1, "writer-1").start();
        new Writer(db, "Flight1", 1, "writer-2").start();
        new Writer(db, "Flight1", 1, "writer-3").start();
    }
}
