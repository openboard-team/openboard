package org.dslul.openboard.inputmethod.latin.ciphers;

/** Shared alphabets for classical substitutions across several keyboard scripts. */
final class CipherAlphabet {
    private static final String[] ALPHABETS = {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "abcdefghijklmnopqrstuvwxyz",
            "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ",
            "αβγδεζηθικλμνξοπρστυφχψω",
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ",
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
            "ԱԲԳԴԵԶԷԸԹԺԻԼԽԾԿՀՁՂՃՄՅՆՇՈՉՊՋՌՍՎՏՐՑՒՓՔՕՖ",
            "աբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆ",
            "აბგდევზთიკლმნოპჟრსტუფქღყშჩცძწჭხჯჰ",
            "אבגדהוזחטיכלמנסעפצקרשת",
            "ابتثجحخدذرزسشصضطظعغفقكلمنهوي",
            "अआइईउऊऋएऐओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसह",
            "অআইঈউঊঋএঐওঔকখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহ",
            "અઆઇઈઉઊઋએઐઓઔકખગઘઙચછજઝઞટઠડઢણતથદધનપફબભમયરલવશષસહ",
            "ਅਆਇਈਉਊਏਐਓਔਕਖਗਘਙਚਛਜਝਞਟਠਡਢਣਤਥਦਧਨਪਫਬਭਮਯਰਲਵਸ਼ਸਹ",
            "அஆஇஈஉஊஎஏஐஒஓஔகஙசஞடணதநபமயரலவழளறனஜஷஸஹ",
            "అఆఇఈఉఊఋఎఏఐఒఓఔకఖగఘఙచఛజఝఞటఠడఢణతథదధనపఫబభమయరలవశషసహ",
            "ಅಆಇಈಉಊಋಎಏಐಒಓಔಕಖಗಘಙಚಛಜಝಞಟಠಡಢಣತಥದಧನಪಫಬಭಮಯರಲವಶಷಸಹ",
            "അആഇഈഉഊഋഎഏഐഒഓഔകഖഗഘങചഛജഝഞടഠഡഢണതഥദധനപഫബഭമയരലവശഷസഹ",
            "กขฃคฅฆงจฉชซฌญฎฏฐฑฒณดตถทธนบปผฝพฟภมยรลวศษสหฬอฮ",
            "ກຂຄຆງຈຉຊຍດຕຖທນບປຜຝພຟມຢຣລວສຫອຮ"
    };

    private static final int HIRAGANA_START = 0x3041;
    private static final int HIRAGANA_END = 0x3096;
    private static final int KATAKANA_START = 0x30A1;
    private static final int KATAKANA_END = 0x30FA;
    private static final int HANGUL_START = 0xAC00;
    private static final int HANGUL_END = 0xD7A3;

    private CipherAlphabet() {
    }

    static boolean contains(final int codePoint) {
        if (!Character.isBmpCodePoint(codePoint)) {
            return false;
        }
        final char character = (char)codePoint;
        if (character == 'ς') {
            return true;
        }
        return findAlphabet(character) != null || findRange(character) != null;
    }

    static char shift(final char character, final int amount) {
        final char normalized = character == 'ς' ? 'σ' : character;
        final String alphabet = findAlphabet(normalized);
        if (alphabet != null) {
            final int index = alphabet.indexOf(normalized);
            final int shiftedIndex = positiveMod(index + amount, alphabet.length());
            return alphabet.charAt(shiftedIndex);
        }
        final int[] range = findRange(normalized);
        if (range == null) {
            return character;
        }
        return (char)(range[0] + positiveMod(normalized - range[0] + amount,
                range[1] - range[0] + 1));
    }

    static char reverse(final char character) {
        final char normalized = character == 'ς' ? 'σ' : character;
        final String alphabet = findAlphabet(normalized);
        if (alphabet != null) {
            return alphabet.charAt(alphabet.length() - 1 - alphabet.indexOf(normalized));
        }
        final int[] range = findRange(normalized);
        return range == null ? character : (char)(range[1] - (normalized - range[0]));
    }

    static int position(final char character) {
        final char normalized = character == 'ς' ? 'σ' : character;
        final String alphabet = findAlphabet(normalized);
        if (alphabet != null) {
            return alphabet.indexOf(normalized);
        }
        final int[] range = findRange(normalized);
        return range == null ? -1 : normalized - range[0];
    }

    private static String findAlphabet(final char character) {
        for (String alphabet : ALPHABETS) {
            if (alphabet.indexOf(character) >= 0) {
                return alphabet;
            }
        }
        return null;
    }

    private static int[] findRange(final char character) {
        if (character >= HIRAGANA_START && character <= HIRAGANA_END) {
            return new int[] { HIRAGANA_START, HIRAGANA_END };
        }
        if (character >= KATAKANA_START && character <= KATAKANA_END) {
            return new int[] { KATAKANA_START, KATAKANA_END };
        }
        if (character >= HANGUL_START && character <= HANGUL_END) {
            return new int[] { HANGUL_START, HANGUL_END };
        }
        return null;
    }

    private static int positiveMod(final int value, final int modulus) {
        return ((value % modulus) + modulus) % modulus;
    }
}
