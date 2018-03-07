#!/bin/bash
# Build script for Travis-CI.

SCRIPTDIR=$(cd $(dirname "$0") && pwd)
ROOTDIR="$SCRIPTDIR/../../.."
WHISKDIR="$ROOTDIR/openwhisk"
PACKAGESDIR="$WHISKDIR/catalog/extra-packages"

cd $WHISKDIR

tools/build/scanCode.py "$SCRIPTDIR/../.."

cd $WHISKDIR/ansible

ANSIBLE_CMD="ansible-playbook -i environments/local"

$ANSIBLE_CMD setup.yml
$ANSIBLE_CMD prereq.yml
$ANSIBLE_CMD couchdb.yml
$ANSIBLE_CMD initdb.yml

cd $WHISKDIR

./gradlew distDocker

cd $WHISKDIR/ansible

$ANSIBLE_CMD wipe.yml
$ANSIBLE_CMD openwhisk.yml
$ANSIBLE_CMD postdeploy.yml

cd $WHISKDIR

VCAP_SERVICES_FILE="$(readlink -f $WHISKDIR/../tests/credentials.json)"

#update whisk.properties to add tests/credentials.json file to vcap.services.file, which is needed in tests
WHISKPROPS_FILE="$WHISKDIR/whisk.properties"
sed -i 's:^[ \t]*vcap.services.file[ \t]*=\([ \t]*.*\)$:vcap.services.file='$VCAP_SERVICES_FILE':'  $WHISKPROPS_FILE
cat whisk.properties

WSK_CLI=$WHISKDIR/bin/wsk
AUTH_KEY=$(cat $WHISKDIR/ansible/files/auth.whisk.system)
EDGE_HOST=$(grep '^edge.host=' $WHISKPROPS_FILE | cut -d'=' -f2)

# Set Environment
export OPENWHISK_HOME=$WHISKDIR

# Place this template in correct location to be included in packageDeploy
mkdir -p $PACKAGESDIR/preInstalled/ibm-functions
cp -r $ROOTDIR/template-reminder-slack $PACKAGESDIR/preInstalled/ibm-functions/

# Install the deploy package
cd $PACKAGESDIR/packageDeploy/packages
source $PACKAGESDIR/packageDeploy/packages/installCatalog.sh $AUTH_KEY $EDGE_HOST $WSK_CLI

# Install alarms package
# cd $PACKAGESDIR/alarms-package
# source $PACKAGESDIR/alarms-package/installCatalog.sh $AUTH_KEY $EDGE_HOST $WSK_CLI


# Test
cd $ROOTDIR/template-reminder-slack
./gradlew :tests:test
