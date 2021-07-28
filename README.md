# micro-kotlin-args
The thinnest possible, yet usable, flags/options parser for Kotlin

> code-golf as a service

This library/snippet seeks to be the smallest possible args/flags/options parsing library 
for Kotlin, dependency free, and suitable for inclusion in kotlin apps or kotlin `.kts`
scripts. 

This will have known defects, becasue of it's enforced terseness, notably that it will have
only a limited (but surprisingly flexible) set of features, will not be as safe, or easy to
debug as other libraries.

Some (current) limitations include:
   - will not do up-front init validation (lazily evaluating the flags)
   - No support for commands, post-flag/option list arguments
   
These may never be added or "fixed", as it may not be feasible to impelement them and keep
under the ideal goal of 50 lines of code.

**micro-kotlin-args** definitely primarily a tool for scripting. The constraint on size is
so that it can be cut-and-pasted into scripts, even those that only use `kotlinc -script`
and don't have extra fancy dependency management systems like kscript. That said, it's
similar enough to xenomachina or kotlin-cli that it should be familiar to use even in 
non-scripting contexts, and will be released as a maven artifact as well for this purpose.

# Usage

## Scripting

### Cut and paste

Just go to the source page, and cut and paste it in. 

### Scripting

Get the raw url of the source file, and use in kscript (or another similar system) and
use `@file:Include(<url_to_source_code>)` in your kotlin script.

## Maven Artifact

TBD

## License

Copyright (c) 2021, Christian Gruber, All Rights Reserved.

This code is multi-licensed, which license chosen at the user's discretion as:
    - Apache 2.0
    - BSD 2-clause simplified
    - MIT license
    - WTFPL if you aren't at liability risk using it

I would make it Public Domain but US and other legal regimes have real problems around
Public Domain and I don't want someone re-asserting property rights over this code, when
I wish it to be free for anyone to use, hence my reservation of rights in the copyright
line. I wish I didn't have to do that. I hereby also waive any notice clauses in the above
licenses, and so credit in consuming software is not required to be given to the author.
