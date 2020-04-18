import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class MainClass {
    public static final int CARS_COUNT = 4;


    public static void main(String[] args) {
        // Создаем объект CyclicBarrier для потоков = колличеству машин и главного потока.
        CyclicBarrier cb = new CyclicBarrier(CARS_COUNT + 1);
        // Создаем счетчик выполненных потоков CountDownLatch по количеству машин
        CountDownLatch cdl = new CountDownLatch(CARS_COUNT);

        System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Подготовка!!!");
        Race race = new Race(new Road(60), new Tunnel(), new Road(40));
        Car[] cars = new Car[CARS_COUNT];
        for (int i = 0; i < cars.length; i++) {
            cars[i] = new Car(race, 20 + (int) (Math.random() * 10), cb, cdl);
        }
        for (int i = 0; i < cars.length; i++) {
            new Thread(cars[i]).start();
        }
        // Как только все потоки будут готовы продолжить после await - продолжает главный поток и ломает барьер
        try {
            cb.await();
            System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка началась!!!");
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }


        try {
            // Пока счетчик CountDown выполненных потоков car не будет равен нулю - стоим в этой точке
            cdl.await();
            System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка закончилась!!!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
class Car implements Runnable {
    private static int CARS_COUNT;
    // Для определения выигрыша добавляем статическую переменную, одну на все объекты класса.
    private static volatile boolean winnerFound = false;
    // Для синхронного старта потоков объявим CyclicBarrier
    CyclicBarrier cb;
    // Для контроля завершения потоков добавим CountDownLatch
    CountDownLatch cdl;

    static {
        CARS_COUNT = 0;
    }
    private Race race;
    private int speed;
    private String name;


    public String getName() {
        return name;
    }
    public int getSpeed() {
        return speed;
    }
    public Car(Race race, int speed, CyclicBarrier cb, CountDownLatch cdl) {
        this.race = race;
        this.speed = speed;
        CARS_COUNT++;
        this.name = "Участник #" + CARS_COUNT;
        this.cb = cb;
        this.cdl = cdl;
    }
    @Override
    public void run() {
        try {
            System.out.println(this.name + " готовится");
            Thread.sleep(500 + (int)(Math.random() * 800));
            System.out.println(this.name + " готов");
            // Пока все потоки не будут готовы и не будет сломан барьер - стоим в этой точке
            cb.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < race.getStages().size(); i++) {
            race.getStages().get(i).go(this);
        }
        // Посмторим не выиграл ли этот поток
        getWinner(this);
        // Поток завершил работу - уменьшим счетчик CountDownLatch
        cdl.countDown();
    }

    // синхронизованный статический метод определения победителя (в ед. времени только один поток зайдет)
    private static synchronized void getWinner(Car car) {
        if (!winnerFound) {
            System.out.println(car.name + " WINS!");
            winnerFound = true;
        }
    }
}
abstract class Stage {
    protected int length;
    protected String description;
    public String getDescription() {
        return description;
    }
    public abstract void go(Car c);
}
class Road extends Stage {
    public Road(int length) {
        this.length = length;
        this.description = "Дорога " + length + " метров";
    }
    @Override
    public void go(Car c) {
        try {
            System.out.println(c.getName() + " начал этап: " + description);
            Thread.sleep(length / c.getSpeed() * 1000);
            System.out.println(c.getName() + " закончил этап: " + description);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
class Tunnel extends Stage {

    //Добавим семафор с доступом 2х потоков к общему ресурсу.
    Semaphore smp = new Semaphore(2);

    public Tunnel() {
        this.length = 80;
        this.description = "Тоннель " + length + " метров";
    }
    @Override
    public void go(Car c) {
        try {
            try {
                System.out.println(c.getName() + " готовится к этапу(ждет): " + description);
                // Запрашиваем у семафора разрешение на выполнение
                smp.acquire();
                System.out.println(c.getName() + " начал этап: " + description);
                Thread.sleep(length / c.getSpeed() * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println(c.getName() + " закончил этап: " + description);
                // Освобождаем семафор для другого потока
                smp.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
class Race {
    private ArrayList<Stage> stages;
    public ArrayList<Stage> getStages() { return stages; }
    public Race(Stage... stages) {
        this.stages = new ArrayList<>(Arrays.asList(stages));
    }
}
