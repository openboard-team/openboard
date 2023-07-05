#!/bin/python
import math
import os
import time
import regex
from spylls.hunspell import Dictionary

# issues:
# for english got 'i' as word (shouldn't it be 'I' only? or is 'i' the imaginary number?)
# potentially_offensive is not set, where to get info? parse android dicts?
# maybe ignore compound words like 'long-term'? will android actually suggest them?

# maybe useful
# https://wortschatz.uni-leipzig.de/en/download/
#  really useful source of sentences / fragments, but should be checked against dictionaries as they are taken from news / web
# https://en.wiktionary.org/wiki/Wiktionary:Frequency_lists
#  word frequency lists linked, in some cases there are also sentence lists
# https://github.com/wooorm/dictionaries
#  hunspell dicts, are they the same as the one included in phunspell?

# memory usage depends on word lists and language, expect 0.5 - 2 GB


# from https://github.com/zverok/spylls/blob/master/examples/unmunch.py
def unmunch_word(word, aff):
    result = set()

    if aff.FORBIDDENWORD and aff.FORBIDDENWORD in word.flags:
        return result

    if not (aff.NEEDAFFIX and aff.NEEDAFFIX in word.flags):
        result.add(word.stem)

    suffixes = [
        suffix
        for flag in word.flags
        for suffix in aff.SFX.get(flag, [])
        if suffix.cond_regexp.search(word.stem)
    ]
    prefixes = [
        prefix
        for flag in word.flags
        for prefix in aff.PFX.get(flag, [])
        if prefix.cond_regexp.search(word.stem)
    ]

    for suffix in suffixes:
        root = word.stem[0:-len(suffix.strip)] if suffix.strip else word.stem
        suffixed = root + suffix.add
        if not (aff.NEEDAFFIX and aff.NEEDAFFIX in suffix.flags):
            result.add(suffixed)

        secondary_suffixes = [
            suffix2
            for flag in suffix.flags
            for suffix2 in aff.SFX.get(flag, [])
            if suffix2.cond_regexp.search(suffixed)
        ]
        for suffix2 in secondary_suffixes:
            root = suffixed[0:-len(suffix2.strip)] if suffix2.strip else suffixed
            result.add(root + suffix2.add)

    for prefix in prefixes:
        root = word.stem[len(prefix.strip):]
        prefixed = prefix.add + root
        if not (aff.NEEDAFFIX and aff.NEEDAFFIX in prefix.flags):
            result.add(prefixed)

        if prefix.crossproduct:
            additional_suffixes = [
                suffix
                for flag in prefix.flags
                for suffix in aff.SFX.get(flag, [])
                if suffix.crossproduct and not suffix in suffixes and suffix.cond_regexp.search(prefixed)
            ]
            for suffix in suffixes + additional_suffixes:
                root = prefixed[0:-len(suffix.strip)] if suffix.strip else prefixed
                suffixed = root + suffix.add
                result.add(suffixed)

                secondary_suffixes = [
                    suffix2
                    for flag in suffix.flags
                    for suffix2 in aff.SFX.get(flag, [])
                    if suffix2.crossproduct and suffix2.cond_regexp.search(suffixed)
                ]
                for suffix2 in secondary_suffixes:
                    root = suffixed[0:-len(suffix2.strip)] if suffix2.strip else suffixed
                    result.add(root + suffix2.add)

    return result


def unmunch_dictionary(dictionary: Dictionary) -> set[str]:
    result = set()
    for word in dictionary.dic.words:
        result.update(unmunch_word(word, dictionary.aff))
    return result


class wordlist:
    def __init__(self,
                 # spylls dictionary
                 dictionary: Dictionary | None = None,
                 # words that should be ignored, typically (international) names we don't want in a language word list
                 neutral_words: set[str] | None = None,
                 # path where unmunched dictionary is cached (because creation is really slow)
                 # file will be written if not read
                 unmunched_cache: str | None = None):
        self.dictionary = dictionary
        self.dict_words: set[str] = set()
        if dictionary is not None:
            # this and the whole unmunch possibly would not be necessary if hunspell had implemented
            # this 15+ year old feature request: https://github.com/hunspell/hunspell/issues/258

            # use unmunched_cache spell-checked list if available
            if unmunched_cache is not None and os.path.isfile(unmunched_cache):
                try:
                    with open(unmunched_cache) as f:
                        for w in f:
                            self.dict_words.add(w.strip())
                except:
                    print(f"error reading {unmunched_cache}")
                    self.dict_words = {}
            if len(self.dict_words) == 0:
                s = unmunch_dictionary(dictionary)
                # unmunch may create word fragments
                #  remove words that are not valid according to dictionary
                #  or that start or end with -
                # unfortunately this can be really slow depending on language, seen from a few seconds up to hours (cs)
                #  but with the cache it's ok
                for word in s:
                    if not word.startswith("-") and not word.endswith("-") and not word.isdigit() and dictionary.lookup(word):
                        self.dict_words.add(word)
                if unmunched_cache is not None:
                    try:
                        with open(unmunched_cache, 'w') as f:
                            f.writelines([str(i)+'\n' for i in self.dict_words])
                    except:
                        print(f"could not write to {unmunched_cache}")
        if neutral_words is None:
            self.neutral_words = set()
        else:
            self.neutral_words = neutral_words
    # number of identified words
    count = 0
    # number of words used for frequency
    count_valid = 0
    # word and number of occurrences
    frequencies: dict[str, int] = {}
    # contains dict for each word, with next word and number of occurrences
    bigrams: dict[str, dict[str, int]] = {}
    # words to ignore, as they should be in some additional dictionary (mostly names)
    # these are not counted as valid
    neutral_word_count = 0
    # words detected as invalid
    invalid_words: set[str] = set()
    not_words: set[str] = set()
    # unclear words with more than one match group in above regex
    # check and decide in the end what to do
    weird_things: set[str] = set()

    # regex for that kicks out things that are definitely not words
    # next word will be treated as ngram start
    # allow latin letters, and ' and - (but not at start/end)
    possible_word_regex = r"(?!['-])([\p{L}\d'-]+)(?<!['-])"  # \p{L} requires regex, not re
    # possible_word_regex = r"([\w\d]+)"  # this works with re, but problematic for most languages

    def add_unmunched_dictionary(self):
        count = 0
        for word in self.dict_words:
            if word not in self.frequencies:
                self.frequencies[word] = 1
                count += 1
        print(count, "words added using add_unmunched_dictionary")

    # currently ngram ends after every non-word character
    # like 2, ", ,, ., -
    # any cases where this shouldn't happen?
    def add_line(self, line: str,
                 # true for proper sentences / sentence fragments, false for word lists
                 add_to_bigrams: bool = True,
                 # set to false if line may contain spelling mistake or words that should not be learned otherwise
                 add_new_words: bool = True) -> None:
        previous_word: str | None = None
        for word in line.split():
            if len(word) >= 48:
                # android dicttool ignores those, so let's skip them already here
                previous_word = None
                continue
            if word.isspace():
                # don't treat spaces as countable word (assuming a line does not contain a line break)
                continue
            if word.isnumeric():
                # don't put numbers info not_words
                previous_word = None
                continue
            if not regex.search(r"\p{L}", word):
                # no letters, no word (but ngram ends here)
                self.not_words.add(word)
                previous_word = None
                continue
            # hunspell dict has ', not ’, but we want to understand both
            word = word.replace('’', '\'')
            # must find something, because r"\p{L}" non-matches are already removed
            re_find = regex.findall(self.possible_word_regex, word)
            self.count += 1
            if len(re_find) > 1:
                self.weird_things.add(word)
            # treat re_find[0] as the actual word, but need to investigate weird_things to maybe improve possible_word_regex
            full_word = word
            word = re_find[0]
            # if the match is not at the start, treat as ngram start
            if not full_word.startswith(word):
                previous_word = None

            if word in self.neutral_words:
                self.neutral_word_count += 1
                continue

            if len(self.dict_words) == 0:  # no (valid) dictionary
                if previous_word is None and word[0].isupper() and word not in self.frequencies:
                    # uppercase might come from sentence start, so better ignore this word
                    continue
            elif word not in self.dict_words:
                # if ngram start, look up the version with first letter lowercase
                # this is not always sentence start, but should be quite often
                if previous_word is None and word[0].isupper():
                    decapitalized = word[0].lower() + word[1:]
                    if decapitalized in self.dict_words:
                        word = decapitalized
                    else:
                        # don't add to invalid words, we're not sure and just ignore it
                        continue
                else:
                    # the word was not capitalized, so checking with hunspell should be fine.
                    # this finds some words that are not in unmunched dict_words, especially compound words
                    #  but also words we don't want, where capitalization makes the word invalid... but hunspell can't deal with this
                    #  only way so far: find words manually and add to self.neutral_words
                    #  TODO: possible approach for allowing adding compound words, but not prohibiting capitalized versions of common words:
                    #   treat short words (e.g. less than 8 or 10 letters) always as invalid here
                    #   but this likely has unwanted side effects... just try and check the invalid word list (have a separate one for initial testing)
                    # we also want to exclude full uppercase words here (these are not in dict_words, but allowed by hunspell)
                    try:
                        if (self.dictionary is not None and not self.dictionary.lookup(word)) or word.isupper():
                            self.invalid_words.add(word)
                            previous_word = None
                            continue
                    except IndexError:
                        # happens for İsmail and other words starting with 'İ', possibly spylls issue
                        print(f"dictionary issue when looking up {word}")
                        self.invalid_words.add(word)
                        previous_word = None
                        continue

            if add_new_words or word in self.frequencies:
                self.frequencies[word] = self.frequencies.get(word, 0) + 1
            self.count_valid += 1
            if previous_word is not None and (add_new_words or word in self.frequencies):
                bigram = self.bigrams.get(previous_word, {})
                bigram[word] = bigram.get(word, 0) + 1
                self.bigrams[previous_word] = bigram
            # set new previous word, or None if ngram end is suspected (this could be optimized, but no priority)
            if add_to_bigrams and full_word.endswith(word):
                previous_word = word
            else:
                previous_word = None

    def add_file(self, filename: str, add_to_bigrams: bool = True, add_new_words: bool = True):
        with open(filename) as f:
            for line in f:
                self.add_line(line, add_to_bigrams, add_new_words)

    # dicts need all the input, but only type and locale are relevant
    # type can be any ASCII string, but typically main is used
    #  note that only one dict can be loaded for each type
    #  using main also overrides any built-in dictionary in my version of OpenBoard
    # locale should be in compatible format (e.g. en, en_US, fr, fr_CA,...)
    def create_android_word_list(self, file_path, dict_type: str, locale: str, description: str, version: int):
        with open(file_path, 'w') as f:
            t = int(time.time())
            # e.g. dictionary=main:en_us,locale=en_US,description=English (US),date=1414726260,version=54
            header = f"dictionary={dict_type}:{locale.lower()},locale={locale},description={description},date={t},version={version}"
            if locale.startswith("de"):
                header += ",REQUIRES_GERMAN_UMLAUT_PROCESSING=1"
                # any special things for other languages?
                # russian dict has MULTIPLE_WORDS_DEMOTION_RATE=50 -> what's this?
            f.write(header + "\n")
            bigrams = filter_bigrams(self.bigrams, 3, 2)  # todo: maybe set min to 3 to avoid huge dict for rare words
            # todo: check android dicts and maybe some documentation about frequencies
            for word, frequency in sort_dict_by_frequency(normalize_frequencies(self.frequencies, 230, 1)):
                # todo: check other dicts whether flags or originalFreq is necessary
                # and check what they actually do
                f.write(f" word={word},f={frequency}\n")
                if word not in bigrams:
                    continue
                for next_word, freq in bigrams[word].items():
                    f.write(f"  bigram={next_word},f={freq}\n")


# returns a dictionary with the frequencies being the log of input frequencies, and scaled between min and max
def normalize_frequencies(frequencies: dict[str, int], max_frequency: int, min_frequency: int) -> dict[str, int]:
    assert max_frequency > min_frequency
    max_f = 0
    min_f = 2147483647  # simply start with a very large number (int32 max)
    d = {}
    for word, f in frequencies.items():
        f = math.log(f)
        if f < min_f:
            min_f = f
        if f > max_f:
            max_f = f
    fdiff = max(max_f - min_f, 1)
    for word, f in frequencies.items():
        f = math.log(f)
        d[word] = int((f - min_f) * (max_frequency - min_frequency) / fdiff + min_frequency)
    return d


# returns a dict like bigrams, but
#  fewer entries (limiting to max_bigrams and requiring min frequency)
#  frequency replaced by order, starting at 1 for the most used, like it seems to be in the AOSP en(_US) dictionary
def filter_bigrams(bigrams: dict[str, dict[str, int]], max_bigrams, min_bigram_frequency) -> dict[str, dict[str, int]]:
    new_bigrams: dict[str, dict[str, int]] = {}
    for word, bigram in bigrams.items():
        new_bigram = dict()
        count = 1
        for next_word, frequency in sort_dict_by_frequency(bigram):
            if count > max_bigrams or frequency < min_bigram_frequency:
                break
            new_bigram[next_word] = count
            count += 1
        new_bigrams[word] = new_bigram
    return new_bigrams


# highest frequency first
def sort_dict_by_frequency(d: dict[str, int]):
    return sorted(d.items(), key=lambda item: -item[1])


# use existing dictionary for spell check (not perfect with capitalization)
# use sentence list to build frequencies and bigrams
def example_1():
    d = Dictionary.from_files("/home/user/.local/lib/python3.10/site-packages/phunspell/data/dictionary/en/en_US")
    # unmunched cache not necessary for english, but helps for e.g. German or Czech
    w = wordlist(dictionary=d, unmunched_cache="/home/user/unmunched_en_US.txt")
    w.add_file("/home/user/eng_news_2020_100K/eng_news_2020_100K-sentences.txt",
               add_to_bigrams=True,  # these are sentences, so use them to build bigrams
               add_new_words=True)  # we don't have any words in our frequency list yet, so False results in nothing happening
    w.create_android_word_list("/home/user/en_US_wordlist.compiled", "main", "en_US", "english", 1)
    print("frequencies", w.frequencies)
    print("bigrams", w.bigrams)


# use existing dictionary for spell check (not perfect with capitalization)
# use words from unmunched (affix-expanded) dictionary as the only valid words
#  this is bad for languages that allow compound words, such as German
# use sentence list to build frequencies and bigrams of words in the list only
def example_2():
    d = Dictionary.from_files("/home/user/.local/lib/python3.10/site-packages/phunspell/data/dictionary/en/en_US")
    # unmunched cache not necessary for english, but helps for e.g. german or czech
    w = wordlist(dictionary=d)
    w.add_unmunched_dictionary()  # adds all words with frequency 1
    w.add_file("/home/user/eng_news_2020_100K/eng_news_2020_100K-sentences.txt",
               add_to_bigrams=True,  # these are sentences, so use them to build bigrams
               add_new_words=False)  # we have a list of valid words and don't want to add more
    w.create_android_word_list("/home/user/en_US_wordlist.compiled", "main", "en_US", "english", 1)


# don't use a dictionary, only a word list
# this will produce low-quality suggestions, as frequency is the same for all words
def example_3():
    w = wordlist()
    w.add_file("/home/user/some_word_list.txt",
               add_to_bigrams=False)  # only words without connection
    w.create_android_word_list("/home/user/en_US_wordlist.compiled", "main", "en_US", "english", 1)


# don't use a dictionary, but a list of sentences
# android word list may contain spelling errors depending on source of the sentences
def example_4():
    w = wordlist()
    w.add_file("/home/user/eng_news_2020_100K/eng_news_2020_100K-sentences.txt")
    w.create_android_word_list("/home/user/en_US_wordlist.compiled", "main", "en_US", "english", 1)

