# Changelog

## 3.1 (Stable) - 23/7/2020

### **Added**

* Experimental CrossFade Loop has been added to SamplePlayer
    * Users must set a loop start point and a loop end point for crossfade to work.
* Instructions in the README file for setting up Maven or Gradle dependencies.

### **Fixed**

* Build.gradle for Maven Beads Distribution (v3.1) has been fixed to properly manage and locate dependencies now.

## 3.0  - 9/7/2020

### **Changed**

* Processing library build has now been flattened
* Build.gradle can now upload subsequent releases to Sonatype OSSRH
    * Credentials are stored in the .gradle folder seperate from this project.
