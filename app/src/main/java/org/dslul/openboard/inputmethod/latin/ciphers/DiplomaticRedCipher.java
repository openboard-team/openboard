package org.dslul.openboard.inputmethod.latin.ciphers;

/** Educational keyed-substitution approximation inspired by the historical Japanese Red system. */
public final class DiplomaticRedCipher extends KeywordSubstitutionCipher {
    public DiplomaticRedCipher() {
        this("DIPLOMATICRED");
    }

    public DiplomaticRedCipher(final String keyword) {
        super(keyword == null || keyword.trim().isEmpty() ? "DIPLOMATICRED" : keyword);
    }
}
