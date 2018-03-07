#!/bin/bash

SCRIPTDIR=$(cd $(dirname "$0") && pwd)
HOMEDIR="$SCRIPTDIR/../../../"
DEPLOYDIR="$HOMEDIR/openwhisk/catalog/extra-packages/packageDeploy"
ALARMSDIR="$HOMEDIR/openwhisk/catalog/extra-packages/alarms-package"

# jshint support
sudo apt-get -y install nodejs npm
sudo npm install -g jshint

# clone utilties repo. in order to run scanCode.py
cd $HOMEDIR
git clone https://github.com/apache/incubator-openwhisk-utilities.git

# shallow clone OpenWhisk repo.
git clone --depth 1 https://github.com/apache/incubator-openwhisk.git openwhisk

# shallow clone deploy package repo.
git clone --depth 1 https://github.com/apache/incubator-openwhisk-package-deploy $DEPLOYDIR

# shallow clone alarms package repo.
git clone --depth 1 https://github.com/apache/incubator-openwhisk-package-alarms $ALARMSDIR

cd openwhisk
./tools/travis/setup.sh
