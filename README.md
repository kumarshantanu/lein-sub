# lein-sub

Leiningen plugin to execute tasks on sub-projects.

Should you need recursive behavior, consider Leiningen aliases or
[lein-cascade](https://github.com/kumarshantanu/lein-cascade).

## Installation

### Lein 2 users

Either install as a plugin in `~/.lein/profiles.clj`:

    {:user {:plugins [[lein-sub "0.4.0"]]}}

Or, specify as a plugin in `project.clj`:

    :plugins [[lein-sub "0.4.0"]]

### Leiningen 1.x users

Either install as a plugin:

    $ lein plugin install lein-sub "0.1.2"

Or, include as a dev-dependency in `project.clj`:

    :dev-dependencies [[lein-sub "0.1.2"]]

## Usage

Your project may have sub-projects -
you can specify the sub-projects as follows in the top level `project.clj`:

```clojure
:sub ["module/foo-common" "module/dep-vendor-xyz"]
```

These values are the _directory_ names of the sub-projects.
Each sub-project must have its own `project.clj`.

Execute the plugin:

```bash
$ lein sub deps   # runs "lein deps" on each of 'foo-common' and 'dep-vendor-xyz'

$ lein sub jar      # runs "lein jar" on both
$ lein sub install  # install both sub-project artifacts to local Maven repo
```

Starting in 0.4.0, lein-sub will automatically determine the
correct execution order for the sub-projects based on
inter-project dependencies.


Alternately, you may pass sub-project directories via the command line, using the
`--submodules` (or `-s`) option:

```bash
$ lein sub -s "module/foo-common:module/dep-vendor-xyz" jar
```

The `:` character is used to separate the directory names.

When `--submodules` is not specified, your top-level project *must*
have a :sub key.
When `--submodules` *is* specified, the :sub key is ignored.

## Notes

lein-sub will create a file, `target/sub-cache.edn`, to store
project information between executions, including the
execution order.
This cache file will be recreated if deleted, or any time
any `project.clj` file is changed.
Even on a large project (over 40 modules), it takes less than a second
to rebuild.

## Getting in touch

On Twitter: [@kumarshantanu](http://twitter.com/kumarshantanu)

On E-mail: kumar.shantanu(at)gmail.dot

On Leiningen mailing list: [http://groups.google.com/group/leiningen](http://groups.google.com/group/leiningen)


## Contributors

* Shantanu Kumar **(Author)** (https://github.com/kumarshantanu)
* Phil Hagelberg (https://github.com/technomancy)
* Hugo Duncan (https://github.com/hugoduncan)
* Creighton Kirkendall (https://github.com/ckirkendall)
* Howard M. Lewis Ship (https://github.com/hlship)


## License

Copyright © 2011-2017 Shantanu Kumar and contributors

Adapted from Phil Hagelberg's example: http://j.mp/oC9TTo

Distributed under the Eclipse Public License, the same as Clojure.
