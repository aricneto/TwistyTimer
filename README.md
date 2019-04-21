<p align="center">
  <img src="https://i.imgur.com/OLlZRiO.png?1"/>
</p>

Twisty Timer is a highly customizable Material Design twisty puzzle timer for Android. It uses the TNoodle library to generate scramble sequences for all current official speedsolving puzzles.  

If you would like to add a new feature or fix something, just send a pull request.  

Special thanks to Prisma Puzzle Timer, TNoodle and PlusTimer for being my inspirations to create this project :)

<a href="https://play.google.com/store/apps/details?id=com.aricneto.twistytimer" target="_blank">
  <img alt="Get it on Google Play"
       src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="60"/>
</a>

# Translating [![Crowdin](https://d322cqt584bo4o.cloudfront.net/twisty-timer/localized.svg)](https://crowdin.com/project/twisty-timer)
If you're interested in translating Twisty Timer, just go to <a href="https://crowdin.com/project/twisty-timer">this page</a> and select your language. If it's not on the list, just contact me and I'll add it.

If you spot a problem in any of the translations, please correct it on the Crowdin page and send me a message, don't open a request here! This makes it easier to manage the localizations.

# Adding new algorithms
If you would like to contribute with new algorithms for the algorithm reference page of the app, just edit the `algorithms.json` file found in `main/res/raw/algorithms.json`. The JSON format used is as following:
```
{
  "subset": "[subset name]",
  "puzzle": "[222, 333, ..., pyra, mega, clock, sq1]", (only supports cube puzzles at the moment)
  "cases": [
    {
      "name": "[case name]",
      "algorithms": [
        "[algorithm]",
        "[other algorithm]"
      ],
      "state": [ (describes the state of the cube)
                 (each string represents a face)
                 (each char represents a sticker color (N is no color), starting from the top-left of the cube)
        "YNONNNRNG", (U face)
        "YNONNNNNN", (F)
        "YNBNNNNNN", (R)
        "YNBNNNNNN", (B)
        "RNGNNNNNN", (L)
        "NNNNNNNNN"  (D)
      ]
    }, ... (other cases)
  ]
}
```

# License (GNU GPL v3)

    Copyright (C) 2016  Ariovaldo Neto

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
