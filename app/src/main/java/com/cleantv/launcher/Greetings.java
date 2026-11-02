package com.cleantv.launcher;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The home screen greeting: chosen by time of day, season and weekday/weekend,
 * and rotated once a day so it stays the same for the whole time slot.
 */
final class Greetings {

    /** Time slots, each starting at the given hour. */
    enum Slot {
        NIGHT(0), MORNING(5), AFTERNOON(12), EVENING(18), LATE_EVENING(20);

        final int startHour;

        Slot(int startHour) {
            this.startHour = startHour;
        }
    }

    enum Season { WINTER, SPRING, SUMMER, AUTUMN }

    private enum Day { ANY, WEEKDAY, WEEKEND }

    private static final class Line {
        final Slot slot;
        final Season season; // null = any season
        final Day day;
        final String text;

        Line(Slot slot, Season season, Day day, String text) {
            this.slot = slot;
            this.season = season;
            this.day = day;
            this.text = text;
        }
    }

    private static final List<Line> LINES = new ArrayList<>();

    private static void add(Slot slot, Season season, Day day, String text) {
        LINES.add(new Line(slot, season, day, text));
    }

    static {
        // Mattina, 5-12
        add(Slot.MORNING, null, Day.ANY, "Buongiorno — il caffè è pronto, cosa mettiamo su?");
        add(Slot.MORNING, null, Day.ANY, "Buongiorno, Casa Ciompi. Si comincia con calma.");
        add(Slot.MORNING, null, Day.ANY, "Buongiorno — che la giornata sia gentile con voi.");
        add(Slot.MORNING, Season.WINTER, Day.ANY, "Buongiorno — fuori fa freddo, qui dentro si sta bene.");
        add(Slot.MORNING, Season.SPRING, Day.ANY, "Buongiorno — finestre aperte e un po' di musica?");
        add(Slot.MORNING, Season.SUMMER, Day.ANY, "Buongiorno — approfittiamo del fresco del mattino.");
        add(Slot.MORNING, Season.AUTUMN, Day.ANY, "Buongiorno — una tazza calda e si parte.");
        add(Slot.MORNING, null, Day.WEEKDAY, "Buongiorno — giusto due minuti prima di uscire.");
        add(Slot.MORNING, null, Day.WEEKEND, "Buongiorno — niente sveglia oggi, ce la prendiamo comoda.");

        // Pomeriggio, 12-18
        add(Slot.AFTERNOON, null, Day.ANY, "Buon pomeriggio — mettetevi comodi.");
        add(Slot.AFTERNOON, null, Day.ANY, "Buon pomeriggio — una pausa ce la meritiamo.");
        add(Slot.AFTERNOON, null, Day.ANY, "Buon pomeriggio — c'è sempre tempo per una puntata.");
        add(Slot.AFTERNOON, Season.WINTER, Day.ANY, "Buon pomeriggio — tè caldo e un bel film?");
        add(Slot.AFTERNOON, Season.SPRING, Day.ANY, "Buon pomeriggio — le giornate si allungano, che bello.");
        add(Slot.AFTERNOON, Season.SUMMER, Day.ANY, "Buon pomeriggio — persiane socchiuse e si sta al fresco.");
        add(Slot.AFTERNOON, Season.AUTUMN, Day.ANY, "Buon pomeriggio — foglie fuori, plaid dentro.");
        add(Slot.AFTERNOON, null, Day.WEEKDAY, "Buon pomeriggio — ancora un po' e la giornata è fatta.");
        add(Slot.AFTERNOON, null, Day.WEEKEND, "Buon pomeriggio — pigrizia da weekend autorizzata.");

        // Sera, 18-20
        add(Slot.EVENING, null, Day.ANY, "Buonasera — bentornati a casa.");
        add(Slot.EVENING, null, Day.ANY, "Buonasera — cosa si mangia stasera?");
        add(Slot.EVENING, null, Day.ANY, "Buonasera — com'è andata oggi?");
        add(Slot.EVENING, Season.WINTER, Day.ANY, "Buonasera — fuori è già buio, qui c'è una luce calda.");
        add(Slot.EVENING, Season.SPRING, Day.ANY, "Buonasera — ancora un po' di luce, godiamocela.");
        add(Slot.EVENING, Season.SUMMER, Day.ANY, "Buonasera — aperitivo sul balcone, poi si vede.");
        add(Slot.EVENING, Season.AUTUMN, Day.ANY, "Buonasera — è l'ora delle cose buone e calde.");
        add(Slot.EVENING, null, Day.WEEKDAY, "Buonasera — giornata finita, ora si stacca.");
        add(Slot.EVENING, null, Day.WEEKEND, "Buonasera — è weekend, nessuna fretta stasera.");

        // Serata, 20-24 (weekend = venerdì e sabato: domani non si lavora)
        add(Slot.LATE_EVENING, null, Day.ANY, "Buonasera — cosa guardiamo stasera?");
        add(Slot.LATE_EVENING, null, Day.ANY, "Buonasera — a voi la scelta del film.");
        add(Slot.LATE_EVENING, null, Day.ANY, "Buonasera — luci basse, si comincia.");
        add(Slot.LATE_EVENING, Season.WINTER, Day.ANY, "Buonasera — coperta, divano… cosa guardiamo stasera?");
        add(Slot.LATE_EVENING, Season.SPRING, Day.ANY, "Buonasera — finestra socchiusa e qualcosa di leggero?");
        add(Slot.LATE_EVENING, Season.SUMMER, Day.ANY, "Buonasera — ventilatore acceso, cosa guardiamo?");
        add(Slot.LATE_EVENING, Season.AUTUMN, Day.ANY, "Buonasera — plaid e una bella serie?");
        add(Slot.LATE_EVENING, null, Day.WEEKDAY, "Buonasera — un episodio e poi a nanna?");
        add(Slot.LATE_EVENING, null, Day.WEEKEND, "Buonasera — domani niente sveglia: maratona?");

        // Notte, 0-5 (weekend = sabato e domenica: è la notte dopo venerdì e sabato)
        add(Slot.NIGHT, null, Day.ANY, "Ancora svegli? Facciamo piano.");
        add(Slot.NIGHT, null, Day.ANY, "Notte fonda a Casa Ciompi — volume basso.");
        add(Slot.NIGHT, null, Day.ANY, "Ancora svegli? Il divano non giudica.");
        add(Slot.NIGHT, Season.WINTER, Day.ANY, "Ancora svegli? Fuori gela, restate al calduccio.");
        add(Slot.NIGHT, Season.SPRING, Day.ANY, "Ancora svegli? Ultimo episodio, promesso.");
        add(Slot.NIGHT, Season.SUMMER, Day.ANY, "Ancora svegli? Notte d'estate, finestre aperte.");
        add(Slot.NIGHT, Season.AUTUMN, Day.ANY, "Ancora svegli? Ultima tisana e poi a letto.");
        add(Slot.NIGHT, null, Day.WEEKDAY, "Ancora svegli? Domani si lavora… ultimo episodio.");
        add(Slot.NIGHT, null, Day.WEEKEND, "Ancora svegli? È weekend, si può.");
    }

    private Greetings() {
    }

    static String forNow() {
        return forTime(Calendar.getInstance());
    }

    static String forTime(Calendar now) {
        Slot slot = slotFor(now.get(Calendar.HOUR_OF_DAY));
        Season season = seasonFor(now.get(Calendar.MONTH));
        boolean weekend = isWeekend(slot, now.get(Calendar.DAY_OF_WEEK));

        List<String> candidates = new ArrayList<>();
        for (Line line : LINES) {
            boolean seasonOk = line.season == null || line.season == season;
            boolean dayOk = line.day == Day.ANY || (line.day == Day.WEEKEND) == weekend;
            if (line.slot == slot && seasonOk && dayOk) {
                candidates.add(line.text);
            }
        }
        // Same line for the whole slot, a different one the next day.
        int day = now.get(Calendar.YEAR) * 366 + now.get(Calendar.DAY_OF_YEAR);
        return candidates.get(Math.floorMod(day, candidates.size()));
    }

    static Slot slotFor(int hour) {
        Slot current = Slot.NIGHT;
        for (Slot slot : Slot.values()) {
            if (hour >= slot.startHour) {
                current = slot;
            }
        }
        return current;
    }

    static Season seasonFor(int month) {
        switch (month) {
            case Calendar.DECEMBER:
            case Calendar.JANUARY:
            case Calendar.FEBRUARY:
                return Season.WINTER;
            case Calendar.MARCH:
            case Calendar.APRIL:
            case Calendar.MAY:
                return Season.SPRING;
            case Calendar.JUNE:
            case Calendar.JULY:
            case Calendar.AUGUST:
                return Season.SUMMER;
            default:
                return Season.AUTUMN;
        }
    }

    /** In the evening the weekend starts on Friday; after midnight it lasts until Sunday night. */
    static boolean isWeekend(Slot slot, int dayOfWeek) {
        if (slot == Slot.LATE_EVENING) {
            return dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY;
        }
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
    }
}
