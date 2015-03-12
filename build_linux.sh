#!/bin/sh
cd panbox-core
ant
cd ../panbox-common
ant
cd ../panbox-linux
ant
mv installer/output/panbox.zip ..
echo "Finished building PanBox client. The panbox.zip is ready to use."
