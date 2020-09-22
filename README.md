

# The Beads Project
![](http://www.beadsproject.net/minimalismheader.jpeg)

This is the Beads project, a Java library for creative realtime audio and music, which is also available as a library for Processing.

Written by Ollie Bown, with contributions from Ben Porter, Benito, Aengus Martin, Neil Smith, Evan Merz, Angelo Fraietta and Charlton Wong. It also uses some code from other Java projects including MEAP and JASS. For convenience it wraps Java dependencies Tritonus, JavaZOOM, mp3spi and JAudioLibs.

The library is licensed under the Gnu Public License (GPL v3). Contact ollie at icarus.nu if you have any questions.

For examples on how to use this library, see the [tutorials.](https://github.com/orsjb/beads/tree/master/src/beads_tutorial/java)
For all other information, see http://www.beadsproject.net.



## How to Install



Beads uses the Tritonus library found in the clojars repository.<br>



#### Maven    \[Current Release: [3.2](https://search.maven.org/artifact/net.beadsproject/beads/3.2/jar)\]

Add the following to pom.xml:



```

  <repository>

    <id>clojars_repo</id>

    <url>https://clojars.org/repo/</url>

  </repository>

  

  ....

  

  <dependency>

    <groupId>net.beadsproject</groupId>

    <artifactId>beads</artifactId>

    <version>3.2</version>

  </dependency>

```



#### Gradle



Add the following to build.gradle:



```

repositories {

    maven {url "https://clojars.org/repo"}

}



...



dependencies {

    implementation 'net.beadsproject:beads:3.2'

}

```



#### Manual Installation

Please visit [beadsproject.net](http://www.beadsproject.net) for beads.jar and manual installation instructions for Eclipse or Processing.



## Changelog



[Latest Version: 3.2](https://github.com/orsjb/beads/blob/master/CHANGELOG.md)
