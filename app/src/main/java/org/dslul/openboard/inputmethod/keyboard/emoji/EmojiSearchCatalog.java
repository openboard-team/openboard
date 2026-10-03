package org.dslul.openboard.inputmethod.keyboard.emoji;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Small offline emoji keyword index used by the in-keyboard search panel. */
final class EmojiSearchCatalog {
    static final class Result {
        final String mEmoji;
        final String mKeywords;

        Result(final String emoji, final String keywords) {
            mEmoji = emoji;
            mKeywords = normalize(keywords);
        }
    }

    private static final Result[] ENTRIES = {
            entry("😀", "grin happy smile feliz sourire lachen"),
            entry("😂", "joy tears laugh lol risa rire lachen"),
            entry("🥹", "tears touched emotional cry llorar pleurer"),
            entry("😍", "love eyes heart amor amour liebe"),
            entry("😘", "kiss love beso bisou kuss"),
            entry("😎", "cool sunglasses gafas lunettes sonnenbrille"),
            entry("🤔", "think thinking pensar penser denken"),
            entry("😭", "cry sob sad llorar triste pleurer traurig"),
            entry("😡", "angry mad rage enfadado colere wut"),
            entry("🥳", "party birthday fiesta fete geburtstag"),
            entry("🤯", "mind blown shocked surprise sorprendido"),
            entry("😴", "sleep tired dormir sommeil schlafen"),
            entry("🤢", "sick nausea enfermo malade krank"),
            entry("🤡", "clown payaso clown"),
            entry("👻", "ghost halloween fantasma fantome geist"),
            entry("💀", "skull dead death calavera mort tod"),
            entry("❤️", "heart red love corazon coeur herz"),
            entry("🧡", "heart orange love"), entry("💛", "heart yellow love"),
            entry("💚", "heart green love"), entry("💙", "heart blue love"),
            entry("💜", "heart purple love"), entry("🖤", "heart black love"),
            entry("💔", "broken heart heartbreak corazon coeur herz"),
            entry("🔥", "fire flame hot fuego feu feuer"),
            entry("✨", "sparkles magic shine brillo magie"),
            entry("⭐", "star favorite estrella etoile stern"),
            entry("🎉", "party celebration confetti fiesta fete"),
            entry("👍", "thumb up yes good like bien oui gut ja"),
            entry("👎", "thumb down no bad dislike mal non schlecht nein"),
            entry("👏", "clap applause bravo aplauso applaudieren"),
            entry("🙏", "pray thanks please gracias merci danke"),
            entry("💪", "strong muscle fuerza fort stark"),
            entry("🤝", "handshake agreement deal acuerdo accord"),
            entry("👋", "wave hello goodbye hola bonjour hallo tschuss"),
            entry("👌", "ok perfect perfecto parfait perfekt"),
            entry("🤞", "luck fingers cruzados chance gluck"),
            entry("🫶", "heart hands love amor amour liebe"),
            entry("👀", "eyes look watch mirar voir sehen"),
            entry("🧠", "brain smart idea cerebro cerveau gehirn"),
            entry("🐶", "dog puppy perro chien hund"),
            entry("🐱", "cat kitten gato chat katze"),
            entry("🐭", "mouse raton souris maus"),
            entry("🐰", "rabbit bunny conejo lapin hase"),
            entry("🦊", "fox zorro renard fuchs"),
            entry("🐻", "bear oso ours bar"),
            entry("🐼", "panda bear oso ours"),
            entry("🦁", "lion leon lion lowe"),
            entry("🐸", "frog rana grenouille frosch"),
            entry("🐵", "monkey mono singe affe"),
            entry("🐔", "chicken bird pollo poulet huhn"),
            entry("🦄", "unicorn magic unicornio licorne einhorn"),
            entry("🐝", "bee honey abeja abeille biene"),
            entry("🦋", "butterfly mariposa papillon schmetterling"),
            entry("🌸", "flower blossom pink flor fleur blume"),
            entry("🌹", "rose flower love rosa fleur"),
            entry("🌻", "sunflower flower girasol tournesol sonnenblume"),
            entry("🌞", "sun sunny sol soleil sonne"),
            entry("🌙", "moon night luna lune mond nacht"),
            entry("🌈", "rainbow pride arcoiris arc ciel regenbogen"),
            entry("🍎", "apple fruit manzana pomme apfel"),
            entry("🍕", "pizza food comida nourriture essen"),
            entry("🍔", "burger food hamburger comida essen"),
            entry("🍟", "fries food patatas frites pommes"),
            entry("🍣", "sushi food japanese comida"),
            entry("🍩", "donut sweet dessert dulce sucre"),
            entry("🎂", "cake birthday cumpleanos anniversaire geburtstag"),
            entry("☕", "coffee drink cafe kaffee morning"),
            entry("🍺", "beer drink cerveza biere bier"),
            entry("🚗", "car vehicle coche voiture auto"),
            entry("✈️", "airplane travel flight avion flugzeug"),
            entry("🚀", "rocket space cohete fusee rakete"),
            entry("🏠", "house home casa maison haus"),
            entry("⚽", "football soccer ball futbol fussball"),
            entry("🏀", "basketball ball sport baloncesto"),
            entry("🎮", "game controller gaming juego jeu spiel"),
            entry("🎵", "music note song musica musique musik"),
            entry("📱", "phone mobile telefono telephone handy"),
            entry("💻", "computer laptop ordenador ordinateur"),
            entry("💡", "idea light bulb idea lumiere licht"),
            entry("✅", "check done yes correct listo oui richtig"),
            entry("❌", "cross no wrong error nein faux"),
            entry("⚠️", "warning danger caution peligro gefahr"),
            entry("🚩", "flag red bandera drapeau fahne")
    };

    private EmojiSearchCatalog() {
    }

    static List<Result> search(final String query, final int limit) {
        final String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return Collections.emptyList();
        }
        final String[] terms = normalizedQuery.split("\\s+");
        final ArrayList<Result> results = new ArrayList<>();
        for (Result entry : ENTRIES) {
            boolean matches = true;
            for (String term : terms) {
                if (!entry.mKeywords.contains(term)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                results.add(entry);
                if (results.size() == limit) {
                    break;
                }
            }
        }
        return results;
    }

    private static Result entry(final String emoji, final String keywords) {
        return new Result(emoji, keywords);
    }

    private static String normalize(final String value) {
        final String decomposed = Normalizer.normalize(value == null ? "" : value,
                Normalizer.Form.NFD).toLowerCase(Locale.ROOT);
        return decomposed.replaceAll("\\p{M}+", "").trim();
    }
}
