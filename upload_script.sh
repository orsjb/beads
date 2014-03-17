#!/bin/bash

cd build/beads_processing
zip -r beads.zip beads
scp beads.zip orsjb@beadsproject.net:beadsproject.net/library/
scp beads/library.properties orsjb@beadsproject.net:beadsproject.net/library/beads.txt