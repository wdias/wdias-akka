# wdias
Weather Data Integration and Assimilation System

## Installation

#### How to build
**WDIAS** is using [sbt-pack plugin](https://github.com/xerial/sbt-pack) which is using for creating distributable Scala packages.

**Create a package**

    $ git clone git@github.com:wdias/wdias.git
    $ cd <PATH_WDIAS>
    $ sbt pack

WDIAS package will be generated in `<PATH_WDIAS>/target/pack` folder.

**Run command**

    $ <PATH_WDIAS>/target/pack/bin/WDIAS
    SingleRoutes - Server online at http://0.0.0.0:8000/
    Press RETURN to stop...

**Install the command**

Install the command to `$(HOME)/local/bin`:
```
$ sbt packInstall
```

or

```
$ cd target/pack; make install
```

To launch the command:
```    
    $ ~/local/bin/WDIAS
   SingleRoutes - Server online at http://0.0.0.0:8000/
   Press RETURN to stop...
```

Add the following configuration to your .bash_profile, .zsh_profile, etc. for the usability:
```
export PATH=$(HOME)/local/bin:$PATH
```

**Install the command to the system**
   
    $ cd target/pack
    $ sudo make install PREFIX="/usr/local"
    $ /usr/local/bin/WDIAS
    SingleRoutes - Server online at http://0.0.0.0:8000/
    Press RETURN to stop...

For more information about available options look into [sbt-pack plugin](https://github.com/xerial/sbt-pack).
