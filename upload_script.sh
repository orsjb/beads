#!/bin/bash

# only run this from the folder it lives in

# create and upload the Processing 2.0 official distro (which lives at http://www.beadsproject.net/library/)

cd build/beads_processing
zip -r beads.zip beads
scp beads.zip orsjb@beadsproject.net:beadsproject.net/library/
scp beads/library.properties orsjb@beadsproject.net:beadsproject.net/library/beads.txt
rm beads.zip
cd ../..

# create and upload the two zip packages

cd build
rm *.zip
NOW=`date +"%Y%m%d"`
zip -r Beads.zip beads
zip -r Beads_Processing.zip beads_processing
cp Beads.zip Beads${NOW}.zip
cp Beads_Processing.zip Beads_Processing${NOW}.zip
scp *.zip orsjb@beadsproject.net:beadsproject.net/downloads/
rm *.zip
cd ..

# upload the docs

cd build
scp -r doc orsjb@beadsproject.net:beadsproject.net/
cd ..
