This directory contains dictionaries compiled from sentence lists to make use of next-word predictions.
Currently all dictionaries are based on word lists available at https://wortschatz.uni-leipzig.de/en/download/ under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) license.

Dictionaries are created using create_wordlist_from_sentences.py (in this directory) to create `<locale>_wordlist.combined` and [`dicttool_aosp.jar`](https://github.com/remi0s/aosp-dictionary-tools) for creating `.dict` files. See the `example_()` functions in the python script for how to use it. You can simply adjust paths and add your sentence lists (or word lists with one word per line).
The script is still experimental, rather slow and may produce bad dictionaries in some languages. Some words seem to be wrongly added (e.g. "i" for English, or "Der" for German), and names are typically missing, though this depends on how exactly you create the Android dictionaries.

Flags (like "offensive") are currently missing, same for shortcuts (e.g. ill -> I'll or écoeuré -> écœuré, as found in AOSP dictionaries).
