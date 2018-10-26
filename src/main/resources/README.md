---
title: Resources for TEI services
---

# Dictionaries

- `dereko_capital_only.txt`  – words occurring capitalized-only in
  DeReKo.
- `FOLK_Normalization_Lexicon.xml` (currently link to
    `FOLK_Normalization_Lexicon_MAY_2018.xml`) – dictionary of
    normalizations in the FOLK corpus, containing counts of
    normalizations.
- `dict.tsv`: dictionary taking majority normalization from the FOLK and
  first match from the DeReKo dictionaries.


# Language codes

There seems to be no good list beside [this list of ISO 639-2
codes](http://www.loc.gov/standards/iso639-2/php/code_list.php) linked
to [by ISO](https://www.iso.org/iso-639-language-codes.html). From this
one we compiled lists which use the three letter code of languages as
the identifier, the terminological one if there are two (i.e., `sqi` for
Albanian, not `alb`).

- `language-list-639-1-and-639-2.json`: list of languages with all
  features (two- and three-letter codes, names in English, French,
  German) from [this list of ISO 639-2
  codes](http://www.loc.gov/standards/iso639-2/php/code_list.php)
- `language-list-639-1-to-639-2.json`: list of two-letter codes
  pointing to the three-letter code
- `languages-639-most-tolerant.json`: list of names and codes pointing
  to the three-letter code


# TreeTagger models

- `tt-langs.json` list from three-letter language codes pointing to the
  [TreeTagger
  model](http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/) we
  use for the TEI tools.
