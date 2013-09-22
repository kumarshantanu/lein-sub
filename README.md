# lein-sub

Leiningen plugin to execute tasks on sub-projects

Should you need recursive behavior, consider Leiningen aliases or
[lein-cascade](https://github.com/kumarshantanu/lein-cascade).


## Installation

### Lein 2 users

Either install as a plugin in `~/.lein/profiles.clj`:

    {:user {:plugins [[lein-sub "0.3.0"]]}}

Or, specify as a plugin in `project.clj`:

    :plugins [[lein-sub "0.3.0"]]

### Leiningen 1.x users

Either install as a plugin:

    $ lein plugin install lein-sub "0.1.2"

Or, include as a dev-dependency in `project.clj`:

    :dev-dependencies [[lein-sub "0.1.2"]]


## Usage

Your project may have sub-projects (each having its own project.clj file) -
you can specify them as follows:

```clojure
:sub ["module/foo-common" "module/dep-vendor-xyz"]
```

Execute the plugin:

```bash
$ lein sub deps   # runs "lein deps" on each of 'foo-common' and 'dep-vendor-xyz'

$ lein sub jar      # runs "lein jar" on both
$ lein sub install  # install both sub-project artifacts to local Maven repo
```

You can pass subproject directory locations via command line (overrides `:sub`):

```bash
$ lein sub -s "module/foo-common:module/dep-vendor-xyz" jar
```


## Getting in touch

On Twitter: [@kumarshantanu](http://twitter.com/kumarshantanu)

On E-mail: kumar.shantanu(at)gmail.dot

On Leiningen mailing list: [http://groups.google.com/group/leiningen](http://groups.google.com/group/leiningen)


## Contributors

* Shantanu Kumar **(Author)** (https://github.com/kumarshantanu)
* Phil Hagelberg (https://github.com/technomancy)
* Hugo Duncan (https://github.com/hugoduncan)
* Creighton Kirkendall (https://github.com/ckirkendall)


## License

Copyright © 2011-2013 Shantanu Kumar and contributors

Adapted from Phil Hagelberg's example: http://j.mp/oC9TTo

Distributed under the Eclipse Public License, the same as Clojure.
